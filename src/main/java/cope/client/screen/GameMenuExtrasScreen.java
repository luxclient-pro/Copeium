package cope.client.screen;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import cope.client.util.CopeSystem;
import meteordevelopment.meteorclient.systems.modules.Modules;
import cope.client.modules.BetterPauseScreen;
import cope.client.util.TextConstants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ForkJoinPool;

public class GameMenuExtrasScreen extends Screen {
    public final GameMenuScreen parent;
    private static final String API_ENDPOINT = "api/server/log";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public GameMenuExtrasScreen(GameMenuScreen parent) {
        super(TextConstants.GAME_MENU);
        this.parent = parent;
    }

    @Override
    protected void init() {
        GridWidget gW = new GridWidget();
        gW.getMainPositioner().margin(4, 4, 4, 0);
        GridWidget.Adder adder = gW.createAdder(1);

        // Title
        this.addDrawableChild(new TextWidget(0, 40, this.width, this.textRenderer.fontHeight, this.title, this.textRenderer));

        // Back button at the top with margin
        addWideButton(adder, ScreenTexts.BACK, button -> client.setScreen(parent), gW, true);

        // Regular buttons in the middle
        GridWidget buttonsGrid = new GridWidget();
        buttonsGrid.getMainPositioner().margin(4, 4, 4, 0);
        GridWidget.Adder buttonAdder = buttonsGrid.createAdder(2);

        addButton(buttonAdder, TextConstants.COPY_IP, button -> client.keyboard.setClipboard(client.getCurrentServerEntry().address));
        addButton(buttonAdder, TextConstants.ACCOUNTS, button -> client.setScreen(new AccountsConfirmReconnectScreen(this)));

        buttonsGrid.refreshPositions();
        SimplePositioningWidget.setPos(buttonsGrid, 0, 0, this.width, this.height, 0.5f, 0.45f);
        buttonsGrid.forEachChild(this::addDrawableChild);

        // Wide buttons at the bottom
        addWideButton(adder, TextConstants.DISCONNECT_AND_DELETE, button -> {
            BetterPauseScreen bps = Modules.get().get(BetterPauseScreen.class);
            if (bps != null) {
                bps.deleteCurrentServer();
                parent.disconnect();
            }
        }, gW, false);

        addWideButton(adder, Text.literal("Mark as Griefed"), button -> {
            handleGriefAndDisconnect(button);
        }, gW, false);

        gW.refreshPositions();
        SimplePositioningWidget.setPos(gW, 0, 0, this.width, this.height, 0.5f, 0.25f);
        gW.forEachChild(this::addDrawableChild);
    }

    private void handleGriefAndDisconnect(ButtonWidget button) {
        button.active = false;
        ServerInfo server = client.getCurrentServerEntry();

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

                        // Delete server from list
                        BetterPauseScreen bps = Modules.get().get(BetterPauseScreen.class);
                        if (bps != null) {
                            bps.deleteCurrentServer();
                            parent.disconnect();
                        }
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

    private void addButton(GridWidget.Adder adder, Text message, ButtonWidget.PressAction onPress) {
        adder.add(new ButtonWidget.Builder(message, onPress).width(98).build());
    }

    private void addWideButton(GridWidget.Adder adder, Text message, ButtonWidget.PressAction onPress, GridWidget gridWidget, boolean marginTop) {
        ButtonWidget widget = new ButtonWidget.Builder(message, onPress).width(204).build();
        if (marginTop) adder.add(widget, 1, gridWidget.copyPositioner().marginTop(50));
        else adder.add(widget, 1);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Screen.isCopy(keyCode) && copyServerIP()) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static boolean copyServerIP() {
        ServerInfo serverEntry = MeteorClient.mc.getCurrentServerEntry();
        if (serverEntry != null) {
            MeteorClient.mc.keyboard.setClipboard(serverEntry.address);
            return true;
        }
        return false;
    }
}
