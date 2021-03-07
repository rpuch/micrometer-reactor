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
