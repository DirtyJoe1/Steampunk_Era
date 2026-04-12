package com.steampunkera;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.EnumMap;
import java.util.Map;

public final class SteampunkEraAttachments {

    public record PipeData(
            Map<Direction, Boolean> disabledConnections,
            Map<Direction, Boolean> servoAttachments,
            Map<Direction, ServoConfig> servoConfigs
    ) {
        private static final Direction[] ALL = Direction.values();

        public static final Codec<PipeData> CODEC = new Codec<>() {
            @Override
            public <T> DataResult<Pair<PipeData, T>> decode(DynamicOps<T> ops, T input) {
                if (ops instanceof NbtOps && input instanceof NbtCompound nbt) {
                    Map<Direction, Boolean> disabled = new EnumMap<>(Direction.class);
                    Map<Direction, Boolean> servos = new EnumMap<>(Direction.class);
                    Map<Direction, ServoConfig> configs = new EnumMap<>(Direction.class);

                    for (Direction dir : ALL) {
                        String key = dir.asString();
                        disabled.put(dir, nbt.getBoolean("disabled_" + key).orElse(false));
                        servos.put(dir, nbt.getBoolean("servo_" + key).orElse(false));
                        nbt.getCompound("config_" + key).ifPresentOrElse(
                                cn -> configs.put(dir, ServoConfig.fromNbt(cn)),
                                () -> configs.put(dir, ServoConfig.DEFAULT)
                        );
                    }

                    return DataResult.success(Pair.of(new PipeData(disabled, servos, configs), input));
                }
                return DataResult.success(Pair.of(PipeData.empty(), input));
            }

            @Override
            public <T> DataResult<T> encode(PipeData input, DynamicOps<T> ops, T prefix) {
                if (ops instanceof NbtOps) {
                    NbtCompound nbt = new NbtCompound();
                    for (Direction dir : ALL) {
                        String key = dir.asString();
                        nbt.putBoolean("disabled_" + key, input.disabledConnections.getOrDefault(dir, false));
                        nbt.putBoolean("servo_" + key, input.servoAttachments.getOrDefault(dir, false));
                        ServoConfig config = input.servoConfigs.get(dir);
                        if (config != null && !config.equals(ServoConfig.DEFAULT)) {
                            nbt.put("config_" + key, config.toNbt());
                        }
                    }
                    return DataResult.success((T) nbt);
                }
                return DataResult.success(prefix);
            }
        };

        public static final PacketCodec<RegistryByteBuf, PipeData> PACKET_CODEC = PacketCodec.of(
                SteampunkEraAttachments::encodePacket,
                SteampunkEraAttachments::decodePacket
        );

        public static PipeData empty() {
            Map<Direction, Boolean> d = new EnumMap<>(Direction.class);
            Map<Direction, Boolean> s = new EnumMap<>(Direction.class);
            Map<Direction, ServoConfig> c = new EnumMap<>(Direction.class);
            for (Direction dir : ALL) { d.put(dir, false); s.put(dir, false); c.put(dir, ServoConfig.DEFAULT); }
            return new PipeData(d, s, c);
        }

        public boolean isDisabled(Direction dir) { return disabledConnections.getOrDefault(dir, false); }
        public boolean hasServo(Direction dir) { return servoAttachments.getOrDefault(dir, false); }
        public ServoConfig getServoConfig(Direction dir) { return servoConfigs.getOrDefault(dir, ServoConfig.DEFAULT); }
        public PipeData withDisabled(Direction dir, boolean v) { var m = new EnumMap<>(disabledConnections); m.put(dir, v); return new PipeData(m, servoAttachments, servoConfigs); }
        public PipeData withServo(Direction dir, boolean v) { var m = new EnumMap<>(servoAttachments); m.put(dir, v); return new PipeData(disabledConnections, m, servoConfigs); }
        public PipeData withServoConfig(Direction dir, ServoConfig c) { var m = new EnumMap<>(servoConfigs); m.put(dir, c); return new PipeData(disabledConnections, servoAttachments, m); }
    }

    private static final Direction[] ALL_DIRS = Direction.values();

    private static void encodePacket(PipeData data, RegistryByteBuf buf) {
        for (Direction dir : ALL_DIRS) {
            buf.writeVarInt(Integer.parseInt(dir.getId()));
            buf.writeBoolean(data.disabledConnections().getOrDefault(dir, false));
            buf.writeBoolean(data.servoAttachments().getOrDefault(dir, false));
            buf.writeNbt(data.servoConfigs().getOrDefault(dir, ServoConfig.DEFAULT).toNbt());
        }
    }

    private static PipeData decodePacket(RegistryByteBuf buf) {
        Map<Direction, Boolean> disabled = new EnumMap<>(Direction.class);
        Map<Direction, Boolean> servos = new EnumMap<>(Direction.class);
        Map<Direction, ServoConfig> configs = new EnumMap<>(Direction.class);
        for (int i = 0; i < 6; i++) {
            Direction dir = Direction.values()[buf.readVarInt()];
            disabled.put(dir, buf.readBoolean());
            servos.put(dir, buf.readBoolean());
            var nbt = buf.readNbt();
            configs.put(dir, nbt != null ? ServoConfig.fromNbt(nbt) : ServoConfig.DEFAULT);
        }
        return new PipeData(disabled, servos, configs);
    }

    public static final AttachmentType<PipeData> PIPE_DATA = AttachmentRegistry.create(
            id("pipe_data"),
            builder -> builder.initializer(PipeData::empty).persistent(PipeData.CODEC)
    );

    private static Identifier id(String path) { return Identifier.of(SteampunkEra.MOD_ID, path); }
    public static void init() { AttachmentType<?> dummy = PIPE_DATA; }
}
