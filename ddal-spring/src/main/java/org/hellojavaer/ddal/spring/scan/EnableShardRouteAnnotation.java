/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hellojavaer.ddal.spring.scan;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hellojavaer.ddal.ddr.shard.ShardRoute;
import org.hellojavaer.ddal.ddr.shard.ShardRouteContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * add the following tags in spring configuration file
 * <pre>
        <aop:aspectj-autoproxy/>
        <bean class="org.hellojavaer.ddal.spring.scan.EnableShardRouteAnnotation"/>
 * </pre>
 *
 * @author <a href="mailto:hellojavaer@gmail.com">Kaiming Zou</a>,created on 01/01/2017.
 */
@Aspect
@Component
public class EnableShardRouteAnnotation {

    private Map<Method, MethodBasedSpelExpression> expressionCache = new HashMap<>();

    @Around("@annotation(shardRoute)")
    public Object around(ProceedingJoinPoint joinPoint, ShardRoute shardRoute) throws Throwable {
        try {
            ShardRouteContext.pushContext();
            if (shardRoute.scName() != null && shardRoute.scName().length() > 0 //
                && shardRoute.sdValue() != null && shardRoute.sdValue().length() > 0) {
                MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
                Method method = methodSignature.getMethod();
                MethodBasedSpelExpression expression = expressionCache.get(method);
                Object[] args = joinPoint.getArgs();
                if (expression == null) {
                    synchronized (expressionCache) {
                        expression = expressionCache.get(method);
                        if (expression == null) {
                            expression = new MethodBasedSpelExpression(shardRoute.sdValue(), method);
                            expressionCache.put(method, expression);
                        }
                    }
                }
                Object val = expression.parse(Object.class, args);
                String[] scNames = shardRoute.scName().split(",");
                for (String scName : scNames) {
                    ShardRouteContext.setRouteInfo(scName, val);
                }
            } else {
                if ((shardRoute.scName() == null || shardRoute.scName().length() == 0)
                    && (shardRoute.sdValue() == null || shardRoute.sdValue().length() == 0)) {
                    // ok
                } else {
                    throw new IllegalArgumentException(
                                                       "scName and sdValue should either both have a non-empty value or both have a empty value");
                }
            }
            return joinPoint.proceed(joinPoint.getArgs());
        } finally {
            ShardRouteContext.popContext();
        }
    }

}
