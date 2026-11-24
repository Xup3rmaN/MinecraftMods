package dev.signals.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Настройки клиента: кому шептать сигналы на серверах без мода. Хранятся в config/signals.json. */
public class SignalsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("signals.json");

	/** Команда личных сообщений сервера (msg, tell, w, m …). */
	public String command = "msg";
	/** Ники тимейтов, которым уходят сигналы. */
	public List<String> team = new ArrayList<>();
	/** Режим «только сигналы»: все остальные звуки игры выключены. */
	public boolean muteOthers = false;

	public static SignalsConfig load() {
		if (Files.exists(PATH)) {
			try {
				SignalsConfig config = GSON.fromJson(Files.readString(PATH), SignalsConfig.class);
				if (config != null) {
					if (config.team == null) {
						config.team = new ArrayList<>();
					}
					if (config.command == null || config.command.isBlank()) {
						config.command = "msg";
					}
					return config;
				}
			} catch (Exception e) {
				SignalsClient.LOGGER.warn("Не удалось прочитать {}", PATH, e);
			}
		}
		return new SignalsConfig();
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			SignalsClient.LOGGER.warn("Не удалось сохранить {}", PATH, e);
		}
	}
}
