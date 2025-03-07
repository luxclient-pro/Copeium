package cope.client.modules;

import cope.client.Cope;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.item.SignItem;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.client.network.ServerInfo;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.block.Block;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.HangingSignBlock;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class AutoSign extends Module {
    final SettingGroup sgSign = settings.createGroup("Sign Text");
    final SettingGroup sgSignAura = settings.createGroup("Sign Aura");
    final SettingGroup sgExtra = settings.createGroup("Extra Settings");
    private static final String TICKET_ID_PLACEHOLDER = "<ticketid>";

    private final Setting<Boolean> bothside = sgExtra.add(new BoolSetting.Builder()
        .name("both-sides")
        .description("Write on the rear of the signs as well.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> lineOne = sgSign.add(new StringSetting.Builder()
        .name("line-one")
        .description("What to put on the first line of the sign.")
        .defaultValue("Server Pwned by")
        .build()
    );

    private final Setting<String> lineTwo = sgSign.add(new StringSetting.Builder()
        .name("line-two")
        .description("What to put on the second line of the sign.")
        .defaultValue(".gg/npGmN4hb2U")
        .build()
    );

    private final Setting<String> lineThree = sgSign.add(new StringSetting.Builder()
        .name("line-three")
        .description("What to put on the third line of the sign.")
        .defaultValue("I Stay Winning")
        .build()
    );

    private final Setting<String> lineFour = sgSign.add(new StringSetting.Builder()
        .name("line-four")
        .description("What to put on the fourth line of the sign.")
        .defaultValue("#<ticketid>")
        .build()
    );

    private final Setting<Boolean> signAura = sgSignAura.add(new BoolSetting.Builder()
        .name("sign-aura")
        .description("Automatically edits signs for you")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> signAuraRotate = sgSignAura.add(new BoolSetting.Builder()
        .name("sign-aura-rotate")
        .description("Rotates to signs")
        .defaultValue(true)
        .visible(signAura::get)
        .build()
    );

    private final Setting<Double> signAuraRange = sgSignAura.add(new DoubleSetting.Builder()
        .name("sign-aura-range")
        .description("The interact range")
        .defaultValue(4)
        .min(0)
        .max(6)
        .sliderRange(0,6)
        .visible(signAura::get)
        .build()
    );

    private final Setting<Integer> signAuraDelay = sgSignAura.add(new IntSetting.Builder()
        .name("sign-aura-delay")
        .description("Delay between editing signs, in ticks")
        .defaultValue(5)
        .sliderMax(20)
        .visible(signAura::get)
        .build()
    );

    private boolean editrear = false;
    private BlockPos signPos = new BlockPos(99999999,99999999,99999999);
    private BlockPos prevsignPos = new BlockPos(99999999,99999999,99999999);
    private final ArrayList<BlockPos> openedSigns = new ArrayList<>();
    private int timer = 0;

    public AutoSign() {
        super(Cope.CATEGORY, "AutoSign", "Automatically writes signs. Credits to MeteorTweaks.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        openedSigns.clear();
        editrear = false;
    }

    private String replacePlaceholders(String text) {
        if (text.contains(TICKET_ID_PLACEHOLDER)) {
            return text.replace(TICKET_ID_PLACEHOLDER, getTicketNumber());
        }
        return text;
    }

    private boolean isValidSign(BlockPos pos) {
        if (mc.world == null) return false;
        Block block = mc.world.getBlockState(pos).getBlock();
        return (block instanceof SignBlock || block instanceof WallSignBlock) && !(block instanceof HangingSignBlock);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null) return;
        timer--;
        if(!signAura.get() || timer > 0) return;

        for(BlockEntity block : Utils.blockEntities()) {
            if(!(block instanceof SignBlockEntity) ||
                mc.player.getEyePos().distanceTo(Vec3d.ofCenter(block.getPos())) >= signAuraRange.get() ||
                !isValidSign(block.getPos())) continue;

            BlockPos pos = block.getPos();
            if(openedSigns.contains(pos)) continue;

            Runnable click = () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false));
            if(signAuraRotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), click);
            else click.run();

            openedSigns.add(pos);
            timer = signAuraDelay.get();
            break;
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if(!(event.screen instanceof SignEditScreen)) return;

        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) event.screen).getSign();
        if (!isValidSign(sign.getPos())) return;

        if (mc.world.getBlockState(sign.getPos()).getBlock().asItem() instanceof SignItem) {
            mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(sign.getPos(), true,
                replacePlaceholders(lineOne.get()),
                replacePlaceholders(lineTwo.get()),
                replacePlaceholders(lineThree.get()),
                replacePlaceholders(lineFour.get())
            ));
            if (bothside.get()) {
                editrear = true;
                if (prevsignPos != sign.getPos()) signPos = sign.getPos();
            }
        }

        event.cancel();
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (!editrear || !bothside.get() || prevsignPos == signPos || mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (!isValidSign(signPos)) return;

        if (mc.world.getBlockState(signPos).getBlock().asItem() instanceof SignItem) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(signPos.getX(), signPos.getY(), signPos.getZ()), Direction.DOWN, signPos, false));
            mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(signPos, false,
                replacePlaceholders(lineOne.get()),
                replacePlaceholders(lineTwo.get()),
                replacePlaceholders(lineThree.get()),
                replacePlaceholders(lineFour.get())
            ));
            prevsignPos = signPos;
            editrear = false;
        }
    }

    private String getTicketNumber() {
        ServerInfo entry = this.mc.getCurrentServerEntry();

        try {
            String[] addressParts = entry.address.split(":");
            String ip = addressParts[0];
            int port = addressParts.length > 1 ? Integer.parseInt(addressParts[1]) : 25565;

            Inet4Address address = (Inet4Address) Inet4Address.getByName(ip);
            byte[] bytes = address.getAddress();

            long ipNum = ((bytes[0] & 0xFFL) << 24) |
                ((bytes[1] & 0xFFL) << 16) |
                ((bytes[2] & 0xFFL) << 8) |
                (bytes[3] & 0xFFL);

            long combined = (ipNum << 16) | (port & 0xFFFF);

            return toBase36(combined);

        } catch (UnknownHostException e) {
            return StringUtils.abbreviate(this.mc.player.getName().getString(), 15);
        }
    }

    private String toBase36(long number) {
        final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder result = new StringBuilder();

        do {
            result.insert(0, ALPHABET.charAt((int)(number % 36)));
            number /= 36;
        } while (number > 0);

        return result.toString();
    }
}
