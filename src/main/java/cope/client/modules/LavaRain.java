package cope.client.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import cope.client.Cope;

import java.util.Random;

public class LavaRain extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> chunkRadius = sgGeneral.add(new IntSetting.Builder()
        .name("chunk-radius")
        .description("How many chunks in each direction to place lava (1 chunk = 16 blocks).")
        .defaultValue(2)
        .min(1)
        .max(20)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> minHeight = sgGeneral.add(new IntSetting.Builder()
        .name("min-height")
        .description("Minimum height to place lava.")
        .defaultValue(50)
        .min(-64)
        .max(319)
        .sliderRange(-64, 319)
        .build()
    );

    private final Setting<Integer> maxHeight = sgGeneral.add(new IntSetting.Builder()
        .name("max-height")
        .description("Maximum height to place lava.")
        .defaultValue(319)
        .min(-64)
        .max(319)
        .sliderRange(-64, 319)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to convert to lava per tick.")
        .defaultValue(5)
        .min(1)
        .max(50)
        .sliderMax(50)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether to render the current operation area.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the rendered box.")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the rendered box.")
        .defaultValue(new SettingColor(255, 0, 0))
        .visible(render::get)
        .build()
    );

    private boolean hasCheckedOp = false;
    private final Random random = new Random();

    public LavaRain() {
        super(Cope.CATEGORY, "lava-rain", "Randomly places lava blocks within the specified area (requires op)");
    }

    @Override
    public void onActivate() {
        hasCheckedOp = false;

        if (maxHeight.get() < minHeight.get()) {
            error("Maximum height cannot be lower than minimum height!");
            toggle();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (!hasCheckedOp) {
            if (!mc.player.hasPermissionLevel(2)) {
                error("You don't have operator permissions!");
                toggle();
                return;
            }
            hasCheckedOp = true;
        }

        int playerChunkX = ((int) mc.player.getX()) >> 4;
        int playerChunkZ = ((int) mc.player.getZ()) >> 4;

        for (int i = 0; i < blocksPerTick.get(); i++) {
            // Generate random positions within the chunk radius
            int chunkX = random.nextInt(chunkRadius.get() * 2 + 1) - chunkRadius.get() + playerChunkX;
            int chunkZ = random.nextInt(chunkRadius.get() * 2 + 1) - chunkRadius.get() + playerChunkZ;

            // Generate random block position within the chunk
            int x = chunkX * 16 + random.nextInt(16);
            int z = chunkZ * 16 + random.nextInt(16);
            int y = random.nextInt(maxHeight.get() - minHeight.get() + 1) + minHeight.get();

            // Place lava at the random position
            String setBlockCommand = String.format("setblock %d %d %d minecraft:lava", x, y, z);
            mc.player.networkHandler.sendCommand(setBlockCommand);
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }

    @Override
    public void onDeactivate() {
        hasCheckedOp = false;
    }
}
