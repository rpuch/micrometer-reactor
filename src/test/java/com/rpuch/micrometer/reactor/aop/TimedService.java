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

import io.micrometer.core.annotation.Timed;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Puchkovskiy
 */
public class TimedService {
    private final RuntimeException exception;
    private final Error error;

    public TimedService() {
        this(new RuntimeException("Oops"), new Error("Oops"));
    }

    public TimedService(RuntimeException exception, Error error) {
        this.exception = exception;
        this.error = error;
    }

    @Timed(value = "lazyMonoWithSuccess", extraTags = {"extra", "tag"})
    public Mono<String> lazyMonoWithSuccess() {
        return Mono.fromCallable(() -> "ok");
    }

    @Timed(value = "lazyMonoWithException", extraTags = {"extra", "tag"})
    public Mono<String> lazyMonoWithException() {
        return Mono.defer(() -> Mono.error(exception));
    }

    @Timed(value = "eagerMonoWithException", extraTags = {"extra", "tag"})
    public Mono<String> eagerMonoWithException() {
        throw exception;
    }

    @Timed(value = "eagerMonoWithError", extraTags = {"extra", "tag"})
    public Mono<String> eagerMonoWithError() {
        throw error;
    }

    @Timed(value = "lazyFluxWithSuccess", extraTags = {"extra", "tag"})
    public Flux<String> lazyFluxWithSuccess() {
        return Flux.defer(() -> Flux.just("ok"));
    }

    @Timed(value = "lazyFluxWithException", extraTags = {"extra", "tag"})
    public Flux<String> lazyFluxWithException() {
        return Flux.defer(() -> Flux.error(exception));
    }

    @Timed(value = "eagerFluxWithException", extraTags = {"extra", "tag"})
    public Flux<String> eagerFluxWithException() {
        throw exception;
    }

    @Timed(value = "eagerFluxWithError", extraTags = {"extra", "tag"})
    public Flux<String> eagerFluxWithError() {
        throw error;
    }

    @Timed(value = "lazyMonoWithSuccessLong", longTask = true, extraTags = {"extra", "tag"})
    public Mono<String> lazyMonoWithSuccessLong() {
        return Mono.fromCallable(() -> "ok");
    }

    @Timed(value = "lazyMonoWithExceptionLong", longTask = true, extraTags = {"extra", "tag"})
    public Mono<String> lazyMonoWithExceptionLong() {
        return Mono.defer(() -> Mono.error(exception));
    }

    @Timed(value = "eagerMonoWithExceptionLong", longTask = true, extraTags = {"extra", "tag"})
    public Mono<String> eagerMonoWithExceptionLong() {
        throw exception;
    }

    @Timed(value = "eagerMonoWithErrorLong", longTask = true, extraTags = {"extra", "tag"})
    public Mono<String> eagerMonoWithErrorLong() {
        throw error;
    }

    @Timed(value = "lazyFluxWithSuccessLong", longTask = true, extraTags = {"extra", "tag"})
    public Flux<String> lazyFluxWithSuccessLong() {
        return Flux.defer(() -> Flux.just("ok"));
    }

    @Timed(value = "lazyFluxWithExceptionLong", longTask = true, extraTags = {"extra", "tag"})
    public Flux<String> lazyFluxWithExceptionLong() {
        return Flux.defer(() -> Flux.error(exception));
    }

    @Timed(value = "eagerFluxWithExceptionLong", longTask = true, extraTags = {"extra", "tag"})
    public Flux<String> eagerFluxWithExceptionLong() {
        throw exception;
    }

    @Timed(value = "eagerFluxWithErrorLong", longTask = true, extraTags = {"extra", "tag"})
    public Flux<String> eagerFluxWithErrorLong() {
        throw error;
    }
}
