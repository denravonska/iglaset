package com.markupartist.iglaset.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.markupartist.iglaset.R;
import com.markupartist.iglaset.activity.SearchDrinksTask.SearchDrinkCompletedListener;
import com.markupartist.iglaset.provider.AuthStore;
import com.markupartist.iglaset.provider.AuthStore.Authentication;
import com.markupartist.iglaset.provider.AuthenticationException;
import com.markupartist.iglaset.provider.Drink;
import com.markupartist.iglaset.provider.DrinksStore;
import com.markupartist.iglaset.provider.Producer;
import com.markupartist.iglaset.provider.ProducerSearchCriteria;
import com.markupartist.iglaset.util.StringUtils;
import com.markupartist.iglaset.widget.SearchAction;
import com.markupartist.iglaset.widget.SectionedAdapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;


public class ProducerActivity extends MapActivity implements GetProducerTask.GetProducerListener, SearchDrinkCompletedListener {

	/**
	 * @author marco
	 * Class used to retain data between configuration changes such as screen rotation.
	 */
	private static class ConfigurationInstance {
		public Producer producer;
		public GeoPoint producerLocation;
	}
	
	private final static String TAG = ProducerActivity.class.getSimpleName();
	public final static String EXTRA_PRODUCER_ID = "com.markupartist.iglaset.action.PRODUCER_ID";
	public final static String EXTRA_PRODUCER = "com.markupartist.iglaset.action.PRODUCER_ID";
	private Producer producer;
	private GeoPoint producerLocation;
	private GetProducerTask getProducerTask;
	private GetAddressTask getAddressTask;
	private SearchDrinksTask searchDrinksTask;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.producer_details);
		
		MapView mapView = (MapView) findViewById(R.id.producer_map);
		mapView.displayZoomControls(true);
		
		// TODO Implement double-click-to-zoom
		// http://stackoverflow.com/questions/3044259/google-maps-in-android
		
		ConfigurationInstance instance = (ConfigurationInstance) getLastNonConfigurationInstance();
		if(instance != null) {
			producer = instance.producer;
			producerLocation = instance.producerLocation;
			
			if(producerLocation != null) {
				this.onProducerLocation(producerLocation);
			}
		}
		
        Bundle extras = getIntent().getExtras();
        if(extras.containsKey(EXTRA_PRODUCER)) {
        	producer = (Producer) extras.getParcelable(EXTRA_PRODUCER);
        }
        
        if(producer != null) {
        	onGetProducerComplete(producer);
        } else if(extras.containsKey(EXTRA_PRODUCER_ID)) {
			final int producerId = extras.getInt(EXTRA_PRODUCER_ID);
			launchGetProducerTask(producerId);
		} else {
			Log.e(TAG, "No producer data available :(");
		}
	}
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	ConfigurationInstance instance = new ConfigurationInstance();
    	instance.producer = producer;
    	instance.producerLocation = producerLocation;
    	//instance.producer = mProducer;
    	return instance;
    }
    
    SectionedAdapter sectionedAdapter = new SectionedAdapter() {
        protected View getHeaderView(Section section, int index, 
                View convertView, ViewGroup parent) {
            TextView result = (TextView) convertView;

            if (convertView == null)
                result = (TextView) getLayoutInflater().inflate(R.layout.header, null);

            result.setText(section.caption);
            return (result);
        }
    };
    
    @Override
    public void onDestroy() {
    	cancelGetProducerTask();
    	cancelGetAddressTask();
    	super.onDestroy();
    }
    
    private void launchGetProducerTask(int producerId) {
    	cancelGetProducerTask();
    	getProducerTask = new GetProducerTask(this);
    	getProducerTask.execute(producerId);
    }
    
    private void cancelGetProducerTask() {
    	if(null != getProducerTask && getProducerTask.getStatus() == AsyncTask.Status.RUNNING) {
    		getProducerTask.cancel(true);
    	}
    }
    
    private void launchGetAddressTask(Producer producer) {
    	cancelGetAddressTask();
    	getAddressTask = new GetAddressTask(this, (MapView) findViewById(R.id.producer_map));
    	getAddressTask.execute(producer);
    }
    
    private void cancelGetAddressTask() {
    	if(null != getAddressTask && getAddressTask.getStatus() == AsyncTask.Status.RUNNING) {
    		getAddressTask.cancel(true);
    	}
    }
    
    private void launchSearchDrinksTask(int producerId) {
    	cancelSearchDrinksTask();
    	
    	ProducerSearchCriteria searchCriteria = new ProducerSearchCriteria(producerId);
		try {
			Authentication auth = AuthStore.getInstance().getAuthentication(this); 
			searchCriteria.setAuthentication(auth);
		} catch (AuthenticationException e) {
		}
    	
    	searchDrinksTask = new SearchDrinksTask();
    	searchDrinksTask.setSearchDrinkCompletedListener(this);
    	searchDrinksTask.execute(searchCriteria);
    }
    
    private void cancelSearchDrinksTask() {
    	if(null != getProducerTask && getProducerTask.getStatus() == AsyncTask.Status.RUNNING) {
    		getProducerTask.cancel(true);
    	}
    }

	private String getBestLocation(Producer producer) {
		ArrayList<String> fields = new ArrayList<String>();
		
		if(isAddressValid(producer.getAdress()))
			fields.add(producer.getAdress());
		
		if(isAddressValid(producer.getTown()))
			fields.add(producer.getTown());

		// We intentionally ignore the state location here since it does more
		// harm than good.
		
		if(isAddressValid(producer.getCountry()))
			fields.add(producer.getCountry());
		
		return StringUtils.join(fields.toArray(), ", ");
	}
	
	private Boolean isAddressValid(String field) {
		// Sometimes "-" is used since you HAVE to enter something when
		// creating producers on iglaset.se. Try to work around that.
		return !TextUtils.isEmpty(field) && field.length() > 1;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	
	private static class MarkerOverlay extends ItemizedOverlay<OverlayItem> {

		private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();

		public MarkerOverlay(Drawable drawable) {
			super(boundCenter(drawable));
		}
		
		@Override
		protected OverlayItem createItem(int i) {
			return overlays.get(i);
		}

		@Override
		public int size() {
			return overlays.size();
		}
		
		public void addOverlay(OverlayItem item) {
			overlays.add(item);
			populate();
		}
	}


	@Override
	public void onGetProducerComplete(Producer producer) {
    	this.producer = producer;
    	
    	if(producer != null) {
    		if(producerLocation == null) {
    			launchGetAddressTask(producer);
    		}
    		
    		launchSearchDrinksTask(producer.getId());
    		
            ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
            actionBar.setHomeAction(new IntentAction(this, StartActivity.createIntent(this), R.drawable.ic_actionbar_home_default));
            actionBar.setTitle(producer.getName());
            actionBar.addAction(new SearchAction() {
                @Override
                public void performAction() {
                    onSearchRequested();
                }
            });
            
    		/*CharSequence info[] = {
    				producer.getName(),		
    		};
    		sectionedAdapter.addSection(0, "Producent", createSimpleAdapter(info));
    		
    		ListView list = (ListView) findViewById(R.id.producer_information_list);
    		list.setAdapter(sectionedAdapter);*/
    	}
	}
	
	private SimpleAdapter createSimpleAdapter(CharSequence[] payload) {
    	Map<String, Object> data = new HashMap<String, Object>();
    	for(int i=0; i<payload.length; ++i) {
    		data.put("text", payload[i]);
    	}
    	
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.add(data);

    	return new SimpleAdapter(
    			this,
    			list,
    			R.layout.simple_row,
                new String[] { "text" },
                new int[] { 
                    R.id.simple_row_text
    			});
	}

	@Override
	public void onGetProducerError() {
		Log.e(TAG, "Failed to get producer.");
	}
	
	private void onUpdatedDrinks(ArrayList<Drink> drinks) {
		
	}
	
	public class GetAddressTask extends AsyncTask<Producer, Void, GeoPoint> {
		
		private Context context;
		MapView mapView;
		
		public GetAddressTask(Context context, MapView mapView) {
			this.context = context;
			this.mapView = mapView;
		}
		
	    @Override
	    protected GeoPoint doInBackground(Producer... params) {
			Geocoder geoCoder = new Geocoder(context);
			GeoPoint point = null;
			
			try {
				List<Address> locations = geoCoder.getFromLocationName(getBestLocation(producer), 1);
				if(locations.size() > 0) {
					Address address = locations.get(0);
					
					Double lat = address.getLatitude() * 1e6;
					Double lon = address.getLongitude() * 1e6;
					point = new GeoPoint(lat.intValue(), lon.intValue());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}
			
	    	return point;	
	    }
	    
	    @Override
	    protected void onPostExecute(GeoPoint point) {
	    	if(null != point) {
	    		onProducerLocation(point);
	    	} else {
	    		Log.e(TAG, "Error fetching producer location");
	    	}
	    }
	}

    /**
     * List adapter for drinks. Deals with pagination internally.
     */
    class DrinkAdapter extends ArrayAdapter<Drink> implements SearchDrinkCompletedListener {
        private AtomicBoolean mShouldAppend = new AtomicBoolean(true);
        private AtomicInteger mPage = new AtomicInteger(1);
        private int mMaxResult = 100;

        public DrinkAdapter(Context context, List<Drink> objects) {
            super(context, R.layout.drink_detail, objects);
        }

        private boolean shouldAppend(int position) {
            return (position == super.getCount() - 1 
                    && mShouldAppend.get()
                    && super.getCount() >= 10
                    && super.getCount() <= mMaxResult);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DrinkViewHolder dvh = null;
            
            if (shouldAppend(position)) {
                /*getListView().addFooterView(mFooterProgressView);
                getListView().forceLayout();
                mSearchCriteria.setPage(mPage.addAndGet(1));
                SearchDrinksTask task = createSearchDrinksTask();
                task.setSearchDrinkCompletedListener(this);
                setProgressBarIndeterminateVisibility(true);
                task.execute(mSearchCriteria);*/
            }

            if (convertView == null) {
	            convertView = getLayoutInflater().inflate(R.layout.drink_detail, null);
	            dvh = new DrinkViewHolder(convertView);
	            convertView.setTag(dvh);
            } else {
                dvh = (DrinkViewHolder) convertView.getTag(); 

                // Change to the default image or else the thumbnail will show
                // an old image used in the reused view. The user won't notice it.
                dvh.getImageView().setImageResource(R.drawable.noimage);
            }

            final Drink drink = getItem(position);
            if (drink != null && dvh != null) {
            	dvh.populate(getContext(), drink, null);
                if(drink.hasUserRating()) {
                	dvh.getRateView().setRating(drink.getUserRating());
                } else {
                	float rating = (drink.hasEstimatedRating() ? drink.getEstimatedRating() : drink.getAverageRating());
                	dvh.getRateView().setRating(rating);
                }
            }

            return convertView;
        }

        /**
         * Append drinks to the list.
         * @param drinks drinks to add to the list
         * @return if we should append more or not
         */
        protected boolean append(ArrayList<Drink> drinks) {
            if (drinks.isEmpty()) {
                mShouldAppend.set(false);
                return mShouldAppend.get();
            }

            for (Drink drink : drinks) {
                add(drink);
            }

            return mShouldAppend.get();
        }

        public void onSearchDrinkComplete(ArrayList<Drink> result) {
            append(result);
            setProgressBarIndeterminateVisibility(false);
            //if (mFooterProgressView != null) {
            //    getListView().removeFooterView(mFooterProgressView);
            //}
        }

        public void onItemClick() {
            Log.d("onItemClick", "");
        }
    }

	@Override
	public void onSearchDrinkComplete(ArrayList<Drink> result) {
        DrinkAdapter adapter = new DrinkAdapter(this, result);
		ListView list = (ListView) findViewById(R.id.producer_information_list);
        list.setAdapter(adapter);
	}
	
	private void onProducerLocation(GeoPoint point) {
		producerLocation = point;
		
		MapView mapView = (MapView) findViewById(R.id.producer_map);
		
		mapView.getController().setCenter(producerLocation);
		
		Drawable marker = getResources().getDrawable(R.drawable.map_indicator);
		MarkerOverlay overlay = new MarkerOverlay(marker);
		overlay.addOverlay(new OverlayItem(producerLocation, producer.getName(), producer.getName()));
		mapView.getOverlays().add(overlay);
	}
}
