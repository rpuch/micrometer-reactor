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

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static com.rpuch.micrometer.reactor.aop.AspectWrapping.wrapInAspects;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Puchkovskiy
 */
class CountedAspectsInteroperationTests {
    private final MeterRegistry registry = new SimpleMeterRegistry();

    private final CountedAspect standardCountedAspect = new CountedAspect(registry);
    private final ReactorCountedAspect reactorCountedAspect = new ReactorCountedAspect(registry);
    private final NonReactorCountedAspect nonReactorCountedAspect = new NonReactorCountedAspect(registry);

    private final CountedService countedService = new CountedService();

    @Test
    void countedMethodIsCountedTwice_whenBothReactiveAndStandardAspectsAreInContext() {
        CountedService countedServiceProxy = wrapInAspects(countedService, standardCountedAspect, reactorCountedAspect);

        countedServiceProxy.lazyMonoWithSuccess().block();

        double invocationCount = registry.get("lazyMonoWithSuccess").counter().count();
        assertThat(invocationCount).isEqualTo(2);
    }

    @Test
    void countedMonoMethodIsCountedOnce_whenBothReactiveAndNonReactorAspectsAreInContext() {
        CountedService countedServiceProxy = wrapInAspects(countedService,
                nonReactorCountedAspect, reactorCountedAspect);

        countedServiceProxy.lazyMonoWithSuccess().block();

        double invocationCount = registry.get("lazyMonoWithSuccess").counter().count();
        assertThat(invocationCount).isEqualTo(1);
    }

    @Test
    void countedFluxMethodIsCountedOnce_whenBothReactiveAndNonReactorAspectsAreInContext() {
        CountedService countedServiceProxy = wrapInAspects(countedService,
                nonReactorCountedAspect, reactorCountedAspect);

        countedServiceProxy.lazyFluxWithSuccess().blockLast();

        double invocationCount = registry.get("lazyFluxWithSuccess").counter().count();
        assertThat(invocationCount).isEqualTo(1);
    }
}
