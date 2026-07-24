package com.vadym.gravitygun;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Cardboard sheet: crafting material that can also be laid down as a carpet. */
public class CardboardItem extends BlockItem {

    public CardboardItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("block.gravitygun.cardboard.tooltip").formatted(Formatting.GRAY));
    }
}
