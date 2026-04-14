package com.steampunkera.screen.servo;

import com.steampunkera.util.ServoConfig;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ServoMenu extends ScreenHandler {
    private final BlockPos pos;
    private final Direction servoSide;
    private final int mouseX, mouseY;
    private boolean enabled;
    private ServoConfig config;

    public ServoMenu(int syncId, PlayerInventory playerInventory, BlockPos pos, Direction servoSide, boolean enabled, ServoConfig config, int mouseX, int mouseY) {
        super(ServoMenuData.SERVO_MENU_TYPE, syncId);
        this.pos = pos;
        this.servoSide = servoSide;
        this.enabled = enabled;
        this.config = config;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.addPlayerInventorySlots(playerInventory, 8, 84);
        this.addPlayerHotbarSlots(playerInventory, 8, 142);
    }

    public ServoMenu(int syncId, PlayerInventory playerInventory, ItemPipeBlockEntity blockEntity, Direction servoSide, boolean enabled) {
        this(syncId, playerInventory, blockEntity != null ? blockEntity.getPos() : BlockPos.ORIGIN, servoSide, enabled, blockEntity != null ? blockEntity.getServoConfig(servoSide) : ServoConfig.DEFAULT, 0, 0);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) { return ItemStack.EMPTY; }
    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    public BlockPos getPos() { return pos; }
    public Direction getServoSide() { return servoSide; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public ServoConfig getConfig() { return config; }
    public void setConfig(ServoConfig config) { this.config = config; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
}
