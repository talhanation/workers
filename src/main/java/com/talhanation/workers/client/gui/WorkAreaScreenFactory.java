package com.talhanation.workers.client.gui;

import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.AnimalPenArea;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.entities.workarea.CropArea;
import com.talhanation.workers.entities.workarea.FishingArea;
import com.talhanation.workers.entities.workarea.HomeArea;
import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.entities.workarea.LumberArea;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.entities.workarea.MiningArea;
import com.talhanation.workers.entities.workarea.StorageArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class WorkAreaScreenFactory {
    private WorkAreaScreenFactory() {
    }

    public static void open(AbstractWorkAreaEntity area, Player player) {
        Screen screen = create(area, player);
        if (screen != null) {
            Minecraft.getInstance().setScreen(screen);
        }
    }

    private static Screen create(AbstractWorkAreaEntity area, Player player) {
        if (area instanceof AnimalPenArea value) return new AnimalPenAreaScreen(value, player);
        if (area instanceof BuildArea value) return new BuildAreaScreen(value, player);
        if (area instanceof CropArea value) return new CropAreaScreen(value, player);
        if (area instanceof FishingArea value) return new FishingAreaScreen(value, player);
        if (area instanceof HomeArea value) return new HomeAreaScreen(value, player);
        if (area instanceof KitchenArea value) return new KitchenAreaScreen(value, player);
        if (area instanceof LumberArea value) return new LumberAreaScreen(value, player);
        if (area instanceof MarketArea value) return new MarketAreaScreen(value, player);
        if (area instanceof MiningArea value) return new MiningAreaScreen(value, player);
        if (area instanceof StorageArea value) return new StorageAreaScreen(value, player);
        return null;
    }
}
