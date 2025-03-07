package cope.client.modules;

import cope.client.Cope;
import cope.client.util.CopeSystem;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;

public class ServerSender extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> webhookUrl = sgGeneral.add(new StringSetting.Builder()
        .name("Webhook URL")
        .description("The Discord webhook URL to send messages to.")
        .defaultValue("")
        .build());

    private boolean sentMessage = false;

    public ServerSender() {
        super(Cope.CATEGORY, "ServerSender", "Sends the server ip and your coords to a webhook.");
    }

    @Override
    public void onActivate() {
        sentMessage = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!sentMessage && isActive()) {
            sendWebhookMessage();
            sentMessage = true;
            toggle(); // Disable the module after sending the message
        }
    }

    private void sendWebhookMessage() {
        String serverIp = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "N/A";
        Vec3d playerPos = mc.player.getPos();

        String username = CopeSystem.get().discordUsername;
        if (username == null || username.isEmpty()) {
            username = mc.player.getName().getString();
        }

        try {
            URL url = new URL(webhookUrl.get());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonPayload = String.format(
                "{\"embeds\":[{" +
                    "\"title\":\"Copeium Client\"," +
                    "\"color\":0," +
                    "\"description\":\"`%s` is on **`%s`** at (**X: %.1f, Y: %.1f, Z: %.1f**)\"," +
                    "\"footer\":{\"text\":\"%s was here discord.gg/HQyNhNKwMv\"}," +
                    "\"timestamp\":\"%s\"" +
                    "}]}",
                username,
                serverIp,
                playerPos.x, playerPos.y, playerPos.z,
                username,
                Instant.now().toString()
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes("utf-8"));
            }

            if (conn.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                error("Failed to send message: " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            error("Error sending webhook: " + e.getMessage());
        }
    }
}
