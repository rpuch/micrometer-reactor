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

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.lang.NonNullApi;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

/**
 * AspectJ aspect for intercepting {@link Mono} and {@link Flux} methods annotated with {@link Timed @Timed}.
 *
 * @author David J. M. Karlsen
 * @author Jon Schneider
 * @author Johnny Lim
 * @author Nejc Korasa
 * @author Roman Puchkovskiy
 */
@Aspect
@NonNullApi
public class ReactorTimedAspect {
    public static final String DEFAULT_METRIC_NAME = "method.timed";
    public static final String DEFAULT_EXCEPTION_TAG_VALUE = "none";
    public static final String CANCELLATION_EXCEPTION_TAG_VALUE = "cancellation";

    /**
     * Tag key for an exception.
     */
    public static final String EXCEPTION_TAG = "exception";

    private final MeterRegistry registry;
    private final Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinPoint;

    /**
     * Create a {@code ReactorTimedAspect} instance with {@link Metrics#globalRegistry}.
     */
    public ReactorTimedAspect() {
        this(Metrics.globalRegistry);
    }

    public ReactorTimedAspect(MeterRegistry registry) {
        this(registry, pjp ->
                Tags.of("class", pjp.getStaticPart().getSignature().getDeclaringTypeName(),
                        "method", pjp.getStaticPart().getSignature().getName())
        );
    }

    public ReactorTimedAspect(MeterRegistry registry,
            Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinPoint) {
        this.registry = registry;
        this.tagsBasedOnJoinPoint = tagsBasedOnJoinPoint;
    }

    @Pointcut("execution(reactor.core.publisher.Mono *..*.*(..))")
    private void returnsMono() {
    }

    @Pointcut("execution(reactor.core.publisher.Flux *..*.*(..))")
    private void returnsFlux() {
    }

    @Pointcut("execution (@io.micrometer.core.annotation.Timed * *.*(..))")
    private void timedMethod() {
    }

    @Around("timedMethod() && (returnsMono() || returnsFlux())")
    public Object timedMonoMethod(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Timed timed = method.getAnnotation(Timed.class);
        if (timed == null) {
            method = pjp.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes());
            timed = method.getAnnotation(Timed.class);
        }

        final String metricName = timed.value().isEmpty() ? DEFAULT_METRIC_NAME : timed.value();
        final boolean isMono = Mono.class.isAssignableFrom(method.getReturnType());

        if (isMono) {
            if (!timed.longTask()) {
                return processMonoWithTimer(pjp, timed, metricName);
            } else {
                return processMonoWithLongTaskTimer(pjp, timed, metricName);
            }
        } else {
            if (!timed.longTask()) {
                return processFluxWithTimer(pjp, timed, metricName);
            } else {
                return processFluxWithLongTaskTimer(pjp, timed, metricName);
            }
        }
    }

    private Mono<?> processMonoWithTimer(ProceedingJoinPoint pjp, Timed timed, String metricName) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(registry);

            Object invocationResult;
            try {
                invocationResult = pjp.proceed();
            } catch (Error e) {
                throw e;
            } catch (Throwable ex) {
                record(pjp, timed, metricName, sample, ex.getClass().getSimpleName());
                return Mono.error(ex);
            }

            if (!(invocationResult instanceof Mono)) {
                return Mono.error(new IllegalStateException(
                        "Only Mono is supported, should not be here, got " + invocationResult));
            }

            Mono<?> mono = (Mono<?>) invocationResult;
            return mono.doOnSuccess(result -> recordSuccess(pjp, timed, metricName, sample))
                    .doOnError(throwable -> recordFailure(pjp, timed, metricName, sample, throwable))
                    .doOnCancel(() -> recordCancallation(pjp, timed, metricName, sample));
        });
    }

    private void recordSuccess(ProceedingJoinPoint pjp, Timed timed, String metricName, Timer.Sample sample) {
        record(pjp, timed, metricName, sample, DEFAULT_EXCEPTION_TAG_VALUE);
    }

    private void recordCancallation(ProceedingJoinPoint pjp, Timed timed, String metricName, Timer.Sample sample) {
        record(pjp, timed, metricName, sample, CANCELLATION_EXCEPTION_TAG_VALUE);
    }

    private Flux<?> processFluxWithTimer(ProceedingJoinPoint pjp, Timed timed, String metricName) {
        return Flux.defer(() -> {
            Timer.Sample sample = Timer.start(registry);

            Object invocationResult;
            try {
                invocationResult = pjp.proceed();
            } catch (Error e) {
                throw e;
            } catch (Throwable ex) {
                record(pjp, timed, metricName, sample, ex.getClass().getSimpleName());
                return Mono.error(ex);
            }

            if (!(invocationResult instanceof Flux)) {
                return Flux.error(new IllegalStateException(
                        "Only Flux is supported, should not be here, got " + invocationResult));
            }

            Flux<?> flux = (Flux<?>) invocationResult;
            return flux.doOnComplete(() -> recordSuccess(pjp, timed, metricName, sample))
                    .doOnError(throwable -> recordFailure(pjp, timed, metricName, sample, throwable))
                    .doOnCancel(() -> recordCancallation(pjp, timed, metricName, sample));
        });
    }

    private void recordFailure(ProceedingJoinPoint pjp, Timed timed, String metricName, Timer.Sample sample,
            Throwable throwable) {
        record(pjp, timed, metricName, sample, getExceptionTag(throwable));
    }

    private void record(ProceedingJoinPoint pjp, Timed timed, String metricName, Timer.Sample sample,
            String exceptionClass) {
        try {
            sample.stop(Timer.builder(metricName)
                    .description(timed.description().isEmpty() ? null : timed.description())
                    .tags(timed.extraTags())
                    .tags(EXCEPTION_TAG, exceptionClass)
                    .tags(tagsBasedOnJoinPoint.apply(pjp))
                    .publishPercentileHistogram(timed.histogram())
                    .publishPercentiles(timed.percentiles().length == 0 ? null : timed.percentiles())
                    .register(registry));
        } catch (Exception e) {
            // ignoring on purpose
        }
    }

    private String getExceptionTag(Throwable throwable) {

        if (throwable.getCause() == null) {
            return throwable.getClass().getSimpleName();
        }

        return throwable.getCause().getClass().getSimpleName();
    }

    private Mono<?> processMonoWithLongTaskTimer(ProceedingJoinPoint pjp, Timed timed, String metricName) {
        return Mono.defer(() -> {
            Optional<LongTaskTimer.Sample> sample = buildLongTaskTimer(pjp, timed, metricName)
                    .map(LongTaskTimer::start);

            Object invocationResult;
            try {
                invocationResult = pjp.proceed();
            } catch (Error e) {
                throw e;
            } catch (Throwable ex) {
                sample.ifPresent(this::stopTimer);
                return Mono.error(ex);
            }

            if (!(invocationResult instanceof Mono)) {
                return Mono.error(new IllegalStateException(
                        "Only Mono is supported, should not be here, got " + invocationResult));
            }

            Mono<?> mono = (Mono<?>) invocationResult;
            return mono.doFinally(signalType -> sample.ifPresent(this::stopTimer));
        });
    }

    private Flux<?> processFluxWithLongTaskTimer(ProceedingJoinPoint pjp, Timed timed, String metricName) {
        return Flux.defer(() -> {
            Optional<LongTaskTimer.Sample> sample = buildLongTaskTimer(pjp, timed, metricName)
                    .map(LongTaskTimer::start);

            Object invocationResult;
            try {
                invocationResult = pjp.proceed();
            } catch (Error e) {
                throw e;
            } catch (Throwable ex) {
                sample.ifPresent(this::stopTimer);
                return Mono.error(ex);
            }

            if (!(invocationResult instanceof Flux)) {
                return Flux.error(new IllegalStateException(
                        "Only Flux is supported, should not be here, got " + invocationResult));
            }

            Flux<?> flux = (Flux<?>) invocationResult;
            return flux.doFinally(signalType -> sample.ifPresent(this::stopTimer));
        });
    }

    private void stopTimer(LongTaskTimer.Sample sample) {
        try {
            sample.stop();
        } catch (Exception e) {
            // ignoring on purpose
        }
    }

    /**
     * Secure long task timer creation - it should not disrupt the application flow in case of exception
     */
    private Optional<LongTaskTimer> buildLongTaskTimer(ProceedingJoinPoint pjp, Timed timed, String metricName) {
        try {
            return Optional.of(LongTaskTimer.builder(metricName)
                                       .description(timed.description().isEmpty() ? null : timed.description())
                                       .tags(timed.extraTags())
                                       .tags(tagsBasedOnJoinPoint.apply(pjp))
                                       .register(registry));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
