package pulsefx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Dark graphite settings menu (opens with P). */
public class FxScreen extends Screen {
    // palette: graphite / silver, no neon
    private static final int TEXT = 0xFFD2D4DA, TEXT_DIM = 0xFF8C8F98, ACCENT = 0xFFDADCE2;
    private static final String[] TABS = {"Effects", "Sky", "General", "About"};
    private static int tab = 0;

    private int px, py, pw, ph;
    private final List<ClickableWidget> widgets = new ArrayList<>();
    private final List<Object[]> labels = new ArrayList<>(); // {x, y, text}

    public FxScreen() { super(Text.literal("PulseFX")); }

    private static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    // ================= layout =================
    @Override
    protected void init() {
        widgets.clear();
        labels.clear();
        pw = Math.min(440, width - 16);
        ph = Math.min(262, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            add(new FlatButton(px + 10, py + 54 + i * 28, 104, 24, TABS[i], false, () -> idx == tab, b -> { tab = idx; clearAndInit(); }));
        }
        add(new FlatButton(px + pw / 2 - 102, py + ph - 34, 96, 22, "Defaults", false, () -> false, b -> { FxConfig.reset(); clearAndInit(); }));
        add(new FlatButton(px + pw / 2 + 6, py + ph - 34, 96, 22, "Save", true, () -> false, b -> { FxConfig.save(); close(); }));

        int cx = px + 128, cw = pw - 128 - 14, y0 = py + 70;
        switch (tab) {
            case 0 -> {
                int colW = (cw - 10) / 2, x2 = cx + colW + 10;
                add(new Toggle(cx, y0, colW, "Stars Around Player", () -> FxConfig.stars, () -> FxConfig.stars = !FxConfig.stars));
                add(new Toggle(cx, y0 + 24, colW, "Sakura Petals", () -> FxConfig.petals, () -> FxConfig.petals = !FxConfig.petals));
                add(new Toggle(cx, y0 + 48, colW, "Color Trail", () -> FxConfig.trail, () -> FxConfig.trail = !FxConfig.trail));
                add(new Toggle(cx, y0 + 72, colW, "Jump Rings", () -> FxConfig.jumpRings, () -> FxConfig.jumpRings = !FxConfig.jumpRings));
                add(new Toggle(x2, y0, colW, "Hit Sparks", () -> FxConfig.hitSparks, () -> FxConfig.hitSparks = !FxConfig.hitSparks));
                add(new Toggle(x2, y0 + 24, colW, "Magic Particles", () -> FxConfig.magic, () -> FxConfig.magic = !FxConfig.magic));
                add(new Toggle(x2, y0 + 48, colW, "Crit Boost", () -> FxConfig.crit, () -> FxConfig.crit = !FxConfig.crit));
                add(new Toggle(x2, y0 + 72, colW, "Run Flames", () -> FxConfig.flames, () -> FxConfig.flames = !FxConfig.flames));
                add(new Cycle(cx, y0 + 108, cw, "Trail Style", () -> FxConfig.TRAIL_STYLES[FxConfig.trailStyle],
                    () -> FxConfig.trailStyle = (FxConfig.trailStyle + 1) % FxConfig.TRAIL_STYLES.length));
            }
            case 1 -> {
                add(new Cycle(cx, y0, cw, "Sky Preset", () -> SkyPresets.NAMES[FxConfig.sky],
                    () -> FxConfig.sky = (FxConfig.sky + 1) % SkyPresets.NAMES.length));
                slider(cx, y0 + 28, cw, "Sky Brightness", 0.2f, 1.0f, () -> FxConfig.skyBrightness, v -> FxConfig.skyBrightness = v);
            }
            case 2 -> {
                slider(cx, y0, cw, "Particle Size", 0.5f, 2.0f, () -> FxConfig.particleSize, v -> FxConfig.particleSize = v);
                slider(cx, y0 + 28, cw, "Effect Rate", 0.25f, 2.0f, () -> FxConfig.rate, v -> FxConfig.rate = v);
                slider(cx, y0 + 56, cw, "Effect Range", 0.5f, 2.0f, () -> FxConfig.range, v -> FxConfig.range = v);
                add(new Cycle(cx, y0 + 90, cw, "Performance", () -> FxConfig.PERFORMANCE[FxConfig.performance],
                    () -> FxConfig.performance = (FxConfig.performance + 1) % FxConfig.PERFORMANCE.length));
            }
            default -> {}
        }
    }

    private void add(ClickableWidget w) { widgets.add(w); addDrawableChild(w); }

    private void slider(int cx, int y, int cw, String label, float min, float max, Supplier<Float> get, Consumer<Float> set) {
        labels.add(new Object[]{cx + 8, y + 7, label});
        add(new Slide(cx + cw - 150, y, 104, 22, min, max, get, set));
    }

    // ================= drawing =================
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fillGradient(0, 0, width, height, 0xC0121214, 0xE0050506);

        c.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF34343A);
        c.fillGradient(px, py, px + pw, py + ph, 0xFF2B2B30, 0xFF0E0E10);

        TextRenderer f = font();
        c.drawText(f, Text.literal("PulseFX").formatted(Formatting.BOLD), px + 14, py + 10, 0xFFF2F3F5, true);
        c.drawText(f, Text.literal("Visual effects settings"), px + 14, py + 24, TEXT_DIM, false);
        String ver = "v1.0.0";
        c.drawText(f, Text.literal(ver), px + pw - 14 - f.getWidth(ver), py + 12, TEXT_DIM, false);
        c.fill(px + 10, py + 42, px + pw - 10, py + 43, 0xFF3A3A41);
        c.fill(px + 120, py + 52, px + 121, py + ph - 44, 0xFF2F2F35);
        c.fill(px + 10, py + ph - 42, px + pw - 10, py + ph - 41, 0xFF3A3A41);

        int cx = px + 128;
        c.drawText(f, Text.literal(TABS[tab].toUpperCase()), cx, py + 52, TEXT_DIM, false);

        for (ClickableWidget w : widgets) w.render(c, mx, my, delta);
        for (Object[] l : labels) c.drawText(f, Text.literal((String) l[2]), (Integer) l[0], (Integer) l[1], TEXT, false);

        if (tab == 1) { // sky preview swatch
            int cw = pw - 128 - 14, sy = py + 70 + 66;
            int top = 0xFF000000 | SkyPresets.TOP[FxConfig.sky], hor = 0xFF000000 | SkyPresets.HORIZON[FxConfig.sky];
            c.fill(cx - 1, sy - 1, cx + cw + 1, sy + 61, 0xFF3A3A41);
            c.fillGradient(cx, sy, cx + cw, sy + 60, top, hor);
            if (SkyPresets.STARS[FxConfig.sky]) {
                for (int i = 0; i < 30; i++) {
                    int sx = cx + 6 + (i * 53) % (cw - 12), sty = sy + 4 + (i * 29) % 36;
                    c.fill(sx, sty, sx + 1, sty + 1, 0xCCFFFFFF);
                }
            }
            c.drawText(f, Text.literal("Preview"), cx + 6, sy + 48, 0xFFFFFFFF, true);
        }
        if (tab == 3) {
            String[] lines = {
                "PulseFX - client-side visual effects.",
                "Version 1.0.0  |  Minecraft 1.21.11  |  Fabric",
                "",
                "Press P in game to open this menu.",
                "Settings are saved to config/pulsefx.properties.",
                "Invisible players are never revealed by this mod."
            };
            for (int i = 0; i < lines.length; i++) c.drawText(f, Text.literal(lines[i]), cx, py + 72 + i * 14, i == 0 ? TEXT : TEXT_DIM, false);
        }
    }

    @Override
    public void removed() { FxConfig.save(); }

    @Override
    public boolean shouldPause() { return false; }

    // ================= custom widgets =================
    private static void text(DrawContext c, String s, int x, int y, int col) {
        c.drawText(font(), Text.literal(s), x, y, col, false);
    }

    static class FlatButton extends ButtonWidget {
        private final boolean primary;
        private final BooleanSupplier selected;
        FlatButton(int x, int y, int w, int h, String label, boolean primary, BooleanSupplier selected, ButtonWidget.PressAction a) {
            super(x, y, w, h, Text.literal(label), a, DEFAULT_NARRATION_SUPPLIER);
            this.primary = primary; this.selected = selected;
        }
        @Override
        protected void renderWidget(DrawContext c, int mx, int my, float d) {
            boolean hov = isHovered(), sel = selected.getAsBoolean();
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            if (primary) {
                c.fillGradient(x, y, x + w, y + h, hov ? 0xFFEDEEF1 : 0xFFD6D8DD, hov ? 0xFFB9BCC4 : 0xFF9EA2AB);
            } else if (sel) {
                c.fill(x, y, x + w, y + h, 0xFF000000 | 0x44444B);
                c.fill(x, y, x + 2, y + h, ACCENT);
            } else {
                c.fill(x, y, x + w, y + h, hov ? 0xFF2E2E34 : 0xFF202024);
            }
            int col = primary ? 0xFF15151A : (sel ? 0xFFFFFFFF : (hov ? TEXT : TEXT_DIM));
            String s = getMessage().getString();
            int tx = (primary || w < 100) ? x + (w - font().getWidth(s)) / 2 : x + 14;
            text(c, s, tx, y + (h - 8) / 2, col);
        }
    }

    static class Toggle extends ButtonWidget {
        private final String label; private final BooleanSupplier get;
        Toggle(int x, int y, int w, String label, BooleanSupplier get, Runnable flip) {
            super(x, y, w, 22, Text.literal(label), b -> flip.run(), DEFAULT_NARRATION_SUPPLIER);
            this.label = label; this.get = get;
        }
        @Override
        protected void renderWidget(DrawContext c, int mx, int my, float d) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean on = get.getAsBoolean();
            if (isHovered()) c.fill(x, y, x + w, y + h, 0xFF26262B);
            text(c, label, x + 6, y + (h - 8) / 2, on ? TEXT : TEXT_DIM);
            int sx = x + w - 32, sy = y + (h - 12) / 2;
            c.fill(sx, sy, sx + 26, sy + 12, on ? 0xFFC9CCD3 : 0xFF3A3A41);
            int kx = on ? sx + 15 : sx + 1;
            c.fill(kx, sy + 1, kx + 10, sy + 11, on ? 0xFF16161A : 0xFF8C8F98);
        }
    }

    static class Cycle extends ButtonWidget {
        private final String label; private final Supplier<String> value;
        Cycle(int x, int y, int w, String label, Supplier<String> value, Runnable next) {
            super(x, y, w, 22, Text.literal(label), b -> next.run(), DEFAULT_NARRATION_SUPPLIER);
            this.label = label; this.value = value;
        }
        @Override
        protected void renderWidget(DrawContext c, int mx, int my, float d) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            if (isHovered()) c.fill(x, y, x + w, y + h, 0xFF26262B);
            text(c, label, x + 6, y + (h - 8) / 2, TEXT);
            int bw = 124, bx = x + w - bw - 4;
            c.fill(bx - 1, y + 1, bx + bw + 1, y + h - 1, 0xFF3A3A41);
            c.fillGradient(bx, y + 2, bx + bw, y + h - 2, 0xFF2C2C32, 0xFF18181B);
            String s = value.get();
            text(c, s, bx + (bw - font().getWidth(s)) / 2, y + (h - 8) / 2, 0xFFF2F3F5);
            text(c, ">", bx + bw - 10, y + (h - 8) / 2, TEXT_DIM);
        }
    }

    static class Slide extends SliderWidget {
        private final float min, max; private final Consumer<Float> set;
        private boolean ready = false;
        Slide(int x, int y, int w, int h, float min, float max, Supplier<Float> get, Consumer<Float> set) {
            super(x, y, w, h, Text.empty(), (get.get() - min) / (max - min));
            this.min = min; this.max = max; this.set = set;
            this.ready = true;
            updateMessage();
        }
        @Override protected void updateMessage() {
            if (!ready) return;
            setMessage(Text.literal(Math.round((min + value * (max - min)) * 100) + "%"));
        }
        @Override protected void applyValue() { set.accept((float) (min + value * (max - min))); }
        @Override
        protected void renderWidget(DrawContext c, int mx, int my, float d) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            int ty = y + h / 2 - 2;
            c.fill(x, ty, x + w, ty + 4, 0xFF34343A);
            int fx = x + (int) (value * w);
            c.fill(x, ty, fx, ty + 4, 0xFFC9CCD3);
            int kx = Math.max(x, Math.min(x + w - 6, fx - 3));
            c.fill(kx, y + 3, kx + 6, y + h - 3, isHovered() ? 0xFFFFFFFF : 0xFFE4E6EA);
            text(c, getMessage().getString(), x + w + 8, y + (h - 8) / 2, TEXT_DIM);
        }
    }
}
