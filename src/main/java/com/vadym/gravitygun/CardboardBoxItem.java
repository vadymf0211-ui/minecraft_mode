package com.vadym.gravitygun;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Block item for the cardboard box. Carries its contents, hates water. */
public class CardboardBoxItem extends BlockItem {

    public CardboardBoxItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public boolean canBeNested() {
        return false; // no boxes inside boxes, shulkers or bundles
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("block.gravitygun.cardboard_box.tooltip1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("block.gravitygun.cardboard_box.tooltip2").formatted(Formatting.AQUA));
    }
}
