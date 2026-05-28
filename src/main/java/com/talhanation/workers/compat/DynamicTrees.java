package com.talhanation.workers.compat;

import com.mojang.authlib.GameProfile;
import com.talhanation.workers.entities.LumberjackEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class DynamicTrees {

    private static final int MIN_RADIUS_FOR_GROWN = 8;
    private static final int MAX_FERTILITY = 15;
    private static final int MIN_GROWN_RADIUS_COUNT = 2;
    private static final int MAX_HEIGHT_FALLBACK = 12;

    private static final GameProfile LUMBERJACK_FAKE_PROFILE = new GameProfile(
            UUID.fromString("a8c7e2f1-3b4d-5e6f-7a8b-9c0d1e2f3a4b"), "WorkerLumberjack"
    );
    @Nullable private static Class<?> seedClass = null;
    @Nullable private static Method doPlantingMethod = null;
    private static boolean reflectionInitialized = false;

    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        try {
            seedClass = Class.forName("com.ferreusveritas.dynamictrees.item.Seed");
            doPlantingMethod = seedClass.getMethod("doPlanting",
                    Level.class,
                    BlockPos.class,
                    net.minecraft.world.entity.player.Player.class,
                    ItemStack.class
            );
        } catch (Exception e) {
            seedClass = null;
            doPlantingMethod = null;
        }
    }

    public static boolean isDynamicTreesSeed(ItemStack itemStack) {
        initReflection();
        if (seedClass == null) return false;
        return seedClass.isInstance(itemStack.getItem());
    }

    public static boolean plantSeed(Level level, BlockPos pos, ItemStack seedStack) {
        initReflection();
        if (doPlantingMethod == null || !seedClass.isInstance(seedStack.getItem())) return false;
        try {
            Object result = doPlantingMethod.invoke(seedStack.getItem(), level, pos, null, seedStack);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isDynamicTreesBranch(Block block) {
        String id = block.getDescriptionId();
        if (id.contains("dynamictrees")) {
            return id.contains("_branch");
        }
        return false;
    }

    public static int getDynamicTreeRadius(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("radius")) {
                Object value = state.getValue(property);
                if (value instanceof Integer radius) {
                    return radius;
                }
            }
        }
        return 0;
    }

    public static boolean isDynamicTreesRootySoil(Block block) {
        String id = block.getDescriptionId();
        if (id.contains("dynamictrees")) {
            return id.contains("rooty");
        }
        return false;
    }

    public static int getSoilFertility(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("fertility")) {
                Object value = state.getValue(property);
                if (value instanceof Integer fertility) {
                    return fertility;
                }
            }
        }
        return 0;
    }

    public static boolean hasMaxFertility(BlockState state) {
        return getSoilFertility(state) >= MAX_FERTILITY;
    }

    public static boolean isGrownDynamicTree(List<BlockPos> branchPositions, Level level) {
        int grownRadiusCount = 0;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (BlockPos pos : branchPositions) {
            BlockState state = level.getBlockState(pos);
            if (getDynamicTreeRadius(state) >= MIN_RADIUS_FOR_GROWN) {
                grownRadiusCount++;
            }
            if (pos.getY() < minY) minY = pos.getY();
            if (pos.getY() > maxY) maxY = pos.getY();
        }

        if (grownRadiusCount >= MIN_GROWN_RADIUS_COUNT) return true;

        if (minY == Integer.MAX_VALUE) return false;
        return (maxY - minY) >= MAX_HEIGHT_FALLBACK;
    }

    public static void fellTree(ServerLevel level, BlockPos pos, LumberjackEntity lumberjack) {
        FakePlayer fakePlayer = FakePlayerFactory.get(level, LUMBERJACK_FAKE_PROFILE);
        fakePlayer.setPos(lumberjack.getX(), lumberjack.getY(), lumberjack.getZ());

        ItemStack tool = lumberjack.getMainHandItem().copy();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool);

        fakePlayer.gameMode.destroyBlock(pos);
    }
}