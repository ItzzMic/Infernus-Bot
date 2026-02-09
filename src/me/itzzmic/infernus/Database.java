package me.itzzmic.infernus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Database {
	private Connection c;
	private final String URL;
	private static Database dbv;
	private BlockingQueue<Connection> pool; 
	private ExecutorService executor = Executors.newFixedThreadPool(25);
	public Database() {
		URL = "REDACTED";
		dbv = this;
		try {
			c = DriverManager.getConnection(URL);
			Statement s = c.createStatement();
			//Create all Tables
			s.execute("CREATE TABLE IF NOT EXISTS Tickets (TicketID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, TicketOpener BIGINT NOT NULL,TicketReason TEXT NOT NULL,TicketCloser BIGINT DEFAULT 0,TicketCloseReason TEXT DEFAULT 'Not Closed', EscalationTeam TEXT DEFAULT 'SUPPORT',Timestamp BIGINT NOT NULL);");
			s.execute("CREATE TABLE IF NOT EXISTS Ranks (branch TEXT NOT NULL, RoleID BIGINT NOT NULL PRIMARY KEY, Name TEXT NOT NULL, DemoteID BIGINT DEFAULT 0, PromoteID BIGINT DEFAULT 0);");
			s.execute("CREATE TABLE IF NOT EXISTS IDD (ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,UserID BIGINT NOT NULL,Staff BIGINT DEFAULT 0,Accused TEXT NOT NULL,Evidence TEXT DEFAULT 'NO EVIDENCE',Verdict TEXT DEFAULT 'NONE',Sanction TEXT DEFAULT 'NONE');");
			s.execute("CREATE TABLE IF NOT EXISTS Sanctions (ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,UserID BIGINT NOT NULL,Staff BIGINT NOT NULL,Reason TEXT NOT NULL,Evidence TEXT NOT NULL,SanctionType TEXT NOT NULL,Duration BIGINT DEFAULT -1);");
			s.execute("CREATE INDEX IF NOT EXISTS idx_sanctions_userid_id ON Sanctions(UserID, ID);");
			s.execute("CREATE TABLE IF NOT EXISTS Whitelist (ID BIGINT NOT NULL PRIMARY KEY,Tier TEXT DEFAULT 'NONE');");
			s.execute("CREATE TABLE IF NOT EXISTS MessageLog (MessageID BIGINT NOT NULL PRIMARY KEY,ChannelID BIGINT NOT NULL, Author BIGINT NOT NULL,Content TEXT NOT NULL,Stamp BIGINT NOT NULL);");
			s.execute("CREATE TABLE IF NOT EXISTS Events (EventID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,EventRequester BIGINT NOT NULL,EventType TEXT NOT NULL,EventDescription TEXT DEFAULT 'UNSPECIFIED',Scheduled BIGINT DEFAULT -1,Link TEXT DEFAULT 'UNSPECIFIED',Status TEXT DEFAULT 'OPEN');");
			s.close();
			c.close();
			pool = new ArrayBlockingQueue<Connection>(30);
			for (int i = 0; i < 30; i++) {
				pool.add(generateConnection());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ExecutorService getThreadManager() {
		return executor;
	}
	
	public static Database get() {
		return dbv;
	}
	
	public Connection validateConnection(Connection c) {
		try {
			if (c == null || c.isClosed()) {
				closeQuietly(c);
				return generateConnection();
			} else {
				return c;
			}
		} catch (Exception ex) {
			//on Fail, generate new Connection;
			closeQuietly(c);
			return generateConnection();
		}
	}
	
	public Connection borrowConnection() {
		try {
			Connection c = pool.take();
			c = validateConnection(c);
			return c;
		} catch (InterruptedException e) {
			//If Fail, Return brand new Connection
			return generateConnection();
		}
	}
	
	private void closeQuietly(Connection c) {
	    if (c != null) {
	        try { c.close(); } 
	        catch (Exception ignored) {
	        	//ignore Exceptions
	        }
	    }
	}
	
	public void offerConnection(Connection c) {
		if (c == null) {
			return;
		}
		try {
			if (c.isClosed()) {
				closeQuietly(c);
				return;
			}
			if (!pool.offer(c)) {
				closeQuietly(c);
			}
		} catch (Exception ex) {
			closeQuietly(c);
		}
	}
	
	public Connection generateConnection() {
		try {
			return DriverManager.getConnection(URL);
		} catch (SQLException e) {
			return null;
		}
	}

}
