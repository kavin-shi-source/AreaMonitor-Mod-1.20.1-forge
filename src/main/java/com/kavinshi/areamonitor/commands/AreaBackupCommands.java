package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.util.AuditLogger;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AreaBackupCommands {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuuMMdd_HHmmss");

    private AreaBackupCommands() {}

    public static int backupConfigs(CommandContext<CommandSourceStack> context) {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("areamonitor");
            String timestamp = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_FORMAT);
            Path backupDir = configDir.resolve("backups").resolve("backup_" + timestamp);
            Files.createDirectories(backupDir);

            String[] files = {"areas.json", "blacklist.json"};
            int copied = 0;
            for (String f : files) {
                Path src = configDir.resolve(f);
                if (Files.exists(src)) {
                    Files.copy(src, backupDir.resolve(f));
                    copied++;
                }
            }
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
}
