package dev.endertag.mod;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class BoundEntityHandler {

    private BoundEntityHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getWorld() instanceof WorldServer)) {
            return;
        }

        Entity entity = event.getEntity();
        String bindingId = entity.getEntityData().getString(ItemEnderTag.TAG_BINDING_ID);
        if (bindingId.isEmpty()) {
            return;
        }

        BindingRegistry registry = BindingRegistry.get((WorldServer) event.getWorld());
        UUID activeUuid = registry.getActiveEntity(bindingId);

        // Legacy v1.0.0 entities have no registry entry yet. The first instance
        // loaded after the update becomes authoritative until the tag is recalled.
        if (activeUuid == null) {
            registry.claimIfAbsent(bindingId, entity.getUniqueID());
            return;
        }

        if (!activeUuid.equals(entity.getUniqueID())) {
            event.setCanceled(true);
        }
    }
}
