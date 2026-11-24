package com.bigbro.mobtower.mixin;

import com.bigbro.mobtower.MobTower;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Игрок только что зашёл в мир — выпускаем Визершторм с Херобрином. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
	@Inject(method = "placeNewPlayer", at = @At("TAIL"))
	private void mobtower$spawnStorm(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		MobTower.spawnStorm(player);
	}
}
