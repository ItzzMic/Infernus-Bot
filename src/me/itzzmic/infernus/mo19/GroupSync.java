package me.itzzmic.infernus.mo19;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.itzzmic.infernus.RobloxLink;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GroupSync extends ListenerAdapter{
	
	private Set<Long> antiflood = ConcurrentHashMap.newKeySet();
	private long ids[] = {33606492L,33815673L,34481176L,35480178L,35552377L,33688176L,35552374L,33688172L,34154456L};
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private RobloxLink link;
	
	public GroupSync() {
		link = RobloxLink.getLink();
	}
	
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
		if (e.getName().equalsIgnoreCase("update")) {
			User user = e.getOption("user").getAsUser();
			if (!e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				e.reply("Access Denied").setEphemeral(true).queue();
			}
			e.deferReply(true).queue(reply -> {
				e.getGuild().retrieveMember(user).queue(member -> {
					String name = member.getEffectiveName();
					RobloxLink.IDRequest req = new RobloxLink.IDRequest(name);
					link.getUserID(req).thenAccept(id -> {
						if (id == -1L) {
							reply.editOriginal("Unable to find user").queue();
						return;}
						link.isUserApartOfGroup(id,ids).thenAccept(bo -> {
							for (long l : ids) {
								RoleToGroup group = RoleToGroup.getByGroupID(l);
								Role role = e.getGuild().getRoleById(group.getRoleID());
								if (bo.getOrDefault(l, false)) {
									e.getGuild().addRoleToMember(member, role).queue();
								} else {
									e.getGuild().removeRoleFromMember(member, role).queue();
								}
							}
							reply.editOriginal(member.getAsMention() + " has been updated!").queue();
						});
					});				
				}, err -> {
					reply.editOriginal("This user is not a member").queue();
				});
			});
		return;}
	}
	
	@Override
	public void onButtonInteraction(ButtonInteractionEvent e) {
		if (e.getButton().getCustomId().equalsIgnoreCase("roleme")) {
			if (antiflood.contains(e.getUser().getIdLong())) {
				e.reply("You are on cooldown, please try again later").setEphemeral(true).queue();
			return;}
			long userid = e.getUser().getIdLong();
			antiflood.add(userid);
			Runnable task = () -> {
				antiflood.remove(userid);
			};
			scheduler.schedule(task, 30, TimeUnit.SECONDS);
			e.deferReply(true).queue(reply -> {
				String displayname = e.getMember().getEffectiveName();
				RobloxLink.IDRequest req = new RobloxLink.IDRequest(displayname);
				link.getUserID(req).thenAccept(id -> {
					if (id == -1L) {
						reply.editOriginal("Unable to find user").queue();
					return;}
					link.isUserApartOfGroup(id,ids).thenAccept(bo -> {
						for (long l : ids) {
							RoleToGroup group = RoleToGroup.getByGroupID(l);
							Role role = e.getGuild().getRoleById(group.getRoleID());
							if (bo.getOrDefault(l, false)) {
								e.getGuild().addRoleToMember(e.getMember(), role).queue();
							} else {
								e.getGuild().removeRoleFromMember(e.getMember(), role).queue();
							}
						}
						reply.editOriginal("Your roles have been updated").queue();
					});
				});
			});
		return;}
	}

}
