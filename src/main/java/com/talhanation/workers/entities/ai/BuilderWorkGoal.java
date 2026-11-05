package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.BuilderEntity;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.world.BuildBlock;
import com.talhanation.workers.world.BuildBlockParse;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.core.registries.BuiltInRegistries;
import com.talhanation.workers.BuildEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class BuilderWorkGoal extends Goal {

    public BuilderEntity builderEntity;
    public State state;
    public String errorMessage;
    public boolean errorMessageDone;
    public BlockPos blockPos;
    public Stack<BlockPos> stackToBreak;
    public Stack<BlockPos> stackToFree;
    public Stack<BlockPos> stackToPlace;
    public int minBuildHeight;

    // Feature: look-ahead prefetching. Instead of fetching a single material and
    // walking back to storage for the next one, the builder pulls up to this many
    // DISTINCT materials in one storage trip, sized to the whole structure's needs.
    private static final int PREFETCH_MATERIAL_BUDGET = 4;
    private static final int PREFETCH_STACK_LIMIT = 64;

    // Materials the storages did not have. Blocks of these items are skipped and the builder
    // goes on with the next blocks/levels. After MISSING_RETRY ticks he asks the storages again.
    private static final int MISSING_RETRY = 1200;
    private static final int WAIT_CHECK_INTERVAL = 200;
    private final Map<Item, Long> missingItems = new HashMap<>();
    private final Set<Item> requestedItems = new HashSet<>();
    // Owner messages of the builder: always sent (not only once a day), the same text at most every NOTIFY_REPEAT ticks
    private static final int NOTIFY_REPEAT = 6000;
    @Nullable private String lastNotifyText;
    private long lastNotifyTime;
    /** Only true when nothing else is left: blocks without support are placed anyway. */
    private boolean forcePlace;
    private int waitTime;
    @Nullable private UUID lastAreaUUID;

    // The builder must not build himself in: blocks are never placed into an entity (he steps aside),
    // and a block that would close his last way out of the area is placed from outside.
    private static final int ESCAPE_TIMEOUT = 300;
    private static final int ENCLOSE_SEARCH_LIMIT = 2048;
    @Nullable private BlockPos escapePos;
    private int escapeTicks;
    @Nullable private BlockPos outsidePlacePos;

    // Where the builder stands while placing: never on a block of the building and outside the area if
    // possible, so he can not build himself in. He places only from there (WORK_REACH blocks around).
    private static final int WORK_REACH = 5;
    private static final int WORK_SPOT_TIMEOUT = 200;
    @Nullable private BlockPos workSpot;
    @Nullable private BlockPos workTarget;
    private int workSpotTicks;
    private boolean walkingToWork;
    @Nullable private Set<BlockPos> plannedPositions;

    // Dirt path is made with a shovel. The shovel is asked for at most every MISSING_RETRY ticks.
    private long lastShovelRequest = -100000L;

    public BuilderWorkGoal(BuilderEntity builderEntity) {
        this.builderEntity = builderEntity;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !builderEntity.needsToSleep() && builderEntity.shouldWork() && !builderEntity.needsToGetToChest() && this.isBuildingAreaAvailable();
    }

    @Override
    public void start() {
        super.start();
        if(this.builderEntity.getCommandSenderWorld().isClientSide()) return;
        if(builderEntity.getFollowState() == 0) builderEntity.setFollowState(6); //Working

        // after a storage trip or sleeping the old escape is not valid anymore
        this.escapePos = null;
        this.outsidePlacePos = null;
        this.workSpot = null;
        this.plannedPositions = null;

        setState(State.SELECT_WORK_AREA);
    }
    boolean workDone;
    @Override
    public void tick() {
        super.tick();
        if(this.builderEntity.getCommandSenderWorld().isClientSide()) return;
        if(state == null) return;
        if(blockPos != null) this.builderEntity.getLookControl().setLookAt(blockPos.getCenter());

        if(!isBuildingAreaAvailable()) return;

        if(state != State.SELECT_WORK_AREA && this.builderEntity.currentBuildArea == null){
            this.blockPos = null;
            setState(State.SELECT_WORK_AREA);
            return;
        }

        // safety net: stuck in a block (suffocating) -> out to the nearest free spot
        if(builderEntity.tickCount % 20 == 0 && builderEntity.isInWall()){
            BlockPos free = findStandSpot(builderEntity.getCommandSenderWorld(), null, null, false);
            if(free == null && builderEntity.currentBuildArea != null) free = findOutsideSpot(builderEntity.getCommandSenderWorld());
            if(free != null) builderEntity.teleportTo(free.getX() + 0.5D, free.getY(), free.getZ() + 0.5D);
        }

        // stepping aside / leaving the area before a block is placed
        if(this.tickEscape()) return;

        if(builderEntity.tickCount % 10 == 0){
            if(state == State.PLACE_BLOCKS || state == State.PLACE_MULTIBLOCK){
                // placing: walk to a work spot near the block, not onto the block itself.
                // A block that closes the way out is placed from outside, no walking back to it.
                walkingToWork = blockPos != null && !blockPos.equals(outsidePlacePos) && moveToWorkSpot(blockPos);
                if(walkingToWork) return;
            }
            else if(blockPos != null && moveToPosition(blockPos, 40)) return;


            if(state == State.BREAK_BLOCKS){
                if(this.mineBlocks(this.stackToBreak)) return;

                setState(State.PREPARE_PLACE_BLOCKS);
            }

            if(state == State.FREE_AREA){
                if(this.mineFreeBlocks(this.stackToFree)) return;

                this.builderEntity.currentBuildArea.setFreeAreaDone(true);
                setState(State.PREPARE_BREAK_BLOCKS);
            }
        }

        if(builderEntity.tickCount % 5 != 0) return;
        switch(state){
            case SELECT_WORK_AREA ->{
                if(builderEntity.currentBuildArea != null) setState(State.MOVE_TO_WORK_AREA);

                List<BuildArea> areas = getAvailableWorkAreasByPriority((ServerLevel) builderEntity.getCommandSenderWorld(), builderEntity, builderEntity.currentBuildArea);

                if (!areas.isEmpty()) {
                    builderEntity.currentBuildArea = areas.get(0);
                }

                if(builderEntity.currentBuildArea == null) return;

                builderEntity.currentBuildArea.setBeingWorkedOn(true);
                builderEntity.currentBuildArea.setBuilder(builderEntity);
                this.builderEntity.currentBuildArea.setTime(0);
                workDone = false;
                // start() runs again after every storage trip, only forget the missing materials for a new area
                if(!builderEntity.currentBuildArea.getUUID().equals(this.lastAreaUUID)){
                    this.lastAreaUUID = builderEntity.currentBuildArea.getUUID();
                    this.missingItems.clear();
                    this.requestedItems.clear();
                    this.lastNotifyText = null;
                }
                setState(State.MOVE_TO_WORK_AREA);
            }

            case MOVE_TO_WORK_AREA ->{
                this.blockPos = null;
                if(this.moveToPosition(builderEntity.currentBuildArea.getOnPos(), 20)) return;

                // free area and/or the cut of the foundation mode, done once per build
                if(builderEntity.currentBuildArea.needsClearing() && !builderEntity.currentBuildArea.getFreeAreaDone()){
                    setState(State.PREPARE_FREE_AREA);
                }
                else{
                    setState(State.PREPARE_BREAK_BLOCKS);
                }
            }

            case PREPARE_FREE_AREA -> {
                this.builderEntity.currentBuildArea.scanFreeArea();

                this.stackToFree = this.builderEntity.currentBuildArea.stackToFree;

                if(stackToFree.isEmpty()){
                    setState(State.PREPARE_BREAK_BLOCKS);
                    return;
                }

                boolean hasDigger = builderEntity.getInventory().hasAnyMatching(
                        itemStack -> itemStack.getItem() instanceof DiggerItem);
                if (!hasDigger) {
                    notifyOwnerThrottled(Component.literal(builderEntity.getName().getString() + ": I need a pickaxe to clear the area."));
                    builderEntity.addNeededItem(new NeededItem(
                            stack -> stack.getItem() instanceof PickaxeItem, 1, true));
                    this.blockPos = null;
                    return;
                }
                if (!stackToFree.isEmpty()) {
                    BlockState firstState = builderEntity.getCommandSenderWorld()
                            .getBlockState(stackToFree.peek());
                    builderEntity.changeTool(firstState);
                }
                setState(State.FREE_AREA);
            }

            case PREPARE_BREAK_BLOCKS -> {
                this.builderEntity.currentBuildArea.scanBreakArea();//SOMETHING WRONG I CAN FEEL IT

                this.stackToBreak = this.builderEntity.currentBuildArea.stackToBreak;

                if(stackToBreak.isEmpty()){
                    setState(State.PREPARE_PLACE_BLOCKS);
                    return;
                }

                boolean hasDigger = builderEntity.getInventory().hasAnyMatching(
                        itemStack -> itemStack.getItem() instanceof DiggerItem);
                if (!hasDigger) {
                    notifyOwnerThrottled(Component.literal(builderEntity.getName().getString() + ": I need a pickaxe to clear the area."));
                    builderEntity.addNeededItem(new NeededItem(
                            stack -> stack.getItem() instanceof PickaxeItem, 1, true));
                    this.blockPos = null;
                    return;
                }
                if (!stackToBreak.isEmpty()) {
                    BlockState firstState = builderEntity.getCommandSenderWorld()
                            .getBlockState(stackToBreak.peek());
                    builderEntity.changeTool(firstState);
                }
                setState(State.BREAK_BLOCKS);
            }

            case PREPARE_PLACE_BLOCKS -> {
                if (builderEntity.currentBuildArea.stackToPlace.isEmpty()) {
                    if (!builderEntity.currentBuildArea.stackToPlaceMultiBlock.isEmpty()) {
                        setState(State.PREPARE_PLACE_MULTIBLOCK);
                    } else {
                        setState(State.DONE);
                    }
                    return;
                }

                Level level = builderEntity.getCommandSenderWorld();
                this.updateMissingItems(level);

                // Paid with currency: the builder needs no materials, every block is placeable.
                boolean paid = builderEntity.currentBuildArea.isPaidWithCurrency();

                // Remaining blocks per level, lowest level first
                TreeMap<Integer, List<BuildBlock>> levels = new TreeMap<>();
                for (BuildBlock buildBlock : builderEntity.currentBuildArea.stackToPlace) {
                    levels.computeIfAbsent(buildBlock.getPos().getY(), y -> new ArrayList<>()).add(buildBlock);
                }

                this.blockPos = null;
                this.forcePlace = false;
                this.plannedPositions = null;
                this.stackToPlace = new Stack<>();
                Stack<BlockPos> unsupported = new Stack<>();
                Set<Item> missingOnBuild = new HashSet<>();
                boolean requested = false;
                boolean needsShovel = false;

                // Lowest level that can be worked on. A level whose blocks are all missing in the
                // storages (or have no support yet) is skipped, the builder goes on with the next one.
                for (Map.Entry<Integer, List<BuildBlock>> entry : levels.entrySet()) {
                    Set<Item> levelNeeds = new HashSet<>();
                    for (BuildBlock buildBlock : entry.getValue()) {
                        BlockPos pos = buildBlock.getPos();

                        // dirt path: made with a shovel, grass/dirt on the spot needs no material (else one dirt)
                        if (!paid && buildBlock.getState().is(Blocks.DIRT_PATH)) {
                            if (!hasShovel()) {
                                needsShovel = true;
                                continue;
                            }
                            if (BuildArea.isPathGround(level.getBlockState(pos))) {
                                if (canPlaceNow(level, pos, buildBlock.getState())) stackToPlace.push(pos);
                                else unsupported.push(pos);
                                continue;
                            }
                        }

                        Block block = buildBlock.getState().getBlock();
                        Item item = BuildBlockParse.parseBlock(block, level).getItem();
                        if(item == null){
                            if(builderEntity.getOwner() != null) builderEntity.getOwner().sendSystemMessage(Component.literal("Could not found item for " + buildBlock.getState().getBlock().getName() + " i, will skip this block." ));
                            builderEntity.currentBuildArea.removeBuildBlockToPlace(pos);
                            return;
                        }

                        // the finished item (red_carpet) counts too, but only the base material (red_wool) is missing
                        if (!paid && getBuildingItem(block, item) == null) {
                            if (missingItems.containsKey(item)) missingOnBuild.add(item);
                            else levelNeeds.add(item);
                            continue;
                        }

                        if (canPlaceNow(level, pos, buildBlock.getState())) stackToPlace.push(pos);
                        else unsupported.push(pos);
                    }

                    if (!stackToPlace.isEmpty()) {
                        minBuildHeight = entry.getKey();
                        break;
                    }

                    if (!levelNeeds.isEmpty()) {
                        if (this.requestMaterials(levelNeeds)) {
                            minBuildHeight = entry.getKey();
                            requested = true;
                            break;
                        }
                        // could not request anything for this level: treat as missing, go on with the next level
                        long time = level.getGameTime();
                        for (Item item : levelNeeds) missingItems.put(item, time);
                        missingOnBuild.addAll(levelNeeds);
                    }
                }

                if (needsShovel) {
                    missingOnBuild.add(Items.IRON_SHOVEL);
                    long time = level.getGameTime();
                    if (time - lastShovelRequest > MISSING_RETRY) {
                        lastShovelRequest = time;
                        notifyOwnerThrottled(Component.literal(builderEntity.getName().getString() + ": I need a shovel to make dirt paths."));
                        builderEntity.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof ShovelItem, 1, true));
                    }
                }

                if (stackToPlace.isEmpty() && !requested) {
                    if (!missingOnBuild.isEmpty()) {
                        // everything left needs materials the storages do not have
                        notifyOwnerThrottled(Component.literal(builderEntity.getName().getString() + ": I am waiting for materials: " + getItemNames(missingOnBuild)));
                        waitTime = 0;
                        setState(State.WAIT_FOR_MATERIALS);
                        builderEntity.currentBuildArea.setMissingMaterials(missingOnBuild);
                        return;
                    }
                    if (!unsupported.isEmpty()) {
                        // nothing else left, e.g. a torch whose wall is not part of the structure: place them anyway
                        this.stackToPlace = unsupported;
                        this.forcePlace = true;
                    }
                }

                setState(State.PLACE_BLOCKS);
            }

            case WAIT_FOR_MATERIALS -> {
                this.blockPos = null;
                // the tick switch runs every 5 ticks, missing materials expire after MISSING_RETRY
                waitTime += 5;
                if (waitTime < WAIT_CHECK_INTERVAL) return;
                waitTime = 0;

                setState(State.PREPARE_PLACE_BLOCKS);
            }


            case PLACE_BLOCKS -> {
                if(walkingToWork) return;
                if(this.placeBlocks(this.stackToPlace)) return;

                setState(State.PREPARE_PLACE_BLOCKS);
            }

            case PREPARE_PLACE_MULTIBLOCK -> {
                if (builderEntity.currentBuildArea.stackToPlaceMultiBlock.isEmpty()) {
                    setState(State.DONE);
                    return;
                }

                this.plannedPositions = null;
                this.stackToPlace = new Stack<>();
                for (BuildBlock bb : builderEntity.currentBuildArea.stackToPlaceMultiBlock) {
                    stackToPlace.push(bb.getPos());
                }

                setState(State.PLACE_MULTIBLOCK);
            }

            case PLACE_MULTIBLOCK -> {
                if (walkingToWork) return;
                if (this.placeMultiBlocks(this.stackToPlace)) return;

                // halves that were broken went back on the list: build them first
                if (!builderEntity.currentBuildArea.stackToPlace.isEmpty()) setState(State.PREPARE_PLACE_BLOCKS);
                else setState(State.DONE);
            }

            case DONE -> {
                if(!workDone){
                    workDone = true;

                    // Spawn any entities that were scanned with the structure (work areas, etc.)
                    BuildArea finished = builderEntity.currentBuildArea;
                    List<AbstractWorkAreaEntity> spawned = finished.spawnWorkAreas ? spawnScannedEntities(finished) : new ArrayList<>();
                    NeoForge.EVENT_BUS.post(new BuildEvent.Finished(finished, builderEntity, spawned));
                    // the Governor's job is done, the builder picks his next area himself again
                    if (finished.getUUID().equals(builderEntity.getAssignedWorkArea())) builderEntity.setAssignedWorkArea(null);

                    builderEntity.currentBuildArea.setBeingWorkedOn(false);
                    if(this.builderEntity.getFollowState() == 6) this.builderEntity.setFollowState(0);//Wander
                    //ONLY FOR BUILDING AREA WILL REMOVE IT
                    this.builderEntity.currentBuildArea.setDone(true);

                    blockPos = null;
                    builderEntity.currentBuildArea = null;
                    this.start();
                }
            }

            case ERROR ->{
                if(!errorMessageDone){
                    errorMessageDone = true;
                }
            }
        }
    }

    private boolean isBuildingAreaAvailable() {
        if(builderEntity.currentBuildArea == null || !builderEntity.currentBuildArea.isRemoved()) return true;
        else {
            builderEntity.currentBuildArea = null;
        }
        return false;
    }

    public void setState(State state) {
        this.walkingToWork = false;
        //if(builderEntity.getOwner() != null) builderEntity.getOwner().sendSystemMessage(Component.literal(state.toString()));
        this.state = state;
        this.updateBuildStatus(state);
    }

    /**
     * The status of the build area follows the work (GUI and Governor). The PREPARE states change nothing,
     * so waiting for materials does not flicker while he checks again.
     */
    private void updateBuildStatus(State state) {
        BuildArea area = builderEntity.currentBuildArea;
        if (area == null || builderEntity.getCommandSenderWorld().isClientSide()) return;

        switch (state) {
            case PREPARE_FREE_AREA, FREE_AREA -> area.setBuildStatus(BuildArea.BuildStatus.CLEARING);
            case MOVE_TO_WORK_AREA, BREAK_BLOCKS, PLACE_BLOCKS, PLACE_MULTIBLOCK -> area.setBuildStatus(BuildArea.BuildStatus.BUILDING);
            case WAIT_FOR_MATERIALS -> area.setBuildStatus(BuildArea.BuildStatus.WAITING_FOR_MATERIALS);
            case DONE -> area.setBuildStatus(BuildArea.BuildStatus.DONE);
            default -> {}
        }
    }

    /** After a storage trip: requested materials he still does not carry are missing in the storages. */
    private void updateMissingItems(Level level) {
        long time = level.getGameTime();
        if (!requestedItems.isEmpty() && !builderEntity.needsToGetItems()) {
            Map<Item, Map<Item, Integer>> finishedItems = getFinishedItemsByMaterial(level);
            for (Item item : requestedItems) {
                if (countCarriedMaterial(item, finishedItems.getOrDefault(item, Map.of())) <= 0) missingItems.put(item, time);
            }
            requestedItems.clear();
        }
        // ask the storages again after a while, the player may have brought the materials
        missingItems.values().removeIf(markTime -> time - markTime > MISSING_RETRY);
    }

    /**
     * Prefetch several base materials at once (look-ahead over the REMAINING structure) so the
     * builder makes far fewer storage trips. Materials of the current level first, missing ones are skipped.
     */
    private boolean requestMaterials(Set<Item> currentLevelItems) {
        Level level = builderEntity.getCommandSenderWorld();
        List<ItemStack> neededItems = builderEntity.currentBuildArea.getRemainingMaterials();
        neededItems.removeIf(stack -> missingItems.containsKey(stack.getItem()));

        neededItems.sort(Comparator
                .comparing((ItemStack s) -> !currentLevelItems.contains(s.getItem()))
                .thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed()));

        // Finished items (red_carpet) lying in the storage are taken first, the base material (red_wool)
        // is only asked for the rest. The finished item is never asked for if the storage does not have it.
        Map<Item, Map<Item, Integer>> finishedItems = getFinishedItemsByMaterial(level);
        Map<Item, Integer> storedFinishedItems = null;

        int requested = 0;
        boolean currentLevelRequested = false;
        for (ItemStack neededItem : neededItems) {
            if (requested >= PREFETCH_MATERIAL_BUDGET) break;

            Item material = neededItem.getItem();
            Map<Item, Integer> finished = finishedItems.getOrDefault(material, Map.of());

            // Only fetch what is missing on top of what we already carry.
            int carried = countCarriedMaterial(material, finished);
            int amount = Math.min(PREFETCH_STACK_LIMIT, neededItem.getCount()) - carried;
            if (amount <= 0) continue;

            if (!finished.isEmpty()) {
                if (storedFinishedItems == null) {
                    Set<Item> allFinished = new HashSet<>();
                    finishedItems.values().forEach(map -> allFinished.addAll(map.keySet()));
                    storedFinishedItems = countStoredItems(allFinished);
                }

                for (Map.Entry<Item, Integer> entry : finished.entrySet()) {
                    if (amount <= 0) break;
                    Item finishedItem = entry.getKey();
                    int stillNeeded = entry.getValue() - builderEntity.countMatchingItems(itemStack -> itemStack.is(finishedItem));
                    int take = Math.min(amount, Math.min(stillNeeded, storedFinishedItems.getOrDefault(finishedItem, 0)));
                    if (take <= 0) continue;

                    builderEntity.addNeededItem(new NeededItem(
                            itemStack -> itemStack.is(finishedItem), take, true
                    ));
                    amount -= take;
                }
            }

            if (amount > 0) {
                builderEntity.addNeededItem(new NeededItem(
                        itemStack -> itemStack.is(material), amount, true
                ));
            }
            requestedItems.add(material);
            if (currentLevelItems.contains(neededItem.getItem())) currentLevelRequested = true;
            requested++;
        }
        return currentLevelRequested;
    }

    /** The finished item (red_carpet) is used first, otherwise the base material (red_wool). */
    @Nullable
    private ItemStack getBuildingItem(Block block, Item material) {
        Item finished = BuildBlockParse.getFinishedItem(block, builderEntity.getCommandSenderWorld());
        ItemStack stack = finished != null ? builderEntity.getMatchingItem(itemStack -> itemStack.is(finished)) : null;
        return stack != null ? stack : builderEntity.getMatchingItem(itemStack -> itemStack.is(material));
    }

    /** Finished items still needed (red_carpet -> 10), grouped by their base material (red_wool). */
    private Map<Item, Map<Item, Integer>> getFinishedItemsByMaterial(Level level) {
        Map<Item, Map<Item, Integer>> map = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : builderEntity.currentBuildArea.getRemainingFinishedItemMap().entrySet()) {
            if (!(entry.getKey() instanceof BlockItem blockItem)) continue;
            Item material = BuildBlockParse.parseBlock(blockItem.getBlock(), level).getItem();
            map.computeIfAbsent(material, key -> new HashMap<>()).put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    /** Carried base material plus the finished items that stand in for it (at most as many as still needed). */
    private int countCarriedMaterial(Item material, Map<Item, Integer> finishedItems) {
        int carried = builderEntity.countMatchingItems(itemStack -> itemStack.is(material));
        for (Map.Entry<Item, Integer> entry : finishedItems.entrySet()) {
            carried += Math.min(entry.getValue(), builderEntity.countMatchingItems(itemStack -> itemStack.is(entry.getKey())));
        }
        return carried;
    }

    /** Count of these items in the storages the builder fetches from (same choice as GetNeededItemsFromStorage). */
    private Map<Item, Integer> countStoredItems(Set<Item> items) {
        Map<Item, Integer> stored = new HashMap<>();
        if (items.isEmpty()) return stored;

        List<StorageArea> storages = builderEntity.getCommandSenderWorld().getEntitiesOfClass(StorageArea.class, builderEntity.getBoundingBox().inflate(64));
        storages.removeIf(storage -> !storage.canWorkHere(builderEntity));

        UUID lastStorage = builderEntity.lastStorage;
        if (lastStorage != null && storages.stream().anyMatch(storage -> storage.getUUID().equals(lastStorage))) {
            storages.removeIf(storage -> !storage.getUUID().equals(lastStorage));
        }

        for (StorageArea storage : storages) {
            storage.scanStorageBlocks();
            for (Container container : storage.storageMap.values()) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty() && items.contains(stack.getItem())) stored.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
        }
        return stored;
    }

    /** Falling blocks need a block below, torches/doors/carpets need their support. */
    private boolean canPlaceNow(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof FallingBlock && FallingBlock.isFree(level.getBlockState(pos.below()))) return false;
        return state.canSurvive(level, pos);
    }

    private static String getItemNames(Set<Item> items) {
        // sorted, so the same materials always give the same text (see notifyOwnerThrottled)
        return items.stream()
                .map(item -> new ItemStack(item).getHoverName().getString())
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /**
     * Tells the owner why the builder stands still. The normal notifyOwner only sends once a day, then the
     * player wonders why nothing happens. A new text is sent at once, the same text again after NOTIFY_REPEAT.
     */
    private void notifyOwnerThrottled(Component message) {
        long time = builderEntity.getCommandSenderWorld().getGameTime();
        String text = message.getString();
        if (text.equals(lastNotifyText) && time - lastNotifyTime < NOTIFY_REPEAT) return;

        lastNotifyText = text;
        lastNotifyTime = time;
        builderEntity.notifyOwnerAlways(message);
    }

    int blockBreakTime;
    public boolean mineFreeBlocks(Stack<BlockPos> positions){
        if(positions == null) return false;

        if(blockPos == null){
            if(positions.isEmpty()) return false;
            blockPos = positions.pop();
            return true;
        }

        BlockState state = builderEntity.getCommandSenderWorld().getBlockState(blockPos);
        if(state.isAir() || builderEntity.shouldIgnoreBlock(state)){
            if(positions.isEmpty()){
                this.blockPos = null;
                return false;
            }
            blockPos = positions.pop();
            blockBreakTime = 0;
            return true;
        }

        this.builderEntity.changeTool(state);
        this.builderEntity.mineBlock(blockPos);
        this.builderEntity.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    public boolean mineBlocks(Stack<BlockPos> positions){
        if(positions != null){
            if(blockPos == null){
                if(!positions.isEmpty()){
                    blockPos = this.getNewMiningPosition(positions);
                }
                return blockPos != null;
            }

            BlockState state = builderEntity.getCommandSenderWorld().getBlockState(blockPos);
            if(state.isAir() || builderEntity.shouldIgnoreBlock(state)){
                if(!positions.isEmpty()){
                    blockPos = this.getNewMiningPosition(positions);
                }
                else{
                    this.blockPos = null;
                    return false;
                }
                blockBreakTime = 0;
            }
            else{
                this.builderEntity.changeTool(state);

                this.builderEntity.mineBlock(blockPos);
                this.builderEntity.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    private BlockPos getNewMiningPosition(Stack<BlockPos> positions) {
        positions.sort(Comparator.comparingDouble(
                pos -> builderEntity.position().distanceToSqr(pos.getCenter())
        ));
        positions.sort(Comparator.reverseOrder());

        BlockPos newPosition = null;

        if(blockPos == null){
            positions.removeIf(pos ->
                    !canSeeBlock(builderEntity.getCommandSenderWorld(), builderEntity.position().add(0, 1, 0), pos)
            );

            if(positions.isEmpty()){
                setState(State.MOVE_TO_WORK_AREA);
                return null;
            }

            newPosition = positions.pop();
        }
        else if(positions.contains(blockPos.above())){
            newPosition = blockPos.above();
        }
        else if(positions.contains(blockPos.below())){
            newPosition = blockPos.below();
        }
        else{
            newPosition = positions.pop();
        }
        positions.remove(newPosition);
        return newPosition;
    }
    /**
     * Spawns work-area entities that were recorded during the structure scan.
     * Each entity is placed at the world position corresponding to its scanned relative offset,
     * rotated from scan-facing to build-facing, and inherits the owner UUID + team of the BuildArea.
     */
    private List<AbstractWorkAreaEntity> spawnScannedEntities(BuildArea buildArea) {
        List<AbstractWorkAreaEntity> spawned = new ArrayList<>();
        Level level = builderEntity.getCommandSenderWorld();
        if (!(level instanceof ServerLevel serverLevel)) return spawned;

        CompoundTag nbt = buildArea.getStructureNBT();
        if (nbt == null || !nbt.contains("entities", Tag.TAG_LIST)) return spawned;

        ListTag entityList = nbt.getList("entities", Tag.TAG_COMPOUND);
        if (entityList.isEmpty()) return spawned;

        Direction scanFacing  = Direction.byName(nbt.getString("facing"));
        Direction buildFacing = buildArea.getFacing();
        Direction buildRight  = buildFacing.getClockWise();
        BlockPos  origin      = buildArea.getOriginPos();
        int       width       = buildArea.getWidthSize();

        if (scanFacing == null) scanFacing = Direction.SOUTH;

        // Rotation steps: from scan-facing to build-facing (same formula as block rotation)
        int rotSteps = ((buildFacing.get2DDataValue() - scanFacing.get2DDataValue()) % 4 + 4) % 4;

        for (Tag t : entityList) {
            CompoundTag entityTag = (CompoundTag) t;
            String typeId   = entityTag.getString("entity_type");
            int relX        = entityTag.getInt("x");
            int relY        = entityTag.getInt("y");
            int relZ        = entityTag.getInt("z");
            int scanFacingVal = entityTag.getInt("facing");

            // Look up entity type
            ResourceLocation rl = ResourceLocation.parse(typeId);
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(rl);
            if (entityType == null) continue;

            // Compute world position using same formula as setStartBuild / WorkerAreaRenderer
            BlockPos worldPos = origin
                    .relative(buildFacing, relZ)
                    .relative(buildRight, width - 1 - relX)
                    .above(relY);

            // Create the entity
            Entity entity = entityType.create(serverLevel);
            if (entity == null) continue;

            entity.moveTo(worldPos.getX() + 0.5, worldPos.getY() + 1.0, worldPos.getZ() + 0.5, 0, 0);

            // Apply facing rotation: rotate the scanned facing by the same rotSteps
            if (entity instanceof AbstractWorkAreaEntity wa) {
                Direction entityFacing = Direction.from2DDataValue(scanFacingVal);
                Direction rotatedFacing = rotateDirection(entityFacing, rotSteps);
                wa.setFacing(rotatedFacing);

                // Restore stored dimensions, swapping width↔depth for 90° / 270° rotations
                if (entityTag.contains("wa_width")) {
                    int waW = entityTag.getInt("wa_width");
                    int waH = entityTag.getInt("wa_height");
                    int waD = entityTag.getInt("wa_depth");
                    if (rotSteps % 2 == 1) { int tmp = waW; waW = waD; waD = tmp; }
                    wa.setWidthSize(waW);
                    wa.setHeightSize(waH);
                    wa.setDepthSize(waD);
                }

                // Transfer owner and team from the BuildArea
                if (buildArea.getPlayerUUID() != null) {
                    wa.setPlayerUUID(buildArea.getPlayerUUID());
                }
                String ownerName = buildArea.getPlayerName();
                if (ownerName != null && !ownerName.isEmpty()) {
                    wa.setPlayerName(ownerName);
                }
                String team = buildArea.getTeamStringID();
                if (team != null && !team.isEmpty()) {
                    wa.setTeamStringID(team);
                }

                wa.setCustomName(Component.literal(""));
            }

            if (serverLevel.addFreshEntity(entity) && entity instanceof AbstractWorkAreaEntity wa) spawned.add(wa);
        }
        return spawned;
    }

    /** Rotates a horizontal Direction clockwise by the given number of 90° steps. */
    private static Direction rotateDirection(Direction dir, int steps) {
        steps = ((steps % 4) + 4) % 4;
        for (int i = 0; i < steps; i++) dir = dir.getClockWise();
        return dir;
    }

    public boolean placeBlocks(Stack<BlockPos> positions){
        if(positions != null){
            if(blockPos == null){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    return false;
                }
            }

            BlockState buildingState = builderEntity.currentBuildArea.getStateFromPos(blockPos);
            BlockState levelState = builderEntity.getCommandSenderWorld().getBlockState(blockPos);

            if(builderEntity.currentBuildArea.statesMatch(levelState, buildingState)){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    return false;
                }
            }
            else if(buildingState != null) {
                // only from the work spot (a block that closes the way out is placed from outside)
                if (!blockPos.equals(outsidePlacePos) && !isInReach(builderEntity.blockPosition(), blockPos, WORK_REACH + 1)) return true;

                if (!levelState.isAir() && !BuildArea.canDirectlyReplace(levelState, buildingState)) {
                    this.builderEntity.changeTool(levelState);
                    this.builderEntity.mineBlock(blockPos);
                    this.builderEntity.swing(InteractionHand.MAIN_HAND);
                    return true;
                }

                // no support yet (block below still missing): skip, it is tried again in the next round
                if (!forcePlace && !canPlaceNow(builderEntity.getCommandSenderWorld(), blockPos, buildingState)) {
                    if (!positions.isEmpty()) {
                        blockPos = positions.pop();
                        return true;
                    }
                    blockPos = null;
                    return false;
                }

                // flee from the block first: it is never placed right next to him (feet/head height), so it
                // can not hit him or box him in. He walks to a spot at least 2 blocks away, then places it.
                BlockPos pairedPos = builderEntity.currentBuildArea.findPairedMultiBlockPos(blockPos);
                if (isTooClose(blockPos) || (pairedPos != null && isTooClose(pairedPos))) {
                    if (stepAside(builderEntity.getCommandSenderWorld(), blockPos, pairedPos)) return true;
                }

                if (buildingState.is(Blocks.DIRT_PATH)) {
                    return placeDirtPath(positions, levelState);
                }

                BuildBlockParse blockParse = BuildBlockParse.parseBlock(buildingState.getBlock(), builderEntity.getCommandSenderWorld());
                //Paid with currency: no material needed and nothing is consumed
                boolean paid = builderEntity.currentBuildArea.isPaidWithCurrency();
                // the finished item (red_carpet) first, otherwise the base material (red_wool)
                ItemStack buildingItem = paid ? null : getBuildingItem(buildingState.getBlock(), blockParse.getItem());
                if(paid || buildingItem != null){
                    if(buildingItem != null && !builderEntity.getMainHandItem().is(buildingItem.getItem())){
                        builderEntity.switchMainHandItem(itemStack -> itemStack.is(buildingItem.getItem()));
                    }
                    //CHECK IF IT WAS PARSED TO KEEP THE BLOCK-ROTATIONS OF NOT EFFECTED ONES
                    //placeAsBase = true means the base item is also placed as its block (grass -> dirt).
                    //For material-only substitutions (oak_stairs -> oak_planks) we keep the target block.
                    if(blockParse.placeAsBase() && blockParse.getItem() instanceof BlockItem blockItem){
                        buildingState = blockItem.getBlock().defaultBlockState();
                    }

                    BlockState secondaryState = builderEntity.currentBuildArea.findPairedMultiBlockState(blockPos);
                    BlockPos secondaryPos = secondaryState != null ? builderEntity.currentBuildArea.findPairedMultiBlockPos(blockPos) : null;

                    // never build into an entity: the builder steps aside, someone else is waited for
                    Level level = builderEntity.getCommandSenderWorld();
                    if (isBlockedByEntity(level, blockPos, buildingState) || (secondaryPos != null && isBlockedByEntity(level, secondaryPos, secondaryState))) {
                        boolean builderInTheWay = isBuilderIn(blockPos) || (secondaryPos != null && isBuilderIn(secondaryPos));
                        if (builderInTheWay && stepAside(level, blockPos, secondaryPos)) return true;

                        // try the next block, this one comes again in the next round
                        if (!positions.isEmpty()) {
                            blockPos = positions.pop();
                            return true;
                        }
                        blockPos = null;
                        return false;
                    }

                    // do not wall himself in: this block would close the last way out, leave the area first
                    // (from outside the check is always false, then the block is placed from there)
                    if (wouldEncloseBuilder(level, blockPos, secondaryPos)) {
                        BlockPos outside = findOutsideSpot(level);
                        if (outside != null) {
                            outsidePlacePos = blockPos;
                            startEscape(outside);
                            return true;
                        }
                    }

                    if (secondaryState != null) {
                        builderEntity.getCommandSenderWorld().setBlock(blockPos, buildingState, Block.UPDATE_CLIENTS);
                        builderEntity.getCommandSenderWorld().setBlock(secondaryPos, secondaryState, Block.UPDATE_ALL);
                        builderEntity.getCommandSenderWorld().blockUpdated(blockPos, buildingState.getBlock());
                        builderEntity.currentBuildArea.removeMultiBlockToPlace(secondaryPos);
                    } else {
                        builderEntity.getCommandSenderWorld().setBlockAndUpdate(blockPos, buildingState);
                    }

                    builderEntity.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), buildingState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    this.builderEntity.swing(InteractionHand.MAIN_HAND);
                    if(buildingItem != null) buildingItem.shrink(1);
                    builderEntity.currentBuildArea.removeBuildBlockToPlace(blockPos);
                    if(blockPos.equals(outsidePlacePos)) outsidePlacePos = null;
                }
                else{
                    // material ran out: forget this block so the next round does not get stuck on it
                    blockPos = null;
                    return false;
                }
            }
            return true;
        }
        this.blockPos = null;
        return false;
    }


    public boolean placeMultiBlocks(Stack<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) {
            this.blockPos = null;
            return false;
        }

        if (blockPos == null) {
            blockPos = positions.pop();
        }
        if (!isInReach(builderEntity.blockPosition(), blockPos, WORK_REACH + 1)) return true;

        BlockState buildingState = builderEntity.currentBuildArea.getStateFromMultiBlockPos(blockPos);
        if (buildingState != null) {
            BlockState levelState = builderEntity.getCommandSenderWorld().getBlockState(blockPos);

            // a bed head / upper door half is never placed alone: if the other half is missing (e.g. broken
            // by a player) the pair goes back on the list and is placed together again, with a new item.
            // A half placed alone could be broken for a free item.
            BuildBlock primary = BuildArea.getPrimaryFor(blockPos, buildingState);
            if (!levelState.equals(buildingState) && primary != null
                    && !builderEntity.currentBuildArea.statesMatch(builderEntity.getCommandSenderWorld().getBlockState(primary.getPos()), primary.getState())) {
                builderEntity.currentBuildArea.addBuildBlockToPlace(primary);
                if (!positions.isEmpty()) {
                    blockPos = positions.pop();
                    return true;
                }
                blockPos = null;
                return false;
            }

            // flee from the block first, like for normal blocks
            if (!levelState.equals(buildingState) && isTooClose(blockPos) && stepAside(builderEntity.getCommandSenderWorld(), blockPos, null)) return true;

            if (!levelState.equals(buildingState) && isBlockedByEntity(builderEntity.getCommandSenderWorld(), blockPos, buildingState)) {
                // the builder steps aside and tries again, someone else is waited for
                if (isBuilderIn(blockPos)) stepAside(builderEntity.getCommandSenderWorld(), blockPos, null);
                positions.add(0, blockPos);
                blockPos = positions.pop();
                return true;
            }
            if (!levelState.equals(buildingState)) {
                builderEntity.getCommandSenderWorld().setBlockAndUpdate(blockPos, buildingState);
                builderEntity.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                        buildingState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                builderEntity.swing(InteractionHand.MAIN_HAND);
            }
            builderEntity.currentBuildArea.removeMultiBlockToPlace(blockPos);
        }

        if (!positions.isEmpty()) {
            blockPos = positions.pop();
            return true;
        }

        blockPos = null;
        return false;
    }

    // ── Not building himself in ──────────────────────────────────────────────

    /** True if an entity (builder, player, animal...) stands where the block would get a collision box. */
    private static boolean isBlockedByEntity(Level level, BlockPos pos, BlockState state) {
        return !level.isUnobstructed(state, pos, CollisionContext.empty());
    }

    private boolean isBuilderIn(BlockPos pos) {
        return builderEntity.getBoundingBox().intersects(new AABB(pos));
    }

    /** The builder stands where a block goes: a free spot nearby that is not part of the building, else outside the area. */
    private boolean stepAside(Level level, BlockPos pos, @Nullable BlockPos secondaryPos) {
        // a work spot near the block (outside the area if possible), else just outside the area
        BlockPos spot = findWorkSpot(level, pos, secondaryPos);
        if (spot == null) {
            spot = findOutsideSpot(level);
            // an outside spot right next to the block does not help, he would flee forever
            if (spot != null && (isNextTo(spot, pos) || (secondaryPos != null && isNextTo(spot, secondaryPos)))) spot = null;
        }
        if (spot == null) return false;

        startEscape(spot);
        return true;
    }

    private void startEscape(BlockPos target) {
        this.escapePos = target;
        this.escapeTicks = 0;
        builderEntity.getNavigation().stop();
    }

    /** Walks to the escape spot. If he can not get there in time, he is put there. */
    private boolean tickEscape() {
        if (escapePos == null) return false;

        if (++escapeTicks > ESCAPE_TIMEOUT) {
            builderEntity.getNavigation().stop();
            builderEntity.teleportTo(escapePos.getX() + 0.5D, escapePos.getY(), escapePos.getZ() + 0.5D);
            escapePos = null;
            return false;
        }
        if (builderEntity.tickCount % 10 != 0) return true;

        // he has to really stand there, roughly near is not enough (he is 0.6 wide)
        if (!isAtSpot(escapePos)) {
            walkToSpot(escapePos);
            return true;
        }
        builderEntity.getNavigation().stop();
        escapePos = null;
        return false;
    }

    /**
     * Would the block close the builder in? Searches the ways he can WALK from his position (same height,
     * dropping down up to 3, one block up), with the new block as solid. Doors and gates count as open.
     * If he can walk out of the area there is still a way out. A big open area is not searched
     * completely: then it is assumed to be open.
     */
    private boolean wouldEncloseBuilder(Level level, BlockPos placed, @Nullable BlockPos secondaryPos) {
        BlockPos start = builderEntity.blockPosition();
        if (!isInArea(start, 0)) return false;

        Set<BlockPos> blocked = new HashSet<>();
        blocked.add(placed);
        if (secondaryPos != null) blocked.add(secondaryPos);

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            if (visited.size() > ENCLOSE_SEARCH_LIMIT) return false;
            BlockPos pos = queue.poll();
            if (!isInArea(pos, 1)) return false; // way out found

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = findWalkTarget(level, pos, pos.relative(direction), blocked);
                if (next != null && visited.add(next)) queue.add(next);
            }
        }
        return true;
    }

    /** Where he ends up walking from pos to the side: same height, dropping down (max 3) or one block up. */
    @Nullable
    private static BlockPos findWalkTarget(Level level, BlockPos pos, BlockPos side, Set<BlockPos> blocked) {
        if (isPassable(level, side, blocked) && isPassable(level, side.above(), blocked)) {
            for (int drop = 0; drop <= 3; drop++) {
                BlockPos feet = side.below(drop);
                if (drop > 0 && !isPassable(level, feet, blocked)) return null;
                if (isGround(level, feet.below(), blocked)) return feet;
            }
            return null;
        }
        // one block up: room above his head and on top of the block
        BlockPos up = side.above();
        if (isGround(level, side, blocked) && isPassable(level, pos.above(2), blocked)
                && isPassable(level, up, blocked) && isPassable(level, up.above(), blocked)) {
            return up;
        }
        return null;
    }

    private static boolean isPassable(Level level, BlockPos pos, Set<BlockPos> blocked) {
        return !blocked.contains(pos) && isOpen(level, pos);
    }

    private static boolean isGround(Level level, BlockPos pos, Set<BlockPos> blocked) {
        if (blocked.contains(pos)) return true;
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isOpen(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock) return true;
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private boolean isInArea(BlockPos pos, int margin) {
        AABB area = builderEntity.currentBuildArea.getArea();
        return pos.getX() >= area.minX - margin && pos.getX() <= area.maxX + margin
                && pos.getY() >= area.minY - margin && pos.getY() <= area.maxY + margin
                && pos.getZ() >= area.minZ - margin && pos.getZ() <= area.maxZ + margin;
    }

    private static boolean canStandAt(Level level, BlockPos feet) {
        BlockState ground = level.getBlockState(feet.below());
        return isOpen(level, feet) && isOpen(level, feet.above())
                && level.getBlockState(feet).getFluidState().isEmpty()
                && ground.isFaceSturdy(level, feet.below(), Direction.UP);
    }

    /**
     * Nearest spot (radius 3) he can stand on, not on the given block(s). With avoidPlanned also not
     * where a block of the building still goes, so he is not in the way again right after.
     */
    @Nullable
    private BlockPos findStandSpot(Level level, @Nullable BlockPos pos, @Nullable BlockPos secondaryPos, boolean avoidPlanned) {
        Set<BlockPos> planned = new HashSet<>();
        if (avoidPlanned) {
            for (BuildBlock buildBlock : builderEntity.currentBuildArea.stackToPlace) planned.add(buildBlock.getPos());
        }

        BlockPos origin = builderEntity.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
            BlockPos feet = candidate.immutable();
            BlockPos head = feet.above();
            if (feet.equals(pos) || head.equals(pos) || feet.equals(secondaryPos) || head.equals(secondaryPos)) continue;
            if (planned.contains(feet) || planned.contains(head)) continue;
            if (!canStandAt(level, feet)) continue;

            double distance = feet.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = feet;
            }
        }
        return best;
    }

    /** Nearest spot just outside the build area (1 to 3 blocks), on the height of the builder if possible. */
    @Nullable
    private BlockPos findOutsideSpot(Level level) {
        AABB area = builderEntity.currentBuildArea.getArea();
        BlockPos origin = builderEntity.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int ring = 1; ring <= 3; ring++) {
            int minX = (int) area.minX - ring, maxX = (int) area.maxX + ring;
            int minZ = (int) area.minZ - ring, maxZ = (int) area.maxZ + ring;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x != minX && x != maxX && z != minZ && z != maxZ) continue; // only the ring
                    for (int dy = 0; dy <= 4; dy++) {
                        BlockPos feet = null;
                        BlockPos up = new BlockPos(x, origin.getY() + dy, z);
                        BlockPos down = new BlockPos(x, origin.getY() - dy, z);
                        if (canStandAt(level, up)) feet = up;
                        else if (canStandAt(level, down)) feet = down;
                        if (feet == null) continue;

                        double distance = feet.distSqr(origin);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = feet;
                        }
                        break;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    // ── Work spot ────────────────────────────────────────────────────────────

    /** Walks to a work spot for the target block. Returns true while he is still walking. */
    private boolean moveToWorkSpot(BlockPos target) {
        Level level = builderEntity.getCommandSenderWorld();
        BlockPos pairedPos = builderEntity.currentBuildArea.findPairedMultiBlockPos(target);
        // where he stands is fine: stay
        if (builderEntity.onGround() && isGoodWorkSpot(level, builderEntity.blockPosition(), target, pairedPos)
                && !isTooClose(target) && (pairedPos == null || !isTooClose(pairedPos))) {
            builderEntity.getNavigation().stop();
            workSpot = null;
            return false;
        }

        if (!target.equals(workTarget) || workSpot == null || !isGoodWorkSpot(level, workSpot, target, pairedPos)) {
            workTarget = target;
            workSpot = findWorkSpot(level, target, pairedPos);
            workSpotTicks = 0;
        }
        if (workSpot == null) {
            // no free spot near the block: walk to the block like before (30 = within the place reach of 6)
            return moveToPosition(target, 30);
        }

        workSpotTicks += 10;
        if (workSpotTicks > WORK_SPOT_TIMEOUT) {
            // can not get there (path blocked): put him there instead of standing around
            builderEntity.getNavigation().stop();
            builderEntity.teleportTo(workSpot.getX() + 0.5D, workSpot.getY(), workSpot.getZ() + 0.5D);
            workSpotTicks = 0;
            return false;
        }
        if (isAtSpot(workSpot)) {
            builderEntity.getNavigation().stop();
            return false;
        }
        walkToSpot(workSpot);
        builderEntity.getLookControl().setLookAt(target.getCenter());
        return true;
    }

    /** Best spot to place the target from: in reach, not on a block of the building, outside the area first. */
    @Nullable
    private BlockPos findWorkSpot(Level level, BlockPos target, @Nullable BlockPos secondaryPos) {
        BlockPos origin = builderEntity.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(target.offset(-WORK_REACH, -8, -WORK_REACH), target.offset(WORK_REACH, 2, WORK_REACH))) {
            if (!isGoodWorkSpot(level, candidate, target, secondaryPos)) continue;

            double score = candidate.distSqr(target) + 0.25D * candidate.distSqr(origin);
            // outside the area nothing can be built around him
            if (isInArea(candidate, 0)) score += 100;
            if (score < bestScore) {
                bestScore = score;
                best = candidate.immutable();
            }
        }
        return best;
    }

    private boolean isGoodWorkSpot(Level level, BlockPos spot, BlockPos target, @Nullable BlockPos secondaryPos) {
        if (!isInReach(spot, target, WORK_REACH)) return false;
        BlockPos head = spot.above();
        // not on the block and not right next to it, so placing it can not hit or box him in
        if (isNextTo(spot, target) || (secondaryPos != null && isNextTo(spot, secondaryPos))) return false;
        Set<BlockPos> planned = getPlannedPositions();
        if (planned.contains(spot) || planned.contains(head)) return false;
        return canStandAt(level, spot);
    }

    /** True if the block is at feet or head height of the spot, on it or one of the 8 blocks around it. */
    private static boolean isNextTo(BlockPos spot, BlockPos block) {
        if (block.getY() < spot.getY() || block.getY() > spot.getY() + 1) return false;
        return Math.abs(block.getX() - spot.getX()) <= 1 && Math.abs(block.getZ() - spot.getZ()) <= 1;
    }

    /** Same as isNextTo, but with his real position (he can stand near the edge of his block). */
    private boolean isTooClose(BlockPos block) {
        int feetY = builderEntity.getBlockY();
        if (block.getY() < feetY || block.getY() > feetY + 1) return false;
        return Math.abs(builderEntity.getX() - (block.getX() + 0.5D)) < 1.4D
                && Math.abs(builderEntity.getZ() - (block.getZ() + 0.5D)) < 1.4D;
    }

    /** He really stands on the spot (middle of the block), roughly near is not enough. */
    private boolean isAtSpot(BlockPos spot) {
        double dx = builderEntity.getX() - (spot.getX() + 0.5D);
        double dz = builderEntity.getZ() - (spot.getZ() + 0.5D);
        return dx * dx + dz * dz <= 0.16D && Math.abs(builderEntity.getY() - spot.getY()) < 1.0D;
    }

    /** Walks to the middle of the spot, the last bit straight with the move control (the path ends roughly). */
    private void walkToSpot(BlockPos spot) {
        double x = spot.getX() + 0.5D;
        double z = spot.getZ() + 0.5D;
        double dx = builderEntity.getX() - x;
        double dz = builderEntity.getZ() - z;
        if (dx * dx + dz * dz < 2.25D) {
            builderEntity.getNavigation().stop();
            builderEntity.getMoveControl().setWantedPosition(x, spot.getY(), z, 0.8D);
        }
        else {
            builderEntity.getNavigation().moveTo(x, spot.getY(), z, 0.8F);
        }
    }

    /** Horizontal reach, he can place higher and lower blocks like before. */
    private static boolean isInReach(BlockPos from, BlockPos target, int reach) {
        int dx = from.getX() - target.getX();
        int dz = from.getZ() - target.getZ();
        return dx * dx + dz * dz <= reach * reach;
    }

    /** Positions of blocks the building still needs, rebuilt when a new round of placing starts. */
    private Set<BlockPos> getPlannedPositions() {
        if (plannedPositions == null) {
            plannedPositions = new HashSet<>();
            for (BuildBlock buildBlock : builderEntity.currentBuildArea.stackToPlace) plannedPositions.add(buildBlock.getPos());
            for (BuildBlock buildBlock : builderEntity.currentBuildArea.stackToPlaceMultiBlock) plannedPositions.add(buildBlock.getPos());
        }
        return plannedPositions;
    }

    // ── Dirt path ────────────────────────────────────────────────────────────

    private boolean hasShovel() {
        return builderEntity.getInventory().hasAnyMatching(itemStack -> itemStack.getItem() instanceof ShovelItem);
    }

    /**
     * Dirt path is made with a shovel like a player does: grass/dirt on the spot is flattened without
     * material, on air he needs one dirt. The dirt path item itself is never needed.
     */
    private boolean placeDirtPath(Stack<BlockPos> positions, BlockState levelState) {
        Level level = builderEntity.getCommandSenderWorld();
        boolean paid = builderEntity.currentBuildArea.isPaidWithCurrency();
        ItemStack dirt = null;
        if (!paid) {
            // the next round asks for what is missing
            if (!hasShovel()) {
                blockPos = null;
                return false;
            }
            if (!BuildArea.isPathGround(levelState)) {
                dirt = builderEntity.getMatchingItem(itemStack -> itemStack.is(Items.DIRT));
                if (dirt == null) {
                    blockPos = null;
                    return false;
                }
            }
            builderEntity.switchMainHandItem(itemStack -> itemStack.getItem() instanceof ShovelItem);
        }

        BlockState path = Blocks.DIRT_PATH.defaultBlockState();
        if (isBlockedByEntity(level, blockPos, path)) {
            if (isBuilderIn(blockPos) && stepAside(level, blockPos, null)) return true;
            if (!positions.isEmpty()) {
                blockPos = positions.pop();
                return true;
            }
            blockPos = null;
            return false;
        }

        level.setBlockAndUpdate(blockPos, path);
        level.playSound(null, blockPos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
        builderEntity.swing(InteractionHand.MAIN_HAND);
        if (dirt != null) dirt.shrink(1);
        if (!paid) builderEntity.damageMainHandItem();
        builderEntity.currentBuildArea.removeBuildBlockToPlace(blockPos);
        return true;
    }

    //PERFORMANCE HEAVY DO NOT USE FREQUENTLY
    private boolean canSeeBlock(Level level, Vec3 start, BlockPos target) {
        Vec3 targetCenter = target.getCenter();
        ClipContext ctx = new ClipContext(start, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, builderEntity);
        BlockPos ctxPos = level.clip(ctx).getBlockPos();
        return ctxPos.equals(target);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public static List<BuildArea> getAvailableWorkAreasByPriority(ServerLevel level, BuilderEntity builderEntity, @Nullable BuildArea currentArea) {
        List<BuildArea> list = builderEntity.findWorkAreas(BuildArea.class, 64);

        Map<BuildArea, Integer> priorityMap = new HashMap<>();

        for (BuildArea area : list) {
            if (area == null || area == currentArea || !area.canWorkHere(builderEntity)) continue;

            if(area.isDone() || area.stackToPlace.isEmpty()) continue;

            int priority = 0;

            boolean perfectCandidate = true;//area.isWorkerPerfectCandidate(builderEntity);

            if (perfectCandidate) {
                priority += 10;
            } else {
                priority += 1;
            }

            if (!area.isBeingWorkedOn()) {
                priority += 3;
            }

            priority += area.time;

            //double dist = area.position().distanceToSqr(builderEntity.position());
            //priority -= dist / 10.0;

            priorityMap.put(area, priority);
        }


        List<BuildArea> sorted = new ArrayList<>(priorityMap.keySet());
        sorted.sort((a, b) -> Integer.compare(priorityMap.get(b), priorityMap.get(a)));

        return sorted;
    }



    public boolean moveToPosition(BlockPos pos, int threshold){
        if(pos == null){
            return false;
        }
        else{
            double distance = builderEntity.getHorizontalDistanceTo(pos.getCenter());
            if(distance < threshold){
                builderEntity.getNavigation().stop();
                return false;
            }
            else{

                // start() already claimed the working state; calling it here every
                // tick would override owner commands and pull the worker back to work.
                builderEntity.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
                builderEntity.getLookControl().setLookAt(pos.getCenter());
            }
            return true;
        }
    }

    public enum State{
        SELECT_WORK_AREA,
        MOVE_TO_WORK_AREA,
        PREPARE_FREE_AREA,
        FREE_AREA,
        PREPARE_BREAK_BLOCKS,
        BREAK_BLOCKS,
        PREPARE_PLACE_BLOCKS,
        PLACE_BLOCKS,
        WAIT_FOR_MATERIALS,
        PREPARE_PLACE_MULTIBLOCK,
        PLACE_MULTIBLOCK,
        DONE,
        ERROR
    }
}