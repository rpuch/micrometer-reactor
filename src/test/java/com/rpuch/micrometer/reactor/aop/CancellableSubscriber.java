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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author Roman Puchkovskiy
 */
@SuppressWarnings("ReactiveStreamsSubscriberImplementation")
public class CancellableSubscriber implements Subscriber<Object> {
    private volatile Subscription subscription;

    @Override
    public void onSubscribe(Subscription s) {
        subscription = s;
    }

    @Override
    public void onNext(Object o) {
        // no op
    }

    @Override
    public void onError(Throwable t) {
        // no op
    }

    @Override
    public void onComplete() {
        // no op
    }

    public void cancel() {
        subscription.cancel();
    }
}
