package org.moinex.common

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.toRounded(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)

fun BigDecimal.isZero() = this.compareTo(BigDecimal.ZERO) == 0
