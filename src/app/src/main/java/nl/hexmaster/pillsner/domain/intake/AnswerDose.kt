package nl.hexmaster.pillsner.domain.intake

import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.TransactionRunner
import nl.hexmaster.pillsner.domain.stock.ConsumeStockOnTaken
import nl.hexmaster.pillsner.domain.stock.TakenStockEffect

/** The three answers a user can give about a dose, wherever they give them. */
enum class DoseAnswer { TAKEN, SNOOZE, SKIP }

/**
 * Answering a dose, in one place (design D1).
 *
 * An answer is not one write: the outcome goes in, the notification comes down, and the alarms and
 * the watch are brought back in line with what is now true. That sequence is the whole correctness
 * of an answer, and its value is that it is never half done — so it lives here rather than being
 * copied into every surface that offers the three buttons. The notification receiver and the dose
 * detail screen both call this and nothing else; the next surface that answers a dose will too.
 *
 * @param consumeStockOnTaken what a taken outcome does to the medicine's stock
 *   (`medicine-stock-tracking`). A medicine with no stock batches is left untouched; this is the one
 *   place consumption happens, regardless of which surface answered.
 * @param transactionRunner what makes a taken answer's intake write and its stock deduction one
 *   unit: a dose is never recorded taken without its stock effect, and two surfaces answering the
 *   same dose at once cannot both deduct.
 * @param onAnswered what has to follow every answer, whoever gave it. Supplied by `AppContainer`
 *   because both halves of it — taking the notification down and running a wake — are Android
 *   concerns that a domain use case must not depend on. It is called with the dose as it was
 *   *before* the answer, which is what the notifier needs to identify what to take down.
 */
class AnswerDose(
    private val doseRepository: DoseRepository,
    private val recordIntake: RecordIntake,
    private val snoozeDose: SnoozeDose,
    private val consumeStockOnTaken: ConsumeStockOnTaken,
    private val transactionRunner: TransactionRunner,
    private val onAnswered: suspend (Dose) -> Unit,
) {

    /**
     * Applies [answer] to the dose [id].
     *
     * A dose that is not there, or that someone has already answered from another surface, is left
     * exactly as it is: an answer is not an edit, and the first one wins.
     */
    suspend operator fun invoke(id: DoseId, answer: DoseAnswer) {
        val dose = doseRepository.get(id) ?: return
        if (!dose.isPending) return

        when (answer) {
            DoseAnswer.TAKEN -> if (!recordTaken(id)) return
            DoseAnswer.SKIP -> recordIntake(id, IntakeOutcome.SKIPPED)
            DoseAnswer.SNOOZE -> snoozeDose(id)
        }

        onAnswered(dose)
    }

    /**
     * Records [id] taken and deducts its stock in one transaction, then flags a stock warning once
     * that has committed.
     *
     * @return false when the dose was answered from another surface in the meantime, in which case
     *   nothing was written.
     */
    private suspend fun recordTaken(id: DoseId): Boolean {
        var stockEffect: TakenStockEffect? = null
        val recorded = transactionRunner.inTransaction {
            // Checked again inside the transaction: the check above is only a fast path, and two
            // surfaces answering at once must not both record the intake and both deduct.
            val current = doseRepository.get(id)
            if (current == null || !current.isPending) return@inTransaction false
            recordIntake(id, IntakeOutcome.TAKEN)
            stockEffect = consumeStockOnTaken.deduct(current.medicationId, current.amount)
            true
        }
        stockEffect?.let { consumeStockOnTaken.flagWarning(it) }
        return recorded
    }
}
