package cope.client;


import cope.client.commands.CCrash;
import cope.client.commands.CrackedKickCommand;
import cope.client.commands.Griefed;
import cope.client.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import net.minecraft.util.NameGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class Cope extends MeteorAddon {
    public static final Identifier CAPE_TEXTURE = Identifier.of("nc:cape.png");
    public static final Logger LOG = LogUtils.getLogger();
    public static final MinecraftClient MC = MinecraftClient.getInstance();
    public static final Category CATEGORY = new Category("Copeium", Items.FLINT_AND_STEEL.getDefaultStack());


    @Override
    public void onInitialize() {
        LOG.info("Initializing Copeium");

        // Modules
        Modules.get().add(new BoomPlus());
        Modules.get().add(new CopeTrail());
        Modules.get().add(new BetterPauseScreen());
        Modules.get().add(new DisconnectScreenPlus());
        Modules.get().add(new CopeLavaCast());
        Modules.get().add(new ServerSender());
        Modules.get().add(new CopeFly());
        Modules.get().add(new StorageBreaker());
        Modules.get().add(new AutoWither());
        Modules.get().add(new GamemodeNotifier());
        Modules.get().add(new CopeSpin());
        Modules.get().add(new PenisESP());
        Modules.get().add(new ItemBurner());
        Modules.get().add(new ContainerAction());
        Modules.get().add(new AutoSign());
        Modules.get().add(new CopeStrike());
        Modules.get().add(new AutoScoreboard());
        Modules.get().add(new LavaLand());
        Modules.get().add(new CopeVoider());
        Modules.get().add(new CopeNames());
        Modules.get().add(new GirlBoss());
        Modules.get().add(new BoyKisser());
        Modules.get().add(new CrackedKickModule());
        Modules.get().add(new LavaRain());


        Commands.add(new CCrash());
        Commands.add(new CrackedKickCommand());
        Commands.add(new Griefed());
    }
    public static final CopeService copeService = new CopeService();
    public static CopeService getCopeService() {
        return copeService;
    }
    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "cope.client";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
    public static class GenericNames {
        private static final Map<UUID, String> names = new HashMap<>();
        public static String getName(UUID uuid) {
            names.computeIfAbsent(uuid, k -> NameGenerator.name(uuid));
            return names.get(uuid);
        }
        public static void clear() {
            names.clear();
        }
    }
}
