package dev.signals.network;

import dev.signals.Signals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Сервер → клиенты: кто-то подал сигнал. */
public record SignalBroadcastPayload(int signal) implements CustomPacketPayload {
	public static final Type<SignalBroadcastPayload> TYPE = new Type<>(Signals.id("signal_broadcast"));
	public static final StreamCodec<ByteBuf, SignalBroadcastPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SignalBroadcastPayload::signal,
			SignalBroadcastPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
