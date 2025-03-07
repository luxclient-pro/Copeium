package cope.client.util.api.responses;

public class AuthResponse {
    public String error;
    public String accessToken;
    public String refreshToken;
    public String discord_id;
    public String discord_username;

    public boolean isError() {
        return error != null;
    }
}
