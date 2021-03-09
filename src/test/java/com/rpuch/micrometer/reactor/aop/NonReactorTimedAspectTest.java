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
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Puchkovskiy
 */
class NonReactorTimedAspectTest {
    private final MeterRegistry registry = new SimpleMeterRegistry();

    private final NonReactorTimedAspect aspect = new NonReactorTimedAspect(registry);

    private final NonReactiveService serviceProxy = AspectWrapping.wrapInAspect(new NonReactiveService(), aspect);

    @Test
    void hasDefaultConstructor() {
        new NonReactorTimedAspect();
    }

    @Test
    void hasMeterRegistryAndFunctionConstructor() {
        new NonReactorTimedAspect(registry, pjp -> emptyList());
    }

    @Test
    void passesThroughCallReturnValue() {
        assertThat(serviceProxy.call()).isEqualTo("ok");
    }

    @Test
    void timesNonReactiveMethodInvocations() {
        serviceProxy.call();

        long invocationCount = registry.get("call").timer().count();
        assertThat(invocationCount).isEqualTo(1);
    }
}