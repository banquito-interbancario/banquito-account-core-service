package ec.edu.espe.banquito.accountcore.grpc;

import ec.edu.espe.banquito.accountcore.dto.BatchCreditReqDTO;
import ec.edu.espe.banquito.accountcore.dto.BatchCreditResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.CorporateDebitReqDTO;
import ec.edu.espe.banquito.accountcore.dto.CorporateDebitResponseDTO;
import ec.edu.espe.banquito.accountcore.grpc.accountcore.AccountCoreServiceGrpc;
import ec.edu.espe.banquito.accountcore.grpc.accountcore.BatchCreditRequest;
import ec.edu.espe.banquito.accountcore.grpc.accountcore.BatchCreditResult;
import ec.edu.espe.banquito.accountcore.grpc.accountcore.CorporateDebitRequest;
import ec.edu.espe.banquito.accountcore.service.AccountTransactionService;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountCoreGrpcService extends AccountCoreServiceGrpc.AccountCoreServiceImplBase {

    private final AccountTransactionService transactionService;

    public AccountCoreGrpcService(AccountTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public void batchCredit(BatchCreditRequest request,
                            StreamObserver<ec.edu.espe.banquito.accountcore.grpc.accountcore.BatchCreditResponse> responseObserver) {
        try {
            BatchCreditReqDTO dto = new BatchCreditReqDTO(
                    request.getBatchId(),
                    request.getCreditsList().stream()
                            .map(credit -> new BatchCreditReqDTO.CreditItemDTO(
                                    credit.getAccountId(),
                                    new BigDecimal(credit.getAmount()),
                                    credit.getReference(),
                                    credit.getTransactionUuid()
                            ))
                            .toList()
            );
            BatchCreditResponseDTO response = transactionService.executeBatchCredit(dto);

            ec.edu.espe.banquito.accountcore.grpc.accountcore.BatchCreditResponse.Builder builder =
                    ec.edu.espe.banquito.accountcore.grpc.accountcore.BatchCreditResponse.newBuilder()
                            .setBatchId(response.batchId())
                            .setProcessed(response.processed())
                            .setFailed(response.failed());

            response.results().stream()
                    .map(this::toGrpcResult)
                    .forEach(builder::addResults);

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception exception) {
            responseObserver.onError(GrpcExceptionMapper.toStatusRuntimeException(exception));
        }
    }

    @Override
    public void corporateDebit(CorporateDebitRequest request,
                               StreamObserver<ec.edu.espe.banquito.accountcore.grpc.accountcore.CorporateDebitResponse> responseObserver) {
        try {
            CorporateDebitReqDTO dto = new CorporateDebitReqDTO(
                    request.getAccountId(),
                    new BigDecimal(request.getTotalAmount()),
                    new BigDecimal(request.getCommissionAmount()),
                    request.getBatchId(),
                    request.getTransactionUuid()
            );
            CorporateDebitResponseDTO response = transactionService.executeCorporateDebit(dto);

            responseObserver.onNext(ec.edu.espe.banquito.accountcore.grpc.accountcore.CorporateDebitResponse.newBuilder()
                    .setTransactionId(response.transactionId())
                    .setDebitedAmount(response.debitedAmount().toPlainString())
                    .setCommissionNet(response.commissionNet().toPlainString())
                    .setIvaAmount(response.ivaAmount().toPlainString())
                    .setStatus(response.status().name())
                    .setAccountingDate(response.accountingDate().toString())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception exception) {
            responseObserver.onError(GrpcExceptionMapper.toStatusRuntimeException(exception));
        }
    }

    private BatchCreditResult toGrpcResult(BatchCreditResponseDTO.BatchCreditResultDTO result) {
        return BatchCreditResult.newBuilder()
                .setAccountId(result.accountId())
                .setStatus(result.status())
                .setTransactionId(result.transactionId())
                .build();
    }
}
