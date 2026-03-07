/*
 * Filename: YearMonthStringConverter.kt
 * Created on: March 07, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.moinex.util.Constants
import java.time.YearMonth

@Converter(autoApply = false)
class YearMonthStringConverter : AttributeConverter<YearMonth, String> {
    override fun convertToDatabaseColumn(attribute: YearMonth?): String? = attribute?.format(Constants.DB_MONTH_YEAR_FORMATTER)

    override fun convertToEntityAttribute(dbData: String?): YearMonth? =
        dbData?.let {
            YearMonth.parse(it, Constants.DB_MONTH_YEAR_FORMATTER)
        }
}
