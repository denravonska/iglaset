package com.markupartist.iglaset.provider;

public class TextSearchCriteria extends SearchCriteria {

	private final static int[] SortModes = {
		SearchCriteria.SORT_MODE_NONE,
		SearchCriteria.SORT_MODE_NAME,
		SearchCriteria.SORT_MODE_PRODUCER
	};

	@Override
	public int[] getSortModes() {
		return SortModes.clone();
	}

	@Override
	public int getDefaultSortMode() {
		return SearchCriteria.SORT_MODE_NONE;
	}

}
