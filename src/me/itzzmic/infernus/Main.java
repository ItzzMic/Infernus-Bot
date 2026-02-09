package me.itzzmic.infernus;

import java.util.ArrayList;

import me.itzzmic.infernus.deployment.DeploymentManager;
import me.itzzmic.infernus.legion.WhitelistManager;
import me.itzzmic.infernus.mo19.GroupSync;
import me.itzzmic.infernus.moderation.MessageLogger;
import me.itzzmic.infernus.moderation.ModerationHandler;
import me.itzzmic.infernus.rank.RankCommands;
import me.itzzmic.infernus.ticketsystem.TicketManager;
import me.itzzmic.infernus.util.RankManager;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class Main {
	public static final String VERSION = "DISCONTINUED";

	public static void main(String[] args) {
		System.out.println("LOADING VERSION: " + VERSION);
		new Database();
		new RankManager();
		new RobloxLink();
		ArrayList<GatewayIntent> intents = new ArrayList<>();
		intents.add(GatewayIntent.GUILD_PRESENCES);
		intents.add(GatewayIntent.GUILD_MESSAGES);
		intents.add(GatewayIntent.GUILD_MEMBERS);
		intents.add(GatewayIntent.GUILD_VOICE_STATES);
		intents.add(GatewayIntent.MESSAGE_CONTENT);
		JDABuilder.createDefault("REDACTED", intents)
				.addEventListeners(new MessageLogger(),new GroupSync(),new RankCommands(),new GuildHook(), new TicketManager(), new BulkCommands(), new DeploymentManager(), new ModerationHandler(),new WhitelistManager()).setActivity(Activity.watching("over INFERNUS")).setMemberCachePolicy(MemberCachePolicy.NONE).build();

	}

}
