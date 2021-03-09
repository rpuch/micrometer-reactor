/*
 * Copyright 2021 micrometer-reactor contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rpuch.micrometer.reactor.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

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
        countedServiceProxy = wrapWithAspect(new CountedService(exception, error));
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
                .tag("result", "success")
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
                .tag("result", "failure")
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
                .tag("result", "failure")
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
    void cancellationIsCountedViaMono() {
        CancellableSubscriber subscriber = countedServiceProxy.lazyMonoWithSuccess()
                .subscribeWith(new CancellableSubscriber());
        subscriber.cancel();

        double countedCount = registry.get("lazyMonoWithSuccess")
                .tag("class", CountedService.class.getName())
                .tag("method", "lazyMonoWithSuccess")
                .tag("result", "cancellation")
                .tag("extra", "tag")
                .counter().count();

        assertThat(countedCount).isEqualTo(1);
    }

    @Test
    void invocationEagerExceptionIsCountedViaFlux() {
        assertThatThrownBy(() -> countedServiceProxy.eagerFluxWithException().blockLast())
                .isEqualTo(exception);

        double countedCount = registry.get("eagerFluxWithException")
                .tag("class", CountedService.class.getName())
                .tag("method", "eagerFluxWithException")
                .tag("result", "failure")
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
                .tag("result", "success")
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
                .tag("result", "failure")
                .tag("extra", "tag")
                .tag("exception", "RuntimeException")
                .counter().count();

        assertThat(countedCount).isEqualTo(1);
    }
    
    @Test
    void cancellationIsCountedViaFlux() {
        CancellableSubscriber subscriber = countedServiceProxy.lazyFluxWithSuccess()
                .subscribeWith(new CancellableSubscriber());
        subscriber.cancel();

        double countedCount = registry.get("lazyFluxWithSuccess")
                .tag("class", CountedService.class.getName())
                .tag("method", "lazyFluxWithSuccess")
                .tag("result", "cancellation")
                .tag("extra", "tag")
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
    void cancellationIsNotCountedViaMono_recordOnlyFailures() {
        Disposable disposable = countedServiceProxy.lazyMonoWithSuccessRecordOnlyFailures().subscribe();
        disposable.dispose();

        assertThatNoMeterIsCreated();
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
                .tag("result", "failure")
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
                .tag("result", "failure")
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
                .tag("result", "failure")
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
                .tag("result", "failure")
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
    void cancellationIsNotCountedViaFlux_recordOnlyFailures() {
        Disposable disposable = countedServiceProxy.lazyFluxWithSuccessRecordOnlyFailures().subscribe();
        disposable.dispose();

        assertThatNoMeterIsCreated();
    }

    @Test
    void classIsAnnotatedWithAspect() {
        assertThat(ReactorCountedAspect.class.getAnnotation(Aspect.class))
                .isNotNull();
    }

}