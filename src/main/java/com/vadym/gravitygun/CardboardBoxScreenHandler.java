package com.vadym.gravitygun;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/** 14 box slots (2 rows x 7) + the standard player inventory. */
public class CardboardBoxScreenHandler extends ScreenHandler {
    public static final int SIZE = CardboardBoxBlockEntity.SIZE;

    private final Inventory inventory;

    /** Client-side constructor. */
    public CardboardBoxScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(SIZE));
    }

    public CardboardBoxScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(GravityGunMod.CARDBOARD_BOX_SCREEN_HANDLER, syncId);
        checkSize(inventory, SIZE);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        // box: 2 rows x 7 slots, centred (x0 = 25)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 7; col++) {
                addSlot(new BoxSlot(inventory, col + row * 7, 25 + col * 18, 18 + row * 18));
            }
        }
        // player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 67 + row * 18));
            }
        }
        // hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 125));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (slotIndex < SIZE) {
                if (!insertItem(stack, SIZE, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!stack.getItem().canBeNested() || !insertItem(stack, 0, SIZE, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }

    /** No boxes inside boxes. */
    private static class BoxSlot extends Slot {
        BoxSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return stack.getItem().canBeNested();
        }
    }
}
