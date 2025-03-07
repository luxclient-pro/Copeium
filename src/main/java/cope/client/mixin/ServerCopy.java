package cope.client.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameMenuScreen.class)
public abstract class ServerCopy extends Screen {
    public ServerCopy(Text text) {
        super(text);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ServerInfo serverEntry;
        if (Screen.isCopy(keyCode) && (serverEntry = MeteorClient.mc.getCurrentServerEntry()) != null) {
            MeteorClient.mc.keyboard.setClipboard(serverEntry.address);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
