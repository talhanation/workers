package com.talhanation.workers.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public record ScannedBlock(BlockState state, Direction facing, @Nullable CompoundTag blockEntityTag, BlockPos relativePos) {}
