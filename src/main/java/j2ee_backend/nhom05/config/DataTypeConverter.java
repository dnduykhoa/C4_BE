package j2ee_backend.nhom05.config;

import j2ee_backend.nhom05.model.AttributeDefinition.DataType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts DataType enum to/from the database, handling case-insensitively.
 * This handles legacy data stored with lowercase values (e.g., "string" → STRING).
 */
@Converter
public class DataTypeConverter implements AttributeConverter<DataType, String> {

    @Override
    public String convertToDatabaseColumn(DataType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public DataType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return DataType.valueOf(dbData.toUpperCase());
    }
}
