package com.steampunkera.screen;

import com.steampunkera.ServoConfig;
import com.steampunkera.network.ServoPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ServoScreen extends HandledScreen<ServoMenu> {

    private static final Identifier TEXTURE = Identifier.of("steampunk-era", "textures/gui/servo_gui.png");
    private static final int TEXTURE_RESOLUTION = 256;
    private static final Identifier TORCH_ON = Identifier.of("steampunk-era", "textures/gui/redstone_torch_lit.png");
    private static final Identifier TORCH_OFF = Identifier.of("steampunk-era", "textures/gui/redstone_torch_unlit.png");

    private boolean enabled;
    private ServoConfig currentConfig;

    private ButtonWidget toggleButton;
    private ButtonWidget filterModeButton;
    private ButtonWidget routingModeButton;
    private ButtonWidget intervalLabelButton;
    private ButtonWidget intervalDownButton;
    private ButtonWidget intervalUpButton;
    private ButtonWidget maxLabelButton;
    private ButtonWidget maxDownButton;
    private ButtonWidget maxUpButton;

    public ServoScreen(ServoMenu menu, PlayerInventory inventory, Text title) {
        super(menu, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = 72;
        this.enabled = menu.isEnabled();
        this.currentConfig = menu.getConfig() != null ? menu.getConfig() : ServoConfig.DEFAULT;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Ряд 1: [Факел] [ON/OFF] [Filter: MODE]
        toggleButton = ButtonWidget.builder(
                Text.literal(enabled ? "ON" : "OFF"),
                btn -> toggleEnabled()
        ).dimensions(x + 28, y + 8, 32, 14).build();
        this.addDrawableChild(toggleButton);

        filterModeButton = ButtonWidget.builder(
                Text.literal("Filter: " + currentConfig.filterMode().name()),
                btn -> cycleFilterMode()
        ).dimensions(x + 64, y + 8, 90, 14).build();
        this.addDrawableChild(filterModeButton);

        // Ряд 2: [Route: MODE]
        routingModeButton = ButtonWidget.builder(
                Text.literal("Route: " + currentConfig.routingMode().name()),
                btn -> cycleRoutingMode()
        ).dimensions(x + 8, y + 26, 146, 14).build();
        this.addDrawableChild(routingModeButton);

        // Ряд 3: [-] Interval: 60 [+]
        intervalDownButton = ButtonWidget.builder(
                Text.literal("-"),
                btn -> changeInterval(-5)
        ).dimensions(x + 8, y + 44, 16, 14).build();
        this.addDrawableChild(intervalDownButton);

        intervalLabelButton = ButtonWidget.builder(
                Text.literal("Interval: " + currentConfig.extractInterval()),
                btn -> {}
        ).dimensions(x + 26, y + 44, 100, 14).build();
        this.addDrawableChild(intervalLabelButton);

        intervalUpButton = ButtonWidget.builder(
                Text.literal("+"),
                btn -> changeInterval(5)
        ).dimensions(x + 128, y + 44, 16, 14).build();
        this.addDrawableChild(intervalUpButton);

        // Ряд 4: [-] Max: 8 [+]
        maxDownButton = ButtonWidget.builder(
                Text.literal("-"),
                btn -> changeMaxExtract(-1)
        ).dimensions(x + 8, y + 62, 16, 14).build();
        this.addDrawableChild(maxDownButton);

        maxLabelButton = ButtonWidget.builder(
                Text.literal("Max: " + currentConfig.maxExtract()),
                btn -> {}
        ).dimensions(x + 26, y + 62, 100, 14).build();
        this.addDrawableChild(maxLabelButton);

        maxUpButton = ButtonWidget.builder(
                Text.literal("+"),
                btn -> changeMaxExtract(1)
        ).dimensions(x + 128, y + 62, 16, 14).build();
        this.addDrawableChild(maxUpButton);
    }

    private void toggleEnabled() {
        enabled = !enabled;
        handler.setEnabled(enabled);
        currentConfig = currentConfig.withEnabled(enabled);
        if (toggleButton != null) toggleButton.setMessage(Text.literal(enabled ? "ON" : "OFF"));
        sendSettings();
    }

    private void cycleFilterMode() {
        ServoConfig.FilterMode[] modes = ServoConfig.FilterMode.values();
        int idx = (currentConfig.filterMode().ordinal() + 1) % modes.length;
        currentConfig = currentConfig.withFilterMode(modes[idx]);
        if (filterModeButton != null) filterModeButton.setMessage(Text.literal("Filter: " + currentConfig.filterMode().name()));
        sendSettings();
    }

    private void cycleRoutingMode() {
        ServoConfig.RoutingMode[] modes = ServoConfig.RoutingMode.values();
        int idx = (currentConfig.routingMode().ordinal() + 1) % modes.length;
        currentConfig = currentConfig.withRoutingMode(modes[idx]);
        if (routingModeButton != null) routingModeButton.setMessage(Text.literal("Route: " + currentConfig.routingMode().name()));
        sendSettings();
    }

    private void changeInterval(int delta) {
        int newVal = Math.max(5, Math.min(120, currentConfig.extractInterval() + delta));
        currentConfig = currentConfig.withExtractInterval(newVal);
        if (intervalLabelButton != null) intervalLabelButton.setMessage(Text.literal("Interval: " + newVal));
        sendSettings();
    }

    private void changeMaxExtract(int delta) {
        int newVal = Math.max(1, Math.min(64, currentConfig.maxExtract() + delta));
        currentConfig = currentConfig.withMaxExtract(newVal);
        if (maxLabelButton != null) maxLabelButton.setMessage(Text.literal("Max: " + newVal));
        sendSettings();
    }

    private void sendSettings() {
        ClientPlayNetworking.send(ServoPayload.ServoSettings.fromConfig(
                handler.getPos(), handler.getServoSide(), enabled, currentConfig));
    }

    public void updateConfig(ServoConfig config) {
        this.currentConfig = config;
        if (filterModeButton != null) filterModeButton.setMessage(Text.literal("Filter: " + config.filterMode().name()));
        if (routingModeButton != null) routingModeButton.setMessage(Text.literal("Route: " + config.routingMode().name()));
        if (intervalLabelButton != null) intervalLabelButton.setMessage(Text.literal("Interval: " + config.extractInterval()));
        if (maxLabelButton != null) maxLabelButton.setMessage(Text.literal("Max: " + config.maxExtract()));
    }

    public void updateButton(boolean enabled) {
        if (toggleButton != null) toggleButton.setMessage(Text.literal(enabled ? "ON" : "OFF"));
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, this.backgroundWidth, this.backgroundHeight, TEXTURE_RESOLUTION, TEXTURE_RESOLUTION);

        // Иконка факела слева от ON/OFF
        Identifier torch = enabled ? TORCH_ON : TORCH_OFF;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, torch, x + 8, y + 8, 0, 0, 16, 16, 16, 16);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public void close() {
        sendSettings();
        super.close();
    }
}
