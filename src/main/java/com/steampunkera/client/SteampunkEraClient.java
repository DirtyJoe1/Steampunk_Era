package com.steampunkera.client;

import com.steampunkera.screen.filter.FilterMenuData;
import com.steampunkera.screen.filter.FilterScreen;
import com.steampunkera.screen.servo.ServoMenuData;
import com.steampunkera.screen.servo.ServoScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class SteampunkEraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ServoMenuData.SERVO_MENU_TYPE, ServoScreen::new);
        HandledScreens.register(FilterMenuData.FILTER_MENU_TYPE, FilterScreen::new);
    }
}