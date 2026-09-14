package nl.hexmaster.pillsner.domain.model

/** Who put the user on this medication. */
enum class Prescriber {
    GENERAL_PRACTITIONER,
    SPECIALIST,
    PHARMACIST,
    SELF,
    OTHER,
}
