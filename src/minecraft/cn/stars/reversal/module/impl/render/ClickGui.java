/*
 Copyright Alan Wood 2021
 None of this code to be reused without my written permission
 Intellectual Rights owned by Alan Wood
 */
package cn.stars.reversal.module.impl.render;

import cn.stars.reversal.Reversal;
import cn.stars.reversal.module.Category;
import cn.stars.reversal.module.Module;
import cn.stars.reversal.module.ModuleInfo;
import cn.stars.reversal.value.impl.ModeValue;
import cn.stars.reversal.value.impl.NumberValue;
import org.lwjgl.input.Keyboard;

@ModuleInfo(name = "ClickGui", localizedName = "点击界面", description = "Opens a Gui where you can toggle modules and change their settings",
        localizedDescription = "显示一个可以让你管理功能的界面", category = Category.RENDER, defaultKey = Keyboard.KEY_RSHIFT)
public final class ClickGui extends Module {

    private final ModeValue mode = new ModeValue("Mode", this, "Modern", "Modern");
    public final NumberValue scrollSpeed = new NumberValue("Scroll Speed", this, 4.0, 0.5, 10.0, 1.0);

    @Override
    protected void onEnable() {
        switch (mode.getMode()) {
            case "MomoTalk": {
                mc.displayGuiScreen(Reversal.mmtClickGUI);
                break;
            }

            case "Modern": {
                mc.displayGuiScreen(Reversal.modernClickGUI);
                break;
            }
        }

        this.setEnabled(false);
        Reversal.saveAll();
    }

    @Override
    protected void onDisable() {
        Reversal.saveAll();
    }
}
