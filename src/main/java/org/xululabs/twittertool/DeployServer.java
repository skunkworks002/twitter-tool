package org.xululabs.twittertool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.xululabs.twitter.Twitter4jApi;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class DeployServer extends AbstractVerticle {
	HttpServer server;
	Router router;
	Twitter4jApi twitter4jApi;
	String host;
	int port;

	/**
	 * constructor use to initialize values
	 */
	public DeployServer() {
		this.host = "localhost";
		this.port = 8283;
		this.twitter4jApi = new Twitter4jApi();
	}

	/**
	 * Deploying the verical
	 */
	@Override
	public void start() {
		server = vertx.createHttpServer();
		router = Router.router(vertx);
		// Enable multipart form data parsing
		router.route().handler(BodyHandler.create());
		router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST)
				.allowedMethod(HttpMethod.OPTIONS).allowedHeader("Content-Type, Authorization"));
		// registering different route handlers
		this.registerHandlers();
		server.requestHandler(router::accept).listen(port, host);
	}

	/**
	 * For Registering different Routes
	 */
	public void registerHandlers() {
		router.route(HttpMethod.GET, "/").blockingHandler(this::welcomeRoute);
		router.route(HttpMethod.POST, "/searchTweets").blockingHandler(this::searchTweets);
		router.route(HttpMethod.POST, "/searchProfiles").blockingHandler(this::searchProfiles);
		router.route(HttpMethod.POST, "/searchInTweetsAndProfiles").blockingHandler(this::searchInTweetsAndProfiles);
		router.route(HttpMethod.POST, "/retweet").blockingHandler(this::retweetRoute);
		router.route(HttpMethod.POST, "/followUser").blockingHandler(this::followRoute);

	}

	/**
	 * welcome route
	 * 
	 * @param routingContext
	 */

	public void welcomeRoute(RoutingContext routingContext) {
		routingContext.response().end("<h1>Welcome To Twitter Tool</h1>");
	}

	/**
	 * route to search tweets
	 * @param routingContext
	 * twitter api limit : 160-180 calls(search 100 tweets per call) in 15 min window
	 */
	public void searchTweets(RoutingContext routingContext) {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;
		String query = (routingContext.request().getParam("query") == null) ? "data science" : routingContext.request().getParam("query");
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
		try {
			TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType);
			ArrayList<Map<String, Object>> tweets = this.getTweets(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")), query);
		    responseMap.put("tweets", tweets);
		    response = mapper.writeValueAsString(responseMap);
		} catch (Exception ex) {
			response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
		}
		routingContext.response().end(response);
	}
	/**
	 * route for search Profile
	 * @param routingContext
	 * limit : twitter api can search 900 users per 15 minute window,20 per call
	 */
	public void searchProfiles(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;
		String keyword = (routingContext.request().getParam("keyword") == null) ? "data science" : routingContext.request().getParam("keyword");
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
		try {
			TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType);
			ArrayList<Map<String, Object>> users = this.getUsers(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")), keyword);
		    responseMap.put("users", users);
		    response = mapper.writeValueAsString(responseMap);
		} catch (Exception ex) {
			response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
		}
		routingContext.response().end(response);
	}
	/**
	 * route use to search in both profiles and tweets
	 * @param routingContext
	 */
	public void searchInTweetsAndProfiles(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;
		String query = (routingContext.request().getParam("query") == null) ? "data science" : routingContext.request().getParam("query");
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
		String keyword = (routingContext.request().getParam("keyword") == null) ? "data science" : routingContext.request().getParam("keyword");
		try {
			TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType);
			ArrayList<Map<String, Object>> users = this.getUsers(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")), keyword);
			ArrayList<Map<String, Object>> tweets = this.getTweets(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")), query);
			responseMap.put("users", users);
			responseMap.put("tweets", tweets);
		    response = mapper.writeValueAsString(responseMap);
		} catch (Exception ex) {
			response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
		}
		routingContext.response().end(response);
	}
	/**
	 * use to retweet by id
	 * @param routingContext
	 * limits : 2,400 tweets per day also having limts  according hour
	 */
	public void retweetRoute(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;
		String tweetId = (routingContext.request().getParam("tweetId") == null) ? "" : routingContext.request().getParam("tweetId");
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
	    try{
	    	TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType);
			boolean retweeted = twitter4jApi.retweet(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")), Long.parseLong(tweetId));
	        responseMap.put("retweeted", retweeted);
	        response = mapper.writeValueAsString(responseMap);
	    }catch(Exception ex){
	    	response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
	    }
	    routingContext.response().end(response);
	}
	/**
	 * use to follow by id
	 * @param routingContext
	 * can't follow more than 1000 user in one day, total 5000 users can be followed by a account
	 */
	public void followRoute(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;
		String userId = (routingContext.request().getParam("userId") == null) ? "" : routingContext.request().getParam("userId");
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
	    try{
	    	TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType); 
			ArrayList<Map<String, Object>> FreindshipResponse = twitter4jApi.createFriendship(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")), Long.parseLong(userId));
	        responseMap.put("following ", FreindshipResponse);
	        response = mapper.writeValueAsString(responseMap);
	    }catch(Exception ex){
	    	response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
	    }
	    routingContext.response().end(response);
	}
	
	/**
	 * use to get tweets 
	 * @param twitter
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public ArrayList<Map<String, Object>> getTweets(Twitter twitter, String query) throws Exception{
		return twitter4jApi.getTweets(twitter, query);
	}
	
	/**
	 * use to get users by searching keyword in their profiles
	 * @param twitter
	 * @param keyword
	 * @return
	 * @throws TwitterException
	 */
	public ArrayList<Map<String, Object>> getUsers(Twitter twitter, String keyword) throws TwitterException{
		ArrayList<Map<String, Object>> usersList = twitter4jApi.getUsersProfiles(twitter, keyword);
		return usersList;
	}
	/**
	 * use to get twitter instance
	 * @param consumerKey
	 * @param consumerSecret
	 * @param accessToken
	 * @param accessTokenSecret
	 * @return
	 */

	public Twitter getTwitterInstance(String consumerKey, String consumerSecret, String accessToken,
			String accessTokenSecret) {
		return twitter4jApi.getTwitterInstance(consumerKey, consumerSecret, accessToken, accessTokenSecret);
	}
}
