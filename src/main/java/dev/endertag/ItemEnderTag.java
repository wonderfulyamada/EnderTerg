package dev.endertag.mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class ItemEnderTag extends Item {

    static final String TAG_BINDING_ID = "BindingId";
    private static final String TAG_ENTITY_NBT = "BoundEntityNbt";

    public ItemEnderTag() {
        setRegistryName(Tags.MOD_ID, "ender_tag");
        setTranslationKey(Tags.MOD_ID + ".ender_tag");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        if (target instanceof EntityPlayer) {
            return false;
        }

        if (!player.world.isRemote) {
            // EntityPlayer#interactOn passes a copy here in creative mode. Mutate the
            // authoritative hand slot so the binding survives the interaction.
            ItemStack heldStack = player.getHeldItem(hand);
            if (heldStack.isEmpty() || heldStack.getItem() != this || !heldStack.hasDisplayName()) {
                sendMessage(player, "message.endertag.unnamed");
                return true;
            }

            target.setCustomNameTag(heldStack.getDisplayName());
            NBTTagCompound tag = heldStack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                heldStack.setTagCompound(tag);
            }

            String bindingId = UUID.randomUUID().toString();
            target.getEntityData().setString(TAG_BINDING_ID, bindingId);
            NBTTagCompound entityNbt = writeBoundEntity(target);
            if (entityNbt == null) {
                target.getEntityData().removeTag(TAG_BINDING_ID);
                sendMessage(player, "message.endertag.unavailable");
                return true;
            }

            tag.setString(TAG_BINDING_ID, bindingId);
            tag.setTag(TAG_ENTITY_NBT, entityNbt);
            BindingRegistry.get((WorldServer) player.world).setActiveEntity(bindingId, target.getUniqueID());
            sendMessage(player, "message.endertag.bound");
        }
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            recallBoundEntity(stack, player);
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    private void recallBoundEntity(ItemStack stack, EntityPlayer player) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_BINDING_ID, 8) || !tag.hasKey(TAG_ENTITY_NBT, 10)
                || !tag.getCompoundTag(TAG_ENTITY_NBT).hasKey("id", 8)) {
            sendMessage(player, "message.endertag.unbound");
            return;
        }

        String bindingId = tag.getString(TAG_BINDING_ID);
        WorldServer world = (WorldServer) player.world;
        NBTTagCompound entityNbt = tag.getCompoundTag(TAG_ENTITY_NBT).copy();

        // Reconstructed entities get a fresh runtime UUID. BindingId is the
        // persistent Ender Terg identity used to reject stale copies later.
        entityNbt.removeTag("UUIDMost");
        entityNbt.removeTag("UUIDLeast");
        Entity entity = EntityList.createEntityFromNBT(entityNbt, world);
        if (!(entity instanceof EntityLivingBase)) {
            sendMessage(player, "message.endertag.unavailable");
            return;
        }

        EntityLivingBase living = (EntityLivingBase) entity;
        living.getEntityData().setString(TAG_BINDING_ID, bindingId);
        living.setPositionAndUpdate(player.posX, player.posY, player.posZ);

        BindingRegistry registry = BindingRegistry.get(world);
        UUID previousActiveUuid = registry.getActiveEntity(bindingId);
        UUID newActiveUuid = living.getUniqueID();

        // EntityJoinWorldEvent validates against this registry while spawnEntity
        // is running, so temporarily nominate the new instance before spawning.
        registry.setActiveEntity(bindingId, newActiveUuid);
        if (!world.spawnEntity(living)) {
            if (previousActiveUuid == null) {
                registry.removeActiveEntity(bindingId);
            } else {
                registry.setActiveEntity(bindingId, previousActiveUuid);
            }
            sendMessage(player, "message.endertag.unavailable");
            return;
        }

        // Only retire older loaded instances after the replacement has spawned
        // successfully. Unloaded stale instances are rejected when they later
        // receive EntityJoinWorldEvent during chunk loading.
        removeLoadedInstances(player, bindingId, newActiveUuid);

        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        NBTTagCompound updatedEntityNbt = writeBoundEntity(living);
        if (updatedEntityNbt != null) {
            tag.setTag(TAG_ENTITY_NBT, updatedEntityNbt);
        }
    }

    private NBTTagCompound writeBoundEntity(Entity entity) {
        ResourceLocation entityId = EntityList.getKey(entity);
        if (entityId == null) {
            return null;
        }
        NBTTagCompound entityNbt = new NBTTagCompound();
        entity.writeToNBT(entityNbt);
        entityNbt.setString("id", entityId.toString());
        return entityNbt;
    }

    private void removeLoadedInstances(EntityPlayer player, String bindingId, UUID activeUuid) {
        for (WorldServer world : player.getServer().worlds) {
            List<Entity> entities = new ArrayList<Entity>(world.loadedEntityList);
            for (Entity entity : entities) {
                if (bindingId.equals(entity.getEntityData().getString(TAG_BINDING_ID))
                        && !activeUuid.equals(entity.getUniqueID())) {
                    entity.setDead();
                }
            }
        }
    }

    private void sendMessage(EntityPlayer player, String key) {
        player.sendMessage(new TextComponentTranslation(key));
    }
}
