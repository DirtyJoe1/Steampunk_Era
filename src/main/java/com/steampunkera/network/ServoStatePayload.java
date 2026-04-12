package com.steampunkera.network;

import com.steampunkera.SteampunkEra;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record ServoStatePayload(BlockPos pos, Direction servoSide, boolean enabled) implements CustomPayload {
    public static final Id<ServoStatePayload> ID = new Id<>(Identifier.of(SteampunkEra.MOD_ID, "servo_state"));
    public static final PacketCodec<RegistryByteBuf, ServoStatePayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, ServoStatePayload::pos,
            PacketCodecs.indexed(i -> Direction.values()[i], Direction::ordinal), ServoStatePayload::servoSide,
            PacketCodecs.BOOLEAN, ServoStatePayload::enabled,
            ServoStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
