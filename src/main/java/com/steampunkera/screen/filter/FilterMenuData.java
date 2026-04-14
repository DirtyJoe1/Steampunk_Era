package com.steampunkera.screen.filter;

import com.steampunkera.SteampunkEra;
import com.steampunkera.util.ServoConfig;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class FilterMenuData {

    public record FilterData(BlockPos pos, Direction servoSide, boolean enabled, ServoConfig.FilterMode filterMode, int mouseX, int mouseY) {
        public static final PacketCodec<RegistryByteBuf, FilterData> PACKET_CODEC = new PacketCodec<>() {
            @Override
            public FilterData decode(RegistryByteBuf buf) {
                BlockPos pos = buf.readBlockPos();
                Direction servoSide = Direction.values()[buf.readInt()];
                boolean enabled = buf.readBoolean();
                ServoConfig.FilterMode filterMode = ServoConfig.FilterMode.values()[buf.readInt()];
                int mouseX = buf.readInt();
                int mouseY = buf.readInt();
                return new FilterData(pos, servoSide, enabled, filterMode, mouseX, mouseY);
            }

            @Override
            public void encode(RegistryByteBuf buf, FilterData data) {
                buf.writeBlockPos(data.pos());
                buf.writeInt(data.servoSide().ordinal());
                buf.writeBoolean(data.enabled());
                buf.writeInt(data.filterMode().ordinal());
                buf.writeInt(data.mouseX());
                buf.writeInt(data.mouseY());
            }
        };
    }

    public static final ExtendedScreenHandlerType<FilterMenu, FilterData> FILTER_MENU_TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            id("filter_menu"),
            new ExtendedScreenHandlerType<>(
                    (syncId, inventory, data) -> new FilterMenu(syncId, inventory, data.pos(), data.servoSide(), data.enabled(), data.filterMode(), data.mouseX(), data.mouseY()),
                    FilterData.PACKET_CODEC
            )
    );

    private static Identifier id(String path) {
        return Identifier.of(SteampunkEra.MOD_ID, path);
    }

    public static void init() {
        ExtendedScreenHandlerType<?, ?> dummy = FILTER_MENU_TYPE;
    }
}
