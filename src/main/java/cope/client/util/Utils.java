package cope.client.util;

import com.google.gson.Gson;

import java.util.Base64;
import java.util.Random;

public class Utils {
    private static final Gson GSON = new Gson();

    public static String randomString(int amount) {
        StringBuilder message = new StringBuilder();
        String[] chars = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
        for (int i = 0; i < amount; ++i) {
            message.append(chars[new Random().nextInt(chars.length)]);
        }
        return message.toString();
    }

    public static void sleep(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static long getJWTExpiration(String token) {
        String[] chunks = token.split("\\.");

        Base64.Decoder decoder = Base64.getUrlDecoder();

        String payload = new String(decoder.decode(chunks[1]));
        JWT jwt = GSON.fromJson(payload, JWT.class);
        return jwt.exp;
    }

    public static final class JWT {
        public String id;
        public String username;
        public long iat;
        public long exp;
    }
}
