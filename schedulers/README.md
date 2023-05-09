# Schedulers
![](https://repo.md5lukas.de/api/badge/latest/releases/de/md5lukas/schedulers)

Abstraction layer on top of the schedulers from Bukkit (therefore also Spigot and Paper) and Folia
to make it easier creating plugins targeting both platforms.

## Usage

First you must obtain an `AbstractScheduler` instance.

```kotlin
Schedulers.global(plugin) // For general purpose
Schedulers.region(plugin, location) // When the scheduler is used for a specific location
Schedulers.entity(plugin, entity) // When the scheduler is used for a specific entity
```

On that instance we can call the various schedule functions.

To view all available methods take a look at the [KDocs](https://repo.md5lukas.de/javadoc/releases/de/md5lukas/schedulers/1.0.0/raw/index.html)
```kotlin
lateinit var scheduler: AbstractScheduler

scheduler.schedule {
  // Do stuff sync
}

scheduler.scheduleAsync {
  // Do stuff async
}
```

The created instances of the `AbstractScheduler` can be stored in some variable or the calls to
the schedule functions be directly chained.

### Executors

When using `CompletableFuture`s in your project, you might want to do something in the "main" server thread
(depending on server implementation) with the result, but for that you need to provide an executor.

```kotlin
lateinit var scheduler: AbstractScheduler

// The returned executors are async by default
val executor = scheduler.asExecutor(async = false)
val future: CompletableFuture<String> = doSomethingExpensiveAsync()

// Then accept async is called that way because the execution of the consumer is
// asynchronous to the thread that calculated the result, not related to Bukkit sync/async
future.thenAcceptAsync(executor) { string -> 
  // Do something on the "main" thread with the string
}
```


### Adding the dependency to your project

```kotlin
repositories {
  maven("https://repo.md5lukas.de/releases")
}

dependencies {
  implementation("de.md5lukas:schedulers:1.0.0")
}
```