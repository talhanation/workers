package com.talhanation.workers.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.config.BuildPaymentMode;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.world.BuildingPrice;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
public class MessageUpdateBuildArea implements Message<MessageUpdateBuildArea> {

    public UUID uuid;
    public CompoundTag structureNBT;
    public int xSize;
    public int ySize;
    public int zSize;
    public boolean build;
    public boolean isCreative;
    public boolean freeArea;
    /** Whether the alwaysShowProjection value below should be applied. Size/build
     *  updates leave this false so they never overwrite the projection setting. */
    public boolean updateAlwaysShowProjection;
    public boolean alwaysShowProjection;
    public MessageUpdateBuildArea() {}
    public MessageUpdateBuildArea(UUID uuid, int xSize, int ySize, int zSize, CompoundTag structureNBT, boolean build, boolean isCreative, boolean freeArea) {
        this.uuid = uuid;
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        this.structureNBT = structureNBT;
        this.build = build;
        this.isCreative = isCreative;
        this.freeArea = freeArea;
        this.updateAlwaysShowProjection = false;
        this.alwaysShowProjection = false;
    }
    public MessageUpdateBuildArea(UUID uuid, int xSize, int ySize, int zSize, CompoundTag structureNBT, boolean build, boolean isCreative, boolean freeArea, boolean alwaysShowProjection) {
        this(uuid, xSize, ySize, zSize, structureNBT, build, isCreative, freeArea);
        this.updateAlwaysShowProjection = true;
        this.alwaysShowProjection = alwaysShowProjection;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(BuildArea.class, player.getBoundingBox()
                        .inflate(32.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(buildArea -> this.update(player, buildArea));

    }

    public void update(ServerPlayer player, BuildArea buildArea){
        //a paid building can not be swapped while it is under construction (pay hut -> build mansion)
        if(buildArea.isPaidWithCurrency() && !buildArea.stackToPlace.isEmpty() && !buildArea.getStructureNBT().equals(this.structureNBT)){
            player.sendSystemMessage(Translatable.TEXT_BUILDING_ALREADY_PAID);
            return;
        }

        buildArea.setWidthSize(this.xSize);
        buildArea.setHeightSize(this.ySize);
        buildArea.setDepthSize(this.zSize);
        buildArea.setStructureNBT(this.structureNBT);
        buildArea.setFreeArea(this.freeArea);
        if(this.updateAlwaysShowProjection){
            buildArea.setAlwaysShowProjection(this.alwaysShowProjection);
        }

        boolean currencyMode = WorkersServerConfig.BuildPaymentModeConfig.get() == BuildPaymentMode.CURRENCY;
        if(currencyMode){
            buildArea.setCurrencyPrice(buildArea.calculateCurrencyPrice(this.structureNBT));
        }

        if(build){
            //never trust the client: instant placing only for creative players
            boolean creative = this.isCreative && player.isCreative();

            if(!creative && currencyMode){
                this.buildWithCurrency(player, buildArea);
            }
            else{
                buildArea.setPaidWithCurrency(false);
                buildArea.setStartBuild(creative);
            }
        }
    }

    private void buildWithCurrency(ServerPlayer player, BuildArea buildArea){
        if(buildArea.isPaidWithCurrency() && !buildArea.stackToPlace.isEmpty()){
            player.sendSystemMessage(Translatable.TEXT_BUILDING_ALREADY_PAID);
            return;
        }

        //only the blocks that are still missing in the world are charged
        buildArea.setStartBuild(false);
        Map<Item, Integer> materials = buildArea.getRemainingMaterialMap();

        Item blocked = BuildingPrice.findBlocked(materials);
        if(blocked != null){
            buildArea.cancelBuild();
            player.sendSystemMessage(Translatable.TEXT_BUILDING_BLOCKED_MATERIAL(blocked.getDescription().getString()));
            return;
        }

        int price = BuildingPrice.calculate(materials);
        String currencyName = FactionEvents.getCurrency().getHoverName().getString();
        if(!FactionEvents.playerHasEnoughEmeralds(player, price)){
            buildArea.cancelBuild();
            player.sendSystemMessage(Translatable.TEXT_BUILDING_NOT_ENOUGH_CURRENCY(price, currencyName));
            return;
        }

        FactionEvents.doPayment(player, price);
        buildArea.setPaidWithCurrency(true);
        player.sendSystemMessage(Translatable.TEXT_BUILDING_PAID(price, currencyName));
    }

    @Override
    public MessageUpdateBuildArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.xSize = buf.readInt();
        this.ySize = buf.readInt();
        this.zSize = buf.readInt();

        byte[] compressed = buf.readByteArray();
        try {
            this.structureNBT = NbtIo.readCompressed(new ByteArrayInputStream(compressed));
        } catch (IOException e) {
            e.printStackTrace();
            this.structureNBT = new CompoundTag(); // Fallback
        }

        this.build = buf.readBoolean();
        this.isCreative = buf.readBoolean();
        this.freeArea = buf.readBoolean();
        this.updateAlwaysShowProjection = buf.readBoolean();
        this.alwaysShowProjection = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeInt(xSize);
        buf.writeInt(ySize);
        buf.writeInt(zSize);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            NbtIo.writeCompressed(structureNBT, out);
            byte[] compressed = out.toByteArray();
            buf.writeByteArray(compressed);
        } catch (IOException e) {
            e.printStackTrace();
            buf.writeByteArray(new byte[0]);
        }

        buf.writeBoolean(build);
        buf.writeBoolean(isCreative);
        buf.writeBoolean(freeArea);
        buf.writeBoolean(updateAlwaysShowProjection);
        buf.writeBoolean(alwaysShowProjection);
    }
}
