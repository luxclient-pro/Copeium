package cope.client;

import com.google.gson.Gson;
import meteordevelopment.meteorclient.MeteorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import puyodead1.mlp.events.GrieferUpdateEvent;
//import puyodead1.mlp.modules.GrieferTracer;
//import puyodead1.mlp.modules.PrivacyMode;
//import puyodead1.mlp.modules.StreamerMode;
//import puyodead1.mlp.modules.WaypointSync;
import cope.client.util.CopeSystem;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public final class CopeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopeService.class);
    private static final Long backgroundRefreshIntervalSeconds = 5L;
    private static final Gson GSON = new Gson();
    private static final String RSA_PUBLIC_KEY = """
        -----BEGIN PUBLIC KEY-----
        MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxuFgmy8LJG5Lt3cHBOOa
        plV+EsP3E/Z2ipWybLh/5wIpBPMEOC5fX2VQb5BOJeR0BZh2NgesRb1/KRfKMRSM
        D4iQNsnFFhaHmraMcC3Wv5T4X8BUOkEgJhjyxZt8a/TkaNt42PNLmSHsZwYnr3QR
        duNeC+nHCFCkB6sJTELPNcJtUVsu2/vspmkwUhE1Evw6zNJeRIuU6QihfUo/ZHnL
        W+03t7U9+RqScoHt1I4izk4ddWaGHh4vB8SL29w67E8SQOo7TesQuqkuhzeG/2we
        s9gOU1inon8/iesJCY8I7LA7k5ib0RnDyHLo/vpEg9atJ/lAG+Ooc4tt99K5xsiU
        oQIDAQAB
        -----END PUBLIC KEY-----
        """;
    public final Executor executor = Executors.newFixedThreadPool(3, r -> {
        Thread thread = new Thread(r);
        thread.setName("SkibidiService");
        return thread;
    });
    public final ScheduledExecutorService backgroundActiveExecutorService = new ScheduledThreadPoolExecutor(1);
    private final long HTTP_TIMEOUT = 30L;
    private final HttpClient clientDelegate = HttpClient.newBuilder().build();


    public static String getApiUrl() {
        return CopeSystem.get().apiUrl;
    }

    public void refreshAuth(RefreshAuthRequest req, Consumer<RefreshAuthResponse> resultConsumer) {
        this.executor.execute(() -> {
            try {
                String content = this.post(getApiUrl() + "api/auth/refresh", req);
                RefreshAuthResponse result = GSON.fromJson(content, RefreshAuthResponse.class);
                resultConsumer.accept(result);
            } catch (Throwable e) {
                LOGGER.error("CopeService RefreshAuth Error", e);
            }
        });
    }

    public CheckAuthResponse checkAuth() {
        try {
            String content = this.httpGet(getApiUrl() + "api/auth/check");
            return GSON.fromJson(content, CheckAuthResponse.class);
        } catch (Throwable e) {
            LOGGER.error("CopeService CheckAuth Error", e);
        }

        return null;
    }


    private String post(String url, Object req) {
        String body = GSON.toJson(req);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(HTTP_TIMEOUT)).header("Authorization", "Bearer " + CopeSystem.get().accessToken).headers("x-player-name", MeteorClient.mc.getSession().getUsername()).headers("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return this.execute(request);
    }

    private String httpGet(String url) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(HTTP_TIMEOUT)).header("Authorization", "Bearer " + CopeSystem.get().accessToken).headers("x-player-name", MeteorClient.mc.getSession().getUsername()).GET().build();
        return this.execute(request);
    }

    private String execute(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = this.clientDelegate.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
        int code = response.statusCode();
        if (code != 200) {
            throw new RuntimeException("status: " + code + " body: " + response.body());
        }
        return response.body();
    }

    public static class CheckAuthResponse {
        public String error;
        public String data;
        public String signature;

        public static RSAPublicKey loadPublicKey() throws GeneralSecurityException {
            String publicKeyContent = RSA_PUBLIC_KEY.replaceAll("\\n", "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");

            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
            return (RSAPublicKey) kf.generatePublic(keySpecX509);
        }

        public boolean isError() {
            return error != null;
        }

        public boolean isValid() throws GeneralSecurityException {
            Signature signatureVerify = Signature.getInstance("SHA256withRSA");
            signatureVerify.initVerify(loadPublicKey());
            signatureVerify.update(Base64.getDecoder().decode(data));
            return signatureVerify.verify(Base64.getDecoder().decode(signature));
        }
    }

    public static final class RefreshAuthRequest {
        public String refreshToken;
        public String id;
    }

    public static final class RefreshAuthResponse {
        public String accessToken;
        public String refreshToken;
        public String error;

        public boolean isError() {
            return error != null;
        }
    }


    public static final class GetAccountResponse {
        public String username;
        public String uuid;
        public String token;
    }


}
