package pulsefx;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/** Nothing here changes entity visibility: invisible players/mobs stay invisible. */
public class PulseFxClient implements ClientModInitializer {
    private static KeyBinding menuKey;
    private final Random rnd = new Random();
    private Vec3d lastPos = Vec3d.ZERO;
    private boolean wasOnGround = true, wasAttack = false;
    private int tick;

    @Override
    public void onInitializeClient() {
        FxConfig.load();
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("pulsefx", "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.pulsefx.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, cat));
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudElementRegistry.addFirst(Identifier.of("pulsefx", "sky"), (ctx, tc) -> SkyOverlay.render(ctx));
    }

    // ---------- helpers ----------
    private int cnt(double base) {
        double v = base * FxConfig.rate * FxConfig.perfMul();
        int n = (int) v;
        if (rnd.nextDouble() < v - n) n++;
        return n;
    }
    private double j(double x) { return (rnd.nextDouble() - 0.5) * 2 * x; }
    private static DustParticleEffect dust(int rgb, float s) { return new DustParticleEffect(rgb, s); }
    private static DustColorTransitionParticleEffect trans(int a, int b, float s) { return new DustColorTransitionParticleEffect(a, b, s); }

    // ---------- main tick ----------
    private void onTick(MinecraftClient mc) {
        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new FxScreen());
        }
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null || mc.isPaused()) return;
        tick++;
        boolean visible = !p.isInvisible();
        Vec3d pos = p.getEntityPos();
        float size = FxConfig.particleSize;
        double range = FxConfig.range;

        if (FxConfig.stars) {
            for (int i = cnt(1.5); i > 0; i--) {
                double a = rnd.nextDouble() * Math.PI * 2, r = (3 + rnd.nextDouble() * 10) * range;
                w.addParticleClient(ParticleTypes.END_ROD,
                    pos.x + Math.cos(a) * r, pos.y + 0.5 + rnd.nextDouble() * 6, pos.z + Math.sin(a) * r,
                    0, 0.01 + rnd.nextDouble() * 0.01, 0);
            }
        }

        if (FxConfig.petals) {
            for (int i = cnt(0.7); i > 0; i--) {
                w.addParticleClient(ParticleTypes.CHERRY_LEAVES,
                    pos.x + j(8 * range), pos.y + 4 + rnd.nextDouble() * 3, pos.z + j(8 * range), 0, -0.02, 0);
            }
        }

        if (FxConfig.magic) {
            for (int i = cnt(2); i > 0; i--) {
                w.addParticleClient(ParticleTypes.ENCHANT, pos.x, pos.y + 1.3, pos.z,
                    j(1.6), rnd.nextDouble() * 1.4 - 0.6, j(1.6));
            }
        }

        if (FxConfig.flames && visible && p.isSprinting() && p.isOnGround()) {
            for (int i = cnt(2); i > 0; i--) {
                w.addParticleClient(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                    pos.x + j(0.2), pos.y + 0.05, pos.z + j(0.2), j(0.02), 0.03, j(0.02));
            }
        }

        // ---- trail: a dense ribbon interpolated between last and current position ----
        Vec3d d = pos.subtract(lastPos);
        double dist = d.length();
        if (FxConfig.trail && visible && dist > 0.04 && dist < 6) {
            int steps = (int) Math.min(10, Math.max(1, dist / 0.16));
            for (int s = 0; s < steps; s++) {
                Vec3d q = lastPos.add(d.multiply((s + 1.0) / steps));
                spawnTrail(w, q, tick * 1.0 + s * 0.35, 0.9f * size);
            }
        }
        lastPos = pos;

        // ---- jump rings ----
        if (FxConfig.jumpRings && visible && wasOnGround && !p.isOnGround() && p.getVelocity().y > 0.1) {
            for (int i = 0; i < 28; i++) {
                double a = i / 28.0 * Math.PI * 2;
                int c = MathHelper.hsvToRgb(i / 28f, 0.45f, 1f);
                w.addParticleClient(dust(c, 1.0f * size), pos.x + Math.cos(a) * 0.55, pos.y + 0.1, pos.z + Math.sin(a) * 0.55, 0, 0, 0);
                if (i % 3 == 0) w.addParticleClient(ParticleTypes.END_ROD, pos.x, pos.y + 0.1, pos.z,
                    Math.cos(a) * 0.16, 0.02, Math.sin(a) * 0.16);
            }
        }
        wasOnGround = p.isOnGround();

        // ---- hit effects: detected from the attack key + crosshair target ----
        boolean atk = mc.options.attackKey.isPressed();
        if (atk && !wasAttack && mc.currentScreen == null && (FxConfig.hitSparks || FxConfig.crit)
            && mc.crosshairTarget instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            if (e instanceof LivingEntity && !e.isInvisible()) burst(w, ehr.getPos(), size);
        }
        wasAttack = atk;
    }

    private void spawnTrail(ClientWorld w, Vec3d q, double t, float sz) {
        switch (FxConfig.trailStyle) {
            case 0 -> { // Rainbow Ribbon
                for (int k = 0; k < 2; k++) {
                    int c = MathHelper.hsvToRgb((float) (((t * 6 + k * 40) % 360) / 360.0), 0.6f, 1f);
                    w.addParticleClient(dust(c, sz), q.x + j(0.1), q.y + 0.25 + k * 0.8, q.z + j(0.1), 0, 0, 0);
                }
            }
            case 1 -> { // Aurora Helix
                for (int k = 0; k < 2; k++) {
                    double a = t * 0.6 + k * Math.PI;
                    w.addParticleClient(trans(0x4DFFC3, 0x8A5CFF, sz),
                        q.x + Math.cos(a) * 0.45, q.y + 0.9 + Math.sin(t * 0.3) * 0.5, q.z + Math.sin(a) * 0.45, 0, 0, 0);
                }
                if (rnd.nextDouble() < 0.15) w.addParticleClient(ParticleTypes.END_ROD, q.x + j(0.3), q.y + 0.9, q.z + j(0.3), 0, 0.01, 0);
            }
            case 2 -> { // Cosmic Dust
                w.addParticleClient(trans(0xFF4FD8, 0x3A2BFF, sz), q.x + j(0.22), q.y + 0.2 + rnd.nextDouble() * 1.4, q.z + j(0.22), 0, 0, 0);
                if (rnd.nextDouble() < 0.2) w.addParticleClient(ParticleTypes.END_ROD, q.x + j(0.3), q.y + 0.2 + rnd.nextDouble() * 1.6, q.z + j(0.3), 0, 0.005, 0);
            }
            default -> { // Ice Crystal
                w.addParticleClient(trans(0xFFFFFF, 0x7FD6FF, sz), q.x + j(0.2), q.y + 0.15 + rnd.nextDouble() * 1.2, q.z + j(0.2), 0, 0, 0);
                if (rnd.nextDouble() < 0.3) w.addParticleClient(ParticleTypes.SNOWFLAKE, q.x + j(0.25), q.y + 0.4 + rnd.nextDouble(), q.z + j(0.25), 0, -0.02, 0);
            }
        }
    }

    private void burst(ClientWorld w, Vec3d at, float size) {
        if (FxConfig.hitSparks) {
            for (int i = cnt(22); i > 0; i--) {
                double a = rnd.nextDouble() * Math.PI * 2, sp = 0.15 + rnd.nextDouble() * 0.3, up = rnd.nextDouble() * 0.35;
                double vx = Math.cos(a) * sp, vz = Math.sin(a) * sp;
                switch (i % 3) {
                    case 0 -> w.addParticleClient(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, vx, up, vz);
                    case 1 -> w.addParticleClient(ParticleTypes.FIREWORK, at.x, at.y, at.z, vx * 0.6, up, vz * 0.6);
                    default -> w.addParticleClient(trans(0xFFE08A, 0xFF5A3C, 1.2f * size), at.x + j(0.15), at.y + j(0.15), at.z + j(0.15), 0, 0, 0);
                }
            }
        }
        if (FxConfig.crit) {
            for (int i = cnt(18); i > 0; i--) {
                double a = rnd.nextDouble() * Math.PI * 2, sp = 0.2 + rnd.nextDouble() * 0.3;
                w.addParticleClient(i % 3 == 0 ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT,
                    at.x, at.y, at.z, Math.cos(a) * sp, rnd.nextDouble() * 0.4, Math.sin(a) * sp);
            }
        }
    }
}
