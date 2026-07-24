package com.vadym.gravitygun;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Conveyor belt: a half-slab powered track that pushes entities (mobs, items,
 * players — anything riding on it) toward its facing direction and keeps the
 * cargo centred on the line.
 *
 * - A redstone signal pauses the belt.
 * - Right-clicking the belt with an item in hand places that item on the belt
 *   (like an item frame), so it rides away.
 * - Sneak + right-click to build blocks on/against the belt as usual.
 *
 * Movement uses {@code onEntityCollision}: entities standing on the half-tall
 * belt always overlap its block space, so the hook fires every tick on both
 * logical sides — the same mechanism as vanilla bubble columns, which keeps
 * player motion smooth with zero extra networking. (The previous version used
 * {@code onSteppedOn}, which vanilla only calls for the block half a block
 * below the entity's feet — a thin belt never received it.)
 */
public class ConveyorBeltBlock extends HorizontalFacingBlock {
    public static final MapCodec<ConveyorBeltBlock> CODEC = createCodec(ConveyorBeltBlock::new);

    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    /** Top speed along the belt, blocks per tick (~5.6 blocks per second). */
    private static final double MAX_SPEED = 0.28;
    /** Acceleration applied each tick until the target speed is reached. */
    private static final double ACCELERATION = 0.07;
    /** How strongly cargo is pulled toward the centre line of the belt. */
    private static final double CENTERING = 0.12;

    public ConveyorBeltBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        // Only move cargo that is actually riding the belt surface (top is at +0.5).
        double rel = entity.getY() - pos.getY();
        if (rel < 0.4 || rel > 0.75) {
            return;
        }
        if (world.isReceivingRedstonePower(pos)) {
            return; // paused by redstone
        }

        Direction dir = state.get(FACING);
        Vec3d velocity = entity.getVelocity();
        double along = velocity.x * dir.getOffsetX() + velocity.z * dir.getOffsetZ();
        double boost = Math.min(ACCELERATION, Math.max(0.0, MAX_SPEED - along));

        // Nudge toward the centre line so cargo rides in a tidy row.
        double centerX = 0.0;
        double centerZ = 0.0;
        if (dir.getAxis() == Direction.Axis.X) {
            centerZ = (pos.getZ() + 0.5 - entity.getZ()) * CENTERING;
        } else {
            centerX = (pos.getX() + 0.5 - entity.getX()) * CENTERING;
        }

        entity.addVelocity(dir.getOffsetX() * boost + centerX, 0.0, dir.getOffsetZ() * boost + centerZ);
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Let belts (and empty hands) fall through to normal behaviour so lines are easy to build.
        if (stack.isEmpty()
                || (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ConveyorBeltBlock)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (world instanceof ServerWorld serverWorld) {
            ItemStack cargo = stack.copyWithCount(1);
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            ItemEntity item = new ItemEntity(serverWorld,
                    pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, cargo);
            item.setVelocity(Vec3d.ZERO);
            item.setPickupDelay(30);
            serverWorld.spawnEntity(item);
            serverWorld.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM,
                    SoundCategory.BLOCKS, 0.8F, 1.0F);
        }
        return ItemActionResult.SUCCESS;
    }
}
