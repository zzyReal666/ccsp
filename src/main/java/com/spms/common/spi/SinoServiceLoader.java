package com.spms.common.spi;


import com.google.common.base.Preconditions;
import com.spms.common.spi.annotation.SingletonSPI;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/3/20 15:19
 */
public class SinoServiceLoader<T> {
    private static final Map<Class<?>, SinoServiceLoader<?>> LOADERS = new ConcurrentHashMap<>();

    private final Class<T> serviceInterface;

    @Getter
    private final Collection<T> services;

    private SinoServiceLoader(final Class<T> serviceInterface) {
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
        for (T each : ServiceLoader.load(serviceInterface)) {
            result.add(each);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> Collection<T> getServiceInstances(final Class<T> serviceInterface) {
        SinoServiceLoader<?> result = LOADERS.get(serviceInterface);
        return (Collection<T>) (null != result ? result.getServiceInstances() : LOADERS.computeIfAbsent(serviceInterface, SinoServiceLoader::new).getServiceInstances());
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
