package com.splegg.trajectory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.EggItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpleggTrajectoryClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("splegg-trajectory");
    private static final String CATEGORY = "category.splegg-trajectory";
    /** Сколько тиков после броска показывать траекторию, пока клиент считает руку пустой. */
    private static final int EMPTY_HAND_GRACE_TICKS = 10;

    private static ItemStack lastHeldStack = ItemStack.EMPTY;
    private static long lastHeldTick = Long.MIN_VALUE;

    private static KeyBinding toggleKey;
    private static KeyBinding speedUpKey;
    private static KeyBinding speedDownKey;

    @Override
    public void onInitializeClient() {
        TrajectoryConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.splegg-trajectory.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));
        speedUpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.splegg-trajectory.speed_up", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY));
        speedDownKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.splegg-trajectory.speed_down", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(SpleggTrajectoryClient::onTick);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(SpleggTrajectoryClient::render);
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> ThrowTracker.onEntityLoad(entity));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> ThrowTracker.onEntityUnload(entity));
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> HudMessage.render(drawContext));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ThrowTracker.reset());
    }

    private static void onTick(MinecraftClient client) {
        if (client.player == null) return;
        while (toggleKey.wasPressed()) {
            TrajectoryConfig.enabled = !TrajectoryConfig.enabled;
            TrajectoryConfig.save();
            client.player.sendMessage(Text.literal("Траектория: ")
                    .append(TrajectoryConfig.enabled
                            ? Text.literal("ВКЛ").formatted(Formatting.GREEN)
                            : Text.literal("ВЫКЛ").formatted(Formatting.RED)), true);
        }
        boolean speedChanged = false;
        while (speedUpKey.wasPressed()) {
            TrajectoryConfig.speed = Math.min(10.0, round(TrajectoryConfig.speed + 0.05));
            speedChanged = true;
        }
        while (speedDownKey.wasPressed()) {
            TrajectoryConfig.speed = Math.max(0.1, round(TrajectoryConfig.speed - 0.05));
            speedChanged = true;
        }
        if (speedChanged) {
            TrajectoryConfig.save();
            client.player.sendMessage(Text.literal("Скорость яйца: " + TrajectoryConfig.speed)
                    .formatted(Formatting.YELLOW), true);
        }

        // Запоминаем состояние игрока каждый тик, чтобы потом сопоставить с вылетевшим яйцом
        rememberHeldItem(client.player);
        ThrowTracker.recordTick(client.player);
        ThrowTracker.tick();
    }

    /** Предмет, для которого показываем траекторию (основная рука в приоритете), или null. */
    public static ItemStack heldTrajectoryItem(ClientPlayerEntity player) {
        if (isTrajectoryItem(player.getMainHandStack())) return player.getMainHandStack();
        if (isTrajectoryItem(player.getOffHandStack())) return player.getOffHandStack();
        // Бросили последнее яйцо: клиент на пару тиков считает руку пустой, пока сервер не вернёт предмет
        if (player.getMainHandStack().isEmpty() && !lastHeldStack.isEmpty()
                && player.getWorld().getTime() - lastHeldTick <= EMPTY_HAND_GRACE_TICKS) {
            return lastHeldStack;
        }
        return null;
    }

    private static void rememberHeldItem(ClientPlayerEntity player) {
        ItemStack main = player.getMainHandStack(), off = player.getOffHandStack();
        ItemStack held = isTrajectoryItem(main) ? main : isTrajectoryItem(off) ? off : null;
        if (held != null) {
            lastHeldStack = held.copy();
            lastHeldTick = player.getWorld().getTime();
        } else if (!main.isEmpty()) {
            lastHeldStack = ItemStack.EMPTY;
        }
    }

    public static TrajectorySimulator.Params paramsFor(ItemStack stack) {
        // Параметры одни для всех предметов: их измеряет автокалибровка на текущем сервере
        return new TrajectorySimulator.Params(TrajectoryConfig.speed,
                TrajectoryConfig.inheritMovement, TrajectoryConfig.spawnYOffset, TrajectoryConfig.spawnForward,
                TrajectoryConfig.liftY);
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    public static String itemId(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    private static boolean isShovel(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ShovelItem;
    }

    public static boolean isEgg(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof EggItem;
    }

    /** Лопаты, яйца и предметы, из которых на этом сервере уже вылетали яйца. */
    public static boolean isTrajectoryItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return isShovel(stack) || isEgg(stack) || TrajectoryConfig.weaponItems.contains(itemId(stack));
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (!TrajectoryConfig.enabled || player == null || client.world == null || player.isSpectator()) return;

        ItemStack stack = heldTrajectoryItem(player);
        if (stack == null) return;
        boolean offhand = stack != player.getMainHandStack();

        float tickDelta = context.tickCounter().getTickDelta(true);
        TrajectorySimulator.Result result = TrajectorySimulator.simulate(player, tickDelta, paramsFor(stack));
        HologramRenderer.render(context.matrixStack().peek().getPositionMatrix(), context.camera(), player,
                offhand, result, tickDelta);
    }
}
