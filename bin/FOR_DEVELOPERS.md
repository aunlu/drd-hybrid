## DRD developer documentation

DRD is a tool that performs dynamic analysis on target application in order to find potential data races in it. ASM framework is used to instrument classes being loaded with additional instructions to handle various operations in target application with underlying vector clock-based "fasttrack" algorithm. Races are detected and reported on-the-fly to drd_races.log file.

### Modules

- **agent**: core module, responsible for handling operations and detecting races in target app. It uses copies of java.util.concurrent classes for managing internal data structures. These copies are not instrumented by DRD to improve performance. Therefore agent module is loaded via standalone classloader. As far as java.* classes are system classes and can be loaded only by system classloader, their copies's names are prepended with "drd" to hack this restriction.
- **transformer**: performs bytecode instrumentation. Loaded by standalone classloader to avoid possible dependencies conflict with target application. Provides two class file transformers: application transformer (inserts bytecode instructions into target app's code to handle its operations) and system transformer (generates drdjava.util.concurrent classes, instruments all class loaders to make them be able to load these classes, etc.).
- **bootstrap**: loads transformer and agent modules in standalone classloaders and sets both transformers up into JVM. As far as agent module is loaded via standalone class loader, its classes are unavailable for target application and therefore can't be directly referenced in it. To get round this issue *bootstrap* module (that is loaded by java bootstrap classloader) provides facade interfaces, which calls are inserted by transformers into application code. Implementations of these interfaces are substituted from *agent* module on DRD configuration stage.
- **bin**: maven-specific module to assembly distributive

### Handling operations in target application
DRD distinguishes two scopes in target application: Race Detection Scope (RD) and wider Synchronization Detection Scope (SD).

 - if any synchronization operation occur in SD, it is handled (synhronized, hb-contract, fork/join, etc.)
 - any access to non-volatile field in RD is checked against race on that field
 - any access from RD to object, whose type is not from RD is checked against race on that object. It's called "foreign call": someone from RD executes method call on object that does not belong to RD. In this case we consider this method to be read/write (can be specified in config, by default - write) operation on that object.
All classes from RD implement marker Clocked interface.

Operations are handled by VectorClockInterceptor. It obtain corresponding vector clock and merges them or checks against races. Field vector clocks are stored in-place and have name %field%$vc. All other clocks are stored in concurrent hash maps in ClockStorage.

| Clock                             | How they are stored                                     |
|-----------------------------------|---------------------------------------------------------|
| Field data clock                  | In place: myfield$vc                                    |
| Foreign object data clock         | ConcurrentWeakHashMap<Object; DataClock>                |
| Clock for monitors                | ConcurrentWeakHashMap<Object; DataClock>                |
| Clock for volatiles and contracts | ConcurrentHashMap<OurSpecialSyntheticObject; SyncClock> |
| Thread clocks                     | ThreadLocal<ThreadClock>                                |

RD is usually limited only to target application's self code - i.e., all dependencies and java classes are excluded from it as compromise between precision and performance.

Ideally, SD should also be equal to SD, but this would lead to false positives because a lot of synchronization operations would be lost. Here happens-before contracts come.

### Happens-before contracts

Lots of thread-safe structures explicitly declare and guarantee some happens-before relation on calls of their methods. E.g., lock.lock() happens-before lock.unlock() and chm.put(key) happens-before next chm.get(key). DRD provides ability to describe such contracts. Then it detects them dynamically and treat as high-level synchronization events, avoiding processing all little operations inside them, thus winning performance and lowering "noise" ratio. Also describing contracts allows exclude code from SD and make it more narrow.

In order to check if current thread should process operations or it is in contract method and therefore should ignore them, DRD supports a thread-local set of guards. If guard is free, all operations should be processed. If guard is soft locked, current thread is in hb-contract and should process nothing. If guard is hard-locked, current thread is on critical class loading path and nothing should be processed at all because it can cause internal JVM errors or class circularity errors.

So, transformer that instruments application code, stores the flag state at the beginning of each method into local variable (for performance reasons) and instead of each operation generates and if-else branch that checks flag state and handles operation in a proper way - process, skip and execute or simply execute operation.

