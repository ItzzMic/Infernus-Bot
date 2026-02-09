package me.itzzmic.infernus.ticketsystem;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import me.itzzmic.infernus.Database;
import me.itzzmic.infernus.GuildHook;
import me.itzzmic.infernus.util.OptionPair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.TimeFormat;

public class TicketManager extends ListenerAdapter{
	private Database db;
	public TicketManager() {
		db = Database.get();
	}
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
		if (e.getName().equalsIgnoreCase("retrieveticket")){
			Member member = e.getMember();
			if (getClearance(e.getGuild(), member) == e.getGuild().getPublicRole()) {
				e.reply("Access Denied").setEphemeral(true).queue();
				return;}
			int a = e.getOption("ticket").getAsInt();
			GuildHook.get().logCommand(e.getGuild(), member.getIdLong(), e.getName(), new OptionPair("TicketID",a));
			e.deferReply(true).queue(reply -> {
				getTicket(a).thenAccept(ticket -> {
					if (ticket == null) {
						reply.editOriginal("Unable to find a ticket by that id!").queue();
						return;}
					EmbedBuilder builder = new EmbedBuilder();
					boolean closed = (ticket.getCloser() != 0);
					builder.setTitle("Ticket-" + ticket.getID());
					builder.addField("Ticket Opened By",UserSnowflake.fromId(ticket.getOpener()).getAsMention(), false);
					builder.addField("Reason for Ticket", ticket.getReason(), false);
					builder.addField("Team Responsible",ticket.getEscalationTeam().getName(),false);
					if (closed) {
						builder.setColor(Color.RED);
						builder.addField("Status","Closed",false);
						builder.addField("Closed By",UserSnowflake.fromId(ticket.getCloser()).getAsMention(),false);
						builder.addField("Close Reason",ticket.getClosereason(),false);
					} else {
						builder.setColor(Color.GREEN);
						builder.addField("Status","Open",false);
					}
					builder.addField("Created",TimeFormat.RELATIVE.format(ticket.getTimestamp()),false);
					reply.editOriginalEmbeds(builder.build()).queue();	
				});
			});
		return;}
	}
	
	
	
	@Override
	public void onModalInteraction(ModalInteractionEvent e) {
		if (e.getModalId().equalsIgnoreCase("ticketcreator")) {
			e.deferReply(true).queue(reply -> {
				String reason = e.getValue("ticketreason").getAsString();
				long userid = e.getUser().getIdLong();
				long timestamp = System.currentTimeMillis();
				EscalationTeam escal = EscalationTeam.valueOf(e.getValue("team").getAsStringList().get(0));
				Category cat = e.getGuild().getCategoryById(1423068237889015988L);
				Ticket tick = new Ticket(-1L,userid,reason,timestamp);
				tick.setEscalationTeam(escal);
				saveTicket(tick).thenAccept(ticket -> {
					ArrayList<Permission> deny = new ArrayList<>();
					deny.add(Permission.VIEW_CHANNEL);
					cat.createTextChannel("Ticket-" + ticket.getID()).addPermissionOverride(e.getGuild().getPublicRole(), null, deny).queue(channel -> {
						Member member = e.getMember();
						channel.upsertPermissionOverride(member).setAllowed(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
						if (ticket.getEscalationTeam() == EscalationTeam.SUPPORT) {
							for (EscalationTeam team : EscalationTeam.values()) {
								channel.upsertPermissionOverride(e.getGuild().getRoleById(team.getRoleId())).setAllowed(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
							}
						} else {
							updatePermissions(channel, ticket.getEscalationTeam(), e.getGuild());
						}
						EmbedBuilder builder = new EmbedBuilder();
						builder.setTitle("Ticket-" + ticket.getID());
						builder.setFooter("The Infernus support team reserves the right to close this ticket for any reason!");
						builder.setColor(Color.decode("#0FBF75"));
						builder.addField("Ticket Opener", UserSnowflake.fromId(ticket.getOpener()).getAsMention(), false);
						builder.addField("Opened", TimeFormat.RELATIVE.format(ticket.getTimestamp()),false);
						builder.addField("Ticket Reason", ticket.getReason(), false);
						Button button = Button.primary("openticketpanel", "Open Ticket Panel")
								.withEmoji(Emoji.fromUnicode("⚙️"));
						channel.sendMessageEmbeds(builder.build()).setComponents(ActionRow.of(button)).queue(x -> {
							x.reply(e.getGuild().getRoleById(ticket.getEscalationTeam().getRoleId()).getAsMention() + " " + e.getMember().getAsMention()).queue();
						});
						reply.editOriginal("Successfully created Ticket: " + channel.getAsMention()).queue();
					});
				});
				});
		return;}
		if (e.getModalId().equalsIgnoreCase("ticketcloser")) {
			e.deferReply(true).queue(reply -> {
				getTicket(Long.parseLong(e.getChannel().getName().replace("ticket-", ""))).thenAccept(ticket -> {
					if (ticket.getOpener() == e.getUser().getIdLong()) {
						reply.editOriginal("You can't close your own ticket!").queue();
					return;}
					if (ticket.getCloser() != 0) {
						reply.editOriginal("This ticket has been closed").queue();
					return;}
					ticket.setCloser(e.getMember().getIdLong());
					String closereason = e.getValue("closereason").getAsString();
					ticket.setClosereason(closereason);
					updateTicket(ticket);
					e.getChannel().sendMessage("This ticket has been closed by " + e.getMember().getAsMention() + ", and will be deleted in 3s").queue(a -> {
						e.getChannel().delete().queueAfter(3, TimeUnit.SECONDS);
					});
					e.getJDA().retrieveUserById(ticket.getOpener()).queue(member -> {
						member.openPrivateChannel().queue(channel -> {
							EmbedBuilder builder = new EmbedBuilder();
							builder.setTitle("Ticket-" + ticket.getID());
							builder.setColor(Color.GREEN);
							builder.addField("Ticket Closed By", e.getMember().getAsMention(), false);
							builder.addField("Ticket Closure Reason", closereason,false);
							channel.sendMessageEmbeds(builder.build()).queue(suc -> {
								System.out.println("All Checks Passed, Ticket Closed!");
								// Channel Creation has multiple error points, all are caught however Soft fail - no further action
							}, fail -> {
								System.out.println("Failed to msg user");
							});
						}, fail -> {
							System.out.println("Failed to msg user");
						});
					}, fail -> {
						System.out.println("Failed to msg user");
					});
					reply.editOriginal("Ticket closed successfully!").queue();
				});
			});
		return;}
		if (e.getModalId().equalsIgnoreCase("ticketescalator")) {
			e.deferReply(true).queue(reply -> {
				getTicket(Long.parseLong(e.getChannel().getName().replace("ticket-", ""))).thenAccept(ticket -> {
					if (ticket.getOpener() == e.getUser().getIdLong()) {
						reply.editOriginal("You can't escalate your own ticket!").queue();
					return;}
					if (ticket.getCloser() != 0) {
						reply.editOriginal("This ticket has been closed").queue();
					return;}
					Member member = e.getMember();
					String reason = e.getValue("escalationreason").getAsString();
					EscalationTeam team = EscalationTeam.valueOf( e.getValue("team").getAsStringList().get(0));
					if (ticket.getEscalationTeam() == team) {
						reply.editOriginal("This ticket is already escalated to this team!").queue();
					return;}
					ticket.setEscalationTeam(team);
					updateTicket(ticket);
					updatePermissions((TextChannel) e.getChannel(),team,e.getGuild());
					Category esca = e.getGuild().getCategoryById(1423159841295433799L);
					if (e.getGuildChannel().asTextChannel().getParentCategory() != esca) {
						e.getChannel().asTextChannel().getManager().setParent(esca).queue();
					}
					EmbedBuilder builder = new EmbedBuilder();
					builder.setTitle("Ticket has been Escalated!");
					builder.setColor(Color.decode("#4f24bf"));
					builder.addField("Escalated by", member.getAsMention(), false);
					builder.addField("Escalation reason", reason,false);
					builder.addField("Escalated to", team.getName(), false);
					reply.editOriginal("Ticket Escalated Successfully").queue();
					e.getChannel().sendMessageEmbeds(builder.build()).queue(a -> {
						a.reply(e.getGuild().getRoleById(team.getRoleId()).getAsMention()).queue();
					});
				});
			});
		}
	}
	
	
	public void updatePermissions(TextChannel channel, EscalationTeam team,Guild guild) {
		if (team == EscalationTeam.LEADERSHIP) {
			for (EscalationTeam teams : EscalationTeam.values()) {
				if (teams != team) {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setDenied(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();

				}
			}
		return;}
		if (team == EscalationTeam.IDD_LEAD) {
			for (EscalationTeam teams : EscalationTeam.values()) {
				if (teams != team && teams != EscalationTeam.LEADERSHIP && teams != EscalationTeam.DEVELOPER) {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setDenied(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				} else {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setAllowed(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				}
			}
		}
		if (team == EscalationTeam.DEVELOPER) {
			
			for (EscalationTeam teams : EscalationTeam.values()) {
				if (teams != team && teams != EscalationTeam.LEADERSHIP) {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setDenied(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				} else {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setAllowed(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				}
			}
		return;}
		if (team == EscalationTeam.SUPERVISOR) {
			
			for (EscalationTeam teams : EscalationTeam.values()) {
				if (teams != team && teams != EscalationTeam.LEADERSHIP && teams != EscalationTeam.DEVELOPER && teams != EscalationTeam.IDD_LEAD) {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setDenied(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				} else {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setAllowed(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				}
			}
		return;}
		if (team == EscalationTeam.SMOD) {
			
			for (EscalationTeam teams : EscalationTeam.values()) {
				if (teams == EscalationTeam.SUPPORT || teams == EscalationTeam.MODERATION || teams == EscalationTeam.IDD) {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setDenied(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				} else {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setAllowed(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				}
			}
		return;}
		if (team == EscalationTeam.IDD) {
			for (EscalationTeam teams : EscalationTeam.values()) {
				if (teams != EscalationTeam.LEADERSHIP && teams != team && teams != EscalationTeam.IDD_LEAD) {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setDenied(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				} else {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setAllowed(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				}
			}
		}
		if (team == EscalationTeam.MODERATION) {
			for (EscalationTeam teams : EscalationTeam.values()) {
				if (teams == EscalationTeam.SUPPORT) {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setDenied(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				} else {
					channel.upsertPermissionOverride(guild.getRoleById(teams.getRoleId())).setAllowed(Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY,Permission.MESSAGE_SEND).queue();
				}
			}
		}
		
	}
	
	
	public Role getClearance(Guild guild, Member member) {
		EscalationTeam[] ls = EscalationTeam.values();
		for (int i = ls.length-1; i >= 0; i--) {
			Role role = guild.getRoleById(ls[i].getRoleId());
			if (member.getRoles().contains(role)) {
				return role;
			}
		}
		return guild.getPublicRole();
		
	}
	
	
	
	@Override
	public void onButtonInteraction(ButtonInteractionEvent e) {
		Button button = e.getButton();
		if (button.getCustomId().equalsIgnoreCase("createticket")){
			TextInput input = TextInput.create("ticketreason", TextInputStyle.PARAGRAPH)
					.setPlaceholder("Please provide a detailed reason on why you are opening a ticket.").setMinLength(10).setMaxLength(1000).build();
			StringSelectMenu menu = StringSelectMenu.create("team")
					.addOption("General Question", "SUPPORT")
					.addOption("Report OCG Member", "IDD")
					.addOption("Report Discord Member", "MODERATION").setRequired(true).setDefaultOptions(SelectOption.of("General Question", "SUPPORT")).build();
			Modal modal = Modal.create("ticketcreator", "Creating a Ticket")
					.addComponents(Label.of("Ticket Reason", input),Label.of("Ticket Reason", menu)).build();
			e.replyModal(modal).queue();
		return;}
		if (button.getCustomId().equalsIgnoreCase("openticketpanel")) {
			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle("TICKET PANEL");
			builder.setColor(Color.RED);
			builder.addField("Clearance Level", getClearance(e.getGuild(),e.getMember()).getAsMention(),false);
			builder.setFooter("Please Select an Option Below");
			Button close = Button.primary("closeticket", "Close Ticket").withEmoji(Emoji.fromUnicode("❌"));
			Button escalate = Button.success("escalateticket", "Escalate Ticket").withEmoji(Emoji.fromUnicode("⏫"));
			Button troll = Button.danger("trollticket", "Mark Ticket as Troll Ticket").withEmoji(Emoji.fromUnicode("🚨"));
			e.replyEmbeds(builder.build()).setComponents(ActionRow.of(close,escalate,troll)).setEphemeral(true).queue();
		return;}
		if (button.getCustomId().equalsIgnoreCase("closeticket")) {
			TextInput input = TextInput.create("closereason", TextInputStyle.PARAGRAPH)
					.setPlaceholder("Please provide the reason for why the ticket is being closed!").setMinLength(1).setMaxLength(500).build();
			Modal modal = Modal.create("ticketcloser", "Closing a Ticket")
					.addComponents(Label.of("Close Reason", input)).build();
			e.replyModal(modal).queue();
		return;}
		if (button.getCustomId().equalsIgnoreCase("escalateticket")) {
			TextInput input = TextInput.create("escalationreason", TextInputStyle.PARAGRAPH)
					.setPlaceholder("Please provide the reason for why the ticket is being escalated!").setMinLength(1).setMaxLength(500).build();
			StringSelectMenu menu = StringSelectMenu.create("team")
					.addOption("Moderation Team", "MODERATION", Emoji.fromUnicode("🔨"))
					.addOption("Infernus Disiplinary Department", "IDD", Emoji.fromUnicode("🚓"))
					.addOption("Senior Moderation Team", "SMOD",Emoji.fromUnicode("⚒️"))
					.addOption("Supervisor Team", "SUPERVISOR",Emoji.fromUnicode("📋"))
					.addOption("IDD Lead", "IDD_LEAD",Emoji.fromUnicode("♦️"))
					.addOption("Developer", "DEVELOPER",Emoji.fromUnicode("💻"))
					.addOption("Leadership", "LEADERSHIP", Emoji.fromUnicode("💎"))
					.setMinValues(1).setMaxValues(1).build();
			Modal modal = Modal.create("ticketescalator", "Escalating a Ticket")
					.addComponents(Label.of("Escalation Reason", input),Label.of("Escalation Team", menu)).build();
			e.replyModal(modal).queue();
		return;}
		if (button.getCustomId().equalsIgnoreCase("trollticket")) {
			e.deferReply(true).queue();
			getTicket(Long.parseLong(e.getChannel().getName().replace("ticket-", ""))).thenAccept(ticket -> {
				if (ticket.getCloser() != 0) {
					e.getHook().editOriginal("This ticket has been closed").queue();
				return;}
				ticket.setCloser(e.getMember().getIdLong());
				ticket.setClosereason("Troll Ticket");
				updateTicket(ticket);
				TextChannel channel = (TextChannel) e.getChannel();
				channel.sendMessage("This ticket has been marked as a troll ticket by " + e.getMember().getAsMention() + "!").onSuccess(x -> {
					channel.delete().queueAfter(10, TimeUnit.SECONDS);
					e.getGuild().retrieveMemberById(ticket.getOpener()).queue(member -> {
						if (!member.hasPermission(Permission.ADMINISTRATOR)) {
							member.timeoutFor(30, TimeUnit.MINUTES).queue();
							e.getHook().editOriginal("User has been sanctioned for making a troll ticket").queue();
						} else {
							e.getHook().editOriginal("User is an administrator, unable to time out").queue();
						}
					}, fail -> {
						System.out.println("User not found, ignoring...");
						e.getHook().editOriginal("User not found, ignoring and closing ticket...").queue();
					});
				}).queue();
			});
		return;}
	}
	
	
	public CompletableFuture<Boolean> doesUserHaveTicketOpen(Long id) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("SELECT TicketID FROM Tickets WHERE TicketOpener=? AND TicketCloser=0");
				ps.setLong(1, id);
				ResultSet set = ps.executeQuery();
				boolean exists = set.next();
				set.close();
				ps.close();
				future.complete(exists);
			} catch (Exception e) {
				e.printStackTrace();
				//Default to true
				future.complete(true);
			} finally {
				db.offerConnection(c);
			}
		});
		return future;
	}
	
	public void updateTicket(Ticket ticket) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
			 	PreparedStatement ps = c.prepareStatement("UPDATE Tickets SET TicketCloser=?, TicketCloseReason=?, EscalationTeam=? WHERE TicketID=?");
			 	ps.setLong(1, ticket.getCloser());
			 	ps.setString(2, ticket.getClosereason());
			 	ps.setString(3, ticket.getEscalationTeam().toString());
			 	ps.setLong(4, ticket.getID());
			 	ps.execute();
			 	ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				db.offerConnection(c);
			}
		});
	}
	
	public CompletableFuture<Ticket> saveTicket(Ticket ticket) {
		CompletableFuture<Ticket> future = new CompletableFuture<>();
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
					PreparedStatement ps = c.prepareStatement("INSERT INTO Tickets(TicketOpener, TicketReason, TicketCloser, TicketCloseReason, EscalationTeam, Timestamp)  VALUES (?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
					ps.setLong(1, ticket.getOpener());
					ps.setString(2, ticket.getReason());
					ps.setLong(3, ticket.getCloser());
					ps.setString(4, ticket.getClosereason());
					ps.setString(5, ticket.getEscalationTeam().toString());
					ps.setLong(6, ticket.getTimestamp());
					ps.execute();
					ResultSet set = ps.getGeneratedKeys();
					set.next();
					long id = set.getLong(1);
					ticket.changeID(id);
					set.close();
					ps.close();
					future.complete(ticket);
			} catch (Exception e) {
				//Error - Print at runtime
				e.printStackTrace();
				future.complete(null);
			}finally {
				db.offerConnection(c);
			}
		});
		return future;
	}
	
	public CompletableFuture<Ticket> getTicket(long id) {
		CompletableFuture<Ticket> future = new CompletableFuture<>();
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("SELECT * FROM Tickets WHERE TicketID=?");
				ps.setLong(1, id);
				ResultSet set = ps.executeQuery();
				if (!set.next()) {
					set.close();
					ps.close();
					future.complete(null);
					return;
				}
				long tickid = set.getLong("TicketID");
				long tickopen = set.getLong("TicketOpener");
				String reason = set.getString("TicketReason");
				long tickclose = set.getLong("TicketCloser");
				String closere = set.getString("TicketCloseReason");
				EscalationTeam team = EscalationTeam.valueOf(set.getString("EscalationTeam"));
				Long time = set.getLong("Timestamp");
				Ticket ticket = new Ticket(tickid,tickopen,reason,time);
				ticket.setEscalationTeam(team);
				if (tickclose != 0) {
					ticket.setCloser(tickclose);
					ticket.setClosereason(closere);
				}
				set.close();
				ps.close();
				future.complete(ticket);
			} catch (Exception e) {
				future.complete(null);
			}finally {
				db.offerConnection(c);
			}
		});
		return future;
	}

}
