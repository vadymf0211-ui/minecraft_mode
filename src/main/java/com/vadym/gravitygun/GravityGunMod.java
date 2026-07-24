package com.vadym.gravitygun;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.List;

/**
 * Gravity Gun — grab mobs and blocks, hold them in mid-air and hurl them with
 * devastating force. Plus industrial conveyor belts and humble cardboard
 * logistics to automate the aftermath.
 */
public class GravityGunMod implements ModInitializer {
    public static final String MOD_ID = "gravitygun";

    // ------------------------------------------------------------------
    // Gravity gun
    // ------------------------------------------------------------------

    public static final GravityGunItem GRAVITY_GUN = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "gravity_gun"),
            new GravityGunItem(new Item.Settings()
                    .maxCount(1)
                    .rarity(Rarity.EPIC)
                    .fireproof()));

    // ------------------------------------------------------------------
    // Conveyor belt
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Cardboard
    // ------------------------------------------------------------------

    public static final CarpetBlock CARDBOARD = Registry.register(
            Registries.BLOCK,
            Identifier.of(MOD_ID, "cardboard"),
            new CarpetBlock(AbstractBlock.Settings.create()
                    .strength(0.1F)
                    .sounds(BlockSoundGroup.WOOL)));

    public static final CardboardItem CARDBOARD_ITEM = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "cardboard"),
            new CardboardItem(CARDBOARD, new Item.Settings()));

    public static final Item WET_CARDBOARD = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "wet_cardboard"),
            new Item(new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    tooltip.add(Text.translatable("item.gravitygun.wet_cardboard.tooltip").formatted(Formatting.GRAY));
                }
            });

    // ------------------------------------------------------------------
    // Cardboard box
    // ------------------------------------------------------------------

    public static final CardboardBoxBlock CARDBOARD_BOX = Registry.register(
            Registries.BLOCK,
            Identifier.of(MOD_ID, "cardboard_box"),
            new CardboardBoxBlock(AbstractBlock.Settings.create()
                    .strength(0.5F)
                    .sounds(BlockSoundGroup.WOOL)));

    public static final CardboardBoxItem CARDBOARD_BOX_ITEM = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "cardboard_box"),
            new CardboardBoxItem(CARDBOARD_BOX, new Item.Settings().maxCount(1)));

    public static final BlockEntityType<CardboardBoxBlockEntity> CARDBOARD_BOX_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MOD_ID, "cardboard_box"),
            BlockEntityType.Builder.create(CardboardBoxBlockEntity::new, CARDBOARD_BOX).build(null));

    public static final ScreenHandlerType<CardboardBoxScreenHandler> CARDBOARD_BOX_SCREEN_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MOD_ID, "cardboard_box"),
            new ScreenHandlerType<>(CardboardBoxScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    @Override
    public void onInitialize() {
        // Creative tabs.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(GRAVITY_GUN));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE)
                .register(entries -> entries.add(CONVEYOR_BELT_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register(entries -> entries.add(CARDBOARD_BOX_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(entries -> {
                    entries.add(CARDBOARD_ITEM);
                    entries.add(WET_CARDBOARD);
                });

        // Physics driver: moves held targets and tracks launched projectiles.
        ServerTickEvents.END_SERVER_TICK.register(GrabManager::tick);

        // Cardboard vs. water.
        ServerTickEvents.END_SERVER_TICK.register(CardboardManager::tick);
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> CardboardManager.watch(entity));

        // Never leave floating entities behind.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GrabManager.releaseAll());
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> GrabManager.releaseFor(handler.getPlayer()));
    }
}
