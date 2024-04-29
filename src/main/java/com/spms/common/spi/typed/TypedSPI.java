package com.spms.common.spi.typed;

import java.util.Collection;
import java.util.Collections;

/**
 * @author zzxka
 * @date 2023-07-17
 * @desc
 */
public interface TypedSPI {

    default String getType() {
        return "";
    }

    /**
     * Get type aliases.
     *
     * @return type aliases
     */
    default Collection<String> getTypeAliases() {
        return Collections.emptyList();
    }
}
