package com.vadym.gravitygun;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Cardboard box: portable 14-slot storage (half a shulker box) that keeps its
 * contents when broken — but soaks and falls apart the moment it touches
 * water, spilling everything and leaving wet cardboard behind.
 */
public class CardboardBoxBlock extends BlockWithEntity {
    public static final MapCodec<CardboardBoxBlock> CODEC = createCodec(CardboardBoxBlock::new);

    public CardboardBoxBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CardboardBoxBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL; // BlockWithEntity defaults to INVISIBLE
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof CardboardBoxBlockEntity box) {
            player.openHandledScreen(box);
            world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 0.8F, 1.1F);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null
                : validateTicker(type, GravityGunMod.CARDBOARD_BOX_BLOCK_ENTITY, CardboardBoxBlock::tick);
    }

    /** Every half second: soak if any neighbouring block holds water. */
    private static void tick(World world, BlockPos pos, BlockState state, CardboardBoxBlockEntity box) {
        if (world.getTime() % 10 != 0 || !(world instanceof ServerWorld serverWorld)) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (world.getFluidState(pos.offset(direction)).isIn(FluidTags.WATER)) {
                soak(serverWorld, pos, box);
                return;
            }
        }
    }

    private static void soak(ServerWorld world, BlockPos pos, CardboardBoxBlockEntity box) {
        ItemScatterer.spawn(world, pos, box.getItems());
        ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(GravityGunMod.WET_CARDBOARD, CardboardManager.WET_SCRAPS));
        world.removeBlock(pos, false);
        CardboardManager.splash(world, Vec3d.ofCenter(pos));
    }
}
