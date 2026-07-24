package com.vadym.gravitygun;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

/**
 * Gravity Gun — grab mobs and blocks, hold them in mid-air and hurl them
 * with devastating force. Plus industrial conveyor belts to automate the
 * aftermath. Inspired by a certain famous physics gun.
 */
public class GravityGunMod implements ModInitializer {
    public static final String MOD_ID = "gravitygun";

    public static final GravityGunItem GRAVITY_GUN = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "gravity_gun"),
            new GravityGunItem(new Item.Settings()
                    .maxCount(1)
                    .rarity(Rarity.EPIC)
                    .fireproof()));

    public static final ConveyorBeltBlock CONVEYOR_BELT = Registry.register(
            Registries.BLOCK,
            Identifier.of(MOD_ID, "conveyor_belt"),
            new ConveyorBeltBlock(AbstractBlock.Settings.create()
                    .strength(1.0F)
                    .sounds(BlockSoundGroup.METAL)
                    .nonOpaque()));

    public static final ConveyorBeltItem CONVEYOR_BELT_ITEM = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "conveyor_belt"),
            new ConveyorBeltItem(CONVEYOR_BELT, new Item.Settings()));

    @Override
    public void onInitialize() {
        // Creative tabs: the gun with tools, the belt with redstone machinery.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(GRAVITY_GUN));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE)
                .register(entries -> entries.add(CONVEYOR_BELT_ITEM));

        // Physics driver: moves held targets and tracks launched projectiles.
        ServerTickEvents.END_SERVER_TICK.register(GrabManager::tick);

        // Never leave floating entities behind.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GrabManager.releaseAll());
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> GrabManager.releaseFor(handler.getPlayer()));
    }
}
