package pulsefx.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pulsefx.FxConfig;
import pulsefx.SkyPresets;

/**
 * Optional hook: if this method exists in the game version and gets called, the real sky is recoloured
 * and the overlay sky is switched off automatically (FxConfig.skyHookWorks).
 */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void pulsefx$sky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        FxConfig.skyHookWorks = true;
        if (FxConfig.sky != 0) cir.setReturnValue(SkyPresets.TOP[FxConfig.sky]);
    }
}
