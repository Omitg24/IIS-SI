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

import org.openmarkov.core.action.PNESupport;
import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.NormalizeNullVectorException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceAlgorithm;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.inference.tasks.Propagation;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.EvidencePotentials;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.type.BayesianNetworkType;
import org.openmarkov.inference.heuristic.minimalFillIn.MinimalFillIn;

/**
 * This abstract class defines the basic operations to create a
 * <code>ClusterForest</code> given a <code>ProbNet</code> and to obtain the
 * individual and join probabilities of a set of variables.
 *
 * @author marias
 * @author fjdiez
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public abstract class ClusterPropagation extends InferenceAlgorithm implements Propagation {
	// Attributes
	protected ClusterForest clusterForest;
	/**
	 * Indicates next node to eliminate when compiling the net.
	 */
	protected EliminationHeuristic heuristic;
	protected boolean netCompiled;
	protected boolean isEvidencePropagated;
	protected EvidenceCase evidence = new EvidenceCase();
	/**
	 * Indicates the amount of intermediate results stored by the propagation
	 * algorithm
	 */
	protected StorageLevel storageLevel = StorageLevel.MEDIUM;
	private EvidenceCase postResolutionEvidence = new EvidenceCase();
	/**
	 * @param probNet <code>ProbNet</code>.
	 * @throws NotEvaluableNetworkException notEvaluableNetworkException
	 */
	public ClusterPropagation(ProbNet probNet) throws NotEvaluableNetworkException {
		super(probNet);
		this.probNet = probNet.copy();
		PNESupport pNESupport = probNet.getPNESupport();
		if (pNESupport == null) {
			pNESupport = new PNESupport(false);
		}
		this.pNESupport = pNESupport;
		netCompiled = false;
		isEvidencePropagated = false;
	}

	/**
	 * @param network network
	 * @param evidence evidence
	 * @return markovNetworkInference
	 * @throws IncompatibleEvidenceException incompatibleEvidenceException
	 */
	public static ProbNet projectTablesAndBuildMarkovDecisionNetwork(ProbNet network, EvidenceCase evidence)
			throws IncompatibleEvidenceException {
		ProbNet markovNetworkInference = null;
		List<TablePotential> returnedProjectedPotentials;

		try {
			returnedProjectedPotentials = network.tableProjectPotentials(evidence);
		} catch (NonProjectablePotentialException | WrongCriterionException e1) {
			throw new IncompatibleEvidenceException("Unexpected inference exception :" + e1.getMessage());
		}
		List<TablePotential> projectedPotentials = new ArrayList<>();

		for (TablePotential potential : returnedProjectedPotentials) {

			if (potential.getVariables().size() != 0) {
				projectedPotentials.add(potential);
			} else if (potential.isAdditive()) {
				// It is a utility potential
				if (potential.values[0] != 0) {
					projectedPotentials.add(potential);
				}
			} else {
				// It is a probability potential
				if (potential.values[0] != 1) {
					projectedPotentials.add(potential);
				}
			}

		}

		markovNetworkInference = network.buildMarkovDecisionNetwork(projectedPotentials);

		return markovNetworkInference;
	}

	// Constructor

	public EvidenceCase getPostResolutionEvidence() {
		return postResolutionEvidence;
	}

	// Methods

	public void setPostResolutionEvidence(EvidenceCase postResolutionEvidence) {
        this.postResolutionEvidence = postResolutionEvidence;
        try {
            updateEvidence();
        } catch (IncompatibleEvidenceException e) {
            e.printStackTrace();
        }
	}



	/**
	 * Creates a ClusterForest given a MarkovNetwork and a query (a set of
	 * variables of interest).
	 *
	 * @param markovNet <code>ProbNet</code>.
	 * @param heuristic <code>EliminationHeuristic</code>.
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws ConstraintViolationException ConstraintViolationException
	 * @throws DoEditException DoEditException
	 */
	protected abstract ClusterForest createForest(ProbNet markovNet, EliminationHeuristic heuristic,
			List<Node> queryNodes)
			throws ConstraintViolationException, NonProjectablePotentialException,
			WrongCriterionException, DoEditException;

	/**
	 * Creates a <code>ClusterForest</code> given a <code>MarkovNet</code>.
	 *
	 * @param markovNet <code>ProbNet</code>.
	 * @param heuristic <code>EliminationHeuristic</code>.
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws DoEditException DoEditException
	 */
	protected abstract ClusterForest createForest(ProbNet markovNet, EliminationHeuristic heuristic)
			throws DoEditException, NonProjectablePotentialException, WrongCriterionException;

	public boolean isEvaluable(ProbNet probNet) {
		return probNet.getNetworkType().equals(BayesianNetworkType.getUniqueInstance());
	}

	public TablePotential getGlobalUtility() throws IncompatibleEvidenceException {
		// TODO Auto-generated method stub
		return null;
	}

	public HashMap<Variable, TablePotential> getPosteriorValues() throws IncompatibleEvidenceException {
		return getPosteriorValues(probNet.getVariables());
	}

	/**
	 * Creates a <code>ClusterForest</code>, introduce the evidence and for each
	 * root cluster collects the evidence and distribute the evidence.
	 *
	 * @param variablesOfInterest variablesOfInterest
	 * @return A <code>HashMap</code> with a potential for each variable
	 * identified by the variable name
	 */
	public HashMap<Variable, TablePotential> getPosteriorValues(List<Variable> variablesOfInterest)
			throws IncompatibleEvidenceException {
		// to be returned
		HashMap<Variable, TablePotential> individualProbabilities = new HashMap<>();

		if (!netCompiled) {
			compilePriorPotentials();
		}
		if (!isEvidencePropagated) {
			// propagates evidence
			propagateProbabilities();
		}
		// gets the posterior probability of each variable
		List<Variable> variablesNoEvidence = new ArrayList<Variable>(variablesOfInterest);
		variablesNoEvidence.removeAll(evidence.getVariables());
		for (Variable variable : variablesOfInterest) {
			ClusterOfVariables cluster = clusterForest.getCluster(variable);
			List<Variable> variablesToKeep = new ArrayList<Variable>(1);
			variablesToKeep.add(variable);
			individualProbabilities.put(variable, (TablePotential) DiscretePotentialOperations
					.marginalize(cluster.getPosteriorPotential(storageLevel), variablesToKeep));
		}
		// Normalize potentials in individualProbabilities
		for (Variable variable : variablesNoEvidence) {
			try {
				individualProbabilities
						.put(variable, DiscretePotentialOperations.normalize(individualProbabilities.get(variable)));
			} catch (NormalizeNullVectorException e) {
				throw new IncompatibleEvidenceException("Incompatible evidence");
			}
		}
		return EvidencePotentials.addEvidencePotentials(individualProbabilities, variablesOfInterest, evidence);
	}

	@Override public void setPreResolutionEvidence(EvidenceCase preResolutionEvidence) throws IncompatibleEvidenceException {
		super.setPreResolutionEvidence(preResolutionEvidence);
		updateEvidence();
	}

	/**
	 * /**
	 * Creates the <code>clusterForest</code>, introduces evidence and calls
	 * <code>collectEvidence</code> in all the root clusters
	 *
	 * @param variables <code>ArrayList</code> of <code>Variable</code>
	 * @return One marginalized potential with the
	 * <code>variablesOfInterest</code> join probability table.
	 * <code>Potential</code>
	 */
	public TablePotential getJointProbability(List<Variable> variables) {
		if (!netCompiled) {
			compilePriorPotentials();
		}
		if (!isEvidencePropagated) {
			propagateProbabilities();
		}
		ClusterOfVariables queryCluster = getQueryCluster(clusterForest, variables);
		TablePotential jointProbability = (TablePotential) DiscretePotentialOperations
				.marginalize(queryCluster.getPosteriorPotential(storageLevel), variables);
		// TODO Investigate why at this point the potential's role is CONDITIONAL PROBABILITY
		jointProbability.setPotentialRole(PotentialRole.JOINT_PROBABILITY);
		try {
			jointProbability = DiscretePotentialOperations.normalize(jointProbability);
		} catch (NormalizeNullVectorException e) {
			e.printStackTrace();
		}
		return jointProbability;
	}

	/**
	 * Looks for the cluster that contains all the <code>queryVariables</code>
	 *
	 * @param clusterForest  <code>ClusterForest</code>
	 * @param queryVariables <code>ArrayList</code> of <code>Variable</code>
	 * @return A <code>ClusterOfVariables</code>
	 */
	protected ClusterOfVariables getQueryCluster(ClusterForest clusterForest, List<Variable> queryVariables) {
		// Brute force algorithm
		int numQueryVariables = queryVariables.size();
		for (ClusterOfVariables cluster : clusterForest.getNodes()) {
			List<Variable> clusterVariables = cluster.getVariables();
			if ((clusterVariables.size() >= numQueryVariables) && (clusterVariables.containsAll(queryVariables))) {
				return cluster;
			}
		}
		return null;
	}

	private void updateEvidence() throws IncompatibleEvidenceException {
		evidence = joinPreAndPostResolutionEvidence();

		if (!netCompiled) {
			compilePriorPotentials();
		}
		introduceEvidence(evidence);
		isEvidencePropagated = false;
	}

	private void propagateProbabilities() {
		for (ClusterOfVariables cluster : clusterForest.getRootClusters()) {
			// collects the evidence and assigns the resulting potential
			// as the posterior potential of this root cluster
			TablePotential collectedEvidence = cluster.collectEvidence(storageLevel);
			cluster.setPosteriorPotential(collectedEvidence);
			cluster.distributeEvidence(storageLevel);
		}
		isEvidencePropagated = true;
	}

	private EvidenceCase joinPreAndPostResolutionEvidence() throws IncompatibleEvidenceException {
		EvidenceCase evidence = new EvidenceCase(getPreResolutionEvidence());
		try {
			evidence.addFindings(getPostResolutionEvidence().getFindings());
		} catch (InvalidStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return evidence;
	}

	/**
	 * For each <code>Finding</code> in the <code>EvidenceCase</code> gets the
	 * <code>Potential</code> associated to the probability and insert this in
	 * the <code>ClusterOfVariables</code> associated to the finding variable
	 *
	 * @param evidenceCase <code>EvidenceCase</code>.
 	 */
	private void introduceEvidence(EvidenceCase evidenceCase) {
		// gets the evidence in an ArrayList<Potential>
		List<Finding> findings = null;
		if (evidenceCase != null) {
			findings = evidenceCase.getFindings();
		}
		if ((findings != null) && (findings.size() > 0)) {
			ArrayList<TablePotential> evidencePotentials = new ArrayList<TablePotential>();
			for (Finding finding : findings) {
				// Role = JOIN_PROBABILITY only for Bayesian Networks
				try {
					evidencePotentials.add(finding.getVariable().deltaTablePotential(finding.getState()));
				} catch (InvalidStateException e) {
					// Can not happen
				}
			}
			// inserts the evidence
			for (TablePotential potential : evidencePotentials) {
				// each potential has only one variable in the 0 position
				Variable variable = potential.getVariables().get(0);
				// selects a cluster containing the variable
				ClusterOfVariables cluster = clusterForest.getCluster(variable);
				cluster.addEvidencePotential(potential);
			}
		}
	}

	/**
	 * @return storageLevel <code>StorageLevel</code>.
	 */
	public StorageLevel getStorageLevel() {
		return storageLevel;
	}

	/**
	 * @param storageLevel <code>StorageLevel</code>.
	 */
	public void setStorageLevel(StorageLevel storageLevel) {
		this.storageLevel = storageLevel;
	}

	/**
	 * Creates a <code>ClusterForest</code> given the potentials stored in the
	 * <code>probNet</code>
	 */
	public void compilePriorPotentials() {
		try {
			//            ProbNet markovNet = probNet.getMarkovDecisionNetwork();
			// TODO -FIX!!!
			ProbNet markovNet = projectTablesAndBuildMarkovDecisionNetwork(probNet, null);
			heuristic = heuristicFactory(markovNet);
			clusterForest = createForest(markovNet, heuristic);
			// Multiply prior potentials in each clique to form one prior potential
			for (ClusterOfVariables rootCluster : clusterForest.getRootClusters()) {
				rootCluster.compilePriorPotentials();
			}
			netCompiled = true;
		} catch (DoEditException | NonProjectablePotentialException | WrongCriterionException e) {
			e.printStackTrace();
		} catch (IncompatibleEvidenceException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates an heuristic associated to <code>network</code>
	 *
	 * @param markovNetwork <code>MarkovDecisionNetwork</code>
	 * @return <code>EliminationHeuristic</code>
	 */
	private EliminationHeuristic heuristicFactory(ProbNet markovNetwork) {
		List<List<Variable>> variables;
		variables = new ArrayList<>();
		variables.add(markovNetwork.getChanceAndDecisionVariables());
		EliminationHeuristic heuristic = null;
		heuristic = new MinimalFillIn(markovNetwork, variables);
		return heuristic;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer(this.getClass().getSimpleName() + "\n");
		buffer.append("Storage level: " + storageLevel + "\n");
		/*
		 * if (heuristic != null) { buffer.append("Elimination heuristic: " +
		 * heuristic.getClass().getSimpleName() + ". "); } else {
		 * buffer.append("No elimination heuristic. "); }
		 */
		if (netCompiled) {
			buffer.append("Net compiled.");
		} else {
			buffer.append("Net not compiled.");
		}
		return buffer.toString();
	}

	public Potential getOptimizedPolicy(Variable decisionVariable)
			throws IncompatibleEvidenceException, UnexpectedInferenceException {
		// TODO Auto-generated method stub
		return null;
	}

	public Potential getExpectedUtilities(Variable decisionVariable)
			throws IncompatibleEvidenceException, UnexpectedInferenceException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Indicates the amount of intermediate results stored by the propagation
	 * algorithm
	 */
	public enum StorageLevel {
		NO_STORAGE, // No storage
		MEDIUM, // Medium storage = up going messages
		FULL // Maximum storage = up going messages and posterior potentials.
	}
}
