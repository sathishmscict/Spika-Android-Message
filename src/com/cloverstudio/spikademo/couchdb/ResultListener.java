package com.cloverstudio.spikademo.couchdb;

public interface ResultListener<T> {
	public void onResultsSucceded(T result);
	public void onResultsFail();
}
