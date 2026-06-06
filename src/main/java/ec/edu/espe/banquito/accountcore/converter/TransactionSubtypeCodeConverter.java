package ec.edu.espe.banquito.accountcore.converter;

import ec.edu.espe.banquito.accountcore.enums.TransactionSubtypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionSubtypeCodeConverter implements AttributeConverter<TransactionSubtypeCode, String> {

    @Override
    public String convertToDatabaseColumn(TransactionSubtypeCode attribute) {
        return attribute == null ? null : attribute.databaseCode();
    }

    @Override
    public TransactionSubtypeCode convertToEntityAttribute(String dbData) {
        return TransactionSubtypeCode.fromDatabaseValue(dbData);
    }
}
