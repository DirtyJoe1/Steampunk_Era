package com.steampunkera.block.entity;

import com.steampunkera.util.ServoConfig;
import com.steampunkera.SteampunkEra;
import com.steampunkera.block.ItemPipeBlock;
import com.steampunkera.util.ItemPipeNetwork;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemPipeBlockEntity extends BlockEntity {

    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    private final Map<Direction, Boolean> disabledConnections = new EnumMap<>(Direction.class);
    private final Map<Direction, Boolean> servoAttachments = new EnumMap<>(Direction.class);
    private final Map<Direction, ServoConfig> servoConfigs = new EnumMap<>(Direction.class);
    private final Map<Direction, Integer> tickCounters = new EnumMap<>(Direction.class);
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(SteampunkEra.ITEM_PIPE_BLOCK_ENTITY_TYPE, pos, state);
        initDefaults();
        SteampunkEra.TICKING_PIPES.add(this);
    }

    private void initDefaults() {
        for (Direction dir : ALL_DIRECTIONS) {
            disabledConnections.put(dir, false);
            servoAttachments.put(dir, false);
            servoConfigs.put(dir, ServoConfig.DEFAULT);
            tickCounters.put(dir, 0);
        }
        roundRobinIndex.set(0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        for (Direction dir : ALL_DIRECTIONS) {
            String key = dir.asString();
            view.putBoolean("disabled_" + key, disabledConnections.getOrDefault(dir, false));
            view.putBoolean("servo_" + key, servoAttachments.getOrDefault(dir, false));
            ServoConfig config = servoConfigs.get(dir);
            if (config != null && !config.equals(ServoConfig.DEFAULT)) {
                NbtCompound nbt = config.toNbt();
                view.put("config_" + key, NbtCompound.CODEC, nbt);
            }
        }
        view.putInt("roundRobin", roundRobinIndex.get());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        for (Direction dir : ALL_DIRECTIONS) {
            String key = dir.asString();
            disabledConnections.put(dir, view.getBoolean("disabled_" + key, false));
            servoAttachments.put(dir, view.getBoolean("servo_" + key, false));
            Optional<NbtCompound> cfgOpt = view.read("config_" + key, NbtCompound.CODEC);
            cfgOpt.ifPresentOrElse(
                    cfg -> servoConfigs.put(dir, ServoConfig.fromNbt(cfg)),
                    () -> servoConfigs.put(dir, ServoConfig.DEFAULT)
            );
            tickCounters.put(dir, 0);
        }
        roundRobinIndex.set(view.getInt("roundRobin", 0));
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

        for (Direction dir : ALL_DIRECTIONS) {
            if (!servoAttachments.getOrDefault(dir, false)) continue;
            ServoConfig config = servoConfigs.getOrDefault(dir, ServoConfig.DEFAULT);
            if (!config.enabled()) continue;

            int counter = tickCounters.getOrDefault(dir, 0);
            counter++;
            if (counter < config.extractInterval()) {
                tickCounters.put(dir, counter);
                continue;
            }
            tickCounters.put(dir, 0);
            processServo(world, dir, config);
        }
    }

    private void processServo(World world, Direction dir, ServoConfig config) {
        BlockPos neighborPos = pos.offset(dir);
        Storage<ItemVariant> neighborStorage = ItemStorage.SIDED.find(world, neighborPos, dir.getOpposite());
        if (neighborStorage == null) return;

        for (var view : neighborStorage) {
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            ItemVariant variant = view.getResource();
            if (!config.matchesFilter(variant.getItem())) continue;

            long maxExtract = Math.min(view.getAmount(), config.maxExtract());
            boolean success = false;

            try (Transaction tx = Transaction.openOuter()) {
                long extracted = neighborStorage.extract(variant, maxExtract, tx);
                if (extracted > 0) {
                    long inserted = ItemPipeNetwork.tryInsertIntoNetwork(world, pos, dir, variant, extracted, tx, dir, config.routingMode(), this);
                    if (inserted == extracted) {
                        tx.commit();
                        success = true;
                    }
                }
            }

            if (success) break;
        }
    }

    public int getAndIncrementRoundRobin() { return roundRobinIndex.getAndIncrement(); }

    public boolean isDisabled(Direction dir) { return disabledConnections.getOrDefault(dir, false); }
    public void toggleDisabled(Direction dir) {
        disabledConnections.put(dir, !disabledConnections.getOrDefault(dir, false));
        markDirty();
        updateBlockState();
    }
    public boolean hasServo(Direction dir) { return servoAttachments.getOrDefault(dir, false); }
    public void setServo(Direction dir, boolean v) {
        servoAttachments.put(dir, v);
        markDirty();
        updateServoState();
    }
    public ServoConfig getServoConfig(Direction dir) { return servoConfigs.getOrDefault(dir, ServoConfig.DEFAULT); }
    public void setServoConfig(Direction dir, ServoConfig c) {
        servoConfigs.put(dir, c);
        markDirty();
    }

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
