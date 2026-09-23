package nl.hexmaster.pillsner.data.db

import androidx.room.withTransaction
import nl.hexmaster.pillsner.domain.repository.TransactionRunner

/**
 * The wired [TransactionRunner]: one Room transaction. Every DAO call made inside the block joins
 * it, whichever repository makes the call, and Room runs one transaction at a time.
 */
class RoomTransactionRunner(private val database: PillsnerDatabase) : TransactionRunner {
    override suspend fun <R> inTransaction(block: suspend () -> R): R = database.withTransaction(block)
}
