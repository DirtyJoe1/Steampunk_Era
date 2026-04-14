package com.steampunkera.util;

import com.steampunkera.block.ItemPipeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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

    /**
     * Выбрасывает предмет в мир с небольшим случайным смещением скорости.
     * 
     * @param world мир
     * @param pos позиция выброса
     * @param stack предмет для выброса
     * @param velocity множитель скорости (обычно 0.1)
     */
    public static void dropItemAt(World world, BlockPos pos, ItemStack stack, double velocity) {
        Vec3d dropPos = Vec3d.ofCenter(pos);
        ItemEntity itemEntity = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, stack);
        itemEntity.setVelocity(
            (world.random.nextDouble() - 0.5) * velocity,
            0.2,
            (world.random.nextDouble() - 0.5) * velocity
        );
        world.spawnEntity(itemEntity);
    }

    /**
     * Выбрасывает предмет в мир с направленным смещением.
     * 
     * @param world мир
     * @param pos позиция блока
     * @param direction направление смещения от центра
     * @param stack предмет для выброса
     * @param velocity множитель скорости
     */
    public static void dropItemAtOffset(World world, BlockPos pos, Direction direction, ItemStack stack, double velocity) {
        Vec3d dropPos = Vec3d.ofCenter(pos).offset(direction, 0.5);
        ItemEntity itemEntity = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, stack);
        itemEntity.setVelocity(
            (world.random.nextDouble() - 0.5) * velocity,
            0.2,
            (world.random.nextDouble() - 0.5) * velocity
        );
        world.spawnEntity(itemEntity);
    }
}
