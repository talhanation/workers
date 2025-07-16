package com.talhanation.workers.world;

import com.talhanation.workers.entities.workarea.BuildArea;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StructureManager {
    public static CompoundTag scanStructure(Level level, BuildArea buildArea, String name) {
        CompoundTag root = new CompoundTag();
        ListTag blockList = new ListTag();

        Direction facing = buildArea.getFacing();
        Direction right = facing.getClockWise();
        BlockPos origin = buildArea.getOriginPos();

        int width = buildArea.getWidthSize();
        int height = buildArea.getHeightSize();
        int depth = buildArea.getDepthSize();

        for (int y = 0; y < height; y++) {
            for (int dz = 0; dz < depth; dz++) {
                for (int dx = 0; dx < width; dx++) {
                    BlockPos offset = origin
                            .relative(facing, dz)
                            .relative(right, width - 1 - dx)
                            .above(y);

                    BlockState state = level.getBlockState(offset);
                    if (!state.isAir() && !(state.getBlock() instanceof BushBlock)) {
                        CompoundTag blockTag = new CompoundTag();
                        blockTag.putInt("x", dx);
                        blockTag.putInt("y", y);
                        blockTag.putInt("z", dz);

                        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                        blockTag.putString("block", id.toString());
                        blockTag.put("state", NbtUtils.writeBlockState(state));

                        blockList.add(blockTag);
                    }
                }
            }
        }

        root.putString("name", name);
        root.putInt("width", width);
        root.putInt("height", height);
        root.putInt("depth", depth);
        root.putString("facing", facing.getName());
        root.put("blocks", blockList);
        return root;
    }

    public static void saveStructureToFile(String filename, List<ScannedBlock> structure, int width, int height, int depth, Direction facing) {
        File dir = new File(Minecraft.getInstance().gameDirectory, "config/workers/scan");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, filename.endsWith(".nbt") ? filename : filename + ".nbt");

        ListTag list = new ListTag();
        for (ScannedBlock block : structure) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", block.relativePos().getX());
            tag.putInt("y", block.relativePos().getY());
            tag.putInt("z", block.relativePos().getZ());

            CompoundTag stateTag = NbtUtils.writeBlockState(block.state());
            tag.put("state", stateTag);

            list.add(tag);
        }

        CompoundTag root = new CompoundTag();
        root.put("blocks", list);

        root.putString("name", filename);
        root.putInt("width", width);
        root.putInt("height", height);
        root.putInt("depth", depth);
        root.putString("facing", facing.getName());

        try {
            NbtIo.writeCompressed(root, file);
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Scan saved to: " + file.getPath()), true);
        } catch (IOException e) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Error saving scan: " + e.getMessage()), true);
            e.printStackTrace();
        }
    }

    public static List<String> loadAvailableScans() {
        List<String> scanNames = new ArrayList<>();
        Path path = Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "config", "workers", "scan");
        if (Files.exists(path) && Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.nbt")) {
                for (Path path1 : stream) {
                    String fileName = path1.getFileName().toString();

                    if (fileName.endsWith(".nbt")) {
                        scanNames.add(fileName.substring(0, fileName.length() - 4));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return scanNames;
    }

    public static CompoundTag loadScanNbt(String scanName) {
        Path scanFile = Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "config", "workers", "scan", scanName + ".nbt");

        try (InputStream input = Files.newInputStream(scanFile)) {
            return NbtIo.readCompressed(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
