package com.vadym.gravitygun;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Block item for the conveyor belt with a usage hint tooltip. */
public class ConveyorBeltItem extends BlockItem {

    public ConveyorBeltItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("block.gravitygun.conveyor_belt.tooltip1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("block.gravitygun.conveyor_belt.tooltip2").formatted(Formatting.GRAY));
    }
}
