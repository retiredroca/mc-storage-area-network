package com.retiredroca.networkrouting.neoforge;

import com.retiredroca.mcstorageareanetwork.api.StorageRouter;
import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.NetworkRoutingHooks;
import com.retiredroca.networkrouting.NetworkRoutingRoutes;
import com.retiredroca.networkrouting.routing.NetworkRoutingRule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(NetworkRoutingCommon.MODID)
public class NetworkRouting {
    public NetworkRouting(IEventBus modEventBus, ModContainer modContainer) {
        NetworkRoutingCommon.setPlatform(new NeoForgeNetworkRoutingPlatform());
        Registration.register(modEventBus);
        Networking.register(modEventBus);
        StorageRouter.register(NetworkRoutingRule.INSTANCE);
        NetworkRoutingHooks.register();
        NetworkRoutingRoutes.register();
    }
}
