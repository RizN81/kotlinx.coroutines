/*
 * Copyright 2016-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("unused")

package kotlinx.coroutines.linearizability

import kotlinx.coroutines.*
import kotlinx.coroutines.internal.*
import org.jetbrains.kotlinx.lincheck.annotations.*
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.paramgen.*
import org.jetbrains.kotlinx.lincheck.verifier.*
import org.jetbrains.kotlinx.lincheck.verifier.quiescent.*
import kotlin.test.*

@Param(name = "value", gen = IntGen::class, conf = "1:5")
internal abstract class LockFreeTaskQueueWithoutRemoveLCStressTest(singleConsumer: Boolean) : VerifierState() {
    @JvmField
    protected val q = LockFreeTaskQueue<Int>(singleConsumer = singleConsumer)

    @Operation
    fun close() = q.close()

    @Operation
    fun addLast(@Param(name = "value") value: Int) = q.addLast(value)

    override fun extractState() = q.map { it } to q.isClosed()

    @Test
    fun testWithoutRemove() = LCStressOptionsDefault().check(this::class)

    @Test
    fun testWithRemoveForQuiescentConsistency() {
        LCStressOptionsDefault()
            .verifier(QuiescentConsistencyVerifier::class.java)
            .actorsPerThread(if (isStressTest) 3 else 2)
            .check(this::class)
    }
}


internal class MCLockFreeTaskQueueWithRemoveLCStressTest : LockFreeTaskQueueWithoutRemoveLCStressTest(singleConsumer = false) {
    @Operation
    fun removeFirstOrNull() = q.removeFirstOrNull()
}

@OpGroupConfig(name = "consumer", nonParallel = true)
internal class SCLockFreeTaskQueueWithRemoveLCStressTest : LockFreeTaskQueueWithoutRemoveLCStressTest(singleConsumer = true) {
    @Operation(group = "consumer")
    fun removeFirstOrNull() = q.removeFirstOrNull()
}