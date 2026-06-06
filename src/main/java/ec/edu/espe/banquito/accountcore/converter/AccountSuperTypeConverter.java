package ec.edu.espe.banquito.accountcore.converter;

import ec.edu.espe.banquito.accountcore.enums.AccountSuperType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountSuperTypeConverter implements AttributeConverter<AccountSuperType, String> {

    @Override
    public String convertToDatabaseColumn(AccountSuperType attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case SAVINGS -> "AHORROS";
            case CHECKING -> "CORRIENTE";
        };
    }

    @Override
    public AccountSuperType convertToEntityAttribute(String dbData) {
        return AccountSuperType.fromDatabaseValue(dbData);
    }
}
