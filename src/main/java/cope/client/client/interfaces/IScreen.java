package cope.client.client.interfaces;

import net.minecraft.client.gui.Element;

public interface IScreen {
    <T extends Element> T invokeAddDrawableChild(T drawableElement);
}
