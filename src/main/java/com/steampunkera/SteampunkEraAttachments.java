package com.steampunkera;

import com.steampunkera.block.ItemPipeBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Типы аттачментов для блоков.
 */
public final class SteampunkEraAttachments {

    /**
     * Данные о disabled-соединениях, сервоприводах и их состоянии для трубы.
     */
    public record PipeData(
            Map<Direction, Boolean> disabledConnections,
            Map<Direction, Boolean> servoAttachments,
            Map<Direction, Boolean> servoActive
    ) {
        public static final Codec<PipeData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        directionBoolMapCodec().fieldOf("disabled").forGetter(PipeData::disabledConnections),
                        directionBoolMapCodec().fieldOf("servos").forGetter(PipeData::servoAttachments),
                        directionBoolMapCodec().fieldOf("servo_active").forGetter(PipeData::servoActive)
                ).apply(instance, PipeData::new)
        );

        public static final PacketCodec<RegistryByteBuf, PipeData> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.map(HashMap::new, DirectionPacketCodec.INSTANCE, PacketCodecs.BOOLEAN),
                PipeData::disabledConnections,
                PacketCodecs.map(HashMap::new, DirectionPacketCodec.INSTANCE, PacketCodecs.BOOLEAN),
                PipeData::servoAttachments,
                PacketCodecs.map(HashMap::new, DirectionPacketCodec.INSTANCE, PacketCodecs.BOOLEAN),
                PipeData::servoActive,
                PipeData::new
        );

        public static PipeData empty() {
            Map<Direction, Boolean> disabled = new EnumMap<>(Direction.class);
            Map<Direction, Boolean> servos = new EnumMap<>(Direction.class);
            Map<Direction, Boolean> active = new EnumMap<>(Direction.class);
            for (Direction dir : Direction.values()) {
                disabled.put(dir, false);
                servos.put(dir, false);
                active.put(dir, true);
            }
            return new PipeData(disabled, servos, active);
        }

        public boolean isDisabled(Direction dir) {
            return disabledConnections.getOrDefault(dir, false);
        }

        public boolean hasServo(Direction dir) {
            return servoAttachments.getOrDefault(dir, false);
        }

        public boolean isServoActive(Direction dir) {
            return servoActive.getOrDefault(dir, true);
        }

        public PipeData withDisabled(Direction dir, boolean value) {
            Map<Direction, Boolean> newMap = new EnumMap<>(disabledConnections);
            newMap.put(dir, value);
            return new PipeData(newMap, servoAttachments, servoActive);
        }

        public PipeData withServo(Direction dir, boolean value) {
            Map<Direction, Boolean> newMap = new EnumMap<>(servoAttachments);
            newMap.put(dir, value);
            return new PipeData(disabledConnections, newMap, servoActive);
        }

        public PipeData withServoActive(Direction dir, boolean value) {
            Map<Direction, Boolean> newMap = new EnumMap<>(servoActive);
            newMap.put(dir, value);
            return new PipeData(disabledConnections, servoAttachments, newMap);
        }
    }

    private static Codec<Map<Direction, Boolean>> directionBoolMapCodec() {
        return Codec.unboundedMap(
                Direction.CODEC,
                Codec.BOOL
        ).xmap(
                map -> {
                    EnumMap<Direction, Boolean> result = new EnumMap<>(Direction.class);
                    for (Direction dir : Direction.values()) {
                        result.put(dir, map.getOrDefault(dir, false));
                    }
                    return result;
                },
                map -> map
        );
    }

    /**
     * Кастомный PacketCodec для Direction.
     */
    private enum DirectionPacketCodec implements PacketCodec<RegistryByteBuf, Direction> {
        INSTANCE;

        @Override
        public void encode(RegistryByteBuf buf, Direction value) {
            buf.writeString(value.name());
        }

        @Override
        public Direction decode(RegistryByteBuf buf) {
            return Direction.byId(buf.readString());
        }
    }

    /**
     * Аттачмент для хранения данных трубы (disabled + servo + servo active).
     */
    public static final AttachmentType<PipeData> PIPE_DATA = AttachmentRegistry.create(
            id("pipe_data"),
            builder -> builder
                    .initializer(PipeData::empty)
                    .persistent(PipeData.CODEC)
    );

    private static Identifier id(String path) {
        return Identifier.of(SteampunkEra.MOD_ID, path);
    }

    /**
     * Вызывается из SteampunkEra.onInitialize() для регистрации аттачментов.
     */
    public static void init() {
        AttachmentType<?> dummy = PIPE_DATA;
    }
}
