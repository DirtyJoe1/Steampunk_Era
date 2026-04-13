package com.steampunkera.screen;

import com.steampunkera.ServoConfig;
import com.steampunkera.network.ServoPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ServoScreen extends HandledScreen<ServoMenu> {

    private static final Identifier TEXTURE = Identifier.of("steampunk-era", "textures/gui/servo_gui.png");
    private static final int TEXTURE_RESOLUTION = 256;
    private static final Identifier TORCH_ON = Identifier.of("steampunk-era", "textures/gui/redstone_torch_lit.png");
    private static final Identifier TORCH_OFF = Identifier.of("steampunk-era", "textures/gui/redstone_torch_unlit.png");
    private static final Identifier ICON_ROUND_ROBIN = Identifier.of("steampunk-era", "textures/gui/round_robin.png");
    private static final Identifier ICON_NEAREST_FIRST = Identifier.of("steampunk-era", "textures/gui/nearest_first.png");
    private static final Identifier ICON_FURTHEST_FIRST = Identifier.of("steampunk-era", "textures/gui/furthest_first.png");

    private boolean enabled;
    private ServoConfig currentConfig;

    private ButtonWidget toggleButton;
    private ButtonWidget filterModeButton;
    private ButtonWidget routingModeButton;
    private ServoSlider intervalSlider;
    private ServoSlider maxSlider;

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

        // Ряд 2: [Иконка] [Route: MODE]
        routingModeButton = ButtonWidget.builder(
                Text.literal(currentConfig.routingMode().name()),
                btn -> cycleRoutingMode()
        ).dimensions(x + 26, y + 26, 146, 14).build();
        this.addDrawableChild(routingModeButton);

        // Ряд 3: Interval slider
        intervalSlider = new ServoSlider(x + 8, y + 44, 160, 14,
                "Interval", 5, 120, currentConfig.extractInterval(),
                val -> {
                    currentConfig = currentConfig.withExtractInterval(val);
                    sendSettings();
                });
        this.addDrawableChild(intervalSlider);

        // Ряд 4: Max slider
        maxSlider = new ServoSlider(x + 8, y + 62, 160, 14,
                "Max", 1, 64, currentConfig.maxExtract(),
                val -> {
                    currentConfig = currentConfig.withMaxExtract(val);
                    sendSettings();
                });
        this.addDrawableChild(maxSlider);
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
        if (routingModeButton != null) routingModeButton.setMessage(Text.literal(currentConfig.routingMode().name()));
        sendSettings();
    }

    private void sendSettings() {
        ClientPlayNetworking.send(ServoPayload.ServoSettings.fromConfig(
                handler.getPos(), handler.getServoSide(), enabled, currentConfig));
    }

    public void updateConfig(ServoConfig config) {
        this.currentConfig = config;
        if (filterModeButton != null) filterModeButton.setMessage(Text.literal("Filter: " + config.filterMode().name()));
        if (routingModeButton != null) routingModeButton.setMessage(Text.literal(config.routingMode().name()));
        if (intervalSlider != null) {
            double val = (config.extractInterval() - 5.0) / (120.0 - 5.0);
            intervalSlider.setSliderValue(val);
        }
        if (maxSlider != null) {
            double val = (config.maxExtract() - 1.0) / (64.0 - 1.0);
            maxSlider.setSliderValue(val);
        }
    }

    public void updateButton(boolean enabled) {
        if (toggleButton != null) toggleButton.setMessage(Text.literal(enabled ? "ON" : "OFF"));
    }

    private Identifier getRoutingModeIcon(ServoConfig.RoutingMode mode) {
        return switch (mode) {
            case ROUND_ROBIN -> ICON_ROUND_ROBIN;
            case NEAREST_FIRST -> ICON_NEAREST_FIRST;
            case FURTHEST_FIRST -> ICON_FURTHEST_FIRST;
        };
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, this.backgroundWidth, this.backgroundHeight, TEXTURE_RESOLUTION, TEXTURE_RESOLUTION);

        // Иконка факела слева от ON/OFF
        Identifier torch = enabled ? TORCH_ON : TORCH_OFF;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, torch, x + 8, y + 8, 0, 0, 16, 16, 16, 16);

        // Иконка режима маршрутизации слева от кнопки Route
        Identifier routingIcon = getRoutingModeIcon(currentConfig.routingMode());
        context.drawTexture(RenderPipelines.GUI_TEXTURED, routingIcon, x + 8, y + 26, 0, 0, 16, 16, 16, 16);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public void close() {
        super.close();
    }

    private static class ServoSlider extends SliderWidget {
        private final java.util.function.IntConsumer onChanged;
        private final int min, max;
        private final String label;

        ServoSlider(int x, int y, int width, int height, String label, int min, int max, int initial, java.util.function.IntConsumer onChanged) {
            super(x, y, width, height, Text.literal(label + ": " + initial), (double)(initial - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChanged = onChanged;
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
            double delta = 1.0 / (max - min);
            int oldVal = clampValue();
            if (input.key() == net.minecraft.client.util.InputUtil.GLFW_KEY_LEFT) {
                this.value = Math.max(0.0, this.value - delta);
            } else if (input.key() == net.minecraft.client.util.InputUtil.GLFW_KEY_RIGHT) {
                this.value = Math.min(1.0, this.value + delta);
            } else {
                return super.keyPressed(input);
            }
            int newVal = clampValue();
            if (newVal != oldVal) {
                applyValue();
            }
            updateMessage();
            return true;
        }

        @Override
        protected void applyValue() {
            int val = clampValue();
            onChanged.accept(val);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(label + ": " + clampValue()));
        }

        private int clampValue() {
            return Math.clamp((int) Math.round(this.value * (max - min) + min), min, max);
        }

        void setSliderValue(double val) {
            this.value = val;
            updateMessage();
        }
    }
}
