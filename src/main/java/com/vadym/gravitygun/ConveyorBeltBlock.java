package com.vadym.gravitygun;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Conveyor belt: a thin powered track that pushes entities (mobs, items,
 * anything standing on it) toward its facing direction and keeps them
 * centred on the line.
 *
 * - A redstone signal pauses the belt.
 * - Sneaking players are not moved (vanilla "bypasses stepping effects").
 *
 * The push runs on both logical sides, exactly like vanilla motion blocks
 * (honey, bubble columns), so player movement stays smooth without any
 * extra velocity-sync packets.
 */
public class ConveyorBeltBlock extends HorizontalFacingBlock {
    public static final MapCodec<ConveyorBeltBlock> CODEC = createCodec(ConveyorBeltBlock::new);

    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    /** Top speed along the belt, blocks per tick (~5.6 blocks per second). */
    private static final double MAX_SPEED = 0.28;
    /** Acceleration applied each tick until the target speed is reached. */
    private static final double ACCELERATION = 0.07;
    /** How strongly entities are pulled toward the centre line of the belt. */
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
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isReceivingRedstonePower(pos)) {
            convey(pos, state, entity);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    private static void convey(BlockPos pos, BlockState state, Entity entity) {
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
}
