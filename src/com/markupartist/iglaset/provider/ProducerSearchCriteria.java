package com.markupartist.iglaset.provider;

import com.markupartist.iglaset.provider.AuthStore.Authentication;

public class ProducerSearchCriteria extends SearchCriteria {

	private final static int[] SortModes = {
		SearchCriteria.SORT_MODE_NONE
	};
	
	public ProducerSearchCriteria(int producerId) {
		super.setProducer(producerId);
	}

	public ProducerSearchCriteria(int producerId, Authentication authentication) {
		super.setProducer(producerId);
		super.setAuthentication(authentication);
	}
	
	@Override
	public int[] getSortModes() {
		return SortModes;
	}

	@Override
	public int getDefaultSortMode() {
		return SORT_MODE_NONE;
	}

}
