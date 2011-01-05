package com.markupartist.iglaset.provider;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProducerParser extends AbstractParser<Producer> {

	ArrayList<Producer> producerList = new ArrayList<Producer>();
	Producer currentProducer;

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        producerList.clear();
    }
	
	@Override
	public void onStartElement(String name, Attributes atts) {
		if(name.equals("producer")) {
			currentProducer = new Producer();
		}
	}

	@Override
	public void onEndElement(String name, String result) {
		if(currentProducer != null) {
			if(name.equals("producer")) {
				producerList.add(currentProducer);
				currentProducer = null;
			} else if(name.equals("name")) {
				currentProducer.setName(result);
			} else if(name.equals("id")) {
				currentProducer.setId(Integer.parseInt(result));
			} else if(name.equals("about")) {
				currentProducer.setDescription(result);
			} else if(name.equals("address")) {
				currentProducer.setAdress(result);
			} else if(name.equals("country")) {
				currentProducer.setCountry(result);
			} else if(name.equals("state")) {
				currentProducer.setState(result);
			}
			
			// contact
			
		}
	}

	@Override
	protected ArrayList<Producer> getContent() {
		return producerList;
	}
}
