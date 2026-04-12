package com.steampunkera.util;

import com.steampunkera.block.ItemPipeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Утилиты для работы с трубами.
 */
public final class PipeHelper {

    private PipeHelper() {}

    /**
     * Определяет направление от центра блока к точке попадания.
     */
    public static Direction getDirectionFromHitPos(Vec3d hitPos, BlockPos blockPos) {
        double dx = hitPos.x - (blockPos.getX() + 0.5);
        double dy = hitPos.y - (blockPos.getY() + 0.5);
        double dz = hitPos.z - (blockPos.getZ() + 0.5);

        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);

        if (absX > absY && absX > absZ) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (absY > absX && absY > absZ) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
    public static boolean isValidConnectionTarget(Block block) {
        return block instanceof BlockEntityProvider;
    }

    /**
     * Проверяет, является ли блок инвентарём (BlockEntityProvider, но не труба).
     * Сервопривод можно ставить только на такие блоки.
     */
    public static boolean isInventoryNotAPipe(Block block) {
        return block instanceof BlockEntityProvider && !(block instanceof ItemPipeBlock);
    }
}
