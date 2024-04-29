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

package com.spms.common.spi.typed;

import com.spms.common.spi.ServiceLoader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypedSPIRegistry {

    /**
     * 寻找已注册的服务
     * @param spiClass 要寻找的SPI类 接口
     * @param type 类型 对应实现类中的getType方法
     * @return
     * @param <T>
     */
    public static <T extends TypedSPI> Optional<T> findRegisteredService(final Class<T> spiClass, final String type) {
        for (T each : ServiceLoader.getServiceInstances(spiClass)) {
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
