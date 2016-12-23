package org.xululabs.twitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class Twitter4jApi {

	/**
	 * use to get twitter instance
	 * 
	 * @param consumerKey
	 * @param consumerSecret
	 * @param accessToken
	 * @param accessTokenSecret
	 * @return twitter
	 */

	public Twitter getTwitterInstance(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {

		Twitter twitter = null;		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret)
				.setOAuthAccessToken(accessToken).setOAuthAccessTokenSecret(accessTokenSecret);
		TwitterFactory tf = new TwitterFactory(cb.build());
		twitter = tf.getInstance();
		return twitter;
	}
  /**
   * search keyword
   * @param twitter
   * @param keyword
   * @return tweets
   * @throws Exception
   */
	public ArrayList<Map<String, Object>> getTweets(Twitter twitter, String searchQuery) throws Exception {
		ArrayList<Map<String, Object>> tweets = new ArrayList<Map<String, Object>>();
		Query query = new Query(searchQuery);
		QueryResult queryResult;
		query.setCount(100);
		queryResult = twitter.search(query);
		for (Status tweet : queryResult.getTweets()) {
			Map<String, Object> tweetInfo = new HashMap<String, Object>();
			tweetInfo.put("id", tweet.getId());
			tweetInfo.put("tweet", tweet.getText());
			tweetInfo.put("screenName", tweet.getUser().getScreenName());
			tweetInfo.put("name", tweet.getUser().getName());
			tweetInfo.put("retweetCount", tweet.getRetweetCount());
			tweetInfo.put("followersCount", tweet.getUser().getFollowersCount());
			tweetInfo.put("user_image", tweet.getUser().getProfileImageURL());
			tweetInfo.put("description", tweet.getUser().getDescription());
			tweetInfo.put("retweetedByMe", tweet.isRetweetedByMe());
			tweets.add(tweetInfo);
		}
		return tweets;
	}
	/**
	 * use to get users by searching in their profile
	 * @param twitter
	 * @param searchTerm
	 * @return
	 * @throws TwitterException
	 */
	public ArrayList<Map<String, Object>> getUsersProfiles(Twitter twitter, String searchTerm) throws TwitterException{
		ArrayList<Map<String, Object>> usersList = new ArrayList<Map<String, Object>>();
		ResponseList<User> users = twitter.searchUsers(searchTerm, 1);
		for(User user : users){
			Map<String, Object> userInfo = new HashMap<String, Object>();
			userInfo.put("name", user.getName());
			userInfo.put("screenName", user.getScreenName());
			userInfo.put("image", user.getProfileImageURL());
			userInfo.put("description", user.getDescription());
			usersList.add(userInfo);
		}
		return usersList;
	}
	
	public boolean retweet(Twitter twitter, long tweetId) throws TwitterException{
		Status retweeted = twitter.retweetStatus(tweetId);
		return true;
	}
	
	public ArrayList<Map<String, Object>> createFriendship(Twitter twitter, long userId) throws TwitterException{
	
		ArrayList<Map<String, Object>> tweets = new ArrayList<Map<String, Object>>();
		try {
			User tweet =	twitter.createFriendship(userId);
			
			Map<String, Object> tweetInfo = new HashMap<String, Object>();

			tweetInfo.put("screenName", tweet.getScreenName());
			tweetInfo.put("id", tweet.getId());
			tweetInfo.put("name", tweet.getName());
			tweetInfo.put("followersCount", tweet.getFollowersCount());
			tweetInfo.put("user_image", tweet.getProfileImageURL());
			tweetInfo.put("description", tweet.getDescription());
			tweets.add(tweetInfo);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tweets;
	}
}
