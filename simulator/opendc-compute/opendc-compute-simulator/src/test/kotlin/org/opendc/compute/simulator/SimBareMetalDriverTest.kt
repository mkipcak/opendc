/*
 * Copyright (c) 2020 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.compute.simulator

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.compute.core.ServerEvent
import org.opendc.compute.core.ServerState
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimFlopsWorkload
import org.opendc.simulator.utils.DelayControllerClockAdapter
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
internal class SimBareMetalDriverTest {
    private lateinit var machineModel: SimMachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 4)

        machineModel = SimMachineModel(
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 2000.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    @Test
    fun testFlopsWorkload() {
        val testScope = TestCoroutineScope()
        val clock = DelayControllerClockAdapter(testScope)

        var finalState: ServerState = ServerState.BUILD
        var finalTime = 0L

        testScope.launch {
            val driver = SimBareMetalDriver(this, clock, UUID.randomUUID(), "test", emptyMap(), machineModel)
            val image = SimWorkloadImage(UUID.randomUUID(), "<unnamed>", emptyMap(), SimFlopsWorkload(4_000, 2, utilization = 1.0))

            // Batch driver commands
            withContext(coroutineContext) {
                driver.init()
                driver.setImage(image)
                val server = driver.start().server!!
                server.events.collect { event ->
                    when (event) {
                        is ServerEvent.StateChanged -> {
                            finalState = event.server.state
                            finalTime = clock.millis()
                        }
                    }
                }
            }
        }

        testScope.advanceUntilIdle()
        assertEquals(ServerState.SHUTOFF, finalState)
        assertEquals(1001, finalTime)
    }
}
