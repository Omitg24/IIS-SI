//package org.openmarkov.inference.variableElimination.tasks;
//
//import org.openmarkov.core.exception.IncompatibleEvidenceException;
//import org.openmarkov.core.exception.NotEvaluableNetworkException;
//import org.openmarkov.core.exception.UnexpectedInferenceException;
//import org.openmarkov.core.inference.heuristic.HeuristicFactory;
//import org.openmarkov.core.inference.tasks.OptimalPolicies;
//import org.openmarkov.core.model.network.ProbNet;
//import org.openmarkov.core.model.network.ProbNetOperations;
//import org.openmarkov.core.model.network.Variable;
//import org.openmarkov.core.model.network.constraint.NoMixedParents;
//import org.openmarkov.core.model.network.constraint.NoSuperValueNode;
//import org.openmarkov.core.model.network.constraint.PNConstraint;
//import org.openmarkov.core.model.network.potential.TablePotential;
//import org.openmarkov.core.model.network.type.BayesianNetworkType;
//import org.openmarkov.core.model.network.type.InfluenceDiagramType;
//import org.openmarkov.core.model.network.type.MIDType;
//import org.openmarkov.core.model.network.type.NetworkType;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * @author jperez-martin
// */
//public class VEOptimalPolicies implements OptimalPolicies {
//
//    VEEvaluation veEvaluation;
//
//    Variable decision;
//
//    private ProbNet probNet;
//    /**
//     * @param network Probabilistic network to be resolved
//     * @param decisionVariable The variable for which the optimal policy is required
//     *
//     * @throws NotEvaluableNetworkException
//     *             Constructor
//     * @throws UnexpectedInferenceException
//     * @throws IncompatibleEvidenceException
//     */
//    public VEOptimalPolicies(ProbNet network, Variable decisionVariable)
//            throws NotEvaluableNetworkException, IncompatibleEvidenceException, UnexpectedInferenceException {
//        probNet = network.copy();
//        this.decision = decisionVariable;
//
//        List<Variable> informationalPredecesors = ProbNetOperations.getInformationalPredecessors(
//                probNet,decisionVariable);
//        informationalPredecesors.remove(decisionVariable);
//
//        veEvaluation = new VEEvaluation(network);
//        veEvaluation.setConditioningVariables(informationalPredecesors);
//    }
//
//    @Override
//    public TablePotential getOptimalPolicy() throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException {
//        return (TablePotential) veEvaluation.getOptimalPolicy(decision);
//    }
//
//    @Override
//    public void setHeuristicFactory(HeuristicFactory heuristicFactory) {
//
//    }
//
//    @Override
//    public void checkNetworkConsistency(ProbNet probNet) throws NotEvaluableNetworkException {
//
//    }
//
//    /**
//     * @return A new <code>ArrayList</code> of <code>PNConstraint</code>.
//     */
//    public List<PNConstraint> initializeAdditionalConstraints() {
//        List<PNConstraint> constraints = new ArrayList<>();
//        constraints.add(new NoMixedParents());
//        constraints.add(new NoSuperValueNode());
//        return constraints;
//    }
//
//    /**
//     * @return An <code>ArrayList</code> of <code>NetworkType</code> where the
//     *         algorithm can be applied: Bayesian networks and influence
//     *         diagrams.
//     */
//    public List<NetworkType> initializeNetworkTypesApplicable() {
//        List<NetworkType> networkTypes = new ArrayList<>();
//        networkTypes.add(BayesianNetworkType.getUniqueInstance());
//        networkTypes.add(InfluenceDiagramType.getUniqueInstance());
//        networkTypes.add(MIDType.getUniqueInstance());
//        return networkTypes;
//    }
//
//    @Override
//    public boolean checkEvidenceConsistency() {
//        return false;
//    }
//
//    @Override
//    public boolean checkPoliciesConsistency() {
//        return false;
//    }
//}
