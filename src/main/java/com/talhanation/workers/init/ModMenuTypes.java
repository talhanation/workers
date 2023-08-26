package com.talhanation.workers.init;

import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nullable;

import com.talhanation.workers.client.gui.*;
import com.talhanation.workers.inventory.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.talhanation.workers.Main;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.MinerEntity;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    private static final Logger logger = LogManager.getLogger(Main.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Main.MOD_ID);

    public static final RegistryObject<MenuType<WorkerHireContainer>> HIRE_CONTAINER_TYPE =
            MENU_TYPES.register("hire_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                try {
                    UUID workerId = data.readUUID();
                    logger.info("{} is opening hire container for {}", inv.player.getDisplayName().getString(),
                            workerId);

                    AbstractWorkerEntity rec = getRecruitByUUID(inv.player, workerId);
                    logger.info("Recruit is {}", rec);
                    if (rec == null) {
                        return null;
                    }

                    return new WorkerHireContainer(windowId, inv.player, rec, inv);
                } catch (Exception e) {
                    logger.error("Error in hire container: ");
                    logger.error(e.getMessage());
                    logger.error(Arrays.toString(e.getStackTrace()));
                    return null;
                }
            }));

    public static final RegistryObject<MenuType<WorkerInventoryContainer>> MINER_CONTAINER_TYPE =
            MENU_TYPES.register("miner_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                MinerEntity rec = (MinerEntity) getRecruitByUUID(inv.player, data.readUUID());
                if (rec == null) {
                    return null;
                }
                return new WorkerInventoryContainer(windowId, rec, inv);
            }));

    public static final RegistryObject<MenuType<WorkerInventoryContainer>> ANIMAL_FARMER_CONTAINER_TYPE =
            MENU_TYPES.register("animal_farmer_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                AbstractWorkerEntity rec = getRecruitByUUID(inv.player, data.readUUID());
                if (rec == null) {
                    return null;
                }
                return new WorkerInventoryContainer(windowId, rec, inv);
            }));

    public static final RegistryObject<MenuType<WorkerInventoryContainer>> WORKER_CONTAINER_TYPE =
            MENU_TYPES.register("worker_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                AbstractWorkerEntity rec = getRecruitByUUID(inv.player, data.readUUID());
                if (rec == null) {
                    return null;
                }
                return new WorkerInventoryContainer(windowId, rec, inv);
            }));

    public static final RegistryObject<MenuType<MerchantTradeContainer>> MERCHANT_CONTAINER_TYPE =
            MENU_TYPES.register("merchant_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                MerchantEntity rec = (MerchantEntity) getRecruitByUUID(inv.player, data.readUUID());
                if (rec == null) {
                    return null;
                }
                return new MerchantTradeContainer(windowId, rec, inv);
            }));

    public static final RegistryObject<MenuType<MerchantInventoryContainer>> MERCHANT_OWNER_CONTAINER_TYPE =
            MENU_TYPES.register("merchant_owner_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                MerchantEntity rec = (MerchantEntity) getRecruitByUUID(inv.player, data.readUUID());
                if (rec == null) {
                    return null;
                }
                return new MerchantInventoryContainer(windowId, rec, inv);
            }));

    public static final RegistryObject<MenuType<CommandMenu>> COMMAND_CONTAINER_TYPE =
            MENU_TYPES.register("command_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                Player player = inv.player;
                return new CommandMenu(windowId, player);
            }));

    public static final RegistryObject<MenuType<MerchantWaypointContainer>> WAYPOINT_CONTAINER_TYPE =
            MENU_TYPES.register("waypoint_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                try {
                    UUID workerId = data.readUUID();
                    logger.info("{} is opening waypoint container for {}", inv.player.getDisplayName().getString(),
                            workerId);

                    AbstractWorkerEntity rec = getRecruitByUUID(inv.player, workerId);
                    logger.info("worker is {}", rec);
                    if (rec == null) {
                        return null;
                    }

                    return new MerchantWaypointContainer(windowId, inv.player, rec, inv);
                } catch (Exception e) {
                    logger.error("Error in hire container: ");
                    logger.error(e.getMessage());
                    logger.error(Arrays.toString(e.getStackTrace()));
                    return null;
                }
            }));

    public static void registerMenus() {
        registerMenu(MINER_CONTAINER_TYPE.get(), MinerInventoryScreen::new);
        registerMenu(WORKER_CONTAINER_TYPE.get(), WorkerInventoryScreen::new);
        registerMenu(MERCHANT_CONTAINER_TYPE.get(), MerchantTradeScreen::new);
        registerMenu(MERCHANT_OWNER_CONTAINER_TYPE.get(), MerchantOwnerScreen::new);
        registerMenu(ANIMAL_FARMER_CONTAINER_TYPE.get(), AnimalFarmerInventoryScreen::new);
        registerMenu(HIRE_CONTAINER_TYPE.get(), WorkerHireScreen::new);
        registerMenu(WAYPOINT_CONTAINER_TYPE.get(), MerchantWaypointScreen::new);
        registerMenu(COMMAND_CONTAINER_TYPE.get(), CommandScreen::new);
        logger.info("MenuScreens registered");
    }

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
        return player.level.getEntitiesOfClass(AbstractWorkerEntity.class,
                new AABB(player.getX() - distance, player.getY() - distance, player.getZ() - distance,
                        player.getX() + distance, player.getY() + distance, player.getZ() + distance),
                entity -> entity.getUUID().equals(uuid)).stream().findAny().orElse(null);
    }
}
