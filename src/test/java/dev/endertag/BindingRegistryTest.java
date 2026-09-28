package dev.endertag.mod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

public class BindingRegistryTest {

    @Test
    public void roundTripsActiveEntityMappings() {
        BindingRegistry registry = new BindingRegistry();
        UUID uuid = UUID.randomUUID();
        registry.setActiveEntity("binding-a", uuid);

        NBTTagCompound nbt = registry.writeToNBT(new NBTTagCompound());

        BindingRegistry restored = new BindingRegistry();
        restored.readFromNBT(nbt);

        assertEquals(uuid, restored.getActiveEntity("binding-a"));
    }

    @Test
    public void claimIfAbsentDoesNotReplaceExistingOwner() {
        BindingRegistry registry = new BindingRegistry();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(registry.claimIfAbsent("binding-a", first));
        assertFalse(registry.claimIfAbsent("binding-a", second));
        assertEquals(first, registry.getActiveEntity("binding-a"));
    }

    @Test
    public void removeActiveEntityClearsMapping() {
        BindingRegistry registry = new BindingRegistry();
        registry.setActiveEntity("binding-a", UUID.randomUUID());

        registry.removeActiveEntity("binding-a");

        assertNull(registry.getActiveEntity("binding-a"));
    }

    @Test
    public void failedSpawnRestoresPreviousActiveEntity() {
        BindingRegistry registry = new BindingRegistry();
        UUID previous = UUID.randomUUID();
        registry.setActiveEntity("binding-a", UUID.randomUUID());

        ItemEnderTag.restoreActiveEntityAfterFailedSpawn(registry, "binding-a", previous);

        assertEquals(previous, registry.getActiveEntity("binding-a"));
    }

    @Test
    public void failedSpawnWithoutPreviousActiveEntityClearsFailedEntity() {
        BindingRegistry registry = new BindingRegistry();
        registry.setActiveEntity("binding-a", UUID.randomUUID());

        ItemEnderTag.restoreActiveEntityAfterFailedSpawn(registry, "binding-a", null);

        assertNull(registry.getActiveEntity("binding-a"));
    }
}
