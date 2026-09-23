package nl.hexmaster.pillsner.domain.model

import java.math.BigDecimal

/** The units a dose can be expressed in. */
enum class DoseUnit {
    MILLIGRAM,
    GRAM,
    MICROGRAM,
    MILLILITRE,
    TABLET,
    CAPSULE,
    DROP,
    PUFF,
    UNIT,
}

/**
 * Whether stock counted in this unit holds and loses only whole units (`medicine-stock-tracking`'s
 * "Whole-pill units" requirement). True for tablets and capsules only: once any part of a pill is
 * used it cannot go back into the box, so a partly used pill counts as a whole one. Every other
 * unit is either continuous (a weight or a volume) or never split in practice (a drop, a puff), and
 * keeps exact decimal arithmetic.
 */
val DoseUnit.isWholePill: Boolean get() = this == DoseUnit.TABLET || this == DoseUnit.CAPSULE

/**
 * How much of a medication one dose is: an exact decimal amount greater than zero, in a [DoseUnit].
 *
 * [BigDecimal] rather than a floating-point type so that "0.5 tablet" and "2.5 ml" round-trip
 * through the database exactly and never surface as 2.4999. Two quantities are equal when their
 * numeric values and units match, so 1 tablet equals 1.0 tablet; `BigDecimal.equals` alone would
 * say otherwise because it also compares scale.
 *
 * @throws IllegalArgumentException when [value] is zero or negative.
 */
class Quantity(val value: BigDecimal, val unit: DoseUnit) {

    init {
        require(value > BigDecimal.ZERO) { "A dose amount must be greater than zero" }
    }

    /** The value without trailing zeros, which is what equality and display are based on. */
    private val normalizedValue: BigDecimal get() = value.stripTrailingZeros()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Quantity) return false
        return unit == other.unit && value.compareTo(other.value) == 0
    }

    override fun hashCode(): Int = 31 * normalizedValue.hashCode() + unit.hashCode()

    /** Debug only. Never log this: it is part of a medication's data. */
    override fun toString(): String = "Quantity(${normalizedValue.toPlainString()}, $unit)"

    fun copy(value: BigDecimal = this.value, unit: DoseUnit = this.unit) = Quantity(value, unit)

    companion object {
        /** Convenience for tests and defaults: [amount] is parsed as an exact decimal. */
        fun of(amount: String, unit: DoseUnit) = Quantity(BigDecimal(amount), unit)
    }
}
