package com.steampunkera.screen.filter;

import com.steampunkera.util.ServoConfig;
import com.steampunkera.network.FilterPayload;
import com.steampunkera.network.ServoPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;

public class FilterScreen extends HandledScreen<FilterMenu> {

    private static final Identifier TEXTURE = Identifier.of("steampunk-era", "textures/gui/filter_gui.png");
    private static final int TEXTURE_RESOLUTION = 256;
    private static final Identifier WHITELIST = Identifier.of("steampunk-era", "textures/gui/whitelist.png");
    private static final Identifier BLACKLIST = Identifier.of("steampunk-era", "textures/gui/blacklist.png");

    private ServoConfig.FilterMode filterMode;

    private ButtonWidget filterModeButton;
    private ButtonWidget backButton;

    public FilterScreen(FilterMenu menu, PlayerInventory inventory, Text title) {
        super(menu, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = 72;
        this.filterMode = menu.getFilterMode();
    }

    public void updateFilterMode(ServoConfig.FilterMode filterMode) {
        this.filterMode = filterMode;
        if (filterModeButton != null) {
            Text newText = Text.literal("Mode: " + filterMode.name());
            filterModeButton.setMessage(newText);
            filterModeButton.setWidth(this.textRenderer.getWidth(newText) + 8);
        }
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        glfwSetCursorPos(this.client.getWindow().getHandle(), handler.getMouseX(), handler.getMouseY());
        // Кнопка "Back" — слева сверху
        backButton = ButtonWidget.builder(
                Text.literal("Back"),
                btn -> goBack()
        ).dimensions(x + 8, y + 15, 46, 14).build();
        this.addDrawableChild(backButton);

        // Кнопка "Mode" — справа от иконки фильтра
        int filterButtonWidth = this.textRenderer.getWidth(Text.literal("Mode: " + filterMode.name())) + 8;
        filterModeButton = ButtonWidget.builder(
                Text.literal("Mode: " + filterMode.name()),
                btn -> cycleFilterMode()
        ).dimensions(x + 74, y + 15, filterButtonWidth, 14).build();
        this.addDrawableChild(filterModeButton);
    }

    private void cycleFilterMode() {
        filterMode = filterMode == ServoConfig.FilterMode.BLACKLIST ? ServoConfig.FilterMode.WHITELIST : ServoConfig.FilterMode.BLACKLIST;
        handler.setFilterMode(filterMode);

        Text newText = Text.literal("Mode: " + filterMode.name());
        filterModeButton.setMessage(newText);
        filterModeButton.setWidth(this.textRenderer.getWidth(newText) + 8);
        sendSettings();
    }

    private void goBack() {
        ClientPlayNetworking.send(new FilterPayload.BackToServo(
                handler.getPos(), handler.getServoSide(), handler.isEnabled(), (int) this.client.mouse.getX(), (int) this.client.mouse.getY()));
    }

    private void sendSettings() {
        ClientPlayNetworking.send(new ServoPayload.UpdateFilterMode(
                handler.getPos(), handler.getServoSide(), filterMode));
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, this.backgroundWidth, this.backgroundHeight, TEXTURE_RESOLUTION, TEXTURE_RESOLUTION);

        Identifier filterIcon = filterMode == ServoConfig.FilterMode.WHITELIST ? WHITELIST : BLACKLIST;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, filterIcon, x + 56, y + 15, 0, 0, 16, 16, 16, 16);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // Подсказка для кнопок
        List<OrderedText> tooltipLines = null;
        if (filterModeButton != null && filterModeButton.isMouseOver(mouseX, mouseY)) {
            tooltipLines = this.textRenderer.wrapLines(Text.literal("Toggle between Whitelist and Blacklist mode"), 150);
        } else if (backButton != null && backButton.isMouseOver(mouseX, mouseY)) {
            tooltipLines = this.textRenderer.wrapLines(Text.literal("Return to servo settings"), 150);
        }
        if (tooltipLines != null && !tooltipLines.isEmpty()) {
            context.drawOrderedTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
        }
    }
}
