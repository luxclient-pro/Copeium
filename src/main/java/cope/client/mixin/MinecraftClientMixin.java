package cope.client.mixin;

import com.google.common.collect.Lists;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.QuickPlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import cope.client.client.ui.LoginScreen;
import cope.client.modules.CopeLavaCast;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    protected abstract void createInitScreens(List<Function<Runnable, Screen>> list);

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Shadow
    @Final
    public GameOptions options;

    /**
     * @author Puyodead1
     * @reason return Login screen on init
     */
    @Overwrite
    private Runnable onInitFinished(@Nullable MinecraftClient.LoadingContext loadingContext) {
        ArrayList<Function<Runnable, Screen>> list = new ArrayList<>();
        this.createInitScreens(list);
        Runnable runnable = () -> {
            if (loadingContext != null && loadingContext.quickPlayData().isEnabled()) {
                QuickPlay.startQuickPlay(MinecraftClient.getInstance(), loadingContext.quickPlayData(), loadingContext.realmsClient());
            } else {
                this.setScreen(new LoginScreen());
            }
        };
        for (Function<Runnable, Screen> function : Lists.reverse(list)) {
            Screen screen = function.apply(runnable);
            runnable = () -> this.setScreen(screen);
        }
        return runnable;
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        CopeLavaCast module = Modules.get().get(CopeLavaCast.class);
        if (module.isActive() &&
            options.useKey.isPressed() &&
            module.inputting()) ci.cancel();
    }
}
