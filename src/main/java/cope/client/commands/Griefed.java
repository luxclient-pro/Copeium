package cope.client.commands;

import com.google.gson.JsonObject;
import cope.client.util.CopeSystem;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ForkJoinPool;


public class Griefed extends Command {
    private static final String API_ENDPOINT = "api/server/log";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public Griefed() {
        super("griefed", "Logs the current server to the API.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            ServerInfo server = mc.getCurrentServerEntry();

            // Check if the player is connected to a server
            if (server == null) {
                mc.player.sendMessage(Text.literal("Not connected to a server."), false);
                return SINGLE_SUCCESS;
            }

            // Extract IP and port from the server address
            String address = server.address;
            String[] parts = address.split(":");
            String ip = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;

            // Create JSON payload for the API
            JsonObject payload = new JsonObject();
            payload.addProperty("ip", ip);
            payload.addProperty("port", port);

            // Send the API request asynchronously
            ForkJoinPool.commonPool().submit(() -> {
                try {
                    // Construct the full API URL
                    String apiUrl = CopeSystem.get().apiUrl;
                    if (apiUrl.endsWith("/")) {
                        apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
                    }
                    String fullUrl = apiUrl + "/" + API_ENDPOINT;

                    // Build and send the HTTP POST request
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + CopeSystem.get().accessToken)
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                    HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                    // Handle the response on the main thread
                    mc.execute(() -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            mc.player.sendMessage(Text.literal("Server logged successfully."), false);
                        } else {
                            mc.player.sendMessage(Text.literal("Failed to log server: " + response.body()), false);
                        }
                    });
                } catch (Exception e) {
                    // Handle errors on the main thread
                    mc.execute(() -> {
                        mc.player.sendMessage(Text.literal("Error logging server: " + e.getMessage()), false);
                    });
                }
            });

            return SINGLE_SUCCESS;
        });
    }
}
