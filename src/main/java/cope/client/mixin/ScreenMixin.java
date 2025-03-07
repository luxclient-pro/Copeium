package cope.client.mixin;

import cope.client.client.interfaces.IScreen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Screen.class)
public abstract class ScreenMixin implements IScreen {
    @Shadow protected abstract <T extends Element> T addDrawableChild(T drawableElement);

    @Override
    public <T extends Element> T invokeAddDrawableChild(T drawableElement) {
        return this.addDrawableChild(drawableElement);
    }
}
