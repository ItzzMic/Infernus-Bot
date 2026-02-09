package me.itzzmic.infernus.deployment;

import java.awt.Color;
import java.util.HashSet;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.utils.TimeFormat;

public class Deployment {
	private long userid;
	private String link;
	private String description;
	private HashSet<Role> pings;
	private HashSet<Long> cohosts;
	private Message message;
	private int minutes;
	
	
	public Deployment(long userid) {
		this.userid = userid;
		link = null;
		minutes = 0;
		description = null;
		cohosts = new HashSet<>();
		pings = new HashSet<>(); 
		
	}
	
	public HashSet<Role> getPings(){
		return pings;
	}
	
	public int getMinutes() {
		return minutes;
	}
	
	public void setMinutes(int i) {
		this.minutes = i;
	}
	
	public boolean addCoHost(long id) {
		if (!cohosts.contains(id) && id != userid) {
			cohosts.add(id);
			return true;
		}
		return false;
	}
	
	public void addPing(Role role) {
		pings.add(role);
	}
	
	public MessageEmbed updateMessage(boolean publish) {
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Deployment");
		builder.setColor(Color.decode("#640b0b"));
		builder.addField("Host",UserSnowflake.fromId(userid).getAsMention(),false);
		if (description == null) {
			builder.addField("Description","Not Provided",false);
		} else {
			builder.addField("Description",description,false);
		}
		if (!publish) {
			builder.addField("Starts in",minutes + " minutes",false);
		} else {
			builder.addField("Starts",TimeFormat.RELATIVE.format(System.currentTimeMillis()+(minutes*1000*60)),false);
		}
		if (link != null) {
			builder.addField("Server Link",link,false);
		}
		if (!cohosts.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (Long l : cohosts) {
				sb.append(" ").append(UserSnowflake.fromId(l).getAsMention());
			}
			String cos = sb.toString().replaceFirst(" ","");
			builder.addField("Co-Hosts",cos,false);
		}
		if (!pings.isEmpty() && !publish) {
			StringBuilder sb = new StringBuilder();
			for (Role l : pings) {
				sb.append(" ").append(l.getAsMention());
			}
			String cos = sb.toString().replaceFirst(" ","");
			builder.addField("Pings",cos,false);
		}
		if (!publish) {
			builder.setFooter("Ping any co-hosts you would like to add");
		}
		
		return builder.build();
		
	}
	
	public HashSet<Long> getCoHosts(){
		return cohosts;
	}

	public long getUserID() {
		return userid;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

}
