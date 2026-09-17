package com.retiredroca.networkrouting.fabric;

import com.retiredroca.mcstorageareanetwork.api.StorageRouter;
import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.NetworkRoutingHooks;
import com.retiredroca.networkrouting.NetworkRoutingRoutes;
import com.retiredroca.networkrouting.routing.NetworkRoutingRule;

import net.fabricmc.api.ModInitializer;

public class NetworkRouting implements ModInitializer {
    @Override
    public void onInitialize() {
        NetworkRoutingCommon.setPlatform(new FabricNetworkRoutingPlatform());
        Registration.register();
        Networking.register();
        StorageRouter.register(NetworkRoutingRule.INSTANCE);
        NetworkRoutingHooks.register();
        NetworkRoutingRoutes.register();
    }
}
