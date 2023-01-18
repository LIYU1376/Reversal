package cn.stars.starx.module.impl.render;

import cn.stars.starx.event.impl.PreMotionEvent;
import cn.stars.starx.module.Category;
import cn.stars.starx.module.Module;
import cn.stars.starx.module.ModuleInfo;
import cn.stars.starx.setting.impl.ModeValue;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

@ModuleInfo(name = "Fullbright", description = "Gives your game full brightness", category = Category.RENDER)
public final class Fullbright extends Module {

    private final ModeValue mode = new ModeValue("Mode", this, "Gamma", "Gamma", "NightVision");

    private float oldGamma;

    @Override
    protected void onDisable() {
        switch (mode.getMode()) {
            case "Gamma":
                mc.gameSettings.gammaSetting = oldGamma;
                break;

            case "NightVision":
                mc.thePlayer.removePotionEffect(Potion.nightVision.getId());
                break;
        }
    }

    @Override
    protected void onEnable() {
        oldGamma = mc.gameSettings.gammaSetting;
    }

    @Override
    public void onPreMotion(final PreMotionEvent event) {
        switch (mode.getMode()) {
            case "Gamma":
                mc.gameSettings.gammaSetting = 100;
                break;

            case "NightVision":
                mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.getId(), Integer.MAX_VALUE));
                break;
        }
    }
}
