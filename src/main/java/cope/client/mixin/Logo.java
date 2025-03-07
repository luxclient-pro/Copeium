package cope.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;

@Mixin(LogoDrawer.class)
public class Logo {
    @Unique
    private final Identifier SKIBIDI_TITLE = Identifier.of("nc:logo.png");
    @Shadow
    @Final
    private boolean ignoreAlpha;
    /**
     * @author a
     * @reason a
     */
    @Overwrite
    public void draw(DrawContext context, int screenWidth, float alpha, int y) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.ignoreAlpha ? 1.0f : alpha);
        int textureWidth = (int) (1024 / 3.6);
        int xPosition = (screenWidth / 2) - (textureWidth / 2);
        int textureHeight = (int) (235 / 3.6);
        int yOffset = 0;
        context.drawTexture(RenderLayer::getGuiTextured, SKIBIDI_TITLE, xPosition, y + yOffset, 0.0f, 0.0f, textureWidth, textureHeight, textureWidth, textureHeight);
    }
}
