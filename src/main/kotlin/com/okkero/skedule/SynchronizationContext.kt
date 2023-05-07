package com.okkero.skedule

/** Represents a synchronization context that a scheduler coroutine is currently in. */
enum class SynchronizationContext {

  /**
   * The coroutine is in synchronous context, and all tasks are scheduled on main server or region thread
   */
  SYNC,
  /**
   * The coroutine is in asynchronous context, and all tasks are scheduled asynchronously to any server threads
   */
  ASYNC
}
