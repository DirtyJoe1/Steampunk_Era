package com.steampunkera.screen;

import com.steampunkera.ServoMenuData;
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

    private boolean enabled;

    public ServoScreen(ServoMenu menu, PlayerInventory inventory, Text title) {
        super(menu, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
        this.enabled = menu.isEnabled();
    }

    @Override
    protected void init() {
        super.init();

        int btnX = (this.width - this.backgroundWidth) / 2 + 8;
        int btnY = (this.height - this.backgroundHeight) / 2 + 16;

        ButtonWidget toggleButton = ButtonWidget.builder(
                Text.literal(enabled ? "ON" : "OFF"),
                btn -> {
                    enabled = !enabled;
                    handler.setEnabled(enabled);
                    btn.setMessage(Text.literal(enabled ? "ON" : "OFF"));
                    BlockPos pos = this.handler.getPos();
                    ClientPlayNetworking.send(new ServoTogglePayload(pos));
                }
        ).dimensions(btnX, btnY, 40, 16).build();

        this.addDrawableChild(toggleButton);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, this.backgroundWidth, this.backgroundHeight, 256, 256);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
