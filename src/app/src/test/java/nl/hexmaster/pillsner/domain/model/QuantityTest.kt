package nl.hexmaster.pillsner.domain.model

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Spec: Dose quantity. */
class QuantityTest {

    @Test
    fun aDecimalAmount_isHeldExactly() {
        val quantity = Quantity.of("2.5", DoseUnit.MILLILITRE)

        assertEquals(BigDecimal("2.5"), quantity.value)
        assertEquals(DoseUnit.MILLILITRE, quantity.unit)
    }

    @Test
    fun zero_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Quantity.of("0", DoseUnit.MILLIGRAM)
        }
    }

    @Test
    fun aNegativeAmount_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Quantity.of("-1", DoseUnit.MILLIGRAM)
        }
    }

    @Test
    fun equalityIsByNumericValue_notByScale() {
        assertEquals(Quantity.of("1", DoseUnit.TABLET), Quantity.of("1.0", DoseUnit.TABLET))
        assertEquals(
            Quantity.of("1", DoseUnit.TABLET).hashCode(),
            Quantity.of("1.0", DoseUnit.TABLET).hashCode(),
        )
    }

    @Test
    fun theUnitIsPartOfEquality() {
        assertNotEquals(Quantity.of("1", DoseUnit.TABLET), Quantity.of("1", DoseUnit.CAPSULE))
    }
}
