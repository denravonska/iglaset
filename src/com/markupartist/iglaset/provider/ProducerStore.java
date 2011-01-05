package com.markupartist.iglaset.provider;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

import com.markupartist.iglaset.util.HttpManager;

public class ProducerStore {

    private static String TAG = ProducerStore.class.getSimpleName();
    private static String PRODUCER_URI = "http://www.iglaset.se/producers/%d.xml";
	
    public static Producer getProducer(int id) {
        final HttpGet get = new HttpGet(String.format(PRODUCER_URI, id));
        ArrayList<Producer> producerList = getProducers(get);
        return producerList.size() > 0 ? producerList.get(0) : null;
    }
    
    private static ArrayList<Producer> getProducers(HttpGet get) {
        ArrayList<Producer> producerList = null;
        HttpEntity entity = null;
        try {
            final HttpResponse response = HttpManager.execute(get);
            entity = response.getEntity();
            ProducerParser parser = new ProducerParser();
            producerList = parser.parse(entity.getContent());
        } catch (IOException e) {
            Log.d(TAG, "Failed to read data: " + e.getMessage());
        }
        
        return producerList;
    }
}
