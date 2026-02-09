package me.itzzmic.infernus.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import me.itzzmic.infernus.Database;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

public class RankManager {
	private static RankManager mgr;
	private Database db;
	
	public RankManager() {
		mgr = this;
		db = Database.get();
	}
	
	public static RankManager get() {
		return mgr;
	}
	
	
	public void addRank(Role rank, Role demotion, Role promotion, String name, String branch) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			Objects.requireNonNull(rank, "Rank cannot be null");
		    Objects.requireNonNull(name, "Name cannot be null");
		    Objects.requireNonNull(branch, "Branch cannot be null");
			try {
				PreparedStatement ps = c.prepareStatement("INSERT INTO Ranks VALUES (?,?,?,?,?);");
				ps.setString(1, branch);
				ps.setLong(2, rank.getIdLong());
				ps.setString(3, name);
				long promo = (promotion != null) ? promotion.getIdLong() : 0L;
				long demo = (demotion != null) ? demotion.getIdLong() : 0L;
				ps.setLong(4, demo);
				ps.setLong(5, promo);
				ps.execute();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				db.offerConnection(c);
			}
		});
	}
	
	public void moveBranch(Role rank, String branch) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("UPDATE Ranks SET branch=? WHERE RoleID=?");
				ps.setString(1, branch);
				ps.setLong(2, rank.getIdLong());
				ps.execute();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				db.offerConnection(c);
			}
		});
	}
	
	public void removeRole(long id) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("DELETE FROM Ranks WHERE RoleID=?");
				ps.setLong(1,id);
				ps.execute();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				db.offerConnection(c);
			}
		});
	}
	
	public CompletableFuture<List<Rank>> retrieveRanks(String branch, Guild guild){
		CompletableFuture<List<Rank>> future = new CompletableFuture<>();
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				ArrayList<Rank> ranks = new ArrayList<>();
				PreparedStatement ps = c.prepareStatement("SELECT * FROM Ranks WHERE branch=?");
				ps.setString(1, branch);
				ResultSet set = ps.executeQuery();
				while (set.next()) {
					long id = set.getLong("RoleID");
					Role main = guild.getRoleById(id);
					if (main == null) {
						removeRole(id);
						continue;
					}
					boolean requiremodify = false;
					long promoid = set.getLong("PromoteID");
					long demoid = set.getLong("DemoteID");
					Role Promote = (promoid != 0) ? guild.getRoleById(promoid) : null;
					Role Demote = (demoid != 0) ? guild.getRoleById(demoid) : null;
					requiremodify = ((promoid != 0 && Promote == null) || (demoid != 0 && Demote == null));
					if (requiremodify) {
						modifyEntry(main,Demote,Promote);
					}
					Rank rank = new Rank(branch,main.getName(),main,Demote,Promote);
					ranks.add(rank);
				}
				set.close();
				ps.close();
				future.complete(ranks);
			} catch (Exception e) {
				e.printStackTrace();
				future.complete(new ArrayList<Rank>());
			}finally {
				try {
					c.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
		return future;
	}
	
	
	public CompletableFuture<Rank> retrieveRank(String name,String branch, Guild guild) {
		CompletableFuture<Rank> future = new CompletableFuture<>();
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("SELECT * FROM Ranks WHERE Name=? AND branch=?");
				ps.setString(1, name);
				ps.setString(2, branch);
				ResultSet set = ps.executeQuery();
				if (set.next()) {
					long id = set.getLong("RoleID");
					Role main = guild.getRoleById(id);
					if (main == null) {
						ps.close();
						set.close();
						removeRole(id);
						future.complete(null);
						return;
					}
					boolean requiremodify = false;
					long promoid = set.getLong("PromoteID");
					long demoid = set.getLong("DemoteID");
					Role Promote = (promoid != 0) ? guild.getRoleById(promoid) : null;
					Role Demote = (demoid != 0) ? guild.getRoleById(demoid) : null;
					requiremodify = ((promoid != 0 && Promote == null) || (demoid != 0 && Demote == null));
					if (requiremodify) {
						modifyEntry(main,Demote,Promote);
					}
					Rank rank = new Rank(branch,name,main,Demote,Promote);
					ps.close();
					set.close();
					future.complete(rank);
				} else {
					set.close();
					ps.close();
					future.complete(null);
				}
			} catch (Exception e) {
				e.printStackTrace();
				future.complete(null);
			}finally {
				db.offerConnection(c);
			}
		});
		return future;
	}
	
	public CompletableFuture<Rank> retrieveRank(Role role,String branch, Guild guild) {
		CompletableFuture<Rank> future = new CompletableFuture<>();
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("SELECT * FROM Ranks WHERE RoleID=? AND branch=?");
				ps.setLong(1, role.getIdLong());
				ps.setString(2, branch);
				ResultSet set = ps.executeQuery();
				if (set.next()) {
					long id = set.getLong("RoleID");
					Role main = guild.getRoleById(id);
					if (main == null) {
						ps.close();
						set.close();
						removeRole(id);
						future.complete(null);
						return;
					}
					boolean requiremodify = false;
					long promoid = set.getLong("PromoteID");
					long demoid = set.getLong("DemoteID");
					Role Promote = (promoid != 0) ? guild.getRoleById(promoid) : null;
					Role Demote = (demoid != 0) ? guild.getRoleById(demoid) : null;
					requiremodify = ((promoid != 0 && Promote == null) || (demoid != 0 && Demote == null));
					if (requiremodify) {
						modifyEntry(main,Demote,Promote);
					}
					String name = set.getString("Name");
					Rank rank = new Rank(branch,name,main,Demote,Promote);
					ps.close();
					set.close();
					future.complete(rank);
				} else {
					set.close();
					ps.close();
					future.complete(null);
				}
			} catch (Exception e) {
				e.printStackTrace();
				future.complete(null);
			}finally {
				db.offerConnection(c);
			}
		});
		return future;
	}
	
	public void modifyEntry(Role rank, Role demotion, Role promotion) {
		db.getThreadManager().submit(() -> {
			Connection c = Database.get().borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("UPDATE 'Ranks' SET PromoteID=?,DemoteID=? WHERE RoleID=?");
				long promo = (promotion != null) ? promotion.getIdLong() : 0L;
				long demo = (demotion != null) ? demotion.getIdLong() : 0L;
				ps.setLong(1, promo);
				ps.setLong(2, demo);
				ps.setLong(3, rank.getIdLong());
				ps.execute();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				db.offerConnection(c);
			}
		});
	}

}
