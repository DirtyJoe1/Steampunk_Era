package com.steampunkera.screen.filter;

import com.steampunkera.util.ServoConfig;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FilterMenu extends ScreenHandler {
    private final BlockPos pos;
    private final Direction servoSide;
    private final int mouseX, mouseY;
    private boolean enabled;
    private ServoConfig.FilterMode filterMode;

    public FilterMenu(int syncId, PlayerInventory playerInventory, BlockPos pos, Direction servoSide, boolean enabled, ServoConfig.FilterMode filterMode, int mouseX, int mouseY) {
        super(FilterMenuData.FILTER_MENU_TYPE, syncId);
        this.pos = pos;
        this.servoSide = servoSide;
        this.enabled = enabled;
        this.filterMode = filterMode;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.addPlayerInventorySlots(playerInventory, 8, 84);
        this.addPlayerHotbarSlots(playerInventory, 8, 142);
    }

    public FilterMenu(int syncId, PlayerInventory playerInventory, ItemPipeBlockEntity blockEntity, Direction servoSide) {
        this(syncId, playerInventory, blockEntity != null ? blockEntity.getPos() : BlockPos.ORIGIN, servoSide,
                true, blockEntity != null ? blockEntity.getServoConfig(servoSide).filterMode() : ServoConfig.FilterMode.BLACKLIST, 0, 0);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) { return ItemStack.EMPTY; }
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
}
