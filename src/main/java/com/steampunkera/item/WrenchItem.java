package com.steampunkera.item;

import com.steampunkera.SteampunkEra;
import com.steampunkera.ServoConfig;
import com.steampunkera.block.ItemPipeBlock;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import com.steampunkera.util.PipeHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class WrenchItem extends Item {

    public WrenchItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();

        if (!(state.getBlock() instanceof ItemPipeBlock)) {
            return ActionResult.PASS;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (player == null) {
            return ActionResult.PASS;
        }

        if (player.isSneaking()) {
            ItemPipeBlockEntity blockEntity = (ItemPipeBlockEntity) world.getBlockEntity(pos);
            if (blockEntity != null) {
                for (Direction dir : Direction.values()) {
                    if (blockEntity.hasServo(dir)) {
                        dropItem(world, pos, new ItemStack(SteampunkEra.SERVOS_ITEM, 1));
                    }
                }
            }
            world.breakBlock(pos, true);
            updatePipeConnections(world, pos);
            return ActionResult.SUCCESS;
        }

        ItemPipeBlockEntity blockEntity = (ItemPipeBlockEntity) world.getBlockEntity(pos);
        if (blockEntity == null) {
            return ActionResult.PASS;
        }

        Direction side = PipeHelper.getDirectionFromHitPos(context.getHitPos(), pos);
        Direction oppositeSide = side.getOpposite();

        BlockPos neighborPos = pos.offset(side);
        BlockState neighborState = world.getBlockState(neighborPos);
        if (!PipeHelper.isValidConnectionTarget(neighborState.getBlock())) {
            return ActionResult.PASS;
        }

        if (blockEntity.hasServo(side)) {
            blockEntity.setServo(side, false);
            blockEntity.setServoConfig(side, ServoConfig.DEFAULT);
            dropItem(world, pos, new ItemStack(SteampunkEra.SERVOS_ITEM, 1));
            world.playSound(null, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            player.sendMessage(Text.literal("Servo dismantled"), true);
            updatePipeConnections(world, pos);
            return ActionResult.SUCCESS;
        }

        blockEntity.toggleDisabled(side);

        if (neighborState.getBlock() instanceof ItemPipeBlock) {
            if (world.getBlockEntity(neighborPos) instanceof ItemPipeBlockEntity neighborBE) {
                neighborBE.toggleDisabled(oppositeSide);
            }
        }

        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);

        boolean isConnected = !blockEntity.isDisabled(side);
        String connectionState = isConnected ? "connected" : "disconnected";
        player.sendMessage(Text.literal("Pipe connection: " + connectionState), true);

        updatePipeConnections(world, pos);

        return ActionResult.SUCCESS;
    }

    private void dropItem(World world, BlockPos pos, ItemStack stack) {
        Vec3d dropPos = Vec3d.ofCenter(pos).add(0, 0.5, 0);
        ItemEntity itemEntity = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, stack);
        itemEntity.setVelocity(
            (world.random.nextDouble() - 0.5) * 0.1,
            0.2,
            (world.random.nextDouble() - 0.5) * 0.1
        );
        world.spawnEntity(itemEntity);
    }

    private void updatePipeConnections(World world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() instanceof ItemPipeBlock pipeBlock) {
            pipeBlock.refreshConnections(world, pos);
        }
    }
}
