package com.talhanation.workers.compat;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.UUID;

public class FarmersDelight {

    public static final String MOD_ID = "farmersdelight";

    // Knives are matched via tags first (string-based ResourceLocations), with a registry-name fallback.
    private static final TagKey<Item> KNIVES_FD = ItemTags.create(new ResourceLocation(MOD_ID, "tools/knives"));
    private static final TagKey<Item> KNIVES_FORGE = ItemTags.create(new ResourceLocation("forge", "tools/knives"));

    private static final GameProfile FARMER_FAKE_PROFILE = new GameProfile(
            UUID.fromString("b1d4f6a2-7c8e-4d3a-9f0b-1e2c3d4e5f60"), "WorkerFarmer"
    );

    public static boolean isKnife(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(KNIVES_FD) || stack.is(KNIVES_FORGE)) return true;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().contains("knife");
    }

    public static boolean isRiceSeedItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getNamespace().equals(MOD_ID) && id.getPath().equals("rice");
    }

    public static boolean isRicePlantItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getNamespace().equals(MOD_ID) && id.getPath().equals("panicle");
    }

    // The lower, persistent rice block (stays planted and regrows panicles)
    public static boolean isRiceCrop(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getNamespace().equals(MOD_ID) && id.getPath().equals("rice");
    }

    // The upper, harvestable panicles block
    public static boolean isRicePanicles(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getNamespace().equals(MOD_ID) && id.getPath().contains("panicle");
    }

    public static boolean isAnyRice(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getNamespace().equals(MOD_ID) && id.getPath().contains("rice");
    }

    public static boolean isRiceMature(BlockState state) {
        if (!isRicePanicles(state)) return false;
        int max = getMaxAge(state);
        if (max <= 0) return true; // kein age-Property -> Existenz des Panicles-Blocks = erntereif
        return getAge(state) >= max;
    }

    public static boolean isPickableTomato(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return false;
        if (!id.getNamespace().equals(MOD_ID)) return false;
        if (!id.getPath().contains("tomato")) return false;
        int max = getMaxAge(state);
        if (max <= 0) return false;
        return getAge(state) >= max;
    }

    public static boolean canHostRice(BlockState soil) {
        if (soil.is(net.minecraft.tags.BlockTags.DIRT)) return true;
        if (soil.getBlock() instanceof FarmBlock) return true;

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(soil.getBlock());
        return id != null && id.getNamespace().equals(MOD_ID) && id.getPath().contains("rich_soil");
    }

    public static int getAge(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("age") && property instanceof IntegerProperty ageProperty) {
                return state.getValue(ageProperty);
            }
        }
        return 0;
    }

    public static int getMaxAge(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("age") && property instanceof IntegerProperty ageProperty) {
                return ageProperty.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
            }
        }
        return 0;
    }

    public static boolean plantRice(ServerLevel level, BlockPos waterPos, ItemStack rice) {
        FakePlayer fakePlayer = FakePlayerFactory.get(level, FARMER_FAKE_PROFILE);
        fakePlayer.setPos(waterPos.getX() + 0.5, waterPos.getY() + 1, waterPos.getZ() + 0.5);

        ItemStack single = rice.copy();
        single.setCount(1);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, single);

        Vec3 hitVec = new Vec3(waterPos.getX() + 0.5, waterPos.getY() + 1.0, waterPos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, waterPos, false);
        UseOnContext context = new UseOnContext(level, fakePlayer, InteractionHand.MAIN_HAND, single, hitResult);

        InteractionResult result = single.useOn(context);
        return result.consumesAction();
    }
}