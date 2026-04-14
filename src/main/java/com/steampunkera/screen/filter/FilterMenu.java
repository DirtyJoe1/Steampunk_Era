package com.steampunkera.screen.filter;

import com.steampunkera.util.ServoConfig;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import com.steampunkera.network.FilterPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;

public class FilterMenu extends ScreenHandler {
    private final BlockPos pos;
    private final Direction servoSide;
    private final int mouseX, mouseY;
    private boolean enabled;
    private ServoConfig.FilterMode filterMode;

    private final SimpleInventory filterInventory;
    private static final int FILTER_SLOTS = 18;

    public FilterMenu(int syncId, PlayerInventory playerInventory, BlockPos pos, Direction servoSide, boolean enabled, ServoConfig.FilterMode filterMode, java.util.List<Item> filterItems, int mouseX, int mouseY) {
        super(FilterMenuData.FILTER_MENU_TYPE, syncId);
        this.pos = pos;
        this.servoSide = servoSide;
        this.enabled = enabled;
        this.filterMode = filterMode;
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        // Инвентарь для хранения ghost-предметов фильтра
        this.filterInventory = new SimpleInventory(FILTER_SLOTS);

        // Загружаем предметы из ServoConfig
        if (filterItems != null) {
            for (int i = 0; i < Math.min(filterItems.size(), FILTER_SLOTS); i++) {
                Item item = filterItems.get(i);
                if (item != null) {
                    filterInventory.setStack(i, new ItemStack(item, 1));
                }
            }
        }

        // Слоты фильтра (2 строки по 9) — ghost слоты
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new FilterSlot(filterInventory, row * 9 + col, 8 + col * 18, 31 + row * 18));
            }
        }

        // Инвентарь игрока
        this.addPlayerInventorySlots(playerInventory, 8, 84);
        this.addPlayerHotbarSlots(playerInventory, 8, 142);
    }

    public FilterMenu(int syncId, PlayerInventory playerInventory, ItemPipeBlockEntity blockEntity, Direction servoSide) {
        this(syncId, playerInventory,
                blockEntity != null ? blockEntity.getPos() : BlockPos.ORIGIN,
                servoSide,
                true,
                blockEntity != null ? blockEntity.getServoConfig(servoSide).filterMode() : ServoConfig.FilterMode.BLACKLIST,
                blockEntity != null ? blockEntity.getServoConfig(servoSide).filterItems() : java.util.List.of(),
                0, 0);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        // Ghost слоты: при shift-click очищаем слот фильтра
        if (slotIndex < FILTER_SLOTS) {
            Slot slot = this.slots.get(slotIndex);
            if (slot != null && slot.hasStack()) {
                slot.setStack(ItemStack.EMPTY);
                syncFiltersToServer();
            }
            return ItemStack.EMPTY;
        }
        
        // Из инвентаря в фильтр: QuickStack
        Slot inventorySlot = this.slots.get(slotIndex);
        if (inventorySlot != null && inventorySlot.hasStack()) {
            ItemStack stack = inventorySlot.getStack();
            Item item = stack.getItem();
            
            // Ищем первый пустой слот фильтра без дубликата
            for (int i = 0; i < FILTER_SLOTS; i++) {
                Slot filterSlot = this.slots.get(i);
                if (!filterSlot.hasStack()) {
                    // Проверяем, что такого предмета ещё нет в фильтре
                    boolean duplicate = false;
                    for (int j = 0; j < FILTER_SLOTS; j++) {
                        if (j != i) {
                            Slot checkSlot = this.slots.get(j);
                            if (checkSlot.hasStack() && checkSlot.getStack().isOf(item)) {
                                duplicate = true;
                                break;
                            }
                        }
                    }
                    if (!duplicate) {
                        filterSlot.setStack(new ItemStack(item, 1));
                        syncFiltersToServer();
                        break;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Ghost слоты: перехватываем клик и копируем тип предмета без расхода
        if (slotIndex >= 0 && slotIndex < FILTER_SLOTS) {
            Slot filterSlot = this.slots.get(slotIndex);
            ItemStack cursorStack = player.currentScreenHandler.getCursorStack();

            if (actionType == SlotActionType.PICKUP) {
                if (button == 0) { // ЛКМ
                    if (cursorStack.isEmpty()) {
                        // Курсор пустой — удаляем предмет из слота
                        if (filterSlot.hasStack()) {
                            filterSlot.setStack(ItemStack.EMPTY);
                            syncFiltersToServer();
                        }
                    } else {
                        // Курсор с предметом, ставим ghost в слот
                        Item item = cursorStack.getItem();
                        // Проверка на дубликаты
                        if (!hasDuplicate(filterSlot, item)) {
                            filterSlot.setStack(new ItemStack(item, 1));
                            syncFiltersToServer();
                        }
                    }
                } else if (button == 1) { // ПКМ
                    if (cursorStack.isEmpty() && filterSlot.hasStack()) {
                        // ПКМ по слоту с ghost предметом — очищаем слот
                        filterSlot.setStack(ItemStack.EMPTY);
                        syncFiltersToServer();
                    } else if (!cursorStack.isEmpty()) {
                        // ПКМ с предметом на курсоре — ставим ghost
                        Item item = cursorStack.getItem();
                        if (!hasDuplicate(filterSlot, item)) {
                            filterSlot.setStack(new ItemStack(item, 1));
                            syncFiltersToServer();
                        }
                    }
                }
            } else if (actionType == SlotActionType.SWAP) {
                // Swap (клавиши 1-9) — ставим ghost предмета из хотбара
                if (button >= 0 && button < 9) {
                    ItemStack hotbarStack = player.getInventory().getStack(button);
                    if (!hotbarStack.isEmpty()) {
                        Item item = hotbarStack.getItem();
                        if (!hasDuplicate(filterSlot, item)) {
                            filterSlot.setStack(new ItemStack(item, 1));
                            syncFiltersToServer();
                        }
                    } else {
                        // Пустой слот хотбара — очищаем фильтр
                        if (filterSlot.hasStack()) {
                            filterSlot.setStack(ItemStack.EMPTY);
                            syncFiltersToServer();
                        }
                    }
                }
            } else if (actionType == SlotActionType.QUICK_MOVE) {
                // Shift-click — очищаем слот
                if (filterSlot.hasStack()) {
                    filterSlot.setStack(ItemStack.EMPTY);
                    syncFiltersToServer();
                }
            }
            // Не вызываем super — предотвращаем расход предметов
            return;
        }

        super.onSlotClick(slotIndex, button, actionType, player);
    }

    private boolean hasDuplicate(Slot exceptSlot, Item item) {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            Slot slot = this.slots.get(i);
            if (slot != exceptSlot && slot.hasStack() && slot.getStack().isOf(item)) {
                return true;
            }
        }
        return false;
    }

    private void syncFiltersToServer() {
        java.util.List<Item> items = new ArrayList<>();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack stack = filterInventory.getStack(i);
            if (!stack.isEmpty()) {
                items.add(stack.getItem());
            }
        }
        // На клиенте отправляем пакет на сервер
        ClientPlayNetworking.send(new FilterPayload.UpdateFilterItems(pos, servoSide, items));
    }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    public BlockPos getPos() { return pos; }
    public Direction getServoSide() { return servoSide; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public ServoConfig.FilterMode getFilterMode() { return filterMode; }
    public void setFilterMode(ServoConfig.FilterMode filterMode) { this.filterMode = filterMode; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
    public SimpleInventory getFilterInventory() { return filterInventory; }

    /**
     * Ghost слот — отображает предмет, но не расходует его из инвентаря игрока
     */
    private class FilterSlot extends Slot {
        public FilterSlot(net.minecraft.inventory.Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false; // Ghost слот — не принимает предметы из инвентаря
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false; // Нельзя забрать ghost предмет
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }
    }
}
