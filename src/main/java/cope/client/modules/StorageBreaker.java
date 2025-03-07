package cope.client.modules;

import cope.client.Cope;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;
import java.util.List;
import java.util.ArrayList;

public class StorageBreaker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Block selection setting
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Blocks to mine.")
        .defaultValue(
            Blocks.SHULKER_BOX,
            Blocks.WHITE_SHULKER_BOX,
            Blocks.ORANGE_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX,
            Blocks.LIGHT_BLUE_SHULKER_BOX,
            Blocks.YELLOW_SHULKER_BOX,
            Blocks.LIME_SHULKER_BOX,
            Blocks.PINK_SHULKER_BOX,
            Blocks.GRAY_SHULKER_BOX,
            Blocks.LIGHT_GRAY_SHULKER_BOX,
            Blocks.CYAN_SHULKER_BOX,
            Blocks.PURPLE_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX,
            Blocks.BROWN_SHULKER_BOX,
            Blocks.GREEN_SHULKER_BOX,
            Blocks.RED_SHULKER_BOX,
            Blocks.BLACK_SHULKER_BOX,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.HOPPER
        )
        .build()
    );

    // Range setting
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Mining range.")
        .defaultValue(4)
        .min(1)
        .max(6)
        .build()
    );

    // Delay between mining attempts
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between mining attempts in ticks.")
        .defaultValue(0)
        .min(0)
        .max(20)
        .build()
    );

    // Rotation setting
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates you towards the block being mined.")
        .defaultValue(true)
        .build()
    );

    // Auto tool setting
    private final Setting<Boolean> autoTool = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-tool")
        .description("Automatically switches to the best tool.")
        .defaultValue(true)
        .build()
    );

    // Render settings
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a box around the valid blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    private int delayLeft = 0;
    private BlockPos currentPos = null;
    private List<BlockPos> validBlocks = new ArrayList<>();

    public StorageBreaker() {
        super(Cope.CATEGORY, "Storage-Breaker", "Automatically mines selected blocks using packet mine.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Update valid blocks list
        validBlocks = findAllBlocks();

        // Handle delay
        if (delayLeft > 0) {
            delayLeft--;
            return;
        }

        // Find blocks to mine
        if (currentPos == null || mc.world.getBlockState(currentPos).isAir()) {
            currentPos = validBlocks.isEmpty() ? null : validBlocks.get(0);
        }

        // Mine block if found
        if (currentPos != null) {
            // Rotate to block if enabled
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(currentPos), Rotations.getPitch(currentPos));
            }

            // Switch to best tool if enabled
            if (autoTool.get()) {
                FindItemResult tool = InvUtils.findFastestTool(mc.world.getBlockState(currentPos));
                if (tool.found()) {
                    InvUtils.swap(tool.slot(), false);
                }
            }

            mine(currentPos);
            delayLeft = delay.get();
        }
    }

    private List<BlockPos> findAllBlocks() {
        List<BlockPos> blocks = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();
        int r = range.get();

        // Search in range
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    // Check if block is in target list
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (!this.blocks.get().contains(block)) continue;

                    // Check if block is minable
                    if (!BlockUtils.canBreak(pos)) continue;

                    blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    private void mine(BlockPos pos) {
        Direction direction = mc.player.getHorizontalFacing();

        // Send start mining packet
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
            pos,
            direction
        ));

        // Send stop mining packet
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
            pos,
            direction
        ));
    }

    private void abortMining(BlockPos pos) {
        if (pos != null && mc.getNetworkHandler() != null) {
            Direction direction = mc.player.getHorizontalFacing();

            // Send abort mining packet
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                pos,
                direction
            ));
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        // Render all valid blocks
        for (BlockPos pos : validBlocks) {
            event.renderer.box(pos,
                sideColor.get(),
                lineColor.get(),
                shapeMode.get(),
                0);
        }
    }

    @Override
    public void onDeactivate() {
        // Cancel any ongoing mining operation
        if (currentPos != null) {
            abortMining(currentPos);
        }

        // Also cancel for all valid blocks to be safe
        for (BlockPos pos : validBlocks) {
            abortMining(pos);
        }

        currentPos = null;
        delayLeft = 0;
        validBlocks.clear();
    }
}
