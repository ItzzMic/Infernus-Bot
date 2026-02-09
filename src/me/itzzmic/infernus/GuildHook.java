package me.itzzmic.infernus;

import java.awt.Color;
import java.util.ArrayList;

import me.itzzmic.infernus.legion.Tier;
import me.itzzmic.infernus.legion.WhitelistManager;
import me.itzzmic.infernus.ticketsystem.EscalationTeam;
import me.itzzmic.infernus.util.OptionPair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class GuildHook extends ListenerAdapter{
	private final Long GUILD_ID;
	private final Long LEGION_ID;
	private boolean isReady;
	private Guild guild;
	private Guild lguild;
	private static GuildHook hook;
	public GuildHook() {
		isReady = false;
		hook = this;
		GUILD_ID = 1418276069831872658L;
		LEGION_ID = 1452010390367109194L;
	}
	
	public boolean isReady() {
		return isReady;
	}
	
	public Guild getMainGuild() {
		return guild;
	}
	
	public Guild getLegionGuild() {
		return lguild;
	}
	
	
	@Override
	public void onReady(ReadyEvent e) {
		Guild guild = e.getJDA().getGuildById(GUILD_ID);
		if (guild == null) {
			e.getJDA().shutdown();
		}
		Guild legion = e.getJDA().getGuildById(LEGION_ID);
		if (legion == null) {
			e.getJDA().shutdown();
		}
		this.guild = guild;
		this.lguild = legion;
		isReady = true;
		Category cat = guild.getCategoryById(1423068237889015988L);
		boolean exists = false;
		for (TextChannel channel : cat.getTextChannels()) {
			if (channel.getName().equalsIgnoreCase("support")) {
				exists = true;
				break;
			}
		}
		if (!exists) {
			cat.createTextChannel("support").queue(channel -> {
				channel.upsertPermissionOverride(guild.getPublicRole()).setDenied(Permission.MESSAGE_SEND,Permission.MESSAGE_ADD_REACTION).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY).queue();
				EmbedBuilder builder = new EmbedBuilder();
				builder.setTitle("Need Support?");
				builder.setColor(Color.decode("#19bacf"));
				builder.setFooter("If you need to get in contact with the support team, click the button below!");
				builder.setDescription("The support team is composed of multiple highly talented individuals ready to solve ANY issue you throw at them!");
				builder.addField("","Here are some of our highly trained teams:",false);
				builder.addField("Moderation Team","The Moderation team will deal with reports against individuals outside of the OCG. They are to deal with discord offenses or other offenses that may result in the removal from the discord.", false);
				builder.addField("Infernus Disciplinary Department","The IDD will deal with reports against individuals inside the OCG, and reserve the power for demotions, suspensions or even permanent removals.",false);
				builder.addField("Senior Moderation Team","The Senior Moderators will deal with reports against the moderators or IDD members.", false);
				builder.addField("Supervisor Team","The Supervisor team will deal with reports against Senior Moderators, and serious issues that normally cant be resolved by the other teams.",false);
				builder.addField("Developer","The Development team will deal with any bugs within the bot, suggestions or anything around the overall integrity of the server or bot.",false);
				builder.addField("Leadership","The Leadership team is the highest entity in Infernus and will deal with reports against supervisors, and the biggest and most complex issues.",false);
				Button button = Button.primary("createticket", "Open a Ticket").withEmoji(Emoji.fromUnicode("🎟️"));
				channel.sendMessageEmbeds(builder.build()).setComponents(ActionRow.of(button)).queue();
				builder = new EmbedBuilder();
				builder.setTitle("Raid Notifications");
				builder.setColor(Color.RED);
				builder.setDescription("If you would like to toggle notifications for raids, please click the button below!");
				button = Button.secondary("togglenotif", "Toggle Raid Notifications").withEmoji(Emoji.fromUnicode("✅"));
				channel.sendMessageEmbeds(builder.build()).setComponents(ActionRow.of(button)).queue();
			});
		}
		exists = false;
		for (TextChannel channel : cat.getTextChannels()) {
			if (channel.getName().equalsIgnoreCase("Specialist-Notice")) {
				exists = true;
				break;
			}
		}
		if (!exists) {
			cat.createTextChannel("Specialist-Notice").queue(channel -> {
				channel.upsertPermissionOverride(guild.getPublicRole()).setDenied(Permission.MESSAGE_SEND,Permission.MESSAGE_ADD_REACTION).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY).queue();
				EmbedBuilder builder = new EmbedBuilder();
				builder.setTitle("Role Granter");
				builder.setColor(Color.GREEN);
				builder.setDescription("If you are apart of a Specialist Divison, please click the button below to be ranked");
				Button button = Button.primary("roleme", "Check Group").withEmoji(Emoji.fromUnicode("👮‍♂️"));
				channel.sendMessageEmbeds(builder.build()).setComponents(ActionRow.of(button)).queue();
			});
		}
		exists = false;
		for (TextChannel channel : cat.getTextChannels()) {
			if (channel.getName().equalsIgnoreCase("Event-Request")) {
				exists = true;
				break;
			}
		}
		if (!exists) {
			cat.createTextChannel("Event-Request").queue(channel -> {
				channel.upsertPermissionOverride(guild.getPublicRole()).setDenied(Permission.MESSAGE_SEND,Permission.MESSAGE_ADD_REACTION).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY).queue();
				EmbedBuilder builder = new EmbedBuilder();
				builder.setTitle("Event Request");
				builder.setColor(Color.BLUE);
				builder.setDescription("Are you a **Division supervisor** or a high ranking member of another OCG?\n\nThis system has been created to simplify the process for requesting an events/deployments with **Infernus**\n\n**If you are looking to request an event with Infernus, please click the button below!**");
				builder.setFooter("Our bot will automatically verify your credentials.");
				Button button = Button.success("openrequest", "Request Event").withEmoji(Emoji.fromUnicode("📄")).asDisabled();
				channel.sendMessageEmbeds(builder.build()).setComponents(ActionRow.of(button)).queue();
			});
		}
		OptionData od = new OptionData(OptionType.CHANNEL,"channel","The Channel to send", false);
		od.setChannelTypes(ChannelType.TEXT,ChannelType.GUILD_NEWS_THREAD,ChannelType.NEWS);
		ArrayList<CommandData> cmds = new ArrayList<>();
		ArrayList<CommandData> lcmds = new ArrayList<>();
		CommandData data = Commands.slash("say", "Send a Message as the Bot")
				.addOption(OptionType.STRING, "msg", "The Message to send", true).addOptions(od);
		cmds.add(data);
		lcmds.add(data);
		data = Commands.slash("retrieveticket", "Retrieve A Ticket from the database")
				.addOption(OptionType.INTEGER, "ticket", "Ticket ID To Retrieve", true);
		cmds.add(data);
		data = Commands.slash("ghostping", "Ghost ping the individual across all channels")
				.addOption(OptionType.USER, "user", "The user to ghost ping",true);
		cmds.add(data);
		exists = false;
		TextChannel txt = null;
		for (TextChannel channel : guild.getCategoryById(1424612474103271434L).getTextChannels()) {
			if (channel.getName().equalsIgnoreCase("Deployment")) {
				exists = true;
				txt = channel;
				break;
			}
		}
		if (exists) {
			for (ThreadChannel channel : txt.getThreadChannels()) {
				channel.delete().queue();
			}
			txt = null;
		}
		if (!exists) {
			guild.getCategoryById(1424612474103271434L).createTextChannel("Deployment").queue(channel -> {
				channel.upsertPermissionOverride(guild.getPublicRole()).setDenied(Permission.VIEW_CHANNEL).queue();
				channel.upsertPermissionOverride(guild.getRoleById(EscalationTeam.SUPERVISOR.getRoleId())).setAllowed(Permission.VIEW_CHANNEL).setDenied(Permission.MESSAGE_SEND).queue();
				channel.upsertPermissionOverride(guild.getRoleById(EscalationTeam.LEADERSHIP.getRoleId())).setAllowed(Permission.VIEW_CHANNEL).setDenied(Permission.MESSAGE_SEND).queue();
				EmbedBuilder builder = new EmbedBuilder();
				builder.setTitle("Host Deployment");
				builder.setDescription("Select the button below to host a deployment!");
				builder.setColor(Color.RED);
				builder.setFooter("You can only prepare one deployment at a time");
				Button button = Button.primary("createdeployment", "Host Deployment").withEmoji(Emoji.fromUnicode("🔋"));
				channel.sendMessageEmbeds(builder.build()).setComponents(ActionRow.of(button)).queue();
			});
		}
		data = Commands.slash("addrank", "Adds a Rank into a Branch")
				.addOption(OptionType.STRING, "branch", "The Branch of the rank",true)
				.addOption(OptionType.ROLE, "role", "The Role to Add",true)
				.addOption(OptionType.ROLE, "promotionrole", "Role that is above this one")
				.addOption(OptionType.ROLE, "demotionrole", "Role that is below this one");
		cmds.add(data);
		data = Commands.slash("movebranch", "Move a Rank to a new Branch")
				.addOption(OptionType.STRING, "branch", "The New branch of the rank",true)
				.addOption(OptionType.ROLE, "role", "The Role to Move",true);
		cmds.add(data);
		data = Commands.slash("modifystructure", "Change a Rank Structure")
				.addOption(OptionType.ROLE, "role", "The Role to Modify",true)
				.addOption(OptionType.ROLE, "promotionrole", "Role that is above this one")
				.addOption(OptionType.ROLE, "demotionrole", "Role that is below this one");
		cmds.add(data);
		data = Commands.slash("getstructure", "Get a Rank Structure")
				.addOption(OptionType.STRING, "branch", "The branch to grab",true);
		cmds.add(data);
		
		exists = false;
		od = new OptionData(OptionType.STRING,"sanctiontype","Sanction Type",true);
		od.addChoice("Warning", "WARNING");
		od.addChoice("Timeout", "TIMEOUT");
		od.addChoice("Kick", "KICK");
		od.addChoice("Temporary Ban", "TEMP_BAN");
		od.addChoice("Permanent Ban", "PERM_BAN");
		od.addChoice("Blacklist", "BLACKLIST");
		data = Commands.slash("sanction", "Issue a Moderation Action")
				.addOption(OptionType.USER, "user", "User to sanction",true).addOptions(od);
		cmds.add(data);
		lcmds.add(data);
		data = Commands.slash("revokesanction", "Revoke a Sanction")
				.addOption(OptionType.INTEGER, "id", "Sanction ID to Revoke",true)
				.addOption(OptionType.STRING, "reason", "Reason to Revoke",true);
		cmds.add(data);
		lcmds.add(data);
		data = Commands.slash("delsanction", "Delete a Sanction")
				.addOption(OptionType.INTEGER, "id", "Sanction ID to Revoke",true);
		cmds.add(data);
		lcmds.add(data);
		SlashCommandData slash = Commands.slash("history", "Retrieve History of a User");
		SubcommandData subcmd = new SubcommandData("user", "Search by User")
				.addOption(OptionType.USER, "user", "User to Search",true);
		slash.addSubcommands(subcmd);
		subcmd = new SubcommandData("id", "Search by ID")
				.addOption(OptionType.STRING, "userid", "User ID to search",true);
		slash.addSubcommands(subcmd);
		cmds.add(slash);
		lcmds.add(slash);
		od = new OptionData(OptionType.CHANNEL, "vc1", "Voice Channel to Transfer From", true);
		od.setChannelTypes(ChannelType.VOICE);
		slash = Commands.slash("transfervc", "Transfer Clusters of Users");
		slash.addOptions(od);
		od = new OptionData(OptionType.CHANNEL, "vc2", "Voice Channel to Transfer To", true);
		od.setChannelTypes(ChannelType.VOICE);
		slash.addOptions(od);
		cmds.add(slash);
		lcmds.add(slash);
		slash = Commands.slash("whitelist", "Modify User's Whitelist status to Legion")
				.addOption(OptionType.USER, "user", "User to Add",true);
		od = new OptionData(OptionType.STRING,"tier","Tier to Add", true);
		od.addChoice("Tier I", "I");
		od.addChoice("Tier II", "II");
		od.addChoice("Tier III", "III");
		od.addChoice("Tier IV", "IV");
		od.addChoice("Revoke", "NONE");
		slash.addOptions(od);
		cmds.add(slash);
		lcmds.add(slash);
		slash = Commands.slash("update", "Update a Users Roles");
		slash.addOption(OptionType.USER, "user", "User to Update",true);
		cmds.add(slash);
		guild.updateCommands().addCommands(cmds).queue();
		lguild.updateCommands().addCommands(lcmds).queue();
		lcmds.clear();
		cmds.clear();
		cmds = null;
		lcmds = null;
		System.out.println("VERSION LOADED SUCCESSFULLY: " + Main.VERSION);
		System.out.println("Started Legion member purge");
		if (lguild.getMemberCount() < 1000) {
			lguild.loadMembers(member -> {
				if (member.getRoles().isEmpty()) {
					WhitelistManager.get().getTier(member.getIdLong()).thenAccept(tier -> {
						if (tier == Tier.NONE) {
							member.kick().queue();
						} else {
							lguild.addRoleToMember(member, lguild.getRoleById(tier.getRoleID())).queue();
						}
					});
				};
			}).onSuccess(v -> {
				lguild.pruneMemberCache();
			});
		} else {
			System.out.println("Start up scan too costly...");
		}
	}
	
	public static GuildHook get() {
		return hook;
	}
	
	public void logCommand(Guild guild, Long userid, String cmdname, OptionPair... pairs) {
		TextChannel channel = guild.getTextChannelById(1423744729530761388L);
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Command Executed");
		builder.setColor(Color.decode("#ff0292"));
		builder.addField("User",UserSnowflake.fromId(userid).getAsMention(), false);
		builder.addField("Command",cmdname,false);
		for (OptionPair pair : pairs) {
			Object value = pair.value();
			String name = pair.optionname();
			if (value instanceof IMentionable) {
				builder.addField(name, ((IMentionable)value).getAsMention(), false);
			} else {
				String v = value.toString();
				if (v.length() > 500) {
					v = v.substring(0,500);
				}
				builder.addField(name,v,false);
			}
		}
		channel.sendMessageEmbeds(builder.build()).queue();
		
	}

}
