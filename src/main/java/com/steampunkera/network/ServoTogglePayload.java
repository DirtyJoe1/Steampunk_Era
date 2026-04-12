package com.steampunkera.network;

import com.steampunkera.SteampunkEra;
import com.steampunkera.SteampunkEraAttachments;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import com.steampunkera.screen.ServoMenu;
import com.steampunkera.screen.ServoScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class ServoTogglePayload implements CustomPayload {

    public static final Id<ServoTogglePayload> ID = new Id<>(Identifier.of(SteampunkEra.MOD_ID, "servo_toggle"));
    public static final PacketCodec<RegistryByteBuf, ServoTogglePayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, ServoTogglePayload::pos,
            PacketCodecs.BOOLEAN, ServoTogglePayload::enabled,
            ServoTogglePayload::new
    );

    private final BlockPos pos;
    private final boolean enabled;

    public ServoTogglePayload(BlockPos pos, boolean enabled) {
        this.pos = pos;
        this.enabled = enabled;
    }

    public BlockPos pos() { return pos; }
    public boolean enabled() { return enabled; }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            BlockPos pos = payload.pos();
            boolean enabled = payload.enabled();

            context.server().execute(() -> {
                if (context.server().getOverworld().getBlockEntity(pos) instanceof ItemPipeBlockEntity be) {
                    be.setAttached(SteampunkEraAttachments.SERVO_ACTIVE, enabled);
                    be.markDirty();
                    ServerPlayNetworking.send(context.player(), new ServoStatePayload(pos, enabled));
                }
            });
        });

        PayloadTypeRegistry.playS2C().register(ServoStatePayload.ID, ServoStatePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ServoStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
            if (context.player().currentScreenHandler instanceof ServoMenu menu) {
                menu.setEnabled(payload.enabled());
                if (context.client().currentScreen instanceof ServoScreen screen) {
                    screen.updateButton(payload.enabled());
                }
            }
        }));
    }

    public record ServoStatePayload(BlockPos pos, boolean enabled) implements CustomPayload {
        public static final Id<ServoStatePayload> ID = new Id<>(Identifier.of(SteampunkEra.MOD_ID, "servo_state"));
        public static final PacketCodec<RegistryByteBuf, ServoStatePayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, ServoStatePayload::pos,
                PacketCodecs.BOOLEAN, ServoStatePayload::enabled,
                ServoStatePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
