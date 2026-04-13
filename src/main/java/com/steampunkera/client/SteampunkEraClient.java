package com.steampunkera.client;

import com.steampunkera.util.ServoMenuData;
import com.steampunkera.screen.servo.ServoScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class SteampunkEraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ServoMenuData.SERVO_MENU_TYPE, ServoScreen::new);
    }
}
