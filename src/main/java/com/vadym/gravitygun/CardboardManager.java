package com.vadym.gravitygun;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Watches cardboard boxes for water contact:
 * - a box item thrown into water soaks, spills its contents and turns into wet cardboard;
 * - a player wading into water while HOLDING a box (either hand) soaks it the same way.
 * Placed boxes are watched by their own block entity ticker.
 */
public final class CardboardManager {
    /** How many wet cardboard scraps a soaked box leaves behind. */
    public static final int WET_SCRAPS = 4;

    private static final List<ItemEntity> WATCHED = new ArrayList<>();

    private CardboardManager() {}

    /** Called for every entity loaded/spawned server-side. */
    public static void watch(Entity entity) {
        if (entity instanceof ItemEntity item && item.getStack().isOf(GravityGunMod.CARDBOARD_BOX_ITEM)) {
            WATCHED.add(item);
        }
    }

    public static void tick(MinecraftServer server) {
        // Thrown box items.
        Iterator<ItemEntity> it = WATCHED.iterator();
        while (it.hasNext()) {
            ItemEntity item = it.next();
            if (item.isRemoved() || !item.getStack().isOf(GravityGunMod.CARDBOARD_BOX_ITEM)) {
                it.remove();
                continue;
            }
            if (item.isTouchingWater() && item.getWorld() instanceof ServerWorld world) {
                soakItemEntity(world, item);
                it.remove();
            }
        }

        // Boxes held in hands while the player is in water.
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.isTouchingWater()) {
                continue;
            }
            for (Hand hand : Hand.values()) {
                ItemStack stack = player.getStackInHand(hand);
                if (stack.isOf(GravityGunMod.CARDBOARD_BOX_ITEM)) {
                    ServerWorld world = player.getServerWorld();
                    spillContents(world, player.getPos(), stack);
                    player.setStackInHand(hand, new ItemStack(GravityGunMod.WET_CARDBOARD, WET_SCRAPS));
                    splash(world, player.getPos());
                }
            }
        }
    }

    private static void soakItemEntity(ServerWorld world, ItemEntity item) {
        spillContents(world, item.getPos(), item.getStack());
        item.discard();
        world.spawnEntity(new ItemEntity(world, item.getX(), item.getY(), item.getZ(),
                new ItemStack(GravityGunMod.WET_CARDBOARD, WET_SCRAPS)));
        splash(world, item.getPos());
    }

    private static void spillContents(ServerWorld world, Vec3d pos, ItemStack box) {
        ContainerComponent contents = box.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        contents.stream().forEach(stack -> {
            if (!stack.isEmpty()) {
                ItemScatterer.spawn(world, pos.x, pos.y + 0.3, pos.z, stack.copy());
            }
        });
    }

    static void splash(ServerWorld world, Vec3d pos) {
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_PLAYER_SPLASH,
                SoundCategory.BLOCKS, 0.7F, 1.2F);
        world.spawnParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.3, pos.z, 20, 0.3, 0.2, 0.3, 0.1);
    }
}
