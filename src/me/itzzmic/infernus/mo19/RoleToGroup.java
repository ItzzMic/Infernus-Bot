package me.itzzmic.infernus.mo19;

public enum RoleToGroup {
	MO19(1455596131223605264L, 33606492L),
	MO7(1455602019480899654L, 33815673L),
	MO8(1455602029941494024L, 34481176L),
	CCT(1455789165957615698L, 35480178L),
	PSD(1455787086228619314L, 35552377L),
	LG(1455786985011810416L, 33688176L),
	SPA(1455787145594667183L, 35552374L),
	RMP(1455786789049602131L, 33688172L),
	GG(1455786842623578214L, 34154456L);
	
	
	
	private long roleid;
	private long groupid;
	RoleToGroup(long roleid, long groupid){
		this.roleid = roleid;
		this.groupid = groupid;
	}
	
	
	public static RoleToGroup getByGroupID(long gid) {
		for (RoleToGroup g : RoleToGroup.values()) {
			if (g.getGroupID() == gid) {
				return g;
			}
		}
		return null;
	}
	
	
	public long getGroupID() {
		return groupid;
	}
	
	public long getRoleID() {
		return roleid;
	}

}
