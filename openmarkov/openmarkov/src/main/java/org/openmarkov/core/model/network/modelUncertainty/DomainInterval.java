/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

/**
 * @author manolo
 * Represents the continuous interval [a,b]
 */
public class DomainInterval {

	private double a;
	private double b;

	public DomainInterval(double a, double b) {
		super();
		this.a = a;
		this.b = b;
	}

	public double min() {
		return a;
	}

	public double max() {
		return b;
	}

}
