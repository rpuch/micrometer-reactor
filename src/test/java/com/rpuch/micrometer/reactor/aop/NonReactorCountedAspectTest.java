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
class NonReactorCountedAspectTest {
    private final MeterRegistry registry = new SimpleMeterRegistry();

    private final NonReactorCountedAspect aspect = new NonReactorCountedAspect(registry);

    private final NonReactiveService serviceProxy = AspectWrapping.wrapInAspect(new NonReactiveService(), aspect);

    @Test
    void hasMeterRegistryAndFunctionConstructor() {
        new NonReactorCountedAspect(registry, pjp -> emptyList());
    }

    @Test
    void passesThroughCallReturnValue() {
        assertThat(serviceProxy.call()).isEqualTo("ok");
    }

    @Test
    void countsNonReactiveMethodInvocations() {
        serviceProxy.call();

        double invocationCount = registry.get("call").counter().count();
        assertThat(invocationCount).isEqualTo(1);
    }
}