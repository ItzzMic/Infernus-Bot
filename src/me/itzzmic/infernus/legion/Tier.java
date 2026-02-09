package me.itzzmic.infernus.legion;

public enum Tier {
	NONE(-1L),
	I(1452163845383520421L),
	II(1452163963373486232L),
	III(1452164129728102411L),
	IV(1452164174347243693L);
	
	private long roleid;
	Tier(long roleid){
		this.roleid = roleid;
	}
	
	public long getRoleID() {
		return roleid;
	}

}
