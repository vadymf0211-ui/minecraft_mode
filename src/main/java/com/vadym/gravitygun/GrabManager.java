package com.vadym.gravitygun;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side brain of the Gravity Gun.
 *
 * Keeps track of what every player is currently holding, moves the held
 * target to a point in front of the player's eyes every tick, and tracks
 * launched objects so they deal impact damage when they crash into
 * something (or someone).
 */
public final class GrabManager {

    private static final double GRAB_RANGE = 16.0;
    private static final double HOLD_DISTANCE = 3.0;
    private static final double LAUNCH_POWER = 2.6;
    private static final int THROW_TRACK_TICKS = 80;

    private static final Map<UUID, Held> HELD = new HashMap<>();
    private static final List<Thrown> THROWN = new ArrayList<>();

    private record Held(ServerWorld world, UUID entityId) {}

    private static final class Thrown {
        final ServerWorld world;
        final UUID entityId;
        final UUID ownerId;
        int ticksLeft = THROW_TRACK_TICKS;
        double lastSpeed;

        Thrown(ServerWorld world, UUID entityId, UUID ownerId, double lastSpeed) {
            this.world = world;
            this.entityId = entityId;
            this.ownerId = ownerId;
            this.lastSpeed = lastSpeed;
        }
    }

    private GrabManager() {}

    // ------------------------------------------------------------------
    // Grabbing
    // ------------------------------------------------------------------

    public static boolean isHolding(ServerPlayerEntity player) {
        return HELD.containsKey(player.getUuid());
    }

    /** Raycast up to {@link #GRAB_RANGE} blocks and grab whatever is on the crosshair. */
    public static boolean tryGrabAtCrosshair(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d dir = player.getRotationVec(1.0F);
        Vec3d end = start.add(dir.multiply(GRAB_RANGE));

        BlockHitResult blockHit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        double blockDistSq = blockHit.getType() == HitResult.Type.BLOCK
                ? blockHit.getPos().squaredDistanceTo(start)
                : Double.MAX_VALUE;

        Box searchBox = player.getBoundingBox().stretch(dir.multiply(GRAB_RANGE)).expand(1.0);
        EntityHitResult entityHit = ProjectileUtil.raycast(
                player, start, end, searchBox, GrabManager::canGrabEntity, GRAB_RANGE * GRAB_RANGE);

        if (entityHit != null && entityHit.getPos().squaredDistanceTo(start) < blockDistSq) {
            if (grabEntity(player, entityHit.getEntity())) {
                return true;
            }
        }
        if (blockHit.getType() == HitResult.Type.BLOCK && grabBlock(player, blockHit.getBlockPos())) {
            return true;
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.7F, 1.4F);
        return false;
    }

    static boolean canGrabEntity(Entity e) {
        if (e == null || e.isRemoved() || !e.isAlive()) {
            return false;
        }
        if (e instanceof PlayerEntity || e instanceof EnderDragonEntity || e instanceof WitherEntity) {
            return false;
        }
        if (isHeldByAnyone(e.getUuid())) {
            return false;
        }
        return e instanceof LivingEntity
                || e instanceof VehicleEntity
                || e instanceof TntEntity
                || e instanceof ItemEntity
                || e instanceof FallingBlockEntity;
    }

    private static boolean isHeldByAnyone(UUID entityId) {
        for (Held held : HELD.values()) {
            if (held.entityId().equals(entityId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean grabEntity(ServerPlayerEntity player, Entity target) {
        if (isHolding(player) || !canGrabEntity(target)) {
            return false;
        }
        if (!(target.getWorld() instanceof ServerWorld world) || world != player.getServerWorld()) {
            return false;
        }
        target.stopRiding();
        target.setNoGravity(true);
        target.fallDistance = 0.0F;
        target.setVelocity(Vec3d.ZERO);
        target.velocityModified = true;

        HELD.put(player.getUuid(), new Held(world, target.getUuid()));
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.6F, 1.8F);
        spawnBurst(world, target, ParticleTypes.ELECTRIC_SPARK, 12);
        return true;
    }

    /** Rip a block out of the world and hold it as a floating falling-block entity. */
    public static boolean grabBlock(ServerPlayerEntity player, BlockPos pos) {
        if (isHolding(player)) {
            return false;
        }
        ServerWorld world = player.getServerWorld();
        Vec3d eye = player.getCameraPosVec(1.0F);
        if (eye.squaredDistanceTo(Vec3d.ofCenter(pos)) > GRAB_RANGE * GRAB_RANGE) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        if (!canGrabBlock(world, pos, state)) {
            return false;
        }
        FallingBlockEntity block = FallingBlockEntity.spawnFromBlock(world, pos, state);
        block.setNoGravity(true);
        block.dropItem = true;
        block.timeFalling = 1;

        HELD.put(player.getUuid(), new Held(world, block.getUuid()));
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.6F, 1.8F);
        spawnBurst(world, block, ParticleTypes.ELECTRIC_SPARK, 12);
        return true;
    }

    static boolean canGrabBlock(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.getHardness(world, pos) < 0.0F) {
            return false; // bedrock, end portal frames, etc.
        }
        if (world.getBlockEntity(pos) != null) {
            return false; // chests, furnaces — don't eat anyone's diamonds
        }
        return !(state.getBlock() instanceof FluidBlock);
    }

    // ------------------------------------------------------------------
    // Releasing
    // ------------------------------------------------------------------

    /** Violent throw along the player's view vector. */
    public static void launch(ServerPlayerEntity player) {
        Entity e = takeHeld(player);
        if (e == null) {
            return;
        }
        ServerWorld world = (ServerWorld) e.getWorld();
        Vec3d vel = player.getRotationVec(1.0F).multiply(LAUNCH_POWER).add(0.0, 0.1, 0.0);

        e.setNoGravity(false);
        e.setVelocity(vel);
        e.velocityModified = true;
        e.fallDistance = 0.0F;
        if (e instanceof FallingBlockEntity block) {
            block.setHurtEntities(2.0F, 40);
            block.timeFalling = 2;
        }
        THROWN.add(new Thrown(world, e.getUuid(), player.getUuid(), vel.length()));

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.5F, 1.5F);
        world.spawnParticles(ParticleTypes.SONIC_BOOM,
                e.getX(), e.getBodyY(0.5), e.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        player.getItemCooldownManager().set(GravityGunMod.GRAVITY_GUN, 10);
    }

    /** Gentle drop right where it floats. */
    public static void release(ServerPlayerEntity player) {
        Entity e = takeHeld(player);
        if (e == null) {
            return;
        }
        dropGently(e);
        ServerWorld world = (ServerWorld) e.getWorld();
        world.playSound(null, e.getX(), e.getY(), e.getZ(),
                SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.5F, 1.6F);
        spawnBurst(world, e, ParticleTypes.CLOUD, 6);
    }

    /** Silent cleanup (player logged out or died). */
    public static void releaseFor(ServerPlayerEntity player) {
        Entity e = takeHeld(player);
        if (e != null) {
            dropGently(e);
        }
    }

    /** Server is stopping: drop everything, leave no floating entities behind. */
    public static void releaseAll() {
        for (Held held : HELD.values()) {
            Entity e = held.world().getEntity(held.entityId());
            if (e != null && !e.isRemoved()) {
                dropGently(e);
            }
        }
        HELD.clear();
        THROWN.clear();
    }

    private static Entity takeHeld(ServerPlayerEntity player) {
        Held held = HELD.remove(player.getUuid());
        if (held == null) {
            return null;
        }
        Entity e = held.world().getEntity(held.entityId());
        return (e == null || e.isRemoved()) ? null : e;
    }

    private static void dropGently(Entity e) {
        e.setNoGravity(false);
        e.setVelocity(e.getVelocity().multiply(0.2));
        e.velocityModified = true;
        e.fallDistance = 0.0F;
        if (e instanceof FallingBlockEntity block) {
            block.timeFalling = 2;
        }
    }

    // ------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        if (!HELD.isEmpty()) {
            tickHeld(server);
        }
        if (!THROWN.isEmpty()) {
            tickThrown();
        }
    }

    private static void tickHeld(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Held>> it = HELD.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Held> entry = it.next();
            Held held = entry.getValue();
            Entity e = held.world().getEntity(held.entityId());
            if (e == null || e.isRemoved()) {
                it.remove();
                continue;
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()
                    || player.getServerWorld() != held.world() || !holdsGun(player)) {
                dropGently(e);
                it.remove();
                continue;
            }

            // Target point: HOLD_DISTANCE in front of the eyes, pulled back if a wall is in the way.
            Vec3d eye = player.getCameraPosVec(1.0F);
            Vec3d look = player.getRotationVec(1.0F);
            Vec3d desired = eye.add(look.multiply(HOLD_DISTANCE));
            BlockHitResult wall = held.world().raycast(new RaycastContext(
                    eye, desired, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (wall.getType() == HitResult.Type.BLOCK) {
                desired = wall.getPos().subtract(look.multiply(0.5 + e.getWidth() * 0.5));
            }
            desired = desired.add(0.0, -e.getHeight() * 0.5, 0.0);

            Vec3d delta = desired.subtract(e.getPos());
            if (delta.lengthSquared() > 36.0) {
                // Something yanked it far away — snap it back.
                e.requestTeleport(desired.x, desired.y, desired.z);
                e.setVelocity(Vec3d.ZERO);
            } else {
                Vec3d vel = delta.multiply(0.45);
                if (vel.lengthSquared() > 2.25) {
                    vel = vel.normalize().multiply(1.5);
                }
                e.setVelocity(vel);
            }
            e.velocityModified = true;
            e.fallDistance = 0.0F;
            if (e instanceof FallingBlockEntity block) {
                block.timeFalling = 1; // never auto-drop while held
            }

            if (server.getTicks() % 3 == 0) {
                held.world().spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        e.getX(), e.getBodyY(0.5), e.getZ(), 1,
                        e.getWidth() * 0.4, e.getHeight() * 0.3, e.getWidth() * 0.4, 0.02);
            }
            if (server.getTicks() % 24 == 0) {
                held.world().playSound(null, e.getX(), e.getY(), e.getZ(),
                        SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.25F, 1.7F);
            }
        }
    }

    private static void tickThrown() {
        Iterator<Thrown> it = THROWN.iterator();
        while (it.hasNext()) {
            Thrown t = it.next();
            Entity e = t.world.getEntity(t.entityId);
            if (e == null || e.isRemoved()) {
                it.remove();
                continue;
            }

            double impactSpeed = t.lastSpeed;
            Entity owner = t.world.getEntity(t.ownerId);

            // Plow through living targets on the way.
            if (impactSpeed > 0.8) {
                List<Entity> victims = t.world.getOtherEntities(e, e.getBoundingBox().expand(0.35),
                        v -> v instanceof LivingEntity && !v.getUuid().equals(t.ownerId));
                if (!victims.isEmpty()) {
                    float dmg = (float) Math.min(14.0, impactSpeed * 4.0);
                    Vec3d push = e.getVelocity();
                    for (Entity victim : victims) {
                        victim.damage(t.world.getDamageSources().thrown(e, owner), dmg);
                        victim.addVelocity(push.x * 0.5, Math.max(0.15, push.y * 0.5), push.z * 0.5);
                        victim.velocityModified = true;
                    }
                    t.world.playSound(null, e.getX(), e.getY(), e.getZ(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 0.8F, 1.0F);
                    spawnBurst(t.world, e, ParticleTypes.CRIT, 10);
                    it.remove();
                    continue;
                }
            }

            // Crash into a wall or the ground.
            if ((e.horizontalCollision || e.verticalCollision) && impactSpeed > 0.8) {
                if (e instanceof LivingEntity living) {
                    float dmg = (float) Math.min(12.0, impactSpeed * 3.5);
                    living.damage(t.world.getDamageSources().thrown(e, owner), dmg);
                }
                t.world.playSound(null, e.getX(), e.getY(), e.getZ(),
                        SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.3F, 1.5F);
                spawnBurst(t.world, e, ParticleTypes.CRIT, 12);
                it.remove();
                continue;
            }

            t.lastSpeed = e.getVelocity().length();
            if (--t.ticksLeft <= 0 || (e.isOnGround() && t.lastSpeed < 0.15)) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static boolean holdsGun(ServerPlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof GravityGunItem
                || player.getOffHandStack().getItem() instanceof GravityGunItem;
    }

    private static void spawnBurst(ServerWorld world, Entity e, ParticleEffect type, int count) {
        world.spawnParticles(type, e.getX(), e.getBodyY(0.5), e.getZ(), count,
                e.getWidth() * 0.5, e.getHeight() * 0.4, e.getWidth() * 0.5, 0.05);
    }
}
