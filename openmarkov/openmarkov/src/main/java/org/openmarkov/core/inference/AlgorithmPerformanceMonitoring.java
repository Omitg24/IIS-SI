/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference;

import java.util.List;

public interface AlgorithmPerformanceMonitoring {
	/**
	 * @return An array list containing, for each important operation performed in the inference, the storage space in that moment.
	 * Values are stored in increasing time, with the initial space in the cell 0, and the last operation in the last cell of the array.
	 */
	List<Double> getStorageSpaceRequired();

}
