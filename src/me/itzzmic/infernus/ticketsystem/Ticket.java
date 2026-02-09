package me.itzzmic.infernus.ticketsystem;

public class Ticket {
	private long ID;
	private long opener;
	private String reason;
	private long closer;
	private String closereason;
	private EscalationTeam team;
	private long Timestamp;
	public Ticket(long id, long opener, String reason, long Timestamp) {
		this.ID = id;
		this.opener = opener;
		this.reason = reason;
		closer = 0L;
		closereason = "Ticket is not closed";
		team = EscalationTeam.SUPPORT;
		this.Timestamp = Timestamp;
	}

	public Long getID() {
		return ID;
	}
	
	public void changeID(long id) {
		this.ID = id;
	}

	public Long getOpener() {
		return opener;
	}

	public String getReason() {
		return reason;
	}

	public Long getCloser() {
		return closer;
	}

	public void setCloser(Long closer) {
		this.closer = closer;
	}

	public String getClosereason() {
		return closereason;
	}

	public void setClosereason(String closereason) {
		this.closereason = closereason;
	}

	public EscalationTeam getEscalationTeam() {
		return team;
	}

	public void setEscalationTeam(EscalationTeam team) {
		this.team = team;
	}

	public long getTimestamp() {
		return Timestamp;
	}
	
	

}
