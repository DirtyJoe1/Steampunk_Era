package com.steampunkera.screen;

import com.steampunkera.ServoMenuData;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class ServoMenu extends ScreenHandler {

    private final BlockPos pos;
    private boolean enabled;

    public ServoMenu(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, null, true);
    }

    public ServoMenu(int syncId, PlayerInventory playerInventory, @Nullable ItemPipeBlockEntity blockEntity, boolean enabled) {
        super(ServoMenuData.SERVO_MENU_TYPE, syncId);
        this.pos = blockEntity != null ? blockEntity.getPos() : BlockPos.ORIGIN;
        this.enabled = enabled;

        this.addPlayerInventorySlots(playerInventory, 8, 84);
        this.addPlayerHotbarSlots(playerInventory, 8, 142);
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
