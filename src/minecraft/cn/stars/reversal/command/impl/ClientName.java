package cn.stars.reversal.command.impl;

import cn.stars.reversal.Reversal;
import cn.stars.reversal.command.Command;
import cn.stars.reversal.command.api.CommandInfo;
import cn.stars.reversal.ui.notification.NotificationType;
import cn.stars.reversal.util.render.ThemeUtil;
import net.minecraft.client.resources.I18n;

@CommandInfo(name = "ClientName", description = "Customize the client name", syntax = ".cn <name/reset>", aliases = {"clientname", "cn"})
public final class ClientName extends Command {

    @Override
    public void onCommand(final String command, final String[] args) throws Exception {
        if (args[0].equals("%reset%")) {
            ThemeUtil.setCustomClientName("");
        } else {
            ThemeUtil.setCustomClientName(String.join(" ", args));
        }
        Reversal.notificationManager.registerNotification(I18n.format("command.ClientName.success", ThemeUtil.getCustomClientName()), I18n.format("command.title"), NotificationType.SUCCESS);
        Reversal.showMsg(I18n.format("command.ClientName.success", ThemeUtil.getCustomClientName()));
    }
}
