# Skedule
![](https://repo.md5lukas.de/api/badge/latest/releases/de/md5lukas/skedule)
![](https://img.shields.io/github/license/Sytm/Skedule)  
Please note this is a fork from the forked [Skedule](https://github.com/BrunoSilvaFreire/Skedule).  
All instructions in this ReadMe are update with new repositories and artifacts.

The objetives of this fork is:
* Don't use the continuation shenanigans of the original and instead control the dispatcher via coroutine context elements

Skedule is a small coroutine library for the [AbstractSchedulers](https://github.com/Sytm/Skedule/tree/master/schedulers)
for Bukkit/Spigot/Folia plugin developers using Kotlin.

Tired of designing complex BukkitRunnables to meet your needs? Do you find yourself in [Callback Hell](http://callbackhell.com/) a tad too often?
Fret no more, for with Kotlin's coroutines and this nifty little utility, you will be scheduling tasks like never before!

## How to use Skedule?

To get an overview of the API take a look at the [KDocs](https://repo.md5lukas.de/javadoc/releases/de/md5lukas/skedule/2.0.0/raw/index.html)

### Asynchronous tasks
We often find ourselves
having to do I/O or query a database, or we might have to do some long and costly operations. In all of
these cases, so as to not block the game thread, we want to schedule an asynchronous task. Skedule supports
this. To schedule any task with Skedule, a SynchronizationContext needs to be provided. If you do not provide
a SynchronizationContext, `ASYNC` is inferred. If you want to schedule synchronous tasks with Skedule, you
need to explicitly pass `SYNC`:
```kotlin
plugin.skedule {
    Bukkit.broadcastMessage("Doing some heavy work off the main thread")
    //Do costly operation
}
```
You can also switch back and forth between sync and async execution:
```kotlin
plugin.skedule {
    Bukkit.broadcastMessage("Doing some heavy work off the main thread")
    //Do costly operation off the main thread
    switchContext(SynchronizationContext.SYNC)
    //Do stuff on the main thread
    switchContext(SynchronizationContext.ASYNC)
    //Do more costly stuff off the main thread
}
```
An alternative way to do the above is to use `withSynchronizationContext()` which avoids context switches if already
in the correct context before and after executing the given block:
```kotlin
plugin.skedule {
  Bukkit.broadcastMessage("Doing some heavy work off the main thread")
  // Do costly operation off the main thread
  withSynchronizationContext(SynchronizationContext.SYNC) {
    // Do stuff on the main thread
  }
  // Do more costly stuff off the main thread
}

```

### Changing the used scheduler from within the coroutine
If for whatever reason you need to change the scheduler mid-execution (perhaps useful if performing various changes to
multiple different regions on Folia) you can do that like this:
```kotlin
plugin.skedule(SynchronizationContext.SYNC) { // This would only make sense if we need to perform stuff sync
  // Do something sync on the global scheduler
  switchScheduler(Schedulers.region(plugin, somewhere))
  // Do something else sync on the region scheduler
}
```

### Deferring context and scheduler changes
By default calls to `switchContext()` and `switchScheduler()` are applied immediately, so a new dispatch of the coroutine
will be performed. Lets say you are currently in the `ASYNC` context and want to do something in 20 ticks from now on the
main thread, you can avoid a double dispatch (`delay()` also redispatches the coroutine) like this:
```kotlin
plugin.skedule {
  // Do something async
  switchContext(SynchronizationContext.SYNC, immediate = false)
  // Both can also be combined at the same time
  switchScheduler(Schedulers.region(plugin, somewhere), immediate = false)
  delay(20 * 50) // Now the coroutine is redispatched
}
```
If the context switches are immediate, the `BukkitContext` and `BukkitDispatcher` both need to be present in the
`CoroutineContext` or else it will fail.
When using the deferred variants this requirement loosens to only requiring the `BukkitContext`.
This will work:
```kotlin
plugin.skedule {
  // Async Skedule dispatcher
  withContext(Dispatchers.IO) {
    // IO dispatcher
    switchContext(SynchronizationContext.SYNC, immediate = false)
  }
  // Sync Skedule dispatcher
}
```
While this will fail:
```kotlin
plugin.skedule {
  // Async Skedule dispatcher
  withContext(Dispatchers.IO) {
    // IO dispatcher
    switchContext(SynchronizationContext.SYNC) // Exception is thrown
  }
}
```

### Important note on using `switchContext()` calls
When using Skedule to switch from an `ASYNC` context into the `SYNC` context, the following code is scheduled to be executed
via the server scheduler implementation. It has to be kept in mind that these tasks are only executed *ONCE* per server tick,
so a lot of switches between `SYNC` and `ASYNC` will have an expensive performance penalty.
This issue does not apply when in an `ASYNC` context and using the built-in `withContext()` like this:
```kotlin
plugin.skedule {
  // We are currently in ASYNC context
  withContext(Dispatchers.IO) {
    // Perform file reads/writes or database operations here
  }
  // Back in the ASYNC context of the Skedule dispatcher with no time loss
}
```
This is because the Skedule dispatcher uses `Dispatchers.Default` behind the scenes for `ASYNC`.
An exception to usage of `Dispatchers.Default` in the `ASYNC` context is when using `delay()`, then the scheduler is
always used instead.

### Delays
To suspend the coroutine for a given amount of time we use the default `delay()` implementation of the
coroutines library. Due to the fact that this function takes milliseconds as time delay, we must convert our timings
to milliseconds. Internally this value is again divided by 50 and submitted to the BukkitScheduler.

The simplest example looks like this:
```kotlin
plugin.skedule(SynchronizationContext.SYNC) {
    delay(40 * 50) // or 2000
    Bukkit.broadcastMessage("Waited 40 ticks or 2 seconds")
}
```
Of course, this isn't very useful, and doesn't really showcase what Skedule is capable of.
So here is a more useful example:
```kotlin
plugin.skedule(SynchronizationContext.SYNC) {
    Bukkit.broadcastMessage("Waited 0 ticks")
    delay(1000)
    Bukkit.broadcastMessage("Waited 20 ticks")
    delay(1000)
    Bukkit.broadcastMessage("Waited 40 ticks")
    delay(1000)
    Bukkit.broadcastMessage("Waited 60 ticks")
}
```
This may look like procedural code that will block the main server thread, but it really isn't.
The extension method `skedule` starts a coroutine. At each of the delay calls the coroutine is suspended,
a task is scheduled, and the rest of the coroutine is set aside for continuation at a later point
(20 game ticks in the future in this case). After this, control is yielded back to the caller (your plugin).
From there, the server carries on doing whatever it was doing, until the 40 ticks have passed, after which
the coroutine will continue until suspended again, or finished.

### A more useful example
A great real-world example of when Skedule would be useful, is when you need a countdown of some sort.
Say you wanted to start a game countdown of 10 seconds, and each second you wanted to display the
remaining time. With Skedule, this is super easy. No need to create an entirely new class that implements
Runnable and uses mutable state to track how many seconds are left. All you have to do, is use a regular
for-loop:
```kotlin
plugin.skedule {
    for (i in 10 downTo 1) {
        Bukkit.broadcastMessage("Time left: $i sec...")
        delay(1000)
    }
    Bukkit.broadcastMessage("Game starts now!")
}
```
This example really shows where Skedule is at its most powerful.

### `CompletableFuture<T>` and `Deferred<T>` interop
When using Java libraries that expects a `CompletableFuture<T>` from you can use the built-in future launcher of Skedule
like this:
```kotlin
return plugin.future {
  // Do something async
  "The heavily computed result" // ^future
}
```
If the coroutines equivalent `Deferred<T>` is required, this can be used instead:
```kotlin
return plugin.async {
  // Do something async
  "The heavily computed result" // ^async
}
```

### withTimeout

Calls to `withTimeout()` and `withTimeoutOrNull()` do not work (for what ever reason). To work around
this you can wrap the code in a normal Dispatcher like this:

```kotlin
plugin.skedule {
  val result = withContext(Dispatchers.Default) {
    withTimeoutOrNull(1000) {
      // Do some computation
      "Computed result"
    }
  }
  // Do something with the computed result
}
```

## Adding the dependencies to your project

```kotlin
repositories {
  maven("https://repo.md5lukas.de/releases")
}

dependencies {
  // Both need to be shadowed separately
  implementation("de.md5lukas:skedule:2.0.0")
  implementation("de.md5lukas:schedulers:1.0.1")
}
```