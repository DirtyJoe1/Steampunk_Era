package com.steampunkera;

import com.steampunkera.screen.ServoMenu;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ServoMenuData {

    public static final ScreenHandlerType<ServoMenu> SERVO_MENU_TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            id("servo_menu"),
            new ScreenHandlerType<>(ServoMenu::new, FeatureFlags.VANILLA_FEATURES)
    );

    private static Identifier id(String path) {
        return Identifier.of(SteampunkEra.MOD_ID, path);
    }

    public static void init() {
        ScreenHandlerType<?> dummy = SERVO_MENU_TYPE;
    }
}
