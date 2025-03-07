package cope.client.mixin;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import cope.client.util.CopeSystem;

@Mixin(value = Systems.class, remap = false)
public abstract class SystemsMixin {
    @Shadow
    private static System<?> add(System<?> system) {
        return null;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private static void onInit(CallbackInfo ci) {
        add(new CopeSystem());
    }
}
