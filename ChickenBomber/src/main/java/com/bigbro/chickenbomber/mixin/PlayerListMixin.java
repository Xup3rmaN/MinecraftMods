package com.bigbro.chickenbomber.mixin;

import com.bigbro.chickenbomber.ChickenBomber;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Игрок только что зашёл в мир — выпускаем в небо курицу на самолёте. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
	@Inject(method = "placeNewPlayer", at = @At("TAIL"))
	private void chickenbomber$spawnAirplane(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		ChickenBomber.spawnAirplane(player);
	}
}
