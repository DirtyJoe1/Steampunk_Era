package com.steampunkera.network;

import com.steampunkera.SteampunkEra;
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
import net.minecraft.util.math.Direction;

public record ServoTogglePayload(BlockPos pos, Direction servoSide, boolean enabled) implements CustomPayload {

    public static final Id<ServoTogglePayload> ID = new Id<>(Identifier.of(SteampunkEra.MOD_ID, "servo_toggle"));
    public static final PacketCodec<RegistryByteBuf, ServoTogglePayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, ServoTogglePayload::pos,
            PacketCodecs.indexed(i -> Direction.values()[i], Direction::ordinal), ServoTogglePayload::servoSide,
            PacketCodecs.BOOLEAN, ServoTogglePayload::enabled,
            ServoTogglePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            BlockPos pos = payload.pos();
            Direction servoSide = payload.servoSide();
            boolean enabled = payload.enabled();

            context.server().execute(() -> {
                if (context.server().getOverworld().getBlockEntity(pos) instanceof ItemPipeBlockEntity be) {
                    be.setServoActive(servoSide, enabled);
                    ServerPlayNetworking.send(context.player(), new ServoStatePayload(pos, servoSide, enabled));
                }
            });
        });

        PayloadTypeRegistry.playS2C().register(ServoStatePayload.ID, ServoStatePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ServoStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.player().currentScreenHandler instanceof ServoMenu menu) {
                        if (menu.getServoSide() == payload.servoSide()) {
                            menu.setEnabled(payload.enabled());
                            if (context.client().currentScreen instanceof ServoScreen screen) {
                                screen.updateButton(payload.enabled());
                            }
                        }
                    }
                })
        );
    }
}
