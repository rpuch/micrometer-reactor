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

import io.micrometer.core.annotation.Counted;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Puchkovskiy
 */
public class CountedService {
    private final RuntimeException exception;
    private final Error error;

    public CountedService() {
        this(new RuntimeException("Oops"), new Error("Oops"));
    }

    public CountedService(RuntimeException exception, Error error) {
        this.exception = exception;
        this.error = error;
    }

    @Counted(value = "lazyMonoWithSuccess", extraTags = {"extra", "tag"})
    public Mono<String> lazyMonoWithSuccess() {
        return Mono.fromCallable(() -> "ok");
    }

    @Counted(value = "lazyMonoWithException", extraTags = {"extra", "tag"})
    public Mono<String> lazyMonoWithException() {
        return Mono.defer(() -> Mono.error(exception));
    }

    @Counted(value = "eagerMonoWithException", extraTags = {"extra", "tag"})
    public Mono<String> eagerMonoWithException() {
        throw exception;
    }

    @Counted(value = "eagerMonoWithError", extraTags = {"extra", "tag"})
    public Mono<String> eagerMonoWithError() {
        throw error;
    }

    @Counted(value = "lazyFluxWithSuccess", extraTags = {"extra", "tag"})
    public Flux<String> lazyFluxWithSuccess() {
        return Flux.defer(() -> Flux.just("ok"));
    }

    @Counted(value = "lazyFluxWithException", extraTags = {"extra", "tag"})
    public Flux<String> lazyFluxWithException() {
        return Flux.defer(() -> Flux.error(exception));
    }

    @Counted(value = "eagerFluxWithException", extraTags = {"extra", "tag"})
    public Flux<String> eagerFluxWithException() {
        throw exception;
    }

    @Counted(value = "eagerFluxWithError", extraTags = {"extra", "tag"})
    public Flux<String> eagerFluxWithError() {
        throw error;
    }

    @Counted(value = "lazyMonoWithSuccessRecordOnlyFailures", recordFailuresOnly = true,
            extraTags = {"extra", "tag"})
    public Mono<String> lazyMonoWithSuccessRecordOnlyFailures() {
        return Mono.fromCallable(() -> "ok");
    }

    @Counted(value = "lazyMonoWithExceptionRecordOnlyFailures", recordFailuresOnly = true,
            extraTags = {"extra", "tag"})
    public Mono<String> lazyMonoWithExceptionRecordOnlyFailures() {
        return Mono.defer(() -> Mono.error(exception));
    }

    @Counted(value = "eagerMonoWithExceptionRecordOnlyFailures", recordFailuresOnly = true,
            extraTags = {"extra", "tag"})
    public Mono<String> eagerMonoWithExceptionRecordOnlyFailures() {
        throw exception;
    }

    @Counted(value = "eagerMonoWithErrorRecordOnlyFailures", recordFailuresOnly = true,
            extraTags = {"extra", "tag"})
    public Mono<String> eagerMonoWithErrorRecordOnlyFailures() {
        throw error;
    }

    @Counted(value = "lazyFluxWithSuccessRecordOnlyFailures", recordFailuresOnly = true,
            extraTags = {"extra", "tag"})
    public Flux<String> lazyFluxWithSuccessRecordOnlyFailures() {
        return Flux.defer(() -> Flux.just("ok"));
    }

    @Counted(value = "lazyFluxWithExceptionRecordOnlyFailures", recordFailuresOnly = true,
            extraTags = {"extra", "tag"})
    public Flux<String> lazyFluxWithExceptionRecordOnlyFailures() {
        return Flux.defer(() -> Flux.error(exception));
    }

    @Counted(value = "eagerFluxWithExceptionRecordOnlyFailures", recordFailuresOnly = true,
            extraTags = {"extra", "tag"})
    public Flux<String> eagerFluxWithExceptionRecordOnlyFailures() {
        throw exception;
    }

    @Counted(value = "eagerFluxWithErrorRecordOnlyFailures", recordFailuresOnly = true,
            extraTags = {"extra", "tag"})
    public Flux<String> eagerFluxWithErrorRecordOnlyFailures() {
        throw error;
    }
}
