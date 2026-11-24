package com.splegg.trajectory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Следит за яйцами, которые выпустил игрок, и по ним:
 * 1) находит, каким предметом на сервере стреляют яйцами (если это не лопата);
 * 2) калибрует скорость, инерцию движения и точку вылета;
 * 3) считает, насколько предсказанная точка разошлась с реальной.
 */
public final class ThrowTracker {
    /** Состояние игрока на конец клиентского тика. */
    public record Sample(Vec3d eye, Vec3d dir, Vec3d move, boolean onGround,
                         String itemId, Text itemName, boolean trajectoryItem,
                         List<Vec3d> predictedPath) {}

    private static final class TrackedEgg {
        final EggEntity egg;
        final Sample sample;
        final Vec3d spawnPos;
        Vec3d lastPos;
        /** Реальный путь яйца после запуска. */
        final List<Vec3d> path = new ArrayList<>();
        /** Сколько тиков яйцо летит с настоящей скоростью (сервер может выдать её не сразу). */
        int ticks;
        boolean launched;
        boolean measured;

        TrackedEgg(EggEntity egg, Sample sample) {
            this.egg = egg;
            this.sample = sample;
            this.spawnPos = egg.getPos();
            this.lastPos = spawnPos;
        }
    }

    private static final int HISTORY_TICKS = 40;
    /** На каком тике полёта измерять скорость. */
    private static final int MEASURE_TICK = 3;
    private static final double DRAG = 0.99;
    /** Скорость, начиная с которой считаем, что сервер уже «запустил» яйцо. */
    private static final double LAUNCH_SPEED = 0.3;

    private static final ArrayDeque<Sample> history = new ArrayDeque<>();
    private static final List<TrackedEgg> tracked = new ArrayList<>();
    /** > 0 — сервер добавляет движение игрока к яйцу, < 0 — нет. */
    private static double inheritScore = TrajectoryConfig.inheritMovement ? 0.05 : -0.05;
    private static int speedSamples;
    private static int liftSamples;

    private ThrowTracker() {}

    private static String lastHandsInfo = "";

    public static void recordTick(ClientPlayerEntity player) {
        String hands = "слот " + player.getInventory().selectedSlot + ", основная: " + describe(player.getMainHandStack())
                + ", вторая: " + describe(player.getOffHandStack());
        if (!hands.equals(lastHandsInfo)) {
            lastHandsInfo = hands;
            log("В руках: {}", hands);
        }

        ItemStack stack = SpleggTrajectoryClient.heldTrajectoryItem(player);
        boolean trajectory = stack != null;
        if (stack == null) stack = player.getMainHandStack();

        List<Vec3d> predicted = null;
        if (trajectory) {
            predicted = TrajectorySimulator.simulate(player, 1f, SpleggTrajectoryClient.paramsFor(stack)).points();
        }
        history.addLast(new Sample(
                player.getEyePos(),
                TrajectorySimulator.launchDirection(player.getPitch(), player.getYaw()),
                TrajectorySimulator.playerMovement(player),
                player.isOnGround(),
                stack.isEmpty() ? null : SpleggTrajectoryClient.itemId(stack),
                stack.getName(),
                trajectory,
                predicted));
        while (history.size() > HISTORY_TICKS) history.removeFirst();
    }

    public static void reset() {
        history.clear();
        tracked.clear();
    }

    public static void onEntityLoad(Entity entity) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || !(entity instanceof ProjectileEntity projectile)) return;
        Vec3d spawn = projectile.getPos();
        double distToEye = spawn.distanceTo(player.getEyePos());
        if (distToEye > 6) return;

        Entity owner = projectile.getOwner();
        String ownerName = owner == null ? "нет" : owner == player ? "я" : owner.getName().getString();
        if (!(entity instanceof EggEntity egg)) {
            log("Рядом появился снаряд {} (не яйцо), владелец: {}, до глаз {}",
                    entity.getType().getUntranslatedName(), ownerName, fmt(distToEye));
            return;
        }

        boolean mine = owner == player || (owner == null && distToEye < 2);
        Sample best = findThrowSample(spawn);
        Vec3d offset = best != null ? spawn.subtract(best.eye()) : null;
        if (best != null) log("Взгляд при броске {}, движение {}", fmtVec(best.dir()), fmtVec(best.move()));
        log("Яйцо: владелец {}, до глаз {}, скорость {}, предмет {}, смещение от глаз {}",
                ownerName, fmt(distToEye), fmt(egg.getVelocity().length()),
                best != null ? best.itemId() : "?", offset != null ? fmtVec(offset) : "?");
        if (!mine || best == null) return;

        // Яйцо вылетело из предмета, который мы ещё не знаем, — запоминаем его как оружие Splegg
        if (!best.trajectoryItem() && best.itemId() != null && TrajectoryConfig.weaponItems.add(best.itemId())) {
            TrajectoryConfig.save();
            log("Новое оружие Splegg: {}", best.itemId());
            if (TrajectoryConfig.showCalibrationMessages) HudMessage.show(Text.literal("Оружие Splegg найдено: ").formatted(Formatting.AQUA)
                    .append(best.itemName().copy().formatted(Formatting.WHITE)));
        } else if (TrajectoryConfig.showCalibrationMessages) {
            HudMessage.show(Text.literal("Бросок засечён...").formatted(Formatting.GRAY));
        }
        tracked.add(new TrackedEgg(egg, best));
    }

    /** Тик броска: тот, для которого ожидаемая точка вылета ближе всего к реальной. */
    private static Sample findThrowSample(Vec3d spawn) {
        Sample best = null;
        double bestDist = 2.5;
        for (Sample s : history) {
            Vec3d expected = s.eye().add(s.dir().multiply(TrajectoryConfig.spawnForward));
            double dx = expected.x - spawn.x, dz = expected.z - spawn.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < bestDist) {
                bestDist = dist;
                best = s;
            }
        }
        return best;
    }

    public static void onEntityUnload(Entity entity) {
        Iterator<TrackedEgg> it = tracked.iterator();
        while (it.hasNext()) {
            TrackedEgg t = it.next();
            if (t.egg == entity) {
                finish(t);
                it.remove();
            }
        }
    }

    public static void tick() {
        Iterator<TrackedEgg> it = tracked.iterator();
        while (it.hasNext()) {
            TrackedEgg t = it.next();
            if (t.egg.isRemoved()) {
                finish(t);
                it.remove();
                continue;
            }
            Vec3d pos = t.egg.getPos();
            if (t.ticks < 25 && !pos.equals(t.lastPos)) {
                Vec3d rel = pos.subtract(t.spawnPos);
                log("  полёт: тик {}, от спавна {}, гориз. {}, скорость {}, noGravity {}", t.ticks, fmtVec(rel),
                        fmt(Math.hypot(rel.x, rel.z)), fmtVec(t.egg.getVelocity()), t.egg.hasNoGravity());
            }
            if (!t.launched && t.egg.getVelocity().length() > LAUNCH_SPEED) t.launched = true;
            if (!pos.equals(t.lastPos)) {
                if (t.launched) {
                    t.ticks++;
                    t.path.add(pos);
                }
                t.lastPos = pos;
            }
            if (!t.measured && t.ticks >= MEASURE_TICK) {
                t.measured = true;
                if (TrajectoryConfig.autoCalibrate) calibrateFromVelocity(t);
            }
            if (t.ticks > 400) it.remove();
        }
    }

    /** По скорости яйца в полёте: инерция движения, грубая скорость и точка вылета. */
    private static void calibrateFromVelocity(TrackedEgg t) {
        double g = TrajectoryConfig.gravity;
        int n = t.ticks;
        double dn = Math.pow(DRAG, n);
        double gravitySum = g * (1 - dn) / (1 - DRAG);
        // v_n = d^n * v0 - g * (1 + d + ... + d^(n-1))  =>  v0
        Vec3d v0 = t.egg.getVelocity().add(0, gravitySum, 0).multiply(1 / dn);

        Sample s = t.sample;
        Vec3d dir = s.dir();
        Vec3d move = new Vec3d(s.move().x, s.onGround() ? 0 : s.move().y, s.move().z);

        Vec3d withInherit = v0.subtract(move);
        double speedInherit = withInherit.dotProduct(dir);
        double errInherit = withInherit.subtract(dir.multiply(speedInherit)).length();
        double speedPlain = v0.dotProduct(dir);
        double errPlain = v0.subtract(dir.multiply(speedPlain)).length();

        // Судить об инерции можно только когда игрок заметно двигался
        if (Math.hypot(move.x, move.z) > 0.08) {
            inheritScore = Math.max(-1, Math.min(1, inheritScore + (errPlain - errInherit)));
            TrajectoryConfig.inheritMovement = inheritScore >= 0;
        }

        double measured = TrajectoryConfig.inheritMovement ? speedInherit : speedPlain;
        if (measured > 0.2 && measured < 10) {
            // Первый бросок на сервере сразу задаёт скорость, дальше усредняем
            double weight = speedSamples == 0 ? 1.0 : 0.4;
            TrajectoryConfig.speed = round3(TrajectoryConfig.speed * (1 - weight) + measured * weight);
            speedSamples++;
        }

        // Точка вылета: spawn = eye + dir * forward + (0, y, 0)
        Vec3d offset = t.spawnPos.subtract(s.eye());
        double horiz = dir.x * dir.x + dir.z * dir.z;
        if (horiz > 0.1 && offset.length() < 2) {
            double forward = (offset.x * dir.x + offset.z * dir.z) / horiz;
            double y = offset.y - dir.y * forward;
            if (forward > -0.5 && forward < 1.5 && y > -1.5 && y < 0.5) {
                TrajectoryConfig.spawnForward = round3(TrajectoryConfig.spawnForward * 0.5 + forward * 0.5);
                TrajectoryConfig.spawnYOffset = round3(TrajectoryConfig.spawnYOffset * 0.5 + y * 0.5);
            }
        }
        calibrateLift(t, move);
        TrajectoryConfig.save();
        log("Калибровка по полёту: v0 {} (вдоль взгляда {}), инерция {}, вылет: вперёд {}, по высоте {}",
                fmt(v0.length()), fmt(measured), TrajectoryConfig.inheritMovement,
                TrajectoryConfig.spawnForward, TrajectoryConfig.spawnYOffset);
    }

    /**
     * Первый шаг яйца после запуска равен его начальной скорости. Сравниваем его вертикаль с полётом
     * строго по взгляду — разница и есть «подброс», который добавляет сервер.
     */
    private static void calibrateLift(TrackedEgg t, Vec3d move) {
        if (t.path.isEmpty()) return;
        Vec3d first = t.path.get(0).subtract(t.spawnPos);
        Vec3d expected = t.sample.dir().multiply(TrajectoryConfig.speed);
        if (TrajectoryConfig.inheritMovement) expected = expected.add(move);
        // Убеждаемся, что это ровно один тик полёта
        double expectedH = Math.hypot(expected.x, expected.z), firstH = Math.hypot(first.x, first.z);
        if (expectedH < 0.3 || Math.abs(firstH - expectedH) > expectedH * 0.1) return;
        double lift = first.y - expected.y;
        if (Math.abs(lift) > 0.5) return;
        double weight = liftSamples == 0 ? 1.0 : 0.3;
        TrajectoryConfig.liftY = round3(TrajectoryConfig.liftY * (1 - weight) + lift * weight);
        liftSamples++;
        log("Подброс по первому тику: {}, итог {}", fmt(lift), TrajectoryConfig.liftY);
    }

    /**
     * Сравнивает реальный путь яйца с тем, что показывала голограмма в момент броска.
     * Точку падения напрямую сравнивать нельзя: сервер ломает блок раньше, чем клиентское яйцо до него долетит.
     */
    private static void finish(TrackedEgg t) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !TrajectoryConfig.showCalibrationMessages || t.path.size() < 2) return;
        List<Vec3d> predicted = t.sample.predictedPath();
        if (predicted == null || predicted.size() < 2) {
            HudMessage.show(Text.literal(String.format(Locale.ROOT, "Калибровка: скорость %.3f, инерция: %s",
                    TrajectoryConfig.speed, TrajectoryConfig.inheritMovement ? "да" : "нет")).formatted(Formatting.AQUA));
            return;
        }

        // Максимальное отклонение реального пути от предсказанного (пока не кончилась предсказанная линия)
        double maxError = 0;
        Vec3d worst = Vec3d.ZERO;
        Vec3d start = predicted.get(0);
        double predictedReach = horizontalDistance(start, predicted.get(predicted.size() - 1));
        for (Vec3d p : t.path) {
            if (horizontalDistance(start, p) > predictedReach + 0.5) break;
            Vec3d closest = closestOnPath(predicted, p);
            double d = p.distanceTo(closest);
            if (d > maxError) {
                maxError = d;
                worst = p.subtract(closest);
            }
        }

        Formatting color = maxError < 0.4 ? Formatting.GREEN : maxError < 1.2 ? Formatting.YELLOW : Formatting.RED;
        MutableText text = Text.literal(String.format(Locale.ROOT, "Отклонение: %.2f бл.", maxError)).formatted(color);
        if (maxError >= 0.3) {
            if (Math.abs(worst.y) >= Math.hypot(worst.x, worst.z)) {
                text.append(Text.literal(worst.y > 0 ? " (летело выше — дальше)" : " (летело ниже — ближе)").formatted(color));
            } else {
                text.append(Text.literal(" (снесло вбок)").formatted(color));
            }
        }
        text.append(Text.literal(String.format(Locale.ROOT, "  | скорость %.3f, инерция: %s",
                TrajectoryConfig.speed, TrajectoryConfig.inheritMovement ? "да" : "нет")).formatted(Formatting.GRAY));
        log("{}", text.getString());
        HudMessage.show(text);
    }

    private static double horizontalDistance(Vec3d a, Vec3d b) {
        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    private static Vec3d closestOnPath(List<Vec3d> path, Vec3d p) {
        Vec3d best = path.get(0);
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3d a = path.get(i), ab = path.get(i + 1).subtract(a);
            double len2 = ab.lengthSquared();
            double k = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dotProduct(ab) / len2));
            Vec3d c = a.add(ab.multiply(k));
            double d = c.squaredDistanceTo(p);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    private static void log(String message, Object... args) {
        if (!TrajectoryConfig.debugLog) return;
        SpleggTrajectoryClient.LOGGER.info("[SpleggTrajectory] " + message, args);
    }

    private static double round3(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) return "пусто";
        return SpleggTrajectoryClient.itemId(stack) + " x" + stack.getCount() + " «" + stack.getName().getString() + "»";
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    private static String fmtVec(Vec3d v) {
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", v.x, v.y, v.z);
    }
}
