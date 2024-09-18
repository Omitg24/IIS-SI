/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.huginPropagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.annotation.InferenceAnnotation;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.inference.tasks.Propagation;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.PNConstraint;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.type.BayesianNetworkType;
import org.openmarkov.core.model.network.type.NetworkType;

/**
 * Create objects for Hugin algorithm inference. This class is nearly empty
 * because the algorithm is implemented in <code>HuginForest</code> construction
 * and in <code>openmarkov.inference.ClusterPropagation</code>.
 *
 * @author marias
 * @author fjdiez
 * @version 1.0
 * @since OpenMarkov 1.0
 */
@InferenceAnnotation(name = "HuginPropagation") public class HuginPropagation extends ClusterPropagation implements Propagation {
	// Constructor

	/**
	 * @param probNet <code>ProbNet</code>
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	public HuginPropagation(ProbNet probNet) throws NotEvaluableNetworkException {
		super(probNet);
	}

	/// Is it deprecated?
	public static void checkEvaluability(ProbNet probNet) throws NotEvaluableNetworkException {
		if (!probNet.getNetworkType().equals(BayesianNetworkType.getUniqueInstance()))
			throw new NotEvaluableNetworkException("Hugin propagation can currently only evaluate Bayesian networks.");
	}

	/// Why doesn't HuginPropagation needs to implement setPostResolutionEvidence from Propagation

	/// Proposal of implementation
	@Override protected List<NetworkType> getPossibleNetworkTypes() {
		ArrayList<NetworkType> possibleNetworkTypes = new ArrayList<>();
		possibleNetworkTypes.add(BayesianNetworkType.getUniqueInstance());
		return possibleNetworkTypes;
	}


	@Override protected List<PNConstraint> getAdditionalConstraints() {
		return null;
	}

	// Methods from clusterPropagation
	@Override
	/** Creates a HuginForest for a Markov network having a root clique
	 *   containing all the<code>queryNodes</code>.
	 * @param markovNet <code>ProbNet</code> This Markov network will be
	 *   destroyed.
	 * @param heuristic <code>EliminationHeuristic</code>
	 * @param queryNodes <code>ArrayList</code> of <code>Node</code>
	 * @param pNESupport <code>PNESupport</code>
	 * @return A <code>HuginForest</code> */ protected ClusterForest createForest(ProbNet markovNet,
			EliminationHeuristic heuristic, List<Node> queryNodes) // [Iago] maybe name queryNodes as variablesOfInterest
			throws ConstraintViolationException, NonProjectablePotentialException,
			WrongCriterionException, DoEditException {
		return new HuginForest(markovNet, heuristic, queryNodes);
	}

	@Override
	/** Creates a HuginForest from a MarkovNet having a rootClique containing
	 * all the<code>queryNodes</code>.
	 * @return A <code>HuginForest</code>
	 * @param markovNet <code>ProbNet</code> This Markov network will be
	 *   destroyed.
	 * @param heuristic <code>EliminationHeuristic</code> */ protected ClusterForest createForest(ProbNet markovNet,
			EliminationHeuristic heuristic)
			throws DoEditException, NonProjectablePotentialException, WrongCriterionException {
		return new HuginForest(markovNet, heuristic);
	}

	/**
	 * Creates a HuginForest for a MarkovNet.
	 *
	 * @param markovNet <code>ProbNet</code> This Markov network will be
	 *                  destroyed.
	 * @return A <code>HuginForest</code>
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws DoEditException DoEditException
	 */
	protected ClusterForest createForest(ProbNet markovNet)
			throws DoEditException, NonProjectablePotentialException, WrongCriterionException {
		return new HuginForest(markovNet, heuristic);
	}

	/// No optimal strategy for an algorithm that only works in BayesianNets. There is no specific Exception
	public StrategyTree getOptimalStrategy() throws IncompatibleEvidenceException, UnexpectedInferenceException {
		// TODO Auto-generated method stub. Delete?
		// throw new NotImplementedException();
		return null;
	}

	@Override
	public HashMap<Variable, TablePotential> getPosteriorValues() throws IncompatibleEvidenceException {
		return super.getPosteriorValues();
	}

	@Override
	public void setVariablesOfInterest(List<Variable> variablesOfInterest) {

	}
}
