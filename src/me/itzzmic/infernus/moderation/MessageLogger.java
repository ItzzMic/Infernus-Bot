package me.itzzmic.infernus.moderation;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.itzzmic.infernus.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.TimeFormat;

public class MessageLogger extends ListenerAdapter {
	private Database db;
	private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	public MessageLogger() {
		db = Database.get();
		Runnable task = () -> {
			Connection c = db.borrowConnection();
			try {
				long time = System.currentTimeMillis() - (1000*60*60*24*7);
				PreparedStatement purge = c.prepareStatement("DELETE FROM MessageLog WHERE Stamp<?");
				purge.setLong(1, time);
				purge.execute();
				purge.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				db.offerConnection(c);
			}
		};
		service.scheduleAtFixedRate(task, 0, 30, TimeUnit.SECONDS);
	}

	public CompletableFuture<SimplifiedMessage> getMessage(long id){
		CompletableFuture<SimplifiedMessage> future = new CompletableFuture<>();
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("SELECT * FROM MessageLog WHERE MessageID=?");
				ps.setLong(1, id);
				ResultSet set = ps.executeQuery();
				if (!set.next()) {
					set.close();
					ps.close();
					future.complete(null);
				} else {
					long channelid = set.getLong("ChannelID");
					long authorid = set.getLong("Author");
					long timestamp = set.getLong("Stamp");
					String content = set.getString("Content");
					future.complete(new SimplifiedMessage(id,channelid,authorid,content,timestamp));
					set.close();
					ps.close();
				}
			} catch (Exception e) {
				future.completeExceptionally(e);
			} finally {
				db.offerConnection(c);
			}
		});
		return future;
	}
	
	public void updateMessageContent(String newcontent, long msgid) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("UPDATE MessageLog SET Content=? WHERE MessageID=?");
				ps.setString(1, newcontent);
				ps.setLong(2, msgid);
				ps.execute();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				db.offerConnection(c);
			}
		});
	}
	
	public void deleteMessage(long msgid) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
			try {
				PreparedStatement ps = c.prepareStatement("DELETE FROM MessageLog WHERE MessageID=?");
				ps.setLong(1, msgid);
				ps.execute();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				db.offerConnection(c);
			}
		});
	}

	public void saveMessageToDatabase(Message msg) {
		db.getThreadManager().submit(() -> {
			Connection c = db.borrowConnection();
				try {
					PreparedStatement ps = c.prepareStatement("INSERT INTO MessageLog VALUES (?,?,?,?,?);");
					ps.setLong(1, msg.getIdLong());
					ps.setLong(2, msg.getChannelIdLong());
					ps.setLong(3, msg.getAuthor().getIdLong());
					ps.setString(4, msg.getContentRaw());
					ps.setLong(5, System.currentTimeMillis());
					ps.execute();
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Failed to save message to database!");
				} finally {
					db.offerConnection(c);
				}
		});
	}
	
	
	@Override
	public void onMessageUpdate(MessageUpdateEvent e) {
		long msgid = e.getMessageIdLong();
		CompletableFuture<SimplifiedMessage> ms = getMessage(msgid);
		ms.thenAccept(msg -> {
			if (msg == null) {
				return;
			}
			if (msg.getContent().equalsIgnoreCase(e.getMessage().getContentRaw())) {
				return;
			}
			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(Color.BLUE);
			builder.setTitle("Message Edited");
			builder.addField("User",UserSnowflake.fromId(msg.getAuthorID()).getAsMention(),false);
			builder.addField("Channel",e.getChannel().getAsMention(),false);
			builder.addField("Old Message",(msg.getContent().length() > 1024) ? msg.getContent().substring(0, 1024) : msg.getContent(),false);
			builder.addField("New Message", (e.getMessage().getContentRaw().length() > 1024) ? e.getMessage().getContentRaw().substring(0, 1024) :e.getMessage().getContentRaw(),false);
			builder.addField("Message Sent",TimeFormat.RELATIVE.format(msg.getTimestamp()),false);
			builder.addField("Message Edited", TimeFormat.RELATIVE.now().toString(),false);
			e.getGuild().getTextChannelById(1460878354915000331L).sendMessageEmbeds(builder.build()).queue();
			updateMessageContent(e.getMessage().getContentRaw(), msg.getMessageID());
		});
	}
	
	@Override
	public void onMessageDelete(MessageDeleteEvent e) {
		long msgid = e.getMessageIdLong();
		CompletableFuture<SimplifiedMessage> ms = getMessage(msgid);
		ms.thenAccept(msg -> {
			if (msg == null) {
				return;
			}
			if (msg.getContent() == null || msg.getContent().isBlank()) {
				deleteMessage(msg.getMessageID());
				return;
			}
			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(Color.RED);
			builder.setTitle("Message Deleted");
			builder.addField("User",UserSnowflake.fromId(msg.getAuthorID()).getAsMention(),false);
			builder.addField("Channel",e.getChannel().getAsMention(),false);
			builder.addField("Message",(msg.getContent().length() > 1024) ? msg.getContent().substring(0, 1024) : msg.getContent(),false);
			builder.addField("Message Sent",TimeFormat.RELATIVE.format(msg.getTimestamp()),false);
			builder.addField("Message Deleted", TimeFormat.RELATIVE.now().toString(),false);
			e.getGuild().getTextChannelById(1460878354915000331L).sendMessageEmbeds(builder.build()).queue();
			deleteMessage(msg.getMessageID());
		});
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		if (e.getAuthor().isBot() || e.getAuthor().isSystem()) {
			return;
		}
		if (!e.isFromGuild()) {
			return;
		}
		MessageChannel channel = e.getGuildChannel();
		if (channel instanceof ICategorizableChannel && ((ICategorizableChannel)channel).getParentCategory().getIdLong() == 1418283693599887620L) {
			return;
		}
		saveMessageToDatabase(e.getMessage());

	}

}
