package com.markupartist.iglaset.activity;

import java.security.InvalidParameterException;

import android.os.AsyncTask;

import com.markupartist.iglaset.provider.Producer;
import com.markupartist.iglaset.provider.ProducerStore;

public class GetProducerTask extends AsyncTask<Integer, Void, Producer> {
	
	GetProducerListener listener;
	
	public GetProducerTask(GetProducerListener listener) {
		if(listener == null) {
			throw new InvalidParameterException("Listener cannot be null");
		}
		
		this.listener = listener;
	}
	
    @Override
    protected Producer doInBackground(Integer... params) {
    	return ProducerStore.getProducer(params[0]);	
    }
    
    @Override
    protected void onPostExecute(Producer result) {
    	if(null != result) {
    		listener.onGetProducerComplete(result);
    	} else {
    		listener.onGetProducerError();
    	}
    }
    
    public interface GetProducerListener {
    	public void onGetProducerComplete(Producer producer);
    	public void onGetProducerError();
    }
}
