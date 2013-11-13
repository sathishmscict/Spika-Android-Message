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
import org.apache.http.client.ClientProtocolException;
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
//    private static JSONParser sJsonParser = new JSONParser();
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
    
//***** UPLOAD FILE ****************************
    
    /**
     * Upload file
     * 
     * @param filePath
     * @return file ID
     * @throws IOException 
     * @throws ClientProtocolException 
     * @throws JSONException 
     * @throws UnsupportedOperationException 
     */
    
    public static String uploadFile(String filePath) throws SpikaException, ClientProtocolException, IOException, UnsupportedOperationException, JSONException  {

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (filePath != null && !filePath.equals("")) {
            params.add(new BasicNameValuePair(Const.FILE, filePath));
            String fileId = ConnectionHandler.getIdFromFileUploader(Const.FILE_UPLOADER_URL, params);
            return fileId;
        }
        return null;
    }

    public static void uploadFileAsync(String filePath, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new UploadFile(filePath), resultListener, context, showProgressBar).execute();
    }
    
    private static class UploadFile implements Command<String> 
    {
    	String filePath;
    	
    	public UploadFile (String filePath)
    	{
    		this.filePath = filePath;
    	}

		@Override
		public String execute() throws JSONException, IOException, IllegalStateException, SpikaException {
			return uploadFile(filePath);
		}
    }

//***** DOWNLOAD FILE ****************************
    
    /**
     * Download file
     * 
     * @param fileId
     * @param file
     * @return
     * @throws IOException 
     * @throws ClientProtocolException 
     * @throws JSONException 
     * @throws IllegalStateException 
     */
    public static File downloadFile(String fileId, File file) throws SpikaException, ClientProtocolException, IOException, IllegalStateException, JSONException {

        ConnectionHandler.getFile(Const.FILE_DOWNLOADER_URL + Const.FILE + "=" + fileId, file,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());
        return file;
    }
    
    /**
     * @param fileId
     * @param file
     * @param resultListener
     * @param context
     * @param showProgressBar
     */
    public static void downloadFileAsync(String fileId, File file, ResultListener<File> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, File>(new DownloadFile(fileId, file), resultListener, context, showProgressBar).execute();
    }
    
    private static class DownloadFile implements Command<File>
    {
    	String fileId;
    	File file;
    	
    	public DownloadFile(String fileId, File file) {
			this.fileId = fileId;
			this.file = file;
		}

		@Override
		public File execute() throws JSONException, IOException, SpikaException {
			return downloadFile(fileId, file);
		}
    }
 
//***** UNREGISTER PUSH TOKEN ****************************
    
    /**
     * Unregister push token
     * 
     * @param userId
     * @return
     * @throws JSONException 
     * @throws SpikaException 
     * @throws IOException 
     * @throws IllegalStateException 
     * @throws ClientProtocolException 
     */
    public static String unregisterPushToken(String userId) throws ClientProtocolException, IllegalStateException, IOException, SpikaException, JSONException {
    	String result = ConnectionHandler.getString(Const.UNREGISTER_PUSH_URL + Const.USER_ID + "=" + userId,
                UsersManagement.getLoginUser().getId());
        return result;
    }
    
    public static void unregisterPushTokenAsync (String userId, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new UnregisterPushToken(userId), resultListener, context, showProgressBar).execute();
    }
    
    private static class UnregisterPushToken implements Command<String> {
    	
    	String userId;
    	
    	public UnregisterPushToken(String userId) {
			this.userId = userId;
		}

		@Override
		public String execute() throws JSONException, IOException, IllegalStateException, SpikaException {
			return unregisterPushToken(userId);
		}
    }
    
//***** AUTH ****************************  
    
    /**
     * @param email
     * @param password
     * @return
     * @throws IOException
     * @throws JSONException
     * @throws IllegalStateException
     * @throws SpikaException
     */
    public static String auth(String email, String password) throws IOException, JSONException, IllegalStateException, SpikaException {

        JSONObject jPost = new JSONObject();

        jPost.put("email", email);
        jPost.put("password", password);

        JSONObject json = ConnectionHandler.postAuth(jPost);

        User user = null;

        user = CouchDBHelper.parseSingleUserObjectWithoutRowParam(json);
            
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
    
    public static void authAsync(String email, String password, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new CouchDB.Auth(email, password), resultListener, context, showProgressBar).execute();
    }
    
    private static class Auth implements Command<String>
    {
    	String email;
    	String password;
    	
    	public Auth (String email, String password)
    	{
    		this.email = email;
    		this.password = password;
    	}

		@Override
		public String execute() throws JSONException, IOException, IllegalStateException, SpikaException {
			return auth(email, password);
		}
    }
    
//***** CREATE USER ****************************   
 
    /**
     * @param name
     * @param email
     * @param password
     * @return
     * @throws JSONException
     * @throws ClientProtocolException
     * @throws IOException
     * @throws IllegalStateException
     * @throws SpikaException
     */
    public static String createUser(String name, String email, String password) throws JSONException, ClientProtocolException, IOException, IllegalStateException, SpikaException {

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
    
    public static void createUserAsync(String name, String email, String password, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new CouchDB.CreateUser(name, email, password), resultListener, context, showProgressBar).execute();
    }
    
    private static class CreateUser implements Command<String>
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
		public String execute() throws JSONException, IOException, IllegalStateException, SpikaException {
			return createUser(name, email, password);
		}
    }

//***** FIND USER BY NAME ****************************   
    
    /**
     * @param username
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws JSONException
     * @throws SpikaException
     */
    private static User findUserByName(String username) throws ClientProtocolException, IOException, JSONException, SpikaException {
    	
    	try {
    		username = URLEncoder.encode(username, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

        final String url = Const.FIND_USER_BY_NAME + username;

        JSONObject jsonObject = ConnectionHandler.getJsonObject(url, null);
        
        User user = CouchDBHelper.parseSingleUserObjectWithoutRowParam(jsonObject);
        
        return user;
    }
    
    public static void findUserByNameAsync(String username, ResultListener<User> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, User>(new CouchDB.FindUserByName(username), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindUserByName implements Command<User>
    {
    	String username;
    	
    	public FindUserByName (String username)
    	{
    		this.username = username;
    	}
    	
		@Override
		public User execute() throws JSONException, IOException, SpikaException {
			return findUserByName(username);
		}
    }

//***** FIND USER BY MAIL ****************************   
    
    /**
     * Find user by email
     * 
     * @param email
     * @return
     * @throws SpikaException 
     * @throws JSONException 
     * @throws IOException 
     * @throws ClientProtocolException 
     */
	public static User findUserByEmail(String email) throws ClientProtocolException, IOException, JSONException, SpikaException {
        
        try {
            email = URLEncoder.encode(email, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        JSONObject json = null;

        final String url = Const.FIND_USER_BY_EMAIL + email;
        User user = null;
        
        if (UsersManagement.getLoginUser() != null) {
        	json = ConnectionHandler.getJsonObject(url, UsersManagement.getLoginUser().getId());
        	user = CouchDBHelper.parseSingleUserObjectWithoutRowParam(json);
        } else { 
            json = ConnectionHandler.getJsonObject(url, "");
            user = CouchDBHelper.parseSingleUserObjectWithoutRowParam(json);
        }
        
        return user;
    }
    
    public static void findUserByEmailAsync(String email, ResultListener<User> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, User>(new CouchDB.FindUserByEmail(email), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindUserByEmail implements Command<User>
    {
    	String email;
    	
    	public FindUserByEmail(String email)
    	{
    		this.email = email;
    	}

		@Override
		public User execute() throws JSONException, IOException, SpikaException {
			return findUserByEmail(email);
		}
    }
    
//***** FIND USER BY ID ***********************************
    
    /**
     * Find user by id
     * 
     * @param id
     * @return
     * @throws JSONException 
     * @throws SpikaException 
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    public static User findUserById(String id) throws JSONException, ClientProtocolException, IOException, SpikaException {
    	
        try {
            id = URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }

        String url = Const.FIND_USER_BY_ID + id;
        JSONObject json = ConnectionHandler.getJsonObject(url, UsersManagement.getLoginUser().getId());
        User user = CouchDBHelper.parseSingleUserObjectWithoutRowParam(json);
        
        return user;
    }
    
    public static void findUserByIdAsync(String id, ResultListener<User> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, User>(new CouchDB.FindUserById(id), resultListener, context, showProgressBar).execute();
    }

    private static class FindUserById implements Command<User>
    {
    	String id;
    	
    	public FindUserById (String id)
    	{
    		this.id = id;
    	}
    	
		@Override
		public User execute() throws JSONException, IOException, SpikaException {
			return findUserById(id);
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
    
    private static class GetGroupByName implements Command<Group>
    {
    	String groupname;
    	
    	public GetGroupByName (String groupname)
    	{
    		this.groupname = groupname;
    	}
    	
		@Override
		public Group execute() throws JSONException, IOException, SpikaException {
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

//******* UPDATE USER ***************************
    
    /**
     * Method used for updating user attributes
     * 
     * If you add some new attributes to user object you must also add code to
     * add that data to userJson
     * 
     * @param user
     * @return user object
     * @throws SpikaException 
     * @throws JSONException 
     * @throws IOException 
     * @throws IllegalStateException 
     */
    @Deprecated
    public static boolean updateUser(User user) {

        JSONObject userJson = new JSONObject();
        List<String> contactIds = new ArrayList<String>();
        List<String> groupIds = new ArrayList<String>();

        JSONObject json = null;
        
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

//        JSONObject json = ConnectionHandler.putJsonObject(userJson, user.getId(), user.getId(),
//                user.getToken());
        
        json = ConnectionHandler.postJsonObject(Const.UPDATE_USER, userJson, user.getId(), user.getToken());

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SpikaException e) {
			e.printStackTrace();
		}
        
        return CouchDBHelper.updateUser(json, contactIds, groupIds);
    }
    
    public static void updateUser (User user, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new UpdateUser(user), resultListener, context, showProgressBar).execute();;
    }
    
    private static class UpdateUser implements Command<Boolean>
    {
    	User user;
    	
    	public UpdateUser (User user)
    	{
    		this.user = user;
    	}

		@Override
		public Boolean execute() throws JSONException, IOException {
			return updateUser(user);
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
    
    private static class SearchUsers implements Command<List<User>>
    {
    	UserSearch userSearch;
    	
    	public SearchUsers (UserSearch userSearch)
    	{
    		this.userSearch = userSearch;
    	}

		@Override
		public List<User> execute() throws JSONException, IOException, SpikaException {
			return searchUsers(userSearch);
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
            searchParams = groupSearch.getName();
        }

        JSONArray json = ConnectionHandler.getJsonArrayDeprecated(Const.SEARCH_GROUPS_URL + searchParams,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());

        return CouchDBHelper.parseSearchGroupsResult(json);
    }
    
    public static void searchGroups (GroupSearch groupSearch, ResultListener<List<Group>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<Group>>(new SearchGroups(groupSearch), resultListener, context, showProgressBar).execute();
    }
    
    private static class SearchGroups implements Command<List<Group>>
    {
    	GroupSearch groupSearch;
    	
    	public SearchGroups (GroupSearch groupSearch)
    	{
    		this.groupSearch = groupSearch;
    	}

		@Override
		public List<Group> execute() throws JSONException, IOException, SpikaException {
			String searchParams = "";

	        if (groupSearch.getName() != null) {
	            try {
	                groupSearch.setName(URLEncoder.encode(groupSearch.getName(), "UTF-8"));
	            } catch (UnsupportedEncodingException e) {
	                e.printStackTrace();
	                return null;
	            }
	            searchParams = groupSearch.getName();
	        }

	        JSONArray json = ConnectionHandler.getJsonArray(Const.SEARCH_GROUPS_URL + searchParams, UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());

	        return CouchDBHelper.parseSearchGroupsResult(json);
		}
    }

//**************** FIND AVATAR FILE ID **************************
    
    @Deprecated
    public static String findAvatarFileId(String userId) {

        try {
            userId = URLEncoder.encode(userId, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }

        JSONObject json = null;
		try {
			json = ConnectionHandler.getJsonObject(Const.GET_AVATAR_FILE_ID + userId, UsersManagement.getLoginUser().getId());
			return CouchDBHelper.findAvatarFileId(json);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (SpikaException e) {
			e.printStackTrace();
		}
		return null;    
    }
    
    public static void findAvatarByIdAsync (String userId, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new FindAvatarFileId(userId), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindAvatarFileId implements Command<String>
    {
    	String userId;
    	
    	public FindAvatarFileId (String userId)
    	{
    		this.userId = userId; 
    	}

		@Override
		public String execute() throws JSONException, IOException {
			return findAvatarFileId(userId);
		}
    }
    
    /**
     * @param id
     * @return
     * @throws SpikaException 
     */
    public static List<User> findUserContacts(String id) throws JSONException, IOException, SpikaException {
    	
    	List<User> contacts = new ArrayList<User>();
    	
    	User user = findUserById(id);
        List<String> contactIds = user.getContactIds();
        for (String contactId : contactIds) {
			contacts.add(findUserById(contactId));
		}
        
        return contacts;
    }

    public static void findUserContactsAsync (String id, ResultListener<List<User>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<User>>(new FindUserContacts(id), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindUserContacts implements Command<List<User>>
    {
    	String id;
    	
    	public FindUserContacts (String id)
    	{
    		this.id = id;
    	}

		@Override
		public List<User> execute() throws JSONException, IOException, SpikaException {
	        return findUserContacts(id);
		}
    }
    
    /**
     * Add favorite user contact to current logged in user
     * 
     * @param userId
     * @return
     * @throws SpikaException 
     * @throws IOException 
     * @throws JSONException 
     * @throws ClientProtocolException 
     */
    public static boolean addUserContact(String userId) throws ClientProtocolException, JSONException, IOException, SpikaException {

        User user = UsersManagement.getLoginUser();
        User userUpdated = CouchDB.findUserById(user.getId());

        userUpdated.getContactIds().add(userId);

        return CouchDB.updateUser(userUpdated);
    }
    
    public static void addUserContactAsync (final String userId, final ResultListener<Boolean> resultListener, final Context context, final boolean showProgressBar)
    {
    	 new SpikaAsyncTask<Void, Void, Boolean>(new AddUserContact(userId), resultListener, context, showProgressBar).execute();
    }
    
    private static class AddUserContact implements Command<Boolean> 
    {
    	
    	String userId;
    	
    	public AddUserContact(String userId) {
			this.userId = userId;
		}

		@Override
		public Boolean execute() throws JSONException, IOException, SpikaException {
			return addUserContact(userId);
		}
    }

    /**
     * Remove a user from favorite user contacts of current logged in user
     * 
     * @param userId
     * @return
     * @throws SpikaException 
     * @throws IOException 
     * @throws JSONException 
     * @throws ClientProtocolException 
     */
    public static boolean removeUserContact(String userId) throws ClientProtocolException, JSONException, IOException, SpikaException {

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
    
    public static void removeUserContactAsync (String userId, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new RemoveUserContact(userId), resultListener, context, showProgressBar).execute();
    }
    
    private static class RemoveUserContact implements Command<Boolean>
    {
    	String userId;
    	
    	public RemoveUserContact(String userId) {
			this.userId = userId;
		}

		@Override
		public Boolean execute() throws JSONException, IOException, SpikaException {
			return removeUserContact(userId);
		}
    }
    
    /**
     * Find all groups
     * 
     * @return
     */
    @Deprecated
    public static List<Group> findAllGroups() {

        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
                + "_design/app/_view/find_group_by_name", UsersManagement.getLoginUser().getId());

        return CouchDBHelper.parseMultiGroupObjects(json);
    }
    
    public static void findAllGroups (ResultListener<List<Group>> resultListener, Context context, boolean showProgressBar)
    {
    	new SpikaAsyncTask<Void, Void, List<Group>>(new FindAllGroups(), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindAllGroups implements Command<List<Group>>
    {
		@Override
		public List<Group> execute() throws JSONException, IOException {
			JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
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

        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
                + "_design/app/_view/find_group_by_id?key=" + id, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.parseSingleGroupObject(json);
    }
    
    public static void findGroupById(String id, ResultListener<Group> resultListener, Context context, boolean showProgressBar)
    {
    	new SpikaAsyncTask<Void, Void, Group>(new FindGroupById(id), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindGroupById implements Command<Group>
    {
    	String id;
    	
    	public FindGroupById (String id)
    	{
    		this.id = id;
    	}

		@Override
		public Group execute() throws JSONException, IOException {
			return findGroupById(id);
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

        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(url, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.parseMultiGroupObjects(json);
    }
    
    public static void findGroupsByName (String name, ResultListener<List<Group>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<Group>>(new FindGroupsByName(name), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindGroupsByName implements Command<List<Group>>
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

	        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(url, UsersManagement.getLoginUser().getId());

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

        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
                + "_design/app/_view/find_favorite_groups?key=" + id + "&include_docs=true",
                UsersManagement.getLoginUser().getId());

        return CouchDBHelper.parseFavoriteGroups(json);
    }
    
    public static void findUserFavoriteGroups (String id, ResultListener<List<Group>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<Group>>(new FindUserFavoriteGroups(id), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindUserFavoriteGroups implements Command<List<Group>>
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

	        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
	                + "_design/app/_view/find_favorite_groups?key=" + id + "&include_docs=true",
	                UsersManagement.getLoginUser().getId());

	        return CouchDBHelper.parseFavoriteGroups(json);
		}
    }

//***** FIND USER ACTIVITY SUMMARY ***************************
    
    /**
     * Find activity summary
     * 
     * @param id
     * @return
     * @throws SpikaException 
     * @throws JSONException 
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    private static ActivitySummary findUserActivitySummary(String id) throws ClientProtocolException, IOException, JSONException, SpikaException {
        String url = Const.FIND_USERACTIVITY_SUMMARY;
        JSONObject json = ConnectionHandler.getJsonObject(url, id);
        return CouchDBHelper.parseSingleActivitySummaryObject(json);
    }
    
    public static void findUserActivitySummary(String id, ResultListener<ActivitySummary> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, ActivitySummary>(new CouchDB.FindUserActivitySummary(id), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindUserActivitySummary implements Command<ActivitySummary>
    {
    	String id;
    	
    	public FindUserActivitySummary (String id)
    	{
    		this.id = id;
    	}

		@Override
		public ActivitySummary execute() throws JSONException, IOException, SpikaException {
			return findUserActivitySummary(id);
		}
    }

    /**
     * Add favorite user groups to current logged in user
     * 
     * @param groupId
     * @return
     * @throws SpikaException 
     * @throws IOException 
     * @throws JSONException 
     * @throws ClientProtocolException 
     */
    public static boolean addFavoriteGroup(String groupId) throws ClientProtocolException, JSONException, IOException, SpikaException {

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
    
    public static void addFavoriteGroupAsync (final String groupId, final ResultListener<Boolean> resultListener, final Context context, final boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new AddFavoriteGroup(groupId), resultListener, context, showProgressBar).execute();
    }
    
    private static class AddFavoriteGroup implements Command<Boolean> {
    	
    	String groupId;
    	
    	public AddFavoriteGroup(String groupId) {
			this.groupId = groupId;
		}

		@Override
		public Boolean execute() throws JSONException, IOException, SpikaException {
			return addFavoriteGroup(groupId);
		}
    }

    /**
     * Remove a group from favorite user groups of current logged in user
     * 
     * @param groupId
     * @return
     * @throws SpikaException 
     * @throws IOException 
     * @throws JSONException 
     * @throws ClientProtocolException 
     */
    public static boolean removeFavoriteGroup(String groupId) throws ClientProtocolException, JSONException, IOException, SpikaException {

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
    
    public static void removeFavoriteGroupAsync (String groupId, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new RemoveFavoriteGroup(groupId), resultListener, context, showProgressBar).execute();
    }
    
    private static class RemoveFavoriteGroup implements Command<Boolean> {
    	
    	String groupId;
    	
    	public RemoveFavoriteGroup(String groupId) {
			this.groupId = groupId;
		}

		@Override
		public Boolean execute() throws JSONException, IOException, SpikaException {
			return removeFavoriteGroup(groupId);
		}
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

    private static class CreateUserGroup implements Command<String>
    {
    	UserGroup userGroup;
    	
    	public CreateUserGroup (UserGroup userGroup)
    	{
    		this.userGroup = userGroup;
    	}

		@Override
		public String execute() throws JSONException, IOException, IllegalStateException, SpikaException {
			JSONObject userGroupJson = new JSONObject();
	       
			userGroupJson.put(Const.GROUP_ID, userGroup.getGroupId());
			userGroupJson.put(Const.TYPE, Const.USER_GROUP);
            userGroupJson.put(Const.USER_ID, userGroup.getUserId());
            userGroupJson.put(Const.USER_NAME, userGroup.getUserName());

	        return CouchDBHelper.createUserGroup(ConnectionHandler.postJsonObject(userGroupJson, UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
		}
    }
    
    private static boolean deleteUserGroup(String id, String rev) {

        return CouchDBHelper.deleteUserGroup(ConnectionHandler.deleteJsonObject(id, rev,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
    }

    /**
     * Find user group by group id and user id
     * 
     * @param groupId
     * @param userId
     * @return
     */
    private static List<UserGroup> findUserGroupByIds(String groupId, String userId) {

        String key = "[\"" + groupId + "\",\"" + userId + "\"]";

        try {
            key = URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
                + "_design/app/_view/find_users_group?key=" + key, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.parseMultiUserGroupObjects(json);
    }

    /**
     * Find users group by group id
     * 
     * @param groupId
     * @return
     */
    @Deprecated
    public static List<UserGroup> findUserGroupsByGroupId(String groupId) {

        String key = "\"" + groupId + "\"";

        try {
            key = URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
                + "_design/app/_view/find_users_by_groupid?key=" + key, UsersManagement
                .getLoginUser().getId());

        return CouchDBHelper.parseMultiUserGroupObjects(json);
    }
    
    public static void findUserGroupsByGroupId(String groupId, ResultListener<List<UserGroup>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<UserGroup>>(new FindUserGroupsByGroupId(groupId), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindUserGroupsByGroupId implements Command<List<UserGroup>> {
    	
    	String groupId;
    	
    	public FindUserGroupsByGroupId(String groupId) {
			this.groupId = groupId;
		}

		@Override
		public List<UserGroup> execute() throws JSONException, IOException {
			return findUserGroupsByGroupId(groupId);
		}	
    }

    public static String createGroup(Group group) throws JSONException, IllegalStateException, IOException, SpikaException {

        JSONObject groupJson = new JSONObject();

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

        return CouchDBHelper.createGroup(ConnectionHandler.postJsonObject(groupJson,
                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
    }
    
//    public static void createGroup(Group group, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
//    	new SpikaAsyncTask<Void, Void, String>(new CreateGroup(group), resultListener, context, showProgressBar).execute();
//    }
//    
//    private static class CreateGroup implements Command<String>
//    {
//    	Group group;
//    	
//    	public CreateGroup(Group group)
//    	{
//    		this.group = group;
//    	}
//
//		@Override
//		public String execute() throws JSONException, IOException, IllegalStateException, SpikaException {
//			return createGroup(group);
//		}
//    }

    /**
     * Update a group you own
     * 
     * @param group
     * @return
     */
    @Deprecated
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

    public static void updateGroup(Group group, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new UpdateGroup(group), resultListener, context, showProgressBar).execute();
    }
    
    private static class UpdateGroup implements Command<Boolean> {
    	
    	Group group;
    	
    	public UpdateGroup(Group group) {
			this.group = group;
		}

		@Override
		public Boolean execute() throws JSONException, IOException {
			return updateGroup(group);
		}
    }
    
    @Deprecated
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
    
    public static void deleteGroup(String id, String rev, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new DeleteGroup(id, rev), resultListener, context, showProgressBar).execute();
    }
    
    private static class DeleteGroup implements Command<Boolean> {
    	
    	String id;
    	String rev;
    	
    	public DeleteGroup(String id, String rev) {
			this.id = id;
			this.rev = rev;
		}

		@Override
		public Boolean execute() throws JSONException, IOException {
			return deleteGroup(id, rev);
		}
    }

    /**
     * Get a list of emoticons from database
     * 
     * @return
     */
    @Deprecated
    public static List<Emoticon> findAllEmoticons() {

        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
                + "_design/app/_view/find_all_emoticons", UsersManagement.getLoginUser().getId());

        return CouchDBHelper.parseMultiEmoticonObjects(json);
    }
    
    public static void findAllEmoticons(ResultListener<List<Emoticon>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<Emoticon>>(new FindAllEmoticons(), resultListener, context, showProgressBar).execute();
    }
    
    public static class FindAllEmoticons implements Command<List<Emoticon>>
    {
		@Override
		public List<Emoticon> execute() throws JSONException, IOException {
			return findAllEmoticons();
		}
    }

    /**
     * Get a list of group categories from database
     * 
     * @return
     */
    @Deprecated
    public static List<GroupCategory> findGroupCategories() {

        String cachedJSON = CouchDB.getFromMemCache(groupCategoryCacheKey);
        
        if(cachedJSON == null){
            
            JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
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
    
    public static void findGroupCategories(ResultListener<List<GroupCategory>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<GroupCategory>>(new FindGroupCategories(), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindGroupCategories implements Command<List<GroupCategory>>
    {
		@Override
		public List<GroupCategory> execute() throws JSONException, IOException {
			return findGroupCategories();
		}
    }

    /**
     * Find group by category id
     * 
     * @return
     */
    @Deprecated
    public static List<Group> findGroupByCategoryId(String id) {

        id = "\"" + id + "\"";

        try {
            id = URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
            return null;
        }

        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
                + "_design/app/_view/find_group_by_category_id?key=" + id, UsersManagement
                .getLoginUser().getId());

        return CouchDBHelper.parseMultiGroupObjects(json);
    }
    
    public static void findGroupByCategoryId(String id, ResultListener<List<Group>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<Group>>(new FindGroupByCategoryId(id), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindGroupByCategoryId implements Command<List<Group>> {
    	
    	String id;
    	
    	public FindGroupByCategoryId(String id) {
			this.id = id;
		}

		@Override
		public List<Group> execute() throws JSONException, IOException {
			return findGroupByCategoryId(id);
		}
    }

    /**
     * Get a bitmap object
     * 
     * @param url
     * @return
     */
    @Deprecated
    public static Bitmap getBitmapObject(String url) {

        return ConnectionHandler.getBitmapObject(url, UsersManagement.getLoginUser().getId(),
                UsersManagement.getLoginUser().getToken());
    }
    
    public static void getBitmapObject(String url, ResultListener<Bitmap> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Bitmap>(new GetBitmapObject(url), resultListener, context, showProgressBar).execute();
    }
    
    public static class GetBitmapObject implements Command<Bitmap>{
    	
    	String url;
    	
    	public GetBitmapObject (String url)
    	{
    		this.url = url;
    	}

		@Override
		public Bitmap execute() throws JSONException, IOException {
			return getBitmapObject(url);
		}
    }

    /**
     * Find a single message by ID
     * 
     * @param id
     * @return
     */
    @Deprecated
    public static Message findMessageById(String id) {

        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl + id, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.findMessage(json);
    }

    public static void findMessageById(String id, ResultListener<Message> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Message>(new FindMessageById(id), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindMessageById implements Command<Message> {
    	
    	String id;
    	
    	public FindMessageById (String id) {
    		this.id = id;
    	}

		@Override
		public Message execute() throws JSONException, IOException, SpikaException {
			return findMessageById(id);
		}
    }
    
    /**
     * Find messages sent to user
     * 
     * @param from
     * @param page
     * @return
     */
    
    @Deprecated
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
        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(url, UsersManagement.getLoginUser()
                .getId());

        return CouchDBHelper.findMessagesForUser(json);
    }
    
    public static void findMessagesForUser(User from, int page, ResultListener<ArrayList<Message>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, ArrayList<Message>>(new FindMessagesForUser(from, page), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindMessagesForUser implements Command<ArrayList<Message>> {
    	
    	User from;
    	int page; 
    	
    	public FindMessagesForUser(User from, int page) {
    		this.from = from;
    		this.page = page;
    	}

		@Override
		public ArrayList<Message> execute() throws JSONException, IOException,
				SpikaException {
			return findMessagesForUser(from, page);
		}
    } 

//******* SEND MESSAGE TO USER *************
    
    /**
     * Send message to user
     * 
     * @param m
     * @param isPut
     * @return
     * @throws SpikaException 
     * @throws JSONException 
     * @throws IOException 
     * @throws IllegalStateException 
     * @throws ClientProtocolException 
     */
    public static boolean sendMessageToUser(Message m) throws ClientProtocolException, IllegalStateException, IOException, JSONException, SpikaException {

        boolean isSuccess = true;
        JSONObject resultOfCouchDB = null;
        JSONObject jsonObj = new JSONObject();

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

        resultOfCouchDB = ConnectionHandler.postJsonObject(Const.SEND_MESSAGE_TO_USER, jsonObj, UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());
            
        if (resultOfCouchDB == null) {
            isSuccess = false;
        }
        return isSuccess;
    }

    public static void sendMessageToUser(Message m, boolean isPut, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new SendMessageToUser(m), resultListener, context, showProgressBar).execute();
    }
    
    private static class SendMessageToUser implements Command<Boolean> {
    	
    	Message m;
    	
    	public SendMessageToUser(Message m) {
			this.m = m;
		}

		@Override
		public Boolean execute() throws JSONException, IOException,
				SpikaException {
			return sendMessageToUser(m);
		}
    }
    
    /**
     * Update message for a user
     * 
     * @param m
     * @return
     */
    @Deprecated
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

    public static void updateMessageForUser(Message m, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new UpdateMessageForUser(m), resultListener, context, showProgressBar).execute();
    }
    
    private static class UpdateMessageForUser implements Command<Boolean> {
    	
    	Message m;
    	
    	public UpdateMessageForUser(Message m) {
    		this.m = m;
    	}

		@Override
		public Boolean execute() throws JSONException, IOException,
				SpikaException {
			return updateMessageForUser(m);
		}
    }
   
//************* SEND MESSAGE TO GROUP ********************
    
    /**
     * Send message to a group
     * 
     * @param m
     * @param isPut
     * @return
     * @throws SpikaException 
     * @throws JSONException 
     * @throws IOException 
     * @throws IllegalStateException 
     * @throws ClientProtocolException 
     */
    public static boolean sendMessageToGroup(Message m) throws ClientProtocolException, IllegalStateException, IOException, JSONException, SpikaException {

        boolean isSuccess = true;

        JSONObject resultOfCouchDB = null;
        
        JSONObject jsonObj = new JSONObject();
        
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
        
        resultOfCouchDB = ConnectionHandler.postJsonObject(Const.SEND_MESSAGE_TO_GROUP, jsonObj, UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken());

        if (resultOfCouchDB == null) {
            isSuccess = false;
        }
        return isSuccess;
    }
    
    public static void sendMessageToGroup(Message m, boolean isPut, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new SendMessageToGroup(m), resultListener, context, showProgressBar).execute();
    }
    
    private static class SendMessageToGroup implements Command<Boolean> {
    	
    	Message m;
    	
    	public SendMessageToGroup(Message m) {
    		this.m = m;
    	}

		@Override
		public Boolean execute() throws JSONException, IOException,
				SpikaException {
			return sendMessageToGroup(m);
		}
    }

    /**
     * Update message from group
     * 
     * @param m
     * @return
     */
    @Deprecated
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

    public static void updateMessageForGroup(Message m, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new UpdateMessageForGroup(m), resultListener, context, showProgressBar).execute();
    }
    
    private static class UpdateMessageForGroup implements Command<Boolean>
    {
    	Message m;
    	
    	public UpdateMessageForGroup(Message m)
    	{
    		this.m = m;
    	}

		@Override
		public Boolean execute() throws JSONException, IOException,
				SpikaException {
			return updateMessageForGroup(m);
		}
    }
    
    /**
     * Create new comment
     * 
     * @return
     */
    @Deprecated
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

//        return CouchDBHelper.createComment(ConnectionHandler.deprecatedPostJsonObject(commentJson,
//                UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
        
        try {
			return CouchDBHelper.createComment(ConnectionHandler.postJsonObject(Const.SEND_COMMENT, commentJson,
			        UsersManagement.getLoginUser().getId(), UsersManagement.getLoginUser().getToken()));
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SpikaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return null;
    }
    
    public static void createComment(Comment comment, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new CreateComment(comment), resultListener, context, showProgressBar).execute();
    }
    
    private static class CreateComment implements Command<String>
    {
    	Comment comment;
    	
    	public CreateComment(Comment comment) {
			this.comment = comment;
		}

		@Override
		public String execute() throws JSONException, IOException,
				SpikaException {
			return createComment(comment);
		}
    } 

    /**
     * Find comments by message id
     * 
     * @return
     */
    @Deprecated
    public static List<Comment> findCommentsByMessageId(String messageId) {

//        messageId = "\"" + messageId + "\"";

        try {
            messageId = URLEncoder.encode(messageId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

//        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
//                + "_design/app/_view/find_comments_by_message_id?key=" + messageId, UsersManagement
//                .getLoginUser().getId());
        
        JSONObject json = null;
		try {
			json = ConnectionHandler.getJsonObject(Const.FIND_COMMENTS_BY_MESSAGE_ID + messageId + "/30/0", UsersManagement.getLoginUser().getId());
		} catch (Exception e) {
			e.printStackTrace();
		}

        return CouchDBHelper.parseMultiCommentObjects(json);
    }
    
    public static void findCommentsByMessageId (String messageId, ResultListener<List<Comment>> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, List<Comment>>(new FindCommentsByMessageId(messageId), resultListener, context, showProgressBar).execute();
    }
    
    private static class FindCommentsByMessageId implements Command<List<Comment>> {
    	
    	String messageId;
    	
    	public FindCommentsByMessageId (String messageId)
    	{
    		this.messageId = messageId;
    	}

		@Override
		public List<Comment> execute() throws JSONException, IOException,
				SpikaException {
			return findCommentsByMessageId(messageId);
		}
    }

    /**
     * Get comment count using reduce function
     * 
     * @return
     */
    @Deprecated
    public static int getCommentCount(String messageId) {

        messageId = "\"" + messageId + "\"";

        try {
            messageId = URLEncoder.encode(messageId, "UTF-8");
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
            return 0;
        }

        JSONObject json = ConnectionHandler.getJsonObjectDeprecated(sUrl
                + "_design/app/_view/get_comment_count?key=" + messageId, UsersManagement
                .getLoginUser().getId());

        return CouchDBHelper.getCommentCount(json);
    }
    
    public static void GetCommentCount (String messageId, ResultListener<Integer> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Integer>(new GetCommentCount(messageId), resultListener, context, showProgressBar).execute();
    }
    
    private static class GetCommentCount implements Command<Integer> {
    	
    	String messageId;
    	
    	public GetCommentCount(String messageId) {
			this.messageId = messageId;
		}

		@Override
		public Integer execute() throws JSONException, IOException,
				SpikaException {
			return getCommentCount(messageId);
		}
    }

    public static String getAuthUrl() {
        return sAuthUrl;
    }

    public static void setAuthUrl(String authUrl) {
        CouchDB.sAuthUrl = authUrl;
    }

    private static void sendPassword(String email) throws ClientProtocolException, IllegalStateException, IOException, SpikaException, JSONException {

    	final String URL = Const.PASSWORDREMINDER_URL + "email=" + email;
        ConnectionHandler.getString(URL, null);
    }
    
    public static void sendPassword(String email, ResultListener<Void> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Void>(new CouchDB.SendPassword(email), resultListener, context, showProgressBar).execute();
    }
    
    private static class SendPassword implements Command<Void>
    {
    	String email;
    	
    	public SendPassword (String email)
    	{
    		this.email = email; 
    	}

		@Override
		public Void execute() throws JSONException, IOException, SpikaException {
			
			sendPassword(email);
			return null;
		}
    }
    
    /**
     * Create watching group log
     * 
     */
    @Deprecated
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
    
    public static void CreateWatchingGroupLog (WatchingGroupLog watchingGroupLog, ResultListener<String> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, String>(new CreateWatchingGroupLog (watchingGroupLog), resultListener, context, showProgressBar).execute();
    }
    
    private static class CreateWatchingGroupLog implements Command<String> {
    	
    	WatchingGroupLog watchingGroupLog;
    	
    	public CreateWatchingGroupLog (WatchingGroupLog watchingGroupLog)
    	{
    		this.watchingGroupLog = watchingGroupLog;
    	}

		@Override
		public String execute() throws JSONException, IOException,
				SpikaException {
			return createWatchingGroupLog(watchingGroupLog);
		}
    }
    
    /**
     * Delete watching group log
     * 
     */
    @Deprecated
    public static boolean deleteWatchingGroupLog(String id, String rev) {

        return CouchDBHelper.deleteWatchingGroupLog((ConnectionHandler.deleteJsonObject(id, rev,
                UsersManagement.getLoginUser().getId(), "")));
    }
    
    public static void DeleteWatchingGroupLog(String id, String rev, ResultListener<Boolean> resultListener, Context context, boolean showProgressBar) {
    	new SpikaAsyncTask<Void, Void, Boolean>(new DeleteWatchingGroupLog(id, rev), resultListener, context, showProgressBar).execute();
    }
    
    private static class DeleteWatchingGroupLog implements Command<Boolean> {
    	
    	String id;
    	String rev;
    	
    	public DeleteWatchingGroupLog(String id, String rev)
    	{
    		this.id = id;
    		this.rev = rev;
    	}

		@Override
		public Boolean execute() throws JSONException, IOException,
				SpikaException {
			return deleteWatchingGroupLog(id, rev);
		}
    }

}
