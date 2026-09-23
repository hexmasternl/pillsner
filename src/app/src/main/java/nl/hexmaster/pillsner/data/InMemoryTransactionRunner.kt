package nl.hexmaster.pillsner.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.hexmaster.pillsner.domain.repository.TransactionRunner

/**
 * A [TransactionRunner] for the in-memory repositories that tests and previews use. It serialises
 * blocks the way Room does, but cannot roll anything back, since the in-memory repositories have
 * nothing to roll back to. Not re-entrant: a block must not start another transaction.
 */
class InMemoryTransactionRunner : TransactionRunner {
    private val mutex = Mutex()

    override suspend fun <R> inTransaction(block: suspend () -> R): R = mutex.withLock { block() }
}
