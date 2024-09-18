/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmarkov.core.exception.DivideByZeroException;
import org.openmarkov.core.exception.IllegalArgumentTypeException;
import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.NormalizeNullVectorException;
import org.openmarkov.core.exception.PotentialOperationException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.Choice;
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.modelUncertainty.UncertainValue;
import org.openmarkov.core.model.network.potential.AugmentedTable;
import org.openmarkov.core.model.network.potential.AugmentedTablePotential;
import org.openmarkov.core.model.network.potential.FunctionPotential;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.UnivariateDistrPotential;

import net.sourceforge.jeval.EvaluationException;

/**
 * This class defines a set of common operations over discrete potentials (
 * {@code TablePotential}s) and discrete variables ({@code Variable}
 * s). The method are invoked from {@code PotentialOperations} after
 * checking that the parameters are discrete.
 *
 * @author Manuel Arias
 */
public final class DiscretePotentialOperations {

	// Strings that represent possible causes of exception launch in merge operation
	private final static String nullVariable = "decision variable = null";
	private final static String nullPotentials = "potentials = null";
	private final static String noPotentials = "zero potentials";
	/**
	 * Round error used to compare two numbers. If they differ in less than
	 * {@code maxRoundErrorAllowed} they will be considered equals.
	 */
	public static double maxRoundErrorAllowed = 1E-8;

	/**
	 * @param tablePotentials {@code ArrayList} of extends {@code Potential}.
	 * @return A {@code TablePotential} as result.
	 */
	public static TablePotential multiply(List<TablePotential> tablePotentials) {
		return multiply(tablePotentials, true);
	}

	/**
	 * @param potentials {@code ArrayList} of extends {@code Potential}.
	 * @return A {@code TablePotential} as result.
	 */
	public static TablePotential multiply(TablePotential... potentials) {
		List<TablePotential> potentialsToMultiply;
		potentialsToMultiply = new ArrayList<>();
		for (TablePotential potential : potentials) {
			potentialsToMultiply.add(potential);
		}
		return multiply(potentialsToMultiply);
	}

	/**
	 * @param tablePotentials {@code ArrayList} of extends {@code Potential}.
	 * @param reorder         Sorts or not the potentials prior to multiplication.
	 *                        {@code boolean}.
	 * @return A {@code TablePotential} as result.
	 * //TODO
	 */
	public static TablePotential multiply(List<TablePotential> tablePotentials, boolean reorder) {
		int numPotentials = tablePotentials.size();

		// Special cases: one or zero potentials
		if (numPotentials < 2) {
			if (numPotentials == 1) {
				return (TablePotential) tablePotentials.get(0);
			} else {
				return null;
			}
		}
		//Find out if some potential has criterion. In that case, set that criterion in the resulting potential
		Criterion criterion = findFirstNonNullCriterion(tablePotentials);

		List<TablePotential> potentials = new ArrayList<>(tablePotentials);

		// Sort the potentials according to the table size
		if (reorder) {
			Collections.sort(potentials);
		}

		// Gets constant factor: The product of constant potentials
		double constantFactor = getConstantFactor(potentials);

		// get role
		PotentialRole role = getRole(potentials);

		potentials = AuxiliaryOperations.getNonConstantPotentials(potentials);
		if (potentials.size() == 0) {
			return buildConstantPotential(constantFactor, role);
		}

		// Gets the union
		List<Variable> resultVariables = AuxiliaryOperations.getUnionVariables(potentials);

		int numVariables = resultVariables.size();

		// Gets the tables of each TablePotential
		numPotentials = potentials.size();
		double[][] tables = initializeFromValues(potentials);

		// Gets dimension
		int[] resultDimension = TablePotential.calculateDimensions(resultVariables);

		// Gets offset accumulate
		int[][] offsetAccumulate = DiscretePotentialOperations.getAccumulatedOffsets(potentials, resultVariables);

		// Gets coordinate
		int[] resultCoordinate = initializeCoordinates(numVariables);

		// Position in each table potential
		int[] potentialsPositions = initializeToZero(numPotentials);

		// Multiply
		int incrementedVariable = 0;

		int[] dimensions = TablePotential.calculateDimensions(resultVariables);
		int[] offsets = TablePotential.calculateOffsets(dimensions);
		int tableSize = numVariables > 0 ? dimensions[numVariables - 1] * offsets[numVariables - 1] : 1;
		double[] resultValues = new double[tableSize];

		TablePotential potentialWithInterventions = findFirstPotentialWithInterventions(tablePotentials);
		boolean thereAreInterventions = (potentialWithInterventions != null);
		StrategyTree[] resultStrategyTrees = null;
		StrategyTree strategyTree = null;
		StrategyTree[] inputStrategyTrees = null;
		if (thereAreInterventions) {
			inputStrategyTrees = potentialWithInterventions.strategyTrees;
			resultStrategyTrees = new StrategyTree[tableSize];
			if (potentialWithInterventions.getVariables().size() == 0) {
				// The interventions are in a constant potential
				strategyTree = inputStrategyTrees[0];
			}
		}

		int indexPotentialWithInterventions = potentials.indexOf(potentialWithInterventions);

		for (int resultPosition = 0; resultPosition < tableSize; resultPosition++) {
			double mulResult = constantFactor;

			//increment the result coordinate and find out which variable is to be incremented
			for (int iVariable = 0; iVariable < resultCoordinate.length; iVariable++) {
				// try by incrementing the current variable (given by iVariable)
				resultCoordinate[iVariable]++;
				if (resultCoordinate[iVariable] != resultDimension[iVariable]) {
					// we have incremented the right variable
					incrementedVariable = iVariable;
					// do not increment other variables;
					break;
				}
				/*
				 * this variable could not be incremented; we set it to 0 in
				 * resultCoordinate (the next iteration of the for-loop will
				 * increment the next variable)
				 */
				resultCoordinate[iVariable] = 0;
			}

			// multiply
			for (int iPotential = 0; iPotential < numPotentials; iPotential++) {
				// multiply the numbers
				mulResult = mulResult * tables[iPotential][potentialsPositions[iPotential]];
				//Obtain the intervention
				if (thereAreInterventions && indexPotentialWithInterventions == iPotential) {
					strategyTree = inputStrategyTrees[potentialsPositions[iPotential]];
				}
				// update the current position in each potential table
				potentialsPositions[iPotential] += offsetAccumulate[iPotential][incrementedVariable];
			}

			resultValues[resultPosition] = mulResult;
			if (thereAreInterventions) {
				resultStrategyTrees[resultPosition] = strategyTree;
			}

		}

		TablePotential resultPotential = buildResultPotential(criterion, role, resultVariables, resultValues,
				thereAreInterventions, resultStrategyTrees);
		return resultPotential;
	}

	private static Criterion findFirstNonNullCriterion(List<TablePotential> tablePotentials) {
		Criterion criterion = null;
		for (int i = 0; i < tablePotentials.size() && criterion == null; i++) {
			criterion = tablePotentials.get(i).getCriterion();
		}
		return criterion;
	}

	private static int[] initializeToZero(int numPotentials) {
		int[] potentialsPositions = new int[numPotentials];
		for (int i = 0; i < numPotentials; i++) {
			potentialsPositions[i] = 0;
		}
		return potentialsPositions;
	}

	/**
	 * @param tablePotentials List of TablePotential
	 * @return First potential with interventions; if any, null
	 */
	private static TablePotential findFirstPotentialWithInterventions(List<TablePotential> tablePotentials) {
		TablePotential potentialWithInterventions = null;

		// Find the potential with interventions
		boolean found = false;
		for (int i = 0; i < tablePotentials.size() && !found; i++) {
			TablePotential auxPotential = tablePotentials.get(i);
			if (auxPotential.strategyTrees != null) {
				potentialWithInterventions = auxPotential;
				found = true;
			}
		}
		return potentialWithInterventions;
	}

	//	/**
	//	 * @param potentials List of potentials with their criteria
	//	 * @return if all the potentials have the same criterion, that criterion; otherwise, <code>null</code>.
	//	 */
	//	private static Criterion getCommonDecisionCriterion(List<? extends Potential> potentials) {
	//		Criterion criteron, lastCriterion = null;
	//		int numPotentials = potentials.size();
	//		boolean existsSameCriterion = (numPotentials > 0) ?
	//				((potentials.get(0).getUtilityVariable() != null) ?
	//					(lastCriterion = potentials.get(0).getUtilityVariable().getDecisionCriterion()) != null
	//					:
	//					false)
	//				:
	//				false;
	//		for (int i = 1; i < numPotentials && existsSameCriterion; i++) {
	//			Potential potential = potentials.get(i);
	//			existsSameCriterion =
	//					potential.isUtility() &&
	//					potential.getUtilityVariable().getDecisionCriterion() == lastCriterion;
	//		}
	//		return existsSameCriterion ? lastCriterion : null;
	//	}

	/**
	 * @param tablePotentials {@code List} of {@code TablePotential}s.
	 * @return {@code TablePotential}
	 */
	public static TablePotential sum(List<TablePotential> tablePotentials) {
		if (tablePotentials == null || tablePotentials.size() == 0) {
			return new TablePotential(null, PotentialRole.CONDITIONAL_PROBABILITY, new double[] { 0.0 });
		}
		if (tablePotentials.size() == 1) {
			return tablePotentials.get(0);
		}

		// list of non-constant potentials
		List<TablePotential> potentials = new ArrayList<>(tablePotentials);

		// Leave out the constant potentials
		List<TablePotential> constantPotentials = new ArrayList<>();
		for (int i = 0; i < tablePotentials.size(); i++) {
			Potential auxPotential = tablePotentials.get(i);
			if (auxPotential.getVariables().size() == 0) {
				potentials.remove(auxPotential);
				constantPotentials.add((TablePotential) auxPotential);
			}
		}

		// Calculate the sum of constant potentials
		double sumConstantPotentials = 0.0;
		StrategyTree constantPotentialsStrategyTree = null;
		int numConstantPotentials = constantPotentials.size();
		for (int i = 0; i < numConstantPotentials; i++) {
			sumConstantPotentials += constantPotentials.get(i).values[0];
			StrategyTree[] iConstantPotentialStrategyTrees = constantPotentials.get(i).strategyTrees;
			if (iConstantPotentialStrategyTrees != null) {
				StrategyTree onlyStrategyTreeIConstantPotential = iConstantPotentialStrategyTrees[0];
				constantPotentialsStrategyTree = (constantPotentialsStrategyTree == null) ?
						onlyStrategyTreeIConstantPotential :
						constantPotentialsStrategyTree.concatenate(onlyStrategyTreeIConstantPotential);
			}
		}

		// From here operate only with non constant potentials
		int numPotentials = potentials.size();

		// Gets the union
		List<Variable> resultVariables = AuxiliaryOperations.getUnionVariables(potentials);
		int numVariables = resultVariables.size();

		double[][] tables = initializeFromValues(potentials);

		// Gets the interventions if necessary
		boolean thereAreInterventions = areThereInterventions(potentials);

		StrategyTree[][] strategyTrees = initializeFromStrategyTrees(potentials, thereAreInterventions);

		// Gets the dimensions
		int[] resultDimensions = TablePotential.calculateDimensions(resultVariables);

		// Gets the accumulated offsets
		int[][] accumulatedOffsets = getAccumulatedOffsets(potentials, resultVariables);

		// Gets the coordinates
		int[] resultCoordinates = initializeCoordinates(numVariables);

		// Position in each table potential
		int[] potentialsPositions = initializeToZero(numPotentials);

		// Sum
		int incrementedVariable = 0;
		boolean resultVariablesNotEmpty = !resultVariables.isEmpty();
		int[] dimensions = resultVariablesNotEmpty ? TablePotential.calculateDimensions(resultVariables) : new int[0];
		int[] offsets = resultVariablesNotEmpty ? TablePotential.calculateOffsets(dimensions) : new int[0];
		int tableSize = 1; // If numVariables == 0 the potential is a constant
		if (numVariables > 0) {
			tableSize = dimensions[numVariables - 1] * offsets[numVariables - 1];
		}
		double[] resultValues = new double[tableSize];
		StrategyTree[] resultStrategyTrees = (
				thereAreInterventions || constantPotentialsStrategyTree != null
		) ? new StrategyTree[tableSize] : null;

		if (potentials.size() > 0) {
			double sum;
			for (int resultPosition = 0; resultPosition < tableSize; resultPosition++) {
				/*
				 * increment the result coordinate and find out which variable
				 * is to be incremented
				 */
				for (int iVariable = 0; iVariable < resultCoordinates.length; iVariable++) {
					// try by incrementing the current variable (given by
					// iVariable)
					resultCoordinates[iVariable]++;
					if (resultCoordinates[iVariable] != resultDimensions[iVariable]) {
						// we have incremented the right variable
						incrementedVariable = iVariable;
						// do not increment other variables;
						break;
					}
					/*
					 * this variable could not be incremented; we set it to 0 in
					 * resultCoordinate (the next iteration of the for-loop will
					 * increment the next variable)
					 */
					resultCoordinates[iVariable] = 0;
				}

				// sum
				sum = 0;
				StrategyTree resultStrategyTree = null;
				for (int iPotential = 0; iPotential < numPotentials; iPotential++) {
					// sum the numbers
					sum = sum + tables[iPotential][potentialsPositions[iPotential]];
					if (thereAreInterventions && strategyTrees[iPotential] != null) {
						StrategyTree auxIStrategyTree = strategyTrees[iPotential][potentialsPositions[iPotential]];
						resultStrategyTree = (resultStrategyTree == null) ?
								auxIStrategyTree :
								resultStrategyTree.concatenate(auxIStrategyTree);
					}

					// update the current position in each potential table
					potentialsPositions[iPotential] += accumulatedOffsets[iPotential][incrementedVariable];
				}
				resultValues[resultPosition] = sum;
				if (thereAreInterventions) {
					resultStrategyTrees[resultPosition] = resultStrategyTree;
				}
			}
		}
		// Sum constant potentials to the result
		if ((numConstantPotentials > 0) && (sumConstantPotentials != 0.0 || constantPotentialsStrategyTree != null)) {
			for (int i = 0; i < resultValues.length; i++) {
				resultValues[i] = resultValues[i] + sumConstantPotentials;
				if (constantPotentialsStrategyTree != null) {
					if (resultStrategyTrees[i] == null) {
						resultStrategyTrees[i] = constantPotentialsStrategyTree;
					} else {
						resultStrategyTrees[i].concatenate(constantPotentialsStrategyTree);
					}
				}
			}
		}
		TablePotential result = new TablePotential(resultVariables, getRole(tablePotentials), resultValues);
		result.strategyTrees = resultStrategyTrees;
		if (!potentials.isEmpty()) {
			result.setCriterion(potentials.get(0).getCriterion());
		}

		return result;
	}

	private static StrategyTree[][] initializeFromStrategyTrees(List<TablePotential> potentials, 
			boolean thereAreInterventions) {
		int numPotentials = potentials.size();
		StrategyTree[][] strategyTrees = null;
		if (thereAreInterventions) {
			strategyTrees = new StrategyTree[numPotentials][];
			for (int i = 0; i < numPotentials; i++) {
				strategyTrees[i] = potentials.get(i).strategyTrees;
			}
		}
		return strategyTrees;
	}

	//	/** Given a collection of variables, creates a new variable whose name is the concatenation
	//     * of the names of the other variables. If there is only one, returns that one. If the collection is empty,
	//     * returns a new variable with name "U"
	//     * @param variables collection of variables
	//     * @return <code>Variable</code>
	//     */
	//    private static Variable composeVariable(Collection<Variable> variables) {
	//    	Variable finalVariable = null;
	//		String name = "";
	//		if (variables.isEmpty()) {
	//			name = "U";
	//			finalVariable = new Variable(name);
	//		} else {
	//			if (variables.size() > 1) {
	//				int i = 0;
	//				int size = variables.size();
	//				for (Variable variable : variables) {
	//					i++;
	//					name = name + variable.getName();
	//					if (i < size) {
	//						name = name + "-";
	//					}
	//				}
	//				finalVariable = new Variable(name);
	//			} else {
	//				for (Variable variable : variables) {
	//					finalVariable = variable;
	//				}
	//			}
	//		}
	//		return finalVariable;
	//    }
	//
	//	/**
	//	 * @param tablePotentials Collection of TablePotentials
	//	 * @return A new variable whose name is the concatenation of the names of the utility variables.
	//	 */
	//	private static Variable getNewUtilityVariable(Collection<TablePotential> tablePotentials) {
	//		Set<Variable> utilityVariables = new HashSet<>();
	//		for (TablePotential potential : tablePotentials) {
	//			if (potential.isUtility()) {
	//				Variable utilityVariable = potential.getUtilityVariable();
	//				if (utilityVariable != null) {
	//					utilityVariables.add(utilityVariable);
	//				}
	//			}
	//		}
	//		return composeVariable(utilityVariables);
	//	}

	//	/**
	//     * @param potentials. <code>TablePotential</code>
	//     * @return <code>true</code> when at least one potential has an array of interventions.
	//     */
	//    private static boolean anyPotentialWithInterventions(List<TablePotential> potentials) {
	//    	int i;
	//    	for (i = 0; i < potentials.size() && potentials.get(i).interventions == null; i++);
	//		return i < potentials.size();
	//	}
	//
	//	/**
	//	 * @param result. <code>TablePotential</code>
	//	 * @param allThePotentials. <code>List</code> of <code>TablePotential</code>
	//	 * @return result with the interventions. <code>TablePotential</code>
	//	 */
	//	private static TablePotential sumInterventions(TablePotential result, List<TablePotential> allThePotentials) {
	//		result.interventions = new Intervention[result.values.length];
	//		List<TablePotential> potentials = new ArrayList<TablePotential>();
	//		for (TablePotential potential : allThePotentials) {
	//			if (potential.interventions != null) {
	//				potentials.add(potential);
	//			}
	//		}
	//		int numPotentials = potentials.size();
	//
	//		// Gets the tables of each TablePotential
	//        Intervention[][] interventionTables = new Intervention[numPotentials][];
	//        for (int i = 0; i < numPotentials; i++) {
	//            interventionTables[i] = potentials.get(i).interventions;
	//        }
	//
	//        List<Variable> resultVariables = result.getVariables();
	//
	//        // Gets dimensions
	//        int[] resultDimensions = TablePotential.calculateDimensions(resultVariables);
	//
	//        // Gets accumulated offsets
	//        int[][] accumulatedOffsets = DiscretePotentialOperations.getAccumulatedOffsets(potentials,
	//                resultVariables);
	//
	//        int numVariables = resultVariables.size();
	//
	//        // Gets coordinate
	//        int[] resultCoordinates;
	//        if (numVariables != 0) {
	//            resultCoordinates = new int[numVariables];
	//        } else {
	//            resultCoordinates = new int[1];
	//            resultCoordinates[0] = 0;
	//        }
	//
	//        // Position in each table potential
	//        int[] potentialPositions = new int[numPotentials];
	//        for (int i = 0; i < numPotentials; i++) {
	//            potentialPositions[i] = 0;
	//        }
	//
	//        // Sum
	//        int incrementedVariable = 0;
	//        int[] dimensions = (!resultVariables.isEmpty()) ? TablePotential.calculateDimensions(resultVariables)
	//                : new int[0];
	//        int[] offsets = (!resultVariables.isEmpty()) ? TablePotential.calculateOffsets(dimensions)
	//                : new int[0];
	//        int tableSize = 1; // If numVariables == 0 the potential is a constant
	//        if (numVariables > 0) {
	//            tableSize = dimensions[numVariables - 1] * offsets[numVariables - 1];
	//        }
	//        Intervention[] resultInterventions = new Intervention[tableSize];
	//
	//        if (allThePotentials.size() > 0) {
	//            Intervention sum = null;
	//            for (int resultPosition = 0; resultPosition < tableSize; resultPosition++) {
	//                /*
	//                 * increment the result coordinate and find out which variable
	//                 * is to be incremented
	//                 */
	//                for (int iVariable = 0; iVariable < resultCoordinates.length; iVariable++) {
	//                    // try by incrementing the current variable (given by
	//                    // iVariable)
	//                    resultCoordinates[iVariable]++;
	//                    if (resultCoordinates[iVariable] != resultDimensions[iVariable]) {
	//                        // we have incremented the right variable
	//                        incrementedVariable = iVariable;
	//                        // do not increment other variables;
	//                        break;
	//                    }
	//                    /*
	//                     * this variable could not be incremented; we set it to 0 in
	//                     * resultCoordinate (the next iteration of the for-loop will
	//                     * increment the next variable)
	//                     */
	//                    resultCoordinates[iVariable] = 0;
	//                }
	//
	//                // sum
	//                sum = null;
	//                for (int iPotential = 0; iPotential < numPotentials; iPotential++) {
	//                    // sum the numbers
	//                	Intervention intervention = interventionTables[iPotential][potentialPositions[iPotential]];
	//                	if (sum == null) {
	//                		sum = intervention;
	//                	} else {
	//                		sum.concatenate(intervention);
	//                	}
	//                    // update the current position in each potential table
	//                    potentialPositions[iPotential] += accumulatedOffsets[iPotential][incrementedVariable];
	//                }
	//                resultInterventions[resultPosition] = sum;
	//            }
	//        }
	//
	//		return result;
	//	}

	/**
	 * @param potentials List of table potentials
	 * @return if there is at least one potential with interventions.
	 */
	private static boolean areThereInterventions(List<TablePotential> potentials) {
		return findFirstPotentialWithInterventions(potentials) != null;
	}

	/**
	 * @param utilityPotentials {@code List} of {@code TablePotential}s.
	 * @return A TablePotential for each criterion
	 */
	public static List<TablePotential> sumByCriterion(List<TablePotential> utilityPotentials) {
		// create the set of criteria
		Set<Criterion> criteria = new HashSet<>();
		for (TablePotential potential : utilityPotentials) {
			criteria.add(potential.getCriterion());
		}

		// create an empty list for each criterion
		Map<Criterion, List<TablePotential>> potentialsByCriterion = new HashMap<>();
		for (Criterion criterion : criteria) {
			List<TablePotential> criterionList = new ArrayList<>();
			potentialsByCriterion.put(criterion, criterionList);
		}

		// put each potential in its list
		for (TablePotential potential : utilityPotentials) {
			potentialsByCriterion.get(potential.getCriterion()).add(potential);
		}

		// sum the potentials for each criterion
		List<TablePotential> utilityPotentialsByCriterion = new ArrayList<>(criteria.size());
		for (Criterion criterion : criteria) {
			TablePotential outputUtilityPotentialByCriterion = DiscretePotentialOperations
					.sum(potentialsByCriterion.get(criterion));
			outputUtilityPotentialByCriterion.setCriterion(criterion);
			utilityPotentialsByCriterion.add(outputUtilityPotentialByCriterion);
		}
		return utilityPotentialsByCriterion;
	}

	private static int[] initializeCoordinates(int numVariables) {
		int[] resultCoordinates = new int[Math.max(1, numVariables)];
		if (numVariables == 0) {
			resultCoordinates[0] = 0;
		}
		return resultCoordinates;
	}

	public static TablePotential sum(TablePotential... tablePotentials) {
		List<TablePotential> potentialList = new ArrayList<>(tablePotentials.length);
		for (TablePotential potential : tablePotentials) {
			potentialList.add(potential);
		}
		return sum(potentialList);
	}

	/**
	 * @param potentials Collection of potentials
	 * @return The potential role
	 */
	public static PotentialRole getRole(Collection<? extends Potential> potentials) {
		PotentialRole role = PotentialRole.CONDITIONAL_PROBABILITY; // Default value
		boolean atLeastOneUtility = false;
		boolean atLeastOneJoinProb = false;
		for (Potential potential : potentials) {
			atLeastOneUtility = atLeastOneUtility || potential.isAdditive();
			atLeastOneJoinProb = atLeastOneJoinProb || potential.getPotentialRole() == PotentialRole.JOINT_PROBABILITY;
		}
		if (atLeastOneUtility) {
			role = PotentialRole.UNSPECIFIED;
		} else {
			if (atLeastOneJoinProb) {
				role = PotentialRole.JOINT_PROBABILITY;
			}
		}
		return role;
	}

	/**
	 * @param tablePotentials      array to multiply
	 * @param variablesToKeep      The set of variables that will appear in the resulting
	 *                             potential
	 * @param variablesToEliminate The set of variables eliminated by marginalization (in
	 *                             general, by summing out or maximizing)
	 * @return A {@code TablePotential} result of multiply and marginalize.
	 * Condition: variablesToKeep and variablesToEliminate are a partition of
	 * the union of the variables of the potential
	 */
	public static TablePotential multiplyAndMarginalize(Collection<TablePotential> tablePotentials,
			List<Variable> variablesToKeep, List<Variable> variablesToEliminate) {

		// Constant potentials are those that do not depend on any variables.
		// The product of all the constant potentials is the constant factor.
		double constantFactor = 1.0;
		// Non constant potentials are proper potentials.
		List<TablePotential> nonConstantPotentials = new ArrayList<>();
		for (TablePotential potential : tablePotentials) {
			if (potential.getNumVariables() != 0) {
				nonConstantPotentials.add(potential);
			} else {
				constantFactor *= potential.values[potential.getInitialPosition()];
			}
		}

		int numNonConstantPotentials = nonConstantPotentials.size();

		if (numNonConstantPotentials == 0) {
			TablePotential resultingPotential = new TablePotential(variablesToKeep, getRole(tablePotentials));
			resultingPotential.values[0] = constantFactor;
			return resultingPotential;
		}

		// variables in the resulting potential
		List<Variable> unionVariables = new ArrayList<>(variablesToEliminate);
		unionVariables.addAll(variablesToKeep);
		int numUnionVariables = unionVariables.size();

		// current coordinate in the resulting potential
		int[] unionCoordinate = new int[numUnionVariables];
		int[] unionDimensions = TablePotential.calculateDimensions(unionVariables);

		// Defines some arrays for the proper potentials...
		double[][] tables = new double[numNonConstantPotentials][];
		int[] initialPositions = new int[numNonConstantPotentials];
		int[] currentPositions = new int[numNonConstantPotentials];
		int[][] accumulatedOffsets = new int[numNonConstantPotentials][];
		// ... and initializes them
		for (int i = 0; i < numNonConstantPotentials; i++) {
			TablePotential potential = nonConstantPotentials.get(i);
			tables[i] = potential.values;
			initialPositions[i] = potential.getInitialPosition();
			currentPositions[i] = initialPositions[i];
			accumulatedOffsets[i] = TablePotential.getAccumulatedOffsets(unionVariables, potential.getVariables());
		}

		// The result size is the product of the dimensions of the
		// variables to keep
		int resultSize = TablePotential.computeTableSize(variablesToKeep);
		double[] resultValues = new double[resultSize];
		// The elimination size is the product of the dimensions of the
		// variables to eliminate
		int eliminationSize = 1;
		for (Variable variable : variablesToEliminate) {
			eliminationSize *= variable.getNumStates();
		}

		// Auxiliary variables for the nested loops
		double multiplicationResult; // product of the table values
		double accumulator; // in general, the sum or the maximum
		int increasedVariable = 0; // when computing the next configuration

		// outer iterations correspond to the variables to keep
		for (int outerIteration = 0; outerIteration < resultSize; outerIteration++) {
			// Inner iterations correspond to the variables to eliminate
			// accumulator summarizes the result of all inner iterations

			// first inner iteration
			multiplicationResult = constantFactor;
			for (int i = 0; i < numNonConstantPotentials; i++) {
				// multiply the numbers
				multiplicationResult *= tables[i][currentPositions[i]];
			}
			accumulator = multiplicationResult;

			// next inner iterations
			for (int innerIteration = 1; innerIteration < eliminationSize; innerIteration++) {

				// find the next configuration and the index of the
				// increased variable
				increasedVariable = findNextConfigurationAndIndexIncreasedVariable(unionDimensions, unionCoordinate,
						increasedVariable);

				// update the positions of the potentials we are multiplying
				for (int i = 0; i < numNonConstantPotentials; i++) {
					currentPositions[i] += accumulatedOffsets[i][increasedVariable];
				}

				// multiply the table values of the potentials
				multiplicationResult = constantFactor;
				for (int i = 0; i < numNonConstantPotentials; i++) {
					multiplicationResult *= tables[i][currentPositions[i]];
				}

				// update the accumulator (for this inner iteration)
				accumulator += multiplicationResult;
				// accumulator =
				// operator.combine(accumulator,multiplicationResult);

			} // end of inner iteration

			// when eliminationSize == 0 there is a multiplication without
			// marginalization but we must find the next configuration
			if (outerIteration < resultSize - 1) {
				// find the next configuration and the index of the
				// increased variable
				increasedVariable = findNextConfigurationAndIndexIncreasedVariable(unionDimensions, unionCoordinate,
						increasedVariable);

				// update the positions of the potentials we are multiplying
				for (int i = 0; i < numNonConstantPotentials; i++) {
					currentPositions[i] += accumulatedOffsets[i][increasedVariable];
				}
			}

			resultValues[outerIteration] = accumulator;

		} // end of outer iteration

		return new TablePotential(variablesToKeep, getRole(tablePotentials), resultValues);
	}

	/**
	 * @param probPotential       probability potential
	 * @param utilityPotential    utility potential
	 * @param variableToEliminate The set of variables eliminated by marginalization (in
	 *                            general, by summing out or maximizing)
	 * @return A {@code TablePotential} result of multiply and marginalize.
	 * Condition: variablesToKeep and variablesToEliminate are a partition of
	 * the union of the variables of the potential
	 */
	public static TablePotential multiplyAndMarginalize(TablePotential probPotential, TablePotential utilityPotential,
			Variable variableToEliminate) {
		// when the probability potential is a constant
		if (probPotential.getVariables().isEmpty()) {
			double prob = probPotential.values[0];
			if (prob == 1) {
				return utilityPotential;
			} else {
				TablePotential result = (TablePotential) utilityPotential.copy();
				for (int i = 0; i < result.values.length; i++) {
					result.values[i] *= prob;
				}
				return result;
			}
		}

		List<Variable> allVariables = probPotential.getVariables();
		for (Variable variable : utilityPotential.getVariables()) {
			if (!allVariables.contains(variable)) {
				allVariables.add(variable);
			}
		}
		List<Variable> variablesToKeep = new ArrayList<>(allVariables);
		variablesToKeep.remove(variableToEliminate);

		TablePotential resultPotential = new TablePotential(variablesToKeep, PotentialRole.UNSPECIFIED);
		boolean thereAreInterventions = (utilityPotential.strategyTrees != null);
		if (thereAreInterventions) {
			resultPotential.strategyTrees = new StrategyTree[resultPotential.values.length];
		}

		// current coordinate in the product potential
		int[] coordinates = new int[allVariables.size()];
		int[] dimensions = TablePotential.calculateDimensions(allVariables);

		int currentPositionProb = 0;
		int[] accumulatedOffsetsProb = TablePotential.getAccumulatedOffsets(allVariables, probPotential.getVariables());

		int currentPositionUtil = 0;
		int[] accumulatedOffsetsUtil = TablePotential
				.getAccumulatedOffsets(allVariables, utilityPotential.getVariables());

		// Auxiliary variables for the nested loops
		double accumulator;
		int increasedVariable = 0;
		double[] probValues = probPotential.values;
		double[] utilValues = utilityPotential.values;
		double[] probs = new double[variableToEliminate.getNumStates()];

		StrategyTree[] strategyTrees = new StrategyTree[variableToEliminate.getNumStates()];

		// each outer iteration corresponds to one configuration of the variables to keep
		for (int outerIteration = 0; outerIteration < resultPotential.values.length; outerIteration++) {
			accumulator = 0;

			for (int stateIndex = 0; stateIndex < variableToEliminate.getNumStates(); stateIndex++) {
				if (stateIndex != 0) {
					// find the next configuration and the index of the
					// increased variable
					increasedVariable = findNextConfigurationAndIndexIncreasedVariable(dimensions, coordinates,
							increasedVariable);

					currentPositionProb += accumulatedOffsetsProb[increasedVariable];
					currentPositionUtil += accumulatedOffsetsUtil[increasedVariable];
				}

				accumulator += probValues[currentPositionProb] * utilValues[currentPositionUtil];

				probs[stateIndex] = probValues[currentPositionProb];

				if (thereAreInterventions) {
					strategyTrees[stateIndex] = utilityPotential.strategyTrees[currentPositionUtil];
				}
			}

			resultPotential.values[outerIteration] = accumulator;

			if (thereAreInterventions) {
				resultPotential.strategyTrees[outerIteration] = StrategyTree
						.averageOfInterventions(variableToEliminate, probs, strategyTrees);
			}

			// when eliminationSize == 0 there is a multiplication without
			// marginalization but we must find the next configuration
			if (outerIteration < resultPotential.values.length - 1) {
				// find the next configuration and the index of the
				// increased variable
				increasedVariable = findNextConfigurationAndIndexIncreasedVariable(dimensions, coordinates,
						increasedVariable);

				currentPositionProb += accumulatedOffsetsProb[increasedVariable];
				currentPositionUtil += accumulatedOffsetsUtil[increasedVariable];
			}
		}
		resultPotential.setCriterion(utilityPotential.getCriterion());
		return resultPotential;
	}

	/**
	 * @param potentials          potentials array to multiply
	 * @param variablesOfInterest Set of variables that must be kept (although this set may
	 *                            contain some variables that are not in any potential)
	 *                            {@code potentials}
	 * @return The multiplied potentials
	 */
	public static TablePotential multiplyAndMarginalize(List<TablePotential> potentials,
			List<Variable> variablesOfInterest) {

		// Obtain parameters to invoke multiplyAndMarginalize
		// Union of the variables of the potential list
		List<Variable> unionVariables = AuxiliaryOperations.getUnionVariables(potentials);

		// Classify unionVariables in two possibles arrays
		List<Variable> variablesToKeep = new ArrayList<>();
		List<Variable> variablesToEliminate = new ArrayList<>();
		for (Variable variable : unionVariables) {
			if (variablesOfInterest.contains(variable)) {
				variablesToKeep.add(variable);
			} else {
				variablesToEliminate.add(variable);
			}
		}

		return DiscretePotentialOperations.multiplyAndMarginalize(potentials, variablesToKeep, variablesToEliminate);
	}

	/**
	 * @param potentials          {@code ArrayList} of {@code Potential}s to multiply.
	 * @param variableToEliminate {@code Variable}.
	 * @return result {@code Potential} multiplied without
	 * {@code variableToEliminate}
	 */
	public static TablePotential multiplyAndMarginalize(List<TablePotential> potentials, Variable variableToEliminate) {
		List<Variable> variablesToKeep = AuxiliaryOperations.getUnionVariables(potentials);
		variablesToKeep.remove(variableToEliminate);
		return multiplyAndMarginalize(potentials, variablesToKeep, Arrays.asList(variableToEliminate));
	}

	/**
	 * @param potential           {@code Potential} to marginalize
	 * @param variableToEliminate {@code Variable}
	 * @return Marginalized potential
	 */
	public static TablePotential marginalize(TablePotential potential, Variable variableToEliminate) {
		List<Variable> variablesToKeep = new ArrayList<>(potential.getVariables());
		variablesToKeep.remove(variableToEliminate);
		List<Variable> variablesToEliminate = new ArrayList<>();
		variablesToEliminate.add(variableToEliminate);
		List<TablePotential> potentials = new ArrayList<>();
		potentials.add(potential);
		return multiplyAndMarginalize(potentials, variablesToKeep, variablesToEliminate);
	}

	/**
	 * @param potential           potential
	 * @param variablesOfInterest list of variables of interest
	 * @return Marginalized potential
	 */
	public static TablePotential marginalize(TablePotential potential, List<Variable> variablesOfInterest) {
		// Obtain parameters to invoke multiplyAndMarginalize
		// Union of the variables of the potential list
		List<Variable> variables = potential.getVariables();

		List<Variable> variablesToKeep = new ArrayList<>();
		List<Variable> variablesToEliminate = new ArrayList<>();

		for (Variable variable : variables) {
			if (variablesOfInterest.contains(variable)) {
				variablesToKeep.add(variable);
			} else {
				variablesToEliminate.add(variable);
			}
		}

		List<TablePotential> potentials = new ArrayList<>();
		potentials.add(potential);

		return DiscretePotentialOperations.multiplyAndMarginalize(potentials, variablesToKeep, variablesToEliminate);
	}

	/**
	 * @param potential            that will be marginalized
	 * @param variablesToKeep      variables to keep
	 * @param variablesToEliminate variable to eliminate
	 * Condition: variablesToKeep + variablesToEliminate =
	 * potential.getVariables()
	 * Condition: variablesToKeep
	 * @return Marginalized potential
	 */
	public static Potential marginalize(TablePotential potential, List<Variable> variablesToKeep,
			List<Variable> variablesToEliminate) {
		List<TablePotential> potentials = new ArrayList<>();
		potentials.add(potential);
		return DiscretePotentialOperations.multiplyAndMarginalize(potentials, variablesToKeep, variablesToEliminate);
	}

	/**
	 * @param potentials An array of ordered {@code TablePotential}s
	 * @return constantFactor: The product of the constant potentials (the first
	 * <i>k</i> because the array is ordered by size)
	 * @see org.openmarkov.core.model.network.potential.operation.AuxiliaryOperations#getNonConstantPotentials(Collection)
	 */
	public static double getConstantFactor(List<TablePotential> potentials) {
		double constantFactor = 1.0;
		for (TablePotential potential : potentials) {
			if (potential.values.length == 1) {
				constantFactor *= potential.values[0];
			}
		}
		return constantFactor;
	}

	/**
	 * Compute the accumulated offsets of a {@code Potential}s array with
	 * the order imposed by {@code potentialResult}
	 *
	 * @param potentials      {@code ArrayList} of {@code Potential}s.
	 * @param potentialResult {@code TablePotential}.
	 * @return An array of arrays of integers ({@code int[][]}).
	 */
	public static int[][] getAccumulatedOffsets(List<TablePotential> potentials, TablePotential potentialResult) {

		int numPotentials = potentials.size();
		int[][] accumulatedOffsets = new int[numPotentials][];

		for (int i = 0; i < numPotentials; i++) {
			TablePotential potential = potentials.get(i);
			accumulatedOffsets[i] = potentialResult.getAccumulatedOffsets(
					// potential.getOriginalVariables());
					potential.getVariables());
		}
		return accumulatedOffsets;
	}

	/**
	 * Compute the accumulated offsets of a {@code Potential}s array with
	 * the order imposed by {@code variables}
	 *
	 * @param potentials {@code ArrayList} of {@code Potential}s.
	 * @param variables  list of variables
	 * @return An array of arrays of integers ({@code int[][]}).
	 */
	public static int[][] getAccumulatedOffsets(List<TablePotential> potentials, List<Variable> variables) {

		int numPotentials = potentials.size();
		int[][] accumulatedOffsets = new int[numPotentials][];

		for (int i = 0; i < numPotentials; i++) {
			TablePotential potential = potentials.get(i);
			accumulatedOffsets[i] = TablePotential.getAccumulatedOffsets(variables, potential.getVariables());
		}
		return accumulatedOffsets;
	}

	/**
	 * @param potentials           list of TablePotentials
	 * @param variablesToEliminate variables to eliminate
	 * @return resultant potential
	 */
	public static Potential multiplyAndEliminate(List<TablePotential> potentials, List<Variable> variablesToEliminate) {

		// Obtain parameters to invoke multiplyAndMarginalize
		// Union of the variables of the potential list
		List<Variable> variablesToKeep = AuxiliaryOperations.getUnionVariables(potentials);
		variablesToKeep.removeAll(variablesToEliminate);

		return multiplyAndMarginalize(potentials, variablesToKeep, variablesToEliminate);
	}

	// TODO Eliminar este método si no es usado por otros

	/**
	 * @param potential a {@code TablePotential}
	 * @return The {@code potential} normalized
	 * @throws NormalizeNullVectorException NormalizeNullVectorException
	 */
	public static TablePotential normalize(TablePotential potential) throws NormalizeNullVectorException {
		TablePotential tablePotential = (TablePotential) potential;
		// Check for null vectors
		int p = 0;
		for (p = 0; p < tablePotential.values.length; p++) {
			if (tablePotential.values[p] != 0.0) {
				break;
			}
		}
		if (p == tablePotential.values.length) {
			// All elements in tablePotential.table == 0
			throw new NormalizeNullVectorException(
					"NormalizeNullVectorException: All elements in the TablePotential " + tablePotential
							.getVariables() + " table are equal to 0.0");
		}
		List<Variable> variables = tablePotential.getVariables();
		if ((variables != null) && (variables.size() > 0)) {
			if (potential.getPotentialRole() == PotentialRole.CONDITIONAL_PROBABILITY) {
				int numStates = variables.get(0).getNumStates();
				double normalizationFactor = 0.0;
				for (int i = 0; i < tablePotential.values.length; i += numStates) {
					normalizationFactor = 0.0;
					for (int j = 0; j < numStates; j++) {
						normalizationFactor += tablePotential.values[i + j];
					}
					for (int j = 0; j < numStates; j++) {
						tablePotential.values[i + j] /= normalizationFactor;
					}
				}
			} else if (potential.getPotentialRole() == PotentialRole.JOINT_PROBABILITY) {
				double normalizationFactor = 0.0;
				for (int i = 0; i < tablePotential.values.length; i++) {
					normalizationFactor += tablePotential.values[i];
				}
				for (int i = 0; i < tablePotential.values.length; i++) {
					tablePotential.values[i] /= normalizationFactor;
				}
			}
		}

		if (potential.strategyTrees != null && potential.strategyTrees.length > 0) {
			tablePotential.strategyTrees = potential.strategyTrees.clone();
		}
		return tablePotential;
	}

	/**
	 * Divides two {@code TablePotential}s using the accumulated offsets
	 * algorithm.
	 *
	 * @param numerator   {@code Potential}.
	 * @param denominator {@code Potential}.
	 * @return The quotient: A {@code TablePotential} with the union of the
	 * variables of numerator and denominator.
	 * Condition: numerator and denominator have the same domain (variables)
	 */
	public static TablePotential divide(Potential numerator, Potential denominator) {
		// Get variables and create quotient potential.
		// Quotient potential variables = numerator potential variables union
		// denominator potential variables
		TablePotential tNumerator = (TablePotential) numerator;
		TablePotential tDenominator = (TablePotential) denominator;
		List<Variable> numeratorVariables = new ArrayList<>(tNumerator.getVariables());
		List<Variable> denominatorVariables = new ArrayList<>(tDenominator.getVariables());
		int numNumeratorVariables = numeratorVariables.size();
		int numDenominatorVariables = denominatorVariables.size();
		denominatorVariables.removeAll(numeratorVariables);
		numeratorVariables.addAll(denominatorVariables);
		List<Variable> quotientVariables = numeratorVariables;
		TablePotential quotient = new TablePotential(quotientVariables, PotentialRole.JOINT_PROBABILITY);
		if ((numNumeratorVariables == 0) || (numDenominatorVariables == 0)) {
			return divide(tNumerator, tDenominator, quotient, numNumeratorVariables, numDenominatorVariables);
		}

		int numVariables = quotient.getNumVariables();

		// Gets the tables of each TablePotential
		double[][] tables = new double[2][];
		tables[0] = tNumerator.values;
		tables[1] = tDenominator.values;

		// Gets dimension
		int[] quotientDimension = quotient.getDimensions();

		// Gets offset accumulate
		List<TablePotential> potentials = new ArrayList<>();
		potentials.add(tNumerator);
		potentials.add(tDenominator);
		int[][] offsetAccumulate = DiscretePotentialOperations.getAccumulatedOffsets(potentials, quotient);

		// Gets coordinate
		int[] quotientCoordinate = initializeCoordinates(numVariables);

		// Position in each table potential
		int[] potentialsPositions = new int[2];
		for (int i = 0; i < 2; i++) {
			potentialsPositions[i] = 0;
		}

		// Divide
		int incrementedVariable = 0;
		int[] dimension = quotient.getDimensions();
		int[] offset = quotient.getOffsets();
		int tamTable = 1; // If numVariables == 0 the potential is a constant
		if (numVariables > 0) {
			tamTable = dimension[numVariables - 1] * offset[numVariables - 1];
		}

		for (int quotientPosition = 0; quotientPosition < tamTable; quotientPosition++) {
			/*
			 * increment the result coordinate and find out which variable is to
			 * be incremented
			 */
			for (int iVariable = 0; iVariable < quotientCoordinate.length; iVariable++) {
				// try by incrementing the current variable (given by iVariable)
				quotientCoordinate[iVariable]++;
				if (quotientCoordinate[iVariable] != quotientDimension[iVariable]) {
					// we have incremented the right variable
					incrementedVariable = iVariable;
					// do not increment other variables;
					break;
				}
				/*
				 * this variable could not be incremented; we set it to 0 in
				 * resultCoordinate (the next iteration of the for-loop will
				 * increment the next variable)
				 */
				quotientCoordinate[iVariable] = 0;
			}

			// divide
			if (tDenominator.values[potentialsPositions[1]] == 0.0) {
				quotient.values[quotientPosition] = 0.0;
			} else {
				quotient.values[quotientPosition] = tNumerator.values[potentialsPositions[0]]
						/ tDenominator.values[potentialsPositions[1]];
			}
			for (int iPotential = 0; iPotential < 2; iPotential++) {
				// update the current position in each potential table
				potentialsPositions[iPotential] += offsetAccumulate[iPotential][incrementedVariable];
			}
		}

		return quotient;
	}

	/**
	 * Divide two potentials when one of them has any variable
	 *
	 * @param numerator               {@code TablePotential}
	 * @param denominator             {@code TablePotential}
	 * @param quotient                {@code TablePotential}
	 * @param numNumeratorVariables   {@code int}
	 * @param numDenominatorVariables {@code int}
	 * @return quotient The {@code TablePotential} received with its table.
	 */
	private static TablePotential divide(TablePotential numerator, TablePotential denominator, TablePotential quotient,
			int numNumeratorVariables, int numDenominatorVariables) {
		if (numNumeratorVariables == 0) {
			int sizeTableDenominator = denominator.values.length;
			double dNumerator = numerator.values[0];
			for (int i = 0; i < sizeTableDenominator; i++) {
				quotient.values[i] = dNumerator / denominator.values[i];
			}
		} else {
			int sizeTableNumerator = numerator.values.length;
			double dDenominator = denominator.values[0];
			for (int i = 0; i < sizeTableNumerator; i++) {
				quotient.values[i] = numerator.values[i] / dDenominator;
			}
		}
		return quotient;
	}

	/**
	 * @param numerator   <tt>Potential</tt>
	 * @param denominator <tt>Potential</tt>
	 * @return The quotient
	 * @throws IllegalArgumentTypeException <tt>IllegalArgumentTypeException</tt> if numerator of denominator
	 *                                      are not <tt>TablePotential</tt>
	 * @throws DivideByZeroException DivideByZeroException
	 */
	public static Potential dividePotentials(Potential numerator, Potential denominator)
			throws IllegalArgumentTypeException, DivideByZeroException {
		// parameter correct type verification before calling right method
		if (!(numerator instanceof TablePotential) || !(denominator instanceof TablePotential)) {
			String errMsg = new String("");
			errMsg = errMsg + "Unsupported operation: " + "divide can only manage potentials of type TablePotential.\n";
			if (numerator == null) {
				errMsg = errMsg + "Numerator = null\n";
			} else {
				if (!(numerator instanceof TablePotential)) {
					errMsg = errMsg + "Numerator class is " + numerator.getClass().getName() + "\n";
				}
			}
			if (denominator == null) {
				errMsg = errMsg + "Denominator = null\n";
			} else {
				if (!(denominator instanceof TablePotential)) {
					errMsg = errMsg + "Denominator class is " + denominator.getClass().getName() + "\n";
				}
			}
			throw new IllegalArgumentTypeException(errMsg);
		}

		return DiscretePotentialOperations.divide(numerator, denominator);
	}

	/**
	 * @param tablePotentials      {@code ArrayList} of {@code TablePotential}s.
	 * @param fSVariablesToKeep    {@code ArrayList} of {@code Variable}s.
	 * @param fSVariableToMaximize {@code Variable}.
	 * @return Two potentials: 1) a {@code Potential} resulting of
	 * multiplication and maximization of
	 * {@code variableToMaximize} and 2) a
	 * {@code GTablePotential} of {@code Choice} (same
	 * variables as preceding) with the value choosed for
	 * {@code variableToMaximize} in each configuration.
	 */
	@SuppressWarnings("unchecked") public static Object[] multiplyAndMaximize(List<? extends Potential> tablePotentials,
			List<Variable> fSVariablesToKeep, Variable fSVariableToMaximize) {
		List<TablePotential> potentials = (ArrayList<TablePotential>) ((Object) tablePotentials);
		List<Variable> variablesToKeep = (ArrayList<Variable>) ((Object) fSVariablesToKeep);

		PotentialRole role = getRole(tablePotentials);

		TablePotential resultingPotential = new TablePotential(variablesToKeep, role);
		//        if (role == PotentialRole.UTILITY) {
		//        	resultingPotential.setUtilityVariable(composeVariable(fSVariablesToKeep));
		//        }

		GTablePotential<Choice> gResult = new GTablePotential<>(variablesToKeep, role);
		int numStates = ((Variable) fSVariableToMaximize).getNumStates();
		int[] statesChoosed;
		Choice choice;

		// Constant potentials are those that do not depend on any variables.
		// The product of all the constant potentials is the constant factor.
		double constantFactor = 1.0;
		// Non constant potentials are proper potentials.
		List<TablePotential> properPotentials = new ArrayList<>();
		for (Potential potential : potentials) {
			if (potential.getNumVariables() != 0) {
				properPotentials.add((TablePotential) potential);
			} else {
				constantFactor *= ((TablePotential) potential).values[((TablePotential) potential)
						.getInitialPosition()];
			}
		}

		int numProperPotentials = properPotentials.size();

		if (numProperPotentials == 0) {
			resultingPotential.values[0] = constantFactor;
			return new Object[] { resultingPotential, gResult };
		}

		// variables in the resulting potential
		List<Variable> unionVariables = new ArrayList<>();
		unionVariables.add((Variable) fSVariableToMaximize);
		unionVariables.addAll(variablesToKeep);
		int numUnionVariables = unionVariables.size();

		// current coordinate in the resulting potential
		int[] unionCoordinate = new int[numUnionVariables];
		int[] unionDimensions = TablePotential.calculateDimensions(unionVariables);

		// Defines some arrays for the proper potentials...
		double[][] tables = new double[numProperPotentials][];
		int[] initialPositions = new int[numProperPotentials];
		int[] currentPositions = new int[numProperPotentials];
		int[][] accumulatedOffsets = new int[numProperPotentials][];
		// ... and initializes them
		TablePotential unionPotential = new TablePotential(unionVariables, null);
		for (int i = 0; i < numProperPotentials; i++) {
			TablePotential potential = (TablePotential) properPotentials.get(i);
			tables[i] = potential.values;
			initialPositions[i] = potential.getInitialPosition();
			currentPositions[i] = initialPositions[i];
			accumulatedOffsets[i] = unionPotential.getAccumulatedOffsets(potential.getVariables());
		}

		// The result size is the product of the dimensions of variables to keep
		int resultSize = resultingPotential.values.length;
		// The elimination size is the product of the dimensions of variables to eliminate
		int eliminationSize = 1;
		eliminationSize *= ((Variable) fSVariableToMaximize).getNumStates();

		// Auxiliary variables for the nested loops
		double multiplicationResult; // product of the table values
		double maxValue; // in general, the sum or the maximum
		int increasedVariable = 0; // when computing the next configuration

		// outer iterations correspond to the variables to keep
		for (int outerIteration = 0; outerIteration < resultSize; outerIteration++) {
			// Inner iterations correspond to the variables to eliminate
			// accumulator summarizes the result of all inner iterations

			// first inner iteration
			multiplicationResult = constantFactor;
			for (int i = 0; i < numProperPotentials; i++) {
				// multiply the numbers
				multiplicationResult *= tables[i][currentPositions[i]];
			}
			statesChoosed = new int[numStates];
			statesChoosed[0] = 0;
			choice = new Choice(fSVariableToMaximize, statesChoosed);
			maxValue = multiplicationResult;
			choice.setValue(0); // because in first iteration we have a maximum

			// next inner iterations
			for (int innerIteration = 1; innerIteration < eliminationSize; innerIteration++) {

				// find the next configuration and the index of the
				// increased variable
				increasedVariable = findNextConfigurationAndIndexIncreasedVariable(unionDimensions, unionCoordinate,
						increasedVariable);

				// update the positions of the potentials we are multiplying
				for (int i = 0; i < numProperPotentials; i++) {
					currentPositions[i] += accumulatedOffsets[i][increasedVariable];
				}

				// multiply the table values of the potentials
				multiplicationResult = constantFactor;
				for (int i = 0; i < numProperPotentials; i++) {
					multiplicationResult = multiplicationResult * tables[i][currentPositions[i]];
				}

				// update the accumulator (for this inner iteration)
				if (multiplicationResult > (maxValue + maxRoundErrorAllowed)) {
					choice.setValue(innerIteration);
					maxValue = multiplicationResult;
				} else {
					if ((multiplicationResult < (maxValue + maxRoundErrorAllowed)) && (
							multiplicationResult >= (
									maxValue - maxRoundErrorAllowed
							)
					)) {
						choice.addValue(innerIteration);
					}
				}
				// accumulator =
				// operator.combine(accumlator,multiplicationResult);

			} // end of inner iteration

			// when eliminationSize == 0 there is a multiplication without
			// maximization but we must find the next configuration
			if (outerIteration < resultSize - 1) {
				// find the next configuration and the index of the
				// increased variable
				increasedVariable = findNextConfigurationAndIndexIncreasedVariable(unionDimensions, unionCoordinate,
						increasedVariable);

				// update the positions of the potentials we are multiplying
				for (int i = 0; i < numProperPotentials; i++) {
					currentPositions[i] += accumulatedOffsets[i][increasedVariable];
				}
			}

			resultingPotential.values[outerIteration] = maxValue;
			gResult.elementTable.add(choice);

		} // end of outer iteration

		//        createInterventions(resultingPotential, gResult, fSVariableToMaximize);
		Object[] resultPotentials = { resultingPotential, gResult };
		return resultPotentials;
	}

	/**
	 * @param arrayListPotentials List of table potentials
	 * @return true if there is utility potential in a list of potentials
	 */
	public static boolean isThereAUtilityPotential(List<TablePotential> arrayListPotentials) {
		boolean isThere = false;
		for (int i = 0; (i < arrayListPotentials.size()) && !isThere; i++) {
			//            isThere = arrayListPotentials.get(i).getPotentialRole() == PotentialRole.UTILITY;
		}
		return isThere;
	}

	/**
	 * @param tablePotentials    {@code ArrayList} of {@code TablePotential}s.
	 * @param variablesToKeep    {@code ArrayList} of {@code Variable}s.
	 * @param variableToMaximize {@code Variable}.
	 * @return Two potentials: 1) a {@code Potential} resulting of
	 * multiplication and maximization of
	 * {@code variableToMaximize} and 2) a
	 * {@code TablePotential} with the mass probability 1.0
	 * uniformly distributed among the maximizing states of
	 * {@code variableToMaximize} in each configuration; this is
	 * typically a policy of a decision.
	 */
	public static TablePotential[] multiplyAndMaximizeUniformly(List<TablePotential> tablePotentials,
			List<Variable> variablesToKeep, Variable variableToMaximize) {
		List<TablePotential> potentials = tablePotentials;

		PotentialRole roleResult = (isThereAUtilityPotential(tablePotentials)) ?
				PotentialRole.UNSPECIFIED :
				PotentialRole.CONDITIONAL_PROBABILITY;

		TablePotential resultingPotential = new TablePotential(variablesToKeep, roleResult);

		List<Variable> variablesPolicy = new ArrayList<>();
		variablesPolicy.add(variableToMaximize);
		variablesPolicy.addAll(variablesToKeep);

		TablePotential policy = new TablePotential(variablesPolicy, PotentialRole.CONDITIONAL_PROBABILITY);

		// Constant potentials are those that do not depend on any variables.
		// The product of all the constant potentials is the constant factor.
		double constantFactor = 1.0;
		// Non constant potentials are proper potentials.
		List<TablePotential> properPotentials = new ArrayList<>();
		for (Potential potential : potentials) {
			if (potential.getNumVariables() != 0) {
				properPotentials.add((TablePotential) potential);
			} else {
				constantFactor *= ((TablePotential) potential).values[((TablePotential) potential)
						.getInitialPosition()];
			}
		}

		int numProperPotentials = properPotentials.size();

		if (numProperPotentials == 0) {
			resultingPotential.values[0] = constantFactor;
			return new TablePotential[] { resultingPotential, policy };
		}

		// variables in the resulting potential
		List<Variable> unionVariables = new ArrayList<>();
		unionVariables.add((Variable) variableToMaximize);
		unionVariables.addAll(variablesToKeep);
		int numUnionVariables = unionVariables.size();

		// current coordinate in the resulting potential
		int[] unionCoordinate = new int[numUnionVariables];
		int[] unionDimensions = TablePotential.calculateDimensions(unionVariables);

		// Defines some arrays for the proper potentials...
		double[][] tables = new double[numProperPotentials][];
		int[] initialPositions = new int[numProperPotentials];
		int[] currentPositions = new int[numProperPotentials];
		int[][] accumulatedOffsets = new int[numProperPotentials][];
		// ... and initializes them
		TablePotential unionPotential = new TablePotential(unionVariables, null);
		for (int i = 0; i < numProperPotentials; i++) {
			TablePotential potential = (TablePotential) properPotentials.get(i);
			tables[i] = potential.values;
			initialPositions[i] = potential.getInitialPosition();
			currentPositions[i] = initialPositions[i];
			accumulatedOffsets[i] = unionPotential
					// .getAccumulatedOffsets(potential.getOriginalVariables());
					.getAccumulatedOffsets(potential.getVariables());
		}

		// The result size is the product of the dimensions of the
		// variables to keeep
		int resultSize = resultingPotential.values.length;
		// The elimination size is the product of the dimensions of the
		// variables to eliminate
		int eliminationSize = 1;
		eliminationSize *= ((Variable) variableToMaximize).getNumStates();

		// Auxiliary variables for the nested loops
		double multiplicationResult; // product of the table values
		double accumulator; // in general, the sum or the maximum
		int increasedVariable = 0; // when computing the next configuration

		List<Integer> statesTies;

		// outer iterations correspond to the variables to keep
		for (int outerIteration = 0; outerIteration < resultSize; outerIteration++) {
			// Inner iterations correspond to the variables to eliminate
			// accumulator summarizes the result of all inner iterations

			// first inner iteration
			multiplicationResult = constantFactor;
			for (int i = 0; i < numProperPotentials; i++) {
				// multiply the numbers
				multiplicationResult *= tables[i][currentPositions[i]];
			}
			statesTies = new ArrayList<>();
			statesTies.add(0);
			accumulator = multiplicationResult;
			int[] positionTies = new int[eliminationSize];
			int numTies = 0;
			positionTies[numTies] = currentPositions[0];
			numTies++;

			// next inner iterations
			for (int innerIteration = 1; innerIteration < eliminationSize; innerIteration++) {

				// find the next configuration and the index of the
				// increased variable
				increasedVariable = findNextConfigurationAndIndexIncreasedVariable(unionDimensions, unionCoordinate,
						increasedVariable);

				// update the positions of the potentials we are multiplying
				for (int i = 0; i < numProperPotentials; i++) {
					currentPositions[i] += accumulatedOffsets[i][increasedVariable];
				}

				// multiply the table values of the potentials
				multiplicationResult = constantFactor;
				for (int i = 0; i < numProperPotentials; i++) {
					multiplicationResult = multiplicationResult * tables[i][currentPositions[i]];
				}

				// update the accumulator (for this inner iteration)
				Double diffWithAccumulator = multiplicationResult - accumulator;
				if (diffWithAccumulator > maxRoundErrorAllowed) {
					statesTies = new ArrayList<>();
					statesTies.add(innerIteration);
					accumulator = multiplicationResult;
					numTies = 0;
					positionTies[numTies] = currentPositions[0];
					numTies++;
				} else {
					if (Math.abs(diffWithAccumulator) < maxRoundErrorAllowed) {
						statesTies.add(innerIteration);
						positionTies[numTies] = currentPositions[0];
						numTies++;
					}
				}
				// accumulator =
				// operator.combine(accumlator,multiplicationResult);

			} // end of inner iteration

			// when eliminationSize == 0 there is a multiplication without
			// maximization but we must find the next configuration
			if (outerIteration < resultSize - 1) {
				// find the next configuration and the index of the
				// increased variable
				increasedVariable = findNextConfigurationAndIndexIncreasedVariable(unionDimensions, unionCoordinate,
						increasedVariable);

				// update the positions of the potentials we are multiplying
				for (int i = 0; i < numProperPotentials; i++) {
					currentPositions[i] += accumulatedOffsets[i][increasedVariable];
				}
			}

			resultingPotential.values[outerIteration] = accumulator;
			assignProbUniformlyInTies(policy, variableToMaximize.getNumStates(), statesTies,
					resultingPotential.getConfiguration(outerIteration));
		} // end of outer iteration

		TablePotential[] resultPotentials = { resultingPotential, policy };
		return resultPotentials;
	}

	private static void assignProbUniformlyInTies(TablePotential tp, int numStatesVariable, List<Integer> statesTies,
			int[] policyDomainConfiguration) {
		Double probTies;

		int numStatesTies = statesTies.size();
		probTies = 1.0 / numStatesTies;

		int lenghtPolicyDomainConfiguration = policyDomainConfiguration.length;
		int[] tPConfiguration = new int[lenghtPolicyDomainConfiguration + 1];
		for (int j = 0; j < lenghtPolicyDomainConfiguration; j++) {
			tPConfiguration[j + 1] = policyDomainConfiguration[j];
		}
		// Assign probabilities to states in tie and the other ones
		for (int i = 0; i < numStatesVariable; i++) {
			tPConfiguration[0] = i;
			int posTPConfiguration = tp.getPosition(tPConfiguration);
			double iProb = (statesTies.contains(i)) ? probTies : 0.0;
			tp.values[posTPConfiguration] = iProb;
		}

	}

	/**
	 * @param potentialsVariable {@code ArrayList} of {@code Potential}s to multiply.
	 * @param variableToMaximize {@code Variable}.
	 * @return Two potentials: 1) a {@code Potential} resulting of
	 * multiplication and maximization of
	 * {@code variableToMaximize} and 2) a
	 * {@code GTablePotential} of {@code Choice} (same
	 * variables as preceding) with the value chosen for
	 * {@code variableToMaximize} in each configuration.
	 */
	public static Object[] multiplyAndMaximize(List<? extends Potential> potentialsVariable,
			Variable variableToMaximize) {
		// Use a HashSet to add the variables to avoid adding one variable more
		// than one time
		HashSet<Variable> addedVariables = new HashSet<>();
		for (Potential potential : potentialsVariable) {
			addedVariables.addAll(potential.getVariables());
		}
		List<Variable> variablesToKeep = new ArrayList<>(addedVariables);
		variablesToKeep.remove(variableToMaximize);
		return multiplyAndMaximize(potentialsVariable, variablesToKeep, variableToMaximize);
	}

	//CMI
	//For Univariate

	/**
	 * @param potentialsVariable {@code ArrayList} of {@code Potential}s.
	 * @param variableToMaximize {@code Variable}.
	 * @return Two potentials: 1) a {@code Potential} resulting of
	 * multiplication and maximization of
	 * {@code variableToMaximize} and 2) a
	 * {@code TablePotential} with the mass probability 1.0
	 * uniformly distributed among the maximizing states of
	 * {@code variableToMaximize} in each configuration; this is
	 * typically a policy of a decision.
	 */
	public static TablePotential[] multiplyAndMaximizeUniformly(List<TablePotential> potentialsVariable,
			Variable variableToMaximize) {
		// Use a HashSet to add the variables to avoid adding one variable more
		// than one time
		HashSet<Variable> addedVariables = new HashSet<>();
		for (TablePotential potential : potentialsVariable) {
			addedVariables.addAll(potential.getVariables());
		}
		List<Variable> variablesToKeep = new ArrayList<>(addedVariables);
		variablesToKeep.remove(variableToMaximize);
		return multiplyAndMaximizeUniformly(potentialsVariable, variablesToKeep, variableToMaximize);
	}

	//For AugmentedTable

	/**
	 * @param potential          one {@code TablePotential}.
	 * @param variableToMaximize {@code Variable}.
	 * @return Two potentials: 1) a {@code Potential} resulting of
	 * multiplication and maximization of
	 * {@code variableToMaximize} and 2) a
	 * {@code GTablePotential} of {@code Choice} (same
	 * variables as preceding) with the value chosen for
	 * {@code variableToMaximize} in each configuration.
	 */
	public static Object[] maximize(Potential potential, Variable variableToMaximize) {
		List<Potential> potentialsVariable = new ArrayList<>();
		potentialsVariable.add(potential);
		List<Variable> variablesToKeep = new ArrayList<>(potential.getVariables());
		variablesToKeep.remove(variableToMaximize);
		return multiplyAndMaximize(potentialsVariable, variablesToKeep, variableToMaximize);
	}

	/**
	 * Copy the potential received to another potential with the same variables
	 * but with the order received in {@code otherOrderVariables}
	 *
	 * @param potential      {@code TablePotential}
	 * @param otherOrderVariables {@code ArrayList} of {@code Variable}
	 * @return The {@code TablePotential} generated
	 * Condition: {@code otherVariables} are the same variables than the
	 * variables of {@code potential}
	 */
	public static TablePotential reorder(TablePotential potential, List<Variable> otherOrderVariables) {
		boolean hasInterventions = false;
		TablePotential newPotential = new TablePotential(otherOrderVariables, potential.getPotentialRole());
		int[] accOffsets = potential.getAccumulatedOffsets(otherOrderVariables);
		int[] potentialPositions = new int[potential.getNumVariables()];
		int[] potentialDimensions = potential.getDimensions();
		double[] valuesOrigPotential = potential.values;
		double[] valuesNewPotential = newPotential.values;
		StrategyTree[] intervOrigPotential = potential.strategyTrees;
		StrategyTree[] intervNewPotential = null;
		UncertainValue[] uncertainValues = null;
		UncertainValue[] copyUncertainValues = null;
		if (potential.isUncertain()) {
			uncertainValues = potential.uncertainValues;
			newPotential.uncertainValues = new UncertainValue[potential.uncertainValues.length];
			copyUncertainValues = newPotential.uncertainValues;
		}
		hasInterventions = intervOrigPotential != null && intervOrigPotential.length > 0;
		if (hasInterventions) {
			int newInterventionsLength = potential.strategyTrees.length;
			newPotential.strategyTrees = new StrategyTree[newInterventionsLength];
			intervNewPotential = newPotential.strategyTrees;
		}

		int copyTablePosition = 0;
		int numVariables = otherOrderVariables.size();
		int incrementedVariable, i;
		for (i = 0; i < valuesOrigPotential.length - 1; i++) {
			valuesNewPotential[copyTablePosition] = valuesOrigPotential[i];
			if (potential.isUncertain()) {
				copyUncertainValues[copyTablePosition] = uncertainValues[i];
			}
			if (hasInterventions) {
				intervNewPotential[copyTablePosition] = intervOrigPotential[i];
			}

			for (incrementedVariable = 0; incrementedVariable < numVariables; incrementedVariable++) {
				potentialPositions[incrementedVariable]++;
				if (potentialPositions[incrementedVariable] == potentialDimensions[incrementedVariable]) {
					potentialPositions[incrementedVariable] = 0;
				} else {
					break;
				}
			}
			copyTablePosition += accOffsets[incrementedVariable];
		}
		valuesNewPotential[copyTablePosition] = valuesOrigPotential[i];
		if (potential.isUncertain()) {
			copyUncertainValues[copyTablePosition] = uncertainValues[i];
		}
		if (hasInterventions) {
			intervNewPotential[copyTablePosition] = intervOrigPotential[i];
		}
		if (potential.isAdditive()) {
			newPotential.setCriterion(potential.getCriterion());
		}
		newPotential.properties = potential.properties;
		return newPotential;
	}

	//CMF

	/**
	 * Copy the UnivariateDistrPotential received to another UnivariateDistrPotential with the same variables
	 * but with the order received in {@code otherVariables}
	 *
	 * @param potential      {@code UnivariateDistrPotential}
	 * @param orderVariables {@code ArrayList} of {@code Variable}
	 * @return The {@code UnivariateDistrPotential} generated
	 * Condition: {@code otherVariables} are the same variables than the
	 * variables of {@code potential}
	 */
	public static UnivariateDistrPotential reorder(UnivariateDistrPotential potential, List<Variable> orderVariables) {
		int size = orderVariables.size();
		//orderVariables has the order of the parents of the augmentedTable, so parameterVariables should be added
		for (Variable parameterVariable : potential.getParameterVariables()) {
			orderVariables.add(parameterVariable);
		}
		UnivariateDistrPotential newPotential = new UnivariateDistrPotential(orderVariables,
				potential.getProbDensFunctionClass(), potential.getPotentialRole());
		orderVariables.remove(0);
		// I do use getVariable(0) for be compliant with the comparison in int[] accOffsets = potential.getAccumulatedOffsets(orderVariables);
		orderVariables.add(0, potential.getAugmentedTable().getVariable(0));
		AugmentedTable newDistributionTable = reorder(potential.getAugmentedTable(), orderVariables.subList(0, size));
		newPotential.setDistributionTable(newDistributionTable);
		return newPotential;
	}

	/**
	 * Copy the UnivariateDistrPotential received to another UnivariateDistrPotential with the same variables
	 * but with the order received in {@code otherVariables}
	 *
	 * @param potential      {@code UnivariateDistrPotential}
	 * @param orderVariables {@code ArrayList} of {@code Variable}
	 * @return The {@code UnivariateDistrPotential} generated
	 * Condition: {@code otherVariables} are the same variables than the
	 * variables of {@code potential}
	 */
	public static AugmentedTablePotential reorder(AugmentedTablePotential potential, List<Variable> orderVariables) {
		int size = orderVariables.size();
		//orderVariables has the order of the parents of the augmentedTable, so parameterVariables should be added
		for (Variable parameterVariable : potential.getParameterVariables()) {
			orderVariables.add(parameterVariable);
		}
		AugmentedTablePotential newPotential = new AugmentedTablePotential(orderVariables,
				potential.getPotentialRole());
		AugmentedTable newDistributionTable = reorder(potential.getAugmentedTable(), orderVariables.subList(0, size));
		newPotential.setAugmentedTable(newDistributionTable);
		return newPotential;
	}

	/**
	 * Copy the potential received to another potential with the same variables
	 * but with the order received in {@code otherVariables}
	 *
	 * @param potential      {@code TablePotential}
	 * @param orderVariables {@code ArrayList} of {@code Variable}
	 * @return The {@code TablePotential} generated
	 * Condition: {@code otherVariables} are the same variables than the
	 * variables of {@code potential}
	 */
	public static AugmentedTable reorder(AugmentedTable potential, List<Variable> orderVariables) {
		boolean hasInterventions = false;
		AugmentedTable newPotential = new AugmentedTable(orderVariables, potential.getPotentialRole());
		int[] accOffsets = potential.getAccumulatedOffsets(orderVariables);
		int[] potentialPositions = new int[potential.getNumVariables()];
		int[] potentialDimensions = potential.getDimensions();
		String[] valuesOrigPotential = potential.getFunctionValues();
		String[] valuesNewPotential = newPotential.getFunctionValues();

		int copyTablePosition = 0;
		int numVariables = orderVariables.size();
		int incrementedVariable, i;
		for (i = 0; i < valuesOrigPotential.length - 1; i++) {
			valuesNewPotential[copyTablePosition] = valuesOrigPotential[i];

			for (incrementedVariable = 0; incrementedVariable < numVariables; incrementedVariable++) {
				potentialPositions[incrementedVariable]++;
				if (potentialPositions[incrementedVariable] == potentialDimensions[incrementedVariable]) {
					potentialPositions[incrementedVariable] = 0;
				} else {
					break;
				}
			}
			copyTablePosition += accOffsets[incrementedVariable];
		}
		valuesNewPotential[copyTablePosition] = valuesOrigPotential[i];
		newPotential.properties = potential.properties;
		return newPotential;
	}

	/**
	 * Copy the potential received to another potential with the same variables
	 * but with changes in the order of states in one of the variables
	 *
	 * @param potential {@code TablePotential}
	 * @param variable  {@code VariableList} whose order of states has changed
	 * @param newOrder  array of {@code State}s in the new order
	 * @return The {@code TablePotential} generated
	 */
	public static TablePotential reorder(TablePotential potential, Variable variable, State[] newOrder) {
		TablePotential copyPotential = (TablePotential) potential.copy();
		double[] tablePotential = potential.values;
		double[] tableCopyPotential = copyPotential.values;
		UncertainValue[] uncertainValues = null;
		UncertainValue[] copyUncertainValues = null;
		int[] displacements = new int[newOrder.length];
		List<Variable> variables = copyPotential.getVariables();
		int variableIndex = variables.indexOf(variable);
		int offset = copyPotential.getOffsets()[variableIndex];
		State[] oldOrder = variable.getStates();
		for (int i = 0; i < newOrder.length; ++i) {
			displacements[i] = -1;
			int j = 0;
			boolean found = false;
			while (!found) {
				if (oldOrder[i] == newOrder[j]) {
					displacements[i] = j - i;
					found = true;
				}
				++j;
			}
		}

		if (potential.isUncertain()) {
			uncertainValues = potential.uncertainValues;
			copyPotential.uncertainValues = new UncertainValue[potential.uncertainValues.length];
			copyUncertainValues = copyPotential.uncertainValues;
		}

		for (int i = 0; i < tablePotential.length; i++) {
			int indexOfState = (i / offset) % variable.getNumStates();
			int newIndex = i + (displacements[indexOfState % variable.getNumStates()] * offset);
			tableCopyPotential[newIndex] = tablePotential[i];
			if (potential.isUncertain()) {
				copyUncertainValues[newIndex] = uncertainValues[i];
			}
		}
		if (potential.isAdditive()) {
			copyPotential.setCriterion(potential.getCriterion());
		}
		copyPotential.properties = potential.properties;
		return copyPotential;
	}

	/**
	 * @param potentials set of TablePotentials
	 * @return The maximization of a list of potentials defined over the same variables
	 */
	public static TablePotential maximize(Collection<TablePotential> potentials) {
		TablePotential result;
		Collection<TablePotential> setPot;

		if (potentials == null) {
			result = null;
		} else {
			int numPotentials = potentials.size();
			if (numPotentials == 0) {
				result = null;
			} else {
				Iterator<TablePotential> iterPotentials = potentials.iterator();
				TablePotential potFirst = potentials.iterator().next();
				List<Variable> variablesFirst = potFirst.getVariables();
				setPot = new HashSet<>();
				setPot.add(potFirst);
				while (iterPotentials.hasNext()) {
					setPot.add(reorder(iterPotentials.next(), variablesFirst));
				}
				int lengthValues = potFirst.values.length;
				double newValues[] = new double[lengthValues];
				for (int i = 0; i < lengthValues; i++) {
					double max = Double.NEGATIVE_INFINITY;
					for (TablePotential pot : setPot) {
						max = Math.max(pot.values[i], max);
					}
					newValues[i] = max;
				}

				result = new TablePotential(variablesFirst, potFirst.getPotentialRole(), newValues);
				if (result.isAdditive()) {
					result.setCriterion(potFirst.getCriterion());
				}
			}
		}
		return result;
	}




	/*
	 *//* This method is used to remove a decision variable from a probability potential
	 * that in fact does not depend on the decision variable
	 * @param variable <code>Variable</code>
	 * @param inputPotential <code>TablePotential</code>
	 * @return. A <code>TablePotential</code>
	 *//*
	public static TablePotential oldProjectOutVariable(Variable variable, TablePotential inputPotential) {
		List<Variable> inputPotentialVariables = inputPotential.getVariables();
		int numInputVariables = inputPotentialVariables.size();
		TablePotential projectedPotential;

		if (inputPotentialVariables.contains(variable)) {

			// initialize the output potential
			List<Variable> projectedPotentialVariables = inputPotentialVariables;
			projectedPotentialVariables.remove(variable);
			projectedPotential = new TablePotential(projectedPotentialVariables, PotentialRole.CONDITIONAL_PROBABILITY);

			// in allVariables, the first variable is variable
			List<Variable> allVariables = new ArrayList<>();
			allVariables.add(variable);
			allVariables.addAll(projectedPotentialVariables);

			// constants for the iterations
			int variableSize = variable.getNumStates();
			int[] allVariablesDimensions = TablePotential.calculateDimensions(allVariables);
			int[] accOffsetsInputPotential = TablePotential.getAccumulatedOffsets(allVariables,
					inputPotentialVariables);
			int[] accOffsetsProjectedPotential = TablePotential.getAccumulatedOffsets(allVariables,
					projectedPotentialVariables);

			// auxiliary variables that may change in every iteration
			int[] allVariablesCoordinate = new int[numInputVariables];
			int inputPotentialPosition = 0;
			int projectedPotentialPosition = 0;
			int increasedVariable = 0;

			// outer iterations correspond to the variables in the output
			// potential
			int numOuterIterations = TablePotential.computeTableSize(projectedPotentialVariables);
			for (int outerIteration = 0; outerIteration < numOuterIterations; outerIteration++) {
				// inner iterations correspond to the variable to eliminate
				for (int innerIteration = 0; innerIteration < variableSize; innerIteration++) {
					projectedPotential.values[projectedPotentialPosition] = inputPotential.values[inputPotentialPosition];

					if (!(outerIteration == numOuterIterations - 1 && innerIteration == variableSize - 1)) {
						// find the next configuration and the index of the increased variable
						increasedVariable = findNextConfigurationAndIndexIncreasedVariable(allVariablesDimensions,
								allVariablesCoordinate, increasedVariable);

						// Update coordinates
						inputPotentialPosition += accOffsetsInputPotential[increasedVariable];
						projectedPotentialPosition += accOffsetsProjectedPotential[increasedVariable];
					}
				}
			} // end of the outer loop
		} else {
			projectedPotential = (TablePotential) inputPotential.copy();
		}
		// TODO Manolo. Hacer que siempre devuelva un potencial, aunque sea la
		// unidad
		// TODO Este método está aún en pruebas tras la última refactorización. Ante cualquier duda, preguntar a Manolo.		

		// Do not return the probability potential if it depends on no variables
		// and its value is 1
		
		 * if (projectedPotential.getNumVariables() == 0 &&
		 * almostEqual(projectedPotential.values[0], 1.0)) { projectedPotential
		 * = DiscretePotentialOperations.createUnityProbabilityPotential(); }
		 
		return projectedPotential;
	}
*/

	/**
	 * @param dimension         dimension
	 * @param coordinate        coordinate
	 * @param increasedVariable increased variable
	 * @return index
	 */
	static int findNextConfigurationAndIndexIncreasedVariable(int[] dimension, int[] coordinate,
			int increasedVariable) {
		boolean isCoordinateJLessThanDimensionJ = false;
		for (int j = 0; j < dimension.length && !isCoordinateJLessThanDimensionJ; j++) {
			coordinate[j]++;
			if (coordinate[j] < dimension[j]) {
				increasedVariable = j;
				isCoordinateJLessThanDimensionJ = true;
			} else {
				coordinate[j] = 0;
			}
		}
		return increasedVariable;
	}

	/**
	 * @param outputUtilityPotential Output utility potential
	 * @return True if there are relevant utilities
	 */
	static boolean thereAreRelevantUtilities(TablePotential outputUtilityPotential) {
		boolean thereAreRelevantUtilities = false;
		for (int i = 0; i < outputUtilityPotential.values.length; i++) {
			if (!almostEqual(outputUtilityPotential.values[i], 0.0)) {
				thereAreRelevantUtilities = true;
				break;
			}
		}
		return thereAreRelevantUtilities;
	}

	/**
	 * This method is used to remove a decision variable from a probability potential
	 * that in fact does not depend on the decision variable
	 *
	 * @param variable       {@code Variable}
	 * @param inputPotential {@code TablePotential}
	 * @return A {@code TablePotential}
	 */
	public static TablePotential projectOutVariable(Variable variable, TablePotential inputPotential) {
		TablePotential output = null;
		EvidenceCase evi = new EvidenceCase();
		try {
			evi.addFinding(new Finding(variable, variable.getStates()[0]));
		} catch (InvalidStateException | IncompatibleEvidenceException e) {
			e.printStackTrace();
		}
		try {
			output = inputPotential.tableProject(evi, null).get(0);
		} catch (NonProjectablePotentialException | WrongCriterionException e) {
			e.printStackTrace();
		}
		return output;
	}

	/**
	 * Compares two numbers
	 *
	 * @param a {@code double}
	 * @param b {@code double}
	 * @return {@code true} when a and b are close.
	 */
	public static boolean almostEqual(double a, double b) {
		return (Math.abs(b - a) <= maxRoundErrorAllowed * Math.abs(a));
	}

	public static TablePotential createZeroUtilityPotential(ProbNet dan) {
		TablePotential newPotential = createOneValuePotential(PotentialRole.UNSPECIFIED, 0.0);
		if (dan != null) {
			newPotential.setCriterion(dan.getDecisionCriteria().get(0));
		}
		return newPotential;
	}

	public static TablePotential createUnityProbabilityPotential() {
		return createOneValuePotential(PotentialRole.CONDITIONAL_PROBABILITY, 1.0);
	}

	public static TablePotential createOneValuePotential(PotentialRole role, double value) {
		TablePotential newUtilityPotential;
		newUtilityPotential = new TablePotential(role);
		double values[] = new double[1];
		values[0] = value;
		newUtilityPotential.setValues(values);
		return newUtilityPotential;
	}

	public static double sum(double[] values) {
		double result = 0.0;
		for (int i = 0; i < values.length; i++) {
			result += values[i];
		}
		return result;
	}

	/**
	 * @param potentials List of potentials
	 * @return A potential that results from multiplying the product of probability potentials and the sum of utility potentials
	 */
	public static TablePotential matrixPotential(List<Potential> potentials) {
		List<TablePotential> probs = new ArrayList<>();
		List<TablePotential> utils = new ArrayList<>();
		for (Potential potential : potentials) {
			if (potential.isAdditive()) {
				utils.add((TablePotential) potential);
			} else {
				probs.add((TablePotential) potential);
			}
		}
		return multiply(
				probs.size() > 0 ? multiply(probs) : DiscretePotentialOperations.createUnityProbabilityPotential(),
				sum(utils));
	}

	/**
	 * @param inputPotentialsList List of table potentials
	 * @param decisionsTotallyOrdered List of decision totally ordered
	 * @return an order list of the potentials in 'inputPotentialsList', where the potentials are ordered according to the
	 * total order in 'decisionsTotallyOrdered'
	 */
	public static List<TablePotential> orderPotentialsByTotalOrder(List<TablePotential> inputPotentialsList,
			List<Variable> decisionsTotallyOrdered) {
		List<TablePotential> orderedListOfPotentials = new ArrayList<>();
		Set<TablePotential> inputPotentialsSet = new HashSet<>();

		if (decisionsTotallyOrdered != null) {
			for (TablePotential auxPot : inputPotentialsList) {
				inputPotentialsSet.add(auxPot);
			}

			Set<TablePotential> potentialsWithoutIntervention = new HashSet<>();
			// Remove from inputPotentials the potentials without Interventions
			// and
			// place them in withoutInterv
			for (TablePotential auxPot : inputPotentialsSet) {
				if (!auxPot.hasInterventions()) {
					potentialsWithoutIntervention.add(auxPot);
				}
			}

			for (Variable dec : decisionsTotallyOrdered) {
				Set<TablePotential> potentialsWithDecisionInIntervention;
				potentialsWithDecisionInIntervention = getPotentialsWithDecisionInIntervention(dec, inputPotentialsSet);
				inputPotentialsSet.removeAll(potentialsWithDecisionInIntervention);
				orderedListOfPotentials.addAll(potentialsWithDecisionInIntervention);
			}

			inputPotentialsSet.removeAll(potentialsWithoutIntervention);
			orderedListOfPotentials.addAll(potentialsWithoutIntervention);
			// TODO Manolo> I am not sure if in this point inputPotentials is
			// empty.
			orderedListOfPotentials.addAll(inputPotentialsList);
		} else {
			orderedListOfPotentials.addAll(inputPotentialsList);
		}
		return orderedListOfPotentials;
	}

	/**
	 * @param decision Decision variable
	 * @param potentials List of table potentials
	 * @return TablePotential with decision as the first variable and the union of the variables of the potentials.
	 * @throws PotentialOperationException PotentialOperationException
	 * Condition: The number of states of decision must be equal to the number of potentials.
	 */
	public static TablePotential merge(Variable decision, List<TablePotential> potentials)
			throws PotentialOperationException {
		throwExceptionIfNecessaryInMergeOperation(decision, potentials);
		// --------------
		// Initialization
		// --------------
		// Gets merged potential variables as decision variable plus the union of the variables of the potentials
		List<Variable> potentialsVariables = AuxiliaryOperations.getUnionVariables(potentials);
		List<Variable> mergedVariables = new ArrayList<Variable>(potentialsVariables.size() + 1);
		mergedVariables.add(decision);
		mergedVariables.addAll(potentialsVariables);

		int numMergedVariables = mergedVariables.size();

		// Gets dimension of the merged potential
		int[] mergedDimension = TablePotential.calculateDimensions(mergedVariables);

		// Gets offset accumulate
		int[][] offsetAccumulate = DiscretePotentialOperations.getAccumulatedOffsets(potentials, mergedVariables);

		int[] offsets = TablePotential.calculateOffsets(mergedDimension);
		int tableSize = numMergedVariables > 0 ?
				mergedDimension[numMergedVariables - 1] * offsets[numMergedVariables - 1] :
				1;
		double[] mergedValues = new double[tableSize];

		int numPotentials = potentials.size();

		// Checks the existence of interventions in at least one of the potentials, in that case create an array of interventions in the merged potential.
		boolean thereArePotentialsWithInterventions = thereArePotentialsWithInterventions(potentials);
		StrategyTree[] mergedInterventions = thereArePotentialsWithInterventions ? new StrategyTree[tableSize] : null;
		boolean[] potentialsHaveInterventions = thereArePotentialsWithInterventions ?
				getPotentialsHaveInterventions(potentials) :
				null;
		StrategyTree[][] potentialsInterventions = thereArePotentialsWithInterventions ?
				new StrategyTree[numPotentials][] :
				null;

		// Checks the existence of uncertain values in at least one of the potentials, in that case create an array of uncertain values in the merged potential.
		boolean thereArePotentialsWithUncertainValues = thereArePotentialsWithUncertainValues(potentials);
		UncertainValue[] mergedUncertainValues = thereArePotentialsWithUncertainValues ?
				new UncertainValue[tableSize] :
				null;
		boolean[] potentialsHaveUncertainValues = thereArePotentialsWithUncertainValues ?
				getPotentialsHaveUncertainValues(potentials) :
				null;
		UncertainValue[][] potentialsUncertainValues = thereArePotentialsWithUncertainValues ?
				new UncertainValue[numPotentials][] :
				null;

		// Checks the known subtypes of TablePotential
		boolean thereAreGTablePotentials = thereAreGTablePotentials(potentials);
		List<CEP> mergedElementsTable = thereAreGTablePotentials ? new ArrayList<CEP>(tableSize) : null;
		if (thereAreGTablePotentials) { // Fill the list with something to use the method List.set(index) without problems.
			for (int i = 0; i < tableSize; i++) {
				mergedElementsTable.add(null);
			}
		}
		boolean[] potentialsAreGTablePotentials = thereAreGTablePotentials ?
				getBooleanArrayOfPotentialsThatAreGTablePotentials(potentials, numPotentials) :
				null;
		List<List<CEP>> elementsTables = thereAreGTablePotentials ? new ArrayList<List<CEP>>(numPotentials) : null;
		if (thereAreGTablePotentials) { // same as before
			for (int i = 0; i < numPotentials; i++) {
				elementsTables.add(null);
			}
		}

		// Gets the tables, interventions and uncertain values of each potential
		double[][] tables = new double[numPotentials][];
		for (int indexPotential = 0; indexPotential < numPotentials; indexPotential++) {
			TablePotential potential = potentials.get(indexPotential);
			tables[indexPotential] = potential.values;
			if (thereArePotentialsWithInterventions) {
				potentialsInterventions[indexPotential] = potential.strategyTrees;
			}
			if (thereArePotentialsWithUncertainValues) {
				potentialsUncertainValues[indexPotential] = potential.uncertainValues;
			}
			if (thereAreGTablePotentials && potentialsAreGTablePotentials[indexPotential]) {
				elementsTables.set(indexPotential, ((GTablePotential<CEP>) potential).elementTable);
			}
		}
		// Gets coordinate
		int[] mergedCoordinate = initializeCoordinates(numMergedVariables);

		// Position in each table potential
		int[] potentialsPositions = new int[numPotentials];
		for (int i = 0; i < numPotentials; i++) {
			potentialsPositions[i] = 0;
		}

		// -----------
		// Method body
		// -----------
		int indexIncrementedVariable = 0;

		for (int mergedPosition = 0; mergedPosition < tableSize; mergedPosition++) {
			// Set values
			int indexActualPotential = mergedCoordinate[0]; // Potential corresponding to state=numPotential of the decision variable
			int indexInTableOfActualPotential = potentialsPositions[indexActualPotential]; // Position in actual potential
			mergedValues[mergedPosition] = tables[indexActualPotential][indexInTableOfActualPotential];
			// Set interventions
			if (thereArePotentialsWithInterventions) {
				mergedInterventions[mergedPosition] = potentialsHaveInterventions[indexActualPotential] ?
						potentialsInterventions[indexActualPotential][indexInTableOfActualPotential] :
						null;
			}
			// Set uncertain values
			if (thereArePotentialsWithUncertainValues) {
				mergedUncertainValues[mergedPosition] = potentialsHaveUncertainValues[indexActualPotential] ?
						potentialsUncertainValues[indexActualPotential][indexInTableOfActualPotential] :
						null;
			}
			// Set elementTable in the case there are GTablePotentials
			if (thereAreGTablePotentials) {
				CEP auxCEP = potentialsAreGTablePotentials[indexActualPotential] ?
						elementsTables.get(indexActualPotential).get(indexInTableOfActualPotential) :
						null;
				mergedElementsTable.set(mergedPosition, auxCEP);
			}

			//increment the merged coordinate and find out which variable is to be incremented
			for (int indexVariable = 0; indexVariable < mergedCoordinate.length; indexVariable++) {
				// try by incrementing the current variable (given by iVariable)
				mergedCoordinate[indexVariable]++;
				if (mergedCoordinate[indexVariable] != mergedDimension[indexVariable]) {
					// we have incremented the right variable
					indexIncrementedVariable = indexVariable;
					// do not increment other variables;
					break;
				}
				/*
				 * this variable could not be incremented; we set it to 0 in
				 * mergedCoordinate (the next iteration of the for-loop will
				 * increment the next variable)
				 */
				mergedCoordinate[indexVariable] = 0;
			}

			// update the current position in each potential table
			for (int indexPotential = 0; indexPotential < numPotentials; indexPotential++) {
				potentialsPositions[indexPotential] += offsetAccumulate[indexPotential][indexIncrementedVariable];
			}
		}

		// Create merged potential with previous values
		PotentialRole role = potentials.get(0).getPotentialRole();
		TablePotential mergedPotential = null;
		if (thereAreGTablePotentials) {
			mergedPotential = new GTablePotential<CEP>(mergedVariables, role, mergedElementsTable);
		} else {
			mergedPotential = new TablePotential(mergedVariables, role, mergedValues);
		}
		mergedPotential.strategyTrees = thereArePotentialsWithInterventions ? mergedInterventions : null;
		mergedPotential.uncertainValues = thereArePotentialsWithUncertainValues ? mergedUncertainValues : null;
		return mergedPotential;
	}

	/**
	 * Method used in merge operation, that launches a {@code PotentialOperationException} in this cases:
	 * <ul>
	 * <li>The variable is {@code null}.
	 * <li>The potentials are {@code null}.
	 * <li>The number of potentials is zero.
	 * <li>The number of states of the variable is different than the number of potentials.
	 * </ul>
	 * The message may consist of one or two causes at most.
	 *
	 * @param decision Decision variabl
	 * @param potentials Collection of potentials
	 * @throws PotentialOperationException PotentialOperationException
	 */
	private static void throwExceptionIfNecessaryInMergeOperation(Variable decision,
			Collection<TablePotential> potentials) throws PotentialOperationException {
		String message = null;
		if (decision == null) {
			message = nullVariable;
		}
		if (potentials == null) {
			if (message != null) {
				message += " and " + nullPotentials;
			} else {
				message = nullPotentials;
			}
		} else {
			int numPotentials = potentials.size();
			if (numPotentials == 0) {
				if (message != null) {
					message += " and " + noPotentials;
				} else {
					message = noPotentials;
				}
			} else {
				if (decision != null) {
					int numStates = decision.getNumStates();
					if (numStates != numPotentials) {
						message = "the number of states of the decision variable " + decision.getName() + " is "
								+ numStates + ",\nthe number of potentials is " + numPotentials
								+ " and they must be the same";
					}
				}
			}
		}
		if (message != null) {
			message = message.substring(0, 1).toUpperCase() + message.substring(1) + " in merge operation.";
			throw new PotentialOperationException(message);
		}
	}

	/**
	 * Returns true if there is at least one GTablePotential.
	 */
	private static boolean thereAreGTablePotentials(Collection<? extends Potential> potentials) {
		return findFirstGTablePotential(potentials) != null;
	}

	private static Potential findFirstGTablePotential(Collection<? extends Potential> potentials) {
		for (Potential potential : potentials) {
			if (potential instanceof GTablePotential) {
				return potential;
			}
		}
		return null;
	}

	/**
	 * @param potentials List of table potentials
	 * @param numPotentials Number of potentials
	 * @return array of booleans, the i-th boolean is true if the i-th potential has uncertain values.
	 */
	private static boolean[] getBooleanArrayOfPotentialsThatAreGTablePotentials(List<TablePotential> potentials,
			int numPotentials) {
		boolean[] potentialsThatAreGTablePotentials = new boolean[numPotentials];
		int numPotential = 0;
		for (TablePotential potential : potentials) {
			potentialsThatAreGTablePotentials[numPotential++] = potential instanceof GTablePotential;
		}
		return potentialsThatAreGTablePotentials;
	}

	/**
	 * @param potentials List of table potentials
	 * @return array of booleans, the i-th boolean is true if the i-th potential has interventions.
	 */
	private static boolean[] getPotentialsHaveInterventions(List<TablePotential> potentials) {
		boolean[] potentialsHaveInterventions = new boolean[potentials.size()];
		int numPotential = 0;
		for (TablePotential potential : potentials) {
			potentialsHaveInterventions[numPotential++] = potential.strategyTrees != null;
		}
		return potentialsHaveInterventions;
	}

	/**
	 * @param potentials List of table potentials
	 * @return array of booleans, the i-th boolean is true if the i-th potential has uncertain values.
	 */
	private static boolean[] getPotentialsHaveUncertainValues(List<TablePotential> potentials) {
		boolean[] potentialsHaveUncertainValues = new boolean[potentials.size()];
		int numPotential = 0;
		for (TablePotential potential : potentials) {
			potentialsHaveUncertainValues[numPotential++] = potential.uncertainValues != null;
		}
		return potentialsHaveUncertainValues;
	}

	/**
	 * @param potentials Collection of table potentials
	 * @return True if there are potentials with uncertain values
	 */
	private static boolean thereArePotentialsWithUncertainValues(Collection<TablePotential> potentials) {
		for (TablePotential potential : potentials) {
			if (potential.uncertainValues != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param potentials Collection of table potentials
	 * @return boolean
	 */
	private static boolean thereArePotentialsWithInterventions(Collection<TablePotential> potentials) {
		for (TablePotential potential : potentials) {
			if (potential.strategyTrees != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param decision Decision variable
	 * @param inputPotentials Set of table potentials
	 * @return Set of potentials with a decision
	 */
	private static Set<TablePotential> getPotentialsWithDecisionInIntervention(Variable decision,
			Set<TablePotential> inputPotentials) {
		Set<TablePotential> potsWithDecInIntervention = new HashSet<>();
		for (TablePotential auxPot : inputPotentials) {
			if (auxPot.hasInterventionForDecision(decision)) {
				potsWithDecInIntervention.add(auxPot);
			}
		}
		return potsWithDecInIntervention;
	}

	public static TablePotential createZeroProbabilityPotential() {
		return DiscretePotentialOperations.createOneValuePotential(PotentialRole.CONDITIONAL_PROBABILITY, 0.0);
	}

	public static TablePotential imposeOtherDistributionWhenDistributionIsZero(TablePotential xNewPotential) {
		List<Variable> variables = xNewPotential.getVariables();
		if (variables == null || variables.size() == 0 || xNewPotential.values == null || xNewPotential.values.length <=1) {
			return xNewPotential;
		}
		Variable firstVariable = variables.get(0);
		int numStatesFirstVariable = firstVariable.getNumStates();
		int numOuterIterations = xNewPotential.values.length / numStatesFirstVariable;
		int numConfiguration = 0;
		for (int i = 0; i < numOuterIterations; i++) {
			boolean allZeros = true;
			// Check whether all configurations are zero
			int startConfiguration = numConfiguration;
			for (int j = 0; j < numStatesFirstVariable; j++) {
				allZeros &= almostEqual(0.0, xNewPotential.values[startConfiguration++]);
			}
			if (allZeros) {
				startConfiguration = numConfiguration;
				xNewPotential.values[startConfiguration++] = 1.0;
				for (int j = 1; j < numStatesFirstVariable; j++) {
					xNewPotential.values[startConfiguration++] = 0.0;
				}
			}
			numConfiguration += numStatesFirstVariable;
		}
		return xNewPotential;
	}

	public static TablePotential evaluateFunctionPotential(FunctionPotential utilityPotential,
			List<TablePotential> potentials, List<Variable> utilityVariables) throws NumberFormatException, EvaluationException {
		int numPotentials = potentials.size();
		
		Criterion criterion = findFirstNonNullCriterion(potentials);		
		PotentialRole role = getRole(potentials);
		List<Variable> resultVariables = AuxiliaryOperations.getUnionVariables(potentials);
		int numVariables = resultVariables.size();
		boolean thereAreVariables = numVariables > 0;
		double[][] tables = initializeFromValues(potentials);
		int[] resultDimension = thereAreVariables ? TablePotential.calculateDimensions(resultVariables) : null;
		int[][] offsetAccumulate = DiscretePotentialOperations.getAccumulatedOffsets(potentials, resultVariables);
		int[] resultCoordinate = initializeCoordinates(numVariables);
		int[] potentialsPositions = initializeToZero(numPotentials);

		// Multiply
		int incrementedVariable = 0;

		int[] dimensions = TablePotential.calculateDimensions(resultVariables);
		
		
		int[] offsets = thereAreVariables ? TablePotential.calculateOffsets(dimensions):null;
		int tableSize = thereAreVariables ? dimensions[numVariables - 1] * offsets[numVariables - 1] : 1;
		double[] resultValues = new double[tableSize];

		TablePotential potentialWithInterventions = findFirstPotentialWithInterventions(potentials);
		boolean thereAreInterventions = (potentialWithInterventions != null);
		StrategyTree[] resultStrategyTrees = null;
		StrategyTree strategyTree = null;
		StrategyTree[] inputStrategyTrees = null;
		if (thereAreInterventions) {
			inputStrategyTrees = potentialWithInterventions.strategyTrees;
			resultStrategyTrees = new StrategyTree[tableSize];
			if (potentialWithInterventions.getVariables().size() == 0) {
				// The interventions are in a constant potential
				strategyTree = inputStrategyTrees[0];
			}
		}

		int indexPotentialWithInterventions = potentials.indexOf(potentialWithInterventions);

		List<String> utilityVariablesNames = new ArrayList<>();
		utilityVariables.forEach(x -> utilityVariablesNames.add(x.getName()));
		
		//utilityPotential.
		
		for (int resultPosition = 0; resultPosition < tableSize; resultPosition++) {
			// increment the result coordinate and find out which variable is to be
			// incremented
			for (int iVariable = 0; iVariable < resultCoordinate.length; iVariable++) {
				// try by incrementing the current variable (given by iVariable)
				resultCoordinate[iVariable]++;
				if (resultDimension!=null && resultCoordinate[iVariable] != resultDimension[iVariable]) {
					// we have incremented the right variable
					incrementedVariable = iVariable;
					// do not increment other variables;
					break;
				}
				resultCoordinate[iVariable] = 0;
			}

			Map<String, String> assignment = new Hashtable<>();
			// multiply
			for (int iPotential = 0; iPotential < numPotentials; iPotential++) {
				int potentialsPositionIPotential = potentialsPositions[iPotential];
				String varNameInExpressionToEvaluate = "v" + (iPotential+1);
				//String varNameInExpressionToEvaluate = utilityVariablesNames.get(iPotential);
				assignment.put(varNameInExpressionToEvaluate,
						"" + tables[iPotential][potentialsPositionIPotential]);
				// Obtain the intervention
				if (thereAreInterventions && indexPotentialWithInterventions == iPotential) {
					strategyTree = inputStrategyTrees[potentialsPositionIPotential];
				}
				// update the current position in each potential table
				if (thereAreVariables) {
					potentialsPositions[iPotential] += offsetAccumulate[iPotential][incrementedVariable];
				}
			}
			resultValues[resultPosition] = Double.parseDouble(utilityPotential.getValue(assignment));
			if (thereAreInterventions) {
				resultStrategyTrees[resultPosition] = strategyTree;
			}

		}

		TablePotential resultPotential = buildResultPotential(criterion, role, resultVariables, resultValues,
				thereAreInterventions, resultStrategyTrees);
		return resultPotential;
	}

	private static double[][] initializeFromValues(List<TablePotential> potentials) {
		int numPotentials = potentials.size();
		double[][] tables = new double[numPotentials][];
		for (int i = 0; i < numPotentials; i++) {
			tables[i] = potentials.get(i).values;
		}
		return tables;
	}

	private static TablePotential buildConstantPotential(double constantFactor, PotentialRole role) {
		TablePotential constantTablePotential = new TablePotential(null, role);
		constantTablePotential.values[0] = constantFactor;
		return constantTablePotential;
	}

	private static TablePotential buildResultPotential(Criterion criterion, PotentialRole role,
			List<Variable> resultVariables, double[] resultValues, boolean thereAreInterventions,
			StrategyTree[] resultStrategyTrees) {
		TablePotential resultPotential = new TablePotential(resultVariables, role, resultValues);
		if (criterion != null) {
			resultPotential.setCriterion(criterion);
		}
		if (thereAreInterventions) {
			resultPotential.strategyTrees = resultStrategyTrees;
		}
		return resultPotential;
	}
}
