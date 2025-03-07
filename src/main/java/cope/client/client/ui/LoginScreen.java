package cope.client.client.ui;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import cope.client.Cope;
import cope.client.CopeService;
import cope.client.util.DiscordAuth;
import cope.client.util.CopeSystem;
import cope.client.util.Utils;

import java.util.concurrent.ForkJoinPool;

public class LoginScreen extends Screen {
    public static final Identifier OPTIONS_BACKGROUND_TEXTURE = Identifier.of("minecraft:textures/block/tnt_side.png");
    private static final Identifier MLP_TITLE = Identifier.of("nc:logo.png");
    private ButtonWidget loginButton;
    private ButtonWidget quitButton;
    private String errorMessage;
    private boolean isChecking = false;
    private ButtonWidget retryButton;


    public LoginScreen() {
        super(Text.of("Authentication"));
    }

    @Override
    protected void init() {
        int halfHeight = (this.height / 4) + 40;

        this.loginButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Login"), button -> doAuth()).dimensions(this.width / 2 - 100, halfHeight + 16, 200, 20).build());
        this.quitButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Quit"), button -> this.client.scheduleStop()).dimensions(this.width / 2 - 100, halfHeight + 43, 200, 20).build());
        this.retryButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Retry"), button -> {
            button.active = false;
            validateAuth();
        }).dimensions(this.width / 2 - 100, halfHeight + 70, 200, 20).build());

        this.retryButton.visible = false;
        this.retryButton.active = false;

        if (CopeSystem.get().accessToken != null && !CopeSystem.get().accessToken.isEmpty() && !isChecking) {
            this.validateAuth();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        this.renderBackground(context, mouseX, mouseY, delta);

        // render Logo
        int texWidth = (int) (1024 / 3.6);
        int texHeight = (int) (235 / 3.6);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int i = (this.width / 2) - (texWidth / 2);
        context.drawTexture(RenderLayer::getGuiTextured, MLP_TITLE, i, 30, 0.0f, 0.0f, texWidth, texHeight, texWidth, texHeight);

        int halfWidth = this.width / 2;
        int halfHeight = (this.height / 4) + 40;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.of("Authentication is required"), halfWidth, 84, 0xFFFFFF);

        if (this.errorMessage != null) {
            // change y to 50
            int w = this.textRenderer.getWidth(this.errorMessage);
            context.drawTextWithShadow(this.textRenderer, Text.of(this.errorMessage), halfWidth - w, halfHeight + 100, 0xA0A0A0);
        }

        this.loginButton.render(context, mouseX, mouseY, delta);
        this.quitButton.render(context, mouseX, mouseY, delta);
        this.retryButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        float vOffset = 0.0f;

        Tessellator tessellator = Tessellator.getInstance();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, OPTIONS_BACKGROUND_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        bufferBuilder.vertex(0.0f, this.height, 0.0f).texture(0.0f, (float) this.height / 32.0f + vOffset).color(64, 64, 64, 255);
        bufferBuilder.vertex(this.width, this.height, 0.0f).texture((float) this.width / 32.0f, (float) this.height / 32.0f + vOffset).color(64, 64, 64, 255);
        bufferBuilder.vertex(this.width, 0.0f, 0.0f).texture((float) this.width / 32.0f, vOffset).color(64, 64, 64, 255);
        bufferBuilder.vertex(0.0f, 0.0f, 0.0f).texture(0.0f, vOffset).color(64, 64, 64, 255);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private void doAuth() {
        if (this.client == null) return;

        Text originalText = this.loginButton.getMessage();
        this.loginButton.active = false;
        this.loginButton.setMessage(Text.of("Continue in browser"));

        ForkJoinPool.commonPool().submit(() -> {
            DiscordAuth.auth((accessToken, error) -> {
                MeteorClient.mc.execute(() -> {
                    if (error == null && accessToken != null) {
                        validateAuth();
                        return;
                    }

                    if (error != null) {
                        this.errorMessage = "Failed to authenticate with Discord. Reason: " + error;
                    }

                    if (accessToken == null) {
                        this.errorMessage = "Failed to authenticate with Discord.";
                    }

                    this.loginButton.setMessage(originalText);
                    this.loginButton.active = true;
                    this.retryButton.active = false;
                });
            });
        });
    }

    private void validateAuth() {
        if (this.client == null) return;

        isChecking = true;
        Text originalText = this.loginButton.getMessage();
        this.loginButton.active = false;
        this.loginButton.setMessage(Text.of("Validating Authentication..."));

        this.retryButton.active = false;

        // check if the existing jwt can be refreshed
        // get jwt expiration
        final long exp = Utils.getJWTExpiration(CopeSystem.get().accessToken);
        // get current time
        final long now = (System.currentTimeMillis() / 1000L);
        final boolean isExpired = (now - exp) > 0;
        final String refreshToken = CopeSystem.get().refreshToken;

        ForkJoinPool.commonPool().submit(() -> {
            if (!isExpired) {
                checkAuth(originalText);
            } else if (refreshToken != null) {
                refreshAuth(originalText);
            }
        });
    }

    private void refreshAuth(Text originalText) {
        final String errText = "Failed to refresh token";

        try {
            CopeService.RefreshAuthRequest req = new CopeService.RefreshAuthRequest();
            req.refreshToken = CopeSystem.get().refreshToken;
            req.id = CopeSystem.get().discordId;

            Cope.getCopeService().refreshAuth(req, res -> {
                MeteorClient.mc.execute(() -> {
                    try {
                        if (res == null) {
                            this.errorMessage = errText;
                            retryButton.active = true;
                            retryButton.visible = true;
                        } else {
                            if (res.isError()) {
                                this.errorMessage = res.error;
                            } else {
                                CopeSystem.get().accessToken = res.accessToken;
                                CopeSystem.get().refreshToken = res.refreshToken;
                                CopeSystem.get().save();
                                checkAuth(originalText);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.errorMessage = errText;
                        retryButton.active = true;
                        retryButton.visible = true;
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            this.errorMessage = errText;
            retryButton.active = true;
            retryButton.visible = true;
        }

        this.loginButton.setMessage(originalText);
        this.loginButton.active = true;
        isChecking = false;
    }

    private void checkAuth(Text originalText) {
        final String errText = "Failed to verify auth";

        try {
            CopeService.CheckAuthResponse res = Cope.getCopeService().checkAuth();

            MeteorClient.mc.execute(() -> {
                try {
                    if (res == null) {
                        setErrorState(errText);
                    } else if (res.isError()) {
                        setErrorState(res.error);
                    } else if (!res.isValid()) {
                        this.client.scheduleStop();
                    } else {
                        this.client.setScreen(new TitleScreen(true));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    setErrorState(errText);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            setErrorState(errText);
        } finally {
            isChecking = false;
            this.loginButton.setMessage(originalText);
            this.loginButton.active = true;
        }
    }

    private void setErrorState(String errorMessage) {
        this.errorMessage = errorMessage;
        showRetryButton();
    }

    private void showRetryButton() {
        retryButton.active = true;
        retryButton.visible = true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
