package com.kavinshi.areamonitor.util;

import com.kavinshi.areamonitor.AreaMonitorMod;
import net.minecraft.commands.CommandSourceStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * P2 #24: Lightweight audit logger for administrative commands.
 * Writes timestamped entries to config/areamonitor/audit.log via a single-thread
 * executor to avoid blocking the main thread.
 */
public final class AuditLogger {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AreaMonitor-AuditIO");
        t.setDaemon(true);
        return t;
    });

    private static Path auditFile;

    private AuditLogger() {}

    private static Path getAuditFile() {
        if (auditFile == null) {
            auditFile = Path.of("config", "areamonitor", "audit.log");
        }
        return auditFile;
    }

    public static void log(CommandSourceStack source, String action, String detail) {
        String sourceName = source.getTextName();
        String timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(TS_FORMAT);
        String entry = String.format("[%s] %s | %s | %s%n", timestamp, sourceName, action, detail != null ? detail : "");

        IO_EXECUTOR.submit(() -> {
            try {
                Path file = getAuditFile();
                Files.createDirectories(file.getParent());
                Files.writeString(file, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("Failed to write audit log", e);
            }
        });
    }

    public static void log(CommandSourceStack source, String action) {
        log(source, action, null);
    }
}
