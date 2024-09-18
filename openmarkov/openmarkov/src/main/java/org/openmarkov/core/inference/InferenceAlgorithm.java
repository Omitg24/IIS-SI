/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.action.PNESupport;
import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.inference.tasks.Task;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.PNConstraint;
import org.openmarkov.core.model.network.type.NetworkType;

/**
 * @author mluque
 * @author marias
 * @author fjdiez
 */
public abstract class InferenceAlgorithm implements Task {

	/**
	 * This is a copy of the {@code ProbNet} received.
	 */
	protected ProbNet probNet;
	/**
	 * For undo/redo operations.
	 * TODO - Check if this is necessary
	 */
	protected PNESupport pNESupport;
	/**
	 * Variables that will not be eliminated during the inference, and therefore all the results
	 * contain these variables in the domain.
	 */
	protected List<Variable> conditioningVariables;
	/**
	 * Evidence introduced before the network is resolved.
	 * In influence diagrams this is Ezawa's evidence.
	 */
	private EvidenceCase preResolutionEvidence;

	/**
	 * @param network The network used in the inference
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	public InferenceAlgorithm(ProbNet network) throws NotEvaluableNetworkException {
		this.probNet = network.copy();
		this.preResolutionEvidence = new EvidenceCase();
		this.conditioningVariables = new ArrayList<>();
		checkEvaluability();
		checkConsistency();
	}

	/**
	 * Checks the network and constraints applicability
	 *
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	public void checkEvaluability() throws NotEvaluableNetworkException {
		checkNetworkApplicability();
		checkConstraintsApplicability();
	}

	/**
	 * Checks the network and evidence consistency
	 *
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	private void checkConsistency() throws NotEvaluableNetworkException {
		checkNetworkConsistency();
		checkEvidenceConsistency();
	}

	/**
	 * Checks network consistency
	 *
	 * @throws NotEvaluableNetworkException TODO - Implement that method
	 */
	private void checkNetworkConsistency() throws NotEvaluableNetworkException {

	}

	/**
	 * Checks evidence consistency
	 *
	 * @throws NotEvaluableNetworkException TODO - Implement that method
	 */
	private void checkEvidenceConsistency() throws NotEvaluableNetworkException {

	}

	/**
	 * Check if the network type can be evaluated by the algorithm
	 *
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	private void checkNetworkApplicability() throws NotEvaluableNetworkException {
		boolean isApplicable;

		List<NetworkType> networkTypes = getPossibleNetworkTypes();

		isApplicable = false;
		NetworkType networkType = probNet.getNetworkType();
		// Check that there is a network type applicable equal to type of probNet
		for (int i = 0; (i < networkTypes.size()) && !isApplicable; i++) {
			isApplicable = networkType == networkTypes.get(i);
		}

		if (!isApplicable) {
			throw new NotEvaluableNetworkException(
					"This algorithm cannot evaluate this network because" + "the network is of type " + networkType
							.toString() + ".");
		}
	}

	/**
	 * List of networks that the algorithm can evaluate
	 *
	 * @return List of evaluable networks
	 */
	protected abstract List<NetworkType> getPossibleNetworkTypes();

	/**
	 * Check if the network satisfies all the constraints that requires the algorithm
	 *
	 * @throws NotEvaluableNetworkException TODO - Remove additional constraints
	 */
	private void checkConstraintsApplicability() throws NotEvaluableNetworkException {
		// Check that the probNet satisfies the specific constraints of the algorithm
		List<PNConstraint> additionalConstraints = getAdditionalConstraints();

		List<PNConstraint> notEvaluableConstraints = new ArrayList<>();

		/// [Iago] If there is no constraints, don't execute the next chunk of code.
        /// Without the if, HuginPropagation breaks with a NullPointerException
		if (additionalConstraints != null) {
			for (PNConstraint pnConstraint : additionalConstraints) {
				if (!pnConstraint.checkProbNet(probNet)) {
					/*
					 * if (pnConstraint.getClass().equals(NoSuperValueNode.class)) { throw new
					 * NotEvaluableNetworkException("Evaluation of supervalue nodes is temporarily disabled."
					 * ); }
					 */
					notEvaluableConstraints.add(pnConstraint);
				}
			}

			if (notEvaluableConstraints.size() != 0) {
				String notEvaluableMessage = "This algorithm cannot evaluate this network because the network does "
						+ "not satisfy the following constraints:\n";
				for (PNConstraint pnConstraint : notEvaluableConstraints) {
					notEvaluableMessage += pnConstraint.toString() + "\n";
				}
				throw new NotEvaluableNetworkException(notEvaluableMessage);
			}
		}

	}

	/**
	 * List of additional constraints that network must satisfy in order to be evaluated by the algorithm
	 *
	 * @return List of additional constraints
	 */
	protected abstract List<PNConstraint> getAdditionalConstraints();

	/**
	 * @return The pre-resolution evidence
	 */
	public EvidenceCase getPreResolutionEvidence() {
		return this.preResolutionEvidence;
	}

	/**
	 * @param preResolutionEvidence The pre-resolution evidence to set
	 */
	public void setPreResolutionEvidence(EvidenceCase preResolutionEvidence) throws IncompatibleEvidenceException {
		if (preResolutionEvidence != null) {
			this.preResolutionEvidence = new EvidenceCase(preResolutionEvidence);
		}
	}

	/**
	 * @return The conditioning variables
	 */
	public List<Variable> getConditioningVariables() {
		return conditioningVariables;
	}

	/**
	 * @param conditioningVariables The conditioning variables to set
	 */
	public void setConditioningVariables(List<Variable> conditioningVariables) {
		if (conditioningVariables != null) {
			this.conditioningVariables = conditioningVariables;
		}
	}
}