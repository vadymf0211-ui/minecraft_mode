package com.vadym.gravitygun.client;

import com.vadym.gravitygun.GravityGunMod;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class GravityGunClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HandledScreens.register(GravityGunMod.CARDBOARD_BOX_SCREEN_HANDLER, CardboardBoxScreen::new);
    }
}
