package me.itzzmic.infernus.moderation;

import me.itzzmic.infernus.ticketsystem.EscalationTeam;

public enum SanctionType {
	WARNING(EscalationTeam.SUPPORT,"Warning",false),
	TIMEOUT(EscalationTeam.MODERATION,"Timeout",true),
	KICK(EscalationTeam.MODERATION,"Kicked",false),
	TEMP_BAN(EscalationTeam.MODERATION,"Temporary Ban",true),
	PERM_BAN(EscalationTeam.SMOD,"Permanent Ban",false),
	BLACKLIST(EscalationTeam.SUPERVISOR,"Blacklist",false),
	EXPIRED_BAN(EscalationTeam.LEADERSHIP,"Expired Ban",false),
	REVOKED(EscalationTeam.SMOD,"Revoked",false);
	
	private EscalationTeam team;
	private String name;
	private boolean req;
	SanctionType(EscalationTeam team, String name, boolean req){
		this.team = team;
		this.name = name;
		this.req = req;
	}
	
	public boolean doesSanctionRequireDuration() {
		return req;
	}
	
	public EscalationTeam getTeam() {
		return team;
	}
	
	public String getName() {
		return name;
	}

}
