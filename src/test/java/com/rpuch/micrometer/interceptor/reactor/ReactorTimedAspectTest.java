package com.rpuch.micrometer.interceptor.reactor;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
        assertThatThrownBy(() -> timedServiceProxy.lazyMonoWithError().block())
                .isEqualTo(exception);
    }

    @Test
    void valueIsReturnedViaFlux() {
        String result = timedServiceProxy.lazyFluxWithSuccess().blockFirst();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaFlux() {
        assertThatThrownBy(() -> timedServiceProxy.lazyFluxWithError().blockFirst())
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
        assertThatThrownBy(() -> timedServiceProxy.lazyMonoWithError().block())
                .isEqualTo(exception);

        long timedCount = registry.get("lazyMonoWithError")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyMonoWithError")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .timer().count();

        assertThat(timedCount).isEqualTo(1);
    }

    @Test
    void invocationEagerExceptionIsTimedViaMono() {
        assertThatThrownBy(() -> timedServiceProxy.eagerMonoWithError().block())
                .isEqualTo(exception);

        long timedCount = registry.get("eagerMonoWithError")
                .tag("class", TimedService.class.getName())
                .tag("method", "eagerMonoWithError")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .timer().count();

        assertThat(timedCount).isEqualTo(1);
    }

    @Test
    void invocationEagerExceptionIsTimedViaFlux() {
        assertThatThrownBy(() -> timedServiceProxy.eagerFluxWithError().blockLast())
                .isEqualTo(exception);

        long timedCount = registry.get("eagerFluxWithError")
                .tag("class", TimedService.class.getName())
                .tag("method", "eagerFluxWithError")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .timer().count();

        assertThat(timedCount).isEqualTo(1);
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
        assertThatThrownBy(() -> timedServiceProxy.lazyFluxWithError().blockLast())
                .isEqualTo(exception);

        long timedCount = registry.get("lazyFluxWithError")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyFluxWithError")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .timer().count();

        assertThat(timedCount).isEqualTo(1);
    }

    @Test
    void invocationIsNotTimedViaMonoUntilSubscription() {
        timedServiceProxy.lazyMonoWithSuccess();

        assertThatThrownBy(() -> registry.get("lazyMonoWithSuccess").meter())
                .isInstanceOf(MeterNotFoundException.class);
    }

    @Test
    void invocationIsNotTimedViaFluxUntilSubscription() {
        timedServiceProxy.lazyFluxWithSuccess();

        assertThatThrownBy(() -> registry.get("lazyFluxWithSuccess").meter())
                .isInstanceOf(MeterNotFoundException.class);
    }

    @Test
    void valueIsReturnedViaMono_longTask() {
        String result = timedServiceProxy.lazyMonoWithSuccessLong().block();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaMono_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.lazyMonoWithErrorLong().block())
                .isEqualTo(exception);
    }

    @Test
    void valueIsReturnedViaFlux_longTask() {
        String result = timedServiceProxy.lazyFluxWithSuccessLong().blockFirst();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaFlux_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.lazyFluxWithErrorLong().blockFirst())
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
        assertThatThrownBy(() -> timedServiceProxy.lazyMonoWithErrorLong().block())
                .isEqualTo(exception);

        long timersCount = registry.get("lazyMonoWithErrorLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyMonoWithErrorLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timersCount).isEqualTo(1);
    }

    @Test
    void invocationEagerExceptionIsTimedViaMono_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.eagerMonoWithErrorLong().block())
                .isEqualTo(exception);

        long timersCount = registry.get("eagerMonoWithErrorLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "eagerMonoWithErrorLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timersCount).isEqualTo(1);
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
        assertThatThrownBy(() -> timedServiceProxy.lazyFluxWithErrorLong().blockLast())
                .isEqualTo(exception);

        long timersCount = registry.get("lazyFluxWithErrorLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyFluxWithErrorLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timersCount).isEqualTo(1);
    }
    
    @Test
    void invocationEagerExceptionIsTimedViaFlux_longTask() {
        assertThatThrownBy(() -> timedServiceProxy.eagerFluxWithErrorLong().blockLast())
                .isEqualTo(exception);

        long timersCount = registry.get("eagerFluxWithErrorLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "eagerFluxWithErrorLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timersCount).isEqualTo(1);
    }

    @Test
    void invocationIsNotTimedViaMonoUntilSubscription_longTask() {
        timedServiceProxy.lazyMonoWithSuccessLong();

        assertThatThrownBy(() -> registry.get("lazyMonoWithSuccessLong").meter())
                .isInstanceOf(MeterNotFoundException.class);
    }

    @Test
    void invocationIsNotTimedViaFluxUntilSubscription_longTask() {
        timedServiceProxy.lazyFluxWithSuccessLong();

        assertThatThrownBy(() -> registry.get("lazyFluxWithSuccessLong").meter())
                .isInstanceOf(MeterNotFoundException.class);
    }

    public class TimedService {
        @Timed(value = "lazyMonoWithSuccess", extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithSuccess() {
            return Mono.fromCallable(() -> "ok");
        }

        @Timed(value = "lazyMonoWithError", extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithError() {
            return Mono.defer(() -> Mono.error(exception));
        }

        @Timed(value = "eagerMonoWithError", extraTags = {"extra", "tag"})
        public Mono<String> eagerMonoWithError() {
            throw exception;
        }

        @Timed(value = "lazyFluxWithSuccess", extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithSuccess() {
            return Flux.defer(() -> Flux.just("ok"));
        }

        @Timed(value = "lazyFluxWithError", extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithError() {
            return Flux.defer(() -> Flux.error(exception));
        }

        @Timed(value = "eagerFluxWithError", extraTags = {"extra", "tag"})
        public Flux<String> eagerFluxWithError() {
            throw exception;
        }

        @Timed(value = "lazyMonoWithSuccessLong", longTask = true, extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithSuccessLong() {
            return Mono.fromCallable(() -> "ok");
        }

        @Timed(value = "lazyMonoWithErrorLong", longTask = true, extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithErrorLong() {
            return Mono.defer(() -> Mono.error(exception));
        }

        @Timed(value = "eagerMonoWithErrorLong", longTask = true, extraTags = {"extra", "tag"})
        public Mono<String> eagerMonoWithErrorLong() {
            throw exception;
        }

        @Timed(value = "lazyFluxWithSuccessLong", longTask = true, extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithSuccessLong() {
            return Flux.defer(() -> Flux.just("ok"));
        }

        @Timed(value = "lazyFluxWithErrorLong", longTask = true, extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithErrorLong() {
            return Flux.defer(() -> Flux.error(exception));
        }
        
        @Timed(value = "eagerFluxWithErrorLong", longTask = true, extraTags = {"extra", "tag"})
        public Flux<String> eagerFluxWithErrorLong() {
            throw exception;
        }
    }
}