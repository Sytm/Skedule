package com.okkero.skedule

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.MockPlugin
import be.seeseemelk.mockbukkit.ServerMock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Test

class SchedulerCoroutineTest {

  private lateinit var workerThread: Thread

  private lateinit var server: ServerMock
  private lateinit var plugin: MockPlugin

  @Before
  fun setup() {
    workerThread = Thread.currentThread()

    server = MockBukkit.mock()
    plugin = MockBukkit.createMockPlugin()
  }

  @After
  fun tearDown() {
    MockBukkit.unmock()
  }

  @Test
  fun `coroutine is redispatched on yield`() = runBlocking {
    val calls = AtomicInteger()

    plugin.schedule(SynchronizationContext.SYNC) {
      calls.incrementAndGet()
      yield()
      calls.incrementAndGet()
    }

    assertEquals(0, calls.get())
    server.scheduler.performOneTick()
    assertEquals(1, calls.get())
    server.scheduler.performOneTick()
    assertEquals(2, calls.get())
  }

  @Test
  fun `coroutine is dispatched using runTaskLater on scheduler on delay`() = runBlocking {
    val calls = AtomicInteger()

    plugin.schedule(SynchronizationContext.SYNC) {
      calls.incrementAndGet()
      delay(200) // 4 Ticks total
      calls.incrementAndGet()
    }

    assertEquals(0, calls.get())
    server.scheduler.performOneTick()
    assertEquals(1, calls.get())
    server.scheduler.performTicks(3)
    assertEquals(1, calls.get())
    server.scheduler.performOneTick()
    assertEquals(2, calls.get())
  }

  @Test
  fun `coroutine is dispatched to different threads`() {
    val threads = AtomicReferenceArray<Thread>(3)

    plugin.schedule(SynchronizationContext.SYNC) {
      threads[0] = Thread.currentThread()
      switchContext(SynchronizationContext.ASYNC)
      threads[1] = Thread.currentThread()
      switchContext(SynchronizationContext.SYNC)
      threads[2] = Thread.currentThread()
    }

    server.scheduler.performOneTick()
    server.scheduler.waitAsyncTasksFinished()
    assertEquals(workerThread, threads[0])
    assertNotEquals(workerThread, threads[1])
    assertNull(threads[2])
    server.scheduler.performOneTick()
    server.scheduler.waitAsyncTasksFinished()
    assertEquals(workerThread, threads[2])
  }

  @Test
  fun `coroutine returns to original synchronization context when using withSynchronizationContext`() {
    val threads = AtomicReferenceArray<Thread>(5)

    plugin.schedule(SynchronizationContext.SYNC) {
      threads[0] = Thread.currentThread()
      withSynchronizationContext(SynchronizationContext.ASYNC) {
        threads[1] = Thread.currentThread()
        switchContext(SynchronizationContext.SYNC)
        threads[2] = Thread.currentThread()
        switchContext(SynchronizationContext.ASYNC)
        threads[3] = Thread.currentThread()
      }
      threads[4] = Thread.currentThread()
    }

    server.scheduler.performOneTick()
    server.scheduler.waitAsyncTasksFinished()
    assertEquals(workerThread, threads[0])
    assertNotEquals(workerThread, threads[1])
    server.scheduler.performOneTick()
    server.scheduler.waitAsyncTasksFinished()
    assertEquals(workerThread, threads[2])
    assertNotEquals(workerThread, threads[3])
    server.scheduler.waitAsyncTasksFinished()
    server.scheduler.performOneTick()
    server.scheduler.waitAsyncTasksFinished()
    assertEquals(workerThread, threads[4])
  }
}
