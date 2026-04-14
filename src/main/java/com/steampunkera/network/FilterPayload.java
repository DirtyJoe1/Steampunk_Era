package com.steampunkera.network;

import com.steampunkera.SteampunkEra;
import com.steampunkera.screen.filter.FilterMenu;
import com.steampunkera.screen.filter.FilterMenuData;
import com.steampunkera.screen.servo.ServoMenu;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import com.steampunkera.screen.servo.ServoMenuData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class FilterPayload {

    public record OpenFilterScreen(BlockPos pos, Direction servoSide, int mouseX, int mouseY) implements CustomPayload {
        public static final Id<OpenFilterScreen> ID = new Id<>(id("open_filter_screen"));
        public static final PacketCodec<RegistryByteBuf, OpenFilterScreen> CODEC = PacketCodec.of(
                OpenFilterScreen::write, OpenFilterScreen::new
        );

        private OpenFilterScreen(RegistryByteBuf buf) {
            this(buf.readBlockPos(), Direction.values()[buf.readInt()], buf.readInt(), buf.readInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeInt(servoSide.ordinal());
            buf.writeInt(mouseX);
            buf.writeInt(mouseY);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BackToServo(BlockPos pos, Direction servoSide, boolean enabled, int mouseX, int mouseY) implements CustomPayload {
        public static final Id<BackToServo> ID = new Id<>(id("back_to_servo"));
        public static final PacketCodec<RegistryByteBuf, BackToServo> CODEC = PacketCodec.of(
                BackToServo::write, BackToServo::new
        );

        private BackToServo(RegistryByteBuf buf) {
            this(buf.readBlockPos(), Direction.values()[buf.readInt()], buf.readBoolean(), buf.readInt(), buf.readInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeInt(servoSide.ordinal());
            buf.writeBoolean(enabled);
            buf.writeInt(mouseX);
            buf.writeInt(mouseY);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public static void registerServerReceiver() {
            ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
                context.server().execute(() -> {
                    if (context.server().getOverworld().getBlockEntity(payload.pos()) instanceof ItemPipeBlockEntity pipe) {
                        var servoSide = payload.servoSide();
                        var config = pipe.getServoConfig(servoSide);
                        var enabled = payload.enabled();
                        final int mouseX = payload.mouseX();
                        final int mouseY = payload.mouseY();
                        ExtendedScreenHandlerFactory<ServoMenuData.ServoData> factory =
                                new ExtendedScreenHandlerFactory<>() {
                            @Override
                            public ServoMenuData.ServoData getScreenOpeningData(ServerPlayerEntity player) {
                                return new ServoMenuData.ServoData(payload.pos(), servoSide, enabled, config, mouseX, mouseY);
                            }

                            @Override
                            public Text getDisplayName() {
                                return Text.literal("Servo");
                            }

                            @Override
                            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                                return new ServoMenu(syncId, playerInventory, payload.pos(), servoSide, enabled, config, mouseX, mouseY);
                            }
                        };
                        context.player().openHandledScreen(factory);
                    }
                });
            });
        }
    }

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(OpenFilterScreen.ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                if (context.server().getOverworld().getBlockEntity(payload.pos()) instanceof ItemPipeBlockEntity pipe) {
                    final boolean enabled = !(player.currentScreenHandler instanceof ServoMenu servoMenu) || servoMenu.isEnabled();
                    final var filterMode = pipe.getServoConfig(payload.servoSide()).filterMode();
                    final int mouseX = payload.mouseX();
                    final int mouseY = payload.mouseY();
                    ExtendedScreenHandlerFactory<FilterMenuData.FilterData> factory = new ExtendedScreenHandlerFactory<>() {
                        @Override
                        public FilterMenuData.FilterData getScreenOpeningData(ServerPlayerEntity player) {
                            return new FilterMenuData.FilterData(payload.pos(), payload.servoSide(), enabled, filterMode, mouseX, mouseY);
                        }

                        @Override
                        public Text getDisplayName() {
                            return Text.literal("Filter");
                        }

                        @Override
                        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                            return new FilterMenu(
                                    syncId, playerInventory, payload.pos(), payload.servoSide(), enabled, filterMode, mouseX, mouseY);
                        }
                    };
                    context.player().openHandledScreen(factory);
                }
            });
        });
    }

    private static Identifier id(String path) {
        return Identifier.of(SteampunkEra.MOD_ID, path);
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(OpenFilterScreen.ID, OpenFilterScreen.CODEC);
        PayloadTypeRegistry.playC2S().register(BackToServo.ID, BackToServo.CODEC);
        registerServerReceiver();
        BackToServo.registerServerReceiver();
    }
}
