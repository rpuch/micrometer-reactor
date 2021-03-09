[![Maven Central](https://img.shields.io/maven-central/v/com.rpuch.micrometer/micrometer-reactor.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.rpuch.micrometer%22%20AND%20a:%22micrometer-reactor%22)
[![Build Status](https://travis-ci.com/rpuch/micrometer-reactor.svg?branch=master)](https://travis-ci.com/rpuch/micrometer-reactor)
[![codecov](https://codecov.io/gh/rpuch/micrometer-reactor/branch/master/graph/badge.svg?token=PGLWPN3N61)](https://codecov.io/gh/rpuch/micrometer-reactor)

# micrometer-reactor #

Support for collecting metrics on [Reactor Core](https://github.com/reactor/reactor-core)'s `Mono` and `Flux` methods
with [Micrometer](https://github.com/micrometer-metrics/micrometer) `@Timed` and `@Counted` annotations.

## Motivation ##

`micrometer-core` contains `TimedAspect` and `CountedAspect` that, being added to Spring context, begin timing and
counting methods annotated with `@Timed` and `@Counted`. But the problem is that their logic is not suitable for
reactive types (like `Mono` and `Flux` from `reactor-core`).

Namely, the standard aspects time/count reactive pipelines *assembly*, not their *execution*. That is, to time
a `Mono` correctly, you need to start the timer on subscription (and not just on method invocation) and stop it
on completion/error (and not just exit from the method).

Also, it would be nice to have ability to time/count subscriptions ending up cancelled.

Enter `micrometer-reactor`!

## How to use ##

### Maven ###

```xml
<dependency>
  <groupId>com.rpuch.micrometer</groupId>
  <artifactId>micrometer-reactor</artifactId>
  <version>1.0.1</version>
</dependency>
```

### Gradle ###

```
implementation 'com.rpuch.micrometer:micrometer-reactor:1.0.1'
```

### Java config ###

```java
@Bean
public ReactorTimedAspect reactorTimedAspect(MeterRegistry meterRegistry) {
    return new ReactorTimedAspect(meterRegistry);
}

@Bean
public ReactorCountedAspect reactorCountedAspect(MeterRegistry meterRegistry) {
    return new ReactorCountedAspect(meterRegistry);
}
```

### Timing ###

```java
public class MyService {
    @Timed("loadMyEntity")
    public Mono<MyEntity> loadMyEntity(long id) {
        // ... load code
    }
}

```
### Counting ###

```java
public class MyService {
    @Counted("loadMyEntity")
    public Mono<MyEntity> loadMyEntity(long id) {
        // ... load code
    }
}
```

## Interoperation with standard `TimedAspect` and `CountedAspect` ##

**There is a caveat**. Imagine that you need to time/count not only reactive methods. You still have some
non-reactive services you would like to time. If you just add standard `micrometer`'s'
`TimedAspect` or `CountedAspect`, you will get non-reactive methods covered, but **reactive methods will be
timed/counted twice**! (because both standard and reactive aspects will be triggered for them).

To solve this problem, extensions of standard aspects were created, namely `NonReactorTimedAspect`
and `NonReactorCountedAspect`. They differ from their standard counterparts in that they ignore invocations of
methods returning `Mono` or `Flux`. Use them instead of `TimedAspect` and `CountedAspect` respectively if you use
the reactive counterparts.
