package ec.edu.espe.banquito.accountcore.converter;

import ec.edu.espe.banquito.accountcore.enums.CatalogStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CatalogStatusConverter implements AttributeConverter<CatalogStatus, String> {

    @Override
    public String convertToDatabaseColumn(CatalogStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case ACTIVE -> "ACTIVO";
            case INACTIVE -> "INACTIVO";
        };
    }

    @Override
    public CatalogStatus convertToEntityAttribute(String dbData) {
        return CatalogStatus.fromDatabaseValue(dbData);
    }
}
