package me.itzzmic.infernus.moderation;

import java.awt.Color;
import java.util.ArrayList;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.UserSnowflake;

public class UserSanctionHistory {
	private boolean more;
	private long userid;
	private long highest;
	private long lowest;
	private ArrayList<Sanction> sanctions;
	
	public UserSanctionHistory(long userid, boolean more, long highest, long lowest, ArrayList<Sanction> sanctions) {
		this.more = more;
		this.userid = userid;
		this.highest = highest;
		this.lowest = lowest;
		this.sanctions = sanctions;
	}
	
	public boolean doesHistoryHaveNextPage() {
		return more;
	}
	
	public long getHighest() {
		return highest;
	}
	
	public long getLowest() {
		return lowest;
	}
	
	public ArrayList<Sanction> getSanctions(){
		return sanctions;
	}
	
	public MessageEmbed getMessage() {
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Sanction History");
		builder.setFooter("Confidential");
		builder.setColor(Color.decode("294003"));
		builder.addField("User",UserSnowflake.fromId(userid).getAsMention() + "\n",false);
		if (sanctions.isEmpty()) {
			builder.addField("","**NO HISTORY FOUND**",false);
			return builder.build();
		}
		for (Sanction s: sanctions) {
			builder.addField("[" + s.getID() + "] " + s.getType().getName(),"**Reason:** " + s.getReason() + "\n **By:** " + UserSnowflake.fromId(s.getModID()).getAsMention(),false);
		}
		return builder.build();
	}

}
