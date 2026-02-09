package me.itzzmic.infernus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;




public class RobloxLink {
	
	private static RobloxLink link;
	private HttpClient client;
	private final String LINK_TO_ID;
	private final String GET_GROUP_LINK;
	private ObjectMapper mapper = new ObjectMapper();
	
	
	public RobloxLink() {
		link = this;
		client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
		LINK_TO_ID = "https://users.roblox.com/v1/usernames/users";
		GET_GROUP_LINK = "https://groups.roblox.com/v2/users/%d/groups/roles";
	}
	
	public static RobloxLink getLink() {
		return link;
	}
	
	
	
	public CompletableFuture<HashMap<Long, Boolean>> isUserApartOfGroup(long id, long... ids){
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(GET_GROUP_LINK.replace("%d",String.valueOf(id))))
				.GET().build();
		
		return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					if (response.statusCode() != 200) {
						System.out.println("Failed HTTP Due to: " + response.statusCode());
						return "";
					}
					return response.body();
				}).thenApply(body -> {
					if (body.isEmpty()) {
						return new HashMap<>();
					}
					try {
					JsonNode root = mapper.readTree(body);
					JsonNode data = root.path("data");
					if (data.isEmpty()) {
						return new HashMap<>();
					}
					ArrayList<Long> list = new ArrayList<>();
					for (long i : ids) {
						list.add(i);
					}
					HashMap<Long, Boolean> map = new HashMap<>();
					for (JsonNode node : data) {
						long gid = node.path("group").path("id").asLong();
						if (list.contains(gid)) {
							map.put(gid, true);
						}
					}
					return map;
					} catch (Exception ex) {
						ex.printStackTrace();
						return new HashMap<>();
					}
				});
	}
	
	public CompletableFuture<String> getUserRankInGroup(long id, long groupid){
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(GET_GROUP_LINK.replace("%d",String.valueOf(id))))
				.GET().build();
		
		return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					if (response.statusCode() != 200) {
						System.out.println("Failed HTTP Due to: " + response.statusCode());
						return "";
					}
					return response.body();
				}).thenApply(body -> {
					if (body.isEmpty()) {
						return "NONE";
					}
					try {
					JsonNode root = mapper.readTree(body);
					JsonNode data = root.path("data");
					if (data.isEmpty()) {
						return "NONE";
					}
					String rank = "NONE";
					for (JsonNode node : data) {
						long gid = node.path("group").path("id").asLong();
						if (gid == groupid) {
							rank = node.path("role").path("name").asText();
						}
					}
					return rank;
					} catch (Exception ex) {
						ex.printStackTrace();
						return "NONE";
					}
				});
	}
	
	
	public CompletableFuture<Long> getUserID(IDRequest req){
		try {
		String json = mapper.writeValueAsString(req);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(LINK_TO_ID))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();
		
		return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					if (response.statusCode() != 200) {
						System.out.println("Failed HTTP Due to: " + response.statusCode());
						return null;
					}
					return response.body();
				}).thenApply(body -> {
					try {
					JsonNode data = mapper.readTree(body).path("data");
					return data.get(0).path("id").asLong();
					} catch (Exception e) {
						return -1L;
					}
				});
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static class IDRequest {
		public List<String> usernames;
		public boolean excludeBannedUsers = false;
		public IDRequest(String user) {
			usernames = new ArrayList<>();
			usernames.add(user);
		}
	}

}
