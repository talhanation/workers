package com.talhanation.workers.init;

import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nullable;

import com.talhanation.workers.client.gui.CourierScreen;
import com.talhanation.workers.client.gui.MerchantAddEditTradeScreen;
import com.talhanation.workers.client.gui.MerchantTradeScreen;
import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.CourierContainer;
import com.talhanation.workers.inventory.MerchantAddEditTradeContainer;
import com.talhanation.workers.inventory.MerchantTradeContainer;
import com.talhanation.workers.world.WorkersMerchantTrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
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
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModMenuTypes {
    private static final Logger logger = LogManager.getLogger(WorkersMain.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, WorkersMain.MOD_ID);

    public static void registerMenus() {
        registerMenu(MERCHANT_ADD_EDIT_TRADE_CONTAINER_TYPE.get(), MerchantAddEditTradeScreen::new);
        registerMenu(MERCHANT_TRADE_CONTAINER_TYPE.get(), MerchantTradeScreen::new);
        registerMenu(COURIER_CONTAINER_TYPE.get(), CourierScreen::new);
    }

    public static final RegistryObject<MenuType<MerchantAddEditTradeContainer>> MERCHANT_ADD_EDIT_TRADE_CONTAINER_TYPE =
            MENU_TYPES.register("merchant_add_edit_trade_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                MerchantEntity merchant = (MerchantEntity) getRecruitByUUID(inv.player, data.readUUID());
                CompoundTag nbt = data.readNbt();
                if (merchant == null || nbt == null) {
                    return null;
                }
                WorkersMerchantTrade trade = WorkersMerchantTrade.fromNbt(nbt);
                return new MerchantAddEditTradeContainer(windowId, merchant, inv, trade);
            }));

    public static final RegistryObject<MenuType<MerchantTradeContainer>> MERCHANT_TRADE_CONTAINER_TYPE =
            MENU_TYPES.register("merchant_trade_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                MerchantEntity merchant = (MerchantEntity) getRecruitByUUID(inv.player, data.readUUID());
                if (merchant == null) {
                    return null;
                }
                return new MerchantTradeContainer(windowId, merchant, inv);
            }));

    public static final RegistryObject<MenuType<CourierContainer>> COURIER_CONTAINER_TYPE =
            MENU_TYPES.register("courier_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                CourierEntity courier = (CourierEntity) getRecruitByUUID(inv.player, data.readUUID());
                if (courier == null) return null;
                return new CourierContainer(windowId, courier, inv);
            }));


    /**
     * Registers a menuType/container with a screen constructor.
     * It has a try/catch block because the Forge screen constructor fails silently.
     */
    private static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerMenu(
            MenuType<? extends M> menuType, ScreenConstructor<M, U> screenConstructor) {
        MenuScreens.register(menuType, (ScreenConstructor<M, U>) (menu, inventory, title) -> {
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
