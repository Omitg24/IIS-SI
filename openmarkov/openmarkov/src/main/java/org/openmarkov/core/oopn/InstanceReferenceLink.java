/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.oopn;

public class InstanceReferenceLink extends ReferenceLink {

	private Instance sourceInstance;
	private Instance destInstance;
	private Instance destSubInstance;

	/**
	 * Constructor
	 *
	 * @param sourceInstance Source instance
	 * @param destInstance Destination instance
	 * @param destSubInstance Destination sub-instance
	 */
	public InstanceReferenceLink(Instance sourceInstance, Instance destInstance, Instance destSubInstance) {
		super();
		this.sourceInstance = sourceInstance;
		this.destInstance = destInstance;
		this.destSubInstance = destSubInstance;
	}

	/**
	 * @return the sourceInstance
	 */
	public Instance getSourceInstance() {
		return sourceInstance;
	}

	/**
	 * @return the destInstance
	 */
	public Instance getDestInstance() {
		return destInstance;
	}

	/**
	 * @return the destSubInstance
	 */
	public Instance getDestSubInstance() {
		return destSubInstance;
	}

}
