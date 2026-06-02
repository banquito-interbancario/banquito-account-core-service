package ec.edu.espe.banquito.accountcore.grpc;

import ec.edu.espe.banquito.accountcore.exception.AccountNotFoundException;
import ec.edu.espe.banquito.accountcore.exception.DuplicateTransactionException;
import ec.edu.espe.banquito.accountcore.exception.InactiveAccountException;
import ec.edu.espe.banquito.accountcore.exception.InsufficientBalanceException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public final class GrpcExceptionMapper {

    private GrpcExceptionMapper() {
    }

    public static StatusRuntimeException toStatusRuntimeException(Throwable throwable) {
        if (throwable instanceof AccountNotFoundException) {
            return Status.NOT_FOUND.withDescription(throwable.getMessage()).asRuntimeException();
        }
        if (throwable instanceof DuplicateTransactionException) {
            return Status.ALREADY_EXISTS.withDescription(throwable.getMessage()).asRuntimeException();
        }
        if (throwable instanceof InactiveAccountException
                || throwable instanceof InsufficientBalanceException
                || throwable instanceof IllegalArgumentException
                || throwable instanceof IllegalStateException) {
            return Status.FAILED_PRECONDITION.withDescription(throwable.getMessage()).asRuntimeException();
        }
        if (throwable instanceof StatusRuntimeException statusRuntimeException) {
            return statusRuntimeException;
        }
        return Status.INTERNAL.withDescription(throwable.getMessage()).asRuntimeException();
    }
}
