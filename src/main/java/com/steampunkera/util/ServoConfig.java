package com.steampunkera.util;

import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * Конфигурация сервопривода для извлечения предметов.
 */
public record ServoConfig(
        boolean enabled,
        FilterUtil.FilterMode filterMode,
        List<Item> filterItems,
        RoutingMode routingMode,
        int extractInterval,
        int maxExtract
) {
    public enum RoutingMode { NEAREST_FIRST, FURTHEST_FIRST, ROUND_ROBIN }

    public static final ServoConfig DEFAULT = new ServoConfig(
            true,
            FilterUtil.FilterMode.BLACKLIST, 
            new ArrayList<>(),
            RoutingMode.NEAREST_FIRST, 
            20, 
            8
    );

    public ServoConfig {
        filterItems = new ArrayList<>(filterItems);
    }

    public static ServoConfig fromNbt(NbtCompound nbt) {
        boolean enabled = nbt.getBoolean("enabled").orElse(true);
        FilterUtil.FilterMode filterMode = FilterUtil.FilterMode.valueOf(
            nbt.getString("filterMode").orElse("BLACKLIST")
        );
        RoutingMode routingMode = RoutingMode.valueOf(
            nbt.getString("routingMode").orElse("NEAREST_FIRST")
        );
        int extractInterval = nbt.getInt("extractInterval").orElse(20);
        int maxExtract = nbt.getInt("maxExtract").orElse(8);
        
        FilterUtil.FilterData filterData = FilterUtil.readFromNbt(nbt);
        
        return new ServoConfig(
            enabled, 
            filterData.filterMode(), 
            filterData.filterItems(), 
            routingMode, 
            extractInterval, 
            maxExtract
        );
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("enabled", enabled);
        nbt.putString("routingMode", routingMode.name());
        nbt.putInt("extractInterval", extractInterval);
        nbt.putInt("maxExtract", maxExtract);
        FilterUtil.writeToNbt(nbt, filterMode, filterItems);
        return nbt;
    }

    /**
     * Проверяет, проходит ли предмет фильтрацию сервопривода.
     */
    public boolean matchesFilter(Item item) {
        return FilterUtil.matchesFilter(item, filterMode, filterItems);
    }

    // Builder-методы для иммутабельных изменений
    public ServoConfig withEnabled(boolean v) { 
        return new ServoConfig(v, filterMode, filterItems, routingMode, extractInterval, maxExtract); 
    }
    
    public ServoConfig withFilterMode(FilterUtil.FilterMode m) { 
        return new ServoConfig(enabled, m, filterItems, routingMode, extractInterval, maxExtract); 
    }
    
    public ServoConfig withRoutingMode(RoutingMode m) { 
        return new ServoConfig(enabled, filterMode, filterItems, m, extractInterval, maxExtract); 
    }
    
    public ServoConfig withExtractInterval(int v) { 
        return new ServoConfig(enabled, filterMode, filterItems, routingMode, v, maxExtract); 
    }
    
    public ServoConfig withMaxExtract(int v) { 
        return new ServoConfig(enabled, filterMode, filterItems, routingMode, extractInterval, v); 
    }
    
    public ServoConfig withFilterItems(List<Item> items) { 
        return new ServoConfig(enabled, filterMode, items, routingMode, extractInterval, maxExtract); 
    }
}