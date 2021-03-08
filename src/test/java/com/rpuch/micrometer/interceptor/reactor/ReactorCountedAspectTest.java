package com.rpuch.micrometer.interceptor.reactor;

import io.micrometer.core.annotation.Counted;
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
class ReactorCountedAspectTest {
    private ReactorCountedAspect aspect;

    private final MeterRegistry registry = new SimpleMeterRegistry();

    private CountedService countedServiceProxy;

    private final RuntimeException exception = new RuntimeException("Oops");
    private final Error error = new Error("Oops");

    @BeforeEach
    void init() {
        aspect = new ReactorCountedAspect(registry);
        countedServiceProxy = wrapWithAspect(new CountedService());
    }

    private <T> T wrapWithAspect(T proxiedObject) {
        return AspectWrapping.wrapInAspect(proxiedObject, aspect);
    }

    @Test
    void valueIsReturnedViaMono() {
        String result = countedServiceProxy.lazyMonoWithSuccess().block();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaMono() {
        assertThatThrownBy(() -> countedServiceProxy.lazyMonoWithException().block())
                .isEqualTo(exception);
    }

    @Test
    void valueIsReturnedViaFlux() {
        String result = countedServiceProxy.lazyFluxWithSuccess().blockFirst();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaFlux() {
        assertThatThrownBy(() -> countedServiceProxy.lazyFluxWithException().blockFirst())
                .isEqualTo(exception);
    }

    @Test
    void invocationIsCountedViaMono() {
        countedServiceProxy.lazyMonoWithSuccess().block();

        double countedCount = registry.get("lazyMonoWithSuccess")
                .tag("class", CountedService.class.getName())
                .tag("method", "lazyMonoWithSuccess")
                .tag("extra", "tag")
                .counter().count();

        assertThat(countedCount).isEqualTo(1);
    }

    @Test
    void invocationErrorIsCountedViaMono() {
        assertThatThrownBy(() -> countedServiceProxy.lazyMonoWithException().block())
                .isEqualTo(exception);

        double countedCount = registry.get("lazyMonoWithException")
                .tag("class", CountedService.class.getName())
                .tag("method", "lazyMonoWithException")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .counter().count();

        assertThat(countedCount).isEqualTo(1);
    }

    @Test
    void invocationEagerExceptionIsCountedViaMono() {
        assertThatThrownBy(() -> countedServiceProxy.eagerMonoWithException().block())
                .isEqualTo(exception);

        double countedCount = registry.get("eagerMonoWithException")
                .tag("class", CountedService.class.getName())
                .tag("method", "eagerMonoWithException")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .counter().count();

        assertThat(countedCount).isEqualTo(1);
    }

    @Test
    void invocationEagerErrorIsNotCountedViaMono() {
        assertThatThrownBy(() -> countedServiceProxy.eagerMonoWithError().block())
                .hasCause(error);

        assertThatNoMeterIsCreated();
    }

    @Test
    void invocationEagerExceptionIsCountedViaFlux() {
        assertThatThrownBy(() -> countedServiceProxy.eagerFluxWithException().blockLast())
                .isEqualTo(exception);

        double countedCount = registry.get("eagerFluxWithException")
                .tag("class", CountedService.class.getName())
                .tag("method", "eagerFluxWithException")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .counter().count();

        assertThat(countedCount).isEqualTo(1);
    }
    
    @Test
    void invocationEagerErrorIsNotCountedViaFlux() {
        assertThatThrownBy(() -> countedServiceProxy.eagerFluxWithError().blockLast())
                .hasCause(error);

        assertThatNoMeterIsCreated();
    }

    @Test
    void invocationIsCountedViaFlux() {
        countedServiceProxy.lazyFluxWithSuccess().blockLast();

        double countedCount = registry.get("lazyFluxWithSuccess")
                .tag("class", CountedService.class.getName())
                .tag("method", "lazyFluxWithSuccess")
                .tag("extra", "tag")
                .counter().count();

        assertThat(countedCount).isEqualTo(1);
    }

    @Test
    void invocationErrorIsCountedViaFlux() {
        assertThatThrownBy(() -> countedServiceProxy.lazyFluxWithException().blockLast())
                .isEqualTo(exception);

        double countedCount = registry.get("lazyFluxWithException")
                .tag("class", CountedService.class.getName())
                .tag("method", "lazyFluxWithException")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .counter().count();

        assertThat(countedCount).isEqualTo(1);
    }

    @Test
    void invocationIsNotCountedViaMonoUntilSubscription() {
        countedServiceProxy.lazyMonoWithSuccess();

        assertThatNoMeterIsCreated();
    }

    private void assertThatNoMeterIsCreated() {
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void invocationIsNotCountedViaFluxUntilSubscription() {
        countedServiceProxy.lazyFluxWithSuccess();

        assertThatNoMeterIsCreated();
    }

    @Test
    void valueIsReturnedViaMono_recordOnlyFailures() {
        String result = countedServiceProxy.lazyMonoWithSuccessRecordOnlyFailures().block();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaMono_recordOnlyFailures() {
        assertThatThrownBy(() -> countedServiceProxy.lazyMonoWithExceptionRecordOnlyFailures().block())
                .isEqualTo(exception);
    }

    @Test
    void valueIsReturnedViaFlux_recordOnlyFailures() {
        String result = countedServiceProxy.lazyFluxWithSuccessRecordOnlyFailures().blockFirst();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void exceptionIsPropagatedViaFlux_recordOnlyFailures() {
        assertThatThrownBy(() -> countedServiceProxy.lazyFluxWithExceptionRecordOnlyFailures().blockFirst())
                .isEqualTo(exception);
    }

    @Test
    void successfulInvocationIsNotCountedViaMono_recordOnlyFailures() {
        countedServiceProxy.lazyMonoWithSuccessRecordOnlyFailures().block();

        assertThatNoMeterIsCreated();
    }

    @Test
    void invocationErrorIsCountedViaMono_recordOnlyFailures() {
        assertThatThrownBy(() -> countedServiceProxy.lazyMonoWithExceptionRecordOnlyFailures().block())
                .isEqualTo(exception);

        double invocationCount = registry.get("lazyMonoWithExceptionRecordOnlyFailures")
                .tag("class", CountedService.class.getName())
                .tag("method", "lazyMonoWithExceptionRecordOnlyFailures")
                .tag("extra", "tag")
                .counter().count();

        assertThat(invocationCount).isEqualTo(1);
    }

    @Test
    void invocationEagerExceptionIsCountedViaMono_recordOnlyFailures() {
        assertThatThrownBy(() -> countedServiceProxy.eagerMonoWithExceptionRecordOnlyFailures().block())
                .isEqualTo(exception);

        double invocationCount = registry.get("eagerMonoWithExceptionRecordOnlyFailures")
                .tag("class", CountedService.class.getName())
                .tag("method", "eagerMonoWithExceptionRecordOnlyFailures")
                .tag("extra", "tag")
                .counter().count();

        assertThat(invocationCount).isEqualTo(1);
    }

    @Test
    void invocationEagerErrorIsPassedViaMono_recordOnlyFailures() {
        assertThatThrownBy(() -> countedServiceProxy.eagerMonoWithErrorRecordOnlyFailures().block())
                .hasCause(error);
    }

    @Test
    void successfulInvocationIsNotCountedViaFlux_recordOnlyFailures() {
        countedServiceProxy.lazyFluxWithSuccessRecordOnlyFailures().blockLast();

        assertThatNoMeterIsCreated();
    }
    
    @Test
    void invocationErrorIsCountedViaFlux_recordOnlyFailures() {
        assertThatThrownBy(() -> countedServiceProxy.lazyFluxWithExceptionRecordOnlyFailures().blockLast())
                .isEqualTo(exception);

        double invocationCount = registry.get("lazyFluxWithExceptionRecordOnlyFailures")
                .tag("class", CountedService.class.getName())
                .tag("method", "lazyFluxWithExceptionRecordOnlyFailures")
                .tag("extra", "tag")
                .counter().count();

        assertThat(invocationCount).isEqualTo(1);
    }
    
    @Test
    void invocationEagerExceptionIsCountedViaFlux_recordOnlyFailures() {
        assertThatThrownBy(() -> countedServiceProxy.eagerFluxWithExceptionRecordOnlyFailures().blockLast())
                .isEqualTo(exception);

        double invocationCount = registry.get("eagerFluxWithExceptionRecordOnlyFailures")
                .tag("class", CountedService.class.getName())
                .tag("method", "eagerFluxWithExceptionRecordOnlyFailures")
                .tag("extra", "tag")
                .counter().count();

        assertThat(invocationCount).isEqualTo(1);
    }
    
    @Test
    void invocationEagerErrorIsPassedViaFlux_recordOnlyFailures() {
        assertThatThrownBy(() -> countedServiceProxy.eagerFluxWithErrorRecordOnlyFailures().blockLast())
                .hasCause(error);
    }

    @Test
    void invocationIsNotCountedViaMonoUntilSubscription_recordOnlyFailures() {
        countedServiceProxy.lazyMonoWithSuccessRecordOnlyFailures();

        assertThatNoMeterIsCreated();
    }

    @Test
    void invocationIsNotCountedViaFluxUntilSubscription_recordOnlyFailures() {
        countedServiceProxy.lazyFluxWithSuccessRecordOnlyFailures();

        assertThatNoMeterIsCreated();
    }

    @Test
    void classIsAnnotatedWithAspect() {
        assertThat(ReactorCountedAspect.class.getAnnotation(Aspect.class))
                .isNotNull();
    }

    public class CountedService {
        @Counted(value = "lazyMonoWithSuccess", extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithSuccess() {
            return Mono.fromCallable(() -> "ok");
        }

        @Counted(value = "lazyMonoWithException", extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithException() {
            return Mono.defer(() -> Mono.error(exception));
        }

        @Counted(value = "eagerMonoWithException", extraTags = {"extra", "tag"})
        public Mono<String> eagerMonoWithException() {
            throw exception;
        }

        @Counted(value = "eagerMonoWithError", extraTags = {"extra", "tag"})
        public Mono<String> eagerMonoWithError() {
            throw error;
        }

        @Counted(value = "lazyFluxWithSuccess", extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithSuccess() {
            return Flux.defer(() -> Flux.just("ok"));
        }

        @Counted(value = "lazyFluxWithException", extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithException() {
            return Flux.defer(() -> Flux.error(exception));
        }

        @Counted(value = "eagerFluxWithException", extraTags = {"extra", "tag"})
        public Flux<String> eagerFluxWithException() {
            throw exception;
        }

        @Counted(value = "eagerFluxWithError", extraTags = {"extra", "tag"})
        public Flux<String> eagerFluxWithError() {
            throw error;
        }

        @Counted(value = "lazyMonoWithSuccessRecordOnlyFailures", recordFailuresOnly = true, extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithSuccessRecordOnlyFailures() {
            return Mono.fromCallable(() -> "ok");
        }

        @Counted(value = "lazyMonoWithExceptionRecordOnlyFailures", recordFailuresOnly = true, extraTags = {"extra", "tag"})
        public Mono<String> lazyMonoWithExceptionRecordOnlyFailures() {
            return Mono.defer(() -> Mono.error(exception));
        }

        @Counted(value = "eagerMonoWithExceptionRecordOnlyFailures", recordFailuresOnly = true, extraTags = {"extra", "tag"})
        public Mono<String> eagerMonoWithExceptionRecordOnlyFailures() {
            throw exception;
        }

        @Counted(value = "eagerMonoWithErrorRecordOnlyFailures", recordFailuresOnly = true, extraTags = {"extra", "tag"})
        public Mono<String> eagerMonoWithErrorRecordOnlyFailures() {
            throw error;
        }

        @Counted(value = "lazyFluxWithSuccessRecordOnlyFailures", recordFailuresOnly = true, extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithSuccessRecordOnlyFailures() {
            return Flux.defer(() -> Flux.just("ok"));
        }

        @Counted(value = "lazyFluxWithExceptionRecordOnlyFailures", recordFailuresOnly = true, extraTags = {"extra", "tag"})
        public Flux<String> lazyFluxWithExceptionRecordOnlyFailures() {
            return Flux.defer(() -> Flux.error(exception));
        }
        
        @Counted(value = "eagerFluxWithExceptionRecordOnlyFailures", recordFailuresOnly = true, extraTags = {"extra", "tag"})
        public Flux<String> eagerFluxWithExceptionRecordOnlyFailures() {
            throw exception;
        }

        @Counted(value = "eagerFluxWithErrorRecordOnlyFailures", recordFailuresOnly = true, extraTags = {"extra", "tag"})
        public Flux<String> eagerFluxWithErrorRecordOnlyFailures() {
            throw error;
        }
    }
}