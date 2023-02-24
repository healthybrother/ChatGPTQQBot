package com.chatgptbot;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.NewFriendRequestEvent;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.BotConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author healthyin
 * @version BotService 2023/2/17
 */
public class QQBotService {
    private static Bot qqBot;

    private static Config gtpConfig;

    private static Map<Long, ChatBot> chatBotMap = new HashMap<>();

     static {
        qqBot = BotFactory.INSTANCE.newBot(111, "***", new BotConfiguration() {{
            setProtocol(MiraiProtocol.ANDROID_WATCH);
            fileBasedDeviceInfo("device.json");
        }});
        qqBot.login();
        gtpConfig = new Config();
        gtpConfig.setAccess_token("access_token");}

    private static boolean filterMessage(MessageChain msgChain) {
        boolean flag = false;
        for (SingleMessage msg : msgChain.stream().filter(At.class::isInstance).collect(Collectors.toList())) {
            At at = (At) msg;
            if (at.getTarget() == qqBot.getId()) {
                flag = true;
                break;
            }
        }
        QuoteReply quoteReply = msgChain.get(QuoteReply.Key);
        if (null != quoteReply) {
            if(quoteReply.getSource().getFromId() == qqBot.getId() && quoteReply.getSource().getTargetId() != qqBot.getId()) {
                flag = true;
            }
        }
        return flag;
    }

    private static String getPlantText(MessageChain msgChain) {
        StringBuffer sb = new StringBuffer();
        for (SingleMessage msg : msgChain.stream().filter(PlainText.class::isInstance).collect(Collectors.toList())) {
            PlainText plainText = (PlainText) msg;
            sb.append(plainText.contentToString());
        }
        return sb.toString();
    }

    public static void sendMessage(Group group, Contact contact, MessageChain srcMessage, String replyMessage) {
         if (null != group) {
             MessageChain chain = new MessageChainBuilder()
                     .append(new QuoteReply(srcMessage))
                     .append(new At(contact.getId()))
                     .append(replyMessage)
                     .build();
             group.sendMessage(chain);
         } else {
             contact.sendMessage(replyMessage);
         }
    }

    public static void listenMsgAndCallback() {
        qqBot.getEventChannel().subscribeAlways(Event.class, event -> {
            if (event instanceof GroupMessageEvent) {
                Group group = ((GroupMessageEvent) event).getGroup();
                Member contact = ((GroupMessageEvent) event).getSender();
                MessageChain msgChain = ((GroupMessageEvent)event).getMessage();
                if(filterMessage(msgChain)) {
                    String text = getPlantText(msgChain);
                    ChatBot bot = chatBotMap.computeIfAbsent(group.getId(), key -> new ChatBot(gtpConfig));
                    bot.getChatResponseAndSendAsync(text, group, contact, msgChain);
                }
            }
            if (event instanceof FriendMessageEvent) {
                Friend friend = ((FriendMessageEvent) event).getSender();
                MessageChain msgChain = ((FriendMessageEvent) event).getMessage();
                String text = getPlantText(msgChain);
                ChatBot bot = chatBotMap.computeIfAbsent(friend.getId(), key -> new ChatBot(gtpConfig));
                bot.getChatResponseAndSendAsync(text, null, friend, msgChain);
            }
            //自动同意好友
            if (event instanceof NewFriendRequestEvent) {
                ((NewFriendRequestEvent)event).accept();
            }
        });
    }

    public static void main(String[] args) {
        listenMsgAndCallback();
    }
}
