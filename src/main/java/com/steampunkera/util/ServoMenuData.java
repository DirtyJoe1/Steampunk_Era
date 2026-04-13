package com.steampunkera.util;

import com.steampunkera.SteampunkEra;
import com.steampunkera.screen.servo.ServoMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class ServoMenuData {

    public record ServoData(BlockPos pos, Direction servoSide, boolean enabled, ServoConfig config) {
        public static final PacketCodec<RegistryByteBuf, ServoData> PACKET_CODEC = new PacketCodec<>() {
            @Override
            public ServoData decode(RegistryByteBuf buf) {
                BlockPos pos = buf.readBlockPos();
                Direction servoSide = Direction.values()[buf.readInt()];
                boolean enabled = buf.readBoolean();
                ServoConfig config = ServoConfig.fromNbt(buf.readNbt());
                return new ServoData(pos, servoSide, enabled, config);
            }

            @Override
            public void encode(RegistryByteBuf buf, ServoData data) {
                buf.writeBlockPos(data.pos());
                buf.writeInt(data.servoSide().ordinal());
                buf.writeBoolean(data.enabled());
                buf.writeNbt(data.config().toNbt());
            }
        };
    }

    public static final ExtendedScreenHandlerType<ServoMenu, ServoData> SERVO_MENU_TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            id("servo_menu"),
            new ExtendedScreenHandlerType<>(
                    (syncId, inventory, data) -> new ServoMenu(syncId, inventory, data.pos(), data.servoSide(), data.enabled(), data.config()),
                    ServoData.PACKET_CODEC
            )
    );

    private static Identifier id(String path) {
        return Identifier.of(SteampunkEra.MOD_ID, path);
    }

    public static void init() {
        ExtendedScreenHandlerType<?, ?> dummy = SERVO_MENU_TYPE;
    }
}
