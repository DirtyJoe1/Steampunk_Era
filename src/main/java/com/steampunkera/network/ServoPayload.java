package com.steampunkera.network;

import com.steampunkera.SteampunkEra;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import com.steampunkera.screen.servo.ServoMenu;
import com.steampunkera.screen.servo.ServoScreen;
import com.steampunkera.screen.filter.FilterMenu;
import com.steampunkera.screen.filter.FilterScreen;
import com.steampunkera.util.ServoConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public final class ServoPayload {

    // ==================== C2S: Клиент -> Сервер (настройки) ====================
    public record ServoSettings(BlockPos pos, Direction servoSide,
                                boolean enabled,
                                ServoConfig.FilterMode filterMode,
                                ServoConfig.RoutingMode routingMode,
                                int extractInterval,
                                int maxExtract,
                                List<Item> filterItems) implements CustomPayload {

        public static final Id<ServoSettings> ID = new Id<>(Identifier.of(SteampunkEra.MOD_ID, "servo_settings"));
        public static final PacketCodec<RegistryByteBuf, ServoSettings> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, ServoSettings::pos,
                PacketCodecs.indexed(i -> Direction.values()[i], Direction::ordinal), ServoSettings::servoSide,
                PacketCodecs.BOOLEAN, ServoSettings::enabled,
                PacketCodecs.indexed(i -> ServoConfig.FilterMode.values()[i], Enum::ordinal), ServoSettings::filterMode,
                PacketCodecs.indexed(i -> ServoConfig.RoutingMode.values()[i], Enum::ordinal), ServoSettings::routingMode,
                PacketCodecs.INTEGER, ServoSettings::extractInterval,
                PacketCodecs.INTEGER, ServoSettings::maxExtract,
                PacketCodecs.collection(ArrayList::new, PacketCodecs.registryValue(Registries.ITEM.getKey())), ServoSettings::filterItems,
                ServoSettings::new
        );

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }

        public static ServoSettings fromConfig(BlockPos pos, Direction servoSide, boolean enabled, ServoConfig config) {
            return new ServoSettings(pos, servoSide, enabled, config.filterMode(), config.routingMode(),
                    config.extractInterval(), config.maxExtract(), new ArrayList<>(config.filterItems()));
        }

        public ServoConfig toConfig() {
            return new ServoConfig(enabled, filterMode, filterItems, routingMode, extractInterval, maxExtract);
        }
    }

    // ==================== S2C: Сервер -> Клиент (обновление состояния) ====================
    public record ServoUpdate(BlockPos pos, Direction servoSide, boolean enabled, ServoConfig config) implements CustomPayload {

        public static final Id<ServoUpdate> ID = new Id<>(Identifier.of(SteampunkEra.MOD_ID, "servo_update"));
        public static final PacketCodec<RegistryByteBuf, ServoUpdate> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, ServoUpdate::pos,
                PacketCodecs.indexed(i -> Direction.values()[i], Direction::ordinal), ServoUpdate::servoSide,
                PacketCodecs.BOOLEAN, ServoUpdate::enabled,
                PacketCodec.of((cfg, buf) -> buf.writeNbt(cfg.toNbt()), buf -> ServoConfig.fromNbt(buf.readNbt())),
                        ServoUpdate::config,
                ServoUpdate::new
        );

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== C2S: Обновление только filterMode ====================
    public record UpdateFilterMode(BlockPos pos, Direction servoSide, ServoConfig.FilterMode filterMode) implements CustomPayload {

        public static final Id<UpdateFilterMode> ID = new Id<>(Identifier.of(SteampunkEra.MOD_ID, "update_filter_mode"));
        public static final PacketCodec<RegistryByteBuf, UpdateFilterMode> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, UpdateFilterMode::pos,
                PacketCodecs.indexed(i -> Direction.values()[i], Direction::ordinal), UpdateFilterMode::servoSide,
                PacketCodecs.indexed(i -> ServoConfig.FilterMode.values()[i], Enum::ordinal), UpdateFilterMode::filterMode,
                UpdateFilterMode::new
        );

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== Регистрация ====================
    public static void register() {
        // C2S
        PayloadTypeRegistry.playC2S().register(ServoSettings.ID, ServoSettings.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ServoSettings.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.server().getOverworld().getBlockEntity(payload.pos()) instanceof ItemPipeBlockEntity be) {
                    be.setServoConfig(payload.servoSide(), payload.toConfig());
                    // Отправляем подтверждение клиенту
                    ServerPlayNetworking.send(context.player(),
                            new ServoUpdate(payload.pos(), payload.servoSide(), payload.enabled(), payload.toConfig()));
                }
            });
        });

        // C2S: UpdateFilterMode
        PayloadTypeRegistry.playC2S().register(UpdateFilterMode.ID, UpdateFilterMode.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UpdateFilterMode.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.server().getOverworld().getBlockEntity(payload.pos()) instanceof ItemPipeBlockEntity be) {
                    ServoConfig currentConfig = be.getServoConfig(payload.servoSide());
                    ServoConfig newConfig = currentConfig.withFilterMode(payload.filterMode());
                    be.setServoConfig(payload.servoSide(), newConfig);
                    boolean enabled = true;
                    if (context.player().currentScreenHandler instanceof ServoMenu sm) {
                        enabled = sm.isEnabled();
                    } else if (context.player().currentScreenHandler instanceof FilterMenu fm) {
                        enabled = fm.isEnabled();
                    }
                    // Отправляем обновлённую конфигурацию клиенту
                    ServoUpdate update = new ServoUpdate(payload.pos(), payload.servoSide(), enabled, newConfig);
                    ServerPlayNetworking.send(context.player(), update);
                }
            });
        });

        // S2C
        PayloadTypeRegistry.playS2C().register(ServoUpdate.ID, ServoUpdate.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ServoUpdate.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.player().currentScreenHandler instanceof ServoMenu menu) {
                    if (menu.getServoSide() == payload.servoSide()) {
                        menu.setEnabled(payload.enabled());
                        menu.setConfig(payload.config());
                        if (context.client().currentScreen instanceof ServoScreen screen) {
                            screen.updateButton(payload.enabled());
                            screen.updateConfig(payload.config());
                        }
                    }
                } else if (context.player().currentScreenHandler instanceof FilterMenu fMenu) {
                    if (fMenu.getServoSide() == payload.servoSide()) {
                        fMenu.setEnabled(payload.enabled());
                        fMenu.setFilterMode(payload.config().filterMode());
                        if (context.client().currentScreen instanceof FilterScreen fScreen) {
                            fScreen.updateFilterMode(payload.config().filterMode());
                        }
                    }
                }
            });
        });
    }
}
