package cope.client.mixin;

import cope.client.modules.BetterPauseScreen;
import cope.client.modules.DisconnectScreenPlus;
import cope.client.screen.BetterConfirmScreen;
import cope.client.util.TextConstants;
import it.unimi.dsi.fastutil.Pair;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends Screen {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    protected DisconnectedScreenMixin() {
        super(null);
    }

    @Shadow @Final private DirectionalLayoutWidget grid;
    @Shadow @Final private Screen parent;

    private ServerInfo getLastServerInfo() {
        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
        if (autoReconnect == null) return null;

        Pair<ServerAddress, ServerInfo> lastServerConn = autoReconnect.lastServerConnection;
        if (lastServerConn == null || lastServerConn.right() == null) {
            // Try to get server info from current connection if available
            if (mc.getCurrentServerEntry() != null) {
                ServerInfo currentServer = new ServerInfo(
                    "Current Server",
                    mc.getCurrentServerEntry().address,
                    ServerInfo.ServerType.OTHER // Use the appropriate ServerType constant
                );
                return currentServer;
            }
            return null;
        }
        return lastServerConn.right();
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V"))
    private void beforeRefreshPositions(CallbackInfo ci) {
        if (!Modules.get().isActive(DisconnectScreenPlus.class)) return;

        // Get server info safely
        ServerInfo serverInfo = getLastServerInfo();
        if (serverInfo == null) {
            ChatUtils.error("Cannot add button: No server information available");
            return;
        }

        // Ensure parent screen is MultiplayerScreen
        if (!(parent instanceof MultiplayerScreen)) {
            ChatUtils.error("Cannot add button: Invalid parent screen");
            return;
        }

        MultiplayerScreen multiplayerScreen = (MultiplayerScreen) parent;
        MinecraftClient mcClient = MinecraftClient.getInstance();

        // Add Delete button with null checks
        grid.add(ButtonWidget.builder(TextConstants.DELETE, button -> {
            if (serverInfo != null && mcClient != null) {
                mcClient.setScreen(new BetterConfirmScreen(
                    multiplayerScreen,
                    () -> {
                        BetterPauseScreen.deleteServer(serverInfo);
                        mcClient.setScreen(multiplayerScreen);
                        multiplayerScreen.refresh();
                    },
                    TextConstants.DELETE_CONFIRM_TITLE,
                    TextConstants.DELETE_CONFIRM_DESCRIPTION.get(serverInfo.name)
                ));
            }
        }).dimensions(grid.getX(), grid.getY() + grid.getHeight() + 4, 200, 20).build());
    }
}
