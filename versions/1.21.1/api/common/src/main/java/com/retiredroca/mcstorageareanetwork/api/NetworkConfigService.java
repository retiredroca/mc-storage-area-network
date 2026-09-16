package com.retiredroca.mcstorageareanetwork.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Loader-provided hook that persists changes to the network's container-exclusion list (the loader
 * config). Installed alongside the {@link ItemScanner} so common code can toggle exclusions.
 */
public interface NetworkConfigService {
    void setContainerExcluded(ResourceLocation blockId, boolean excluded);
}
