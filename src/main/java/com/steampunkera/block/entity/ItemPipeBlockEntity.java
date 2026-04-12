package com.steampunkera.block.entity;

import com.steampunkera.SteampunkEra;
import com.steampunkera.SteampunkEraAttachments;
import com.steampunkera.SteampunkEraAttachments.PipeData;
import com.steampunkera.block.ItemPipeBlock;
import com.steampunkera.pipe.ItemPipeNetwork;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.function.UnaryOperator;

public class ItemPipeBlockEntity extends BlockEntity {

    private static final int SERVO_TICK_INTERVAL = 60; // Извлечение каждые 3 секунды (60 тиков)
    private int tickCounter = 0;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(SteampunkEra.ITEM_PIPE_BLOCK_ENTITY_TYPE, pos, state);
        SteampunkEra.TICKING_PIPES.add(this);
    }

    @Override
    public void markRemoved() {
        SteampunkEra.TICKING_PIPES.remove(this);
        super.markRemoved();
    }

    /**
     * Вызывается каждый тик из ServerTickEvents.
     */
    public void tick(World world) {
        if (world.isClient()) return;

        tickCounter++;
        if (tickCounter >= SERVO_TICK_INTERVAL) {
            tickCounter = 0;
            processServos(world);
        }
    }

    /**
     * Обрабатывает все активные сервоприводы на этой трубе.
     */
    private void processServos(World world) {
        PipeData data = getData();
        BlockPos pos = getPos();

        for (Direction dir : Direction.values()) {
            if (!data.hasServo(dir)) continue;
            if (!data.isServoActive(dir)) continue;

            BlockPos neighborPos = pos.offset(dir);
            Storage<ItemVariant> neighborStorage = ItemStorage.SIDED.find(world, neighborPos, dir.getOpposite());
            if (neighborStorage == null) continue;

            try (Transaction transaction = Transaction.openOuter()) {
                for (var view : neighborStorage) {
                    if (view.isResourceBlank() || view.getAmount() <= 0) continue;

                    ItemVariant variant = view.getResource();
                    long maxExtract = Math.min(view.getAmount(), 8);

                    long extracted = neighborStorage.extract(variant, maxExtract, transaction);
                    if (extracted > 0) {
                        long inserted = ItemPipeNetwork.tryInsertIntoNetwork(world, pos, dir, variant, extracted, transaction, dir);
                        if (inserted > 0) {
                            transaction.commit();
                            break;
                        }
                    }
                }
            }
        }
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

    public boolean isServoActive(Direction direction) {
        return getData().isServoActive(direction);
    }

    public void setServoActive(Direction direction, boolean active) {
        modifyData(data -> data.withServoActive(direction, active));
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
