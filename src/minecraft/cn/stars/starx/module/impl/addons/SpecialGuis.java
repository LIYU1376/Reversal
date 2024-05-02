package cn.stars.starx.module.impl.addons;

import cn.stars.starx.module.Category;
import cn.stars.starx.module.Module;
import cn.stars.starx.module.ModuleInfo;
import cn.stars.starx.setting.impl.BoolValue;

@ModuleInfo(name = "SpecialGuis", description = "Change some mc guis", category = Category.ADDONS)
public class SpecialGuis extends Module {
    private final BoolValue guiInGameMenu = new BoolValue("PauseMenu", this, false);
    private final BoolValue betterButton = new BoolValue("BetterButton", this, false);
}
