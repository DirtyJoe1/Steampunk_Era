package com.steampunkera.screen;

import com.steampunkera.network.ServoTogglePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ServoScreen extends HandledScreen<ServoMenu> {

    private static final Identifier TEXTURE = Identifier.of("steampunk-era", "textures/gui/servo_gui.png");
    private static final int TEXTURE_RESOLUTION = 256;
    private static final int BUTTON_X_OFFSET = 8;
    private static final int BUTTON_Y_OFFSET = 16;
    private static final int BUTTON_WIDTH = 40;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BACKGROUND_WIDTH = 176;
    private static final int BACKGROUND_HEIGHT = 166;
    private static final int PLAYER_INVENTORY_TITLE_Y_OFFSET = 94;

    private boolean enabled;
    private ButtonWidget toggleButton;

    public ServoScreen(ServoMenu menu, PlayerInventory inventory, Text title) {
        super(menu, inventory, title);
        this.backgroundWidth = BACKGROUND_WIDTH;
        this.backgroundHeight = BACKGROUND_HEIGHT;
        this.playerInventoryTitleY = BACKGROUND_HEIGHT - PLAYER_INVENTORY_TITLE_Y_OFFSET;
        this.enabled = menu.isEnabled();
    }

    @Override
    protected void init() {
        super.init();

        int btnX = (this.width - this.backgroundWidth) / 2 + BUTTON_X_OFFSET;
        int btnY = (this.height - this.backgroundHeight) / 2 + BUTTON_Y_OFFSET;

        toggleButton = ButtonWidget.builder(
                Text.literal(enabled ? "ON" : "OFF"),
                btn -> toggleServo()
        ).dimensions(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        this.addDrawableChild(toggleButton);
    }

    private void toggleServo() {
        enabled = !enabled;
        handler.setEnabled(enabled);
        toggleButton.setMessage(Text.literal(enabled ? "ON" : "OFF"));
        ClientPlayNetworking.send(new ServoTogglePayload(handler.getPos(), enabled));
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, this.backgroundWidth, this.backgroundHeight, TEXTURE_RESOLUTION, TEXTURE_RESOLUTION);
    }

    public void updateButton(boolean enabled) {
        if (toggleButton != null) {
            toggleButton.setMessage(Text.literal(enabled ? "ON" : "OFF"));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
