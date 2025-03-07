package cope.client.modules;

import cope.client.Cope;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.nio.ByteBuffer;
import java.util.*;


public class GirlBoss extends Module {
    public GirlBoss() {
        super(Cope.CATEGORY, "Girlboss", "Notifies you when someone receives a private message.");
    }

    private static final Multimap<ByteBuffer, UUID> signatures = MultimapBuilder.hashKeys().hashSetValues().build();
    private static final Set<ByteBuffer> seen = new HashSet<>();
    private static final Set<String> alertsSent = new HashSet<>();
    private static int alertsClearedTick = -1;
    private static boolean hasSentFirstAlert = false;

    public static void receivedMessage(final ByteBuffer signature) {
        seen.add(signature);
    }

    public static void addSeenSignature(final UUID sender, final ByteBuffer signature) {
        if (seen.contains(signature) || signatures.containsEntry(signature, sender)) {
            return;
        }

        signatures.put(signature, sender);
        if (MinecraftClient.getInstance().world == null) {
            return;
        }

        final Collection<UUID> uuids = signatures.get(signature);
        final List<String> names = new ArrayList<>();
        for (final UUID uuid : uuids) {
            final PlayerEntity playerEntity = MinecraftClient.getInstance().world.getPlayerByUuid(uuid);
            if (playerEntity != null) {
                names.add(playerEntity.getName().getString());
            }
        }

        final int last = names.size() - 1;
        final String joinedNames;
        if (names.size() > 1) {
            final String initialNames = String.join(", ", names.subList(0, last));
            joinedNames = initialNames + " and " + names.get(last);
        } else {
            joinedNames = names.get(0); // Only one name
        }
        final String verb = names.size() > 1 ? "have" : "has";

        final String message = "§a[Girlboss] §7" + (names.size() == 1 ? names.get(0) : joinedNames) + " " + verb + " seen a message you haven't!";
        if (MinecraftClient.getInstance().player.age != alertsClearedTick) {
            alertsClearedTick = MinecraftClient.getInstance().player.age;
            alertsSent.clear();
        }
        if (alertsSent.add(message) && hasSentFirstAlert) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal(message), false);
        }
        hasSentFirstAlert = true;
    }
}
