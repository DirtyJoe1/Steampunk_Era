package com.steampunkera.block;

import com.steampunkera.util.FilterConfig;
import com.steampunkera.util.ServoConfig;
import com.steampunkera.screen.filter.FilterMenu;
import com.steampunkera.screen.filter.FilterMenuData;
import com.steampunkera.screen.servo.ServoMenuData.ServoData;
import com.steampunkera.SteampunkEra;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import com.steampunkera.item.WrenchItem;
import com.steampunkera.screen.servo.ServoMenu;
import com.steampunkera.util.PipeHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class ItemPipeBlock extends Block implements BlockEntityProvider {

    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;

    protected static final VoxelShape CENTER = Block.createCuboidShape(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    protected static final VoxelShape UP_SHAPE = Block.createCuboidShape(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    protected static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);
    protected static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    protected static final VoxelShape EAST_SHAPE = Block.createCuboidShape(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    protected static final VoxelShape WEST_SHAPE = Block.createCuboidShape(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);

    public ItemPipeBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(NORTH, false)
                .with(SOUTH, false)
                .with(EAST, false)
                .with(WEST, false)
                .with(UP, false)
                .with(DOWN, false));
    }

    public BooleanProperty getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CENTER;
        if (state.get(NORTH)) shape = VoxelShapes.union(shape, NORTH_SHAPE);
        if (state.get(SOUTH)) shape = VoxelShapes.union(shape, SOUTH_SHAPE);
        if (state.get(EAST)) shape = VoxelShapes.union(shape, EAST_SHAPE);
        if (state.get(WEST)) shape = VoxelShapes.union(shape, WEST_SHAPE);
        if (state.get(UP)) shape = VoxelShapes.union(shape, UP_SHAPE);
        if (state.get(DOWN)) shape = VoxelShapes.union(shape, DOWN_SHAPE);
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.afterBreak(world, player, pos, state, blockEntity, tool);

        if (blockEntity instanceof ItemPipeBlockEntity be) {
            for (Direction dir : Direction.values()) {
                if (be.hasServo(dir)) {
                    dropServo(world, pos, dir);
                }
            }
        }
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof WrenchItem) {
            return ActionResult.PASS;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (world.getBlockEntity(pos) instanceof ItemPipeBlockEntity be) {
            Direction side = PipeHelper.getDirectionFromHitPos(hit.getPos(), pos);

            if (be.hasServo(side)) {
                try {
                    boolean enabled = be.getServoConfig(side).enabled();
                    ServoConfig config = be.getServoConfig(side);

                    player.openHandledScreen(new ExtendedScreenHandlerFactory<ServoData>() {
                        @Override
                        public Text getDisplayName() {
                            return Text.literal("Servo");
                        }

                        @Override
                        public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                            return new ServoMenu(syncId, inv, be, side, enabled);
                        }

                        @Override
                        public @NonNull ServoData getScreenOpeningData(@NonNull ServerPlayerEntity player) {
                            return new ServoData(pos, side, enabled, config, 0, 0);
                        }
                    });
                    return ActionResult.SUCCESS;
                } catch (Exception e) {
                    SteampunkEra.LOGGER.error("[ServoMenu] Error opening GUI: {}", e.getMessage());
                    return ActionResult.FAIL;
                }
            }
            
            // Если нет сервопривода, проверяем фильтр на той же стороне
            if (be.hasFilter(side)) {
                try {
                    FilterConfig config = be.getFilterConfig(side);
                    boolean enabled = true; // Фильтр всегда включен
                    
                    player.openHandledScreen(new ExtendedScreenHandlerFactory<FilterMenuData.FilterData>() {
                        @Override
                        public Text getDisplayName() {
                            return Text.literal("Filter");
                        }

                        @Override
                        public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                            return new FilterMenu(syncId, inv, pos, side, enabled, config.filterMode(), config.filterItems(), 0, 0);
                        }

                        @Override
                        public FilterMenuData.FilterData getScreenOpeningData(ServerPlayerEntity player) {
                            return new FilterMenuData.FilterData(pos, side, enabled, config.filterMode(), config.filterItems(), 0, 0);
                        }
                    });
                    return ActionResult.SUCCESS;
                } catch (Exception e) {
                    SteampunkEra.LOGGER.error("[FilterMenu] Error opening GUI: {}", e.getMessage());
                    return ActionResult.FAIL;
                }
            }
        }

        return ActionResult.PASS;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState defaultState = getDefaultState();
        return updateConnections(world, pos, defaultState);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @org.jetbrains.annotations.Nullable net.minecraft.world.block.WireOrientation wireOrientation, boolean notify) {
        refreshConnections(world, pos);
    }

    public void refreshConnections(World world, BlockPos pos) {
        BlockState currentState = world.getBlockState(pos);
        BlockState newState = updateAllConnections(world, pos, currentState);
        world.setBlockState(pos, newState, 3);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof ItemPipeBlock) {
                BlockState updatedNeighbor = updateAllConnections(world, neighborPos, neighborState);
                world.setBlockState(neighborPos, updatedNeighbor, 3);
            }
        }
    }

    private BlockState updateConnections(World world, BlockPos pos, BlockState existingState) {
        return updateAllConnections(world, pos, existingState);
    }

    private BlockState updateAllConnections(World world, BlockPos pos, BlockState existingState) {
        BlockState newState = existingState;
        for (Direction dir : Direction.values()) {
            newState = newState.with(getPropertyForDirection(dir), canConnectTo(world, pos, dir));
        }
        if (world.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipeBE) {
            for (Direction dir : Direction.values()) {
                if (pipeBE.hasServo(dir) && !PipeHelper.isInventoryNotAPipe(world.getBlockState(pos.offset(dir)).getBlock())) {
                    dropServo(world, pos, dir);
                    pipeBE.setServo(dir, false);
                    pipeBE.setServoConfig(dir, ServoConfig.DEFAULT);
                }
            }
        }
        return newState;
    }

    private void dropServo(World world, BlockPos pos, Direction dir) {
        if (!world.isClient()) {
            if (world.getBlockEntity(pos) instanceof ItemPipeBlockEntity be) {
                be.setServo(dir, false);
                be.setServoConfig(dir, ServoConfig.DEFAULT);
            }
            PipeHelper.dropItemAtOffset(world, pos, dir, new ItemStack(SteampunkEra.SERVO), 0.1);
        }
    }

    public void updateServoState(World world, BlockPos pos) {
    }

    public void updateFilterState(World world, BlockPos pos, Direction dir) {
    }

    private boolean canConnectTo(BlockView world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        if (world.getBlockEntity(pos) instanceof ItemPipeBlockEntity currentBE) {
            if (currentBE.isDisabled(direction)) {
                return false;
            }
        }

        if (neighborBlock instanceof ItemPipeBlock) {
            if (world.getBlockEntity(neighborPos) instanceof ItemPipeBlockEntity neighborBE) {
                return !neighborBE.isDisabled(direction.getOpposite());
            }
            return true;
        }

        return neighborBlock instanceof BlockEntityProvider;
    }

    public boolean hasConnection(BlockState state, Direction direction) {
        return state.get(getPropertyForDirection(direction));
    }
}
