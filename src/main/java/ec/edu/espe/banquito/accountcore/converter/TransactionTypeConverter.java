package ec.edu.espe.banquito.accountcore.converter;

import ec.edu.espe.banquito.accountcore.enums.TransactionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionTypeConverter implements AttributeConverter<TransactionType, String> {

    @Override
    public String convertToDatabaseColumn(TransactionType attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case DEBIT -> "DEBITO";
            case CREDIT -> "CREDITO";
        };
    }

    @Override
    public TransactionType convertToEntityAttribute(String dbData) {
        return TransactionType.fromDatabaseValue(dbData);
    }
}
