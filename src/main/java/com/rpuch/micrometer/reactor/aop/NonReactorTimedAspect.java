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
import io.micrometer.core.instrument.Tag;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Extension of {@link TimedAspect} that plays nice with {@link ReactorTimedAspect} when both are defined
 * in same Spring context. This means that a method returning {@link Mono} or {@link Flux} is not timed twice
 * (as it would happen with vanilla TimedAspect).
 *
 * @author Roman Puchkovskiy
 */
public class NonReactorTimedAspect extends TimedAspect {
    public NonReactorTimedAspect() {
        super();
    }

    public NonReactorTimedAspect(MeterRegistry registry) {
        super(registry);
    }

    public NonReactorTimedAspect(MeterRegistry registry,
            Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinPoint) {
        super(registry, tagsBasedOnJoinPoint);
    }

    @Override
    public Object timedMethod(ProceedingJoinPoint pjp) throws Throwable {
        if (isReactorReturnType(pjp)) {
            return pjp.proceed();
        }

        return super.timedMethod(pjp);
    }

    private boolean isReactorReturnType(ProceedingJoinPoint pjp) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        return Mono.class.isAssignableFrom(method.getReturnType())
                || Flux.class.isAssignableFrom(method.getReturnType());
    }
}
