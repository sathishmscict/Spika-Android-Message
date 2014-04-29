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

package com.cloverstudio.spika;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.cloverstudio.spika.R;
import com.cloverstudio.spika.couchdb.Command;
import com.cloverstudio.spika.couchdb.CouchDB;
import com.cloverstudio.spika.couchdb.ResultListener;
import com.cloverstudio.spika.couchdb.SpikaAsyncTask;
import com.cloverstudio.spika.couchdb.SpikaException;
import com.cloverstudio.spika.couchdb.SpikaForbiddenException;
import com.cloverstudio.spika.couchdb.model.User;
import com.cloverstudio.spika.management.UsersManagement;
import com.cloverstudio.spika.utils.Const;
import com.cloverstudio.spika.utils.Logger;
import com.google.android.gcm.GCMBaseIntentService;

/**
 * GCMIntentService
 * 
 * Handles push broadcast and generates HookUp notification if application is in
 * foreground or Android notification if application is in background.
 */

public class GCMIntentService extends GCMBaseIntentService {

	private static int mNotificationCounter = 1;
	public final static String PUSH = "com.cloverstudio.spika.GCMIntentService.PUSH";
	private static final Intent mPushBroadcast = new Intent(PUSH);

	public GCMIntentService() {
		super(Const.PUSH_SENDER_ID);
	}

	private final String TAG = "=== GCMIntentService ===";

	/**
	 * Method called on device registered
	 **/
	@Override
	protected void onRegistered(Context context, String registrationId) {

		if (!registrationId.equals(null)) {
			savePushTokenAsync(registrationId, Const.ONLINE, context);
		}

	}

	/**
	 * Method called on device unregistered
	 * */
	@Override
	protected void onUnregistered(Context context, String registrationId) {

		if (!registrationId.equals(null)) {
			removePushTokenAsync(context);
		}
	}

	/**
	 * Method called on Receiving a new message
	 * */
	@Override
	protected void onMessage(Context context, Intent intent) {

		Bundle pushExtras = intent.getExtras();
		String pushMessage = intent.getStringExtra(Const.PUSH_MESSAGE);
		String pushFromName = intent.getStringExtra(Const.PUSH_FROM_NAME);
		try {
			boolean appIsInForeground = new SpikaApp.ForegroundCheckAsync()
					.execute(getApplicationContext()).get();
			boolean screenLocked = ((KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE))
					.inKeyguardRestrictedInputMode();
			if (appIsInForeground && !screenLocked) {

				mPushBroadcast.replaceExtras(pushExtras);

				LocalBroadcastManager.getInstance(this).sendBroadcast(
						mPushBroadcast);
			} else {
				triggerNotification(this, pushMessage, pushFromName, pushExtras);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Method called on Error
	 * */
	@Override
	protected void onError(Context arg0, String errorId) {
		Logger.error(TAG, "Received error: " + errorId);
	}

	@Override
	protected boolean onRecoverableError(Context context, String errorId) {
		return super.onRecoverableError(context, errorId);
	}

	@SuppressWarnings("deprecation")
	public void triggerNotification(Context context, String message,
			String fromName, Bundle pushExtras) {

		if (fromName != null) {
			final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			Notification notification = new Notification(
					R.drawable.icon_notification, message,
					System.currentTimeMillis());
			notification.number = mNotificationCounter + 1;
			mNotificationCounter = mNotificationCounter + 1;

			Intent intent = new Intent(this, SplashScreenActivity.class);
			intent.replaceExtras(pushExtras);
			intent.putExtra(Const.PUSH_INTENT, true);
			intent.setAction(Long.toString(System.currentTimeMillis()));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_FROM_BACKGROUND
					| Intent.FLAG_ACTIVITY_TASK_ON_HOME);
			PendingIntent pendingIntent = PendingIntent.getActivity(this,
					notification.number, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			notification.setLatestEventInfo(this,
					context.getString(R.string.app_name), message,
					pendingIntent);
			notification.defaults |= Notification.DEFAULT_VIBRATE;
			notification.defaults |= Notification.DEFAULT_SOUND;
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			String notificationId = Double.toString(Math.random());
			notificationManager.notify(notificationId, 0, notification);
		}
	}

	private void savePushTokenAsync (String pushToken, String onlineStatus, Context context) {
		new SpikaAsyncTask<Void, Void, Boolean>(new SavePushToken(pushToken, onlineStatus), new SavePushTokenListener(pushToken), context, false).execute();
	}
	
	private class SavePushToken implements Command<Boolean>{

		String pushToken;
		String onlineStatus;
		
		public SavePushToken (String pushToken, String onlineStatus) {
			this.pushToken = pushToken;
			this.onlineStatus = onlineStatus;
		}
		
		@Override
		public Boolean execute() throws JSONException, IOException,
				SpikaException, IllegalStateException, SpikaForbiddenException {

			/* set new androidToken and onlineStatus */
			UsersManagement.getLoginUser().setOnlineStatus(onlineStatus);
			SpikaApp.getPreferences().setUserEmail(UsersManagement.getLoginUser().getEmail());
			SpikaApp.getPreferences().setUserPushToken(pushToken);
			return CouchDB.updateUser(UsersManagement.getLoginUser());
		}
	}
	
	private class SavePushTokenListener implements ResultListener<Boolean>{

		String currentPushToken;
		
		public SavePushTokenListener (String currentPushToken) {
			this.currentPushToken = currentPushToken;
		}
		
		@Override
		public void onResultsSucceded(Boolean result) {
			if (result) {
			} else {
				SpikaApp.getPreferences().setUserPushToken(currentPushToken);
			}
		}

		@Override
		public void onResultsFail() {
		}
		
	}
	
	private void removePushTokenAsync (Context context) {
		SpikaApp.getPreferences().setUserPushToken("");
		if (UsersManagement.getLoginUser() != null) {
			CouchDB.unregisterPushTokenAsync(UsersManagement.getLoginUser().getId(), new RemovePushTokenListener(), context, false);
		}
	}
	
	private class RemovePushTokenListener implements ResultListener<String> {
		@Override
		public void onResultsSucceded(String result) {
			if (result != null && result.contains("OK")) {
				SpikaApp.getPreferences().setUserEmail("");
				SpikaApp.getPreferences().setUserPassword("");
			}
		}
		@Override
		public void onResultsFail() {
		}
	}
}
