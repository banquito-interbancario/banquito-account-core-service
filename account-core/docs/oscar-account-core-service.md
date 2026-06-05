# Account Core Service - Servicios de Oscar

## Proposito

`account-core-service` administra saldos, movimientos e historial de cuentas del Core BanQuito V2.

El microservicio de Oscar es responsable de:

- Consultar saldos de cuentas.
- Consultar historial de movimientos.
- Ejecutar depositos y retiros por ventanilla.
- Ejecutar transferencias internas P2P.
- Procesar acreditaciones masivas On-Us.
- Debitar la cuenta matriz empresarial para pagos masivos.
- Registrar movimientos locales en `ACCOUNT_TRANSACTION`.
- Invocar a `accounting-service` por gRPC para registrar asientos contables.
- Invocar a `party-service` por gRPC para validar cliente/titular cuando aplica.

No es responsabilidad de este microservicio:

- Cierre contable EOD.
- Balance de comprobacion.
- Persistencia de asientos contables.
- Reportes de novedades.
- Calculo de tarifas del Switch.
- Envio de notificaciones.

## REST Expuesto

Estos endpoints son los que consumen Kong, frontends u otros micros segun la matriz de endpoints.

### GET `/api/v2/accounts/{accountId}/balance`

Consulta saldo disponible y saldo contable de una cuenta.

Consumidores esperados:

- Banca Web Personas.
- Ventanilla.

Path params:

| Campo | Tipo | Requerido | Descripcion |
|---|---:|---:|---|
| `accountId` | Long | Si | ID interno de la cuenta. |

Respuesta `200 OK`:

```json
{
  "accountId": 1,
  "accountNumber": "220001",
  "availableBalance": 1500.00,
  "accountingBalance": 1500.00,
  "status": "ACTIVE",
  "currency": "USD"
}
```

Validaciones:

- La cuenta debe existir.

Errores relevantes:

- `404`: cuenta no encontrada.

### GET `/api/v2/accounts/customer/{customerId}`

Lista las cuentas asociadas a un cliente para seleccionar la cuenta origen.

Consumidores esperados:

- Banca Web Personas.
- Frontend o BFF autenticado que ya resolvio el `customerId` del cliente logueado.

Path params:

| Campo | Tipo | Requerido | Descripcion |
|---|---:|---:|---|
| `customerId` | Long | Si | ID del cliente administrado por `party-service`. |

Respuesta `200 OK`:

```json
[
  {
    "accountId": 1,
    "accountNumber": "2200000001",
    "customerId": 2,
    "status": "ACTIVE",
    "availableBalance": 1200.00,
    "accountingBalance": 1200.00,
    "currency": "USD"
  }
]
```

Notas:

- Si el cliente no tiene cuentas, devuelve lista vacia `[]`.
- El endpoint no resuelve datos personales del cliente; esa informacion le corresponde a `party-service`.

### GET `/api/v2/accounts/{accountId}/transactions`

Consulta historial paginado de movimientos recientes de una cuenta.

Consumidores esperados:

- Banca Web Personas.
- Ventanilla.

Path params:

| Campo | Tipo | Requerido | Descripcion |
|---|---:|---:|---|
| `accountId` | Long | Si | ID interno de la cuenta. |

Query params:

| Campo | Tipo | Requerido | Default | Descripcion |
|---|---:|---:|---:|---|
| `page` | Integer | No | `0` | Numero de pagina. |
| `size` | Integer | No | `20` | Tamano de pagina. |
| `from` | Date | No | - | Fecha inicial `YYYY-MM-DD`. |
| `to` | Date | No | - | Fecha final `YYYY-MM-DD`. |

Respuesta `200 OK`:

```json
{
  "content": [
    {
      "transactionUuid": "uuid",
      "movementType": "CREDIT",
      "amount": 500.00,
      "resultingBalance": 1500.00,
      "transactionDate": "2026-05-30T10:30:00",
      "accountingDate": "2026-05-30",
      "description": "Teller deposit"
    }
  ],
  "totalElements": 1,
  "page": 0
}
```

Validaciones:

- La cuenta debe existir.

Errores relevantes:

- `404`: cuenta no encontrada.

### POST `/api/v2/accounts/teller/deposit`

Registra deposito en efectivo por ventanilla.

Consumidor esperado:

- Frontend de Ventanilla.

Request:

```json
{
  "accountId": 1,
  "amount": 500.00,
  "tellerId": 3,
  "branchId": 2,
  "transactionUuid": "uuid-generado-frontend",
  "reference": "Deposito efectivo"
}
```

Respuesta `200 OK`:

```json
{
  "transactionId": "uuid-generado-frontend",
  "accountingDate": "2026-05-30",
  "newBalance": 1500.00,
  "status": "COMPLETED",
  "timestamp": "2026-05-30T10:30:00"
}
```

Hace:

- Valida idempotencia por `transactionUuid`.
- Valida que la cuenta exista.
- Valida que la cuenta este `ACTIVE`.
- Valida cliente activo contra `party-service` via gRPC.
- Acredita saldo disponible y contable.
- Registra movimiento local `CREDIT / TELLER_DEPOSIT`.
- Llama a `accounting-service` via gRPC para registrar el asiento contable.

Asiento contable enviado:

- Debito: `1.1.0.02` Boveda Central.
- Credito: cuenta pasiva del cliente, segun tipo de cuenta.

Errores relevantes:

- `400`: request invalido o cuenta inactiva.
- `404`: cuenta no encontrada.
- `409`: `transactionUuid` duplicado.
- `503`: `accounting-service` o `party-service` no disponible via gRPC.

### POST `/api/v2/accounts/teller/withdrawal`

Registra retiro en efectivo por ventanilla.

Consumidor esperado:

- Frontend de Ventanilla.

Request:

```json
{
  "accountId": 1,
  "amount": 200.00,
  "tellerId": 3,
  "branchId": 2,
  "transactionUuid": "uuid-generado-frontend",
  "reference": "Retiro efectivo"
}
```

Respuesta `200 OK`:

```json
{
  "transactionId": "uuid-generado-frontend",
  "accountingDate": "2026-05-30",
  "newBalance": 1300.00,
  "status": "COMPLETED",
  "timestamp": "2026-05-30T10:35:00"
}
```

Hace:

- Valida idempotencia por `transactionUuid`.
- Valida que la cuenta exista.
- Valida que la cuenta este `ACTIVE`.
- Valida cliente activo contra `party-service` via gRPC.
- Valida saldo disponible suficiente.
- Debita saldo disponible y contable.
- Registra movimiento local `DEBIT / TELLER_WITHDRAWAL`.
- Llama a `accounting-service` via gRPC para registrar el asiento contable.

Asiento contable enviado:

- Debito: cuenta pasiva del cliente.
- Credito: `1.1.0.02` Boveda Central.

Errores relevantes:

- `400`: request invalido, cuenta inactiva o saldo insuficiente.
- `404`: cuenta no encontrada.
- `409`: `transactionUuid` duplicado.
- `503`: `accounting-service` o `party-service` no disponible via gRPC.

### POST `/api/v2/accounts/transfer/p2p`

Ejecuta transferencia interna entre cuentas BanQuito.

Consumidor esperado:

- Banca Web Personas.

Request:

```json
{
  "originAccountId": 1,
  "destinationAccountNumber": "2200000002",
  "amount": 300.00,
  "transactionUuid": "8df67f8d-2a8c-4d61-94d5-88a6044b54ad",
  "reference": "Pago arriendo"
}
```

Campos:

| Campo | Tipo | Requerido | Descripcion |
|---|---:|---:|---|
| `originAccountId` | Long | Si | ID interno de la cuenta origen seleccionada desde `/api/v2/accounts/customer/{customerId}`. |
| `destinationAccountNumber` | String | Si | Numero de cuenta destino validado previamente por `party-service` usando gRPC contra Oscar. |
| `amount` | Decimal | Si | Monto mayor a cero. |
| `transactionUuid` | String | Si | Identificador unico generado por el frontend/BFF para idempotencia. |
| `reference` | String | No | Concepto visible de la transferencia. |

Respuesta `200 OK`:

```json
{
  "transactionId": "uuid",
  "originNewBalance": 1200.00,
  "destinationAccountNumber": "220002",
  "destinationHolderName": "Juan Perez",
  "status": "COMPLETED",
  "accountingDate": "2026-05-30"
}
```

Hace:

- Valida idempotencia por `transactionUuid`.
- Valida cuenta origen por `originAccountId`.
- Valida cuenta destino por `destinationAccountNumber`.
- Valida que origen y destino sean cuentas distintas.
- Valida que ambas cuentas esten `ACTIVE`.
- Valida clientes activos contra `party-service` via gRPC.
- Valida saldo disponible suficiente en origen.
- Debita origen y acredita destino.
- Registra movimientos locales:
  - `DEBIT / P2P_OUT`
  - `CREDIT / P2P_IN`
- Llama a `party-service` via gRPC para obtener nombre del titular destino.
- Llama a `accounting-service` via gRPC para registrar asiento doble.

Asiento contable enviado:

- Debito: cuenta pasiva del cliente origen.
- Credito: cuenta pasiva del cliente destino.

Errores relevantes:

- `400`: request invalido, cuenta inactiva, saldo insuficiente o cuentas iguales.
- `404`: cuenta no encontrada.
- `409`: `transactionUuid` duplicado.
- `503`: `accounting-service` o `party-service` no disponible via gRPC.

### POST `/api/v2/payments/batch-credit`

Procesa acreditacion masiva On-Us enviada por Switch/Routing.

Consumidor esperado:

- `routing-service`.

Request:

```json
{
  "batchId": "uuid-lote",
  "credits": [
    {
      "accountId": 5,
      "amount": 1000.00,
      "reference": "Nomina mayo",
      "transactionUuid": "uuid-linea-1"
    },
    {
      "accountId": 8,
      "amount": 800.00,
      "reference": "Nomina mayo",
      "transactionUuid": "uuid-linea-2"
    }
  ]
}
```

Respuesta `200 OK`:

```json
{
  "batchId": "uuid-lote",
  "processed": 2,
  "failed": 0,
  "results": [
    {
      "accountId": 5,
      "status": "SUCCESS",
      "transactionId": "uuid-linea-1"
    },
    {
      "accountId": 8,
      "status": "SUCCESS",
      "transactionId": "uuid-linea-2"
    }
  ]
}
```

Hace por cada credito:

- Valida idempotencia por `transactionUuid`.
- Valida que la cuenta exista.
- Valida que la cuenta este `ACTIVE`.
- Valida cliente activo contra `party-service` via gRPC.
- Acredita saldo disponible y contable.
- Registra movimiento local `CREDIT / BATCH_CREDIT`.
- Llama a `accounting-service` via gRPC para registrar asiento contable individual.

Errores relevantes:

- `400`: request invalido o cuenta inactiva.
- `404`: cuenta no encontrada.
- `409`: `transactionUuid` duplicado.
- `503`: `accounting-service` o `party-service` no disponible via gRPC.

### POST `/api/v2/payments/corporate-debit`

Debita la cuenta matriz empresarial para liquidar un lote de pagos masivos.

Consumidor esperado:

- `routing-service`.

Request:

```json
{
  "accountId": 10,
  "totalAmount": 50000.00,
  "commissionAmount": 57.50,
  "batchId": "uuid-lote",
  "transactionUuid": "uuid"
}
```

Respuesta `200 OK`:

```json
{
  "transactionId": "uuid",
  "debitedAmount": 50057.50,
  "commissionNet": 50.00,
  "ivaAmount": 7.50,
  "status": "COMPLETED",
  "accountingDate": "2026-05-30"
}
```

Hace:

- Valida idempotencia por `transactionUuid`.
- Valida que la cuenta exista.
- Valida que la cuenta este `ACTIVE`.
- Valida cliente activo contra `party-service` via gRPC.
- Calcula `debitedAmount = totalAmount + commissionAmount`.
- Calcula IVA y comision neta.
- Valida saldo disponible suficiente.
- Debita saldo disponible y contable.
- Registra movimiento local `DEBIT / CORPORATE_DEBIT`.
- Llama a `accounting-service` via gRPC para registrar asiento contable.

Asiento contable enviado:

- Debito: cuenta pasiva de la empresa.
- Credito: cuenta puente de pagos.
- Credito: `4.1.0.01` ingresos por servicios.
- Credito: `2.2.0.01` IVA por pagar/retenido.

Errores relevantes:

- `400`: request invalido, cuenta inactiva o saldo insuficiente.
- `404`: cuenta no encontrada.
- `409`: `transactionUuid` duplicado.
- `503`: `accounting-service` o `party-service` no disponible via gRPC.

### GET `/api/v2/accounts/health`

Health check del microservicio.

Respuesta `200 OK`:

```json
{
  "status": "UP",
  "service": "account-core-service",
  "version": "2.0"
}
```

## gRPC Consumido Por Oscar

### AccountingService

Proto:

`src/main/proto/accounting_service.proto`

Destino configurable:

| Propiedad | Default |
|---|---:|
| `accounting.grpc.host` | `localhost` |
| `accounting.grpc.port` | `9092` |

Operacion consumida:

```proto
service AccountingService {
  rpc RegisterEntry(AccountingEntryRequest) returns (AccountingEntryResponse);
}
```

Oscar llama a este servicio en:

- Deposito por ventanilla.
- Retiro por ventanilla.
- Transferencia P2P.
- Batch credit.
- Corporate debit.

### PartyService

Proto:

`src/main/proto/party_service.proto`

Destino configurable:

| Propiedad | Default |
|---|---:|
| `party.grpc.host` | `localhost` |
| `party.grpc.port` | `9093` |

Operaciones consumidas:

```proto
service PartyService {
  rpc GetCustomer(GetCustomerRequest) returns (CustomerResponse);
  rpc GetCustomerByAccount(GetCustomerByAccountRequest) returns (AccountHolderResponse);
}
```

Oscar usa `GetCustomer` para validar que el cliente asociado a una cuenta este activo.

Oscar usa `GetCustomerByAccount` para obtener el nombre del titular destino en transferencias P2P.

## gRPC Expuesto Por Oscar

### AccountLookupService

Este contrato es para comunicacion interna core-to-core. Por ejemplo, `party-service` puede usarlo para obtener el `customerId` a partir del numero de cuenta y luego resolver el titular en su propio dominio.

Proto:

`src/main/proto/account_lookup_service.proto`

Puerto configurable:

| Propiedad | Default |
|---|---:|
| `account-core.grpc.port` | `9091` |

Servicio:

```proto
service AccountLookupService {
  rpc GetAccountByNumber(GetAccountByNumberRequest) returns (AccountLookupResponse);
}
```

Request:

```json
{
  "accountNumber": "2200000002"
}
```

Response:

```json
{
  "accountId": 1,
  "accountNumber": "2200000002",
  "customerId": 2,
  "status": "ACTIVA"
}
```

Errores gRPC:

| Status | Caso |
|---|---|
| `INVALID_ARGUMENT` | Numero de cuenta vacio. |
| `NOT_FOUND` | Cuenta no encontrada. |

## Comunicacion Con Switch

Oscar no expone gRPC para Switch/Routing.

Si Switch necesita entregar o consumir informacion de Oscar, debe hacerlo por REST usando:

- `POST /api/v2/payments/batch-credit`
- `POST /api/v2/payments/corporate-debit`

gRPC queda reservado para comunicacion interna entre servicios del core bancario, como contabilidad y party.

## Reglas De Negocio Principales

### Idempotencia

Cada operacion transaccional valida `transactionUuid`.

Si el mismo UUID ya fue usado en las ultimas 24 horas, se rechaza la operacion.

### Estado De Cuenta

Para mover dinero, la cuenta debe estar `ACTIVE`.

Valores legacy como `ACTIVA` o `ACTIVO` pueden leerse desde base por los converters JPA, pero el codigo trabaja con enums en ingles.

### Saldo Disponible

Las operaciones de debito validan `availableBalance`.

Aplican a:

- Retiro por ventanilla.
- Transferencia P2P.
- Corporate debit.

### Concurrencia

Las cuentas se leen con bloqueo pesimista para evitar condiciones de carrera al mover saldos.

### Contabilidad

Oscar no persiste asientos contables.

Oscar solo arma la solicitud y llama a `accounting-service` por gRPC. Si esa llamada falla, la transaccion local se revierte por la transaccion de Spring.

## Configuracion

```properties
server.port=${SERVER_PORT:8081}

accounting.grpc.host=${ACCOUNTING_GRPC_HOST:localhost}
accounting.grpc.port=${ACCOUNTING_GRPC_PORT:9092}

party.grpc.host=${PARTY_GRPC_HOST:localhost}
party.grpc.port=${PARTY_GRPC_PORT:9093}

account-core.grpc.port=${ACCOUNT_CORE_GRPC_PORT:9091}

api.public.base-url=${API_PUBLIC_BASE_URL:/}
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
```

## OpenAPI Y Swagger

Oscar publica documentacion OpenAPI para importar en un API Manager.

URLs locales:

| Uso | URL |
|---|---|
| Swagger UI | `http://localhost:8081/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8081/v3/api-docs` |
| OpenAPI YAML | `http://localhost:8081/v3/api-docs.yaml` |

Para ambientes con API Manager o gateway, configurar `API_PUBLIC_BASE_URL` con la URL publica base que debe aparecer en el contrato OpenAPI.

## Errores Comunes

| Codigo | Caso |
|---:|---|
| `400` | Request invalido, cuenta inactiva, saldo insuficiente o regla de negocio incumplida. |
| `404` | Cuenta no encontrada. |
| `409` | Transaccion duplicada por `transactionUuid`. |
| `503` | Servicio gRPC requerido no disponible. |

## Notas Para Integradores

- Usar `accountId` para operaciones sobre cuentas, excepto P2P destino, que usa `destinationAccountNumber`.
- Usar un `transactionUuid` unico por intento de operacion.
- No reintentar con el mismo `transactionUuid` si se quiere crear una nueva operacion.
- Para comunicacion Switch/Routing -> Oscar usar REST.
- gRPC se usa solo para comunicacion interna entre servicios del core.
