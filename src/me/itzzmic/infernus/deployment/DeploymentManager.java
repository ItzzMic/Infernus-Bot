package me.itzzmic.infernus.deployment;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.DefaultValue;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;

public class DeploymentManager extends ListenerAdapter{
	private HashMap<ThreadChannel,Deployment> memory;
	private static DeploymentManager mgr;
	public DeploymentManager() {
		mgr = this;
		memory = new HashMap<>();
	}
	
	public static DeploymentManager get() {
		return mgr;
	}
	
	public boolean doesUserHaveDeployment(long userid) {
		for (Deployment deploy : memory.values()) {
			if (deploy.getUserID() == userid) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onModalInteraction(ModalInteractionEvent e) {
		if (e.getModalId().equalsIgnoreCase("addfields")) {
			ThreadChannel channel = (ThreadChannel) e.getChannel();
			if (channel == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			Deployment deploy = memory.get(channel);
			if (deploy == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			String desc = e.getValue("description").getAsString();
			deploy.setDescription(desc);
			List<String> roles = e.getValue("roles").getAsStringList();
			for (String st : roles) {
				deploy.addPing(e.getGuild().getRoleById(st));
			}
			deploy.getMessage().editMessageEmbeds(deploy.updateMessage(false)).queue();
			e.reply("Fields added successfully!").setEphemeral(true).queue();
		return;}
		if (e.getModalId().equalsIgnoreCase("linkhandler")) {
			ThreadChannel channel = (ThreadChannel) e.getChannel();
			if (channel == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			Deployment deploy = memory.get(channel);
			if (deploy == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			String link = e.getValue("link").getAsString();
			deploy.setLink(link);
			deploy.getMessage().editMessageEmbeds(deploy.updateMessage(false)).queue();
			e.reply("Link attached Successfully").setEphemeral(true).queue();
		return;}
		if (e.getModalId().equalsIgnoreCase("timehandler")) {
			ThreadChannel channel = (ThreadChannel) e.getChannel();
			if (channel == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			Deployment deploy = memory.get(channel);
			if (deploy == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			String time = e.getValue("time").getAsString();
			int ti;
			try {
				ti = Integer.parseInt(time);
			} catch (Exception ex) {
				ti = 0;
			}
			deploy.setMinutes(ti);
			deploy.getMessage().editMessageEmbeds(deploy.updateMessage(false)).queue();
			e.reply("Timestamp added").setEphemeral(true).queue();
		return;}
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		if (!e.isFromGuild() || e.getAuthor().isBot() || e.getAuthor().isSystem()) {
			return;
		}
		if (e.getChannel() instanceof ThreadChannel) {
			ThreadChannel channel = (ThreadChannel) e.getChannel();
			Deployment deploy = memory.get(channel);
			if (deploy == null) {return;}
			List<Member> members = e.getMessage().getMentions().getMembers();
			if (!members.isEmpty()) {
				boolean ex = false;
				for (Member mem : members) {
					ex = (deploy.addCoHost(mem.getIdLong()) || ex);
				}
				if (ex) {
					e.getMessage().addReaction(Emoji.fromUnicode("👍")).queue();
				}
				deploy.getMessage().editMessageEmbeds(deploy.updateMessage(false)).queue();
			}
		return;}
	}
	
	@Override
	public void onButtonInteraction(ButtonInteractionEvent e) {
		if (e.getButton().getCustomId().equalsIgnoreCase("createdeployment")) {
			long userid = e.getUser().getIdLong();
			if (doesUserHaveDeployment(userid)) {
				e.reply("You already have a Deployment Thread open!").setEphemeral(true).queue();
			return;}
			createDeployment(e.getUser().getIdLong(),(TextChannel) e.getChannel());
			e.reply("Request sent to start deployment").setEphemeral(true).queue();
		return;}
		if (e.getButton().getCustomId().equalsIgnoreCase("link")) {
			ThreadChannel channel = (ThreadChannel) e.getChannel();
			Deployment deploy = memory.get(channel);
			if (deploy == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			TextInput input = TextInput.create("link", TextInputStyle.PARAGRAPH)
					.setMinLength(1).setMaxLength(1000).setPlaceholder("Please provide a link for your deployment").build();
			Modal modal = Modal.create("linkhandler", "Adding a Server Link")
					.addComponents(Label.of("Server Link", input)).build();
			e.replyModal(modal).queue();
		return;}
		if (e.getButton().getCustomId().equalsIgnoreCase("time")) {
			ThreadChannel channel = (ThreadChannel) e.getChannel();
			Deployment deploy = memory.get(channel);
			if (deploy == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			TextInput input = TextInput.create("time", TextInputStyle.PARAGRAPH)
					.setMinLength(1).setMaxLength(1000).setPlaceholder("Please provide the time for your deployment in minutes!").build();
			Modal modal = Modal.create("timehandler", "Adding a Server Link")
					.addComponents(Label.of("Time in Minutes", input)).build();
			e.replyModal(modal).queue();
		return;}
		if (e.getButton().getCustomId().equalsIgnoreCase("cancel")) {
			ThreadChannel channel = (ThreadChannel) e.getChannel();
			Deployment deploy = memory.get(channel);
			if (deploy == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			memory.remove(channel);
			channel.delete().queueAfter(5, TimeUnit.SECONDS);
			e.reply("Deployment Canceled").queue();
		return;}
		if (e.getButton().getCustomId().equalsIgnoreCase("submit")) {
			ThreadChannel channel = (ThreadChannel) e.getChannel();
			Deployment deploy = memory.get(channel);
			if (deploy == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			memory.remove(channel);
			channel.delete().queueAfter(5, TimeUnit.SECONDS);
			e.reply("Deployment Submitted!").queue();
			MessageEmbed msg = deploy.updateMessage(true);
			TextChannel chan = e.getGuild().getTextChannelsByName("scheduling", true).get(0);
			chan.sendMessageEmbeds(msg).queue(a -> {
				a.reply(e.getGuild().getRoleById(1441209795062268039L).getAsMention()).queue();
				MessageChannel raids = (MessageChannel) e.getGuild().getGuildChannelById(1422634628786819164L);
				EmbedBuilder builder = new EmbedBuilder();
				builder.setColor(Color.RED);
				builder.setTitle("INFERNUS IS RAIDING");
				builder.setDescription("Link: **" + ((deploy.getLink() == null) ? "NO LINK PROVIDED**" : deploy.getLink()) + "**");
				raids.sendMessageEmbeds(builder.build()).queueAfter(deploy.getMinutes(), TimeUnit.MINUTES, ms -> {
					if (!deploy.getPings().isEmpty()) {
						StringBuilder sb = new StringBuilder();
					for (Role role : deploy.getPings()) {
						sb.append(" ").append(role.getAsMention());
					}
					String cos = sb.toString().replaceFirst(" ","");
					ms.reply(cos).queue();
					}
				});
			});
		return;}
		if (e.getButton().getCustomId().equalsIgnoreCase("addfields")) {
			ThreadChannel channel = (ThreadChannel) e.getChannel();
			Deployment deploy = memory.get(channel);
			if (deploy == null) {
				e.reply("An error occured!").setEphemeral(true).queue();
			return;}
			TextInput input = TextInput.create("description", TextInputStyle.PARAGRAPH)
					.setMinLength(1).setMaxLength(250).setPlaceholder("Please provide a description for your deployment").build();
			Collection<DefaultValue> defaults = new ArrayList<>();
			defaults.add(DefaultValue.role(1452950475757326449L));
			EntitySelectMenu menu = EntitySelectMenu.create("roles", SelectTarget.ROLE)
					.setMinValues(1).setMaxValues(25).setDefaultValues(defaults).build();
			Modal modal = Modal.create("addfields", "Adding Deployment Fields")
					.addComponents(Label.of("Description", input),Label.of("Roles to Ping", menu)).build();
			e.replyModal(modal).queue();
		return;}
	}
	
	public void createDeployment(long userid,TextChannel channel) {
		channel.createThreadChannel(UUID.randomUUID().toString(), true).queue(thread -> {
			memory.put(thread, new Deployment(userid));
			thread.addThreadMemberById(userid).queue();
			MessageEmbed msg = memory.get(thread).updateMessage(false);
			Button modify = Button.primary("addfields", "Add Fields").withEmoji(Emoji.fromUnicode("📰"));
			Button roles = Button.secondary("link", "Add Server Link").withEmoji(Emoji.fromUnicode("📎"));
			Button time = Button.secondary("time", "Set Deployment Time").withEmoji(Emoji.fromUnicode("⏰"));
			Button cancel = Button.danger("cancel", "Cancel Deployment").withEmoji(Emoji.fromUnicode("❌"));
			Button submit = Button.success("submit", "Submit Deployment").withEmoji(Emoji.fromUnicode("✅"));
			thread.sendMessageEmbeds(msg).addComponents(ActionRow.of(modify,roles,time,cancel,submit)).queue(a -> {
				memory.get(thread).setMessage(a);
			});
		});
	}
	

}
