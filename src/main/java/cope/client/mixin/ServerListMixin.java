package cope.client.mixin;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import cope.client.util.CopeSystem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ForkJoinPool;

@Mixin(MultiplayerScreen.class)
public class ServerListMixin {
    @Shadow
    private MultiplayerServerListWidget serverListWidget;

    private ButtonWidget griefButton;
    private static final String API_ENDPOINT = "api/server/log";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Inject(method = "init", at = @At("TAIL"))
    private void addGriefButton(CallbackInfo ci) {
        MultiplayerScreen screen = (MultiplayerScreen) (Object) this;

        // Back button is at width/2 - 100, width 200
        // Position grief button immediately after back button
        int backButtonX = screen.width / 2 - 100;
        int backButtonWidth = 200;

        this.griefButton = ButtonWidget.builder(
                Text.literal("Griefed"),
                button -> {
                    MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
                    if (entry instanceof MultiplayerServerListWidget.ServerEntry serverEntry) {
                        handleGriefedServer(serverEntry.getServer(), button);
                    }
                })
            .dimensions(backButtonX + backButtonWidth + 60, screen.height - 30, 75, 20)  // Position right after back button
            .build();

        ((IMultiplayerScreenAccessor) screen).invokeAddDrawableChild(this.griefButton);
    }

    private void handleGriefedServer(ServerInfo server, ButtonWidget button) {
        button.active = false;

        JsonObject payload = new JsonObject();
        payload.addProperty("ip", server.address.split(":")[0]);
        payload.addProperty("port", getPortFromAddress(server.address));

        ForkJoinPool.commonPool().submit(() -> {
            try {
                String apiUrl = CopeSystem.get().apiUrl;
                if (apiUrl.endsWith("/")) {
                    apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
                }

                String fullUrl = apiUrl + "/" + API_ENDPOINT;
                System.out.println("Making request to: " + fullUrl);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + CopeSystem.get().accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                MeteorClient.mc.execute(() -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        System.out.println("Server logged successfully");

                        // Get the current screen and server list
                        MultiplayerScreen screen = (MultiplayerScreen)(Object)this;

                        // Remove the server and update the UI
                        screen.getServerList().remove(server);
                        screen.getServerList().saveFile();

                        // Update the server list widget
                        serverListWidget.setServers(screen.getServerList());
                        serverListWidget.setSelected(null);

                        // Refresh the screen
                        screen.init(MeteorClient.mc, screen.width, screen.height);
                    } else {
                        System.out.println("Failed to log server: " + response.body());
                    }
                    button.active = true;
                });
            } catch (Exception e) {
                System.out.println("Error sending request: " + e.getMessage());
                e.printStackTrace();
                MeteorClient.mc.execute(() -> button.active = true);
            }
        });
    }

    private int getPortFromAddress(String address) {
        String[] parts = address.split(":");
        return parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;
    }
}
