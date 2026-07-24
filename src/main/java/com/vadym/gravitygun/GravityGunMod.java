package com.vadym.gravitygun;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

/**
 * Gravity Gun — grab mobs and blocks, hold them in mid-air and hurl them
 * with devastating force. Inspired by a certain famous physics gun.
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

    @Override
    public void onInitialize() {
        // Put the gun into the vanilla "Tools & Utilities" creative tab.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(GRAVITY_GUN));

        // Physics driver: moves held targets and tracks launched projectiles.
        ServerTickEvents.END_SERVER_TICK.register(GrabManager::tick);

        // Never leave floating entities behind.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GrabManager.releaseAll());
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> GrabManager.releaseFor(handler.getPlayer()));
    }
}
