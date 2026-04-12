package com.steampunkera.screen;

import com.steampunkera.ServoMenuData;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class ServoMenu extends ScreenHandler {
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private final BlockPos pos;
    private boolean enabled;

    public ServoMenu(int syncId, PlayerInventory playerInventory, BlockPos pos, boolean enabled) {
        super(ServoMenuData.SERVO_MENU_TYPE, syncId);
        this.pos = pos;
        this.enabled = enabled;
        this.addPlayerInventorySlots(playerInventory, INVENTORY_X, INVENTORY_Y);
        this.addPlayerHotbarSlots(playerInventory, INVENTORY_X, HOTBAR_Y);
    }

    public ServoMenu(int syncId, PlayerInventory playerInventory, ItemPipeBlockEntity blockEntity, boolean enabled) {
        this(syncId, playerInventory, blockEntity != null ? blockEntity.getPos() : BlockPos.ORIGIN, enabled);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public BlockPos getPos() { return pos; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
