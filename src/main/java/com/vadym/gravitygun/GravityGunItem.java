package com.vadym.gravitygun;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * The Gravity Gun item.
 *
 * Right-click        — grab the mob or block you are looking at (16 block range).
 * Right-click again  — launch it where you look.
 * Sneak + right-click — set it down gently.
 */
public class GravityGunItem extends Item {

    public GravityGunItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.success(stack); // client: just swing
        }
        if (GrabManager.isHolding(player)) {
            if (player.isSneaking()) {
                GrabManager.release(player);
            } else {
                GrabManager.launch(player);
            }
        } else {
            GrabManager.tryGrabAtCrosshair(player);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        if (GrabManager.isHolding(player)) {
            if (player.isSneaking()) {
                GrabManager.release(player);
            } else {
                GrabManager.launch(player);
            }
            return ActionResult.SUCCESS;
        }
        return GrabManager.grabEntity(player, entity) ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        if (GrabManager.isHolding(player)) {
            if (player.isSneaking()) {
                GrabManager.release(player);
            } else {
                GrabManager.launch(player);
            }
            return ActionResult.SUCCESS;
        }
        return GrabManager.grabBlock(player, context.getBlockPos()) ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.gravitygun.gravity_gun.tooltip.grab").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.gravitygun.gravity_gun.tooltip.launch").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.gravitygun.gravity_gun.tooltip.drop").formatted(Formatting.GRAY));
    }
}
