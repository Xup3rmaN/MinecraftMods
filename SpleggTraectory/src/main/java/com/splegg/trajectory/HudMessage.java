package com.splegg.trajectory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/** Своя строка над хотбаром (выше actionbar), чтобы сервер не перебивал её своими сообщениями. */
public final class HudMessage {
    private static final long DURATION_MS = 4000;
    private static Text text;
    private static long shownAt;

    private HudMessage() {}

    public static void show(Text message) {
        text = message;
        shownAt = Util.getMeasuringTimeMs();
    }

    public static void render(DrawContext context) {
        if (text == null) return;
        long elapsed = Util.getMeasuringTimeMs() - shownAt;
        if (elapsed > DURATION_MS) {
            text = null;
            return;
        }
        // Плавное исчезание в последнюю секунду
        int alpha = (int) (255 * Math.min(1.0, (DURATION_MS - elapsed) / 1000.0));
        if (alpha < 8) return;
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;
        int x = (context.getScaledWindowWidth() - font.getWidth(text)) / 2;
        int y = context.getScaledWindowHeight() - 59 - 30;
        context.drawTextWithShadow(font, text, x, y, (alpha << 24) | 0xFFFFFF);
    }
}
