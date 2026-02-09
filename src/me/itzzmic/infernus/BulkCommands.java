package me.itzzmic.infernus;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import me.itzzmic.infernus.util.OptionPair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class BulkCommands extends ListenerAdapter{
	private boolean disabled = true;
		
	
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
		if (e.getName().equalsIgnoreCase("transfervc")){
			Member member = e.getMember();
			if (!member.hasPermission(Permission.ADMINISTRATOR)) {
				e.reply("Access denied").setEphemeral(true).queue();
			return;}
			VoiceChannel vc1 = (VoiceChannel) e.getOption("vc1").getAsChannel();
			VoiceChannel vc2 = (VoiceChannel) e.getOption("vc2").getAsChannel();
			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(Color.BLUE);
			builder.setTitle("NOTICE OF MOVE");
			builder.setDescription("Stand by! You are being moved to " + vc2.getAsMention());
			builder.setFooter("You are in the queue!");
			for (Member mem : vc1.getMembers()) {
				mem.getUser()
				   .openPrivateChannel()
				   .flatMap(ch -> ch.sendMessageEmbeds(builder.build()))
				   .queue(
				       success -> {},
				       failure -> {}   
				   );
			}
			for (Member mem : vc1.getMembers()) {
				e.getGuild().moveVoiceMember(mem, vc2).queue();
			}
			e.reply("Successfully Queued").setEphemeral(true).queue();
		}
		if (e.getName().equalsIgnoreCase("say")) {
			Member member = e.getMember();
			if (!member.hasPermission(Permission.ADMINISTRATOR)) {
				e.reply("Access Denied!").setEphemeral(true).queue();
			return;}
			String msg = e.getOption("msg").getAsString();
			MessageChannel channel = (e.getOption("channel") == null) ? e.getGuildChannel() : (MessageChannel) e.getOption("channel").getAsChannel();
			channel.sendMessage(msg).queue();
			e.reply("Message sent successfully!").setEphemeral(true).queue();
			GuildHook.get().logCommand(e.getGuild(), e.getUser().getIdLong(), "say", new OptionPair("Message",msg), new OptionPair("Channel", channel));
		return;}
		if (e.getName().equalsIgnoreCase("ghostping")) {
			if (disabled) {
				e.reply("Command has been disabled due to reputational concerns").setEphemeral(true).queue();
			return;}
			Member member = e.getMember();
			if (!member.hasPermission(Permission.ADMINISTRATOR)) {
				e.reply("Access Denied!").setEphemeral(true).queue();
			return;}
			User user = e.getOption("user").getAsUser();
			GuildHook.get().logCommand(e.getGuild(), member.getIdLong(), "ghostping", new OptionPair("User Pinged",user));
			for (Category cat : e.getGuild().getCategories()) {
				for (GuildChannel channel : cat.getChannels()) {
					if (channel instanceof MessageChannel) {
						((MessageChannel) channel).sendMessage(user.getAsMention()).queue(a -> {
							a.delete().queueAfter(1, TimeUnit.SECONDS);
						});
					}
				}
			}
			e.reply("Successfully Ghost Pinged User...").setEphemeral(true).queue();
		return;}
	}

}
