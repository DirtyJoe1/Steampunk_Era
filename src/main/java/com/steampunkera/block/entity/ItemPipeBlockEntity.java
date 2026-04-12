package com.steampunkera.block.entity;

import com.steampunkera.SteampunkEra;
import com.steampunkera.SteampunkEraAttachments;
import com.steampunkera.SteampunkEraAttachments.PipeData;
import com.steampunkera.ServoConfig;
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

import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ItemPipeBlockEntity extends BlockEntity {

    private final Map<Direction, Integer> tickCounters = new EnumMap<>(Direction.class);
    private int roundRobinIndex = 0;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(SteampunkEra.ITEM_PIPE_BLOCK_ENTITY_TYPE, pos, state);
        if (state.getBlock() instanceof ItemPipeBlock) {
            SteampunkEra.TICKING_PIPES.add(this);
            for (Direction dir : Direction.values()) {
                tickCounters.put(dir, 0);
            }
        }
    }

    @Override
    public void markRemoved() {
        SteampunkEra.TICKING_PIPES.remove(this);
        super.markRemoved();
    }

    public void tick(World world) {
        if (world.isClient()) return;
        if (!(world.getBlockState(pos).getBlock() instanceof ItemPipeBlock)) {
            SteampunkEra.TICKING_PIPES.remove(this);
            return;
        }

        PipeData data = getData();
        if (data == null) return;

        for (Direction dir : Direction.values()) {
            if (!data.hasServo(dir)) continue;

            ServoConfig config = data.getServoConfig(dir);
            if (!config.enabled()) continue;

            int counter = tickCounters.getOrDefault(dir, 0);
            counter++;
            if (counter < config.extractInterval()) {
                tickCounters.put(dir, counter);
                continue;
            }
            tickCounters.put(dir, 0);

            processServo(world, pos, dir, data, config);
        }
    }

    private void processServo(World world, BlockPos pos, Direction dir, PipeData data, ServoConfig config) {
        BlockPos neighborPos = pos.offset(dir);
        Storage<ItemVariant> neighborStorage = ItemStorage.SIDED.find(world, neighborPos, dir.getOpposite());
        if (neighborStorage == null) return;

        try (Transaction transaction = Transaction.openOuter()) {
            for (var view : neighborStorage) {
                if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                ItemVariant variant = view.getResource();
                if (!config.matchesFilter(variant.getItem())) continue;

                long maxExtract = Math.min(view.getAmount(), config.maxExtract());
                long extracted = neighborStorage.extract(variant, maxExtract, transaction);
                if (extracted > 0) {
                    long inserted = ItemPipeNetwork.tryInsertIntoNetwork(world, pos, dir, variant, extracted, transaction, dir, config.routingMode(), this);
                    if (inserted > 0) {
                        transaction.commit();
                        break;
                    }
                }
            }
        }
    }

    public int getAndIncrementRoundRobin() { return roundRobinIndex++; }

    private PipeData getData() { return getAttachedOrCreate(SteampunkEraAttachments.PIPE_DATA); }
    private void setData(PipeData data) { setAttached(SteampunkEraAttachments.PIPE_DATA, data); }
    private void modifyData(UnaryOperator<PipeData> modifier) { setData(modifier.apply(getData())); markDirty(); }

    public boolean isDisabled(Direction dir) { return getData().isDisabled(dir); }
    public void toggleDisabled(Direction dir) { modifyData(d -> d.withDisabled(dir, !d.isDisabled(dir))); updateBlockState(); }
    public boolean hasServo(Direction dir) { return getData().hasServo(dir); }
    public void setServo(Direction dir, boolean v) { modifyData(d -> d.withServo(dir, v)); updateServoState(); }
    public ServoConfig getServoConfig(Direction dir) { return getData().getServoConfig(dir); }
    public void setServoConfig(Direction dir, ServoConfig c) { modifyData(d -> d.withServoConfig(dir, c)); }

    private void updateServoState() {
        if (world != null && !world.isClient() && world.getBlockEntity(pos) == this) {
            if (getCachedState().getBlock() instanceof ItemPipeBlock pb) pb.updateServoState(world, pos);
        }
    }
    public boolean isConnected(Direction dir) {
        if (world == null) return false;
        BlockState state = getCachedState();
        return state.getBlock() instanceof ItemPipeBlock pb && pb.hasConnection(state, dir) && !isDisabled(dir);
    }
    private void updateBlockState() {
        if (world != null && !world.isClient()) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }
}
