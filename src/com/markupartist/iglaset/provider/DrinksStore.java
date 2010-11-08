package com.markupartist.iglaset.provider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import android.text.TextUtils;
import android.util.Log;

import com.markupartist.iglaset.util.HttpManager;
import com.markupartist.iglaset.util.StringUtils;

public class DrinksStore {
    private static DrinksStore mInstance;
    private static String TAG = "DrinksStore";
    private static String ARTICLES_BASE_URI =
    	"http://www.iglaset.se/articles.xml";
    private static String ARTICLE_DETAILS_URI =
    	"http://api.iglaset.se/api/articles/xml/";
    private static String RATE_BASE_URI =
        "http://api.iglaset.se/api/rate/";
    /**
     * Base URI for adding an article comment.
     */
    private static String COMMENT_BASE_URI =
    	"http://api.iglaset.se/api/comment/";
    private static String USER_RECOMMENDATIONS_URI =
        "http://api.iglaset.se/api/user_recommendations/xml/";
    private static String USER_RATINGS_URI =
    	"http://www.iglaset.se/users/%d.xml?show=ratings";

    private DrinksStore() {
    }

    public static DrinksStore getInstance() {
        if (mInstance == null) {
            mInstance = new DrinksStore();
        }
        return mInstance;
    }

    public ArrayList<Drink> searchDrinks(SearchCriteria searchCriteria)
            throws IOException {
        final ArrayList<Drink> drinks = new ArrayList<Drink>();
        final HttpGet get = new HttpGet(buildSearchUri(searchCriteria));
        HttpEntity entity = null;

        final HttpResponse response = HttpManager.execute(get);
        entity = response.getEntity();
        DrinksParser drinksParser = new DrinksParser();
        drinksParser.parseDrinks(entity.getContent(), drinks);

        return drinks;
    }

    /**
     * Fins recommendations for the user.
     * @param searchCriteria the search criteria
     * @return list of recommendations
     * @throws IOException on connection problem
     */
    public ArrayList<Drink> findRecommendations(
            RecommendationSearchCriteria searchCriteria)
                throws IOException {
        final ArrayList<Drink> drinks = new ArrayList<Drink>();

        final HttpGet get = new HttpGet(USER_RECOMMENDATIONS_URI
                + searchCriteria.getUserId() + "/"
                + "?page=" + searchCriteria.getPage());
        HttpEntity entity = null;

        final HttpResponse response = HttpManager.execute(get);
        entity = response.getEntity();
        DrinksParser drinksParser = new DrinksParser();
        drinksParser.parseDrinks(entity.getContent(), drinks);

        return drinks;
    }

    /**
     * Find all drinks that the user has rated.
     * @param searchCriteria the search criteria
     * @return list of rated drinks
     * @throws IOException on connection problem
     */
    public ArrayList<Drink> findRatedDrinks(RatingSearchCriteria searchCriteria)
            throws IOException {
        final ArrayList<Drink> drinks = new ArrayList<Drink>();

        final HttpGet get = new HttpGet(
        		String.format(USER_RATINGS_URI, searchCriteria.getUserId())
                + "&page=" + searchCriteria.getPage()
                + "&user_credentials=" + searchCriteria.getAuthentication().v2.token);
        HttpEntity entity = null;

        final HttpResponse response = HttpManager.execute(get);
        entity = response.getEntity();
        DrinksParser drinksParser = new DrinksParser();
        drinksParser.parseDrinks(entity.getContent(), drinks);

        return drinks;
    }
    
    public Drink getDrink(int id) {
        return getDrink(id, null);
    }

    public Drink getDrink(int id, String token) {
        String searchUri = ARTICLE_DETAILS_URI + id;
        if (!TextUtils.isEmpty(token)) {
            searchUri += "/?token=" + token;
        }

        final HttpGet get = new HttpGet(searchUri);
        final ArrayList<Drink> drinks = new ArrayList<Drink>();

        try {
            final HttpResponse response = HttpManager.execute(get);
            HttpEntity entity = response.getEntity();
            DrinksParser drinksParser = new DrinksParser();
            drinksParser.parseDrinks(entity.getContent(), drinks);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read data: " + e.getMessage());
        }

        Drink drink = null;
        if (drinks.size() == 1) {
            drink = drinks.get(0);
        }

        return drink;
    }

    public void rateDrink(Drink drink, float grade, String token) {
        final HttpGet get = new HttpGet(RATE_BASE_URI + drink.getId() 
                + "/" + (int)grade + "/" + token);
        HttpEntity entity = null;

        try {
            final HttpResponse response = HttpManager.execute(get);
            entity = response.getEntity();
            entity.getContent();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read data: " + e.getMessage());
        }
    }

    /**
     * Add a new comment to a drink. This will, unlike Android Market, add
     * an additional comment. It will not overwrite the user's previous one.
     * @param drink Drink to comment.
     * @param comment Comment to add.
     * @param token Authorization token.
     * @return Returns true if successful, false otherwise.
     * @throws IOException on connection problem.
     */
    public Boolean commentDrink(Drink drink, String comment, String token) throws IOException {
    	final HttpPost post = new HttpPost(COMMENT_BASE_URI + drink.getId() + "/" + token);
    	
        ArrayList<NameValuePair> payload = new ArrayList<NameValuePair>(1);
        payload.add(new BasicNameValuePair("comment", comment));
        
        try {
			post.setEntity(new UrlEncodedFormEntity(payload, "utf-8"));
			final HttpResponse response = HttpManager.execute(post);
	        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
	            return true;
	        } else {
	            Log.w(TAG, "Request failed, http status code was not OK.");
	            return false;
	        }
			
		} catch (UnsupportedEncodingException e) {
			// TODO What to do? :(
			e.printStackTrace();
			return false;
		}
    }
    
    /**
     * Builds a the uri for search from a SearchCriteria
     * @param searchCriteria the search criteria
     * @return the search uri
     */
    private String buildSearchUri(SearchCriteria searchCriteria) {
        Log.d(TAG, "building search uri from " + searchCriteria);
    	
    	StringBuilder builder = new StringBuilder();    	
        builder.append(ARTICLES_BASE_URI).append("?");
        builder.append("page=").append(searchCriteria.getPage());

        if (!TextUtils.isEmpty(searchCriteria.getQuery()))
        	builder.append("&str=").append(URLEncoder.encode(searchCriteria.getQuery()));
        if (searchCriteria.getCategory() > 0)
        	builder.append("&category=").append(searchCriteria.getCategory());
        if (!TextUtils.isEmpty(searchCriteria.getAuthentication().v2.token))
        	builder.append("&user_credentials=").append(searchCriteria.getAuthentication().v2.token);

        if (null != searchCriteria.getTags()) {
            builder.append("&tag_filter=or");
        	ArrayList<Integer> tags = searchCriteria.getTags();
        	if(tags.size() > 0) {
        		builder.append("&tags[]=");
        		builder.append(StringUtils.join(tags.toArray(), "&tags[]="));
        	}
        }
        
        return builder.toString();
    }
}
