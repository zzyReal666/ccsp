/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spms.common.spi;

import com.google.common.base.Preconditions;
import com.spms.common.spi.annotation.SingletonSPI;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  service loader. 用于加载SPI的实现类
 */
public final class ServiceLoader<T> {
    
    private static final Map<Class<?>, ServiceLoader<?>> LOADERS = new ConcurrentHashMap<>();
    
    private final Class<T> serviceInterface;
    
    @Getter
    private final Collection<T> services;
    
    private ServiceLoader(final Class<T> serviceInterface) {
        this.serviceInterface = serviceInterface;
        validate();
        services = load();
    }
    
    private void validate() {
        Preconditions.checkNotNull(serviceInterface, "SPI interface is null.");
        Preconditions.checkArgument(serviceInterface.isInterface(), "SPI interface `%s` is not interface.", serviceInterface);
    }
    
    private Collection<T> load() {
        Collection<T> result = new LinkedList<>();
        for (T each : java.util.ServiceLoader.load(serviceInterface)) {
            result.add(each);
        }
        return result;
    }


    /**
     * 获取服务实例
     * @param serviceInterface 服务接口
     * @return 服务实例列表
     * @param <T> 服务接口类型
     */
    @SuppressWarnings("unchecked")
    public static <T> Collection<T> getServiceInstances(final Class<T> serviceInterface) {
        ServiceLoader<?> result = LOADERS.get(serviceInterface);
        return (Collection<T>) (null != result ? result.getServiceInstances() : LOADERS.computeIfAbsent(serviceInterface, ServiceLoader::new).getServiceInstances());
    }
    
    private Collection<T> getServiceInstances() {
        return null == serviceInterface.getAnnotation(SingletonSPI.class) ? createNewServiceInstances() : getSingletonServiceInstances();
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    @SuppressWarnings("unchecked")
    private Collection<T> createNewServiceInstances() {
        Collection<T> result = new LinkedList<>();
        for (Object each : services) {
            result.add((T) each.getClass().getDeclaredConstructor().newInstance());
        }
        return result;
    }
    
    private Collection<T> getSingletonServiceInstances() {
        return services;
    }
}
