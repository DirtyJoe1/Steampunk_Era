package com.steampunkera.screen.filter;

import com.steampunkera.util.ServoConfig;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
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

        // Инвентарь для хранения предметов фильтра
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

        // Слоты фильтра (2 строки по 9)
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
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex < FILTER_SLOTS) {
                // Из слота фильтра в инвентарь игрока
                if (!this.insertItem(originalStack, FILTER_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Из инвентаря игрока в слот фильтра
                if (!this.insertItem(originalStack, 0, FILTER_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (player.getEntityWorld().getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            ServoConfig config = pipe.getServoConfig(servoSide);
            java.util.List<Item> filterItems = new ArrayList<>();
            for (int i = 0; i < FILTER_SLOTS; i++) {
                ItemStack stack = filterInventory.getStack(i);
                if (!stack.isEmpty()) {
                    filterItems.add(stack.getItem());
                }
            }
            config = config.withFilterMode(filterMode).withFilterItems(filterItems);
            pipe.setServoConfig(servoSide, config);
        }
    }

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
     * Кастомный слот, ограничивающий вставку по 1 предмету и запрещающий дубликаты (как фильтр в EnderIO)
     */
    private class FilterSlot extends Slot {
        public FilterSlot(net.minecraft.inventory.Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }

        @Override
        public int getMaxItemCount(ItemStack stack) {
            return 1;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                if (i != this.id && filterInventory.getStack(i).isOf(stack.getItem())) {
                    return false;
                }
            }
            return true;
        }
    }
}
