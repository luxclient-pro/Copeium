package cope.client.mixin;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import cope.client.modules.BetterPauseScreen;
import cope.client.Cope;
import cope.client.screen.BetterConfirmScreen;
import cope.client.screen.GameMenuExtrasScreen;
import cope.client.util.TextConstants;
import cope.client.util.CopeSystem;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    private static final String API_ENDPOINT = "api/server/log";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    protected GameMenuScreenMixin() {
        super(null);
    }

    @Shadow protected abstract ButtonWidget createButton(Text text, Supplier<Screen> screenSupplier);

    @Shadow public abstract void tick();

    @Shadow public abstract void disconnect();

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        BetterPauseScreen bps = Modules.get().get(BetterPauseScreen.class);
        if (!bps.isActive() || !bps.disconnectAndDeleteButton.get()) return;

        // Add Disconnect and Delete button
        ButtonWidget disconnectButton = ButtonWidget.builder(TextConstants.DISCONNECT_AND_DELETE, btn -> {
            Cope.MC.setScreen(new BetterConfirmScreen(this, () -> {
                BetterPauseScreen.deleteCurrentServer();
                disconnect();
            }, TextConstants.DISCONNECT_AND_DELETE_CONFIRM_TITLE, TextConstants.DISCONECT_AND_DELETE_CONFIRM_DESCRIPTION));
        }).dimensions(this.width / 2 - 102, this.height / 4 + 144, 204, 20).build();

        // Add Mark as Griefed button
        ButtonWidget griefButton = ButtonWidget.builder(Text.literal("Mark as Griefed"), btn -> {
            Cope.MC.setScreen(new BetterConfirmScreen(this, () -> {
                handleGriefAndDisconnect(btn);
            }, Text.literal("Confirm Grief"), Text.literal("Are you sure you want to mark this server as griefed?")));
        }).dimensions(this.width / 2 - 102, this.height / 4 + 168, 204, 20).build();

        if (Cope.MC.isInSingleplayer()) {
            disconnectButton.active = false;
            disconnectButton.setTooltip(Tooltip.of(TextConstants.NOT_AVAILABLE_IN_SINGLEPLAYER));
            griefButton.active = false;
            griefButton.setTooltip(Tooltip.of(TextConstants.NOT_AVAILABLE_IN_SINGLEPLAYER));
        }

        addDrawableChild(disconnectButton);
        addDrawableChild(griefButton);
    }

    private void handleGriefAndDisconnect(ButtonWidget button) {
        button.active = false;
        ServerInfo server = Cope.MC.getCurrentServerEntry();

        if (server == null) {
            button.active = true;
            return;
        }

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
                        BetterPauseScreen.deleteCurrentServer();
                        disconnect();
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Screen.isCopy(keyCode) && Modules.get().isActive(BetterPauseScreen.class) && GameMenuExtrasScreen.copyServerIP()) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
