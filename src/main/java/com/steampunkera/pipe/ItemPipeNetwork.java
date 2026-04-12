package com.steampunkera.pipe;

import com.steampunkera.ServoConfig;
import com.steampunkera.block.ItemPipeBlock;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

public final class ItemPipeNetwork {

    private ItemPipeNetwork() {}

    public static long tryInsertIntoNetwork(World world, BlockPos fromPos, Direction fromSide,
                                            ItemVariant variant, long maxAmount, Transaction outerTransaction,
                                            Direction extractSide, ServoConfig.RoutingMode routingMode,
                                            ItemPipeBlockEntity sourceBE) {
        BlockPos sourceBlockPos = fromPos.offset(extractSide);
        List<InventoryTarget> targets = findInventoryTargets(world, fromPos, extractSide, sourceBlockPos);

        if (targets.isEmpty()) return 0;

        // Сортировка в зависимости от режима
        switch (routingMode) {
            case FURTHEST_FIRST -> targets.sort((a, b) -> Integer.compare(b.distance(), a.distance()));
            case ROUND_ROBIN -> {
                int idx = sourceBE.getAndIncrementRoundRobin() % targets.size();
                if (idx < 0) idx += targets.size();
                InventoryTarget target = targets.get(idx);
                targets.clear();
                targets.add(target);
            }
            default -> targets.sort(Comparator.comparingInt(InventoryTarget::distance)); // NEAREST_FIRST
        }

        long totalInserted = 0;
        long remaining = maxAmount;

        for (InventoryTarget target : targets) {
            if (remaining <= 0) break;
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, target.pos(), target.side());
            if (storage == null) continue;

            long inserted = storage.insert(variant, remaining, outerTransaction);
            if (inserted > 0) {
                totalInserted += inserted;
                remaining -= inserted;
            }
        }

        return totalInserted;
    }

    private static List<InventoryTarget> findInventoryTargets(World world, BlockPos startPos, Direction extractSide, BlockPos sourceBlockPos) {
        List<InventoryTarget> targets = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BfsNode> queue = new ArrayDeque<>();

        queue.add(new BfsNode(startPos, 0));
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BfsNode current = queue.poll();
            BlockPos pos = current.pos();
            int distance = current.distance();

            for (Direction dir : Direction.values()) {
                if (pos.equals(startPos) && dir == extractSide) continue;

                BlockPos neighborPos = pos.offset(dir);
                if (visited.contains(neighborPos)) continue;
                visited.add(neighborPos);

                if (world.getBlockState(neighborPos).getBlock() instanceof ItemPipeBlock) {
                    if (world.getBlockEntity(neighborPos) instanceof ItemPipeBlockEntity pipeBE) {
                        if (pipeBE.hasServo(dir.getOpposite())) continue;
                    }
                    queue.add(new BfsNode(neighborPos, distance + 1));
                } else {
                    if (neighborPos.equals(sourceBlockPos)) continue;

                    Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, neighborPos, dir.getOpposite());
                    if (storage != null && supportsInsert(storage)) {
                        targets.add(new InventoryTarget(neighborPos, dir.getOpposite(), distance + 1));
                    }
                }
            }
        }

        return targets;
    }

    private static boolean supportsInsert(Storage<ItemVariant> storage) {
        for (var view : storage) {
            if (view.isResourceBlank() || view.getAmount() < view.getCapacity()) return true;
        }
        return !storage.iterator().hasNext();
    }

    private record BfsNode(BlockPos pos, int distance) {}
    public record InventoryTarget(BlockPos pos, Direction side, int distance) {}
}
