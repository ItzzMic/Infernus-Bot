package me.itzzmic.infernus.ticketsystem;

public enum EscalationTeam {
	SUPPORT("No Escalation",1423082087325630585L),
	MODERATION("Moderation Team",1423082616298541166L),
	IDD("Infernus Disiplinary Team",1423083248682143845L),
	SMOD("Senior Moderation Team",1423083725192695919L),
	SUPERVISOR("Supervisor Team",1422593132993974312L),
	IDD_LEAD("IDD Lead",1441018689708232704L),
	DEVELOPER("Developer",1423143307881742336L),
	LEADERSHIP("Leadership",1423084198306123816L);
	
	
	private String name;
	private Long roleid;
	EscalationTeam(String name, Long id){
		this.name = name;
		roleid = id;
	}
	
	
	public String getName() {
		return name;
	}
	
	public Long getRoleId() {
		return roleid;
	}

}
