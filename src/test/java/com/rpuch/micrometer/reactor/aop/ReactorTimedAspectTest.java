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
        timedServiceProxy = wrapWithAspect(new TimedService(exception, error));
    }

    private <T> T wrapWithAspect(T proxiedObject) {
        return AspectWrapping.wrapInAspect(proxiedObject, aspect);
    }

    @Test
    void hasDefaultConstructor() {
        new ReactorTimedAspect();
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
    void cancellationIsTimedViaMono() {
        CancellableSubscriber subscriber = timedServiceProxy.lazyMonoWithSuccess()
                .subscribeWith(new CancellableSubscriber());
        subscriber.cancel();

        long timedCount = registry.get("lazyMonoWithSuccess")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyMonoWithSuccess")
                .tag("exception", "cancellation")
                .tag("extra", "tag")
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
    void cancellationIsTimedViaFlux() {
        CancellableSubscriber subscriber = timedServiceProxy.lazyFluxWithSuccess()
                .subscribeWith(new CancellableSubscriber());
        subscriber.cancel();

        long timedCount = registry.get("lazyFluxWithSuccess")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyFluxWithSuccess")
                .tag("exception", "cancellation")
                .tag("extra", "tag")
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
    void cancellationIsTimedViaMono_longTask() {
        CancellableSubscriber subscriber = timedServiceProxy.lazyMonoWithSuccessLong()
                .subscribeWith(new CancellableSubscriber());
        subscriber.cancel();

        long timedCount = registry.get("lazyMonoWithSuccessLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyMonoWithSuccessLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timedCount).isEqualTo(1);
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
    void cancellationIsTimedViaFlux_longTask() {
        CancellableSubscriber subscriber = timedServiceProxy.lazyFluxWithSuccessLong()
                .subscribeWith(new CancellableSubscriber());
        subscriber.cancel();

        long timedCount = registry.get("lazyFluxWithSuccessLong")
                .tag("class", TimedService.class.getName())
                .tag("method", "lazyFluxWithSuccessLong")
                .tag("extra", "tag")
                .longTaskTimers().size();

        assertThat(timedCount).isEqualTo(1);
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
}