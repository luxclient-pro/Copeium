package cope.client.util;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.NbtCompound;


public class CopeSystem extends System<CopeSystem> {
    public String apiUrl = "http://185.239.239.96:3000/";
    //public String apiUrl = "http://localhost:3000/";
    public String accessToken = null;
    public String refreshToken = null;
    public String discordId = null;
    public String discordUsername = null;

    public CopeSystem() {
        super("skibidi-config");
        init();
        load(MeteorClient.FOLDER);
    }

    public static CopeSystem get() {
        return Systems.get(CopeSystem.class);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("apiUrl", apiUrl);
        tag.putString("accessToken", accessToken);
        tag.putString("refreshToken", refreshToken);
        tag.putString("userId", discordId);
        tag.putString("username", discordUsername);

        return tag;
    }

    @Override
    public CopeSystem fromTag(NbtCompound tag) {
        apiUrl = tag.getString("apiUrl");
        accessToken = tag.getString("accessToken");
        refreshToken = tag.getString("refreshToken");
        discordId = tag.getString("userId");
        discordUsername = tag.getString("username");

        return this;
    }
}
