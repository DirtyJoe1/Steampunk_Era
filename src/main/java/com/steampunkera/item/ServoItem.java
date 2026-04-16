package com.steampunkera.item;

import com.steampunkera.block.ItemPipeBlock;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import com.steampunkera.util.PipeHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Предмет "Сервопривод" — устанавливается на соединение трубы,
 * подключённое к инвентарю (BlockEntityProvider).
 */
public class ServoItem extends Item {

    public ServoItem(Settings settings) {
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

        ItemPipeBlockEntity blockEntity = (ItemPipeBlockEntity) world.getBlockEntity(pos);
        if (blockEntity == null) {
            return ActionResult.CONSUME;
        }

        Direction side = PipeHelper.getDirectionFromHitPos(context.getHitPos(), pos);

        if (!PipeHelper.isInventoryNotAPipe(world.getBlockState(pos.offset(side)).getBlock())) {
            player.sendMessage(Text.literal("Servo can be installed only to side with inventory"), true);
            return ActionResult.PASS;
        }

        if (blockEntity.hasServo(side)) {
            player.sendMessage(Text.literal("Servo already installed"), true);
            return ActionResult.FAIL;
        }
        
        if (blockEntity.hasFilter(side)) {
            player.sendMessage(Text.literal("Cannot place servo: filter already installed on this side"), true);
            return ActionResult.FAIL;
        }
        
        blockEntity.setServo(side, true);
        player.sendMessage(Text.literal("Servo installed"), true);
        context.getStack().decrement(1);
        world.playSound(null, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        return ActionResult.SUCCESS;
    }
}
