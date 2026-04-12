package com.steampunkera.network;

import com.steampunkera.SteampunkEra;
import com.steampunkera.SteampunkEraAttachments;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class ServoTogglePayload implements CustomPayload {

    public static final Id<ServoTogglePayload> ID = new Id<>(Identifier.of(SteampunkEra.MOD_ID, "servo_toggle"));
    public static final PacketCodec<RegistryByteBuf, ServoTogglePayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, ServoTogglePayload::pos,
            ServoTogglePayload::new
    );

    private final BlockPos pos;

    public ServoTogglePayload(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos pos() { return pos; }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            BlockPos pos = payload.pos();

            context.server().execute(() -> {
                var world = context.server().getOverworld();
                if (world.getBlockEntity(pos) instanceof ItemPipeBlockEntity be) {
                    boolean current = be.getAttachedOrCreate(SteampunkEraAttachments.SERVO_ACTIVE);
                    be.setAttached(SteampunkEraAttachments.SERVO_ACTIVE, !current);
                    be.markDirty();
                }
            });
        });
    }
}
