package dev.signals.client;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Передача сигналов на серверах, где мода нет: личное сообщение тимейту со скрытой меткой.
 * Клиент с модом прячет такие сообщения из чата и проигрывает звук.
 */
public final class WhisperTransport {
	private static final Pattern MARKER = Pattern.compile("~sig([1-9])~([0-9a-f]{6})");
	private static final int REMEMBERED_NONCES = 64;

	/** Метки, которые уже обработаны: свои отправленные и полученные (сервер может прислать эхо или дубль). */
	private static final Set<String> SEEN = Collections.newSetFromMap(new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
			return size() > REMEMBERED_NONCES;
		}
	});

	private WhisperTransport() {
	}

	public static void register() {
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> handle(message));
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) -> handle(message));
	}

	/** Отправляет сигнал всем тимейтам из списка. Возвращает false, если список пуст. */
	public static boolean send(Minecraft client, SignalsConfig config, int signal) {
		if (config.team.isEmpty() || client.getConnection() == null) {
			return false;
		}

		String nonce = String.format("%06x", ThreadLocalRandom.current().nextInt(0x1000000));
		SEEN.add(nonce);
		String marker = "~sig" + (signal + 1) + "~" + nonce;

		String self = client.player != null ? client.player.getGameProfile().name() : "";
		for (String name : config.team) {
			if (!name.equalsIgnoreCase(self)) {
				client.getConnection().sendCommand(config.command + " " + name + " " + marker);
			}
		}
		return true;
	}

	/** false — сообщение скрыть. */
	private static boolean handle(Component message) {
		Matcher matcher = MARKER.matcher(message.getString());
		if (!matcher.find()) {
			return true;
		}

		int signal = Integer.parseInt(matcher.group(1)) - 1;
		// Своё эхо «вы шепчете …» и повторы просто скрываем
		if (SEEN.add(matcher.group(2))) {
			SignalsClient.playSignal(signal);
		}
		return false;
	}
}
