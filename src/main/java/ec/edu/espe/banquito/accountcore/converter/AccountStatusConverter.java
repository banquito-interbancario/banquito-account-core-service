package ec.edu.espe.banquito.accountcore.converter;

import ec.edu.espe.banquito.accountcore.enums.AccountStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountStatusConverter implements AttributeConverter<AccountStatus, String> {

    @Override
    public String convertToDatabaseColumn(AccountStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case ACTIVE -> "ACTIVA";
            case INACTIVE -> "INACTIVA";
            case BLOCKED -> "BLOQUEADA";
            case SUSPENDED -> "SUSPENDIDA";
        };
    }

    @Override
    public AccountStatus convertToEntityAttribute(String dbData) {
        return AccountStatus.fromDatabaseValue(dbData);
    }
}
