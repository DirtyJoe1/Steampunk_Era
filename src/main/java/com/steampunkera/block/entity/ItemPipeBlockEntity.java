package com.steampunkera.block.entity;

import com.steampunkera.util.FilterConfig;
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

    private int disabledMask = 0;
    private int servoMask = 0;
    private int filterMask = 0;
    private final Map<Direction, ServoConfig> servoConfigs = new EnumMap<>(Direction.class);
    private final Map<Direction, Integer> tickCounters = new EnumMap<>(Direction.class);
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    
    private final Map<Direction, FilterConfig> filterConfigs = new EnumMap<>(Direction.class);

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(SteampunkEra.ITEM_PIPE_BLOCK_ENTITY_TYPE, pos, state);
        initDefaults();
        SteampunkEra.TICKING_PIPES.add(this);
    }

    private void initDefaults() {
        for (Direction dir : ALL_DIRECTIONS) {
            servoConfigs.put(dir, ServoConfig.DEFAULT);
            tickCounters.put(dir, 0);
            filterConfigs.put(dir, FilterConfig.DEFAULT);
        }
        roundRobinIndex.set(0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("disabledMask", disabledMask);
        view.putInt("servoMask", servoMask);
        view.putInt("filterMask", filterMask);
        for (Direction dir : ALL_DIRECTIONS) {
            String key = dir.asString();
            ServoConfig config = servoConfigs.get(dir);
            if (config != null && !config.equals(ServoConfig.DEFAULT)) {
                NbtCompound nbt = config.toNbt();
                view.put("config_" + key, NbtCompound.CODEC, nbt);
            }
            FilterConfig fConfig = filterConfigs.get(dir);
            if (fConfig != null && !fConfig.equals(FilterConfig.DEFAULT)) {
                view.put("filter_config_" + key, NbtCompound.CODEC, fConfig.toNbt());
            }
        }
        view.putInt("roundRobin", roundRobinIndex.get());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        disabledMask = view.getInt("disabledMask", 0);
        servoMask = view.getInt("servoMask", 0);
        filterMask = view.getInt("filterMask", 0);
        for (Direction dir : ALL_DIRECTIONS) {
            String key = dir.asString();
            Optional<NbtCompound> cfgOpt = view.read("config_" + key, NbtCompound.CODEC);
            servoConfigs.put(dir, cfgOpt.map(ServoConfig::fromNbt).orElse(ServoConfig.DEFAULT));
            Optional<NbtCompound> filterOpt = view.read("filter_config_" + key, NbtCompound.CODEC);
            filterConfigs.put(dir, filterOpt.map(FilterConfig::fromNbt).orElse(FilterConfig.DEFAULT));
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
            if (!hasServo(dir)) continue;
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

    public boolean isDisabled(Direction dir) { return ((disabledMask >>> dir.ordinal()) & 1) != 0; }
    public void toggleDisabled(Direction dir) {
        int idx = dir.ordinal();
        disabledMask ^= (1 << idx);
        markDirty();
        updateBlockState();
    }
    public boolean hasServo(Direction dir) { return ((servoMask >>> dir.ordinal()) & 1) != 0; }
    public void setServo(Direction dir, boolean v) {
        int idx = dir.ordinal();
        if (v) servoMask |= (1 << idx); else servoMask &= ~(1 << idx);
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

    public boolean hasFilter(Direction dir) { return ((filterMask >>> dir.ordinal()) & 1) != 0; }
    public void setFilter(Direction dir, boolean hasFilter) {
        int idx = dir.ordinal();
        if (hasFilter) filterMask |= (1 << idx); else filterMask &= ~(1 << idx);
        if (!hasFilter) {
            filterConfigs.put(dir, FilterConfig.DEFAULT);
        }
        markDirty();
        updateBlockState();
    }
    public FilterConfig getFilterConfig(Direction dir) { return filterConfigs.getOrDefault(dir, FilterConfig.DEFAULT); }
    public void setFilterConfig(Direction dir, FilterConfig config) {
        filterConfigs.put(dir, config);
        markDirty();
    }
}
