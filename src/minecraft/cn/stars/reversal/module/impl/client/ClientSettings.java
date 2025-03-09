package cn.stars.reversal.module.impl.client;

import cn.stars.reversal.Reversal;
import cn.stars.reversal.value.impl.*;
import cn.stars.addons.rawinput.RawMouseHelper;
import cn.stars.reversal.event.impl.ValueChangedEvent;
import cn.stars.reversal.module.Category;
import cn.stars.reversal.module.Module;
import cn.stars.reversal.module.ModuleInfo;
import cn.stars.reversal.util.ReversalLogger;
import net.minecraft.util.MouseHelper;
import net.optifine.Lang;

import java.awt.*;

@ModuleInfo(name = "ClientSettings", localizedName = "module.ClientSettings.name", description = "Some settings to change your hud.",
        localizedDescription = "module.ClientSettings.desc", category = Category.CLIENT)
public final class ClientSettings extends Module {
    public final NoteValue note1 = new NoteValue("< COLOR SETTINGS >", "value.ClientSettings.note1", this);
    public final ModeValue theme = new ModeValue("Theme", "value.ClientSettings.theme", this, "Simple",
            "Minecraft", "Reversal", "Modern", "Simple", "Empathy");
    public final ModeValue colorType = new ModeValue("Color Type", "value.ClientSettings.colorType", this, "Rainbow", "Rainbow", "Double", "Fade", "Static");
    public final ColorValue color1 = new ColorValue("Color 1", "value.ClientSettings.color1", this, new Color(20,250,255), true);
    public final ColorValue color2 = new ColorValue("Color 2", "value.ClientSettings.color2", this, new Color(20,250,255), true);
    public final NumberValue indexTimes = new NumberValue("Index Times",  "value.ClientSettings.indexTimes",this, 1, 1, 10, 0.1);
    public final NumberValue indexSpeed = new NumberValue("Index Speed", "value.ClientSettings.indexSpeed",this, 1, 1, 5, 0.1);

    public final NoteValue note2 = new NoteValue("< SPECIFIC SETTINGS >", "value.ClientSettings.note2", this);
    public final ModeValue listAnimation = new ModeValue("List Animation", "value.ClientSettings.listAnimation", this, "Reversal", "Reversal", "Slide");
    public final BoolValue thunderHack = new BoolValue("Thunder Hack", "value.ClientSettings.thunderHack", this, false);
    public final BoolValue empathyGlow = new BoolValue("Empathy Glow", "value.ClientSettings.empathyGlow", this, false);

    public final NoteValue note3 = new NoteValue("< CLIENT SETTINGS >", "value.ClientSettings.note3", this);
    public final BoolValue localization = new BoolValue("Localization", "value.ClientSettings.localization", this, true);
    public final ModeValue language = new ModeValue("Language", "value.ClientSettings.language", this, "English", "English", "Chinese");
    public final BoolValue showNotifications = new BoolValue("Show Notifications", "value.ClientSettings.showNotifications", this, true);
    public final BoolValue hudTextWithBracket = new BoolValue("Hud Text With Bracket", "value.ClientSettings.hudTextWithBracket", this, false);
    public final BoolValue clientMsgCustomName = new BoolValue("Client Message Custom Name", "value.ClientSettings.clientMsgCustomName", this, false);

    public final NoteValue note4 = new NoteValue("< MINECRAFT SETTINGS >", "value.ClientSettings.note4", this);
    public final BoolValue loadingScreenBg = new BoolValue("Loading Screen Background", "value.ClientSettings.loadingScreenBg", this, false);
    public final BoolValue rawInput = new BoolValue("Raw Input", "value.ClientSettings.rawInput", this, false);

    public ClientSettings() {
    }

    @Override
    public void onUpdateAlways() {
        Reversal.CLIENT_THEME_COLOR = color1.getColor().getRGB();
        Reversal.CLIENT_THEME_COLOR_BRIGHT = new Color(Math.min(color1.getColor().getRed(), 255), Math.min(color1.getColor().getGreen() + 45, 255), Math.min(color1.getColor().getBlue() + 13, 255)).hashCode();
        Reversal.CLIENT_THEME_COLOR_2 = color2.getColor().getRGB();
        Reversal.CLIENT_THEME_COLOR_BRIGHT_2 = new Color(Math.min(color2.getColor().getRed(), 255), Math.min(color2.getColor().getGreen() + 45, 255), Math.min(color2.getColor().getBlue() + 13, 255)).hashCode();

        thunderHack.hidden = !theme.getMode().equals("Modern");
        empathyGlow.hidden = !theme.getMode().equals("Empathy");
        language.hidden = !localization.enabled;

        if (this.enabled) this.enabled = false;
    }

    @Override
    public void onValueChanged(ValueChangedEvent event) {
        // For Raw Input
        if (event.setting == rawInput) {
            if (rawInput.enabled) {
                mc.mouseHelper = new RawMouseHelper();
                ReversalLogger.info("Switched mc.mouseHelper to RawMouseHelper.");
            }
            else {
                mc.mouseHelper = new MouseHelper();
                ReversalLogger.info("Switched mc.mouseHelper to MouseHelper.");
            }
        }
        if (event.setting == language) {
            Lang.resourcesReloaded();
        }
    }

    @Override
    public void onLoad() {
        if (rawInput.enabled) {
            mc.mouseHelper = new RawMouseHelper();
            ReversalLogger.info("Switched mc.mouseHelper to RawMouseHelper.");
        }
        else {
            mc.mouseHelper = new MouseHelper();
            ReversalLogger.info("Switched mc.mouseHelper to MouseHelper.");
        }
    }
}
