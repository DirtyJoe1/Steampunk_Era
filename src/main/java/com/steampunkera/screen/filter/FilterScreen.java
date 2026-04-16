package com.steampunkera.screen.filter;

import com.steampunkera.util.FilterUtil;
import com.steampunkera.network.FilterConfigPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;

public class FilterScreen extends HandledScreen<FilterMenu> {

    // Текстуры
    private static final Identifier TEXTURE = Identifier.of("steampunk-era", "textures/gui/filter_gui.png");
    private static final int TEXTURE_RESOLUTION = 256;
    private static final Identifier WHITELIST = Identifier.of("steampunk-era", "textures/gui/whitelist.png");
    private static final Identifier BLACKLIST = Identifier.of("steampunk-era", "textures/gui/blacklist.png");
    
    // Размеры GUI
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final int INVENTORY_TITLE_Y = 72;
    
    // Размеры кнопок
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_CLOSE_WIDTH = 46;
    private static final int BUTTON_CLEAR_WIDTH = 20;
    
    // Позиции элементов
    private static final int BUTTON_CLOSE_X = 8;
    private static final int BUTTON_MODE_X = 62;
    private static final int BUTTON_CLEAR_X = 130;
    private static final int ROW_Y = 15;
    private static final int ICON_X = 44;
    private static final int ICON_SIZE = 16;

    private FilterUtil.FilterMode filterMode;

    private ButtonWidget filterModeButton;
    private ButtonWidget closeButton;
    private ButtonWidget clearButton;

    public FilterScreen(FilterMenu menu, PlayerInventory inventory, Text title) {
        super(menu, inventory, title);
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
        this.playerInventoryTitleY = INVENTORY_TITLE_Y;
        this.filterMode = menu.getFilterMode();
    }

    public void updateFilterMode(FilterUtil.FilterMode filterMode) {
        this.filterMode = filterMode;
        if (filterModeButton != null) {
            Text newText = Text.literal(filterMode.name());
            filterModeButton.setMessage(newText);
            filterModeButton.setWidth(this.textRenderer.getWidth(newText) + 8);
        }
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        
        // Не устанавливаем позицию курсора - это вызывает скачок
        // Курсор остаётся там, где был
        
        // Кнопка "Close" — слева сверху (закрывает экран без возврата к сервоприводу)
        closeButton = ButtonWidget.builder(
                Text.literal("Close"),
                btn -> closeScreen()
        ).dimensions(x + BUTTON_CLOSE_X, y + ROW_Y, BUTTON_CLOSE_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(closeButton);

        // Иконка режима — сразу после кнопки
        // Рисуется в drawBackground() на x + ICON_X
        
        // Кнопка режима — справа от иконки
        int filterButtonWidth = this.textRenderer.getWidth(Text.literal(filterMode.name())) + 8;
        filterModeButton = ButtonWidget.builder(
                Text.literal(filterMode.name()),
                btn -> cycleFilterMode()
        ).dimensions(x + BUTTON_MODE_X, y + ROW_Y, filterButtonWidth, BUTTON_HEIGHT).build();
        this.addDrawableChild(filterModeButton);
        
        // Кнопка "X" — очистить фильтр
        clearButton = ButtonWidget.builder(
                Text.literal("X"),
                btn -> clearFilter()
        ).dimensions(x + BUTTON_CLEAR_X, y + ROW_Y, BUTTON_CLEAR_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(clearButton);
    }

    private void cycleFilterMode() {
        filterMode = filterMode == FilterUtil.FilterMode.BLACKLIST ? FilterUtil.FilterMode.WHITELIST : FilterUtil.FilterMode.BLACKLIST;
        handler.setFilterMode(filterMode);

        Text newText = Text.literal(filterMode.name());
        filterModeButton.setMessage(newText);
        filterModeButton.setWidth(this.textRenderer.getWidth(newText) + 8);
        
        // Отправляем обновление на сервер
        sendFilterUpdate();
    }
    
    private void clearFilter() {
        // Очищаем все слоты фильтра
        for (int i = 0; i < 18; i++) {
            handler.getFilterInventory().setStack(i, ItemStack.EMPTY);
        }
        
        // Отправляем пустой список на сервер
        ClientPlayNetworking.send(new FilterConfigPayload.UpdateFilterConfig(
                handler.getPos(), handler.getServoSide(), filterMode, new ArrayList<>()));
    }

    private void closeScreen() {
        // Собираем актуальные filterItems из инвентаря
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            ItemStack stack = handler.getFilterInventory().getStack(i);
            if (!stack.isEmpty()) {
                items.add(stack.getItem());
            }
        }
        
        // Отправляем закрытие с сохранением на сервер
        ClientPlayNetworking.send(new FilterConfigPayload.CloseFilterScreen(
                handler.getPos(), handler.getServoSide(), filterMode, items));
        
        // Закрываем экран
        this.client.player.closeHandledScreen();
    }

    private void sendFilterUpdate() {
        // Собираем актуальные filterItems
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            ItemStack stack = handler.getFilterInventory().getStack(i);
            if (!stack.isEmpty()) {
                items.add(stack.getItem());
            }
        }
        
        ClientPlayNetworking.send(new FilterConfigPayload.UpdateFilterConfig(
                handler.getPos(), handler.getServoSide(), filterMode, items));
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, this.backgroundWidth, this.backgroundHeight, TEXTURE_RESOLUTION, TEXTURE_RESOLUTION);

        Identifier filterIcon = filterMode == FilterUtil.FilterMode.WHITELIST ? WHITELIST : BLACKLIST;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, filterIcon, x + ICON_X, y + ROW_Y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slotIndex = row * 9 + col;
                ItemStack stack = handler.getFilterInventory().getStack(slotIndex);
                if (!stack.isEmpty()) {
                    int slotX = x + 8 + col * 18;
                    int slotY = y + 31 + row * 18;
                    context.drawItem(stack, slotX, slotY);
                    context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x99888888);
                }
            }
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);

        List<OrderedText> tooltipLines = null;
        int iconX = (this.width - this.backgroundWidth) / 2 + ICON_X;
        int iconY = (this.height - this.backgroundHeight) / 2 + ROW_Y;
        if (mouseX >= iconX && mouseX < iconX + ICON_SIZE && mouseY >= iconY && mouseY < iconY + ICON_SIZE) {
            tooltipLines = this.textRenderer.wrapLines(Text.literal("Current mode: " + filterMode.name()), 150);
        } else if (filterModeButton != null && filterModeButton.isMouseOver(mouseX, mouseY)) {
            tooltipLines = this.textRenderer.wrapLines(Text.literal("Toggle between Whitelist and Blacklist mode"), 150);
        } else if (closeButton != null && closeButton.isMouseOver(mouseX, mouseY)) {
            tooltipLines = this.textRenderer.wrapLines(Text.literal("Close and save filter settings"), 150);
        } else if (clearButton != null && clearButton.isMouseOver(mouseX, mouseY)) {
            tooltipLines = this.textRenderer.wrapLines(Text.literal("Clear all filter items"), 150);
        }
        if (tooltipLines != null && !tooltipLines.isEmpty()) {
            context.drawOrderedTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
        }
    }
}