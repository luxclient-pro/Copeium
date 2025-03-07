package cope.client.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import static cope.client.Cope.CATEGORY;
import static cope.client.Cope.MC;

public class ItemBurner extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum DestroyMode {
        Fire,
        Lava
    }

    private final Setting<DestroyMode> mode = sgGeneral.add(new EnumSetting.Builder<DestroyMode>()
        .name("mode")
        .description("The mode to use for destroying items.")
        .defaultValue(DestroyMode.Fire)
        .build()
    );

    private final Setting<Integer> lightDelay = sgGeneral.add(new IntSetting.Builder()
        .name("light-delay")
        .description("The delay in ticks before lighting items on fire.")
        .defaultValue(2)
        .range(1, 20)
        .visible(() -> mode.get() == DestroyMode.Fire)
        .build()
    );

    private final Setting<Integer> lavaDelay = sgGeneral.add(new IntSetting.Builder()
        .name("lava-delay")
        .description("The delay in ticks between placing and picking up lava.")
        .defaultValue(4)
        .range(1, 20)
        .visible(() -> mode.get() == DestroyMode.Lava)
        .build()
    );

    private final Setting<Boolean> extinguishFire = sgGeneral.add(new BoolSetting.Builder()
        .name("extinguish-fire")
        .description("Automatically extinguishes fire after a delay.")
        .defaultValue(false)
        .visible(() -> mode.get() == DestroyMode.Fire)
        .build()
    );

    private final Setting<Integer> extinguishDelay = sgGeneral.add(new IntSetting.Builder()
        .name("extinguish-delay")
        .description("The delay in ticks before extinguishing fire.")
        .defaultValue(5)
        .min(1)
        .max(6)
        .visible(() -> mode.get() == DestroyMode.Fire && extinguishFire.get())
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range to search for items.")
        .defaultValue(4.0)
        .min(1.0)
        .max(6.0)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates towards the items when destroying.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> silentSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("silent-switch")
        .description("Silently switches to the required tool.")
        .defaultValue(true)
        .build()
    );

    private int ticks = 0;
    private BlockPos targetPos = null;
    private State state = State.SEARCH;
    private ItemEntity targetItem = null;

    private enum State {
        SEARCH,
        WAIT_BEFORE_PLACE,
        PLACE,
        WAIT,
        PICKUP,
        EXTINGUISH
    }

    public ItemBurner() {
        super(CATEGORY, "item-burner", "Burns or drowns items on the ground using Fire or Lava.");
    }

    @Override
    public void onDeactivate() {
        ticks = 0;
        targetPos = null;
        targetItem = null;
        state = State.SEARCH;
    }

    private void switchToSlot(int slot) {
        if (silentSwitch.get()) {
            InvUtils.swap(slot, false);
        } else {
            MC.player.getInventory().selectedSlot = slot;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (MC.world == null || MC.player == null) return;

        if (ticks > 0) {
            ticks--;
            return;
        }

        switch (state) {
            case SEARCH -> searchForItems();
            case WAIT_BEFORE_PLACE -> {
                state = State.PLACE;
                ticks = 0;
            }
            case PLACE -> placeDestructiveBlock();
            case WAIT -> {
                if (mode.get() == DestroyMode.Lava) {
                    state = State.PICKUP;
                } else if (extinguishFire.get()) {
                    state = State.EXTINGUISH;
                } else {
                    state = State.SEARCH;
                }
                ticks = 0;
            }
            case PICKUP -> handleLavaPickup();
            case EXTINGUISH -> handleFireExtinguish();
        }
    }

    private void searchForItems() {
        Box box = new Box(
            MC.player.getX() - range.get(),
            MC.player.getY() - range.get(),
            MC.player.getZ() - range.get(),
            MC.player.getX() + range.get(),
            MC.player.getY() + range.get(),
            MC.player.getZ() + range.get()
        );

        List<ItemEntity> items = MC.world.getEntitiesByClass(ItemEntity.class, box, Entity::isAlive);
        if (items.isEmpty()) return;

        for (ItemEntity item : items) {
            BlockPos itemPos = item.getBlockPos();

            if (!MC.world.getBlockState(itemPos.down()).isSolidBlock(MC.world, itemPos.down())) continue;
            if (!MC.world.getBlockState(itemPos).isAir()) continue;

            targetItem = item;
            targetPos = itemPos;
            state = State.WAIT_BEFORE_PLACE;
            ticks = mode.get() == DestroyMode.Fire ? lightDelay.get() : 1;
            break;
        }
    }

    private void placeDestructiveBlock() {
        if (targetPos == null || targetItem == null) {
            state = State.SEARCH;
            return;
        }

        FindItemResult tool = (mode.get() == DestroyMode.Fire) ?
            InvUtils.findInHotbar(Items.FLINT_AND_STEEL) :
            InvUtils.findInHotbar(Items.LAVA_BUCKET);

        if (!tool.found()) {
            state = State.SEARCH;
            return;
        }

        int prevSlot = MC.player.getInventory().selectedSlot;
        switchToSlot(tool.slot());

        Vec3d placePos = targetItem.getPos();
        BlockHitResult blockHit = new BlockHitResult(
            placePos,
            Direction.UP,
            targetPos.down(),
            false
        );

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(placePos), Rotations.getPitch(placePos));
        }

        MC.interactionManager.interactBlock(MC.player, Hand.MAIN_HAND, blockHit);
        MC.player.swingHand(Hand.MAIN_HAND);

        if (silentSwitch.get()) {
            switchToSlot(prevSlot);
        }

        state = State.WAIT;
        ticks = mode.get() == DestroyMode.Lava ? lavaDelay.get() :
            (extinguishFire.get() ? extinguishDelay.get() : 2);
    }

    private void handleLavaPickup() {
        if (targetPos == null) {
            state = State.SEARCH;
            return;
        }

        FindItemResult bucket = InvUtils.findInHotbar(Items.BUCKET);
        if (bucket.found()) {
            int prevSlot = MC.player.getInventory().selectedSlot;
            switchToSlot(bucket.slot());

            BlockHitResult blockHit = new BlockHitResult(
                Vec3d.ofCenter(targetPos),
                Direction.UP,
                targetPos,
                false
            );

            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(Vec3d.ofCenter(targetPos)), Rotations.getPitch(Vec3d.ofCenter(targetPos)));
            }

            MC.interactionManager.interactBlock(MC.player, Hand.MAIN_HAND, blockHit);
            MC.player.swingHand(Hand.MAIN_HAND);

            if (silentSwitch.get()) {
                switchToSlot(prevSlot);
            }
        }

        state = State.SEARCH;
        targetPos = null;
        targetItem = null;
        ticks = 2;
    }

    private void handleFireExtinguish() {
        if (targetPos != null) {
            MC.interactionManager.attackBlock(targetPos, Direction.UP);
        }

        state = State.SEARCH;
        targetPos = null;
        targetItem = null;
        ticks = 2;
    }
}
