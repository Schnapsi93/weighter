package com.aljaz.weighttracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for [WeightParser]. */
class WeightParserTest {

    @Test
    fun parse_commaDecimal_isValid() {
        assertEquals(WeightParseResult.Valid(79.5), WeightParser.parse("79,5"))
    }

    @Test
    fun parse_dotDecimal_isValid() {
        assertEquals(WeightParseResult.Valid(79.5), WeightParser.parse("79.5"))
    }

    @Test
    fun parse_surroundingSpaces_areIgnored() {
        assertEquals(WeightParseResult.Valid(80.0), WeightParser.parse(" 80 "))
    }

    @Test
    fun parse_empty_returnsEmptyError() {
        assertEquals(
            WeightParseResult.Invalid(WeightInputError.EMPTY),
            WeightParser.parse(""),
        )
    }

    @Test
    fun parse_onlySeparator_returnsNotANumber() {
        assertEquals(
            WeightParseResult.Invalid(WeightInputError.NOT_A_NUMBER),
            WeightParser.parse(","),
        )
    }

    @Test
    fun parse_tooLow_returnsOutOfRange() {
        assertEquals(
            WeightParseResult.Invalid(WeightInputError.OUT_OF_RANGE),
            WeightParser.parse("5"),
        )
    }

    @Test
    fun parse_tooHigh_returnsOutOfRange() {
        assertEquals(
            WeightParseResult.Invalid(WeightInputError.OUT_OF_RANGE),
            WeightParser.parse("450"),
        )
    }

    @Test
    fun parse_moreThanTwoDecimals_isRounded() {
        assertEquals(WeightParseResult.Valid(79.46), WeightParser.parse("79.456"))
    }
}