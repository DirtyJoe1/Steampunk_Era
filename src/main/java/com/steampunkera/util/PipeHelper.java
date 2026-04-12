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
        Vec3d center = blockPos.toCenterPos();
        Vec3d delta = hitPos.subtract(center);

        double absX = Math.abs(delta.x);
        double absY = Math.abs(delta.y);
        double absZ = Math.abs(delta.z);

        if (absX > absY && absX > absZ) {
            return delta.x > 0 ? Direction.EAST : Direction.WEST;
        } else if (absY > absX && absY > absZ) {
            return delta.y > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return delta.z > 0 ? Direction.SOUTH : Direction.NORTH;
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
