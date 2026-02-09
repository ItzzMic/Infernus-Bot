package me.itzzmic.infernus.legion;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;

import me.itzzmic.infernus.Database;
import me.itzzmic.infernus.GuildHook;
import me.itzzmic.infernus.ticketsystem.EscalationTeam;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class WhitelistManager extends ListenerAdapter{
	private static WhitelistManager mgr;
	private Database db;
	
	public WhitelistManager() {
		mgr = this;
		db = Database.get();
	}
	
	public static WhitelistManager get() {
		return mgr;
	}
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
		if (e.getName().equalsIgnoreCase("whitelist")) {
			Member member = e.getMember();
			getTier(member.getIdLong()).thenAccept(own -> {
				Guild main = GuildHook.get().getMainGuild();
				if ((own != Tier.III && own != Tier.IV) && !(e.getGuild().equals(main) && member.getRoles().contains(main.getRoleById(EscalationTeam.DEVELOPER.getRoleId())))) {
					e.reply("You must be a Legion Supervisor or above to modify whitelist status").setEphemeral(true).queue();
				return;}
				boolean bypass = (e.getGuild().equals(main) && member.getRoles().contains(main.getRoleById(EscalationTeam.DEVELOPER.getRoleId())));
				Tier tier = Tier.valueOf(e.getOption("tier").getAsString());
				if (tier == Tier.III && own != Tier.IV && !bypass) {
					e.reply("You do not have high enough clearance to grant Tier III").setEphemeral(true).queue();
				return;}
				if (tier == Tier.IV && !member.isOwner() && !bypass) {
					e.reply("Granting Tier IV requires Developer Status or Server Ownership.").setEphemeral(true).queue();
				return;}
				User other = e.getOption("user").getAsUser();
				if (tier == Tier.NONE) {
					revokeWhitelist(other.getIdLong());
					e.reply("Whitelist successfully revoked!").setEphemeral(true).queue();
				return;}
				getTier(other.getIdLong()).thenAccept(othert -> {
					if (othert == Tier.NONE) {
						whitelistUser(other.getIdLong(),tier);
						e.reply("Whitelist successfully granted!").setEphemeral(true).queue();
					} else {
						updateWhitelist(other.getIdLong(),tier);
						e.reply("Whitelist successfully modified!").setEphemeral(true).queue();
					}
				});
			});
		}
	}
	
	public void whitelistUser(long id, Tier tier) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("INSERT INTO Whitelist VALUES (?,?);");
				ps.setLong(1, id);
				ps.setString(2, tier.toString());
				ps.execute();
				ps.close();
				Guild legion = GuildHook.get().getLegionGuild();
				legion.retrieveMemberById(id).queue(mem -> {
					legion.addRoleToMember(mem, legion.getRoleById(tier.getRoleID())).queue();
				}, err -> {});
			} catch (Exception ex) {
				ex.printStackTrace();
			}finally {
				db.offerConnection(c);
			}
		});
	}
	
	public void revokeWhitelist(long id) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("DELETE FROM Whitelist WHERE ID=?");
				ps.setLong(1, id);
				ps.execute();
				ps.close();
				Guild legion = GuildHook.get().getLegionGuild();
				legion.retrieveMemberById(id).queue(mem -> {
					mem.kick().queue();
				}, err -> {});
			} catch (Exception ex) {
				ex.printStackTrace();
			}finally {
				db.offerConnection(c);
			}
		});
	}
	
	public void updateWhitelist(long id, Tier tier) {
		db.getThreadManager().submit(() -> {
			Connection c = Database.get().borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("UPDATE Whitelist SET Tier=? WHERE ID=?");
				ps.setLong(2, id);
				ps.setString(1, tier.toString());
				Guild legion = GuildHook.get().getLegionGuild();
				ps.execute();
				ps.close();
				legion.retrieveMemberById(id).queue(mem -> {
					getTier(id).thenAccept(old -> {
						if (old != Tier.NONE) {
							legion.removeRoleFromMember(mem,legion.getRoleById(old.getRoleID())).queue();
						}
					});
					legion.addRoleToMember(mem, legion.getRoleById(tier.getRoleID())).queue();
				}, err -> {});
			} catch (Exception ex) {
				ex.printStackTrace();
			}finally {
				db.offerConnection(c);
			}
		});
	}
	
	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent e) {
		if (e.getGuild().equals(GuildHook.get().getLegionGuild())) {
			Member member = e.getMember();
			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(Color.BLUE);
			builder.setTitle("Authentication");
			builder.setDescription("Welcome to Legion! Please allow me to check your whitelist clearance!");
			member.getUser().openPrivateChannel().queue(pc -> {
				pc.sendMessageEmbeds(builder.build()).queue(msg -> {
					getTier(member.getIdLong()).thenAccept(tier -> {
						if (tier == Tier.NONE) {
							EmbedBuilder builder2 = new EmbedBuilder();
							builder2.setColor(Color.RED);
							builder2.setTitle("ACCESS DENIED");
							builder2.setDescription("Sorry! You are not whitelisted to Legion!\nYou have been **removed**.");
							msg.editMessageEmbeds(builder2.build()).queue(suc -> {
								member.kick().queue();
							}, err -> {
								member.kick().queue();
							});
						return;}
						e.getGuild().addRoleToMember(member, e.getGuild().getRoleById(tier.getRoleID())).queue();
						EmbedBuilder builder2 = new EmbedBuilder();
						builder2.setColor(Color.GREEN);
						builder2.setTitle("ACCESS GRANTED");
						builder2.setDescription("You are currently whitelisted!");
						builder2.addField("Whitelist Clearance", "Tier " + tier.toString(),false);
						msg.editMessageEmbeds(builder2.build()).queue();
					});
				}, err -> {
					System.out.println("Silent Mode Enabled for " + member.getIdLong());
					getTier(member.getIdLong()).thenAccept(tier -> {
						if (tier == Tier.NONE) {
							member.kick().queue();
						return;}
						e.getGuild().addRoleToMember(member, e.getGuild().getRoleById(tier.getRoleID())).queue();
					});
				});
			}, err -> {
				System.out.println("Silent Mode Enabled for " + member.getIdLong());
				getTier(member.getIdLong()).thenAccept(tier -> {
					if (tier == Tier.NONE) {
						member.kick().queue();
					return;}
					e.getGuild().addRoleToMember(member, e.getGuild().getRoleById(tier.getRoleID())).queue();
				});
			});
		return;}
	}
	
	public CompletableFuture<Tier> getTier(long id) {
		CompletableFuture<Tier> future = new CompletableFuture<>();
		db.getThreadManager().submit(() -> {
			Connection c = Database.get().borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("SELECT Tier FROM Whitelist WHERE ID=?");
				ps.setLong(1, id);
				ResultSet set = ps.executeQuery();
				if (set.next()) {
					Tier tier = Tier.valueOf(set.getString("Tier"));
					set.close();
					ps.close();
					future.complete(tier);
				}
				ps.close();
				set.close();
				future.complete(Tier.NONE);
			} catch (Exception e) {
				e.printStackTrace();
				future.complete(Tier.NONE);
			}finally {
				db.offerConnection(c);
			}
		});
		return future;
	}

}
