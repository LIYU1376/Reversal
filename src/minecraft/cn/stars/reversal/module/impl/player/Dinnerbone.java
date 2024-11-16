/*
 * Reversal Client - A PVP Client with hack visual.
 * Copyright 2024 Starlight, All rights reserved.
 */
package cn.stars.reversal.module.impl.player;

import cn.stars.reversal.module.Category;
import cn.stars.reversal.module.Module;
import cn.stars.reversal.module.ModuleInfo;
import cn.stars.reversal.value.impl.BoolValue;

@ModuleInfo(name = "Dinnerbone", chineseName = "玩家倒立", description = "Make the player upside down", chineseDescription = "使玩家模型倒立", category = Category.PLAYER)
public class Dinnerbone extends Module {
    public final BoolValue self = new BoolValue("Self", this, true);
}
