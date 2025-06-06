package cn.stars.reversal.module.impl.player;

import cn.stars.reversal.Reversal;
import cn.stars.reversal.event.impl.PacketReceiveEvent;
import cn.stars.reversal.event.impl.UpdateEvent;
import cn.stars.reversal.module.Category;
import cn.stars.reversal.module.Module;
import cn.stars.reversal.module.ModuleInfo;
import cn.stars.reversal.ui.notification.NotificationType;
import cn.stars.reversal.util.MiscUtil;
import cn.stars.reversal.util.math.TimeUtil;
import cn.stars.reversal.value.impl.TextValue;
import net.minecraft.network.play.server.S02PacketChat;

@ModuleInfo(name = "AutoGG", localizedName = "module.AutoGG.name", description = "Auto send gg when game end", localizedDescription = "module.AutoGG.desc", category = Category.PLAYER)
public class AutoGG extends Module {
    public final TextValue message = new TextValue("Message", this, "gg");
    String[] winMessage = new String[] {"Winner", "第一名", "1st", "胜利"};
    String[] exceptMessage = new String[] {"任务", "初尝"};
    TimeUtil timeUtil = new TimeUtil();
    private boolean isWinning = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.packet instanceof S02PacketChat) {
            String message = ((S02PacketChat) event.packet).getChatComponent().getUnformattedText();
            if (MiscUtil.containsAnyIgnoreCase(message, winMessage) && !MiscUtil.containsAnyIgnoreCase(message, exceptMessage) && !isWinning) {
                isWinning = true;
                mc.thePlayer.sendChatMessage(this.message.getText());
                Reversal.notificationManager.registerNotification("Sent GG at chat!", "AutoGG", 3000L, NotificationType.SUCCESS, 5);
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (timeUtil.hasReached(5000L)) {
            isWinning = false;
            timeUtil.reset();
        }
    }
}
