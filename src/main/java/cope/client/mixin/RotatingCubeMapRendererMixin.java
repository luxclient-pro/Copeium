package cope.client.mixin;

import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(RotatingCubeMapRenderer.class)
public abstract class RotatingCubeMapRendererMixin {
    @Shadow @Final
    public static Identifier OVERLAY_TEXTURE;

    @Unique
    private long lastUpdateTime = 0;

    @Unique
    private Identifier currentBackgroundId = generateNewBackground();

    @Unique
    private static final long UPDATE_INTERVAL = 15000; // 15 seconds

    @Unique
    private Identifier generateNewBackground() {
        return Identifier.of("nc:" + ThreadLocalRandom.current().nextInt(1, 44) + ".png");
    }

    @Unique
    private void updateBackgroundIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
            currentBackgroundId = generateNewBackground();
            lastUpdateTime = currentTime;
        }
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/RotatingCubeMapRenderer;OVERLAY_TEXTURE:Lnet/minecraft/util/Identifier;"))
    private Identifier n$modifyPanoramaOverlay() {
        updateBackgroundIfNeeded();
        return currentBackgroundId;
    }
}
