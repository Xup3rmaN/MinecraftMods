package dev.holoplane;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/** Меню голограммы: выбор блока, размер, высота, прозрачность. */
public final class PlaneScreen extends Screen {
	private static final int ROW = 24;
	private static final int SMALL = 30;

	private int left, top;

	public PlaneScreen() {
		super(Text.literal("Holo Plane"));
	}

	@Override
	protected void init() {
		PlaneState s = HoloPlaneClient.STATE;
		left = width / 2 - 130;
		top = Math.max(6, height / 2 - 165);
		int y = top + 20;

		// Выбор блока
		for (PlaneState.HoloBlock b : PlaneState.HoloBlock.values()) {
			String label = (b == s.block ? "▶ " : "") + b.label;
			addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
				s.block = b;
				update();
			}).dimensions(left + 24, y, 236, 20).build());
			y += ROW;
		}
		y += 8;

		// Ширина, длина, обе стороны, высота, прозрачность: подписи рисуются в render()
		stepRow(y, () -> s.resize(-5, 0), () -> s.resize(-1, 0), () -> s.resize(1, 0), () -> s.resize(5, 0));
		y += ROW;
		stepRow(y, () -> s.resize(0, -5), () -> s.resize(0, -1), () -> s.resize(0, 1), () -> s.resize(0, 5));
		y += ROW;
		stepRow(y, () -> s.resize(-5, -5), () -> s.resize(-1, -1), () -> s.resize(1, 1), () -> s.resize(5, 5));
		y += ROW;
		stepRow(y, () -> s.move(0, -5, 0), () -> s.move(0, -1, 0), () -> s.move(0, 1, 0), () -> s.move(0, 5, 0));
		y += ROW;
		stepRow(y, () -> s.changeOpacity(-20), () -> s.changeOpacity(-5), () -> s.changeOpacity(5), () -> s.changeOpacity(20));
		y += ROW;
		stepRow(y, () -> s.changeHue(-30), () -> s.changeHue(-5), () -> s.changeHue(5), () -> s.changeHue(30));
		y += ROW;
		addDrawableChild(ButtonWidget.builder(Text.literal("Переливание: " + (s.hueCycle ? "вкл" : "выкл")), btn -> {
			s.hueCycle = !s.hueCycle;
			update();
		}).dimensions(left, y, 260, 20).build());
		y += ROW + 8;

		addDrawableChild(ButtonWidget.builder(Text.literal("Поставить у себя"), btn -> {
			ClientPlayerEntity p = MinecraftClient.getInstance().player;
			if (p != null) s.spawnAt(p.getX(), p.getY(), p.getZ());
			update();
		}).dimensions(left, y, 126, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal(s.visible ? "Скрыть" : "Показать"), btn -> {
			s.visible = !s.visible;
			update();
		}).dimensions(left + 134, y, 126, 20).build());
		y += ROW;
		addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить 20×20"), btn -> {
			s.sizeX = PlaneState.DEFAULT_SIZE;
			s.sizeZ = PlaneState.DEFAULT_SIZE;
			update();
		}).dimensions(left, y, 126, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), btn -> close())
				.dimensions(left + 134, y, 126, 20).build());
	}

	/** Строка из четырёх кнопок −5 −1 +1 +5 справа от подписи. */
	private void stepRow(int y, Runnable m5, Runnable m1, Runnable p1, Runnable p5) {
		int x = left + 260 - 4 * SMALL - 6;
		button(x, y, "−5", m5);
		button(x + SMALL + 2, y, "−1", m1);
		button(x + 2 * (SMALL + 2), y, "+1", p1);
		button(x + 3 * (SMALL + 2), y, "+5", p5);
	}

	private void button(int x, int y, String label, Runnable action) {
		addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
			action.run();
			update();
		}).dimensions(x, y, SMALL, 20).build());
	}

	private void update() {
		HoloPlaneClient.changed(null);
		clearAndInit();
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		PlaneState s = HoloPlaneClient.STATE;
		ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 4, 0xFF55FFFF);

		int y = top + 20;
		for (PlaneState.HoloBlock b : PlaneState.HoloBlock.values()) {
			ctx.drawItem(new ItemStack(b.item), left + 2, y + 2);
			if (b == s.block) ctx.drawStrokedRectangle(left, y, 20, 20, 0xFF55FFFF);
			y += ROW;
		}
		y += 8;

		String[] labels = {
				"Ширина X: " + s.sizeX,
				"Длина Z: " + s.sizeZ,
				"Обе стороны",
				"Высота Y: " + s.y,
				"Прозрачность: " + s.opacity + "%",
				"Сдвиг hue: " + s.hue + "°"
		};
		for (String label : labels) {
			ctx.drawTextWithShadow(textRenderer, label, left, y + 6, 0xFFFFFFFF);
			y += ROW;
		}
		// Образец оттенка рядом с подписью hue
		int swatchY = y - ROW + 4;
		ctx.fill(left + 92, swatchY, left + 106, swatchY + 12, MathHelper.hsvToArgb(s.hue / 360f, 0.8f, 1f, 255));
		ctx.drawStrokedRectangle(left + 92, swatchY, 14, 12, 0xFF000000);
		ctx.drawCenteredTextWithShadow(textRenderer,
				"PgUp/PgDn — вверх/вниз, стрелки — двигать, +/− — размер, V — переливание",
				width / 2, height - 14, 0xFFAAAAAA);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
