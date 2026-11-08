package pulsefx;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** All settings. Saved to config/pulsefx.properties. */
public final class FxConfig {
    public static final String[] TRAIL_STYLES = {"Rainbow Ribbon", "Aurora Helix", "Cosmic Dust", "Ice Crystal"};
    public static final String[] PERFORMANCE = {"Low", "Medium", "High"};
    private static final float[] PERF_MUL = {0.4f, 0.7f, 1.0f};

    public static boolean stars = true, petals = false, trail = true, jumpRings = true,
        hitSparks = true, magic = false, crit = false, flames = false;
    public static int trailStyle = 0, sky = 0, performance = 2;
    public static float skyBrightness = 0.7f, particleSize = 1f, rate = 1f, range = 1f;

    /** Set to true if the vanilla sky-colour hook fired (then the overlay sky is not drawn). */
    public static boolean skyHookWorks = false;

    private FxConfig() {}

    public static float perfMul() { return PERF_MUL[performance]; }

    public static void reset() {
        stars = true; petals = false; trail = true; jumpRings = true;
        hitSparks = true; magic = false; crit = false; flames = false;
        trailStyle = 0; sky = 0; performance = 2;
        skyBrightness = 0.7f; particleSize = 1f; rate = 1f; range = 1f;
    }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("pulsefx.properties"); }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("stars", "" + stars); p.setProperty("petals", "" + petals);
        p.setProperty("trail", "" + trail); p.setProperty("jumpRings", "" + jumpRings);
        p.setProperty("hitSparks", "" + hitSparks); p.setProperty("magic", "" + magic);
        p.setProperty("crit", "" + crit); p.setProperty("flames", "" + flames);
        p.setProperty("trailStyle", "" + trailStyle); p.setProperty("sky", "" + sky);
        p.setProperty("performance", "" + performance);
        p.setProperty("skyBrightness", "" + skyBrightness); p.setProperty("particleSize", "" + particleSize);
        p.setProperty("rate", "" + rate); p.setProperty("range", "" + range);
        try (OutputStream o = Files.newOutputStream(file())) { p.store(o, "PulseFX"); } catch (IOException ignored) {}
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (InputStream i = Files.newInputStream(f)) { p.load(i); } catch (IOException e) { return; }
        stars = b(p, "stars", stars); petals = b(p, "petals", petals);
        trail = b(p, "trail", trail); jumpRings = b(p, "jumpRings", jumpRings);
        hitSparks = b(p, "hitSparks", hitSparks); magic = b(p, "magic", magic);
        crit = b(p, "crit", crit); flames = b(p, "flames", flames);
        trailStyle = clamp(i(p, "trailStyle", trailStyle), 0, TRAIL_STYLES.length - 1);
        sky = clamp(i(p, "sky", sky), 0, SkyPresets.NAMES.length - 1);
        performance = clamp(i(p, "performance", performance), 0, 2);
        skyBrightness = f(p, "skyBrightness", skyBrightness); particleSize = f(p, "particleSize", particleSize);
        rate = f(p, "rate", rate); range = f(p, "range", range);
    }

    private static boolean b(Properties p, String k, boolean d) { String v = p.getProperty(k); return v == null ? d : Boolean.parseBoolean(v); }
    private static int i(Properties p, String k, int d) { try { return Integer.parseInt(p.getProperty(k)); } catch (Exception e) { return d; } }
    private static float f(Properties p, String k, float d) { try { return Float.parseFloat(p.getProperty(k)); } catch (Exception e) { return d; } }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
