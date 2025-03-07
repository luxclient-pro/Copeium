package cope.client.modules;

import cope.client.Cope;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class BoyKisser extends Module {
    public BoyKisser() {
        super(Cope.CATEGORY, "Boykisser", "Makes everyone a boykisser :3");
    }

    public static final Identifier boykisser = Identifier.of("nc","boykisserskin.png");
}
