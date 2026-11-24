package dev.signals.network;

import dev.signals.Signals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: игрок нажал клавишу сигнала. */
public record SendSignalPayload(int signal) implements CustomPacketPayload {
	public static final Type<SendSignalPayload> TYPE = new Type<>(Signals.id("send_signal"));
	public static final StreamCodec<ByteBuf, SendSignalPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SendSignalPayload::signal,
			SendSignalPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
