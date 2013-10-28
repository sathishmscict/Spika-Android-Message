/*
 * The MIT License (MIT)
 * 
 * Copyright � 2013 Clover Studio Ltd. All rights reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cloverstudio.spikademo.couchdb;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.cloverstudio.spikademo.SpikaApp;
import com.cloverstudio.spikademo.couchdb.model.ActivitySummary;
import com.cloverstudio.spikademo.couchdb.model.Comment;
import com.cloverstudio.spikademo.couchdb.model.Emoticon;
import com.cloverstudio.spikademo.couchdb.model.Group;
import com.cloverstudio.spikademo.couchdb.model.GroupCategory;
import com.cloverstudio.spikademo.couchdb.model.GroupSearch;
import com.cloverstudio.spikademo.couchdb.model.Message;
import com.cloverstudio.spikademo.couchdb.model.User;
import com.cloverstudio.spikademo.couchdb.model.UserGroup;
import com.cloverstudio.spikademo.couchdb.model.UserSearch;
import com.cloverstudio.spikademo.couchdb.model.WatchingGroupLog;
import com.cloverstudio.spikademo.management.SettingsManager;
import com.cloverstudio.spikademo.management.UsersManagement;
import com.cloverstudio.spikademo.utils.Const;
import com.cloverstudio.spikademo.utils.JSONParser;
import com.cloverstudio.spikademo.utils.Logger;
import com.cloverstudio.spikademo.utils.Utils;

/**
 * CouchDB
 * 
 * Creates and sends requests to CouchDB.
 */

public class CouchDB {

    private final static String groupCategoryCacheKey = "groupCategoryCacheKey";
    private static String TAG = "CouchDB: ";
    private static CouchDB sCouchDB;
    private static String sUrl;
    private static String sAuthUrl;
    private static JSONParser sJsonParser = new JSONParser();
    private static HashMap<String,String> keyValueCache= new HashMap<String,String>();
    
    public CouchDB() {

        /* CouchDB credentials */

        sUrl = Const.API_URL;
        setAuthUrl(Const.AUTH_URL);
        sCouchDB = this;

        new ConnectionHandler();
    }

    public static void saveToMemCache(String key,String value){
        keyValueCache.put(key, value);
    }
    
    public static String getFromMemCache(String key){
        return keyValueCache.get(key);
    }
    
    public static CouchDB getCouchDB() {
        return sCouchDB;
    }

    public static String getUrl() {
        return sUrl;
    }

    /**
     * Upload file
     * 
     * @param filePath
     * @return file ID
     */
    public static String uploadFile(String filePath) {

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (filePath != null && !filePath.equals("")) {
            params.add(new BasicNameValuePair(Const.FILE, filePath));
            String fileId = sJsonParser.getIdFromFileUploader(Const.FILE_UPLOADER_URL, params);
            return fileId;
        }
        return null;
    }

    /**
     * Download file
     * 
     * @param fileId
     * @param file
     * @return
     */
    public static File downloadFile(String fileId, File file) {

        ConnectionHandler.getFile(Const.FILE_DOWNLOADER_URL + Const.FILE + "=" + fileId, file,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());
        return file;
    }
    
    /**
     * Unregister push token
     * 
     * @param userId
     * @return
     */
    public static String unregisterPushToken(String userId) {
    	String result = ConnectionHandler.getString(Const.UNREGISTER_PUSH_URL + Const.USER_ID + "=" + userId,
                UsersManagement.getLoginUser().getId());
        return result;
    }

    /**
     * Create a user
     * 
     * @param name
     * @param email
     * @param password
     * @return
     */
//    public static String createUser(String name, String email, String password) {
//
//        JSONObject userJson = new JSONObject();
//
//        try {
//            userJson.put(Const.NAME, name);
//            userJson.put(Const.PASSWORD, password);
//            userJson.put(Const.TYPE, Const.USER);
//            userJson.put(Const.EMAIL, email);
//            userJson.put(Const.LAST_LOGIN, Utils.getCurrentDateTime());
//            userJson.put(Const.TOKEN_TIMESTAMP, Utils.getCurrentDateTime() / 1000);
//            userJson.put(Const.TOKEN, Utils.generateToken());
//            userJson.put(Const.MAX_CONTACT_COUNT, Const.MAX_CONTACTS);
//            userJson.put(Const.MAX_FAVORITE_COUNT, Const.MAX_FAVORITES);
//            userJson.put(Const.ONLINE_STATUS, Const.ONLINE);
//        } catch (JSONException e) {
//            e.printStackTrace();
//
//            return null;
//        }
//
//        Log.e("Json", userJson.toString());
//        
//        return CouchDBHelper.createUser(ConnectionHandler.postJsonObject("createUser",userJson,
//                Const.CREATE_USER, ""));
//    }
    
    public static void createUser(String name, String email, String password, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new CouchDB.CreateUser(name, email, password), resultListener, context, showProgressBar).execute();
    }
    
    public static class CreateUser implements Command<String>
    {
    	String name;
    	String email;
    	String password;
    	
    	public CreateUser(String name, String email, String password)
    	{
    		this.name = name;
    		this.email = email;
    		this.password = password;
    	}

		@Override
		public String execute() throws JSONException, IOException {
			JSONObject userJson = new JSONObject();

            userJson.put(Const.NAME, name);
            userJson.put(Const.PASSWORD, password);
            userJson.put(Const.TYPE, Const.USER);
            userJson.put(Const.EMAIL, email);
            userJson.put(Const.LAST_LOGIN, Utils.getCurrentDateTime());
            userJson.put(Const.TOKEN_TIMESTAMP, Utils.getCurrentDateTime() / 1000);
            userJson.put(Const.TOKEN, Utils.generateToken());
            userJson.put(Const.MAX_CONTACT_COUNT, Const.MAX_CONTACTS);
            userJson.put(Const.MAX_FAVORITE_COUNT, Const.MAX_FAVORITES);
            userJson.put(Const.ONLINE_STATUS, Const.ONLINE);
	        
	        Log.e("Json", userJson.toString());
	        
	        return CouchDBHelper.createUser(ConnectionHandler.postJsonObject("createUser",userJson,
	                Const.CREATE_USER, ""));
		}
    }

    /**
     * Returns true if the provided username is already taken.
     * 
     * @param username
     *            String value that will be checked
     * @return true if the provided string is already taken username, otherwise
     *         false
     */
    @Deprecated
    public static User getUserByName(String username) {
    	
    	String params = "";
    	try {
			params = URLEncoder.encode(username, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

        final String URL = Const.CHECKUNIQUE_URL + "username=" + params;

        JSONArray jsonArray = ConnectionHandler.getJsonArrayDeprecated(URL, null, null);

        if (jsonArray.length() == 0)
            return null;

        User user = null;

        try {
            user = CouchDBHelper.parseSingleUserObjectWithoutRowParam(jsonArray.getJSONObject(0));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return user;
    }
    
    public static void getUserByName(String username, ResultListener<User> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, User>(new CouchDB.GetUserByName(username), resultListener, context, showProgressBar).execute();
    }
    
    public static class GetUserByName implements Command<User>
    {
    	String username;
    	
    	public GetUserByName (String username)
    	{
    		this.username = username;
    	}
    	
		@Override
		public User execute() throws JSONException, IOException {
			String params = "";
	    	try {
				params = URLEncoder.encode(username, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

	        final String URL = Const.CHECKUNIQUE_URL + "username=" + params;

	        JSONArray jsonArray = ConnectionHandler.getJsonArray(URL, null, null);

	        if (jsonArray.length() == 0)
	            return null;

	        User user = CouchDBHelper.parseSingleUserObjectWithoutRowParam(jsonArray.getJSONObject(0));

	        return user;
		}
    }

    /**
     * Returns true if the provided email is already taken.
     * 
     * @param email
     *            String value that will be checked
     * @return true if the provided string is already taken email, otherwise
     *         false
     */
    @Deprecated
    public static User getUserByEmail(String email) {
    	
    	String params = "";
    	try {
			params = URLEncoder.encode(email, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

        final String URL = Const.CHECKUNIQUE_URL + "email=" + params;

        JSONArray jsonArray = ConnectionHandler.getJsonArrayDeprecated(URL, null, null);

        if (jsonArray.length() == 0)
            return null;

        User user = null;

        try {
            user = CouchDBHelper.parseSingleUserObjectWithoutRowParam(jsonArray.getJSONObject(0));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return user;
    }
    
    public static void getUserByEmail(String email, ResultListener<User> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, User>(new CouchDB.GetUserByEmail(email), resultListener, context, showProgressBar).execute();
    }
    
    public static class GetUserByEmail implements Command<User>
    {
    	String email;
    	
    	public GetUserByEmail (String email)
    	{
    		this.email = email;
    	}
    	
		@Override
		public User execute() throws JSONException, IOException {
			String params = "";
	    	try {
				params = URLEncoder.encode(email, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

	        final String URL = Const.CHECKUNIQUE_URL + "email=" + params;

	        JSONArray jsonArray = ConnectionHandler.getJsonArray(URL, null, null);

	        if (jsonArray.length() == 0)
	            return null;

	        User user = CouchDBHelper.parseSingleUserObjectWithoutRowParam(jsonArray.getJSONObject(0));

	        return user;
		}
    }

    /**
     * Returns group by group name
     * 
     * @param groupname
     *            String value that will be used for search
     * @return true if the provided string is already taken email, otherwise
     *         false
     */
    @Deprecated
    public static Group getGroupByName(String groupname) {
    	
    	String params = "";
    	try {
			params = URLEncoder.encode(groupname, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

        final String URL = Const.CHECKUNIQUE_URL + "groupname=" + params;

        JSONArray jsonArray = ConnectionHandler.getJsonArrayDeprecated(URL, null, null);

        if (jsonArray.length() == 0)
            return null;

        Group group = null;

        try {
            group = CouchDBHelper.parseSingleGroupObjectWithoutRowParam(jsonArray.getJSONObject(0));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return group;
    }
    
    public static void getGroupByName(String groupname, ResultListener<Group> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Group>(new CouchDB.GetGroupByName(groupname), resultListener, context, showProgressBar).execute();
    }
    
    public static class GetGroupByName implements Command<Group>
    {
    	String groupname;
    	
    	public GetGroupByName (String groupname)
    	{
    		this.groupname = groupname;
    	}
    	
		@Override
		public Group execute() throws JSONException, IOException {
			String params = "";
	    	try {
				params = URLEncoder.encode(groupname, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

	        final String URL = Const.CHECKUNIQUE_URL + "groupname=" + params;

	        JSONArray jsonArray = ConnectionHandler.getJsonArray(URL, null, null);

	        if (jsonArray.length() == 0)
	            return null;

	        Group group = CouchDBHelper.parseSingleGroupObjectWithoutRowParam(jsonArray.getJSONObject(0));

	        return group;
		}
    	
    }

    /**
     * Method used for updating user attributes
     * 
     * If you add some new attributes to user object you must also add code to
     * add that data to userJson
     * 
     * @param user
     * @return user object
     */
    @Deprecated
    public static boolean updateUser(User user) {

        JSONObject userJson = new JSONObject();
        List<String> contactIds = new ArrayList<String>();
        List<String> groupIds = new ArrayList<String>();

//        User newUser = CouchDB.findUserById(user.getId());

        try {
            /* General user info */
            userJson.put(Const._ID, user.getId());
            userJson.put(Const._REV, user.getRev());
            userJson.put(Const.EMAIL, user.getEmail());
            userJson.put(Const.NAME, user.getName());
            userJson.put(Const.TYPE, Const.USER);
            userJson.put(Const.PASSWORD, SpikaApp.getPreferences().getUserPassword());
            userJson.put(Const.LAST_LOGIN, user.getLastLogin());
            userJson.put(Const.ABOUT, user.getAbout());
            userJson.put(Const.BIRTHDAY, user.getBirthday());
            userJson.put(Const.GENDER, user.getGender());
            userJson.put(Const.TOKEN, SpikaApp.getPreferences().getUserToken());
            userJson.put(Const.TOKEN_TIMESTAMP, user.getTokenTimestamp());
            userJson.put(Const.ANDROID_PUSH_TOKEN, SpikaApp.getPreferences().getUserPushToken());
            userJson.put(Const.ONLINE_STATUS, user.getOnlineStatus());
            userJson.put(Const.AVATAR_FILE_ID, user.getAvatarFileId());
            userJson.put(Const.MAX_CONTACT_COUNT, user.getMaxContactCount());
            userJson.put(Const.AVATAR_THUMB_FILE_ID, user.getAvatarThumbFileId());


            // JSONObject imageJPG = new JSONObject();
            // if (!user.getAttachments().isEmpty()) {
            // JSONObject imageData;
            // for (Attachment attachment : user.getAttachments()) {
            // imageData = new JSONObject();
            // try {
            // boolean stub = true;
            // imageData.put(Const.CONTENT_TYPE,
            // attachment.getContentType());
            // imageData.put(Const.REVPOS, attachment.getRevpos());
            // imageData.put(Const.STUB, stub);
            // imageData.put(Const.LENGTH, attachment.getLength());
            // imageJPG.put(attachment.getName(), imageData);
            //
            // } catch (JSONException e) {
            // e.printStackTrace();
            // }
            // }
            // }
            //
            // if (bitmapImage != null) {
            // /* Set a new avatar */
            //
            // JSONObject imageData = new JSONObject();
            // user.setAvatarName(getNewAvatarName(user.getAvatarName(),
            // Const.USER_AVATAR));
            //
            // try {
            // ByteArrayOutputStream stream = new ByteArrayOutputStream();
            // bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100,
            // stream);
            // byte[] byteArray = stream.toByteArray();
            // StringBuilder encoded = new StringBuilder(
            // Base64.encodeToString(byteArray, Base64.NO_WRAP));
            // imageData.put(Const.DATA, encoded);
            // imageData.put(Const.CONTENT_TYPE, "image/jpeg");
            // imageJPG.put(user.getAvatarName(), imageData);
            // } catch (JSONException e) {
            // e.printStackTrace();
            // }
            // // userJson.put(Const.ATTACHMENTS, imageJPG);
            // user.setImageUrl(sUrl + user.getId() + "/"
            // + user.getAvatarName());
            // }
            // userJson.put(Const.AVATAR_NAME, user.getAvatarName());
            // userJson.put(Const.ATTACHMENTS, imageJPG);

            /* Set users favorite contacts */
            JSONArray contactsArray = new JSONArray();
            contactIds = user.getContactIds();
            if (!contactIds.isEmpty()) {
                for (String id : contactIds) {
                    contactsArray.put(id);
                }
            }
            if (contactsArray.length() > 0) {
                userJson.put(Const.CONTACTS, contactsArray);
            }

            /* Set users favorite groups */
            JSONArray groupsArray = new JSONArray();
            groupIds = user.getGroupIds();

            if (!groupIds.isEmpty()) {
                for (String id : groupIds) {
                    groupsArray.put(id);
                }
            }

            if (groupsArray.length() > 0) {
                userJson.put(Const.FAVORITE_GROUPS, groupsArray);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject json = ConnectionHandler.putJsonObject(userJson, user.getId(), user.getId(),
                user.getToken());

        return CouchDBHelper.updateUser(json, contactIds, groupIds);
    }
    
    public static void updateUser (User user, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new UpdateUser(user), resultListener, context, showProgressBar).execute();;
    }
    
    public static class UpdateUser implements Command<Boolean>
    {
    	User user;
    	
    	public UpdateUser (User user)
    	{
    		this.user = user;
    	}

		@Override
		public Boolean execute() throws JSONException, IOException {
			JSONObject userJson = new JSONObject();
	        List<String> contactIds = new ArrayList<String>();
	        List<String> groupIds = new ArrayList<String>();

            /* General user info */
            userJson.put(Const._ID, user.getId());
            userJson.put(Const._REV, user.getRev());
            userJson.put(Const.EMAIL, user.getEmail());
            userJson.put(Const.NAME, user.getName());
            userJson.put(Const.TYPE, Const.USER);
            userJson.put(Const.PASSWORD, SpikaApp.getPreferences().getUserPassword());
            userJson.put(Const.LAST_LOGIN, user.getLastLogin());
            userJson.put(Const.ABOUT, user.getAbout());
            userJson.put(Const.BIRTHDAY, user.getBirthday());
            userJson.put(Const.GENDER, user.getGender());
            userJson.put(Const.TOKEN, SpikaApp.getPreferences().getUserToken());
            userJson.put(Const.TOKEN_TIMESTAMP, user.getTokenTimestamp());
            userJson.put(Const.ANDROID_PUSH_TOKEN, SpikaApp.getPreferences().getUserPushToken());
            userJson.put(Const.ONLINE_STATUS, user.getOnlineStatus());
            userJson.put(Const.AVATAR_FILE_ID, user.getAvatarFileId());
            userJson.put(Const.MAX_CONTACT_COUNT, user.getMaxContactCount());
            userJson.put(Const.AVATAR_THUMB_FILE_ID, user.getAvatarThumbFileId());

            /* Set users favorite contacts */
            JSONArray contactsArray = new JSONArray();
            contactIds = user.getContactIds();
            if (!contactIds.isEmpty()) {
                for (String id : contactIds) {
                    contactsArray.put(id);
                }
            }
            if (contactsArray.length() > 0) {
                userJson.put(Const.CONTACTS, contactsArray);
            }

            /* Set users favorite groups */
            JSONArray groupsArray = new JSONArray();
            groupIds = user.getGroupIds();

            if (!groupIds.isEmpty()) {
                for (String id : groupIds) {
                    groupsArray.put(id);
                }
            }

            if (groupsArray.length() > 0) {
                userJson.put(Const.FAVORITE_GROUPS, groupsArray);
            }

	        JSONObject json = ConnectionHandler.putJsonObject(userJson, user.getId(), user.getId(), user.getToken());

	        return CouchDBHelper.updateUser(json, contactIds, groupIds);
		}
    }

    /**
     * Find all users
     * 
     * @return
     */
    @Deprecated
    public static List<User> findAllUsers() {

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_user_by_email", UsersManagement.getLoginUser().getId());

        return CouchDBHelper.parseMultiUserObjects(json);
    }
    
    public static void findAllUsers (ResultListener<List<User>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<User>>(new FindAllUsers(), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindAllUsers implements Command<List<User>>
    {
		@Override
		public List<User> execute() throws JSONException, IOException {
			JSONObject json = ConnectionHandler.getJsonObject(sUrl
	                + "_design/app/_view/find_user_by_email", UsersManagement.getLoginUser().getId());

	        return CouchDBHelper.parseMultiUserObjects(json);
		}
    }

    /**
     * Finds users given the search criteria in userSearch
     * 
     * @param userSearch
     * @return
     */
    @Deprecated
    public static List<User> searchUsers(UserSearch userSearch) {

        String searchParams = "";

        if (userSearch.getName() != null) {
            try {
                userSearch.setName(URLEncoder.encode(userSearch.getName(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
            searchParams = "n=" + userSearch.getName();
        }

        if (userSearch.getFromAge() != null && !"".equals(userSearch.getFromAge())) {
            searchParams += "&af=" + userSearch.getFromAge();
        }
        if (userSearch.getToAge() != null && !"".equals(userSearch.getToAge())) {
            searchParams += "&at=" + userSearch.getToAge();
        }
        if (userSearch.getGender() != null
                && (userSearch.getGender().equals(Const.FEMALE) || userSearch.getGender().equals(
                        Const.MALE))) {
            searchParams += "&g=" + userSearch.getGender();
        }
        if (userSearch.getOnlineStatus() != null && !userSearch.getOnlineStatus().equals("")) {
        	searchParams += "&status=" + userSearch.getOnlineStatus();
        }
        
        Logger.error("Search", Const.SEARCH_USERS_URL + searchParams);

        JSONArray json = ConnectionHandler.getJsonArrayDeprecated(Const.SEARCH_USERS_URL + searchParams,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());

        return CouchDBHelper.parseSearchUsersResult(json);
    }
    
    public static void searchUsers(UserSearch userSearch, ResultListener<List<User>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<User>>(new SearchUsers(userSearch), resultListener, context, showProgressBar).execute();
    }
    
    public static class SearchUsers implements Command<List<User>>
    {
    	UserSearch userSearch;
    	
    	public SearchUsers (UserSearch userSearch)
    	{
    		this.userSearch = userSearch;
    	}

		@Override
		public List<User> execute() throws JSONException, IOException {
			String searchParams = "";

	        if (userSearch.getName() != null) {
	            try {
	                userSearch.setName(URLEncoder.encode(userSearch.getName(), "UTF-8"));
	            } catch (UnsupportedEncodingException e) {
	                e.printStackTrace();
	                return null;
	            }
	            searchParams = "n=" + userSearch.getName();
	        }

	        if (userSearch.getFromAge() != null && !"".equals(userSearch.getFromAge())) {
	            searchParams += "&af=" + userSearch.getFromAge();
	        }
	        if (userSearch.getToAge() != null && !"".equals(userSearch.getToAge())) {
	            searchParams += "&at=" + userSearch.getToAge();
	        }
	        if (userSearch.getGender() != null
	                && (userSearch.getGender().equals(Const.FEMALE) || userSearch.getGender().equals(
	                        Const.MALE))) {
	            searchParams += "&g=" + userSearch.getGender();
	        }
	        if (userSearch.getOnlineStatus() != null && !userSearch.getOnlineStatus().equals("")) {
	        	searchParams += "&status=" + userSearch.getOnlineStatus();
	        }
	        
	        Logger.error("Search", Const.SEARCH_USERS_URL + searchParams);

	        JSONArray json = ConnectionHandler.getJsonArray(Const.SEARCH_USERS_URL + searchParams, UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());

	        return CouchDBHelper.parseSearchUsersResult(json);
		}
    }

    /**
     * Finds groups given the search criteria in groupSearch
     * 
     * @param groupSearch
     * @return
     */
    @Deprecated
    public static List<Group> searchGroups(GroupSearch groupSearch) {

        String searchParams = "";

        if (groupSearch.getName() != null) {
            try {
                groupSearch.setName(URLEncoder.encode(groupSearch.getName(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
            searchParams = "n=" + groupSearch.getName();
        }

        JSONArray json = ConnectionHandler.getJsonArrayDeprecated(Const.SEARCH_GROUPS_URL + searchParams,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());

        return CouchDBHelper.parseSearchGroupsResult(json);
    }
    
    public static void searchGroups (GroupSearch groupSearch, ResultListener<List<Group>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<Group>>(new SearchGroups(groupSearch), resultListener, context, showProgressBar).execute();
    }
    
    public static class SearchGroups implements Command<List<Group>>
    {
    	GroupSearch groupSearch;
    	
    	public SearchGroups (GroupSearch groupSearch)
    	{
    		this.groupSearch = groupSearch;
    	}

		@Override
		public List<Group> execute() throws JSONException, IOException {
			String searchParams = "";

	        if (groupSearch.getName() != null) {
	            try {
	                groupSearch.setName(URLEncoder.encode(groupSearch.getName(), "UTF-8"));
	            } catch (UnsupportedEncodingException e) {
	                e.printStackTrace();
	                return null;
	            }
	            searchParams = "n=" + groupSearch.getName();
	        }

	        JSONArray json = ConnectionHandler.getJsonArray(Const.SEARCH_GROUPS_URL + searchParams, UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());

	        return CouchDBHelper.parseSearchGroupsResult(json);
		}
    }

    /**
     * Find user by email
     * 
     * @param email
     * @return
     */
    @Deprecated
    public static User findUserByEmail(String email, boolean isLoggedIn) {

        email = "\"" + email + "\"";
        
        try {
            email = URLEncoder.encode(email, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        JSONObject json = null;

        if (null != UsersManagement.getLoginUser() && isLoggedIn) {
            json = ConnectionHandler.getJsonObject(sUrl
                    + "_design/app/_view/find_user_by_email?key=" + email, UsersManagement
                    .getLoginUser().getId());

            try {
                return CouchDBHelper.parseSingleUserObject(json);
            } catch (JSONException e) {
                Logger.error(TAG + "parseSingleUserObject",
                        "Error while retrieving data from json... Probably no user find!", e);

                return null;
            }
        } else {
            json = ConnectionHandler.getJsonObject(Const.API_URL
                    + "_design/app/_view/find_user_by_email?key=" + email, "");
            
            try {
                return CouchDBHelper.parseSingleUserObject(json);
            } catch (JSONException e) {
                Logger.error(TAG + "parseSingleUserObject",
                        "Error while retrieving data from json... Probably no user find!", e);

                return null;
            }
        }
    }
    
    public static void findUserByEmail(String email, boolean isLoggedIn, ResultListener<User> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, User>(new CouchDB.FindUserByEmail(email, isLoggedIn), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindUserByEmail implements Command<User>
    {
    	String email;
    	boolean isLoggedIn;
    	
    	public FindUserByEmail(String email, boolean isLoggedIn)
    	{
    		this.email = email;
    		this.isLoggedIn = isLoggedIn;
    	}

		@Override
		public User execute() throws JSONException, IOException {
			email = "\"" + email + "\"";
	        
	        try {
	            email = URLEncoder.encode(email, "UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            e.printStackTrace();
	            return null;
	        }

	        JSONObject json = null;

	        if (null != UsersManagement.getLoginUser() && isLoggedIn) {
	            json = ConnectionHandler.getJsonObject(sUrl
	                    + "_design/app/_view/find_user_by_email?key=" + email, UsersManagement
	                    .getLoginUser().getId());
	            
	            return CouchDBHelper.parseSingleUserObject(json);
	            
	        } else {
	            json = ConnectionHandler.getJsonObject(Const.API_URL
	                    + "_design/app/_view/find_user_by_email?key=" + email, "");
	            	                    
	            return CouchDBHelper.parseSingleUserObject(json);
	        }
		}
    }

    /**
     * Email SignIn, Auth
     * 
     * @param email
     * @return
     * @throws JSONException 
     * @throws IOException 
     */
    @Deprecated
    public static String auth(String email, String password) throws IOException, JSONException {

        JSONObject jPost = new JSONObject();

        try {
            jPost.put("email", email);
            jPost.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject json = ConnectionHandler.postAuth(jPost);

        User user = null;

        Log.e("CouchDB", "auth");

        user = CouchDBHelper.parseSingleUserObject(json);
            
        if (user != null) {

        	SpikaApp.getPreferences().setUserToken(user.getToken());
        	SpikaApp.getPreferences().setUserEmail(user.getEmail());
        	SpikaApp.getPreferences().setUserId(user.getId());
        	SpikaApp.getPreferences().setUserPassword(user.getPassword());

        	UsersManagement.setLoginUser(user);
        	UsersManagement.setToUser(user);
        	UsersManagement.setToGroup(null);

        	return Const.LOGIN_SUCCESS;
        } else {
        	return Const.LOGIN_ERROR;
        }
    }
    
    public static void auth(String email, String password, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new CouchDB.Auth(email, password), resultListener, context, showProgressBar).execute();
    }
    
    public static class Auth implements Command<String>
    {
    	String email;
    	String password;
    	
    	public Auth (String email, String password)
    	{
    		this.email = email;
    		this.password = password;
    	}

		@Override
		public String execute() throws JSONException, IOException {
			JSONObject jPost = new JSONObject();

	        jPost.put("email", email);
	        jPost.put("password", password);

	        JSONObject json = ConnectionHandler.postAuth(jPost);

	        User user = null;

	        Log.e("CouchDB", "auth");

	        user = CouchDBHelper.parseSingleUserObject(json);
	            
	        if (user != null) {

	        	SpikaApp.getPreferences().setUserToken(user.getToken());
	        	SpikaApp.getPreferences().setUserEmail(user.getEmail());
	        	SpikaApp.getPreferences().setUserId(user.getId());
	        	SpikaApp.getPreferences().setUserPassword(user.getPassword());

	        	UsersManagement.setLoginUser(user);
	        	UsersManagement.setToUser(user);
	        	UsersManagement.setToGroup(null);

	        	return Const.LOGIN_SUCCESS;
	        } else {
	        	return Const.LOGIN_ERROR;
	        }
		}
    }

    /**
     * Email SignIn, Auth
     * 
     * @param email
     * @return
     * @throws JSONException 
     * @throws IOException 
     */
    @Deprecated
    public static User getUserByEmailAndPassword(String email, String password) throws JSONException, IOException {

        JSONObject jPost = new JSONObject();

        jPost.put("email", email);
        jPost.put("password", password);

        JSONObject json = ConnectionHandler.postAuth(jPost);

        User user = null;

        Log.e("CouchDB", "auth");

        user = CouchDBHelper.parseSingleUserObject(json);

        if (user != null) {

        	SpikaApp.getPreferences().setUserToken(user.getToken());
        	SpikaApp.getPreferences().setUserEmail(user.getEmail());
        	SpikaApp.getPreferences().setUserId(user.getId());
        	SpikaApp.getPreferences().setUserPassword(user.getPassword());

        	UsersManagement.setLoginUser(user);
        	UsersManagement.setToUser(user);
        	UsersManagement.setToGroup(null);

        	return user;
        } else {
        	return null;
        }
    }

    public static void getUserByEmailAndPassword(String email, String password, ResultListener<User> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, User>(new CouchDB.GetUserByEmailAndPassword(email, password), resultListener, context, showProgressBar).execute();
    }
    
    public static class GetUserByEmailAndPassword implements Command<User>
    {
    	String email;
    	String password;
    	
    	public GetUserByEmailAndPassword (String email, String password)
    	{
    		this.email = email;
    		this.password = password;
    	}
    	
		@Override
		public User execute() throws JSONException, IOException {
			JSONObject jPost = new JSONObject();

	        jPost.put("email", email);
	        jPost.put("password", password);

	        JSONObject json = ConnectionHandler.postAuth(jPost);

	        User user = null;

	        Log.e("CouchDB", "auth");

	        user = CouchDBHelper.parseSingleUserObject(json);

	        if (user != null) {

	        	SpikaApp.getPreferences().setUserToken(user.getToken());
	        	SpikaApp.getPreferences().setUserEmail(user.getEmail());
	        	SpikaApp.getPreferences().setUserId(user.getId());
	        	SpikaApp.getPreferences().setUserPassword(user.getPassword());

	        	UsersManagement.setLoginUser(user);
	        	UsersManagement.setToUser(user);
	        	UsersManagement.setToGroup(null);

	        	return user;
	        } else {
	        	return null;
	        }
		}
    }
    
    /**
     * Find user by name
     * 
     * @param name
     * @return
     */
    @Deprecated
    public static List<User> findUsersByName(String name) {

        String endKey = "\"" + name + "\u9999\"";
        name = "\"" + name + "\"";

        try {
            name = URLEncoder.encode(name, "UTF-8");
            endKey = URLEncoder.encode(endKey, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        String url = sUrl + "_design/app/_view/find_user_by_name?startkey=" + name + "&endkey="
                + endKey;

        JSONObject json = ConnectionHandler.getJsonObject(url, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.parseMultiUserObjects(json);
    }
    
    public static void findUsersByName(String name, ResultListener<List<User>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<User>>(new CouchDB.FindUsersByName(name), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindUsersByName implements Command<List<User>> {

    	String name;
    	
		public FindUsersByName(String name) {
			this.name = name;
		}

		@Override
		public List<User> execute() throws JSONException, IOException {
			String endKey = "\"" + name + "\u9999\"";
	        name = "\"" + name + "\"";

	        try {
	            name = URLEncoder.encode(name, "UTF-8");
	            endKey = URLEncoder.encode(endKey, "UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            e.printStackTrace();
	            return null;
	        }

	        String url = sUrl + "_design/app/_view/find_user_by_name?startkey=" + name + "&endkey="
	                + endKey;

	        JSONObject json = ConnectionHandler.getJsonObject(url, UsersManagement.getLoginUser()
	                .getId());

	        return CouchDBHelper.parseMultiUserObjects(json);
		}
    }

    /**
     * Find user by name, age and gender
     * 
     * @param name
     * @return
     */
    // TODO
    @Deprecated
    public static List<User> findUsersByNameAgeGender(String name, String age, String gender) {

        //long time = System.currentTimeMillis() / 1000;
        //long timeStart = time - 315569520;

        String endKey = "[\"" + name + "\u9999\",\"" + gender + "\u9999\",{}]";

        name = "\"" + name + "\"";
        age = "\"" + age + "\"";
        gender = "\"" + gender + "\"";

        String startKey = "[" + name + "," + gender + "]";

        try {
            endKey = URLEncoder.encode(endKey, "UTF-8");
            startKey = URLEncoder.encode(startKey, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

            return null;
        }

        // String url=_url+"_design/app/_view/find_user_by_name?startkey=" +
        // name
        // +"&endkey="+endKey;

        String url = sUrl + "_design/app/_view/find_user_by_name_gender_age?startkey=" + startKey
                + "&endkey=" + endKey;

        // String url=_url+"_design/app/_view/find_user_by_name_gender_age?key="
        // + startKey;

        JSONObject json = ConnectionHandler.getJsonObject(url, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.parseMultiUserObjects(json);
    }
    
    public static void findUsersByNameAndGender (String name, String age, String gender, ResultListener<List<User>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<User>>(new FindUsersByNameAndGender(name, age, gender), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindUsersByNameAndGender implements Command<List<User>>
    {
    	String name;
    	String age;
    	String gender;
    	
    	public FindUsersByNameAndGender (String name, String age, String gender)
    	{
    		this.name = name;
    		this.age = age;
    		this.gender = gender;
    	}

		@Override
		public List<User> execute() throws JSONException, IOException {
			String endKey = "[\"" + name + "\u9999\",\"" + gender + "\u9999\",{}]";

	        name = "\"" + name + "\"";
	        age = "\"" + age + "\"";
	        gender = "\"" + gender + "\"";

	        String startKey = "[" + name + "," + gender + "]";

	        try {
	            endKey = URLEncoder.encode(endKey, "UTF-8");
	            startKey = URLEncoder.encode(startKey, "UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            e.printStackTrace();

	            return null;
	        }

	        String url = sUrl + "_design/app/_view/find_user_by_name_gender_age?startkey=" + startKey
	                + "&endkey=" + endKey;

	        JSONObject json = ConnectionHandler.getJsonObject(url, UsersManagement.getLoginUser()
	                .getId());

	        return CouchDBHelper.parseMultiUserObjects(json);
		}
    }

    /**
     * Find user by id
     * 
     * @param id
     * @return
     */
    @Deprecated
    public static User findUserById(String id) {

        id = "\"" + id + "\"";

        try {
            id = URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();

            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_user_by_id?key=" + id, UsersManagement.getLoginUser()
                .getId());

        Log.e("CouchDB", "findUserById");
        try {
            return CouchDBHelper.parseSingleUserObject(json);
        } catch (JSONException e) {
            Logger.error(TAG + "parseSingleUserObject",
                    "Error while retrieving data from json... Probably no user find!", e);

            return null;
        }
    }
    
    public static void findUserById(String id, ResultListener<User> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, User>(new CouchDB.FindUserById(id), resultListener, context, showProgressBar).execute();
    }

    public static class FindUserById implements Command<User>
    {
    	String id;
    	
    	public FindUserById (String id)
    	{
    		this.id = id;
    	}
    	
		@Override
		public User execute() throws JSONException, IOException {
			
			id = "\"" + id + "\"";

	        try {
	            id = URLEncoder.encode(id, "UTF-8");
	        } catch (UnsupportedEncodingException e1) {
	            e1.printStackTrace();

	            return null;
	        }

	        JSONObject json = ConnectionHandler.getJsonObject(sUrl
	                + "_design/app/_view/find_user_by_id?key=" + id, UsersManagement.getLoginUser()
	                .getId());

	        Log.e("CouchDB", "findUserById");
	        
	        return CouchDBHelper.parseSingleUserObject(json); 
		}
    }
    
    @Deprecated
    public static String findAvatarFileId(String userId) {

        userId = "\"" + userId + "\"";

        try {
            userId = URLEncoder.encode(userId, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_avatar_file_id?key=" + userId, UsersManagement
                .getLoginUser().getId());

        return CouchDBHelper.findAvatarFileId(json);
    }
    
    public static void findAvatarById (String userId, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new FindAvatarFileId(userId), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindAvatarFileId implements Command<String>
    {
    	String userId;
    	
    	public FindAvatarFileId (String userId)
    	{
    		this.userId = userId; 
    	}

		@Override
		public String execute() throws JSONException, IOException {
			userId = "\"" + userId + "\"";

	        try {
	            userId = URLEncoder.encode(userId, "UTF-8");
	        } catch (UnsupportedEncodingException e1) {
	            e1.printStackTrace();
	            return null;
	        }

	        JSONObject json = ConnectionHandler.getJsonObject(sUrl
	                + "_design/app/_view/find_avatar_file_id?key=" + userId, UsersManagement
	                .getLoginUser().getId());

	        return CouchDBHelper.findAvatarFileId(json);
		}
    }

    /**
     * Find users favorite contacts
     * 
     * @return
     */
    @Deprecated
    public static List<User> findUserContacts(String id) {

        id = "\"" + id + "\"";

        try {
            id = URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_contacts?key=" + id + "&include_docs=true",
                UsersManagement.getLoginUser().getId());

        return CouchDBHelper.parseUserContacts(json);
    }

    public void findUserContacts (String id, ResultListener<List<User>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<User>>(new FindUserContacts(id), resultListener, context, showProgressBar).execute();
    }
    
    public class FindUserContacts implements Command<List<User>>
    {
    	String id;
    	
    	public FindUserContacts (String id)
    	{
    		this.id = id;
    	}

		@Override
		public List<User> execute() throws JSONException, IOException {
			id = "\"" + id + "\"";

	        try {
	            id = URLEncoder.encode(id, "UTF-8");
	        } catch (UnsupportedEncodingException e) {

	            e.printStackTrace();
	            return null;
	        }

	        JSONObject json = ConnectionHandler.getJsonObject(sUrl
	                + "_design/app/_view/find_contacts?key=" + id + "&include_docs=true",
	                UsersManagement.getLoginUser().getId());

	        return CouchDBHelper.parseUserContacts(json);
		}
    }
    
    /**
     * Add favorite user contact to current logged in user
     * 
     * @param userId
     * @return
     */
    @Deprecated
    public static boolean addUserContact(String userId) {

        User user = UsersManagement.getLoginUser();
        User userUpdated = CouchDB.findUserById(user.getId());

        userUpdated.getContactIds().add(userId);

        return CouchDB.updateUser(userUpdated);
    }
    
    public static void addUserContact (final String userId, final ResultListener<Boolean> resultListener, final Context context, final boolean showProgressBar)
    {
    	 User user = UsersManagement.getLoginUser();
    	 
    	 findUserById(user.getId(), new ResultListener<User>() {
			
			@Override
			public void onResultsSucceded(User result) {
				User userUpdated = result;
				userUpdated.getContactIds().add(userId);
				CouchDB.updateUser(userUpdated, resultListener, context, showProgressBar);
			}
			
			@Override
			public void onResultsFail() {
			}
		}, context, showProgressBar);
    }

    /**
     * Remove a user from favorite user contacts of current logged in user
     * 
     * @param userId
     * @return
     */
    @Deprecated
    public static boolean removeUserContact(String userId) {

        User user = CouchDB.findUserById(UsersManagement.getLoginUser().getId());

        List<String> currentContactIds = UsersManagement.getLoginUser().getContactIds();

        if (!currentContactIds.isEmpty()) {

            List<String> newContactIds = new ArrayList<String>();

            for (String id : currentContactIds) {
                if (!id.equals(userId)) {
                    newContactIds.add(id);
                }
            }

            user.setContactIds(newContactIds);

            return CouchDB.updateUser(user);
        }

        return false;
    }
    
    public static void removeUserContact (final String userId, final ResultListener<Boolean> resultListener, final Context context, final boolean showProgressBar)
    {
    	CouchDB.findUserById(UsersManagement.getLoginUser().getId(), new ResultListener<User>() {
			
			@Override
			public void onResultsSucceded(User result) {
				User user = result;
				
				List<String> currentContactIds = UsersManagement.getLoginUser().getContactIds();
				
				if (!currentContactIds.isEmpty()) {

		            List<String> newContactIds = new ArrayList<String>();

		            for (String id : currentContactIds) {
		                if (!id.equals(userId)) {
		                    newContactIds.add(id);
		                }
		            }

		            user.setContactIds(newContactIds);

		            CouchDB.updateUser(user, resultListener, context, showProgressBar);
		        }
				else
				{
					resultListener.onResultsSucceded(false);
				}
			}
			
			@Override
			public void onResultsFail() {
			}
		}, context, showProgressBar);
    }

    /**
     * Find all groups
     * 
     * @return
     */
    @Deprecated
    public static List<Group> findAllGroups() {

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_group_by_name", UsersManagement.getLoginUser().getId());

        return CouchDBHelper.parseMultiGroupObjects(json);
    }
    
    public static void findAllGroups (ResultListener<List<Group>> resultListener, Context context, boolean showProgressBar)
    {
    	new SpikaAsyncTask<Void, Void, List<Group>>(new FindAllGroups(), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindAllGroups implements Command<List<Group>>
    {
		@Override
		public List<Group> execute() throws JSONException, IOException {
			JSONObject json = ConnectionHandler.getJsonObject(sUrl
	                + "_design/app/_view/find_group_by_name", UsersManagement.getLoginUser().getId());

	        return CouchDBHelper.parseMultiGroupObjects(json);
		}	
    }

    /**
     * Find group by id
     * 
     * @return
     */
    @Deprecated
    public static Group findGroupById(String id) {

        id = "\"" + id + "\"";

        try {
            id = URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_group_by_id?key=" + id, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.parseSingleGroupObject(json);
    }
    
    public static void findGroupById(String id, ResultListener<Group> resultListener, Context context, boolean showProgressBar)
    {
    	new SpikaAsyncTask<Void, Void, Group>(new FindGroupById(id), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindGroupById implements Command<Group>
    {
    	String id;
    	
    	public FindGroupById (String id)
    	{
    		this.id = id;
    	}

		@Override
		public Group execute() throws JSONException, IOException {
			id = "\"" + id + "\"";

	        try {
	            id = URLEncoder.encode(id, "UTF-8");
	        } catch (UnsupportedEncodingException e) {

	            e.printStackTrace();
	            return null;
	        }

	        JSONObject json = ConnectionHandler.getJsonObject(sUrl
	                + "_design/app/_view/find_group_by_id?key=" + id, UsersManagement.getLoginUser()
	                .getId());

	        return CouchDBHelper.parseSingleGroupObject(json);
		}
    }

    /**
     * Find group/groups by name
     * 
     * @param name
     * @return
     */
    @Deprecated
    public static List<Group> findGroupsByName(String name) {

        String endKey = "\"" + name + "\u9999\"";
        name = "\"" + name + "\"";

        try {
            name = URLEncoder.encode(name, "UTF-8");
            endKey = URLEncoder.encode(endKey, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

            return null;
        }

        String url = sUrl + "_design/app/_view/find_group_by_name?startkey=" + name + "&endkey="
                + endKey;

        JSONObject json = ConnectionHandler.getJsonObject(url, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.parseMultiGroupObjects(json);
    }
    
    public static void findGroupsByName (String name, ResultListener<List<Group>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<Group>>(new FindGroupsByName(name), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindGroupsByName implements Command<List<Group>>
    {
    	String name;

		public FindGroupsByName(String name)
    	{
    		this.name = name;
    	}

		@Override
		public List<Group> execute() throws JSONException, IOException {
			String endKey = "\"" + name + "\u9999\"";
	        name = "\"" + name + "\"";

	        try {
	            name = URLEncoder.encode(name, "UTF-8");
	            endKey = URLEncoder.encode(endKey, "UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            e.printStackTrace();

	            return null;
	        }

	        String url = sUrl + "_design/app/_view/find_group_by_name?startkey=" + name + "&endkey=" + endKey;

	        JSONObject json = ConnectionHandler.getJsonObject(url, UsersManagement.getLoginUser().getId());

	        return CouchDBHelper.parseMultiGroupObjects(json);
		}
    }

    /**
     * Find users favorite groups
     * 
     * @param id
     * @return
     */
    @Deprecated
    public static List<Group> findUserFavoriteGroups(String id) {

        id = "\"" + id + "\"";

        try {
            id = URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_favorite_groups?key=" + id + "&include_docs=true",
                UsersManagement.getLoginUser().getId());

        return CouchDBHelper.parseFavoriteGroups(json);
    }
    
    public static void findUserFavoriteGroups (String id, ResultListener<List<Group>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<Group>>(new FindUserFavoriteGroups(id), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindUserFavoriteGroups implements Command<List<Group>>
    {
    	String id;
    	
    	public FindUserFavoriteGroups (String id)
    	{
    		this.id = id;
    	}

		@Override
		public List<Group> execute() throws JSONException, IOException {
			id = "\"" + id + "\"";

	        try {
	            id = URLEncoder.encode(id, "UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            e.printStackTrace();

	            return null;
	        }

	        JSONObject json = ConnectionHandler.getJsonObject(sUrl
	                + "_design/app/_view/find_favorite_groups?key=" + id + "&include_docs=true",
	                UsersManagement.getLoginUser().getId());

	        return CouchDBHelper.parseFavoriteGroups(json);
		}
    }

    /**
     * Find activity summary
     * 
     * @param id
     * @return
     */
    @Deprecated
    public static ActivitySummary findUserActivitySummary(String id) {

        id = "\"" + id + "\"";

        try {
            id = URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/user_activity_summary?key=" + id, UsersManagement
                .getLoginUser().getId());

        // Logger.error("ActivitySummaryJSON", json.toString());

        return CouchDBHelper.parseSingleActivitySummaryObject(json);
    }
    
    public static void findUserActivitySummary(String id, ResultListener<ActivitySummary> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, ActivitySummary>(new CouchDB.FindUserActivitySummary(id), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindUserActivitySummary implements Command<ActivitySummary>
    {
    	String id;
    	
    	public FindUserActivitySummary (String id)
    	{
    		this.id = id;
    	}

		@Override
		public ActivitySummary execute() throws JSONException, IOException {
			id = "\"" + id + "\"";

	        try {
	            id = URLEncoder.encode(id, "UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            e.printStackTrace();
	            return null;
	        }

	        JSONObject json = ConnectionHandler.getJsonObject(sUrl
	                + "_design/app/_view/user_activity_summary?key=" + id, UsersManagement
	                .getLoginUser().getId());

	        // Logger.error("ActivitySummaryJSON", json.toString());

	        return CouchDBHelper.parseSingleActivitySummaryObject(json);
		}
    }

    /**
     * Add favorite user groups to current logged in user
     * 
     * @param groupId
     * @return
     */
    @Deprecated
    public static boolean addFavoriteGroup(String groupId) {

        User user = CouchDB.findUserById(UsersManagement.getLoginUser().getId());
        user.getGroupIds().add(groupId);

        UserGroup userGroup = new UserGroup();
        userGroup.setGroupId(groupId);
        userGroup.setType(Const.USER_GROUP);
        userGroup.setUserId(user.getId());
        userGroup.set_userName(user.getName());
        CouchDB.createUserGroup(userGroup);

        return CouchDB.updateUser(user);
    }
    
    public static void addFavoriteGroup(final String groupId, final ResultListener<Boolean> resultListener, final Context context, final boolean showProgressBar) {
    	CouchDB.findUserById(UsersManagement.getLoginUser().getId(), new ResultListener<User>() {
			
			@Override
			public void onResultsSucceded(User result) {
				final User user = result;
				user.getGroupIds().add(groupId);
				
				UserGroup userGroup = new UserGroup();
		        userGroup.setGroupId(groupId);
		        userGroup.setType(Const.USER_GROUP);
		        userGroup.setUserId(user.getId());
		        userGroup.set_userName(user.getName());
		        
		        CouchDB.createUserGroup(userGroup, new ResultListener<String>() {
					
					@Override
					public void onResultsSucceded(String result) {
						CouchDB.updateUser(user, resultListener, context, showProgressBar);
					}
					
					@Override
					public void onResultsFail() {
					}
				}, context, showProgressBar);
			}
			
			@Override
			public void onResultsFail() {
			}
		}, context, showProgressBar);
    }

    /**
     * Remove a group from favorite user groups of current logged in user
     * 
     * @param groupId
     * @return
     */
    @Deprecated
    public static boolean removeFavoriteGroup(String groupId) {

        User user = CouchDB.findUserById(UsersManagement.getLoginUser().getId());

        List<UserGroup> usersGroup = new ArrayList<UserGroup>(findUserGroupByIds(groupId,
                user.getId()));
        if (usersGroup != null) {
            for (UserGroup userGroup : usersGroup) {
                CouchDB.deleteUserGroup(userGroup.getId(), userGroup.getRev());
            }
        }

        List<String> currentGroupIds = UsersManagement.getLoginUser().getGroupIds();

        if (!currentGroupIds.isEmpty()) {

            List<String> newGroupsIds = new ArrayList<String>();

            for (String id : currentGroupIds) {
                if (!id.equals(groupId)) {
                    newGroupsIds.add(id);
                }
            }

            user.setGroupIds(newGroupsIds);

            return CouchDB.updateUser(user);
        }

        return false;
    }
    
    
    public static void removeFavoriteGroup (final String groupId, final ResultListener<Boolean> resultListener, final Context context, final boolean showProgressBar) {
    	CouchDB.findUserById(UsersManagement.getLoginUser().getId(), new ResultListener<User>() {
    		
			@Override
			public void onResultsSucceded(User result) {
				final User user = result;
				findUserGroupByIds(groupId, user.getId(), new ResultListener<List<UserGroup>>() {

					@Override
					public void onResultsSucceded(List<UserGroup> result) {
						
						final List<UserGroup> usersGroup = result;
						deleteUsersGroup(usersGroup, new ResultListener<Boolean>() {
							@Override
							public void onResultsSucceded(Boolean result) {
								List<String> currentGroupIds = UsersManagement.getLoginUser().getGroupIds();

						        if (!currentGroupIds.isEmpty()) {

						            List<String> newGroupsIds = new ArrayList<String>();

						            for (String id : currentGroupIds) {
						                if (!id.equals(groupId)) {
						                    newGroupsIds.add(id);
						                }
						            }

						            user.setGroupIds(newGroupsIds);
						            
						            CouchDB.updateUser(user, resultListener, context, showProgressBar);
						        }
							}

							@Override
							public void onResultsFail() {
							}
						}, context, showProgressBar);
					}

					@Override
					public void onResultsFail() {
					}
				}, context, showProgressBar);
				
			}
			
			@Override
			public void onResultsFail() {
			}
		}, context, showProgressBar);
    }

    /**
     * Create new user group
     * 
     * @param userGroup
     * @return
     */
    @Deprecated
    private static String createUserGroup(UserGroup userGroup) {

        JSONObject userGroupJson = new JSONObject();

        try {
            userGroupJson.put(Const.GROUP_ID, userGroup.getGroupId());
            userGroupJson.put(Const.TYPE, Const.USER_GROUP);
            userGroupJson.put(Const.USER_ID, userGroup.getUserId());
            userGroupJson.put(Const.USER_NAME, userGroup.getUserName());
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return CouchDBHelper.createUserGroup(ConnectionHandler.deprecatedPostJsonObject(userGroupJson,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
    }
    
    public static void createUserGroup (UserGroup userGroup, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new CreateUserGroup(userGroup), resultListener, context, showProgressBar).execute();
    }

    public static class CreateUserGroup implements Command<String>
    {
    	UserGroup userGroup;
    	
    	public CreateUserGroup (UserGroup userGroup)
    	{
    		this.userGroup = userGroup;
    	}

		@Override
		public String execute() throws JSONException, IOException {
			JSONObject userGroupJson = new JSONObject();
	       
			userGroupJson.put(Const.GROUP_ID, userGroup.getGroupId());
			userGroupJson.put(Const.TYPE, Const.USER_GROUP);
            userGroupJson.put(Const.USER_ID, userGroup.getUserId());
            userGroupJson.put(Const.USER_NAME, userGroup.getUserName());

	        return CouchDBHelper.createUserGroup(ConnectionHandler.postJsonObject(userGroupJson, UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
		}
    }
    
    @Deprecated
    private static boolean deleteUserGroup(String id, String rev) {

        return CouchDBHelper.deleteUserGroup(ConnectionHandler.deleteJsonObject(id, rev,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
    }
    
    public static void deleteUserGroup (String id, String rev, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new DeleteUserGroup(id, rev), resultListener, context, showProgressBar).execute();
    } 
    
    public static class DeleteUserGroup implements Command<Boolean>
    {
    	String id;
    	String rev;
    	
    	public DeleteUserGroup(String id, String rev)
    	{
    		this.id = id;
    		this.rev = rev;
    	}
    	
		@Override
		public Boolean execute() throws JSONException, IOException {
			return CouchDBHelper.deleteUserGroup(ConnectionHandler.deleteJsonObject(id, rev, UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
		}
    }
    
    public static void deleteUsersGroup (List<UserGroup> usersGroup, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar)
    {
    	new SpikaAsyncTask<Void, Void, Boolean>(new DeleteUsersGroup(usersGroup), resultListener, context, showProgressBar).execute();
    }
    
    public static class DeleteUsersGroup implements Command<Boolean>
    {
    	List<UserGroup> usersGroup;
    	
    	public DeleteUsersGroup (List<UserGroup> usersGroup)
    	{
    		this.usersGroup = usersGroup;
    	}

		@Override
		public Boolean execute() throws JSONException, IOException {
			if (usersGroup != null) {
	            for (UserGroup userGroup : usersGroup) {
	                CouchDBHelper.deleteUserGroup(ConnectionHandler.deleteJsonObject(userGroup.getId(), userGroup.getRev(), UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
	            }
	        }
			return true;
		}
    }

    /**
     * Find user group by group id and user id
     * 
     * @param groupId
     * @param userId
     * @return
     */
    @Deprecated
    private static List<UserGroup> findUserGroupByIds(String groupId, String userId) {

        String key = "[\"" + groupId + "\",\"" + userId + "\"]";

        try {
            key = URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_users_group?key=" + key, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.parseMultiUserGroupObjects(json);
    }
    
    public static void findUserGroupByIds(String groupId, String userId, ResultListener<List<UserGroup>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<UserGroup>>(new FindUserGroupByIds(groupId, userId), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindUserGroupByIds implements Command<List<UserGroup>>
    {
    	String groupId;
    	String userId;
    	
    	public FindUserGroupByIds (String groupId, String userId)
    	{
    		this.groupId = groupId;
    		this.userId = userId;
    	}

		@Override
		public List<UserGroup> execute() throws JSONException, IOException {
			String key = "[\"" + groupId + "\",\"" + userId + "\"]";

	        try {
	            key = URLEncoder.encode(key, "UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            e.printStackTrace();

	            return null;
	        }

	        JSONObject json = ConnectionHandler.getJsonObject(sUrl
	                + "_design/app/_view/find_users_group?key=" + key, UsersManagement.getLoginUser()
	                .getId());

	        return CouchDBHelper.parseMultiUserGroupObjects(json);
		}
    }

    /**
     * Find users group by group id
     * 
     * @param groupId
     * @return
     */
    public static List<UserGroup> findUserGroupsByGroupId(String groupId) {

        String key = "\"" + groupId + "\"";

        try {
            key = URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_users_by_groupid?key=" + key, UsersManagement
                .getLoginUser().getId());

        return CouchDBHelper.parseMultiUserGroupObjects(json);
    }

    public static String createGroup(Group group) {

        JSONObject groupJson = new JSONObject();

        try {
            groupJson.put(Const.NAME, group.getName());
            groupJson.put(Const.GROUP_PASSWORD, group.getPassword());
            groupJson.put(Const.TYPE, Const.GROUP);
            groupJson.put(Const.USER_ID, UsersManagement.getLoginUser().getId());
            groupJson.put(Const.DESCRIPTION, group.getDescription());
            groupJson.put(Const.AVATAR_FILE_ID, group.getAvatarFileId());
            groupJson.put(Const.AVATAR_THUMB_FILE_ID, group.getAvatarThumbFileId());
            groupJson.put(Const.CATEGORY_ID, group.getCategoryId());
            groupJson.put(Const.CATEGORY_NAME, group.getCategoryName());
            groupJson.put(Const.DELETED, false);

            // JSONObject imageJPG = new JSONObject();
            // if (bitmapImage != null) {
            // /* Set a new avatar */
            //
            // JSONObject imageData = new JSONObject();
            //
            // try {
            // ByteArrayOutputStream stream = new ByteArrayOutputStream();
            // bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100,
            // stream);
            // byte[] byteArray = stream.toByteArray();
            // StringBuilder encoded = new StringBuilder(
            // Base64.encodeToString(byteArray, Base64.NO_WRAP));
            // imageData.put(Const.DATA, encoded);
            // imageData.put(Const.CONTENT_TYPE, "image/jpeg");
            // imageJPG.put(getNewAvatarName(null, Const.GROUP_AVATAR),
            // imageData);
            // } catch (JSONException e) {
            // e.printStackTrace();
            // }
            // }
            // groupJson.put(Const.AVATAR_NAME,
            // getNewAvatarName(null, Const.GROUP_AVATAR));
            // groupJson.put(Const.ATTACHMENTS, imageJPG);

        } catch (JSONException e) {
            e.printStackTrace();

            return null;
        }

        return CouchDBHelper.createGroup(ConnectionHandler.deprecatedPostJsonObject(groupJson,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
    }

    /**
     * Update a group you own
     * 
     * @param group
     * @return
     */
    public static boolean updateGroup(Group group) {

        JSONObject groupJson = new JSONObject();

        try {
            groupJson.put(Const.NAME, group.getName());
            groupJson.put(Const.GROUP_PASSWORD, group.getPassword());
            groupJson.put(Const.TYPE, Const.GROUP);
            groupJson.put(Const.USER_ID, UsersManagement.getLoginUser().getId());
            groupJson.put(Const.DESCRIPTION, group.getDescription());
            groupJson.put(Const.AVATAR_FILE_ID, group.getAvatarFileId());
            groupJson.put(Const.AVATAR_THUMB_FILE_ID, group.getAvatarThumbFileId());
            groupJson.put(Const._REV, group.getRev());
            groupJson.put(Const._ID, group.getId());
            groupJson.put(Const.CATEGORY_ID, group.getCategoryId());
            groupJson.put(Const.CATEGORY_NAME, group.getCategoryName());
            groupJson.put(Const.DELETED, group.isDeleted());

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        
        if (group.isDeleted()) {
        	List<UserGroup> usersGroup = new ArrayList<UserGroup>(findUserGroupByIds(group.getId(),
                    UsersManagement.getLoginUser().getId()));
            if (usersGroup != null) {
                for (UserGroup userGroup : usersGroup) {
                    CouchDB.deleteUserGroup(userGroup.getId(), userGroup.getRev());
                }
            }
        }

        return CouchDBHelper.updateGroup(ConnectionHandler.putJsonObject(groupJson, group.getId(),
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
    }

    public static boolean deleteGroup(String id, String rev) {

        List<UserGroup> usersGroup = new ArrayList<UserGroup>(findUserGroupsByGroupId(id));
        if (usersGroup != null) {
            for (UserGroup userGroup : usersGroup) {
                CouchDB.deleteUserGroup(userGroup.getId(), userGroup.getRev());
            }
        }

        return CouchDBHelper.deleteGroup(ConnectionHandler.deleteJsonObject(id, rev,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
    }

    /**
     * Get a list of emoticons from database
     * 
     * @return
     */
    public static List<Emoticon> findAllEmoticons() {

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_all_emoticons", UsersManagement.getLoginUser().getId());

        return CouchDBHelper.parseMultiEmoticonObjects(json);
    }

    /**
     * Get a list of group categories from database
     * 
     * @return
     */
    public static List<GroupCategory> findGroupCategories() {

        String cachedJSON = CouchDB.getFromMemCache(groupCategoryCacheKey);
        
        if(cachedJSON == null){
            
            JSONObject json = ConnectionHandler.getJsonObject(sUrl
                    + "_design/app/_view/find_group_categories",
                    UsersManagement.getLoginUser().getId());
            
            CouchDB.saveToMemCache(groupCategoryCacheKey, json.toString());
            
            return CouchDBHelper.parseMultiGroupCategoryObjects(json);
            
        }else{
            
            try {
                
                JSONObject json =  new JSONObject(cachedJSON);
                
                return CouchDBHelper.parseMultiGroupCategoryObjects(json);
                
            } catch (Exception e) {

                CouchDB.saveToMemCache(groupCategoryCacheKey, null);
                
                return findGroupCategories();
            }
            
        }
        

        
    }

    /**
     * Find group by category id
     * 
     * @return
     */
    public static List<Group> findGroupByCategoryId(String id) {

        id = "\"" + id + "\"";

        try {
            id = URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_group_by_category_id?key=" + id, UsersManagement
                .getLoginUser().getId());

        return CouchDBHelper.parseMultiGroupObjects(json);
    }

    /**
     * Get a bitmap object
     * 
     * @param url
     * @return
     */
    public static Bitmap getBitmapObject(String url) {

        return ConnectionHandler.getBitmapObject(url, UsersManagement.getLoginUser().getId(),
                UsersManagement.getLoginUser().getToken());
    }

    /**
     * Find a single message by ID
     * 
     * @param id
     * @return
     */
    public static Message findMessageById(String id) {

        JSONObject json = ConnectionHandler.getJsonObject(sUrl + id, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.findMessage(json);
    }

    /**
     * Find messages sent to user
     * 
     * @param from
     * @param page
     * @return
     */
    public static ArrayList<Message> findMessagesForUser(User from, int page) {

        int skip = page * SettingsManager.sMessageCount;
        int count = 20;

        if (UsersManagement.isTheSameUser()) {
            count = SettingsManager.sMessageCount * 2;
            skip *= 2;
        } else {
            count = SettingsManager.sMessageCount;
        }

        User to_user = UsersManagement.getToUser();
        Group to_group = UsersManagement.getToGroup();

        String luz = "";
        String duz = "";
        String vz = "";

        try {
            luz = URLEncoder.encode("[", "UTF-8");
            duz = URLEncoder.encode("]", "UTF-8");
            vz = URLEncoder.encode("{}", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String urls = "";
        String parameters = "";

        String _from = "\"" + from.getId() + "\"";

        try {
            _from = URLEncoder.encode(_from, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String _to = "";

        if (to_user != null) {
            _to = "\"" + to_user.getId() + "\"";

            try {
                _to = URLEncoder.encode(_to, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            urls = "_design/app/_view/find_user_message?startkey=";
            parameters = luz + _from + "," + _to + "," + vz + duz + "&endkey=" + luz + _from + ","
                    + _to + duz + "&descending=true&limit=" + count + "&skip=" + skip; // &limit=20&descending=true&skip=0
        } else if (to_group != null) {
            _to = "\"" + to_group.getId() + "\"";

            try {
                _to = URLEncoder.encode(_to, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            urls = "_design/app/_view/find_group_message?startkey=";
            parameters = luz + _to + "," + vz + duz + "&endkey=" + luz + _to + duz
                    + "&descending=true&limit=" + count + "&skip=" + skip; // &limit=20&descending=true&skip=0
        }

        String url = sUrl + urls + parameters;
        JSONObject json = ConnectionHandler.getJsonObject(url, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.findMessagesForUser(json);
    }

    /**
     * Send message to user
     * 
     * @param m
     * @param isPut
     * @return
     */
    public static boolean sendMessageToUser(Message m, boolean isPut) {

        boolean isSuccess = true;

        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put(Const.MESSAGE_TYPE, m.getMessageType());
            jsonObj.put(Const.MODIFIED, m.getModified());
            jsonObj.put(Const.TYPE, m.getType());
            jsonObj.put(Const.FROM_USER_NAME, m.getFromUserName());
            jsonObj.put(Const.FROM_USER_ID, m.getFromUserId());
            jsonObj.put(Const.VALID, m.isValid());
            jsonObj.put(Const.MESSAGE_TARGET_TYPE, m.getMessageTargetType());
            jsonObj.put(Const.CREATED, m.getCreated());
            jsonObj.put(Const.TO_USER_NAME, m.getToUserName());
            jsonObj.put(Const.TO_USER_ID, m.getToUserId());
            jsonObj.put(Const.BODY, m.getBody());
            if (!m.getLatitude().equals("")) {
                jsonObj.put(Const.LATITUDE, m.getLatitude());
            }
            if (!m.getLongitude().equals("")) {
                jsonObj.put(Const.LONGITUDE, m.getLongitude());
            }
            if (!m.getAttachments().equals("")) {

                jsonObj.put(Const.ATTACHMENTS, new JSONObject(m.getAttachments()));
            }
            if (!m.getVideoFileId().equals("")) {
                jsonObj.put(Const.VIDEO_FILE_ID, m.getVideoFileId());
            }
            if (!m.getVoiceFileId().equals("")) {
                jsonObj.put(Const.VOICE_FILE_ID, m.getVoiceFileId());
            }
            if (!m.getImageFileId().equals("")) {
                jsonObj.put(Const.PICTURE_FILE_ID, m.getImageFileId());
            }
            if (!m.getImageThumbFileId().equals("")) {
                jsonObj.put(Const.PICTURE_THUMB_FILE_ID, m.getImageFileId());
            }
            if (!m.getEmoticonImageUrl().equals("")) {
                jsonObj.put(Const.EMOTICON_IMAGE_URL, m.getEmoticonImageUrl());
            }

        } catch (JSONException e) {
            e.printStackTrace();
            isSuccess = false;
        }

        // XXX Need to check if json return ok or failed like for delete group

        JSONObject resultOfCouchDB = null;
        if (isPut) {
            resultOfCouchDB = ConnectionHandler.putJsonObject(jsonObj, m.getId(), UsersManagement
                    .getLoginUser().getId(), UsersManagement.getLoginUser().getToken());
        } else {
            resultOfCouchDB = ConnectionHandler.deprecatedPostJsonObject(jsonObj, UsersManagement
                    .getLoginUser().getId(), UsersManagement.getLoginUser().getToken());
        }

        if (resultOfCouchDB == null) {
            isSuccess = false;
        }
        return isSuccess;
    }

    /**
     * Update message for a user
     * 
     * @param m
     * @return
     */
    public static boolean updateMessageForUser(Message m) {
    	
    	Log.d("log", "couchDB: "+m.getImageThumbFileId());

        boolean isSuccess = true;

        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put(Const._ID, m.getId());
            jsonObj.put(Const._REV, m.getRev());
            jsonObj.put(Const.MESSAGE_TYPE, m.getMessageType());
            jsonObj.put(Const.MODIFIED, m.getModified());
            jsonObj.put(Const.TYPE, m.getType());
            jsonObj.put(Const.FROM_USER_NAME, m.getFromUserName());
            jsonObj.put(Const.FROM_USER_ID, m.getFromUserId());
            jsonObj.put(Const.VALID, m.isValid());
            jsonObj.put(Const.MESSAGE_TARGET_TYPE, m.getMessageTargetType());
            jsonObj.put(Const.CREATED, m.getCreated());
            jsonObj.put(Const.TO_USER_NAME, m.getToUserName());
            jsonObj.put(Const.TO_USER_ID, m.getToUserId());
            jsonObj.put(Const.BODY, m.getBody());
            if (!m.getLatitude().equals("")) {
                jsonObj.put(Const.LATITUDE, m.getLatitude());
            }
            if (!m.getLongitude().equals("")) {
                jsonObj.put(Const.LONGITUDE, m.getLongitude());
            }
            if (!m.getAttachments().equals("")) {
                jsonObj.put(Const._ATTACHMENTS, new JSONObject(m.getAttachments()));
            }
            if (!m.getVideoFileId().equals("")) {
                jsonObj.put(Const.VIDEO_FILE_ID, m.getVideoFileId());
            }
            if (!m.getVoiceFileId().equals("")) {
                jsonObj.put(Const.VOICE_FILE_ID, m.getVoiceFileId());
            }
            if (!m.getImageFileId().equals("")) {
                jsonObj.put(Const.PICTURE_FILE_ID, m.getImageFileId());
            }
            if (!m.getImageThumbFileId().equals("")) {
                jsonObj.put(Const.PICTURE_THUMB_FILE_ID, m.getImageThumbFileId());
            }
            if (!m.getEmoticonImageUrl().equals("")) {
                jsonObj.put(Const.EMOTICON_IMAGE_URL, m.getEmoticonImageUrl());
            }
            
        } catch (JSONException e) {
            e.printStackTrace();
            isSuccess = false;
        }

        // XXX Need to check if json return ok or failed like for delete group
        JSONObject resultOfCouchDB = ConnectionHandler.putJsonObject(jsonObj, m.getId(),
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());

        if (resultOfCouchDB == null) {
            isSuccess = false;
        }
        return isSuccess;
    }

    /**
     * Send message to a group
     * 
     * @param m
     * @param isPut
     * @return
     */
    public static boolean sendMessageToGroup(Message m, boolean isPut) {

        boolean isSuccess = true;

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put(Const.MESSAGE_TYPE, m.getMessageType());
            jsonObj.put(Const.MODIFIED, m.getModified());
            jsonObj.put(Const.TYPE, m.getType());
            jsonObj.put(Const.FROM_USER_NAME, m.getFromUserName());
            jsonObj.put(Const.FROM_USER_ID, m.getFromUserId());
            jsonObj.put(Const.VALID, m.isValid());
            jsonObj.put(Const.TO_GROUP_ID, m.getToGroupId());
            jsonObj.put(Const.TO_GROUP_NAME, m.getToGroupName());
            jsonObj.put(Const.MESSAGE_TARGET_TYPE, m.getMessageTargetType());
            jsonObj.put(Const.CREATED, m.getCreated());
            jsonObj.put(Const.BODY, m.getBody());

            if (!m.getLatitude().equals("")) {
                jsonObj.put(Const.LATITUDE, m.getLatitude());
            }
            if (!m.getLongitude().equals("")) {
                jsonObj.put(Const.LONGITUDE, m.getLongitude());
            }
            if (!m.getAttachments().equals("")) {
                jsonObj.put(Const._ATTACHMENTS, new JSONObject(m.getAttachments()));
            }
            if (!m.getVideoFileId().equals("")) {
                jsonObj.put(Const.VIDEO_FILE_ID, m.getVideoFileId());
            }
            if (!m.getVoiceFileId().equals("")) {
                jsonObj.put(Const.VOICE_FILE_ID, m.getVoiceFileId());
            }
            if (!m.getImageFileId().equals("")) {
                jsonObj.put(Const.PICTURE_FILE_ID, m.getImageFileId());
            }
            if (!m.getImageThumbFileId().equals("")) {
                jsonObj.put(Const.PICTURE_THUMB_FILE_ID, m.getImageFileId());
            }
            if (!m.getEmoticonImageUrl().equals("")) {
                jsonObj.put(Const.EMOTICON_IMAGE_URL, m.getEmoticonImageUrl());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            isSuccess = false;
        }

        // XXX Need to check if json return ok or failed like for delete group

        JSONObject resultOfCouchDB = null;
        if (isPut) {
            resultOfCouchDB = ConnectionHandler.putJsonObject(jsonObj, m.getId(), UsersManagement
                    .getLoginUser().getId(), UsersManagement.getLoginUser().getToken());
        } else {
            resultOfCouchDB = ConnectionHandler.deprecatedPostJsonObject(jsonObj, UsersManagement
                    .getLoginUser().getId(), UsersManagement.getLoginUser().getToken());
        }

        if (resultOfCouchDB == null) {
            isSuccess = false;
        }
        return isSuccess;
    }

    /**
     * Update message from group
     * 
     * @param m
     * @return
     */
    public static boolean updateMessageForGroup(Message m) {
        boolean isSuccess = true;

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put(Const._ID, m.getId());
            jsonObj.put(Const._REV, m.getRev());
            jsonObj.put(Const.MESSAGE_TYPE, m.getMessageType());
            jsonObj.put(Const.MODIFIED, m.getModified());
            jsonObj.put(Const.TYPE, m.getType());
            jsonObj.put(Const.FROM_USER_NAME, m.getFromUserName());
            jsonObj.put(Const.FROM_USER_ID, m.getFromUserId());
            jsonObj.put(Const.VALID, m.isValid());
            jsonObj.put(Const.TO_GROUP_ID, m.getToGroupId());
            jsonObj.put(Const.TO_GROUP_NAME, m.getToGroupName());
            jsonObj.put(Const.MESSAGE_TARGET_TYPE, m.getMessageTargetType());
            jsonObj.put(Const.CREATED, m.getCreated());
            jsonObj.put(Const.BODY, m.getBody());

            if (!m.getLatitude().equals("")) {
                jsonObj.put(Const.LATITUDE, m.getLatitude());
            }
            if (!m.getLongitude().equals("")) {
                jsonObj.put(Const.LONGITUDE, m.getLongitude());
            }
            if (!m.getAttachments().equals("")) {
                jsonObj.put(Const._ATTACHMENTS, new JSONObject(m.getAttachments()));
            }
            if (!m.getVideoFileId().equals("")) {
                jsonObj.put(Const.VIDEO_FILE_ID, m.getVideoFileId());
            }
            if (!m.getVoiceFileId().equals("")) {
                jsonObj.put(Const.VOICE_FILE_ID, m.getVoiceFileId());
            }
            if (!m.getImageFileId().equals("")) {
                jsonObj.put(Const.PICTURE_FILE_ID, m.getImageFileId());
            }
            if (!m.getImageThumbFileId().equals("")) {
                jsonObj.put(Const.PICTURE_THUMB_FILE_ID, m.getImageThumbFileId());
            }
            if (!m.getEmoticonImageUrl().equals("")) {
                jsonObj.put(Const.EMOTICON_IMAGE_URL, m.getEmoticonImageUrl());
            }

        } catch (JSONException e) {
            e.printStackTrace();
            isSuccess = false;
        }

        // XXX Need to check if json return ok or failed like for delete group

        JSONObject resultOfCouchDB = ConnectionHandler.putJsonObject(jsonObj, m.getId(),
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());

        if (resultOfCouchDB == null) {
            isSuccess = false;
        }
        return isSuccess;

    }

    /**
     * Create new comment
     * 
     * @return
     */
    public static String createComment(Comment comment) {

        JSONObject commentJson = new JSONObject();

        try {
            commentJson.put(Const.COMMENT, comment.getComment());
            commentJson.put(Const.USER_ID, comment.getUserId());
            commentJson.put(Const.USER_NAME, comment.getUserName());
            commentJson.put(Const.CREATED, comment.getCreated());
            commentJson.put(Const.MESSAGE_ID, comment.getMessageId());
            commentJson.put(Const.TYPE, Const.COMMENT);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return CouchDBHelper.createComment(ConnectionHandler.deprecatedPostJsonObject(commentJson,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
    }

    /**
     * Find comments by message id
     * 
     * @return
     */
    public static List<Comment> findCommentsByMessageId(String messageId) {

        messageId = "\"" + messageId + "\"";

        try {
            messageId = URLEncoder.encode(messageId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/find_comments_by_message_id?key=" + messageId, UsersManagement
                .getLoginUser().getId());

        return CouchDBHelper.parseMultiCommentObjects(json);
    }

    /**
     * Get comment count using reduce function
     * 
     * @return
     */
    public static int getCommentCount(String messageId) {

        messageId = "\"" + messageId + "\"";

        try {
            messageId = URLEncoder.encode(messageId, "UTF-8");
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
            return 0;
        }

        JSONObject json = ConnectionHandler.getJsonObject(sUrl
                + "_design/app/_view/get_comment_count?key=" + messageId, UsersManagement
                .getLoginUser().getId());

        return CouchDBHelper.getCommentCount(json);
    }

    /**
     * Get File from web
     * 
     * @return
     */
    public static void getFile(String url, File file) {
        ConnectionHandler.getFile(url, file, UsersManagement.getLoginUser().getId(),
                UsersManagement.getLoginUser().getToken());
    }

    public static String getAuthUrl() {
        return sAuthUrl;
    }

    public static void setAuthUrl(String authUrl) {
        CouchDB.sAuthUrl = authUrl;
    }

    @Deprecated
    public static void sendPassword(String email) {

        final String URL = Const.PASSWORDREMINDER_URL + "email=" + email;

        ConnectionHandler.getJsonArrayDeprecated(URL, null, null); 
    }
    
    public static void sendPassword(String email, ResultListener<Void> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Void>(new CouchDB.SendPassword(email), resultListener, context, showProgressBar).execute();
    }
    
    public static class SendPassword implements Command<Void>
    {
    	String email;
    	
    	public SendPassword (String email)
    	{
    		this.email = email; 
    	}

		@Override
		public Void execute() throws JSONException, IOException {
			
			final String URL = Const.PASSWORDREMINDER_URL + "email=" + email;

	        ConnectionHandler.getJsonArray(URL, null, null);
			return null;
		}
    }
    
    /**
     * Create watching group log
     * 
     */
    public static String createWatchingGroupLog(WatchingGroupLog watchingGroupLog) {

        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put(Const.TYPE, Const.WATCHING_GROUP_LOG);
            jsonObj.put(Const.USER_ID, watchingGroupLog.getUserId());
            jsonObj.put(Const.GROUP_ID, watchingGroupLog.getGroupId());
            jsonObj.put(Const.CREATED, Utils.getCurrentDateTime());
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return CouchDBHelper.createWatchingGroupLog(ConnectionHandler.deprecatedPostJsonObject(jsonObj,
        		UsersManagement.getLoginUser().getId(), ""));
    }
    
    /**
     * Delete watching group log
     * 
     */
    public static boolean deleteWatchingGroupLog(String id, String rev) {

        return CouchDBHelper.deleteWatchingGroupLog((ConnectionHandler.deleteJsonObject(id, rev,
                UsersManagement.getLoginUser().getId(), "")));
    }

}
