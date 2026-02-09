package me.itzzmic.infernus.moderation;

public record Sanction(long getID, long getUserID, long getModID, String getReason, String getEvidence, SanctionType getType, long getDuration) {

}
