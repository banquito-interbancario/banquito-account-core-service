package ec.edu.espe.banquito.accountcore.exception;

import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsDomainExceptionsToExpectedStatuses() {
        var account = handler.handleAccountNotFound(new AccountNotFoundException(1L));
        var favorite = handler.handleFavoriteAccountNotFound(new FavoriteAccountNotFoundException(1L));
        var duplicate = handler.handleDuplicateTransaction(new DuplicateTransactionException("tx-1"));
        var credentials = handler.handleInvalidCredentials(new InvalidCredentialsException());
        var business = handler.handleBusinessException(new InactiveAccountException("2200000001"));

        assertEquals(HttpStatus.NOT_FOUND, account.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, favorite.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, credentials.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, business.getStatusCode());
    }

    @Test
    void returnsFirstValidationMessage() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("request", "amount", "Amount is required")));

        var response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Amount is required", response.getBody().get("error"));
    }

    @Test
    void fallsBackWhenValidationHasNoMessage() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("request", "amount", null)));

        var response = handler.handleValidation(exception);

        assertEquals("Invalid request", response.getBody().get("error"));
    }

    @Test
    void fallsBackWhenValidationHasNoFieldErrors() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        var response = handler.handleValidation(exception);

        assertEquals("Invalid request", response.getBody().get("error"));
    }

    @Test
    void mapsGrpcFailureToServiceUnavailable() {
        var exception = Status.UNAVAILABLE.withDescription("offline").asRuntimeException();

        var response = handler.handleAccountingUnavailable(exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Accounting gRPC service is unavailable", response.getBody().get("error"));
        assertEquals("Status{code=UNAVAILABLE, description=offline, cause=null}", response.getBody().get("details"));
    }
}
