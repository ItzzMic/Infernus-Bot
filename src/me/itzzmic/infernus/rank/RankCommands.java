package me.itzzmic.infernus.rank;

import java.awt.Color;

import me.itzzmic.infernus.GuildHook;
import me.itzzmic.infernus.util.OptionPair;
import me.itzzmic.infernus.util.RankManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RankCommands extends ListenerAdapter{
	private RankManager mgr;
	public RankCommands() {
		mgr = RankManager.get();
	}
	
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
		if (e.getName().equalsIgnoreCase("addrank")) {
			Member member = e.getMember();
			if (!member.hasPermission(Permission.MANAGE_ROLES)) {
				e.reply("Access Denied").setEphemeral(true).queue();
			return;}
			e.deferReply(true).queue(reply -> {
				String branch = e.getOption("branch").getAsString();
				Role role = e.getOption("role").getAsRole();
				Role promote = (e.getOption("promotionrole") != null) ? e.getOption("promotionrole").getAsRole() : null;
				Role demote = (e.getOption("demotionrole") != null) ? e.getOption("demotionrole").getAsRole() : null;
				mgr.addRank(role, demote, promote, role.getName(), branch);
				EmbedBuilder builder = new EmbedBuilder();
				builder.setColor(Color.GREEN);
				builder.setTitle("Added Rank to Database");
				builder.addField("Role",role.getAsMention(),false);
				builder.addField("Branch",branch,false);
				builder.addField("Promotion Rank", (promote == null) ? "NONE" : promote.getAsMention(),false);
				builder.addField("Demotion Rank", (demote == null) ? "NONE" : demote.getAsMention(),false);
				builder.setFooter("Remember these commands directly modify rank heiarchy, Only use this command if you know what you are doing.");
				reply.editOriginalEmbeds(builder.build()).queue();
				GuildHook.get().logCommand(e.getGuild(), member.getIdLong(), "addrank", new OptionPair("Role",role), new OptionPair("Branch", branch),
						new OptionPair("Promotion Rank", (promote == null) ? "NONE" : promote),
						new OptionPair("Demotionn Rank", (demote == null) ? "NONE" : demote)
						);
				
			});
		return;}
		if (e.getName().equalsIgnoreCase("modifystructure")) {
			Member member = e.getMember();
			if (!member.hasPermission(Permission.MANAGE_ROLES)) {
				e.reply("Access Denied").setEphemeral(true).queue();
			return;}
			e.deferReply(true).queue(reply -> {
				Role role = e.getOption("role").getAsRole();
				Role promote = (e.getOption("promotionrole") != null) ? e.getOption("promotionrole").getAsRole() : null;
				Role demote = (e.getOption("demotionrole") != null) ? e.getOption("demotionrole").getAsRole() : null;
				mgr.modifyEntry(role, demote, promote);
				EmbedBuilder builder = new EmbedBuilder();
				builder.setColor(Color.BLUE);
				builder.setTitle("Modified Structure");
				builder.addField("Role",role.getAsMention(),false);
				builder.addField("Promotion Rank", (promote == null) ? "NONE" : promote.getAsMention(),false);
				builder.addField("Demotion Rank", (demote == null) ? "NONE" : demote.getAsMention(),false);
				builder.setFooter("Remember these commands directly modify rank heiarchy, Only use this command if you know what you are doing.");
				reply.editOriginalEmbeds(builder.build()).queue();
				GuildHook.get().logCommand(e.getGuild(), member.getIdLong(), "modifystructure", new OptionPair("Role",role),
						new OptionPair("Promotion Rank", (promote == null) ? "NONE" : promote),
						new OptionPair("Demotionn Rank", (demote == null) ? "NONE" : demote)
						);
				
			});
		return;}
	}

}
