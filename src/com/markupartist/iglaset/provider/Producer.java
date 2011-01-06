package com.markupartist.iglaset.provider;

import com.markupartist.iglaset.provider.Drink.Volume;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

/**
 * @author marco
 * Container class for keeping producer data.
 */
public class Producer implements Parcelable {

	public final static int UNDEFINED_ID = -1;
	
	private String name;
	private int id = UNDEFINED_ID;
	private String description;
	private String country;
	private String state;
	private String town;
	private String adress;
	private String url;
	
    public Producer(Parcel in) {
    	name = in.readString();
        id = in.readInt();
        description = in.readString();
        country = in.readString();  
        state = in.readString();
        town = in.readString();
        adress = in.readString();
        url = in.readString();
    }
    
	public Producer() {
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeInt(id);
		dest.writeString(description);
		dest.writeString(description);
		dest.writeString(state);
		dest.writeString(town);
		dest.writeString(adress);
		dest.writeString(url);
	}
	
    public static final Creator<Producer> CREATOR = new Creator<Producer>() {
        public Producer createFromParcel(Parcel in) {
            return new Producer(in);
        }

        public Producer[] newArray(int size) {
            return new Producer[size];
        }
    };
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getCountry() {
		return country;
	}
	
	public void setCountry(String country) {
		this.country = country;
	}
	
	public String getState() {
		return state;
	}
	
	public void setState(String state) {
		this.state = state;
	}
	
	public String getTown() {
		return town;
	}
	
	public void setTown(String town) {
		this.town = town;
	}
	
	public String getAdress() {
		return adress;
	}
	
	public void setAdress(String adress) {
		this.adress = adress;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
}
