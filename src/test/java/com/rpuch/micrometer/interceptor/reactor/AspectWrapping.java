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

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.Arrays;

public class AspectWrapping {
    public static <T> T wrapInAspect(T objectToWrap, Object aspect) {
        return wrapInAspects(objectToWrap, aspect);
    }

    public static <T> T wrapInAspects(T objectToWrap, Object... aspects) {
        AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(objectToWrap);
        Object[] aspectsCopy = Arrays.copyOf(aspects, aspects.length);
        AnnotationAwareOrderComparator.sort(aspectsCopy);
        for (Object aspect : aspectsCopy) {
            aspectJProxyFactory.addAspect(aspect);
        }

        DefaultAopProxyFactory proxyFactory = new DefaultAopProxyFactory();
        AopProxy aopProxy = proxyFactory.createAopProxy(aspectJProxyFactory);

        @SuppressWarnings("unchecked") T castResult = (T) aopProxy.getProxy();
        return castResult;
    }
}
