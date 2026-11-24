package dev.signals.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.signals.Signals;
import dev.signals.network.SendSignalPayload;
import dev.signals.network.SignalBroadcastPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SignalsClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Signals.MOD_ID);
	/** Минимальная пауза между своими сигналами, в тиках — чтобы сервер не кикнул за спам в ЛС. */
	private static final int CLIENT_COOLDOWN_TICKS = 20;

	private static final SoundEvent[] SOUNDS = new SoundEvent[Signals.SIGNAL_COUNT];
	private static final KeyMapping[] KEYS = new KeyMapping[Signals.SIGNAL_COUNT];
	private static KeyMapping muteKey;

	private static SignalsConfig config;
	private static int cooldown;

	@Override
	public void onInitializeClient() {
		config = SignalsConfig.load();
		KeyMapping.Category category = KeyMapping.Category.register(Signals.id("signals"));

		for (int i = 0; i < Signals.SIGNAL_COUNT; i++) {
			// Клавиши по умолчанию не назначены — каждый игрок выбирает их сам в «Управлении»
			KEYS[i] = KeyBindingHelper.registerKeyBinding(new KeyMapping(
					"key.signals.signal_" + (i + 1),
					InputConstants.Type.KEYSYM,
					InputConstants.UNKNOWN.getValue(),
					category
			));
			SOUNDS[i] = SoundEvent.createVariableRangeEvent(Signals.id("signal_" + (i + 1)));
		}
		muteKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.signals.mute_others",
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(),
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(SignalsClient::onTick);
		ClientPlayNetworking.registerGlobalReceiver(SignalBroadcastPayload.TYPE, (payload, context) ->
				playSignal(payload.signal()));
		WhisperTransport.register();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				literal("signals")
						.then(literal("add").then(argument("player", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(onlinePlayerNames(), builder))
								.executes(context -> addPlayer(context.getSource(), StringArgumentType.getString(context, "player")))))
						.then(literal("remove").then(argument("player", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(config.team, builder))
								.executes(context -> removePlayer(context.getSource(), StringArgumentType.getString(context, "player")))))
						.then(literal("list").executes(context -> listPlayers(context.getSource())))
						.then(literal("mute").executes(context -> {
							toggleMuteOthers(context.getSource().getClient());
							return 1;
						}))
						.then(literal("command").then(argument("name", StringArgumentType.word())
								.executes(context -> setCommand(context.getSource(), StringArgumentType.getString(context, "name")))))
		));
	}

	private static void onTick(Minecraft client) {
		if (cooldown > 0) {
			cooldown--;
		}
		while (muteKey.consumeClick()) {
			toggleMuteOthers(client);
		}
		for (int i = 0; i < Signals.SIGNAL_COUNT; i++) {
			while (KEYS[i].consumeClick()) {
				if (client.player != null && cooldown == 0) {
					sendSignal(client, i);
				}
			}
		}
	}

	private static void sendSignal(Minecraft client, int signal) {
		cooldown = CLIENT_COOLDOWN_TICKS;
		// Мод есть на сервере — сервер сам разошлёт сигнал всем, включая нас
		if (ClientPlayNetworking.canSend(SendSignalPayload.TYPE)) {
			ClientPlayNetworking.send(new SendSignalPayload(signal));
			return;
		}
		// Иначе — личными сообщениями тимейтам, а себе проигрываем сразу
		if (WhisperTransport.send(client, config, signal)) {
			playSignal(signal);
		} else {
			client.player.displayClientMessage(Component.translatable("signals.no_team"), false);
		}
	}

	public static void playSignal(int signal) {
		if (signal < 0 || signal >= Signals.SIGNAL_COUNT) {
			return;
		}
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SOUNDS[signal], 1.0F, 1.0F));
	}

	public static boolean isMuteOthers() {
		return config != null && config.muteOthers;
	}

	private static void toggleMuteOthers(Minecraft client) {
		config.muteOthers = !config.muteOthers;
		config.save();
		if (config.muteOthers) {
			// Обрываем уже играющее: музыку, дождь, эмбиент
			client.getSoundManager().stop();
		}
		Component message = Component.translatable(config.muteOthers ? "signals.mute_on" : "signals.mute_off");
		if (client.player != null) {
			client.player.displayClientMessage(message, true);
		}
	}

	private static Iterable<String> onlinePlayerNames() {
		var connection = Minecraft.getInstance().getConnection();
		if (connection == null) {
			return java.util.List.of();
		}
		return connection.getOnlinePlayers().stream().map(PlayerInfo::getProfile).map(profile -> profile.name()).toList();
	}

	private static int addPlayer(FabricClientCommandSource source, String name) {
		if (config.team.stream().noneMatch(name::equalsIgnoreCase)) {
			config.team.add(name);
			config.save();
		}
		source.sendFeedback(Component.translatable("signals.added", name));
		return 1;
	}

	private static int removePlayer(FabricClientCommandSource source, String name) {
		if (config.team.removeIf(name::equalsIgnoreCase)) {
			config.save();
			source.sendFeedback(Component.translatable("signals.removed", name));
		} else {
			source.sendError(Component.translatable("signals.not_in_team", name));
		}
		return 1;
	}

	private static int listPlayers(FabricClientCommandSource source) {
		source.sendFeedback(config.team.isEmpty()
				? Component.translatable("signals.team_empty")
				: Component.translatable("signals.team", String.join(", ", config.team)));
		return 1;
	}

	private static int setCommand(FabricClientCommandSource source, String name) {
		config.command = name.startsWith("/") ? name.substring(1) : name;
		config.save();
		source.sendFeedback(Component.translatable("signals.command_set", config.command));
		return 1;
	}
}
