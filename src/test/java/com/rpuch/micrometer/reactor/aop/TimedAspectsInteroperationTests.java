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

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static com.rpuch.micrometer.reactor.aop.AspectWrapping.wrapInAspects;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Puchkovskiy
 */
class TimedAspectsInteroperationTests {
    private final MeterRegistry registry = new SimpleMeterRegistry();

    private final TimedAspect standardTimedAspect = new TimedAspect(registry);
    private final ReactorTimedAspect reactorTimedAspect = new ReactorTimedAspect(registry);
    private final NonReactorTimedAspect nonReactorTimedAspect = new NonReactorTimedAspect(registry);

    private final TimedService timedService = new TimedService();

    @Test
    void timedMethodIsTimedTwice_whenBothReactiveAndStandardAspectsAreInContext() {
        TimedService timedServiceProxy = wrapInAspects(timedService, standardTimedAspect, reactorTimedAspect);

        timedServiceProxy.lazyMonoWithSuccess().block();

        long invocationCount = registry.get("lazyMonoWithSuccess").timer().count();
        assertThat(invocationCount).isEqualTo(2);
    }

    @Test
    void timedMonoMethodIsTimedOnce_whenBothReactiveAndNonReactorAspectsAreInContext() {
        TimedService timedServiceProxy = wrapInAspects(timedService, nonReactorTimedAspect, reactorTimedAspect);

        timedServiceProxy.lazyMonoWithSuccess().block();

        long invocationCount = registry.get("lazyMonoWithSuccess").timer().count();
        assertThat(invocationCount).isEqualTo(1);
    }

    @Test
    void timedFluxMethodIsTimedOnce_whenBothReactiveAndNonReactorAspectsAreInContext() {
        TimedService timedServiceProxy = wrapInAspects(timedService, nonReactorTimedAspect, reactorTimedAspect);

        timedServiceProxy.lazyFluxWithSuccess().blockLast();

        long invocationCount = registry.get("lazyFluxWithSuccess").timer().count();
        assertThat(invocationCount).isEqualTo(1);
    }
}
