package com.steampunkera;

import com.steampunkera.screen.ServoMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class ServoMenuData {

    public record ServoData(BlockPos pos, Direction servoSide, boolean enabled) {
        public static final PacketCodec<RegistryByteBuf, ServoData> PACKET_CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, ServoData::pos,
                PacketCodecs.indexed(i -> Direction.values()[i], Direction::ordinal), ServoData::servoSide,
                PacketCodecs.BOOLEAN, ServoData::enabled,
                ServoData::new
        );
    }

    public static final ExtendedScreenHandlerType<ServoMenu, ServoData> SERVO_MENU_TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            id("servo_menu"),
            new ExtendedScreenHandlerType<>(
                    (syncId, inventory, data) -> new ServoMenu(syncId, inventory, data.pos(), data.servoSide(), data.enabled()),
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
