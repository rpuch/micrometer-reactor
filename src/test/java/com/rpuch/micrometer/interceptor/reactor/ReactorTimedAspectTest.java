package com.rpuch.micrometer.interceptor.reactor;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Puchkovskiy
 */
class ReactorTimedAspectTest {
    private ReactorTimedAspect aspect;

    private final MeterRegistry registry = new SimpleMeterRegistry();

    private TimedService timedServiceProxy;

    private final RuntimeException exception = new RuntimeException("Oops");
    private final Error error = new Error("Oops");

    @BeforeEach
    void init() {
        aspect = new ReactorTimedAspect(registry);
        timedServiceProxy = wrapWithAspect(new TimedService());
    }

    private <T> T wrapWithAspect(T proxiedObject) {
        return AspectWrapping.wrapInAspect(proxiedObject, aspect);
    }

    @Test
    void valueIsReturnedViaMono() {
        String result = timedServiceProxy.lazyMonoWithSuccess().block();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaMono() {
        assertThatThrownBy(() -> timedServiceProxy.lazyMonoWithException().block())
                .isEqualTo(exception);
    }

    @Test
    void valueIsReturnedViaFlux() {
        String result = timedServiceProxy.lazyFluxWithSuccess().blockFirst();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaFlux() {
        assertThatThrownBy(() -> timedServiceProxy.lazyFluxWithException().blockFirst())
                .isEqualTo(exception);
    }

    @Test
    void invocationIsTimedViaMono() {
        timedServiceProxy.lazyMonoWithSuccess().block();

        long timedCount = registry.get("lazyMonoWithSuccess")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyMonoWithSuccess")
                .tag("extra", "tag")
                .timer().count();

        assertThat(timedCount).isEqualTo(1);
    }

    @Test
    void invocationErrorIsTimedViaMono() {
        assertThatThrownBy(() -> timedServiceProxy.lazyMonoWithException().block())
                .isEqualTo(exception);

        long timedCount = registry.get("lazyMonoWithException")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyMonoWithException")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .timer().count();

        assertThat(timedCount).isEqualTo(1);
    }

    @Test
    void invocationEagerExceptionIsTimedViaMono() {
        assertThatThrownBy(() -> timedServiceProxy.eagerMonoWithException().block())
                .isEqualTo(exception);

        long timedCount = registry.get("eagerMonoWithException")
                .tag("class", TimedService.class.getName())
                .tag("method", "eagerMonoWithException")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .timer().count();

        assertThat(timedCount).isEqualTo(1);
    }

    @Test
    void invocationEagerErrorIsNotTimedViaMono() {
        assertThatThrownBy(() -> timedServiceProxy.eagerMonoWithError().block())
                .hasCause(error);

        assertThatNoMeterIsCreated();
    }

    @Test
    void invocationEagerExceptionIsTimedViaFlux() {
        assertThatThrownBy(() -> timedServiceProxy.eagerFluxWithException().blockLast())
                .isEqualTo(exception);

        long timedCount = registry.get("eagerFluxWithException")
                .tag("class", TimedService.class.getName())
                .tag("method", "eagerFluxWithException")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .timer().count();

        assertThat(timedCount).isEqualTo(1);
    }
    
    @Test
    void invocationEagerErrorIsNotTimedViaFlux() {
        assertThatThrownBy(() -> timedServiceProxy.eagerFluxWithError().blockLast())
                .hasCause(error);

        assertThatNoMeterIsCreated();
    }

    @Test
    void invocationIsTimedViaFlux() {
        timedServiceProxy.lazyFluxWithSuccess().blockLast();

        long timedCount = registry.get("lazyFluxWithSuccess")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyFluxWithSuccess")
                .tag("extra", "tag")
                .timer().count();

        assertThat(timedCount).isEqualTo(1);
    }

    @Test
    void invocationErrorIsTimedViaFlux() {
        assertThatThrownBy(() -> timedServiceProxy.lazyFluxWithException().blockLast())
                .isEqualTo(exception);

        long timedCount = registry.get("lazyFluxWithException")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyFluxWithException")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .timer().count();

        assertThat(timedCount).isEqualTo(1);
    }

    @Test
    void invocationIsNotTimedViaMonoUntilSubscription() {
        timedServiceProxy.lazyMonoWithSuccess();

        assertThatNoMeterIsCreated();
    }

    private void assertThatNoMeterIsCreated() {
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void invocationIsNotTimedViaFluxUntilSubscription() {
        timedServiceProxy.lazyFluxWithSuccess();

        assertThatNoMeterIsCreated();
    }

    @Test
    void valueIsReturnedViaMono_longTask() {
        String result = timedServiceProxy.lazyMonoWithSuccessLong().block();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaMono_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.lazyMonoWithExceptionLong().block())
                .isEqualTo(exception);
    }

    @Test
    void valueIsReturnedViaFlux_longTask() {
        String result = timedServiceProxy.lazyFluxWithSuccessLong().blockFirst();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaFlux_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.lazyFluxWithExceptionLong().blockFirst())
                .isEqualTo(exception);
    }

    @Test
    void invocationIsTimedViaMono_longTask() {
        timedServiceProxy.lazyMonoWithSuccessLong().block();

        double timersCount = registry.get("lazyMonoWithSuccessLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyMonoWithSuccessLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timersCount).isEqualTo(1);
    }

    @Test
    void invocationErrorIsTimedViaMono_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.lazyMonoWithExceptionLong().block())
                .isEqualTo(exception);

        long timersCount = registry.get("lazyMonoWithExceptionLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyMonoWithExceptionLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timersCount).isEqualTo(1);
    }

    @Test
    void invocationEagerExceptionIsTimedViaMono_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.eagerMonoWithExceptionLong().block())
                .isEqualTo(exception);

        long timersCount = registry.get("eagerMonoWithExceptionLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "eagerMonoWithExceptionLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timersCount).isEqualTo(1);
    }

    @Test
    void invocationEagerErrorIsPassedViaMono_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.eagerMonoWithErrorLong().block())
                .hasCause(error);
    }

    @Test
    void invocationIsTimedViaFlux_longTask() {
        timedServiceProxy.lazyFluxWithSuccessLong().blockLast();

        long timersCount = registry.get("lazyFluxWithSuccessLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyFluxWithSuccessLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timersCount).isEqualTo(1);
    }
    
    @Test
    void invocationErrorIsTimedViaFlux_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.lazyFluxWithExceptionLong().blockLast())
                .isEqualTo(exception);

        long timersCount = registry.get("lazyFluxWithExceptionLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyFluxWithExceptionLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timersCount).isEqualTo(1);
    }
    
    @Test
    void invocationEagerExceptionIsTimedViaFlux_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.eagerFluxWithExceptionLong().blockLast())
                .isEqualTo(exception);

        long timersCount = registry.get("eagerFluxWithExceptionLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "eagerFluxWithExceptionLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timersCount).isEqualTo(1);
    }
    
    @Test
    void invocationEagerErrorIsPassedViaFlux_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.eagerFluxWithErrorLong().blockLast())
                .hasCause(error);
    }

    @Test
    void invocationIsNotTimedViaMonoUntilSubscription_longTask() {
        timedServiceProxy.lazyMonoWithSuccessLong();

        assertThatNoMeterIsCreated();
    }

    @Test
    void invocationIsNotTimedViaFluxUntilSubscription_longTask() {
        timedServiceProxy.lazyFluxWithSuccessLong();

        assertThatNoMeterIsCreated();
    }

    @Test
    void classIsAnnotatedWithAspect() {
        assertThat(ReactorTimedAspect.class.getAnnotation(Aspect.class))
                .isNotNull();
    }

    public class TimedService {
        @Timed(value = "lazyMonoWithSuccess", extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithSuccess() {
            return Mono.fromCallable(() -> "ok");
        }

        @Timed(value = "lazyMonoWithException", extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithException() {
            return Mono.defer(() -> Mono.error(exception));
        }

        @Timed(value = "eagerMonoWithException", extraTags = {"extra", "tag"})
        public Mono<String> eagerMonoWithException() {
            throw exception;
        }

        @Timed(value = "eagerMonoWithError", extraTags = {"extra", "tag"})
        public Mono<String> eagerMonoWithError() {
            throw error;
        }

        @Timed(value = "lazyFluxWithSuccess", extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithSuccess() {
            return Flux.defer(() -> Flux.just("ok"));
        }

        @Timed(value = "lazyFluxWithException", extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithException() {
            return Flux.defer(() -> Flux.error(exception));
        }

        @Timed(value = "eagerFluxWithException", extraTags = {"extra", "tag"})
        public Flux<String> eagerFluxWithException() {
            throw exception;
        }

        @Timed(value = "eagerFluxWithError", extraTags = {"extra", "tag"})
        public Flux<String> eagerFluxWithError() {
            throw error;
        }

        @Timed(value = "lazyMonoWithSuccessLong", longTask = true, extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithSuccessLong() {
            return Mono.fromCallable(() -> "ok");
        }

        @Timed(value = "lazyMonoWithExceptionLong", longTask = true, extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithExceptionLong() {
            return Mono.defer(() -> Mono.error(exception));
        }

        @Timed(value = "eagerMonoWithExceptionLong", longTask = true, extraTags = {"extra", "tag"})
        public Mono<String> eagerMonoWithExceptionLong() {
            throw exception;
        }

        @Timed(value = "eagerMonoWithErrorLong", longTask = true, extraTags = {"extra", "tag"})
        public Mono<String> eagerMonoWithErrorLong() {
            throw error;
        }

        @Timed(value = "lazyFluxWithSuccessLong", longTask = true, extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithSuccessLong() {
            return Flux.defer(() -> Flux.just("ok"));
        }

        @Timed(value = "lazyFluxWithExceptionLong", longTask = true, extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithExceptionLong() {
            return Flux.defer(() -> Flux.error(exception));
        }
        
        @Timed(value = "eagerFluxWithExceptionLong", longTask = true, extraTags = {"extra", "tag"})
        public Flux<String> eagerFluxWithExceptionLong() {
            throw exception;
        }

        @Timed(value = "eagerFluxWithErrorLong", longTask = true, extraTags = {"extra", "tag"})
        public Flux<String> eagerFluxWithErrorLong() {
            throw error;
        }
    }
}