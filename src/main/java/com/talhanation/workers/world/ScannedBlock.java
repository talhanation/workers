package com.talhanation.workers.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public record ScannedBlock(BlockState state, @Nullable CompoundTag blockEntityTag, BlockPos relativePos) {}
