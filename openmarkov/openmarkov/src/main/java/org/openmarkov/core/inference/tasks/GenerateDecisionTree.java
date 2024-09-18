package org.openmarkov.core.inference.tasks;

import org.openmarkov.core.dt.DecisionTreeNode;
import org.openmarkov.core.exception.NotEvaluableNetworkException;

public interface GenerateDecisionTree extends Task {
	
	/**
	 * @return the decision tree
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	public DecisionTreeNode getDecisionTree() throws NotEvaluableNetworkException;

}
