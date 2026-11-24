package com.splegg.trajectory;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/** Простые настройки в config/splegg-trajectory.properties */
public final class TrajectoryConfig {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("splegg-trajectory.properties");

    public static boolean enabled = true;
    /** Начальная скорость яйца (ванильное яйцо = 1.5, SpleeDY = 1.8). */
    public static double speed = 1.8;
    /** Гравитация за тик (ванильное яйцо = 0.03). */
    public static double gravity = 0.03;
    /** Добавляет ли сервер к яйцу движение игрока (A/D, бег). Определяется автокалибровкой. */
    public static boolean inheritMovement = false;
    /** Высота вылета яйца относительно глаз (ванилла = -0.1). Определяется автокалибровкой. */
    public static double spawnYOffset = 0.016;
    /** Насколько впереди глаз (по направлению взгляда) появляется яйцо. Определяется автокалибровкой. */
    public static double spawnForward = 0.75;
    /** Добавка к вертикальной скорости яйца при вылете (сервер может подбрасывать яйцо чуть вверх). Автокалибровка. */
    public static double liftY = 0.043;
    /** Предметы-оружие Splegg (id через запятую), найденные автоматически: из них вылетали яйца. */
    public static final Set<String> weaponItems = new LinkedHashSet<>();
    /** Автоматически подстраивать скорость/инерцию по реальным яйцам после броска лопатой. */
    public static boolean autoCalibrate = true;
    /** Показывать над хотбаром сообщения калибровки и отклонение после каждого броска. */
    public static boolean showCalibrationMessages = false;
    /** Подробный лог бросков и полёта яиц в latest.log (для отладки). */
    public static boolean debugLog = false;
    /** Цвет голограммы траектории. */
    public static int hologramColor = 0x00E5FF;
    /** Цвет маркера, когда яйцо попадёт в игрока/моба. */
    public static int entityHitColor = 0xFF3355;

    private TrajectoryConfig() {}

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(PATH)) {
            p.load(r);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", String.valueOf(enabled)));
            speed = Double.parseDouble(p.getProperty("speed", String.valueOf(speed)));
            gravity = Double.parseDouble(p.getProperty("gravity", String.valueOf(gravity)));
            inheritMovement = Boolean.parseBoolean(p.getProperty("inheritMovement", String.valueOf(inheritMovement)));
            spawnYOffset = Double.parseDouble(p.getProperty("spawnYOffset", String.valueOf(spawnYOffset)));
            spawnForward = Double.parseDouble(p.getProperty("spawnForward", String.valueOf(spawnForward)));
            liftY = Double.parseDouble(p.getProperty("liftY", String.valueOf(liftY)));
            weaponItems.clear();
            for (String id : p.getProperty("weaponItems", "").split(",")) {
                if (!id.isBlank()) weaponItems.add(id.trim());
            }
            autoCalibrate = Boolean.parseBoolean(p.getProperty("autoCalibrate", String.valueOf(autoCalibrate)));
            showCalibrationMessages = Boolean.parseBoolean(p.getProperty("showCalibrationMessages", String.valueOf(showCalibrationMessages)));
            debugLog = Boolean.parseBoolean(p.getProperty("debugLog", String.valueOf(debugLog)));
            hologramColor = parseColor(p.getProperty("hologramColor"), hologramColor);
            entityHitColor = parseColor(p.getProperty("entityHitColor"), entityHitColor);
        } catch (IOException | NumberFormatException e) {
            SpleggTrajectoryClient.LOGGER.warn("Не удалось прочитать конфиг, используются значения по умолчанию", e);
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", String.valueOf(enabled));
        p.setProperty("speed", String.valueOf(speed));
        p.setProperty("gravity", String.valueOf(gravity));
        p.setProperty("inheritMovement", String.valueOf(inheritMovement));
        p.setProperty("spawnYOffset", String.valueOf(spawnYOffset));
        p.setProperty("spawnForward", String.valueOf(spawnForward));
        p.setProperty("liftY", String.valueOf(liftY));
        p.setProperty("weaponItems", String.join(",", weaponItems));
        p.setProperty("autoCalibrate", String.valueOf(autoCalibrate));
        p.setProperty("showCalibrationMessages", String.valueOf(showCalibrationMessages));
        p.setProperty("debugLog", String.valueOf(debugLog));
        p.setProperty("hologramColor", String.format("%06X", hologramColor));
        p.setProperty("entityHitColor", String.format("%06X", entityHitColor));
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH)) {
                p.store(w, "Splegg Trajectory");
            }
        } catch (IOException e) {
            SpleggTrajectoryClient.LOGGER.warn("Не удалось сохранить конфиг", e);
        }
    }

    private static int parseColor(String s, int def) {
        if (s == null) return def;
        try {
            return Integer.parseInt(s.trim().replace("#", ""), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
