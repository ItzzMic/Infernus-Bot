package me.itzzmic.infernus.moderation;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import me.itzzmic.infernus.Database;
import me.itzzmic.infernus.GuildHook;
import me.itzzmic.infernus.ticketsystem.EscalationTeam;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.TimeFormat;

public class ModerationHandler extends ListenerAdapter{
	private Database db;
	
	public ModerationHandler() {
		db = Database.get();
		
		 Runnable task = () -> {
			 Connection c = db.borrowConnection();
			 try {
			 if (GuildHook.get() == null || !GuildHook.get().isReady()) {
				 return;
			 }
			 Guild guild = GuildHook.get().getMainGuild();
			 Guild lguild = GuildHook.get().getLegionGuild();
			 
				 PreparedStatement ps = c.prepareStatement("SELECT ID,UserID FROM Sanctions WHERE SanctionType='TEMP_BAN' AND DURATION<=?");
				 ps.setLong(1, System.currentTimeMillis());
				 ResultSet set = ps.executeQuery();
				 while (set.next()) {
					 submitUpdate(set.getLong("id"),SanctionType.EXPIRED_BAN);
					 long userid = set.getLong("UserID");
					 guild.unban(UserSnowflake.fromId(set.getLong("UserID"))).queue(suc -> {
						 System.out.println("User unbanned due to expired ban: " + userid);
					 }, err -> {
						System.out.println("User not banned in main: " + userid); 
					 });
					 lguild.unban(UserSnowflake.fromId(set.getLong("UserID"))).queue(suc -> {
						 System.out.println("User unbanned due to expired ban: " + userid);
					 }, err -> {
						System.out.println("User not banned in Legion: " + userid); 
					 });
				 }
				 set.close();
				 ps.close();
			 } catch (Throwable e) {
				 e.printStackTrace();
			 }finally {
					db.offerConnection(c);
				}
		 };
		 
		 Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
		 
	}
	
	public boolean canMemberUseSanction(Member member, SanctionType type, Guild guild) {
		List<Role> roles = member.getRoles();
		Role support = guild.getRoleById(EscalationTeam.SUPPORT.getRoleId());
		Role mod = guild.getRoleById(EscalationTeam.MODERATION.getRoleId());
		Role srmod = guild.getRoleById(EscalationTeam.SMOD.getRoleId());
		Role supe = guild.getRoleById(EscalationTeam.SUPERVISOR.getRoleId());
		if (type.getTeam() == EscalationTeam.SUPPORT) {
			return (member.hasPermission(Permission.ADMINISTRATOR) || roles.contains(support) || roles.contains(mod) || roles.contains(srmod) || roles.contains(supe));
		}
		if (type.getTeam() == EscalationTeam.MODERATION) {
			return (member.hasPermission(Permission.ADMINISTRATOR) || roles.contains(mod) || roles.contains(srmod) || roles.contains(supe));
		}
		if (type.getTeam() == EscalationTeam.SMOD) {
			return (member.hasPermission(Permission.ADMINISTRATOR) || roles.contains(srmod) || roles.contains(supe));
		}
		if (type.getTeam() == EscalationTeam.SUPERVISOR) {
			return (member.hasPermission(Permission.ADMINISTRATOR) || roles.contains(supe));
		}
		return false;
	}
	
	public static long convertDuration(String st) {
		try {
			if (st.endsWith("s")) {
				return (Long.parseLong(st.replace("s", ""))*1000)+System.currentTimeMillis();
			}
			if (st.endsWith("m")) {
				return (Long.parseLong(st.replace("m", ""))*1000*60)+System.currentTimeMillis();
			}
			if (st.endsWith("h")) {
				return (Long.parseLong(st.replace("h", ""))*1000*60*60)+System.currentTimeMillis();
			}
			if (st.endsWith("d")) {
				return (Long.parseLong(st.replace("d", ""))*1000*60*60*24)+System.currentTimeMillis();
			}
			if (st.endsWith("w")) {
				return (Long.parseLong(st.replace("w", ""))*1000*60*60*24*7)+System.currentTimeMillis();
			}
		} catch (Exception e) {
			return -1L;
		}
		return -1L;
	}
	
	@Override
	public void onModalInteraction(ModalInteractionEvent e) {
		if (e.getModalId().startsWith("issuesanction")) {
			String[] split = e.getModalId().split(":");
			SanctionType type = SanctionType.valueOf(split[1].toUpperCase());
			e.deferReply(true).queue(reply -> {
				GuildHook.get().getMainGuild().retrieveMemberById(Long.parseLong(split[2])).queue(acc -> {
					String reason = e.getValue("reason").getAsString();
					String evidence = e.getValue("evidence").getAsString();
					long temp = -1L;
					if (type.doesSanctionRequireDuration()) {
						temp = convertDuration(e.getValue("duration").getAsString());
						if (temp == -1L || temp <= 0) {
							reply.editOriginal("Sorry! This duration is not supported").queue();
						return;}
						if (temp > (System.currentTimeMillis()+(1000*60*60*24*20)) && type == SanctionType.TIMEOUT) {
							reply.editOriginal("Maximum of 20 days for a timeout!").queue();
						return;}
						long duration = temp;
						createSanctionEntry(acc.getIdLong(),e.getMember().getIdLong(),reason,evidence,type,duration).thenAccept(sanc -> {
							pushSanctionToGuild(GuildHook.get().getMainGuild(),sanc);
							pushSanctionToGuildNoMsg(GuildHook.get().getLegionGuild(),sanc);
							reply.editOriginal("Sanction has been submitted").queue();
							EmbedBuilder builder = new EmbedBuilder();
							builder.setTitle("Moderation Sanction");
							builder.setColor(Color.CYAN);
							builder.addField("ID",String.valueOf(sanc.getID()),false);
							builder.addField("User",acc.getAsMention(),false);
							builder.addField("Sanction Issued",type.getName(),false);
							builder.addField("Reason",reason,false);
							builder.addField("Evidence",evidence,false);
							builder.addField("Moderator",e.getMember().getAsMention(),false);
							builder.addField("Duration",(duration == -1L) ? "Permanent" : TimeFormat.RELATIVE.format(duration),false);
							builder.addField("Issued on",TimeFormat.DATE_TIME_LONG.now().toString(),false);
							GuildHook.get().getMainGuild().getTextChannelById(1444920908295700602L).sendMessageEmbeds(builder.build()).queue();
						});
					} else {
						createSanctionEntry(acc.getIdLong(),e.getMember().getIdLong(),reason,evidence,type,-1L).thenAccept(sanc -> {
							pushSanctionToGuild(GuildHook.get().getMainGuild(),sanc);
							pushSanctionToGuildNoMsg(GuildHook.get().getLegionGuild(),sanc);
							reply.editOriginal("Sanction has been submitted").queue();
							EmbedBuilder builder = new EmbedBuilder();
							builder.setTitle("Moderation Sanction");
							builder.setColor(Color.CYAN);
							builder.addField("ID",String.valueOf(sanc.getID()),false);
							builder.addField("User",acc.getAsMention(),false);
							builder.addField("Sanction Issued",type.getName(),false);
							builder.addField("Reason",reason,false);
							builder.addField("Evidence",evidence,false);
							builder.addField("Moderator",e.getMember().getAsMention(),false);
							builder.addField("Duration","Permanent",false);
							builder.addField("Issued on",TimeFormat.DATE_TIME_LONG.now().toString(),false);
							GuildHook.get().getMainGuild().getTextChannelById(1444920908295700602L).sendMessageEmbeds(builder.build()).queue();
						});
					}
				}, err -> {
					GuildHook.get().getLegionGuild().retrieveMemberById(Long.parseLong(split[2])).queue(suc -> {
						suc.kick().queue();
					}, err2 -> {
						//silent
					});
				});
			});
		return;}
	}
	
	public CompletableFuture<UserSanctionHistory> getHistory(long userid, long minimum) {
		CompletableFuture<UserSanctionHistory> future = new CompletableFuture<>();	
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("SELECT * FROM Sanctions WHERE UserID=? AND ID < ? ORDER BY ID DESC LIMIT 11");
				ps.setLong(1, userid);
				ps.setLong(2, minimum);
				ResultSet set = ps.executeQuery();
				ArrayList<Sanction> sanctions = new ArrayList<>();
				boolean more = false;
				while (set.next()) {
					if (sanctions.size() == 10) {
						more = true;
						continue;
					}
					long id = set.getLong("ID");
					long staff = set.getLong("Staff");
					String reason = set.getString("Reason");
					String evidence = set.getString("Evidence");
					SanctionType type = SanctionType.valueOf(set.getString("SanctionType"));
					long duration = set.getLong("Duration");
					Sanction sanc = new Sanction(id,userid,staff,reason,evidence,type,duration);
					sanctions.add(sanc);
				}
				future.complete( new UserSanctionHistory(userid, more, (sanctions.isEmpty()) ? Long.MAX_VALUE : sanctions.get(0).getID(), (sanctions.isEmpty()) ? Long.MIN_VALUE : sanctions.get(sanctions.size()-1).getID(), sanctions));
			} catch (Exception e) {
				e.printStackTrace();
				future.complete(null);
			}finally {
				db.offerConnection(c);
			}
		});
		return future;
	}
	
	@Override
	public void onButtonInteraction(ButtonInteractionEvent e) {
		if (e.getButton().getCustomId().startsWith("nextpage")) {
			String[] split = e.getButton().getCustomId().split(":");
			long userid = Long.parseLong(split[2]);
			long nextmove = Long.parseLong(split[1]);
			e.deferEdit().queue(edit -> {
				getHistory(userid,nextmove).thenAccept(history -> {
					Button nextpage = Button.secondary("nextpage:" + history.getLowest() + ":" + userid, "Next Page >>");
					Button prev = Button.secondary("prev:" + history.getHighest() + ":" + userid, "<< Previous Page");
					if (history.doesHistoryHaveNextPage()) {
					edit.editOriginalEmbeds(history.getMessage()).setComponents(ActionRow.of(prev),ActionRow.of(nextpage)).queue();
					} else {
					edit.editOriginalEmbeds(history.getMessage()).setComponents(ActionRow.of(prev)).queue();
					}
				});
			});
		return;}
		if (e.getButton().getCustomId().startsWith("prev")) {
			String[] split = e.getButton().getCustomId().split(":");
			long userid = Long.parseLong(split[2]);
			long nextmove = Long.parseLong(split[1]);
			e.deferEdit().queue(edit -> {
				getHistory(userid,nextmove).thenAccept(history -> {
					Button nextpage = Button.secondary("nextpage:" + history.getLowest() + ":" + userid, "Next Page >>");
					Button prev = Button.secondary("prev:" + history.getHighest() + ":" + userid, "<< Previous Page");
					if (history.getHighest() == Long.MAX_VALUE) {
					edit.editOriginalEmbeds(history.getMessage()).setComponents(ActionRow.of(nextpage)).queue();
					} else {
					edit.editOriginalEmbeds(history.getMessage()).setComponents(ActionRow.of(prev),ActionRow.of(nextpage)).queue();
					}
				});
			});
		return;}
	}
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
		if (e.getName().equalsIgnoreCase("history")) {
			Member member = e.getMember();
			if (!canMemberUseSanction(member,SanctionType.TIMEOUT,e.getGuild())) {
				e.reply("You do not have permission to view sanctions").setEphemeral(true).queue();
			return;}
			long userid = 0;
			if (e.getSubcommandName().equalsIgnoreCase("id")) {
				try {
					userid = Long.parseLong(e.getOption("userid").getAsString());
				} catch (Exception ex) {
					e.reply("ID is not valid").setEphemeral(true).queue();
					return;
				}
			} else if (e.getSubcommandName().equalsIgnoreCase("user")) {
				userid = e.getOption("user").getAsUser().getIdLong();
			}
			if (userid == 0) {
				e.reply("This method is not supported").setEphemeral(true).queue();
			return;}
			final long finalized = userid;
			e.deferReply(true).queue(reply -> {
				getHistory(finalized, Long.MAX_VALUE).thenAccept(history -> {
					Button nextpage = Button.secondary("nextpage:" + history.getLowest() + ":" + finalized, "Next Page >>");
					if (history.doesHistoryHaveNextPage()) {
					reply.editOriginalEmbeds(history.getMessage()).setComponents(ActionRow.of(nextpage)).queue();
					} else {
					reply.editOriginalEmbeds(history.getMessage()).queue();
					}
				});
			});
		return;}
		if (e.getName().equalsIgnoreCase("delsanction")) {
			Member member = e.getMember();
			if (!member.getRoles().contains(e.getGuild().getRoleById(EscalationTeam.DEVELOPER.getRoleId()))) {
				e.reply("This command has been restricted to Developers!").setEphemeral(true).queue();
			return;}
			e.deferReply(true).queue(reply -> {
				getSanction(e.getOption("id").getAsLong()).thenAccept(cas -> {
					if (cas == null) {
						reply.editOriginal("Unable to find the sanction linked with that id").queue();
					return;}
					deleteCase(cas.getID());
					reply.editOriginal("Sanction has been successfully deleted!").queue();
				});
			});
		return;}
		if (e.getName().equalsIgnoreCase("revokesanction")) {
			Member member = e.getMember();
			if (!canMemberUseSanction(member,SanctionType.REVOKED,e.getGuild())) {
				e.reply("You do not have permission to revoke sanctions").setEphemeral(true).queue();
			return;}
			e.deferReply(true).queue(reply -> {
				getSanction(e.getOption("id").getAsLong()).thenAccept(cas -> {
					if (cas == null) {
						reply.editOriginal("Unable to find the sanction linked with that id").queue();
					return;}
					if (cas.getType() == SanctionType.REVOKED) {
						reply.editOriginal("This sanction has already been revoked!").queue();
					return;}
					submitUpdate(cas.getID(),SanctionType.REVOKED);
					if (cas.getType() == SanctionType.TIMEOUT) {
						GuildHook.get().getMainGuild().retrieveMemberById(cas.getUserID()).queue(other -> {
							if (other.isTimedOut()) {
								other.removeTimeout().queue();
							}
						}, err -> {
							System.out.println("Member not found for sanction revoke");
						});
						GuildHook.get().getLegionGuild().retrieveMemberById(cas.getUserID()).queue(other -> {
							if (other.isTimedOut()) {
								other.removeTimeout().queue();
							}
						}, err -> {
							System.out.println("Member not found for sanction revoke");
						});
					}
					if (cas.getType() == SanctionType.TEMP_BAN || cas.getType() == SanctionType.BLACKLIST || cas.getType() == SanctionType.PERM_BAN) {
						GuildHook.get().getMainGuild().unban(UserSnowflake.fromId(cas.getUserID())).queue(suc -> {
							 System.out.println("User unbanned due to revoked ban: " + cas.getUserID());
						 }, err -> {
							System.out.println("Failed to unban ID in Main" + cas.getUserID()); 
						 });
						GuildHook.get().getLegionGuild().unban(UserSnowflake.fromId(cas.getUserID())).queue(suc -> {
							 System.out.println("User unbanned due to revoked ban: " + cas.getUserID());
						 }, err -> {
							System.out.println("Failed to unban ID in Legion" + cas.getUserID()); 
						 });
					}
					reply.editOriginal("Sanction has been successfully revoked").queue();
					EmbedBuilder builder = new EmbedBuilder();
					builder.setColor(Color.ORANGE);
					builder.setTitle("Moderation Action Revoked");
					builder.addField("ID",cas.getID() + "",false);
					builder.addField("User",UserSnowflake.fromId(cas.getUserID()).getAsMention(),false);
					builder.addField("Sanction Revoked",cas.getType().getName(),false);
					builder.addField("Reason",e.getOption("reason").getAsString(),false);
					builder.addField("Revoked By",member.getAsMention(),false);
					GuildHook.get().getMainGuild().getTextChannelById(1444920908295700602L).sendMessageEmbeds(builder.build()).queue();
				});
			});
		return;}
		if (e.getName().equalsIgnoreCase("sanction")) {
			Member member = e.getOption("user").getAsMember();
			SanctionType type = SanctionType.valueOf(e.getOption("sanctiontype").getAsString());
			if (!canMemberUseSanction(e.getMember(),type,e.getGuild())) {
				e.reply("You do not have the permissions required for this sanction").setEphemeral(true).queue();
			return;}
			TextInput reason = TextInput.create("reason", TextInputStyle.PARAGRAPH)
					.setMinLength(5).setMaxLength(1024).setPlaceholder("Please provide the reason for this sanction").build();
			TextInput evidence = TextInput.create("evidence", TextInputStyle.PARAGRAPH)
					.setMinLength(1).setMaxLength(1024).setPlaceholder("Please provide the Evidence. Try using external sites such as Imgur").build();
			if (type.doesSanctionRequireDuration()) {
				TextInput duration = TextInput.create("duration", TextInputStyle.SHORT)
						.setMinLength(1).setMaxLength(100).setPlaceholder("Enter a Duration such as 1d, 10d, 12h, 6d, 1m").build();
				Modal modal = Modal.create("issuesanction:" + type.toString() + ":" + member.getIdLong(), "Issuing a " + type.getName())
				.addComponents(Label.of("Reason", reason),Label.of("Evidence",evidence),Label.of("Duration", duration)).build();
				e.replyModal(modal).queue();
			} else {
				Modal modal = Modal.create("issuesanction:" + type.toString() + ":" + member.getIdLong(), "Issuing a " + type.getName())
						.addComponents(Label.of("Reason", reason),Label.of("Evidence",evidence)).build();
				e.replyModal(modal).queue();
			}
		return;}
	}
	
	public void submitUpdate(long id, SanctionType type) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("UPDATE Sanctions SET SanctionType=? WHERE ID=?");
				ps.setString(1, type.toString());
				ps.setLong(2, id);
				ps.execute();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				db.offerConnection(c);
			}
		});
	}
	
	public void deleteCase(long id) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("DELETE FROM Sanctions WHERE ID=?");
				ps.setLong(1, id);
				ps.execute();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				db.offerConnection(c);
			}
		});
	}
	
	public void pushSanctionToGuildNoMsg(Guild guild, Sanction sanc) {
		guild.retrieveMemberById(sanc.getUserID()).queue(user -> {
			if (user.hasPermission(Permission.ADMINISTRATOR)) {
				return;
			}
			if (sanc.getType() == SanctionType.TIMEOUT) {
				user.timeoutFor(sanc.getDuration()-System.currentTimeMillis(), TimeUnit.MILLISECONDS).queue();
			return;}
			if (sanc.getType() == SanctionType.KICK) {
				user.kick().queue();
			return;}
			if (sanc.getType() == SanctionType.TEMP_BAN) {
				user.ban(7, TimeUnit.DAYS).queue();
			return;}
			if (sanc.getType() == SanctionType.PERM_BAN) {
				user.ban(7, TimeUnit.DAYS).queue();
			return;}
			if (sanc.getType() == SanctionType.BLACKLIST) {
				user.ban(7, TimeUnit.DAYS).queue();
			return;}
		}, err -> {
			System.out.println("Failed to push update, user not found!");
		});
	}
	
	public void sendMessage(Member member, EmbedBuilder builder) {
		member.getUser().openPrivateChannel().queue(pc -> {
			pc.sendMessageEmbeds(builder.build()).queue(msg -> {}, err -> {
				System.out.println("Failed");
			});
		}, err -> {
			System.out.println("Failed");
		});
	}	
	
	public void pushSanctionToGuild(Guild guild, Sanction sanc) {
		guild.retrieveMemberById(sanc.getUserID()).queue(user -> {
			EmbedBuilder builder = new EmbedBuilder();
			if (sanc.getType() == SanctionType.WARNING) {
				builder.setTitle("You have been issued a WARNING");
				builder.setColor(Color.YELLOW);
				builder.addField("Reason",sanc.getReason(),false);
				builder.addField("Team","Moderation",false);
				sendMessage(user, builder);
			return;}
			if (user.hasPermission(Permission.ADMINISTRATOR)) {
				return;
			}
			if (sanc.getType() == SanctionType.TIMEOUT) {
				builder.setTitle("You have been issued a TimeOut");
				builder.setColor(Color.ORANGE);
				builder.addField("Reason",sanc.getReason(),false);
				builder.addField("Team","Moderation",false);
				builder.addField("Your timeout will be removed",TimeFormat.RELATIVE.format(sanc.getDuration()),false);
				sendMessage(user, builder);
				user.timeoutFor(sanc.getDuration()-System.currentTimeMillis(), TimeUnit.MILLISECONDS).queue();
			return;}
			if (sanc.getType() == SanctionType.KICK) {
				builder.setTitle("You have been removed from the server!");
				builder.setColor(Color.ORANGE);
				builder.addField("Reason",sanc.getReason(),false);
				builder.addField("Team","Moderation",false);
				user.getUser().openPrivateChannel().queue(pc -> {
					pc.sendMessageEmbeds(builder.build()).queue(msg -> {
						user.kick().queue();
					}, err -> {
						System.out.println("Failed");
						user.kick().queue();
					});
				}, err -> {
					System.out.println("Failed");
					user.kick().queue();
				});
			return;}
			if (sanc.getType() == SanctionType.TEMP_BAN) {
				builder.setTitle("You have been temporarily banned!");
				builder.setColor(Color.RED);
				builder.addField("Reason",sanc.getReason(),false);
				builder.addField("Team","Moderation",false);
				builder.addField("Your ban will be removed",TimeFormat.RELATIVE.format(sanc.getDuration()),false);
				user.getUser().openPrivateChannel().queue(pc -> {
					pc.sendMessageEmbeds(builder.build()).queue(msg -> {
						user.ban(7, TimeUnit.DAYS).queue();
					}, err -> {
						System.out.println("Failed");
						user.ban(7, TimeUnit.DAYS).queue();
					});
				}, err -> {
					System.out.println("Failed");
					user.ban(7, TimeUnit.DAYS).queue();
				});
			return;}
			if (sanc.getType() == SanctionType.PERM_BAN) {
				builder.setTitle("You have been permanently banned!");
				builder.setColor(Color.RED);
				builder.addField("Reason",sanc.getReason(),false);
				builder.addField("Team","Moderation",false);
				user.getUser().openPrivateChannel().queue(pc -> {
					pc.sendMessageEmbeds(builder.build()).queue(msg -> {
						user.ban(7, TimeUnit.DAYS).queue();
					}, err -> {
						System.out.println("Failed");
						user.ban(7, TimeUnit.DAYS).queue();
					});
				}, err -> {
					System.out.println("Failed");
					user.ban(7, TimeUnit.DAYS).queue();
				});
			return;}
			if (sanc.getType() == SanctionType.BLACKLIST) {
				builder.setTitle("You have been blacklisted from the server!");
				builder.setColor(Color.BLACK);
				builder.addField("Reason",sanc.getReason(),false);
				builder.addField("Team","Moderation",false);
				user.getUser().openPrivateChannel().queue(pc -> {
					pc.sendMessageEmbeds(builder.build()).queue(msg -> {
						user.ban(7, TimeUnit.DAYS).queue();
					}, err -> {
						System.out.println("Failed");
						user.ban(7, TimeUnit.DAYS).queue();
					});
				}, err -> {
					System.out.println("Failed");
					user.ban(7, TimeUnit.DAYS).queue();
				});
			return;}
		}, err -> {
			System.out.println("Failed to push update, user not found!");
		});
	}
	
	
	public CompletableFuture<Sanction> createSanctionEntry(long userid, long modid, String reason, String evidence, SanctionType type, long dur) {
		CompletableFuture<Sanction> future = new CompletableFuture<>();
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("INSERT INTO Sanctions (UserID, Staff, Reason, Evidence, SanctionType, Duration) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
				ps.setLong(1, userid);
				ps.setLong(2, modid);
				ps.setString(3, reason);
				ps.setString(4, evidence);
				ps.setString(5, type.toString());
				ps.setLong(6, dur);
				ps.execute();
				ResultSet set = ps.getGeneratedKeys();
				set.next();
				long id = set.getLong(1);
				set.close();
				Sanction sanction = new Sanction(id,userid,modid,reason,evidence,type,dur);
				ps.close();
				future.complete(sanction);
			} catch (Exception e) {
				e.printStackTrace();
				future.complete(null);
			}finally {
				db.offerConnection(c);
			}
		});
		return future;
	}
	
	
	public CompletableFuture<Sanction> getSanction(long id) {
		CompletableFuture<Sanction> future = new CompletableFuture<>();
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("SELECT * FROM Sanctions WHERE ID=?");
				ps.setLong(1, id);
				ResultSet set = ps.executeQuery();
				if (set.next()) {
					long userid = set.getLong("UserID");
					long staff = set.getLong("Staff");
					String reason = set.getString("Reason");
					String evidence = set.getString("Evidence");
					SanctionType type = SanctionType.valueOf(set.getString("SanctionType"));
					long duration = set.getLong("Duration");
					future.complete(new Sanction(id,userid,staff,reason,evidence,type,duration));
				} else {
					set.close();
					ps.close();
					future.complete(null);
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
				future.complete(null);
			}finally {
				db.offerConnection(c);
			}
		});
		return future;
	}

}
