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

package com.cloverstudio.spikademo.messageshandling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.util.Log;

import com.cloverstudio.spikademo.WallActivity;
import com.cloverstudio.spikademo.couchdb.Command;
import com.cloverstudio.spikademo.couchdb.CouchDB;
import com.cloverstudio.spikademo.couchdb.ResultListener;
import com.cloverstudio.spikademo.couchdb.SpikaAsyncTask;
import com.cloverstudio.spikademo.couchdb.SpikaException;
import com.cloverstudio.spikademo.couchdb.model.Message;
import com.cloverstudio.spikademo.dialog.HookUpProgressDialog;
import com.cloverstudio.spikademo.extendables.SpikaAsync;
import com.cloverstudio.spikademo.management.SettingsManager;
import com.cloverstudio.spikademo.management.TimeMeasurer;
import com.cloverstudio.spikademo.management.UsersManagement;

/**
 * MessagesUpdater
 * 
 * Executes AsyncTask for fetching messages from CouchDB.
 */

public class MessagesUpdater {

	public static boolean gRegularRefresh = true;
	public static boolean gIsLoading = false;

	public static void update(boolean regularRefresh) {
		gRegularRefresh = regularRefresh;
		if (WallActivity.getInstance() != null) {
			getMessagesAsync(regularRefresh);
		}
	}

	private static void getMessagesAsync (boolean reguralRefresh) {
		gIsLoading = true;
		new SpikaAsyncTask<Void, Void, ArrayList<Message>>(new GetMessages(), new GetMessagesFinish(), WallActivity.getInstance(), reguralRefresh).execute();
	}
	
	private static class GetMessages implements Command<ArrayList<Message>> {

		@Override
		public ArrayList<Message> execute() throws JSONException, IOException,
				SpikaException {
			ArrayList<Message> newMessages = new ArrayList<Message>();

            TimeMeasurer.dumpInterval("Before request");

			if (gIsLoading) {
				if (gRegularRefresh) {
					newMessages = CouchDB.findMessagesForUser(
							UsersManagement.getLoginUser(), 0);
				} else {
					newMessages = CouchDB.findMessagesForUser(
							UsersManagement.getLoginUser(),
							SettingsManager.sPage);
				}
			}
			return newMessages;
		}
	}
	
	private static class GetMessagesFinish implements ResultListener<ArrayList<Message>> {

		@Override
		public void onResultsSucceded(ArrayList<Message> result) {
			TimeMeasurer.dumpInterval("After request");

			if (gIsLoading) {
				UpdateMessagesInListView.updateListView(result);
			}

			gIsLoading = false;
			gRegularRefresh = true;

			if (WallActivity.getInstance() != null)
				WallActivity.getInstance().checkMessagesCount();
		}

		@Override
		public void onResultsFail() {			
		}
	}
}
