package cope.client.modules;

import cope.client.Cope;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;

public class BetterPauseScreen extends BetterModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public Setting<Boolean> disconnectAndDeleteButton = sgGeneral.add(new BoolSetting.Builder()
        .name("Disconnect & Delete button")
        .description("Shows a disconnect & delete button")
        .defaultValue(true)
        .build()
    );

    public BetterPauseScreen() {
        super(Cope.CATEGORY, "pause-screen-plus", "Shows \"More\" button instead of \"Give Feedback\" and makes CTRL + C copy the server IP.");
    }

    public static void deleteCurrentServer() {
        deleteServer(MC.getCurrentServerEntry());
    }

    public static void deleteServer(ServerInfo server) {
        ServerList serverList = new ServerList(Cope.MC);
        serverList.loadFile();
        serverList.servers.removeIf(entry -> entry.address.equals(server.address));
        serverList.saveFile();
    }
}
