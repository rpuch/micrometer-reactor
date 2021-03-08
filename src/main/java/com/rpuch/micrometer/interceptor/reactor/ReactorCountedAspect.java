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
package com.rpuch.micrometer.interceptor.reactor;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.lang.NonNullApi;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Aspect responsible for intercepting {@link Mono} and {@link Flux} methods annotated with the {@link Counted}
 * annotation and record a few counter metrics about their execution status.
 *
 * @author Ali Dehghani
 * @author Roman Puchkovskiy
 * @see Counted
 */
@Aspect
@NonNullApi
public class ReactorCountedAspect {
    public final String DEFAULT_EXCEPTION_TAG_VALUE = "none";
    public final String RESULT_TAG_FAILURE_VALUE = "failure";
    public final String RESULT_TAG_SUCCESS_VALUE = "success";

    /**
     * The tag name to encapsulate the method execution status.
     */
    private static final String RESULT_TAG = "result";

    /**
     * The tag name to encapsulate the exception thrown by the intercepted method.
     */
    private static final String EXCEPTION_TAG = "exception";

    /**
     * Where we're going register metrics.
     */
    private final MeterRegistry meterRegistry;

    /**
     * A function to produce additional tags for any given join point.
     */
    private final Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinPoint;

    /**
     * Construct a new aspect with the given {@code meterRegistry} along with a default
     * tags provider.
     *
     * @param meterRegistry Where we're going register metrics.
     */
    public ReactorCountedAspect(MeterRegistry meterRegistry) {
        this(meterRegistry, pjp ->
                Tags.of("class", pjp.getStaticPart().getSignature().getDeclaringTypeName(),
                        "method", pjp.getStaticPart().getSignature().getName()));
    }

    /**
     * Constructs a new aspect with the given {@code meterRegistry} and tags provider function.
     *
     * @param meterRegistry        Where we're going register metrics.
     * @param tagsBasedOnJoinPoint A function to generate tags given a join point.
     */
    public ReactorCountedAspect(MeterRegistry meterRegistry, Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinPoint) {
        this.meterRegistry = meterRegistry;
        this.tagsBasedOnJoinPoint = tagsBasedOnJoinPoint;
    }

    @Pointcut("execution(reactor.core.publisher.Mono *..*.*(..))")
    private void returnsMono() {
    }

    @Pointcut("execution(reactor.core.publisher.Flux *..*.*(..))")
    private void returnsFlux() {
    }

    /**
     * Intercept methods annotated with the {@link Counted} annotation and expose a few counters about
     * their execution status. By default, this aspect records both failed and successful attempts. If the
     * {@link Counted#recordFailuresOnly()} is set to {@code true}, then the aspect would record only
     * failed attempts. In case of a failure, the aspect tags the counter with the simple name of the thrown
     * exception.
     *
     * <p>When the annotated method returns a {@link CompletionStage} or any of its subclasses, the counters will be incremented
     * only when the {@link CompletionStage} is completed. If completed exceptionally a failure is recorded, otherwise if
     * {@link Counted#recordFailuresOnly()} is set to {@code false}, a success is recorded.
     *
     * @param pjp     Encapsulates some information about the intercepted area.
     * @param counted The annotation.
     * @return Whatever the intercepted method returns.
     */
    @Around("@annotation(counted) && (returnsMono() || returnsFlux())")
    public Object interceptAndRecord(ProceedingJoinPoint pjp, Counted counted) {

        final Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        final boolean isMono = Mono.class.isAssignableFrom(method.getReturnType());

        if (isMono) {
            return Mono.defer(() -> {
                try {
                    Object invocationResult = pjp.proceed();

                    if (invocationResult instanceof Mono) {
                        Mono<?> mono = (Mono<?>) invocationResult;
                        return mono
                                .doOnSuccess(result -> {
                                    if (!counted.recordFailuresOnly()) {
                                        record(pjp, counted, DEFAULT_EXCEPTION_TAG_VALUE, RESULT_TAG_SUCCESS_VALUE);
                                    }
                                })
                                .doOnError(ex -> record(pjp, counted, ex.getClass().getSimpleName(), RESULT_TAG_FAILURE_VALUE));
                    } else {
                        throw new IllegalStateException("Only Mono is supported, should not be here, got "
                                + invocationResult);
                    }
                } catch (Error e) {
                    throw e;
                } catch (Throwable ex) {
                    record(pjp, counted, ex.getClass().getSimpleName(), RESULT_TAG_FAILURE_VALUE);
                    return Mono.error(ex);
                }
            });
        } else {
            return Flux.defer(() -> {
                try {
                    Object invocationResult = pjp.proceed();

                    if (invocationResult instanceof Flux) {
                        Flux<?> flux = (Flux<?>) invocationResult;
                        return flux
                                .doOnComplete(() -> {
                                    if (!counted.recordFailuresOnly()) {
                                        record(pjp, counted, DEFAULT_EXCEPTION_TAG_VALUE, RESULT_TAG_SUCCESS_VALUE);
                                    }
                                })
                                .doOnError(ex -> record(pjp, counted, ex.getClass().getSimpleName(), RESULT_TAG_FAILURE_VALUE));
                    } else {
                        throw new IllegalStateException("Only Mono is supported, should not be here, got "
                                + invocationResult);
                    }
                } catch (Error e) {
                    throw e;
                } catch (Throwable ex) {
                    record(pjp, counted, ex.getClass().getSimpleName(), RESULT_TAG_FAILURE_VALUE);
                    return Mono.error(ex);
                }
            });
        }
    }

    private void record(ProceedingJoinPoint pjp, Counted counted, String exception, String result) {
        counter(pjp, counted)
                .tag(EXCEPTION_TAG, exception)
                .tag(RESULT_TAG, result)
                .tags(counted.extraTags())
                .register(meterRegistry)
                .increment();
    }

    private Counter.Builder counter(ProceedingJoinPoint pjp, Counted counted) {
        Counter.Builder builder = Counter.builder(counted.value()).tags(tagsBasedOnJoinPoint.apply(pjp));
        String description = counted.description();
        if (!description.isEmpty()) {
            builder.description(description);
        }
        return builder;
    }
}
