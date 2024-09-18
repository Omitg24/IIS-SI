/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.Random;

@SuppressWarnings("serial") public class XORShiftRandom extends Random {

	private long seed = System.nanoTime();


	public XORShiftRandom() {
	}

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
    }

    protected int next(int nbits) {
		// TODO N.B. Not thread-safe!
		long x = this.seed;
		x ^= (x << 21);
		x ^= (x >>> 35);
		x ^= (x << 4);
		this.seed = x;
		x &= ((1L << nbits) - 1);
		return (int) x;
	}
}
