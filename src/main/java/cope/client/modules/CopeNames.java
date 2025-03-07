package cope.client.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import cope.client.Cope;
import org.apache.commons.lang3.StringUtils;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CopeNames extends Module {
    private static final String TICKET_ID_PLACEHOLDER = "<ticketid>";
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current(); // More efficient than Random
    private static final ColorModes[] COLOR_VALUES = ColorModes.values(); // Cache enum values

    // Pre-compute formatting values
    private static final Formatting[] FORMATTING_VALUES = Formatting.values();
    private static final String BASE36_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public enum ColorModes {
        aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green,
        dark_purple, dark_red, gold, gray, green, italic, light_purple,
        red, white, yellow
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgText = settings.createGroup("Text Options");
    private final SettingGroup sgSpawning = settings.createGroup("Spawn Settings");
    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");

    private final Setting<Boolean> disconnectdisable = sgGeneral.add(new BoolSetting.Builder()
        .name("Disable on Disconnect")
        .description("Disables module on disconnecting")
        .defaultValue(false)
        .build());

    private final Setting<List<String>> texts = sgText.add(new StringListSetting.Builder()
        .name("Texts")
        .description("Text lines to display. Use <ticketid> for server-specific ID")
        .defaultValue(List.of("Trolled by Skibidi Loss Prevention Inc | #<ticketid> | discord.gg/HQyNhNKwMv", "discord.gg/HQyNhNKwMv | Owned you | #<ticketid>"))
        .build());

    private final Setting<Boolean> rainbow = sgText.add(new BoolSetting.Builder()
        .name("Rainbow")
        .description("Randomly cycles through all available colors")
        .defaultValue(false)
        .build());

    private final Setting<ColorModes> textColor = sgText.add(new EnumSetting.Builder<ColorModes>()
        .name("Text Color")
        .description("Color of the text")
        .defaultValue(ColorModes.red)
        .visible(() -> !rainbow.get())
        .build());

    private final Setting<Integer> radius = sgSpawning.add(new IntSetting.Builder()
        .name("Radius")
        .description("Spawn radius")
        .defaultValue(50) // Increased to 50 blocks
        .min(1)
        .sliderMax(50)
        .build());

    private final Setting<Integer> height = sgSpawning.add(new IntSetting.Builder()
        .name("Height")
        .description("Base spawn height relative to player")
        .defaultValue(0)
        .sliderRange(-50, 50)  // Changed to ±50 blocks
        .build());

    private final Setting<Boolean> heightVariation = sgSpawning.add(new BoolSetting.Builder()
        .name("Height Variation")
        .description("Enable random height variation")
        .defaultValue(true)
        .build());

    private final Setting<Integer> heightVariationRange = sgSpawning.add(new IntSetting.Builder()
        .name("Height Variation Range")
        .description("Maximum blocks to vary height by (plus or minus)")
        .defaultValue(25)
        .min(1)
        .sliderMax(50)
        .visible(() -> heightVariation.get())
        .build());

    private final Setting<Integer> spawnDelay = sgSpawning.add(new IntSetting.Builder()
        .name("Spawn Delay")
        .description("Delay between spawns in ticks")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build());

    private final Setting<Integer> spawnCount = sgSpawning.add(new IntSetting.Builder()
        .name("Spawn Count")
        .description("How many to spawn per tick")
        .defaultValue(1)
        .min(1)
        .sliderMax(100)
        .build());

    private final Setting<Boolean> muteSounds = sgMisc.add(new BoolSetting.Builder()
        .name("Mute Sounds")
        .description("Prevents playing armor stand placement sounds")
        .defaultValue(true)
        .build());

    private int ticks;
    private Vec3d origin;
    private String namecolour;
    private String cachedTicketId;
    private ItemStack cachedArmorStand;
    private long lastServerCheck;

    public CopeNames() {
        super(Cope.CATEGORY, "Cope-texts", "Spawns invisible armor stands with custom text. Requires creative mode.");
        // Pre-create armor stand ItemStack
        cachedArmorStand = new ItemStack(Items.ARMOR_STAND);
    }

    private String getTicketNumber() {
        // Cache ticket ID for 30 seconds
        long currentTime = System.currentTimeMillis();
        if (cachedTicketId != null && currentTime - lastServerCheck < 30000) {
            return cachedTicketId;
        }

        if (mc.getCurrentServerEntry() == null) return "OFFLINE";

        try {
            String[] addressParts = mc.getCurrentServerEntry().address.split(":");
            String ip = addressParts[0];
            int port = addressParts.length > 1 ? Integer.parseInt(addressParts[1]) : 25565;

            Inet4Address address = (Inet4Address) Inet4Address.getByName(ip);
            byte[] bytes = address.getAddress();

            long ipNum = ((bytes[0] & 0xFFL) << 24) |
                ((bytes[1] & 0xFFL) << 16) |
                ((bytes[2] & 0xFFL) << 8) |
                (bytes[3] & 0xFFL);

            long combined = (ipNum << 16) | (port & 0xFFFF);
            cachedTicketId = toBase36(combined);
            lastServerCheck = currentTime;
            return cachedTicketId;

        } catch (UnknownHostException e) {
            return StringUtils.abbreviate(mc.player.getName().getString(), 15);
        }
    }

    private String toBase36(long number) {
        StringBuilder result = new StringBuilder();
        do {
            result.insert(0, BASE36_CHARS.charAt((int)(number % 36)));
            number /= 36;
        } while (number > 0);
        return result.toString();
    }

    private String replacePlaceholders(String text) {
        return text.contains(TICKET_ID_PLACEHOLDER) ?
            text.replace(TICKET_ID_PLACEHOLDER, getTicketNumber()) :
            text;
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required!");
            toggle();
            return;
        }
        ticks = 0;
        origin = null;
        namecolour = "";
        cachedArmorStand = new ItemStack(Items.ARMOR_STAND);
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disconnectdisable.get() && event.screen instanceof DisconnectedScreen) toggle();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disconnectdisable.get()) toggle();
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        if (muteSounds.get() && event.sound.getId().getPath().contains("entity.armor_stand.place")) {
            event.cancel();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        origin = mc.player.getPos();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (ticks >= spawnDelay.get()) {
            updateNameColor();
            for (int i = 0; i < spawnCount.get(); i++) {
                spawnArmorStand();
            }
            ticks = 0;
        }
        ticks++;
    }

    private void updateNameColor() {
        if (rainbow.get()) {
            namecolour = COLOR_VALUES[RANDOM.nextInt(COLOR_VALUES.length)].toString();
        } else {
            namecolour = textColor.get().toString();
        }
    }

    private Vec3d pickRandomPos() {
        double x = RANDOM.nextDouble(origin.x - radius.get(), origin.x + radius.get());

        // Calculate base height with extended range
        double baseHeight = mc.player.getY() + height.get();

        // Add randomized height variation if enabled
        double y = heightVariation.get()
            ? baseHeight + RANDOM.nextDouble(-heightVariationRange.get(), heightVariationRange.get())
            : baseHeight;

        // Ensure y coordinate stays within world bounds
        y = Math.max(-64, Math.min(320, y)); // Clamp to valid Minecraft height range

        double z = RANDOM.nextDouble(origin.z - radius.get(), origin.z + radius.get());
        return new Vec3d(x, y, z);
    }

    private void spawnArmorStand() {
        ItemStack current = mc.player.getMainHandStack();
        Vec3d pos = pickRandomPos();
        List<String> textList = texts.get();
        String selectedText = replacePlaceholders(textList.get(RANDOM.nextInt(textList.size())));

        var changes = ComponentChanges.builder()
            .add(DataComponentTypes.CUSTOM_NAME, Text.literal(selectedText).formatted(Formatting.valueOf(namecolour.toUpperCase())))
            .add(DataComponentTypes.ENTITY_DATA, createEntityData(pos, selectedText))
            .build();

        cachedArmorStand.applyChanges(changes);

        BlockHitResult bhr = new BlockHitResult(pos, Direction.UP, BlockPos.ofFloored(pos), false);
        mc.interactionManager.clickCreativeStack(cachedArmorStand, 36 + mc.player.getInventory().selectedSlot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.interactionManager.clickCreativeStack(current, 36 + mc.player.getInventory().selectedSlot);
    }

    private NbtComponent createEntityData(Vec3d pos, String selectedText) {
        NbtCompound entityTag = new NbtCompound();
        NbtList position = new NbtList();

        position.add(NbtDouble.of(pos.x));
        position.add(NbtDouble.of(pos.y));
        position.add(NbtDouble.of(pos.z));

        entityTag.putString("id", "minecraft:armor_stand");
        entityTag.put("Pos", position);
        entityTag.putBoolean("Invisible", true);
        entityTag.putBoolean("Marker", true);
        entityTag.putBoolean("NoGravity", true);
        entityTag.putBoolean("CustomNameVisible", true);
        entityTag.putString("CustomName", "{\"text\":\"" + selectedText + "\",\"color\":\"" + namecolour + "\"}");

        return NbtComponent.of(entityTag);
    }
}
