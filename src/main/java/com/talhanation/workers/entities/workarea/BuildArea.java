package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.BuildEvent;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.BuildAreaScreen;
import com.talhanation.workers.entities.BuilderEntity;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.network.MessageToClientOpenWorkAreaScreen;
import com.talhanation.workers.world.BuildBlock;
import com.talhanation.workers.world.BuildBlockParse;
import com.talhanation.workers.world.BuildingPrice;
import com.talhanation.workers.world.Foundation;
import com.talhanation.workers.config.WorkersServerConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

public class BuildArea extends AbstractWorkAreaEntity {
    public static final EntityDataAccessor<CompoundTag> STRUCTURE = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.COMPOUND_TAG);
    public static final EntityDataAccessor<Boolean> ALWAYS_SHOW_PROJECTION = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.BOOLEAN);
    /** Currency price of the full blueprint for the GUI (CURRENCY payment mode). -1 = contains blocked materials. */
    public static final EntityDataAccessor<Integer> CURRENCY_PRICE = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.INT);
    /** Foundation mode: level the ground of the footprint (cut above, fill below), see {@link Foundation}. */
    public static final EntityDataAccessor<Boolean> FOUNDATION = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.BOOLEAN);
    /** Fill block for the foundation. Empty = picked from the ground of each column. */
    public static final EntityDataAccessor<ItemStack> FILL_ITEM = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.ITEM_STACK);
    /** Status of the build for the GUI and the Governor, see {@link BuildStatus}. Not saved: a build starts again after loading. */
    public static final EntityDataAccessor<Integer> BUILD_STATUS = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> BUILDER_NAME = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> BLOCKS_PLACED = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> BLOCKS_TOTAL = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.INT);
    /** Item ids the builder waits for (WAITING_FOR_MATERIALS). */
    public static final EntityDataAccessor<CompoundTag> MISSING_MATERIALS = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.COMPOUND_TAG);
    private static final int BUILDER_CHECK_INTERVAL = 100;
    /** Server-side: the builder working on this area, null = none. */
    @Nullable
    private UUID builderUUID;
    /** Server-side flag: true once the clearing step (cut of the foundation) has been completed for the current build. */
    public boolean freeAreaDone;
    /** Server-side flag: the current build was paid with currency, the builder needs no materials. */
    public boolean paidWithCurrency;
    /** Server-side: spawn the work areas of the template when the build is done. The Governor turns it off for villager houses. */
    public boolean spawnWorkAreas = true;
    /** Server-side: set by the Governor, the claim this building belongs to (null = built by a player). */
    @Nullable
    public UUID governorClaim;
    /** stackToPlace is not saved: a paid build is continued once after loading, without charging again. */
    private boolean restorePaidBuild;
    public Stack<BlockPos> stackToBreak = new Stack<>();
    public Stack<BlockPos> stackToFree = new Stack<>();
    public Stack<BuildBlock> stackToPlace = new Stack<>();
    public Stack<BuildBlock> stackToPlaceMultiBlock = new Stack<>();
    /** Server-side cache: base material -> count still needed for stackToPlace. null = dirty. */
    @Nullable
    private Map<Item, Integer> remainingMaterials;
    /** Server-side cache: finished item (red_carpet) -> count still needed for stackToPlace. null = dirty. */
    private Map<Item, Integer> remainingFinishedItems;
    public BuildArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STRUCTURE, new CompoundTag());
        builder.define(ALWAYS_SHOW_PROJECTION, false);
        builder.define(CURRENCY_PRICE, 0);
        builder.define(FOUNDATION, true);
        builder.define(FILL_ITEM, ItemStack.EMPTY);
        builder.define(BUILD_STATUS, BuildStatus.NONE.ordinal());
        builder.define(BUILDER_NAME, "");
        builder.define(BLOCKS_PLACED, 0);
        builder.define(BLOCKS_TOTAL, 0);
        builder.define(MISSING_MATERIALS, new CompoundTag());
    }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setStructureNBT(tag.getCompound("structureNBT"));
        this.freeAreaDone = tag.getBoolean("freeAreaDone");
        this.setAlwaysShowProjection(tag.getBoolean("alwaysShowProjection"));
        this.setCurrencyPrice(tag.getInt("currencyPrice"));
        this.paidWithCurrency = tag.getBoolean("paidWithCurrency");
        this.restorePaidBuild = this.paidWithCurrency;
        // on by default, also for areas saved before the setting existed
        this.setFoundation(!tag.contains("foundation") || tag.getBoolean("foundation"));
        this.spawnWorkAreas = !tag.contains("spawnWorkAreas") || tag.getBoolean("spawnWorkAreas");
        this.governorClaim = tag.hasUUID("governorClaim") ? tag.getUUID("governorClaim") : null;
        this.setFillItem(tag.contains("fillItem") ? ItemStack.parseOptional(this.registryAccess(), tag.getCompound("fillItem")) : ItemStack.EMPTY);
    }
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("structureNBT", this.getStructureNBT());
        tag.putBoolean("freeAreaDone", this.freeAreaDone);
        tag.putBoolean("alwaysShowProjection", this.getAlwaysShowProjection());
        tag.putInt("currencyPrice", this.getCurrencyPrice());
        tag.putBoolean("paidWithCurrency", this.paidWithCurrency);
        tag.putBoolean("foundation", this.getFoundation());
        tag.putBoolean("spawnWorkAreas", this.spawnWorkAreas);
        if (this.governorClaim != null) tag.putUUID("governorClaim", this.governorClaim);
        if (!this.getFillItem().isEmpty()) tag.put("fillItem", this.getFillItem().save(this.registryAccess()));
    }

    public int getCurrencyPrice() {
        return this.entityData.get(CURRENCY_PRICE);
    }

    public void setCurrencyPrice(int price) {
        this.entityData.set(CURRENCY_PRICE, price);
    }

    public boolean isPaidWithCurrency() {
        return this.paidWithCurrency;
    }

    public void setPaidWithCurrency(boolean paid) {
        this.paidWithCurrency = paid;
    }

    public boolean getFreeAreaDone() {
        return this.freeAreaDone;
    }

    public void setFreeAreaDone(boolean done) {
        this.freeAreaDone = done;
    }

    @Override
    public Item getRenderItem() {
        return Items.IRON_SHOVEL;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new BuildAreaScreen(this, player);
    }

    @Override
    public void tick() {
        super.tick();
        if(!this.getCommandSenderWorld().isClientSide() && this.tickCount % BUILDER_CHECK_INTERVAL == 0) this.checkBuilder();
        if(isDone()) this.remove(RemovalReason.DISCARDED);
        else if(restorePaidBuild && !this.getCommandSenderWorld().isClientSide() && this.tickCount > 20){
            restorePaidBuild = false;
            //keep freeAreaDone, otherwise the builder would clear the half built house again
            boolean freeDone = this.freeAreaDone;
            this.setStartBuild(false);
            this.freeAreaDone = freeDone;
        }
    }

    public void setStructureNBT(CompoundTag tag){
        this.entityData.set(STRUCTURE, tag);
    }

    public CompoundTag getStructureNBT(){
        return this.entityData.get(STRUCTURE);
    }

    public void setStartBuild(boolean isCreative) {
        stackToPlace.clear();
        stackToPlaceMultiBlock.clear();
        this.remainingMaterials = null;
        this.remainingFinishedItems = null;
        this.freeAreaDone = false;

        CompoundTag tag = getStructureNBT();
        if (tag == null || !tag.contains("blocks", Tag.TAG_LIST)) return;
        int width = tag.getInt("width");
        String dir = tag.getString("facing");
        Direction scanFacing = Direction.byName(dir);
        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);
        BlockPos origin = this.getOriginPos();
        Direction facing = this.getFacing();
        Direction right = facing.getClockWise();
        int rotationSteps = (4 + facing.get2DDataValue() - scanFacing.get2DDataValue()) % 4;

        if (this.getFoundation()) this.startFoundation(isCreative);

        for (Tag t : blockList) {
            CompoundTag blockTag = (CompoundTag) t;

            int relX = blockTag.getInt("x");
            int relY = blockTag.getInt("y");
            int relZ = blockTag.getInt("z");

            BlockPos worldPos = origin
                    .relative(facing, relZ)
                    .relative(right, width - 1 - relX)
                    .above(relY);

            CompoundTag stateTag = blockTag.getCompound("state");
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag);
            BlockState rotatedState = rotateBlockState(state, rotationSteps);

            if (isCreative) {
                this.getCommandSenderWorld().setBlock(worldPos, rotatedState, 3);
            } else {
                if (isMultiBlockSecondary(rotatedState)) {
                    this.stackToPlaceMultiBlock.push(new BuildBlock(worldPos, rotatedState));
                } else {
                    BlockState levelState = this.getCommandSenderWorld().getBlockState(worldPos);
                    if (rotatedState != null && !statesMatch(levelState, rotatedState)) {
                        this.stackToPlace.push(new BuildBlock(worldPos, rotatedState));
                    }
                }
            }
        }

        // For creative (Place), spawn entities immediately after all blocks are placed
        if (isCreative && this.spawnWorkAreas) {
            spawnScannedEntities(tag, scanFacing, facing, origin, width);
        }

        // status: a builder can start now (Place in creative is already done)
        this.entityData.set(BLOCKS_TOTAL, stackToPlace.size() + stackToPlaceMultiBlock.size());
        this.updateProgress();
        if (isCreative || (stackToPlace.isEmpty() && stackToPlaceMultiBlock.isEmpty())) {
            this.setBuildStatus(BuildStatus.NONE);
        }
        else {
            if (!this.getBuildStatus().isWorking()) this.setBuildStatus(BuildStatus.WAITING_FOR_BUILDER);
            if (!this.getCommandSenderWorld().isClientSide()) NeoForge.EVENT_BUS.post(new BuildEvent.Started(this));
        }
    }

    /**
     * Spawns work-area entities stored in the structure NBT.
     * Rotates their facing from scan direction to build direction and transfers owner/team.
     */
    private void spawnScannedEntities(CompoundTag tag, Direction scanFacing, Direction buildFacing, BlockPos origin, int width) {
        if (!(this.getCommandSenderWorld() instanceof ServerLevel serverLevel)) return;
        if (!tag.contains("entities", Tag.TAG_LIST)) return;

        ListTag entityList = tag.getList("entities", Tag.TAG_COMPOUND);
        Direction buildRight = buildFacing.getClockWise();
        int rotSteps = ((buildFacing.get2DDataValue() - scanFacing.get2DDataValue()) % 4 + 4) % 4;

        for (Tag t : entityList) {
            CompoundTag entityTag = (CompoundTag) t;
            String typeId = entityTag.getString("entity_type");
            int relX = entityTag.getInt("x");
            int relY = entityTag.getInt("y");
            int relZ = entityTag.getInt("z");
            int scanFacingVal = entityTag.getInt("facing");

            ResourceLocation rl = ResourceLocation.parse(typeId);
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(rl);
            if (entityType == null) continue;

            BlockPos worldPos = origin
                    .relative(buildFacing, relZ)
                    .relative(buildRight, width - 1 - relX)
                    .above(relY);

            Entity entity = entityType.create(serverLevel);
            if (entity == null) continue;

            entity.moveTo(worldPos.getX() + 0.5, worldPos.getY() + 1.0, worldPos.getZ() + 0.5, 0, 0);

            if (entity instanceof AbstractWorkAreaEntity wa) {
                Direction entityFacing = Direction.from2DDataValue(scanFacingVal);
                // rotate clockwise rotSteps times
                for (int i = 0; i < rotSteps; i++) entityFacing = entityFacing.getClockWise();
                wa.setFacing(entityFacing);

                // Restore stored dimensions, swapping width↔depth for 90° / 270° rotations
                if (entityTag.contains("wa_width")) {
                    int waW = entityTag.getInt("wa_width");
                    int waH = entityTag.getInt("wa_height");
                    int waD = entityTag.getInt("wa_depth");
                    // width is perpendicular to facing, depth is parallel — swapped on odd rotations
                    if (rotSteps % 2 == 1) { int tmp = waW; waW = waD; waD = tmp; }
                    wa.setWidthSize(waW);
                    wa.setHeightSize(waH);
                    wa.setDepthSize(waD);
                }

                if (this.getPlayerUUID() != null) wa.setPlayerUUID(this.getPlayerUUID());
                String ownerName = this.getPlayerName();
                if (ownerName != null && !ownerName.isEmpty()) wa.setPlayerName(ownerName);
                String team = this.getTeamStringID();
                if (team != null && !team.isEmpty()) wa.setTeamStringID(team);

                wa.setCustomName(Component.literal(""));
            }

            serverLevel.addFreshEntity(entity);
        }
    }

    /**
     * Returns true for secondary parts of multi-block structures (upper door/plant half, bed head).
     * These are placed in a dedicated second stage without consuming extra items.
     */
    public static boolean isMultiBlockSecondary(BlockState state) {
        if (state == null) return false;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)) {
            return state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
        }
        return false;
    }

    public static BlockState rotateBlockState(BlockState state, int steps) {
        if (state == null) return null;
        steps = ((steps % 4) + 4) % 4;
        return switch (steps) {
            case 1 -> state.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
            case 2 -> state.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_180);
            case 3 -> state.rotate(net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90);
            default -> state;
        };
    }


    public void scanBreakArea() {
        stackToBreak.clear();
        Level level = this.getCommandSenderWorld();

        BlockPos.betweenClosedStream(getArea()).forEach(pos -> {
            BlockState buildingState = this.getStateFromPos(pos);
            BlockState levelState = level.getBlockState(pos);

            if (buildingState != null && !levelState.isAir()
                    && !statesMatch(levelState, buildingState)
                    && !canDirectlyReplace(levelState, buildingState)) {
                stackToBreak.push(pos.immutable());
            }
        });
    }

    public void scanFreeArea() {
        stackToFree.clear();
        Level level = this.getCommandSenderWorld();

        java.util.List<BlockPos> found = new java.util.ArrayList<>();
        if (this.getFoundation()) {
            // the cut of the foundation, it also reaches above the area (hills)
            found.addAll(Foundation.computeCut(level, collectStructureStates(), WorkersServerConfig.FoundationMaxCutHeight.get()));
        }

        // Sort ascending by Y, then push — stack pops highest Y first → top-down mining.
        found.sort(java.util.Comparator.comparingInt(BlockPos::getY));
        for (BlockPos p : found) stackToFree.push(p);
    }

    /** True if the builder has to clear something before building: the cut of the foundation. */
    public boolean needsClearing() {
        return this.getFoundation();
    }

    public boolean getFoundation() {
        return this.entityData.get(FOUNDATION);
    }

    public void setFoundation(boolean foundation) {
        this.entityData.set(FOUNDATION, foundation);
    }

    public ItemStack getFillItem() {
        return this.entityData.get(FILL_ITEM);
    }

    /** Fill block for the foundation, an empty stack (or no block item) means: chosen from the ground. */
    public void setFillItem(ItemStack stack) {
        this.entityData.set(FILL_ITEM, stack == null ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    /** Fill blocks the player can choose for the foundation, in this order. Empty = auto (from the ground). */
    public static final List<Item> FILL_PALETTE = List.of(Items.DIRT, Items.SAND, Items.STONE, Items.COBBLESTONE, Items.RED_SAND, Items.SNOW_BLOCK, Items.ICE);

    /** Next fill block of the palette: auto, dirt, sand, ... ice, auto. Back = the other way round. */
    public static ItemStack cycleFillItem(ItemStack current, boolean back) {
        int index = current == null || current.isEmpty() ? -1 : FILL_PALETTE.indexOf(current.getItem());
        int size = FILL_PALETTE.size() + 1; // + auto
        int next = Math.floorMod(index + 1 + (back ? -1 : 1), size) - 1;
        return next < 0 ? ItemStack.EMPTY : new ItemStack(FILL_PALETTE.get(next));
    }

    /** Only auto or a block of the palette (the server does not trust the client). */
    public static boolean isValidFillItem(ItemStack stack) {
        return stack == null || stack.isEmpty() || FILL_PALETTE.contains(stack.getItem());
    }

    @Nullable
    private BlockState getFillOverride() {
        ItemStack stack = this.getFillItem();
        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) return blockItem.getBlock().defaultBlockState();
        return null;
    }

    /**
     * Foundation at build start. Creative: cut and fill right away.
     * Builder: the fill goes into stackToPlace (lowest level first, so before the building),
     * the cut is done in the free area step ({@link #scanFreeArea()}).
     */
    private void startFoundation(boolean isCreative) {
        Level level = this.getCommandSenderWorld();
        Map<BlockPos, BlockState> structure = collectStructureStates();
        if (structure.isEmpty()) return;

        if (isCreative) {
            for (BlockPos pos : Foundation.computeCut(level, structure, WorkersServerConfig.FoundationMaxCutHeight.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        for (BuildBlock fill : Foundation.computeFill(level, structure, getFillOverride(), WorkersServerConfig.FoundationMaxFillDepth.get())) {
            if (isCreative) {
                level.setBlock(fill.getPos(), fill.getState(), 3);
            }
            else if (!statesMatch(level.getBlockState(fill.getPos()), fill.getState())) {
                this.stackToPlace.push(fill);
            }
        }
    }

    /** World position -> rotated state of every block of the structure, same placement as {@link #setStartBuild}. */
    public Map<BlockPos, BlockState> collectStructureStates() {
        Map<BlockPos, BlockState> map = new HashMap<>();
        CompoundTag tag = getStructureNBT();
        if (tag == null || !tag.contains("blocks", Tag.TAG_LIST)) return map;

        int width = tag.getInt("width");
        Direction scanFacing = Direction.byName(tag.getString("facing"));
        if (scanFacing == null) scanFacing = Direction.SOUTH;
        BlockPos origin = this.getOriginPos();
        Direction facing = this.getFacing();
        Direction right = facing.getClockWise();
        int rotationSteps = (4 + facing.get2DDataValue() - scanFacing.get2DDataValue()) % 4;

        for (Tag t : tag.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag blockTag = (CompoundTag) t;
            BlockPos worldPos = origin
                    .relative(facing, blockTag.getInt("z"))
                    .relative(right, width - 1 - blockTag.getInt("x"))
                    .above(blockTag.getInt("y"));
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), blockTag.getCompound("state"));
            map.put(worldPos, rotateBlockState(state, rotationSteps));
        }
        return map;
    }

    public boolean getAlwaysShowProjection() {
        return this.entityData.get(ALWAYS_SHOW_PROJECTION);
    }

    public void setAlwaysShowProjection(boolean alwaysShowProjection) {
        this.entityData.set(ALWAYS_SHOW_PROJECTION, alwaysShowProjection);
    }

    public static boolean canDirectlyReplace(BlockState levelState, BlockState buildingState) {
        if (levelState.isAir()) return true;
        if (buildingState.is(Blocks.DIRT_PATH)) {
            return isPathGround(levelState);
        }
        return false;
    }

    /** Ground a shovel turns into dirt path (like a player does it). */
    public static boolean isPathGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.FARMLAND);
    }

    public boolean statesMatch(BlockState levelState, BlockState buildingState) {
        if(buildingState == null) return true;
        if(levelState.is(Blocks.GRASS_BLOCK) && buildingState.is(Blocks.DIRT)) return true;
        if(levelState.is(Blocks.DIRT) && buildingState.is(Blocks.GRASS_BLOCK)) return true;



        return levelState.equals(buildingState);
    }

    public List<ItemStack> getRequiredMaterials(){
        return this.getRequiredMaterials(this.getStructureNBT());
    }

    public List<ItemStack> getRequiredMaterials(CompoundTag tag) {
        Map<Item, Integer> materialMap = new HashMap<>();
        List<ItemStack> stacks = new ArrayList<>();

        if (tag == null || !tag.contains("blocks", Tag.TAG_LIST)) return stacks;

        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (Tag t : blockList) {
            CompoundTag blockTag = (CompoundTag) t;

            CompoundTag stateTag = blockTag.getCompound("state");
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag);

            Block block = state.getBlock();
            Item item = BuildBlockParse.parseBlock(block, this.getCommandSenderWorld()).getItem();

            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                if (half != DoubleBlockHalf.LOWER) {
                    continue;
                }
            }

            if (state.hasProperty(BlockStateProperties.BED_PART)) {
                BedPart part = state.getValue(BlockStateProperties.BED_PART);
                if (part != BedPart.FOOT) {
                    continue;
                }
            }


            if (item instanceof BlockItem) {
                materialMap.merge(item, 1, Integer::sum);
            }
        }

        for (Map.Entry<Item, Integer> entry : materialMap.entrySet()) {
            ItemStack stack = new ItemStack(entry.getKey());
            if (stack.isEmpty()) continue;

            stack.setCount(entry.getValue());
            stacks.add(stack);
        }

        return stacks;
    }

    /**
     * Base materials still needed for the blocks that are NOT placed yet (stackToPlace).
     * Upper door halves / bed heads live in stackToPlaceMultiBlock and cost nothing,
     * same as in {@link #getRequiredMaterials(CompoundTag)}.
     */
    public Map<Item, Integer> getRemainingMaterialMap() {
        if (remainingMaterials == null) {
            remainingMaterials = new HashMap<>();
            for (BuildBlock buildBlock : stackToPlace) {
                // dirt path on grass/dirt is only flattened with a shovel, no dirt needed
                if (buildBlock.getState().is(Blocks.DIRT_PATH) && isPathGround(this.getCommandSenderWorld().getBlockState(buildBlock.getPos()))) continue;

                Item item = BuildBlockParse.parseBlock(buildBlock.getState().getBlock(), this.getCommandSenderWorld()).getItem();
                if (item instanceof BlockItem) {
                    remainingMaterials.merge(item, 1, Integer::sum);
                }
            }
        }
        return remainingMaterials;
    }

    public List<ItemStack> getRemainingMaterials() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : getRemainingMaterialMap().entrySet()) {
            ItemStack stack = new ItemStack(entry.getKey());
            if (stack.isEmpty()) continue;

            stack.setCount(entry.getValue());
            stacks.add(stack);
        }
        return stacks;
    }

    public int getRemainingMaterialCount(Item item) {
        return getRemainingMaterialMap().getOrDefault(item, 0);
    }

    /**
     * Finished items (red_carpet) of the blocks not placed yet, whose base material (red_wool) is consumed.
     * The builder may use them instead of the base material, see {@link BuildBlockParse#getFinishedItem}.
     */
    public Map<Item, Integer> getRemainingFinishedItemMap() {
        if (remainingFinishedItems == null) {
            remainingFinishedItems = new HashMap<>();
            for (BuildBlock buildBlock : stackToPlace) {
                Item item = BuildBlockParse.getFinishedItem(buildBlock.getState().getBlock(), this.getCommandSenderWorld());
                if (item != null) remainingFinishedItems.merge(item, 1, Integer::sum);
            }
        }
        return remainingFinishedItems;
    }

    public int getRemainingFinishedItemCount(Item item) {
        return getRemainingFinishedItemMap().getOrDefault(item, 0);
    }

    /** Price of the full blueprint (shown in the GUI). The actual charge only counts missing blocks. */
    public int calculateCurrencyPrice(CompoundTag tag) {
        Map<Item, Integer> materials = new HashMap<>();
        for (ItemStack stack : this.getRequiredMaterials(tag)) {
            materials.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return BuildingPrice.calculate(materials);
    }

    public void cancelBuild() {
        stackToPlace.clear();
        stackToPlaceMultiBlock.clear();
        this.remainingMaterials = null;
        this.remainingFinishedItems = null;
        this.paidWithCurrency = false;
        this.entityData.set(BLOCKS_TOTAL, 0);
        this.updateProgress();
        this.setBuilder(null);
        this.setBuildStatus(BuildStatus.NONE);
    }

    @Nullable
    public BlockState getStateFromPos(BlockPos blockPos) {
        for (BuildBlock buildBlock : stackToPlace) {
            if (buildBlock.getPos().equals(blockPos)) {
                return buildBlock.getState();
            }
        }
        for (BuildBlock buildBlock : stackToPlaceMultiBlock) {
            if (buildBlock.getPos().equals(blockPos)) {
                return buildBlock.getState();
            }
        }
        return null;
    }

    @Nullable
    public BlockState getStateFromMultiBlockPos(BlockPos blockPos) {
        for (BuildBlock buildBlock : stackToPlaceMultiBlock) {
            if (buildBlock.getPos().equals(blockPos)) {
                return buildBlock.getState();
            }
        }
        return null;
    }

    /** Second part of the block at primaryPos that is still to place: upper door half above, bed head in front of the foot. */
    @Nullable
    public BlockState findPairedMultiBlockState(BlockPos primaryPos) {
        BlockPos paired = findPairedMultiBlockPos(primaryPos);
        return paired == null ? null : getStateFromMultiBlockPos(paired);
    }

    @Nullable
    public BlockPos findPairedMultiBlockPos(BlockPos primaryPos) {
        BlockPos paired = getSecondaryPos(primaryPos, getStateFromPos(primaryPos));
        for (BuildBlock bb : stackToPlaceMultiBlock) {
            if (bb.getPos().equals(paired)) return paired;
        }
        return null;
    }

    /** Where the secondary part of a multi-block lies: the bed head in facing direction of the foot, otherwise above. */
    private static BlockPos getSecondaryPos(BlockPos primaryPos, @Nullable BlockState primary) {
        if (primary != null && primary.hasProperty(BlockStateProperties.BED_PART) && primary.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return primaryPos.relative(primary.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return primaryPos.above();
    }

    /**
     * The primary part (bed foot, lower door half) that belongs to a secondary part, or null if the block
     * is no multi-block. Used to check that a secondary part is never placed alone.
     */
    @Nullable
    public static BuildBlock getPrimaryFor(BlockPos secondaryPos, BlockState secondary) {
        if (secondary.hasProperty(BlockStateProperties.BED_PART) && secondary.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = secondary.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return new BuildBlock(secondaryPos.relative(facing.getOpposite()), secondary.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
        }
        if (secondary.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return new BuildBlock(secondaryPos.below(), secondary.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        }
        return null;
    }

    /** Puts a block back on the list, e.g. the half of a bed or door a player has broken. */
    public void addBuildBlockToPlace(BuildBlock buildBlock) {
        removeBuildBlockToPlace(buildBlock.getPos());
        stackToPlace.push(buildBlock);
        this.remainingMaterials = null;
        this.remainingFinishedItems = null;
        this.updateProgress();
    }

    public void removeBuildBlockToPlace(BlockPos blockPos) {
        if (stackToPlace.removeIf(buildBlock -> buildBlock.getPos().equals(blockPos))) {
            this.remainingMaterials = null;
            this.remainingFinishedItems = null;
            this.updateProgress();
        }
    }

    public void removeMultiBlockToPlace(BlockPos blockPos) {
        if (stackToPlaceMultiBlock.removeIf(buildBlock -> buildBlock.getPos().equals(blockPos))) {
            this.updateProgress();
        }
    }

    // ── Build status (GUI and Governor) ───────────────────────────────────────

    public BuildStatus getBuildStatus() {
        return BuildStatus.fromIndex(this.entityData.get(BUILD_STATUS));
    }

    /** Server: changes the status and fires {@link BuildEvent.StatusChanged}. Missing materials only stay while waiting for them. */
    public void setBuildStatus(BuildStatus status) {
        if (status != BuildStatus.WAITING_FOR_MATERIALS) this.setMissingMaterials(List.of());

        BuildStatus old = this.getBuildStatus();
        if (old == status) return;
        this.entityData.set(BUILD_STATUS, status.ordinal());
        if (!this.getCommandSenderWorld().isClientSide()) NeoForge.EVENT_BUS.post(new BuildEvent.StatusChanged(this, old, status));
    }

    /** Server: the builder that works on this area, null = none. */
    @Nullable
    public UUID getBuilderUUID() {
        return this.builderUUID;
    }

    /** Name of the builder for the GUI, empty = none. */
    public String getBuilderName() {
        return this.entityData.get(BUILDER_NAME);
    }

    public void setBuilder(@Nullable BuilderEntity builder) {
        this.builderUUID = builder != null ? builder.getUUID() : null;
        this.entityData.set(BUILDER_NAME, builder != null ? builder.getName().getString() : "");
    }

    /** Blocks placed of the current build (doors and beds count each half). */
    public int getPlacedBlocks() {
        return this.entityData.get(BLOCKS_PLACED);
    }

    public int getTotalBlocks() {
        return this.entityData.get(BLOCKS_TOTAL);
    }

    /** 0 to 1. */
    public float getProgress() {
        int total = this.getTotalBlocks();
        return total <= 0 ? 0F : (float) this.getPlacedBlocks() / total;
    }

    private void updateProgress() {
        int remaining = stackToPlace.size() + stackToPlaceMultiBlock.size();
        int total = Math.max(this.getTotalBlocks(), remaining);
        this.entityData.set(BLOCKS_TOTAL, total);
        this.entityData.set(BLOCKS_PLACED, total - remaining);
    }

    /** Materials the builder waits for, only filled while the status is WAITING_FOR_MATERIALS. */
    public List<ItemStack> getMissingMaterials() {
        List<ItemStack> items = new ArrayList<>();
        ListTag list = this.entityData.get(MISSING_MATERIALS).getList("items", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(list.getString(i)));
            if (item != null && item != Items.AIR) items.add(new ItemStack(item));
        }
        return items;
    }

    public void setMissingMaterials(Collection<Item> items) {
        ListTag list = new ListTag();
        for (Item item : items) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) list.add(StringTag.valueOf(id.toString()));
        }
        CompoundTag tag = new CompoundTag();
        tag.put("items", list);
        if (!tag.equals(this.entityData.get(MISSING_MATERIALS))) this.entityData.set(MISSING_MATERIALS, tag);
    }

    /** The builder died, left for another area or is unloaded: the build waits for a builder again. */
    private void checkBuilder() {
        if (this.builderUUID == null || !this.getBuildStatus().isWorking()) return;
        if (!(this.getCommandSenderWorld() instanceof ServerLevel serverLevel)) return;

        Entity entity = serverLevel.getEntity(this.builderUUID);
        if (entity instanceof BuilderEntity builder && builder.isAlive() && builder.currentBuildArea == this) return;

        this.setBuilder(null);
        boolean started = !stackToPlace.isEmpty() || !stackToPlaceMultiBlock.isEmpty();
        this.setBuildStatus(started ? BuildStatus.WAITING_FOR_BUILDER : BuildStatus.NONE);
    }

    public enum BuildStatus {
        /** No build started (no structure loaded or Build not pressed). */
        NONE("Not started", 0xAAAAAA),
        /** Started, but no builder works on it (none free, or he died / left). */
        WAITING_FOR_BUILDER("Waiting for a builder", 0xFFAA00),
        /** The builder clears the ground (cut of the foundation). */
        CLEARING("Clearing the ground", 0xFFFF55),
        /** The builder places blocks (or fetches materials for them). */
        BUILDING("Building", 0x55FF55),
        /** The builder can not go on, see {@link #getMissingMaterials()}. */
        WAITING_FOR_MATERIALS("Waiting for materials", 0xFF5555),
        /** All blocks are placed, the area is removed right after. */
        DONE("Done", 0x55FF55);

        private final String fallbackName;
        private final int color;

        BuildStatus(String fallbackName, int color) {
            this.fallbackName = fallbackName;
            this.color = color;
        }

        /** A builder is assigned and busy with the build. */
        public boolean isWorking() {
            return this == CLEARING || this == BUILDING || this == WAITING_FOR_MATERIALS;
        }

        public Component getDisplayName() {
            return Component.translatableWithFallback("gui.workers.buildStatus." + this.name().toLowerCase(Locale.ROOT), this.fallbackName);
        }

        public int getColor() {
            return this.color;
        }

        public static BuildStatus fromIndex(int index) {
            return index >= 0 && index < values().length ? values()[index] : NONE;
        }
    }
}