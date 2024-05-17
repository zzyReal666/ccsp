package com.spms.common.spi.typed;


import com.spms.common.spi.SinoServiceLoader;

import java.util.Optional;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/3/20 15:40
 */
public class TypedSPIRegistry {

    public static <T extends TypedSPI> Optional<T> findRegisteredService(final Class<T> spiClass, final String type) {
        for (T each : SinoServiceLoader.getServiceInstances(spiClass)) {
            if (matchesType(type, each)) {
                return Optional.of(each);
            }
        }
        return Optional.empty();
    }

    private static boolean matchesType(final String type, final TypedSPI instance) {
        return instance.getType().equalsIgnoreCase(type) || instance.getTypeAliases().contains(type);
    }

}
