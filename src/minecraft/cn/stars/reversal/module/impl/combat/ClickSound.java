/*
 * Reversal Client - A PVP Client with hack visual.
 * Copyright 2024 Starlight, All rights reserved.
 */
package cn.stars.reversal.module.impl.combat;

import cn.stars.reversal.event.impl.ClickEvent;
import cn.stars.reversal.module.Category;
import cn.stars.reversal.module.Module;
import cn.stars.reversal.module.ModuleInfo;
import cn.stars.reversal.setting.impl.ModeValue;
import cn.stars.reversal.setting.impl.NumberValue;
import cn.stars.reversal.util.misc.SoundUtil;
import org.apache.commons.lang3.RandomUtils;

@ModuleInfo(name = "ClickSound", chineseName = "点击声音", description = "Play sound when you click", chineseDescription = "当你点击时播放声音", category = Category.COMBAT)
public class ClickSound extends Module {
    private final ModeValue type = new ModeValue("Type", this, "Normal", "Normal", "Jitter", "Double");
    private final NumberValue volume = new NumberValue("Volume", this, 0.5, 0.1, 2, 0.1);
    private final NumberValue variation = new NumberValue("Variation", this, 5, 0, 100, 1);

    @Override
    public void onClick(ClickEvent event) {
        if (event.getType() != ClickEvent.ClickType.MIDDLE) {
            switch (type.getMode()) {
                case "Normal":
                    SoundUtil.playSound("reversal.click.nc", (float) volume.getValue(), RandomUtils.nextFloat(1.0F, 1 + (float) variation.getValue() / 100f));
                    break;
                case "Jitter":
                    SoundUtil.playSound("reversal.click.jc", (float) volume.getValue(), RandomUtils.nextFloat(1.0F, 1 + (float) variation.getValue() / 100f));
                    break;
                case "Double":
                    SoundUtil.playSound("reversal.click.dbc", (float) volume.getValue(), RandomUtils.nextFloat(1.0F, 1 + (float) variation.getValue() / 100f));
                    break;
            }
        }
    }
}
