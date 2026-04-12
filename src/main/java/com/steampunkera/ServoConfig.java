package com.steampunkera;

import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record ServoConfig(
        boolean enabled,
        FilterMode filterMode,
        List<Item> filterItems,
        RoutingMode routingMode,
        int extractInterval,
        int maxExtract
) {
    public enum FilterMode { BLACKLIST, WHITELIST }
    public enum RoutingMode { NEAREST_FIRST, FURTHEST_FIRST, ROUND_ROBIN }

    public static final ServoConfig DEFAULT = new ServoConfig(
            true,
            FilterMode.BLACKLIST, new ArrayList<>(),
            RoutingMode.NEAREST_FIRST, 60, 8);

    public static ServoConfig fromNbt(NbtCompound nbt) {
        boolean en = nbt.getBoolean("enabled").orElse(true);
        FilterMode fm = FilterMode.valueOf(nbt.getString("filterMode").orElse("BLACKLIST"));
        RoutingMode rm = RoutingMode.valueOf(nbt.getString("routingMode").orElse("NEAREST_FIRST"));
        int ei = nbt.getInt("extractInterval").orElse(60);
        int me = nbt.getInt("maxExtract").orElse(8);
        List<Item> items = new ArrayList<>();
        nbt.getList("filterItems").ifPresent(list -> {
            for (int i = 0; i < list.size(); i++) {
                list.getString(i).ifPresent(idStr -> {
                    Identifier id = Identifier.tryParse(idStr);
                    if (id != null) {
                        Item item = Registries.ITEM.get(id);
                        items.add(item);
                    }
                });
            }
        });
        return new ServoConfig(en, fm, items, rm, ei, me);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("enabled", enabled);
        nbt.putString("filterMode", filterMode.name());
        nbt.putString("routingMode", routingMode.name());
        nbt.putInt("extractInterval", extractInterval);
        nbt.putInt("maxExtract", maxExtract);
        NbtList list = new NbtList();
        for (Item item : filterItems) {
            list.add(NbtString.of(Registries.ITEM.getId(item).toString()));
        }
        nbt.put("filterItems", list);
        return nbt;
    }

    public boolean matchesFilter(Item item) {
        if (filterItems.isEmpty()) return true;
        boolean inList = filterItems.contains(item);
        return (filterMode == FilterMode.BLACKLIST) != inList;
    }

    public ServoConfig withEnabled(boolean v) { return new ServoConfig(v, filterMode, filterItems, routingMode, extractInterval, maxExtract); }
    public ServoConfig withFilterMode(FilterMode m) { return new ServoConfig(enabled, m, filterItems, routingMode, extractInterval, maxExtract); }
    public ServoConfig withRoutingMode(RoutingMode m) { return new ServoConfig(enabled, filterMode, filterItems, m, extractInterval, maxExtract); }
    public ServoConfig withExtractInterval(int v) { return new ServoConfig(enabled, filterMode, filterItems, routingMode, v, maxExtract); }
    public ServoConfig withMaxExtract(int v) { return new ServoConfig(enabled, filterMode, filterItems, routingMode, extractInterval, v); }
    public ServoConfig withFilterItems(List<Item> items) { return new ServoConfig(enabled, filterMode, items, routingMode, extractInterval, maxExtract); }
}
