package org.moinex.common.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.moinex.util.Constants
import java.time.LocalDate

@Converter(autoApply = false)
class LocalDateStringConverter : AttributeConverter<LocalDate, String> {
    override fun convertToDatabaseColumn(attribute: LocalDate?): String? = attribute?.format(Constants.DATE_FORMATTER_NO_TIME)

    override fun convertToEntityAttribute(dbData: String?): LocalDate? =
        dbData?.let {
            LocalDate.parse(it, Constants.DATE_FORMATTER_NO_TIME)
        }
}
