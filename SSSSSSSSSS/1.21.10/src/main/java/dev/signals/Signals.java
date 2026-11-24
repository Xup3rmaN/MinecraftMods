package dev.signals;

import dev.signals.network.SendSignalPayload;
import dev.signals.network.SignalBroadcastPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Signals implements ModInitializer {
	public static final String MOD_ID = "signals";
	public static final int SIGNAL_COUNT = 3;
	/** Минимальная пауза между сигналами одного игрока, в тиках (20 тиков = 1 секунда). */
	public static final int COOLDOWN_TICKS = 10;

	private static final Map<UUID, Integer> LAST_SIGNAL_TICK = new HashMap<>();

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(SendSignalPayload.TYPE, SendSignalPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SignalBroadcastPayload.TYPE, SignalBroadcastPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SendSignalPayload.TYPE, (payload, context) ->
				handleSignal(context.server(), context.player(), payload.signal()));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				LAST_SIGNAL_TICK.remove(handler.getPlayer().getUUID()));
	}

	private static void handleSignal(MinecraftServer server, ServerPlayer sender, int signal) {
		if (signal < 0 || signal >= SIGNAL_COUNT) {
			return;
		}

		int now = server.getTickCount();
		Integer last = LAST_SIGNAL_TICK.get(sender.getUUID());
		if (last != null && now - last < COOLDOWN_TICKS) {
			return;
		}
		LAST_SIGNAL_TICK.put(sender.getUUID(), now);

		SignalBroadcastPayload broadcast = new SignalBroadcastPayload(signal);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			// Игроки без мода просто ничего не получат
			if (ServerPlayNetworking.canSend(player, SignalBroadcastPayload.TYPE)) {
				ServerPlayNetworking.send(player, broadcast);
			}
		}
	}
}
