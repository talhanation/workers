package com.talhanation.workers.world;

import net.minecraft.core.BlockPos;

import java.util.Stack;

public class Tree {
    private final Stack<BlockPos> stackToShear = new Stack<>();
    private final Stack<BlockPos> stackToStrip = new Stack<>();
    private final Stack<BlockPos> stackToBreak = new Stack<>();
    private final String treeType;
    private final BlockPos position;
    private boolean isInWorks;
    private final boolean isDynamicTree;

    public Tree(String treeType, BlockPos position) {
        this(treeType, position, false);
    }

    public Tree(String treeType, BlockPos position, boolean isDynamicTree) {
        this.treeType = treeType;
        this.position = position;
        this.isDynamicTree = isDynamicTree;
    }

    public String toString() {
        return treeType;
    }

    public void addToShear(BlockPos pos) {
        stackToShear.push(pos);
    }

    public void addToStrip(BlockPos pos) {
        stackToStrip.push(pos);
    }

    public void addToBreak(BlockPos pos) {
        stackToBreak.push(pos);
    }

    public Stack<BlockPos> getStackToShear() {
        return stackToShear;
    }

    public Stack<BlockPos> getStackToStrip() {
        return stackToStrip;
    }

    public Stack<BlockPos> getStackToBreak() {
        return stackToBreak;
    }

    public BlockPos getPosition() {
        return position;
    }

    public boolean isEmpty() {
        return stackToShear.isEmpty() && stackToStrip.isEmpty() && stackToBreak.isEmpty();
    }

    public void setInWork(boolean isInWorks) {
        this.isInWorks = isInWorks;
    }

    public boolean isInWorks() {
        return isInWorks;
    }

    public boolean isDynamicTree() {
        return isDynamicTree;
    }
}
