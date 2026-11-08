package pulsefx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.World;

import java.util.Random;

/** Draws the custom sky as a gradient above the horizon (+ twinkling stars) on top of the world. */
public final class SkyOverlay {
    private static final int STAR_COUNT = 90;
    private static final float[] AZ = new float[STAR_COUNT], EL = new float[STAR_COUNT], PH = new float[STAR_COUNT];
    static {
        Random r = new Random(1337);
        for (int i = 0; i < STAR_COUNT; i++) {
            AZ[i] = r.nextFloat() * 360f;
            EL[i] = 3f + r.nextFloat() * 75f;
            PH[i] = r.nextFloat() * 6.28f;
        }
    }

    private SkyOverlay() {}

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (FxConfig.sky == 0 || FxConfig.skyHookWorks || p == null || mc.world == null) return;
        if (mc.world.getRegistryKey() != World.OVERWORLD || p.isSubmergedInWater()) return;

        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        double fov = mc.options.getFov().getValue();
        double tanHalf = Math.tan(Math.toRadians(fov / 2.0));
        double pitch = Math.max(-89, Math.min(89, p.getPitch()));
        double hy = h / 2.0 - Math.tan(Math.toRadians(pitch)) / tanHalf * (h / 2.0);

        int top = SkyPresets.TOP[FxConfig.sky], hor = SkyPresets.HORIZON[FxConfig.sky];
        float a = FxConfig.skyBrightness * 0.8f;

        double skyH = h * 1.1;
        double yTop = hy - skyH;
        double y0 = Math.max(0, yTop), y1 = Math.min(h, hy);
        if (y1 > y0) {
            double t0 = (y0 - yTop) / skyH, t1 = (y1 - yTop) / skyH;
            ctx.fillGradient(0, (int) y0, w, (int) y1, col(top, hor, t0, a), col(top, hor, t1, a));
        }
        // soft fade just below the horizon
        double bandEnd = Math.min(h, hy + h * 0.07);
        if (hy < h && bandEnd > Math.max(0, hy)) {
            int yb0 = (int) Math.max(0, hy);
            ctx.fillGradient(0, yb0, w, (int) bandEnd, col(top, hor, 1.0, a), (hor & 0xFFFFFF));
        }

        if (SkyPresets.STARS[FxConfig.sky]) {
            double yaw = p.getYaw();
            double time = System.nanoTime() / 1.0e9;
            for (int i = 0; i < STAR_COUNT; i++) {
                double dx = ((AZ[i] - yaw) % 360 + 540) % 360 - 180;
                if (Math.abs(dx) > 80) continue;
                double dy = EL[i] + pitch;
                if (Math.abs(dy) > 70) continue;
                int sx = (int) (w / 2.0 + Math.tan(Math.toRadians(dx)) / tanHalf * (h / 2.0));
                int sy = (int) (h / 2.0 - Math.tan(Math.toRadians(dy)) / tanHalf * (h / 2.0));
                if (sy < 0 || sy > hy - 2 || sx < 0 || sx > w) continue;
                float tw = 0.55f + 0.45f * (float) Math.sin(time * 1.6 + PH[i]);
                int al = (int) (255 * tw * FxConfig.skyBrightness);
                int size = (i % 7 == 0) ? 2 : 1;
                ctx.fill(sx, sy, sx + size, sy + size, (al << 24) | 0xFFFFFF);
            }
        }
    }

    private static int col(int top, int hor, double t, float a) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((top >> 16) & 255) * (1 - t) + ((hor >> 16) & 255) * t);
        int g = (int) (((top >> 8) & 255) * (1 - t) + ((hor >> 8) & 255) * t);
        int b = (int) ((top & 255) * (1 - t) + (hor & 255) * t);
        int al = (int) (255 * a * (0.9 - 0.3 * t));
        return (al << 24) | (r << 16) | (g << 8) | b;
    }
}
