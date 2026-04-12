package com.steampunkera.block.entity;

import com.steampunkera.SteampunkEra;
import com.steampunkera.SteampunkEraAttachments;
import com.steampunkera.SteampunkEraAttachments.PipeData;
import com.steampunkera.block.ItemPipeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.function.UnaryOperator;

public class ItemPipeBlockEntity extends BlockEntity {

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(SteampunkEra.ITEM_PIPE_BLOCK_ENTITY_TYPE, pos, state);
    }

    private PipeData getData() {
        return getAttachedOrCreate(SteampunkEraAttachments.PIPE_DATA);
    }

    private void setData(PipeData data) {
        setAttached(SteampunkEraAttachments.PIPE_DATA, data);
    }

    private void modifyData(UnaryOperator<PipeData> modifier) {
        setData(modifier.apply(getData()));
        markDirty();
    }

    public boolean isDisabled(Direction direction) {
        return getData().isDisabled(direction);
    }

    public void toggleDisabled(Direction direction) {
        modifyData(data -> data.withDisabled(direction, !data.isDisabled(direction)));
        updateBlockState();
    }

    public boolean hasServo(Direction direction) {
        return getData().hasServo(direction);
    }

    public void setServo(Direction direction, boolean hasServo) {
        modifyData(data -> data.withServo(direction, hasServo));
        updateServoState();
    }

    private void updateServoState() {
        if (world != null && !world.isClient() && world.getBlockEntity(pos) == this) {
            if (getCachedState().getBlock() instanceof ItemPipeBlock pipeBlock) {
                pipeBlock.updateServoState(world, pos);
            }
        }
    }

    public boolean isConnected(Direction direction) {
        if (world == null) return false;
        BlockState state = getCachedState();
        if (!(state.getBlock() instanceof ItemPipeBlock pipeBlock)) return false;
        return pipeBlock.hasConnection(state, direction) && !isDisabled(direction);
    }

    private void updateBlockState() {
        if (world != null && !world.isClient()) {
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, 3);
        }
    }
}
