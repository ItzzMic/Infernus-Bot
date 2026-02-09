package me.itzzmic.infernus.moderation;

public record SimplifiedMessage(long getMessageID, long getChannelID, long getAuthorID, String getContent, long getTimestamp) {

}
