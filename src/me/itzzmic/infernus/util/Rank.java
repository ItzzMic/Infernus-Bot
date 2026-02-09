package me.itzzmic.infernus.util;

import net.dv8tion.jda.api.entities.Role;

public record Rank(String getBranch, String getName, Role toRole, Role getDemote, Role getPromote) {

}
