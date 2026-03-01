package org.moinex.common

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.moinex.util.Constants
import java.time.LocalDateTime

@Converter(autoApply = false)
class LocalDateTimeStringConverter : AttributeConverter<LocalDateTime, String> {
    override fun convertToDatabaseColumn(attribute: LocalDateTime?): String? {
        return attribute?.format(Constants.DB_DATE_FORMATTER)
    }

    override fun convertToEntityAttribute(dbData: String?): LocalDateTime? {
        return dbData?.let { LocalDateTime.parse(it, Constants.DB_DATE_FORMATTER) }
    }
}
