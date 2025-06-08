package com.talhanation.workers.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class CropArea {
    public BlockPos centerPos;
    public AABB area;
    public int size;
    public ItemStack seedStack;
    public String name;

    public CropArea(BlockPos centerPos, int size, ItemStack seedStack, String name) {
        this.name = name;
        this.centerPos = centerPos;
        this.size = size;
        this.seedStack = seedStack;
        this.createArea();
    }

    public void createArea() {
        AABB aabb = new AABB(centerPos);
        aabb.inflate(size / 2F);
        this.area = aabb;
    }

    public BlockPos getCenterPos() {
        return centerPos;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putInt("CenterX", centerPos.getX());
        tag.putInt("CenterY", centerPos.getY());
        tag.putInt("CenterZ", centerPos.getZ());
        tag.putInt("Size", size);

        tag.put("SeedStack", seedStack.save(new CompoundTag()));
        tag.putString("Name", this.name);
        return tag;
    }

    public static CropArea fromNBT(CompoundTag tag) {
        BlockPos center = new BlockPos(
                tag.getInt("CenterX"),
                tag.getInt("CenterY"),
                tag.getInt("CenterZ")
        );

        int size = tag.getInt("Size");

        ItemStack seed = ItemStack.of(tag.getCompound("SeedStack"));
        String name = tag.getString("Name");
        return new CropArea(center, size, seed, name);
    }

    public static List<CropArea> listFromNBT(CompoundTag tag){
        if (tag == null) return null;
        List<CropArea> result = new ArrayList<>();

        if (tag.contains("CropAreas", Tag.TAG_LIST)) {
            ListTag list = tag.getList("CropAreas", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                if (t instanceof CompoundTag compoundTag) {
                    result.add(fromNBT(compoundTag));
                }
            }
        }

        return result;
    }

    public static CompoundTag listToNBT(List<CropArea> areas) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();

        for (CropArea area : areas) {
            list.add(area.toNBT());
        }

        tag.put("CropAreas", list);
        return tag;
    }
}

