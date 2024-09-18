/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.decompositionIntoSymmetricDANs.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openmarkov.core.exception.CostEffectivenessException;
import org.openmarkov.core.exception.PotentialOperationException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.inference.variableElimination.ChanceVariableElimination;
import org.openmarkov.inference.variableElimination.DecisionVariableElimination;

public abstract class DANInference {
	
	

	boolean isCEAnalysis;

	/**
	 * These two attributes are the result of the evaluation
	 */
	protected TablePotential probability;

	protected ProbNet probNet;

	protected TablePotential utility;
	
	


	/**
	 * These two attributes are used for storing the results of evaluating the children in a decomposition scheme
	 * The i-th element of each list corresponds to the result of the evaluation of the i-th child
	 */
	protected List<TablePotential> childrenProbability;
	protected List<TablePotential> childrenUtility;

	public DANInference(ProbNet network, boolean isCEAnalysis2) {
		this.probNet = network.copy();
		childrenProbability = new ArrayList<>();
		childrenUtility = new ArrayList<>();
		isCEAnalysis = isCEAnalysis2;
	}

	public void setProbability(TablePotential probability) {
		if (probability == null) {
			probability = DiscretePotentialOperations.createUnityProbabilityPotential();
		}
		this.probability = probability;
	}

	protected void setUtility(TablePotential util) {
		if (util.getCriterion() == null) {
			util.setCriterion(this.probNet.getDecisionCriteria().get(0));
		}
		this.utility = util;
	}

	protected void addProbabilityChildEvaluation(TablePotential prob) {

	}

	/**
	 * @param x Variable
	 *          This method...:
	 *          First, it conditions on Vaiable 'x' the sets 'probabilityPotentials' and 'utilityPotentials'.
	 *          Second, it marginalizes 'x' out of the resulting potentials of the first step.
	 *          Third, it stores the resulting probability and utility potentials in the corresponding attributes.
	 */
	protected void conditionEliminateChanceAndSetProbabilityAndUtility(ProbNet dan, Variable x) {
		TablePotential conditionedProbabilityPotential = null;
		TablePotential conditionedUtilityPotential = null;
		try {
			conditionedProbabilityPotential = DiscretePotentialOperations.merge(x, childrenProbability);
			conditionedUtilityPotential = DiscretePotentialOperations.merge(x, childrenUtility);
		} catch (PotentialOperationException e1) {
			e1.printStackTrace();
		}
		eliminateChanceVariable(dan, x, conditionedProbabilityPotential, conditionedUtilityPotential);
	}

	/**
	 * @param dan                  Network
	 * @param x
	 * @param probabilityPotential
	 * @param utilityPotential     It eliminates the variable 'x' from the sets of 'probability'
	 *                             and 'utility' potentials (as in a variable-elimination
	 *                             scheme). The result of the method are a probability and
	 *                             utility potential that are stored in the attributes
	 *                             "probability" and "utility" respectively.
	 */
	protected void eliminateChanceVariable(ProbNet dan, Variable x, TablePotential probabilityPotential,
			TablePotential utilityPotential) {
		ChanceVariableElimination elimination = null;
		try {
			elimination = new ChanceVariableElimination(x, Arrays.asList(probabilityPotential),
					Arrays.asList(utilityPotential));
		} catch (CostEffectivenessException e) {
			e.printStackTrace();
		}
		setProbability(elimination.getMarginalProbability());
		setUtility(DiscretePotentialOperations.sum(elimination.getUtilityPotentials()));
	}

	protected void maximizeAndSetUtility(ProbNet dan, Variable rootDecision, TablePotential probability,
			TablePotential conditionedUtilityPotential) {

		DecisionVariableElimination elimination = null;
		try {
			elimination = new DecisionVariableElimination(rootDecision, Arrays.asList(probability),
					Arrays.asList(conditionedUtilityPotential));
		} catch (PotentialOperationException e) {
			e.printStackTrace();
		}

		TablePotential newUtilityPotential = elimination.getUtility();
		if (newUtilityPotential == null) {
			newUtilityPotential = DiscretePotentialOperations.createZeroUtilityPotential(dan);
		}
		setProbability(elimination.getProjectedProbability());
		setUtility(newUtilityPotential);
	}

	/**
	 * @param dan
	 * @param d   First, it conditions on 'd' the sets 'probabilityPotentials'
	 *            and 'utilityPotentials'. Second, it maximizes over 'd' the
	 *            resulting potentials. Third, it stores the resulting
	 *            probability and utility potentials in the corresponding
	 *            attributes of the object.
	 */
	protected void conditionMaximizeAndSetProbabilityAndUtility(ProbNet dan, Variable d) {
		// We take the probability of any of the children, as it should be equal
		setProbability(childrenProbability.get(0));
		TablePotential conditionedUtilityPotential = null;
		try {
			conditionedUtilityPotential = DiscretePotentialOperations.merge(d, childrenUtility);
		} catch (PotentialOperationException e) {
			e.printStackTrace();
		}
		maximizeAndSetUtility(dan, d, childrenProbability.get(0), conditionedUtilityPotential);
	}

	protected void addResultsOfChildEvaluation(DANInference auxEval) {

		childrenProbability.add(auxEval.getProbability());
		childrenUtility.add(auxEval.getUtility());
	}

	/*
	 * private TablePotential getDANInferenceProcessProbability() { return
	 * probability; }
	 */

	public void setProbabilityAndUtilityFromEvaluation(DANInference eval) {
		setProbability(eval.getProbability());
		setUtility(eval.getUtility());

	}

	/*
	 * private TablePotential getDANInferenceProcessUtility() { return utility; }
	 */
	
	protected List<Variable> getAlwaysObservedVariables(ProbNet dan, List<Variable> conditioningVariablesList,
			EvidenceCase evidenceCase){
		return DANOperations.getVariablesObservedFromTheBegginning(dan,conditioningVariablesList,evidenceCase, true);
	}
	
	
	public TablePotential getProbability() {
		return probability;
	}

	public TablePotential getUtility() {
		return utility;
	}


}
