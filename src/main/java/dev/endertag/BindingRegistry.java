package dev.endertag.mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

public final class BindingRegistry extends WorldSavedData {

    private static final String DATA_NAME = "endertag_bindings";
    private static final String TAG_BINDINGS = "Bindings";
    private static final String TAG_BINDING_ID = "BindingId";
    private static final String TAG_UUID_MOST = "UuidMost";
    private static final String TAG_UUID_LEAST = "UuidLeast";

    private final Map<String, UUID> activeEntities = new HashMap<String, UUID>();

    public BindingRegistry() {
        super(DATA_NAME);
    }

    public BindingRegistry(String name) {
        super(name);
    }

    public static BindingRegistry get(WorldServer world) {
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            throw new IllegalStateException("Ender Terg binding storage is unavailable");
        }

        BindingRegistry registry = (BindingRegistry) storage.getOrLoadData(BindingRegistry.class, DATA_NAME);
        if (registry == null) {
            registry = new BindingRegistry();
            storage.setData(DATA_NAME, registry);
        }
        return registry;
    }

    @Nullable
    public UUID getActiveEntity(String bindingId) {
        return activeEntities.get(bindingId);
    }

    public void setActiveEntity(String bindingId, UUID entityUuid) {
        activeEntities.put(bindingId, entityUuid);
        markDirty();
    }

    public boolean claimIfAbsent(String bindingId, UUID entityUuid) {
        if (activeEntities.containsKey(bindingId)) {
            return false;
        }
        setActiveEntity(bindingId, entityUuid);
        return true;
    }

    public void removeActiveEntity(String bindingId) {
        if (activeEntities.remove(bindingId) != null) {
            markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        activeEntities.clear();
        NBTTagList bindings = nbt.getTagList(TAG_BINDINGS, 10);
        for (int i = 0; i < bindings.tagCount(); i++) {
            NBTTagCompound entry = bindings.getCompoundTagAt(i);
            if (!entry.hasKey(TAG_BINDING_ID, 8)
                    || !entry.hasKey(TAG_UUID_MOST, 4)
                    || !entry.hasKey(TAG_UUID_LEAST, 4)) {
                continue;
            }

            String bindingId = entry.getString(TAG_BINDING_ID);
            if (bindingId.isEmpty()) {
                continue;
            }

            UUID entityUuid = new UUID(entry.getLong(TAG_UUID_MOST), entry.getLong(TAG_UUID_LEAST));
            activeEntities.put(bindingId, entityUuid);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList bindings = new NBTTagList();
        for (Map.Entry<String, UUID> entry : activeEntities.entrySet()) {
            NBTTagCompound binding = new NBTTagCompound();
            binding.setString(TAG_BINDING_ID, entry.getKey());
            binding.setLong(TAG_UUID_MOST, entry.getValue().getMostSignificantBits());
            binding.setLong(TAG_UUID_LEAST, entry.getValue().getLeastSignificantBits());
            bindings.appendTag(binding);
        }
        nbt.setTag(TAG_BINDINGS, bindings);
        return nbt;
    }
}
