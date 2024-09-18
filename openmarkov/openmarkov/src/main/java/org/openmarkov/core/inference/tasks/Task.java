/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference.tasks;

import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Variable;

/**
 * This class represents a user action related to inference.
 */
public interface Task {

	void setPreResolutionEvidence(EvidenceCase preresolutionEvidence) throws IncompatibleEvidenceException;

	void setConditioningVariables(List<Variable> conditioningVariables);

}
