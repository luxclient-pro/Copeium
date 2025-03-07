package cope.client.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.util.Util;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cope.client.util.api.responses.AuthResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiConsumer;


public class DiscordAuth {
    private static final int port = 9426;
    private static final Gson gson = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordAuth.class);

    private static final String clientId = "1327073174864793661";

    public static final String url = "https://discord.com/oauth2/authorize" + "?client_id=" + clientId + "&redirect_uri=http%3A%2F%2F127.0.0.1%3A" + port + "%2F" + "&response_type=code" + "&scope=identify";

    private static HttpServer server;
    private static BiConsumer<String, String> callback;

    public static void auth(BiConsumer<String, String> callback) {
        DiscordAuth.callback = callback;
        Util.getOperatingSystem().open(url);
        startServer();
    }

    private static void startServer() {
        try {
            server = HttpServer.create();
            server.bind(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/", new AuthHandler());
            server.start();
            LOGGER.info("Server started successfully on port " + port);
        } catch (Exception e) {
            LOGGER.error("Error starting server on port " + port, e);
        }
    }

    public static void stopServer() {
        LOGGER.info("Stopping server...");
        if (server != null) {
            server.stop(0);
            server = null;
            callback = null;
        }
    }

    private static class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange req) throws IOException {
            LOGGER.info("Request received: " + req.getRequestMethod());

            try {
                if (req.getRequestMethod().equalsIgnoreCase("GET")) {
                    processGetRequest(req);
                } else if (req.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    req.getResponseHeaders().add("Allow", "GET, OPTIONS");
                    req.sendResponseHeaders(204, -1);
                } else {
                    req.sendResponseHeaders(405, -1);
                    LOGGER.warn("Invalid request method: " + req.getRequestMethod());
                }
            } finally {
                stopServer(); // Shutdown the server after handling the request
            }
        }

        private void processGetRequest(HttpExchange req) throws IOException {
            List<NameValuePair> query = URLEncodedUtils.parse(req.getRequestURI(), StandardCharsets.UTF_8);
            String code = query.stream()
                .filter(pair -> pair.getName().equals("code"))
                .map(NameValuePair::getValue)
                .findFirst()
                .orElse("");

            if (code.isEmpty()) {
                LOGGER.warn("No code parameter found in request.");
                sendResponse(req, "Cannot authenticate. No code provided.", true);
                callback.accept(null, "No code provided.");
            } else {
                LOGGER.info("Code parameter received: " + code);
                handleCode(req, code);
            }
        }

        private void sendResponse(HttpExchange req, String message, boolean isError) throws IOException {
            req.getResponseHeaders().add("Content-Type", "text/plain");
            req.sendResponseHeaders(isError ? 400 : 200, message.length());
            try (OutputStream out = req.getResponseBody()) {
                out.write(message.getBytes(StandardCharsets.UTF_8));
            }
        }

        private void handleCode(HttpExchange req, String code) throws IOException {
            try {
                JsonObject params = new JsonObject();
                params.addProperty("code", code);
                String jsonResponse = SmallHttp.post(CopeSystem.get().apiUrl + "api/auth/callback", params.toString());
                AuthResponse authResponse = gson.fromJson(jsonResponse, AuthResponse.class);

                if (authResponse.isError()) {
                    LOGGER.error("Authentication failed: " + authResponse.error);
                    sendResponse(req, "Authentication failed: " + authResponse.error, true);
                    callback.accept(null, authResponse.error);
                } else {
                    CopeSystem.get().accessToken = authResponse.accessToken;
                    CopeSystem.get().refreshToken = authResponse.refreshToken;
                    CopeSystem.get().discordId = authResponse.discord_id;
                    CopeSystem.get().discordUsername = authResponse.discord_username;
                    CopeSystem.get().save();

                    LOGGER.info("Authentication successful for user: " + authResponse.discord_username);
                    sendResponse(req, "Authentication Successful. You may now close this page.", false);
                    callback.accept(authResponse.accessToken, null);
                }
            } catch (Exception e) {
                LOGGER.error("Error processing authentication code", e);
                sendResponse(req, "Internal Server Error: " + e.getMessage(), true);
                callback.accept(null, e.getMessage());
            }
        }
    }
}
