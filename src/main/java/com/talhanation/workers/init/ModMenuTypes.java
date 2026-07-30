package com.talhanation.workers.init;

import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nullable;

import com.talhanation.workers.client.gui.CourierScreen;
import com.talhanation.workers.client.gui.MerchantAddEditTradeScreen;
import com.talhanation.workers.client.gui.MerchantTradeScreen;
import com.talhanation.workers.client.gui.MerchantAddEditVillagerTradeScreen;
import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.CourierContainer;
import com.talhanation.workers.inventory.MerchantAddEditTradeContainer;
import com.talhanation.workers.inventory.MerchantTradeContainer;
import com.talhanation.workers.inventory.MerchantAddEditVillagerTradeContainer;
import com.talhanation.workers.world.WorkersMerchantTrade;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.talhanation.workers.WorkersMain;

import com.talhanation.workers.entities.AbstractWorkerEntity;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = WorkersMain.MOD_ID, value = Dist.CLIENT)
public class ModMenuTypes {
    private static final Logger logger = LogManager.getLogger(WorkersMain.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, WorkersMain.MOD_ID);

    public static void registerMenus() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        registerMenu(event, MERCHANT_ADD_EDIT_TRADE_CONTAINER_TYPE.get(), MerchantAddEditTradeScreen::new);
        registerMenu(event, MERCHANT_VILLAGER_TRADE_CONTAINER_TYPE.get(), MerchantAddEditVillagerTradeScreen::new);
        registerMenu(event, MERCHANT_TRADE_CONTAINER_TYPE.get(), MerchantTradeScreen::new);
        registerMenu(event, COURIER_CONTAINER_TYPE.get(), CourierScreen::new);
    }

    public static final DeferredHolder<MenuType<?>, MenuType<MerchantAddEditTradeContainer>> MERCHANT_ADD_EDIT_TRADE_CONTAINER_TYPE =
            MENU_TYPES.register("merchant_add_edit_trade_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                MerchantEntity merchant = (MerchantEntity) getRecruitByUUID(inv.player, data.readUUID());
                CompoundTag nbt = data.readNbt();
                if (merchant == null || nbt == null) {
                    return null;
                }
                WorkersMerchantTrade trade = WorkersMerchantTrade.fromNbt(data.registryAccess(), nbt);
                return new MerchantAddEditTradeContainer(windowId, merchant, inv, trade);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<MerchantAddEditVillagerTradeContainer>> MERCHANT_VILLAGER_TRADE_CONTAINER_TYPE =
            MENU_TYPES.register("merchant_villager_trade_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                MerchantEntity merchant = (MerchantEntity) getRecruitByUUID(inv.player, data.readUUID());
                CompoundTag nbt = data.readNbt();
                if (merchant == null || nbt == null) {
                    return null;
                }
                WorkersMerchantTrade trade = WorkersMerchantTrade.fromNbt(data.registryAccess(), nbt);
                return new MerchantAddEditVillagerTradeContainer(windowId, merchant, inv, trade);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<MerchantTradeContainer>> MERCHANT_TRADE_CONTAINER_TYPE =
            MENU_TYPES.register("merchant_trade_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                MerchantEntity merchant = (MerchantEntity) getRecruitByUUID(inv.player, data.readUUID());
                if (merchant == null) {
                    return null;
                }
                return new MerchantTradeContainer(windowId, merchant, inv);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<CourierContainer>> COURIER_CONTAINER_TYPE =
            MENU_TYPES.register("courier_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                CourierEntity courier = (CourierEntity) getRecruitByUUID(inv.player, data.readUUID());
                if (courier == null) return null;
                return new CourierContainer(windowId, courier, inv);
            }));


    /**
     * Registers a menuType/container with a screen constructor.
     * It has a try/catch block because the Forge screen constructor fails silently.
     */
    private static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerMenu(
            RegisterMenuScreensEvent event, MenuType<? extends M> menuType, ScreenConstructor<M, U> screenConstructor) {
        event.register(menuType, (ScreenConstructor<M, U>) (menu, inventory, title) -> {
            try {
                return screenConstructor.create(menu, inventory, title);
            } catch (Exception e) {
                logger.error("Could not instantiate {}", screenConstructor.getClass().getSimpleName());
                logger.error(e.getMessage());
                logger.error(Arrays.toString(e.getStackTrace()));
                return null;
            }
        });
    }

    @Nullable
    private static AbstractWorkerEntity getRecruitByUUID(Player player, UUID uuid) {
        double distance = 10D;
        return player.getCommandSenderWorld().getEntitiesOfClass(AbstractWorkerEntity.class,
                new AABB(player.getX() - distance, player.getY() - distance, player.getZ() - distance,
                        player.getX() + distance, player.getY() + distance, player.getZ() + distance),
                entity -> entity.getUUID().equals(uuid)).stream().findAny().orElse(null);
    }
}
