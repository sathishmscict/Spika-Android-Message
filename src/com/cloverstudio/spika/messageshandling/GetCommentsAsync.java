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

package com.cloverstudio.spika.messageshandling;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.content.Context;
import android.widget.ListView;

import com.cloverstudio.spika.PhotoActivity;
import com.cloverstudio.spika.R;
import com.cloverstudio.spika.VideoActivity;
import com.cloverstudio.spika.VoiceActivity;
import com.cloverstudio.spika.adapters.CommentsAdapter;
import com.cloverstudio.spika.couchdb.CouchDB;
import com.cloverstudio.spika.couchdb.SpikaException;
import com.cloverstudio.spika.couchdb.SpikaForbiddenException;
import com.cloverstudio.spika.couchdb.model.Comment;
import com.cloverstudio.spika.couchdb.model.Message;
import com.cloverstudio.spika.extendables.SpikaAsync;

/**
 * GetCommentsAsync
 * 
 * AsyncTask for fetching comments from CouchDB.
 */

public class GetCommentsAsync extends SpikaAsync<String, Void, List<Comment>> {

	private Context mContext;
	private Message mMessage;
	private List<Comment> mComments;
	private CommentsAdapter mCommentsAdapter;
	private ListView mCommentListView;
	private boolean mFirstTime;

	public GetCommentsAsync(Context context, Message message,
			List<Comment> comments, CommentsAdapter commentsAdapter,
			ListView commentListView) {
		super(context);
		mContext = context;
		mMessage = message;
		mComments = comments;
		mCommentsAdapter = commentsAdapter;
		mCommentListView = commentListView;
	}

	public GetCommentsAsync(Context context, Message message,
			List<Comment> comments, CommentsAdapter commentsAdapter,
			ListView commentListView,
			boolean firstTime) {
		super(context);
		mContext = context;
		mMessage = message;
		mComments = comments;
		mCommentsAdapter = commentsAdapter;
		mCommentListView = commentListView;
		mFirstTime=firstTime;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected List<Comment> backgroundWork(String... params) throws ClientProtocolException, IOException, JSONException, SpikaException, IllegalStateException, SpikaForbiddenException {

		return CouchDB.findCommentsByMessageId(params[0]);
	}

	@Override
	protected void onPostExecute(List<Comment> result) {
		super.onPostExecute(result);

		if (result.size() > mComments.size() || mFirstTime) {
			mComments = result;
			
			Collections.sort(mComments, new Comparator<Comment>() {

				@Override
				public int compare(Comment lhs, Comment rhs) {
					return Integer.parseInt(lhs.getId()) - Integer.parseInt(rhs.getId());
				};
			});
			
			if (mCommentsAdapter == null) {
				mCommentsAdapter = new CommentsAdapter(mContext,
						R.layout.comment_item, mComments);
				mCommentListView.setAdapter(mCommentsAdapter);
				mCommentListView.setOnScrollListener(mCommentsAdapter);
			} else {
				mCommentsAdapter.notifyDataSetChanged();
			}
		}

		if (mContext instanceof VoiceActivity) {
			((VoiceActivity) mContext).setMessageFromAsync(mMessage);
		} else if (mContext instanceof VideoActivity) {
			((VideoActivity) mContext).setMessageFromAsync(mMessage);
		} else if (mContext instanceof PhotoActivity) {
			((PhotoActivity) mContext).setMessageFromAsync(mMessage);
		}
	}
}
