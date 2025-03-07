package cope.client.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.ChunkPos;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import cope.client.Cope;

import java.util.HashSet;
import java.util.Set;

public class LavaLand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> chunkRadius = sgGeneral.add(new IntSetting.Builder()
        .name("chunk-radius")
        .description("How many chunks in each direction to convert (1 chunk = 16 blocks).")
        .defaultValue(2)
        .min(1)
        .max(20)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> minHeight = sgGeneral.add(new IntSetting.Builder()
        .name("min-height")
        .description("Minimum height to convert blocks from.")
        .defaultValue(-63)
        .min(-64)
        .max(319)
        .sliderRange(-64, 319)
        .build()
    );

    private final Setting<Integer> maxHeight = sgGeneral.add(new IntSetting.Builder()
        .name("max-height")
        .description("Maximum height to convert blocks to.")
        .defaultValue(319)
        .min(-64)
        .max(319)
        .sliderRange(-64, 319)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between chunk processing in ticks.")
        .defaultValue(5)
        .min(1)
        .max(100)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether to render the current chunk being processed.")
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

    private int tickCounter = 0;
    private boolean hasCheckedOp = false;
    private Set<Long> processedChunks;
    private ChunkPos currentChunk;
    private int currentHeight;
    private static final int MAX_BLOCKS_PER_OPERATION = 32768;
    private static final int CHUNK_SIZE = 16;
    private static final Block[] GRASS_TYPES = {
        Blocks.GRASS_BLOCK,
        Blocks.PODZOL,
        Blocks.MYCELIUM
    };

    public LavaLand() {
        super(Cope.CATEGORY, "lava-land", "Converts grass blocks to lava chunk by chunk (requires op)");
    }

    @Override
    public void onActivate() {
        hasCheckedOp = false;
        tickCounter = 0;
        processedChunks = new HashSet<>();
        currentChunk = null;
        currentHeight = minHeight.get();

        if (maxHeight.get() < minHeight.get()) {
            error("Maximum height cannot be lower than minimum height!");
            toggle();
        }
    }

    private ChunkPos getNextChunk() {
        if (mc.player == null) return null;

        int playerChunkX = ((int) mc.player.getX()) >> 4;
        int playerChunkZ = ((int) mc.player.getZ()) >> 4;

        for (int x = -chunkRadius.get(); x <= chunkRadius.get(); x++) {
            for (int z = -chunkRadius.get(); z <= chunkRadius.get(); z++) {
                ChunkPos pos = new ChunkPos(playerChunkX + x, playerChunkZ + z);
                if (!processedChunks.contains(pos.toLong())) {
                    return pos;
                }
            }
        }
        return null;
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

        tickCounter++;
        if (tickCounter < delay.get()) return;
        tickCounter = 0;

        if (currentChunk == null) {
            currentChunk = getNextChunk();
            if (currentChunk == null) {
                info("§aFinished converting all chunks!");
                toggle();
                return;
            }
            currentHeight = minHeight.get();
        }

        processChunkSection();
    }

    private void processChunkSection() {
        int heightPerOperation = calculateHeightPerOperation();
        int endHeight = Math.min(currentHeight + heightPerOperation - 1, maxHeight.get());

        // Process each grass type
        for (Block grassType : GRASS_TYPES) {
            String fillCommand = String.format("fill %d %d %d %d %d %d minecraft:lava replace %s",
                currentChunk.getStartX(), currentHeight, currentChunk.getStartZ(),
                currentChunk.getEndX(), endHeight, currentChunk.getEndZ(),
                grassType.getTranslationKey().replace("block.minecraft.", "")
            );

            mc.player.networkHandler.sendCommand(fillCommand);
        }

        // Update progress
        currentHeight = endHeight + 1;
        if (currentHeight > maxHeight.get()) {
            processedChunks.add(currentChunk.toLong());
            currentChunk = null;
        }
    }

    private int calculateHeightPerOperation() {
        // Calculate how many blocks high we can go while staying under the limit
        int volume = CHUNK_SIZE * CHUNK_SIZE; // 16x16 for a chunk section
        int maxHeightPerOp = MAX_BLOCKS_PER_OPERATION / volume;
        int remainingHeight = maxHeight.get() - currentHeight + 1;
        return Math.min(maxHeightPerOp, remainingHeight);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || currentChunk == null || mc.player == null) return;

        double x1 = currentChunk.getStartX();
        double z1 = currentChunk.getStartZ();
        double y1 = minHeight.get();

        double x2 = currentChunk.getEndX() + 1;
        double z2 = currentChunk.getEndZ() + 1;
        double y2 = maxHeight.get();

        event.renderer.box(
            x1, y1, z1,
            x2, y2, z2,
            sideColor.get(),
            lineColor.get(),
            shapeMode.get(),
            0
        );
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }

    @Override
    public void onDeactivate() {
        hasCheckedOp = false;
        tickCounter = 0;
        processedChunks = null;
        currentChunk = null;
        currentHeight = 0;
    }
}
