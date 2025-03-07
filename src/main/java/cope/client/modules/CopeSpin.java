package cope.client.modules;
import cope.client.Cope;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class CopeSpin extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<Mode> mode;
    private final Setting<Integer> speed;
    private float lastYaw;

    public CopeSpin() {
        super(Cope.CATEGORY, "Cope-Spin", "Goofy rotations, interferes with some placement stuff");

        this.sgGeneral = this.settings.getDefaultGroup();

        this.mode = this.sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .defaultValue(Mode.Spin)
            .build()
        );

        this.speed = this.sgGeneral.add(new IntSetting.Builder()
            .name("speed")
            .visible(() -> this.mode.get() == Mode.Spin)
            .defaultValue(5)
            .min(1)
            .max(10)
            .build()
        );
    }

    @Override
    public void onActivate() {
        if (this.mc.player != null) {
            this.lastYaw = this.mc.player.getYaw();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Item handStack = this.mc.player.getMainHandStack().getItem();
        if (handStack == Items.LAVA_BUCKET || handStack == Items.WATER_BUCKET || handStack == Items.BUCKET) {
            return;
        }
        if (this.mode.get() == Mode.Stare) {
            PlayerEntity target = TargetUtils.getPlayerTarget(9999.0, SortPriority.LowestDistance);
            if (target == null) return;
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, Target.Head));
        } else if (this.mode.get() == Mode.Spin) {
            this.lastYaw = (this.lastYaw + (float)(this.speed.get() * 10)) % 360.0f;
            Rotations.rotate(this.lastYaw, this.mc.player.getPitch());
        }
    }

    public enum Mode {
        Spin,
        Stare
    }
}
