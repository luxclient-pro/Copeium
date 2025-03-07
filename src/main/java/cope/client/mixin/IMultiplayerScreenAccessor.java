package cope.client.mixin;

import net.minecraft.client.gui.Element;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.gui.screen.Screen;

@Mixin(Screen.class)
public interface IMultiplayerScreenAccessor {
    @Invoker("addDrawableChild")
    <T extends Element> T invokeAddDrawableChild(T drawable);
}
