package com.talhanation.workers.world.template;

import com.talhanation.workers.WorkersMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Stream;

/**
 * Server-side list of all building templates in {@code <server>/workers/scan}.
 * <p>
 * The client copies the default scans for itself, but a dedicated server does not load {@code assets/},
 * so on server start the defaults are copied from the mod file into {@code scan/vanilla/}. This happens
 * once per mod version, so deleted defaults stay deleted until the next update brings new ones.
 * <p>
 * Files are parsed once and again only when they changed. {@link #refresh} is rate limited,
 * so callers (the Governor) may call it whenever they need a template.
 */
public final class TemplateIndex {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DEFAULTS_MARKER = ".defaults_version";
    private static final long REFRESH_INTERVAL_MS = 30_000L;

    @Nullable
    private static TemplateIndex instance;

    private final Path scanRoot;
    private final Map<String, TemplateInfo> templates = new LinkedHashMap<>();
    private long lastRefresh;

    private TemplateIndex(Path scanRoot) {
        this.scanRoot = scanRoot;
    }

    @Nullable
    public static TemplateIndex get() {
        return instance;
    }

    public static Path getScanRoot(MinecraftServer server) {
        return server.getServerDirectory().toAbsolutePath().resolve("workers").resolve("scan");
    }

    // ------------------------------------------------------------------ lifecycle

    public static void onServerStarting(MinecraftServer server) {
        Path root = getScanRoot(server);
        copyDefaultsIfNewVersion(root);
        instance = new TemplateIndex(root);
        instance.refresh(true);
        LOGGER.info("Workers: {} building templates found, {} valid", instance.templates.size(),
                instance.templates.values().stream().filter(TemplateInfo::isValid).count());
    }

    public static void onServerStopped() {
        instance = null;
    }

    // ------------------------------------------------------------------ queries

    public Collection<TemplateInfo> getAll() {
        return Collections.unmodifiableCollection(templates.values());
    }

    @Nullable
    public TemplateInfo get(String path) {
        return templates.get(path);
    }

    /** All templates of the type, valid or not (the GUI shows the invalid ones greyed out with their reason). */
    public List<TemplateInfo> getByType(BuildingType type, @Nullable String faction) {
        List<TemplateInfo> list = new ArrayList<>();
        for (TemplateInfo info : templates.values()) {
            if (info.type() != type) continue;
            if (info.faction() != null && !info.faction().equals(faction)) continue;
            list.add(info);
        }
        return list;
    }

    /**
     * Valid templates of the type for a biome style: the style's own first, then templates without a style
     * (custom scans), then plains as the last fallback. Empty if there is none at all.
     */
    public List<TemplateInfo> getDefaults(BuildingType type, BiomeStyle style, @Nullable String faction) {
        List<TemplateInfo> own = new ArrayList<>();
        List<TemplateInfo> any = new ArrayList<>();
        List<TemplateInfo> plains = new ArrayList<>();
        for (TemplateInfo info : getByType(type, faction)) {
            if (!info.isValid()) continue;
            if (info.style() == style) own.add(info);
            else if (info.style() == null) any.add(info);
            else if (info.style() == BiomeStyle.PLAINS) plains.add(info);
        }
        if (!own.isEmpty()) return own;
        if (!any.isEmpty()) return any;
        return plains;
    }

    /** Reads the NBT of a template, null if the file is gone or broken. */
    @Nullable
    public CompoundTag load(TemplateInfo info) {
        Path file = resolve(info.path());
        if (file == null || !Files.isRegularFile(file)) return null;
        try (InputStream in = Files.newInputStream(file)) {
            return NbtIo.readCompressed(in, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            LOGGER.warn("Workers: could not read template {}: {}", info.path(), e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ scanning

    /** Looks for new, changed and removed files. Without force at most every 30 seconds. */
    public void refresh(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastRefresh < REFRESH_INTERVAL_MS) return;
        lastRefresh = now;

        if (!Files.isDirectory(scanRoot)) {
            templates.clear();
            return;
        }

        Set<String> seen = new HashSet<>();
        try (Stream<Path> files = Files.walk(scanRoot)) {
            files.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".nbt")).forEach(file -> {
                String rel = scanRoot.relativize(file).toString().replace('\\', '/');
                seen.add(rel);
                long modified = lastModified(file);
                TemplateInfo known = templates.get(rel);
                if (known != null && known.lastModified() == modified) return;
                TemplateInfo info = parse(file, rel, modified);
                if (info != null) templates.put(rel, info);
                else templates.remove(rel);
            });
        } catch (IOException e) {
            LOGGER.warn("Workers: could not list templates: {}", e.getMessage());
        }
        templates.keySet().retainAll(seen);
    }

    @Nullable
    private TemplateInfo parse(Path file, String rel, long modified) {
        CompoundTag root;
        try (InputStream in = Files.newInputStream(file)) {
            root = NbtIo.readCompressed(in, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            LOGGER.warn("Workers: broken template {}: {}", rel, e.getMessage());
            return null;
        }
        if (!root.contains("blocks")) return null;

        String[] parts = rel.split("/");
        String fileName = parts[parts.length - 1].replace(".nbt", "");

        BiomeStyle style = null;
        String faction = null;
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("factions") && i + 1 < parts.length - 1) faction = parts[i + 1];
            BiomeStyle s = BiomeStyle.fromKey(parts[i]);
            if (s != null) style = s;
        }
        // the biome prefix of the default names is not part of the type keyword
        if (style != null && fileName.startsWith(style.getKey() + "_")) fileName = fileName.substring(style.getKey().length() + 1);

        TemplateValidator.Content content = TemplateValidator.read(root);
        BuildingType type = TemplateValidator.detectType(fileName, content);
        String reason = type == null ? null : TemplateValidator.validate(type, content);

        return new TemplateInfo(rel, style, faction, type, reason, content.width, content.height, content.depth, content.beds, modified);
    }

    @Nullable
    private Path resolve(String rel) {
        Path file = scanRoot.resolve(rel).normalize();
        return file.startsWith(scanRoot) ? file : null;
    }

    private static long lastModified(Path file) {
        try {
            FileTime time = Files.getLastModifiedTime(file);
            return time.toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    // ------------------------------------------------------------------ defaults

    /**
     * Copies the default scans from the mod file ({@code assets/workers/structures}) into the scan folder.
     * Only once per mod version and only files that are missing, a player's own files are never overwritten.
     */
    private static void copyDefaultsIfNewVersion(Path scanRoot) {
        String version = ModList.get().getModContainerById(WorkersMain.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("dev");
        Path marker = scanRoot.resolve(DEFAULTS_MARKER);
        try {
            if (Files.isRegularFile(marker) && Files.readString(marker).trim().equals(version)) return;
        } catch (IOException ignored) {
        }

        IModFileInfo modFile = ModList.get().getModFileById(WorkersMain.MOD_ID);
        if (modFile == null) return;
        Path source = modFile.getFile().findResource("assets", WorkersMain.MOD_ID, "structures");
        if (!Files.isDirectory(source)) return;

        int copied = 0;
        try (Stream<Path> files = Files.walk(source)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".nbt"))::iterator) {
                String rel = source.relativize(file).toString().replace('\\', '/').toLowerCase(Locale.ROOT);
                Path dest = scanRoot.resolve(rel);
                if (Files.exists(dest)) continue;
                Files.createDirectories(dest.getParent());
                try (InputStream in = Files.newInputStream(file)) {
                    Files.copy(in, dest);
                }
                copied++;
            }
            Files.createDirectories(scanRoot);
            Files.writeString(marker, version);
        } catch (IOException e) {
            LOGGER.warn("Workers: could not copy the default templates: {}", e.getMessage());
            return;
        }
        if (copied > 0) LOGGER.info("Workers: copied {} default building templates to {}", copied, scanRoot);
    }
}
