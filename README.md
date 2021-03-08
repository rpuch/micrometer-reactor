[![Maven Central](https://img.shields.io/maven-central/v/com.rpuch.micrometer/micrometer-reactor.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.rpuch.micrometer%22%20AND%20a:%22micrometer-reactor%22)
[![Build Status](https://travis-ci.com/rpuch/micrometer-reactor.svg?branch=master)](https://travis-ci.com/rpuch/micrometer-reactor)
[![codecov](https://codecov.io/gh/rpuch/micrometer-reactor/branch/master/graph/badge.svg?token=PGLWPN3N61)](https://codecov.io/gh/rpuch/micrometer-reactor)

# micrometer-reactor #

[Reactor Core](https://github.com/reactor/reactor-core)'s `Mono` and `Flux` support for
[Micrometer](https://github.com/micrometer-metrics/micrometer) `@Timed` and `@Counted` annotations.

## How to use ##

### Maven ###

```xml
<dependency>
  <groupId>com.rpuch.micrometer</groupId>
  <artifactId>micrometer-reactor</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Gradle ###

```
implementation 'com.rpuch.micrometer:micrometer-reactor:1.0.0'
```

### Java config ###

```java
public ReactorTimedAspect reactorTimedAspect(MeterRegistry meterRegistry) {
    return new ReactorTimedAspect(meterRegistry);
}

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