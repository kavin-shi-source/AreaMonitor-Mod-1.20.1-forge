package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.util.AuditLogger;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AreaBackupCommands {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuuMMdd_HHmmss");
    private static final int MAX_BACKUPS = 10;

    private AreaBackupCommands() {}

    public static int backupConfigs(CommandContext<CommandSourceStack> context) {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("areamonitor");
            String timestamp = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_FORMAT);
            Path backupDir = configDir.resolve("backups").resolve("backup_" + timestamp);
            Files.createDirectories(backupDir);

            String[] files = {"areas.json", "blacklist.json", "whitelist.json", "common.toml"};
            int copied = 0;
            for (String f : files) {
                Path src = configDir.resolve(f);
                if (Files.exists(src)) {
                    Files.copy(src, backupDir.resolve(f), StandardCopyOption.REPLACE_EXISTING);
                    copied++;
                }
            }

            cleanupOldBackups(configDir.resolve("backups"));

            context.getSource().sendSystemMessage(
                Component.translatable("area.backup.created", backupDir.getFileName().toString(), copied));
            context.getSource().sendSystemMessage(
                Component.translatable("area.backup.path", backupDir.toString()));
            AuditLogger.log(context.getSource(), "BACKUP", backupDir.getFileName().toString());
        } catch (Exception e) {
            context.getSource().sendSystemMessage(
                Component.translatable("area.backup.failed", e.getMessage()));
            return 0;
        }
        return 1;
    }

    private static void cleanupOldBackups(Path backupsRoot) {
        if (!Files.exists(backupsRoot)) return;
        try (var dirs = Files.list(backupsRoot)) {
            List<Path> sortedDirs = dirs
                .filter(Files::isDirectory)
                .filter(p -> p.getFileName().toString().startsWith("backup_"))
                .sorted()
                .collect(Collectors.toList());
            while (sortedDirs.size() > MAX_BACKUPS) {
                Path oldest = sortedDirs.remove(0);
                deleteDirectoryRecursive(oldest);
                AreaMonitorMod.LOGGER.debug("Removed old backup: {}", oldest);
            }
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.warn("Failed to clean up old backups", e);
        }
    }

    private static void deleteDirectoryRecursive(Path dir) throws IOException {
        List<Path> failed = new ArrayList<>();
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                 .forEach(p -> {
                     try { Files.delete(p); } catch (IOException ex) {
                         failed.add(p);
                     }
                 });
        }
        if (!failed.isEmpty()) {
            AreaMonitorMod.LOGGER.warn("Failed to delete {} file(s) under {}: {}",
                failed.size(), dir, failed.subList(0, Math.min(failed.size(), 5)));
        }
    }
}
