/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.writer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.CDATA;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.openmarkov.core.exception.NoFindingException;
import org.openmarkov.core.exception.WriterException;
import org.openmarkov.core.io.ProbNetWriter;
import org.openmarkov.core.io.format.annotation.FormatType;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Properties;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.StringWithProperties;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.constraint.OnlyAtemporalVariables;
import org.openmarkov.core.model.network.constraint.PNConstraint;
import org.openmarkov.core.model.network.modelUncertainty.ProbDensFunction;
import org.openmarkov.core.model.network.modelUncertainty.ProbDensFunctionType;
import org.openmarkov.core.model.network.modelUncertainty.UncertainValue;
import org.openmarkov.core.model.network.potential.BinomialPotential;
import org.openmarkov.core.model.network.potential.DeltaPotential;
import org.openmarkov.core.model.network.potential.ExactDistrPotential;
import org.openmarkov.core.model.network.potential.ExponentialHazardPotential;
import org.openmarkov.core.model.network.potential.FunctionPotential;
import org.openmarkov.core.model.network.potential.GLMPotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.ProductPotential;
import org.openmarkov.core.model.network.potential.SumPotential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.WeibullHazardPotential;
import org.openmarkov.core.model.network.potential.canonical.ICIPotential;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDBranch;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDPotential;
import org.openmarkov.core.model.network.type.NetworkType;
import org.openmarkov.core.model.network.type.plugin.NetworkTypeManager;
import org.openmarkov.core.oopn.Instance;
import org.openmarkov.core.oopn.InstanceReferenceLink;
import org.openmarkov.core.oopn.NodeReferenceLink;
import org.openmarkov.core.oopn.OOPNet;
import org.openmarkov.core.oopn.ReferenceLink;
import org.openmarkov.io.probmodel.strings.XMLAttributes;
import org.openmarkov.io.probmodel.strings.XMLTags;
import org.openmarkov.io.probmodel.strings.XMLValues;

/**
 * @author Manuel Arias
 * @version 1.0
 */
@FormatType(name = "PGMXWriter0_2",  version = "0.2", extension = "pgmx", description = "OpenMarkov.0.2", role = "Writer")
public class PGMXWriter_0_2 implements ProbNetWriter {

	// Attributes
	/** The version format */
	protected String formatVersionNumber = "";

	public PGMXWriter_0_2() {
		formatVersionNumber= "0.2.0";
	}

	// Methods
	/**
	 * @param netName = path + network name + extension <code>String</code>
	 * @param probNet <code>ProbNet</code>
	 */
	public void writeProbNet(String netName, ProbNet probNet) throws WriterException {
		writeProbNet(netName, probNet, null);
	}

	/**
	 * @param netName = path + network name + extension.
	 * @param probNet <code>ProbNet</code> <code>String</code>
	 * @param evidences list of evidence cases. <code>ArrayList</code> of <code>EvidenceCase</code>
	 */
	public void writeProbNet(String netName, ProbNet probNet, List<EvidenceCase> evidences) throws WriterException {
		UtilParameters.manageParametersWriter(netName, probNet);
		// PrintWriter out = new PrintWriter(new FileOutputStream(netName));
		Element root = new Element("ProbModelXML");
		root.setAttribute(XMLAttributes.FORMAT_VERSION.toString(), formatVersionNumber.toString());
		try {
			writeXMLProbNet(probNet, root);
			writeInferenceOptions(probNet, root);
			writePolicies(probNet, root);
			writeEvidence(probNet, evidences, root);
			Document document = new Document(root);
			XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
			FileOutputStream out;
			try {
				out = new FileOutputStream(netName);
			} catch (FileNotFoundException e) {
				throw new WriterException("Can not create: " + netName + " file.");
			}
			try {
				xmlOutputter.output(document, out);
				out.flush();
				out.close();
			} catch (IOException e) {
				throw new WriterException("General Input/Output error writing: " + netName + ".");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new WriterException(e.getMessage());
		}
	}

	/**
	 * Removes from <code>evidence</code> the variables that are no longer
	 * present in <code>probNet</code>
	 *
	 * @param probNet
	 *            . <code>ProbNet</code>
	 * @param evidence
	 *            . <code>EvidenceCase</code>
	 */
	protected void removeMissingVariablesFromEvidence(ProbNet probNet, List<EvidenceCase> evidence) {
		HashSet<Variable> probNetVariables = new HashSet<Variable>(probNet.getVariables());
		for (EvidenceCase evidenceCase : evidence) {
			List<Variable> evidenceVariables = evidenceCase.getVariables();
			for (Variable variable : evidenceVariables) {
				if (!probNetVariables.contains(variable)) {
					try {
						evidenceCase.removeFinding(variable.getName());
					} catch (NoFindingException e) {
						// Unreachable code
					}
				}
			}
		}
	}

	/**
	 * @param probNet
	 *            . <code>ProbNet</code>
	 * @param root
	 *            . <code>Element</code>
	 */
	protected void writeXMLProbNet(ProbNet probNet, Element root) {
		Element probNetElement = new Element(XMLTags.PROB_NET.toString());
		probNetElement.setAttribute(XMLAttributes.TYPE.toString(), getXMLNetworkType(probNet));
		getProbNetChildren(probNet, probNetElement);
		root.addContent(probNetElement);
	}

	/**
	 * Writes Inference Options in the PGMX
	 *
	 * @param probNet
	 * @param root
	 */
	protected void writeInferenceOptions(ProbNet probNet, Element root) {
		Element inferenceOptionsElement = new Element(XMLTags.INFERENCE_OPTIONS.toString());

		if (probNet.getInferenceOptions().getMultiCriteriaOptions().getMulticriteriaType() != null) {
			getMulticriteriaOptions(probNet, inferenceOptionsElement);
		}

		if (!probNet.hasConstraint(OnlyAtemporalVariables.class)) {
			getTemporalOptions(probNet, inferenceOptionsElement);
		}

		root.addContent(inferenceOptionsElement);
	}

	/**
	 * @param probNet
	 * @param root
	 */
	protected void writePolicies(ProbNet probNet, Element root) {
		Element policiesElement = new Element(XMLTags.POLICIES.toString());
		boolean existsAtLeastOnePolicy = false;
		List<Variable> decisionVariables = probNet.getVariables(NodeType.DECISION);
		for (Variable variable : decisionVariables) {
			Node node = probNet.getNode(variable);
			if (node.hasPolicy()) {
				existsAtLeastOnePolicy = true;
				List<Potential> decisionPotentials = node.getPotentials();
				for (Potential decisionPotential : decisionPotentials) {
					Element potentialElement = new Element(XMLTags.POTENTIAL.toString());
					getPotential(probNet, decisionPotential, potentialElement);
					potentialElement.setAttribute(XMLAttributes.ROLE.toString(),
							decisionPotential.getPotentialRole().toString());
					policiesElement.addContent(potentialElement);
				}
			}
		}
		if (existsAtLeastOnePolicy) {
			root.addContent(policiesElement);
		}
	}

	/**
	 * Writes evidence nodes into XML format
	 *
	 * @param evidence
	 *            . <code>ArrayList</code> of <code>EvidenceCase</code>
	 * @param root
	 *            . <code>Element</code>
	 */
	protected void writeEvidence(ProbNet probNet, List<EvidenceCase> evidence, Element root) {
		if (evidence != null && !evidence.isEmpty()) {
			removeMissingVariablesFromEvidence(probNet, evidence);
			Element evidenceElement = new Element(XMLTags.EVIDENCE.toString());
			for (EvidenceCase evidenceCase : evidence) {
				Element evidenceCaseElement = new Element(XMLTags.EVIDENCE_CASE.toString());
				for (Finding finding : evidenceCase.getFindings()) {
					Element findingElement = new Element(XMLTags.FINDING.toString());
					findingElement.setAttribute("variable", finding.getVariable().getName());
					if (finding.getVariable().getVariableType() == VariableType.FINITE_STATES) {
						findingElement.setAttribute("state", finding.getState());
					} else {
						findingElement.setAttribute("numericValue", new Double(finding.getNumericalValue()).toString());
					}
					evidenceCaseElement.addContent(findingElement);
				}
				evidenceElement.addContent(evidenceCaseElement);

			}
			root.addContent(evidenceElement);
		}
	}

	/**
	 * @param probNet
	 *            . <code>ProbNet</code>
	 * @param probNetElement
	 *            . <code>Element</code>
	 */
	protected void getProbNetChildren(ProbNet probNet, Element probNetElement) {
		getAdditionalConstraints(probNet, probNetElement, new Element(XMLTags.ADDITIONAL_CONSTRAINTS.toString()));
		getProbNetComment(probNet, probNetElement, new Element(XMLTags.COMMENT.toString()));
		getDecisionCriteria(probNet, probNetElement, new Element(XMLTags.DECISION_CRITERIA.toString()));
		getAgents(probNet, probNetElement, new Element(XMLTags.AGENTS.toString()));
		getLanguage(probNet, probNetElement, new Element(XMLTags.LANGUAGE.toString()));
		getTemporaUnit(probNet, probNetElement);
		getAdditionalProperties(probNet, probNetElement, new Element(XMLTags.ADDITIONAL_PROPERTIES.toString()));
		getVariables(probNet, probNetElement, new Element(XMLTags.VARIABLES.toString()));
		getLinks(probNet, probNetElement, new Element(XMLTags.LINKS.toString()));
		getPotentials(probNet, probNetElement, new Element(XMLTags.POTENTIALS.toString()));

		// OOPN start
		if (probNet instanceof OOPNet)
			getOOPN((OOPNet) probNet, probNetElement, new Element(XMLTags.OOPN.toString()));
		// OOPN end
	}

	protected void getTemporaUnit(ProbNet probNet, Element probNetElement) {
		if (!probNet.hasConstraint(OnlyAtemporalVariables.class) && probNet.getCycleLength() != null) {
			Element temporalUnitElement = new Element(XMLTags.TIME_UNIT.toString());
			temporalUnitElement.setAttribute(XMLAttributes.UNIT.toString(),
					probNet.getCycleLength().getUnit().toString());
			temporalUnitElement.setAttribute(XMLTags.VALUE.toString(),
					String.valueOf(probNet.getCycleLength().getValue()));
			probNetElement.addContent(temporalUnitElement);
		}

	}

	/**
	 * @param probNet
	 *            . <code>ProbNet</code>
	 * @param probNetElement
	 *            . <code>Element</code>
	 * @param additionalPropertiesElement
	 *            . <code>Properties</code>
	 */
	protected void getAdditionalProperties(ProbNet probNet, Element probNetElement,
										   Element additionalPropertiesElement) {
		for (String propertyName : probNet.additionalProperties.keySet()) {
			if (probNet.additionalProperties.get(propertyName) != null) {
				Element propertyElement = new Element(XMLTags.PROPERTY.toString());
				propertyElement.setAttribute(XMLAttributes.NAME.toString(), propertyName);
				propertyElement.setAttribute(XMLAttributes.VALUE.toString(),
						probNet.additionalProperties.get(propertyName).toString());
				additionalPropertiesElement.addContent(propertyElement);
			}
		}
		probNetElement.addContent(additionalPropertiesElement);
	}

	/**
	 * @param probNet
	 *            . <code>ProbNet</code>
	 * @param probNetElement
	 *            . <code>Element</code>
	 * @param agentsElement
	 *            . <code>Element</code>
	 */
	protected void getAgents(ProbNet probNet, Element probNetElement, Element agentsElement) {
		List<StringWithProperties> agents = probNet.getAgents();
		if (agents != null && !agents.isEmpty()) {
			for (int i = 0; i < agents.size(); i++) {
				getAgent(agentsElement, new Element(XMLTags.AGENT.toString()), agents.get(i).getString(),
						agents.get(i).getAdditionalProperties());
			}

			/*
			 * for (String agentName : agents.getValuesInAString()) {
			 * getAgent(agentsElement, new Element(XMLTags.AGENT.toString()),
			 * agentName, agents.getProperties(agentName)); }
			 */
			probNetElement.addContent(agentsElement);
		}
	}

	/**
	 * @param agentsElement
	 *            . <code>Element</code>
	 * @param agentElement
	 *            . <code>Element</code>
	 * @param agentName
	 *            . <code>String</code>
	 * @param properties
	 *            . <code>Properties</code>
	 */
	protected void getAgent(Element agentsElement, Element agentElement, String agentName,
							Properties properties) {
		agentElement.setAttribute(XMLAttributes.NAME.toString(), agentName);
		if (properties != null && properties.size() > 0) {
			Element additionalPropertiesElement = getAdditionalPropertiesElement(properties);
			agentElement.addContent(additionalPropertiesElement);
		}
	}

	/**
	 * @param probNet
	 * @param probNetElement
	 * @param decisionCriteriaElement
	 */
	protected void getDecisionCriteria(ProbNet probNet, Element probNetElement, Element decisionCriteriaElement) {
		List<Criterion> decisionCritera = probNet.getDecisionCriteria();
		if (decisionCritera != null && !decisionCritera.isEmpty()) {
			for (int i = 0; i < decisionCritera.size(); i++) {
				getCriterion(
						decisionCriteriaElement,
						new Element(XMLTags.CRITERION.toString()),
						decisionCritera.get(i).getCriterionName(),
						decisionCritera.get(i).getCriterionUnit());

			}
			probNetElement.addContent(decisionCriteriaElement);
		}
	}

	/**
	 * @param decisionCriteriaElement
	 * @param criterionElement
	 * @param criterionName
	 * @param criterionUnit
	 */
	protected void getCriterion(Element decisionCriteriaElement, Element criterionElement,
								String criterionName, String criterionUnit) {
		criterionElement.setAttribute(XMLAttributes.NAME.toString(), criterionName);
		if (criterionUnit != null) {
			criterionElement.setAttribute(XMLAttributes.UNIT.toString(), criterionUnit);
		}
		decisionCriteriaElement.addContent(criterionElement);
	}

	/**
	 * @param properties
	 * @return Element
	 */
	protected Element getAdditionalPropertiesElement(Properties properties) {
		Element additionalPropertiesElement = new Element(XMLTags.ADDITIONAL_PROPERTIES.toString());
		for (String propertyName : properties.getKeySet()) {
			Element propertyElement = new Element(XMLTags.PROPERTY.toString());
			propertyElement.setAttribute(XMLAttributes.NAME.toString(), propertyName);
			propertyElement.setAttribute(XMLAttributes.VALUE.toString(), properties.get(propertyName).toString());
			additionalPropertiesElement.addContent(propertyElement);
		}
		return additionalPropertiesElement;
	}

	/**
	 * @param probNet
	 * @param probNetElement
	 * @param constraintsElement
	 */
	protected void getAdditionalConstraints(ProbNet probNet, Element probNetElement, Element constraintsElement) {
		List<PNConstraint> constraints = probNet.getAdditionalConstraints();
		NetworkType networkType = probNet.getNetworkType();
		if (constraints.size() > 1) {
			for (int i = 1; i < constraints.size(); i++) {
				// TODO To implement the getArguments method in constraints
				/*
				 * if (constraints.get(i).getArguments() != null){ close =
				 * false; }
				 */
				PNConstraint constraint = constraints.get(i);
				if (!networkType.isApplicableConstraint(constraint)) {
					constraintsElement.addContent(new Element(XMLTags.CONSTRAINT.toString())
							.setAttribute(XMLAttributes.NAME.toString(), constraint.toString()));
				}
				// To be extended here when the arguments of the restrictions
				// are available
				// TODO Eliminar XMLBasicConstraints y XMLCompoundConstraints
				// TODO revisar que el toString de cada constraint sea correcto
			}
			probNetElement.addContent(constraintsElement);
		}
	}

	protected void getProbNetComment(ProbNet probNet, Element probNetElement, Element commentElement) {
		if (probNet.getComment() != null && !probNet.getComment().isEmpty()) {
			CDATA cdata = new CDATA(probNet.getComment());
			/*
			 * probNetElement.addContent( commentElement.setText( probNet.
			 * getComment() ) );
			 */
			commentElement.setAttribute(XMLAttributes.SHOW_COMMENT.toString(),
					String.valueOf(probNet.getShowCommentWhenOpening()));
			probNetElement.addContent(commentElement.addContent(cdata));
		}
	}

	/**
	 * @param probNet
	 * @param probNetElement
	 * @param languageElement
	 */
	protected void getLanguage(ProbNet probNet, Element probNetElement, Element languageElement) {
		if (probNet.additionalProperties.get(XMLTags.LANGUAGE.toString()) != null)
			probNetElement.addContent(
					languageElement.setText(probNet.additionalProperties.get(XMLTags.LANGUAGE.toString()).toString()));
	}

	/**
	 * @param probNet
	 * @param probNetElement
	 * @param variablesElement
	 */
	protected void getVariables(ProbNet probNet, Element probNetElement, Element variablesElement) {
		if (probNet.getNumNodes() > 0) {
			for (Node node : probNet.getNodes()) {
				getVariable(variablesElement, new Element(XMLTags.VARIABLE.toString()), node);
			}
			probNetElement.addContent(variablesElement);
		}
	}

	/**
	 * @param variablesElement
	 * @param variableElement
	 * @param node
	 */
	protected void getVariable(Element variablesElement, Element variableElement, Node node) {
		writeVariableName(node.getVariable(), variableElement);
		String variableType = node.getVariable().getVariableType().toString();
		variableElement.setAttribute(XMLAttributes.TYPE.toString(), variableType);
		String nodeType = node.getNodeType().toString();
		variableElement.setAttribute(XMLAttributes.ROLE.toString(), nodeType);
		// OOPN start
		if (node.isInput()) {
			variableElement.setAttribute(XMLAttributes.IS_INPUT.toString(), String.valueOf(node.isInput()));
		}
		// OOPN end
		getVariableChildren(variableElement, node);
		variablesElement.addContent(variableElement);
	}

	/**
	 * @param variableElement
	 * @param node
	 */
	protected void getVariableChildren(Element variableElement, Node node) {
		getCommment(variableElement, node);
		// TODO verificar que las coordenadas sean validas no null
		getCoordinates(variableElement, node);
		getAdditionalProperties(variableElement, new Element(XMLTags.ADDITIONAL_PROPERTIES.toString()), node);
		if (node.getVariable().getVariableType() != VariableType.FINITE_STATES) {
			String unit = node.getVariable().getUnit().getString();
			if (unit != null) {
				Element unitElement = new Element(XMLTags.UNIT.toString());
				unitElement.setText(String.valueOf(unit));
				variableElement.addContent(unitElement);
			}
			Element precisionElement = new Element(XMLTags.PRECISION.toString());
			double precision = node.getVariable().getPrecision();
			precisionElement = precisionElement.setText(String.valueOf(precision));
			variableElement.addContent(precisionElement);
		}
		getAlwaysObservedAttribute(variableElement, node);
		NodeType nodeType = node.getNodeType();
		if (nodeType == NodeType.UTILITY) {
			Element decisionCriterionElement = new Element(XMLTags.CRITERION.toString());
			getDecisionCriterion(variableElement, decisionCriterionElement, node);
		}
		VariableType variableType = node.getVariable().getVariableType();

		// Write states
		if (variableType == VariableType.FINITE_STATES || variableType == VariableType.DISCRETIZED) {
			getStates(variableElement, new Element(XMLTags.STATES.toString()), node);
		}

		// Write intervals
		if (variableType == VariableType.NUMERIC || variableType == VariableType.DISCRETIZED) {
			if (node.getNodeType() != NodeType.UTILITY) {
				getThresholds(variableElement, new Element(XMLTags.THRESHOLDS.toString()),
						node.getVariable().getPartitionedInterval());
			}
		}

	}

	/**
	 * @param variableElement
	 * @param thresholdsElement
	 * @param partitionedInterval
	 */
	protected void getThresholds(Element variableElement, Element thresholdsElement,
								 PartitionedInterval partitionedInterval) {
		if (partitionedInterval.getLimits().length > 0) {
			int i = 0;
			for (double limit : partitionedInterval.getLimits()) {
				getThresholdElement(limit, thresholdsElement, partitionedInterval, i++);
			}
			variableElement.addContent(thresholdsElement);
		}
	}

    /**
     * Creates a threshold that belongs to a partitioned interval.
     * @param limit
     * @param thresholdsElement
     * @param partitionedInterval
     * @param i
     */
    protected void getThresholdElement(double limit, Element thresholdsElement, PartitionedInterval partitionedInterval, int i) {
		Element thresholdElement = new Element(XMLTags.THRESHOLD.toString());
		thresholdElement.setAttribute(XMLAttributes.VALUE.toString(), String.valueOf(limit));
		thresholdElement.setAttribute(XMLAttributes.BELONGS_TO.toString(), partitionedInterval.getBelongsTo(i));
		thresholdsElement.addContent(thresholdElement);
	}

	/**
	 *
	 * @param variableElement
	 * @param statesElement
	 * @param node
	 */
	protected void getStates(Element variableElement, Element statesElement, Node node) {
		// TODO revisar el caso para variables numéricas
		for (State singleState : node.getVariable().getStates()) {
			statesElement.addContent(new Element(XMLTags.STATE.toString()).setAttribute(XMLAttributes.NAME.toString(),
					singleState.getName()));
		}
		variableElement.addContent(statesElement);
	}

	/**
	 * Writes Decision Criterion in the PGMX
	 *
	 * @param variableElement
	 * @param decisionCriterion
	 * @param node
	 */
	protected void getDecisionCriterion(Element variableElement, Element decisionCriterion, Node node) {
		if (node.getVariable().getDecisionCriterion() != null) {
			decisionCriterion.setAttribute(XMLAttributes.NAME.toString(),
					node.getVariable().getDecisionCriterion().getCriterionName());
			variableElement.addContent(decisionCriterion);
		}
	}

	/**
	 *
	 * @param variableElement
	 * @param additionalElement
	 * @param node
	 */
	protected void getAdditionalProperties(Element variableElement, Element additionalElement, Node node) {
		if (!node.getPurpose().isEmpty()) {
			Element propertyElement = new Element(XMLTags.PROPERTY.toString());
			propertyElement.setAttribute(XMLAttributes.NAME.toString(), XMLTags.PURPOSE.toString());
			propertyElement.setAttribute(XMLAttributes.VALUE.toString(), node.getPurpose());
			additionalElement.addContent(propertyElement);
		}
		if (node.getRelevance() != Node.defaultRelevance) {
			Element propertyElement = new Element(XMLTags.PROPERTY.toString());
			propertyElement.setAttribute(XMLAttributes.NAME.toString(), XMLTags.RELEVANCE.toString());
			propertyElement.setAttribute(XMLAttributes.VALUE.toString(), String.valueOf(node.getRelevance()));
			additionalElement.addContent(propertyElement);
		}
		for (String propertyKey : node.additionalProperties.keySet()) {
			String propertyValue = node.additionalProperties.get(propertyKey);
			Element propertyElement = new Element(XMLTags.PROPERTY.toString());
			propertyElement.setAttribute(XMLAttributes.NAME.toString(), propertyKey);
			propertyElement.setAttribute(XMLAttributes.VALUE.toString(), propertyValue);
			additionalElement.addContent(propertyElement);
		}
		if (additionalElement.getChildren().size() > 0)
			variableElement.addContent(additionalElement);
	}

	/**
	 *
	 * @param variableElement
	 * @param node
	 */
	protected void getCoordinates(Element variableElement, Node node) {
		Element coordinatesElement = new Element(XMLTags.COORDINATES.toString());
		coordinatesElement.setAttribute(XMLAttributes.X.toString(),
				String.valueOf(new Double(node.getCoordinateX()).intValue()));
		coordinatesElement.setAttribute(XMLAttributes.Y.toString(),
				String.valueOf(new Double(node.getCoordinateY()).intValue()));
		variableElement.addContent(coordinatesElement);
	}

	/**
	 *
	 * @param variableElement
	 * @param node
	 */
	protected void getCommment(Element variableElement, Node node) {
		if (node.getComment() != null && !node.getComment().isEmpty()) {
			CDATA cdata = new CDATA(node.getComment());
			variableElement.addContent(new Element(XMLTags.COMMENT.toString()).setContent(cdata));
		}
	}

	/**
	 *
	 * @param variableElement
	 * @param node
	 */
	protected void getAlwaysObservedAttribute(Element variableElement, Node node) {
		if (node.isAlwaysObserved()) {
			variableElement.addContent(new Element(XMLTags.ALWAYS_OBSERVED.toString()));
		}
	}

	/**
	 *
	 * @param probNet
	 * @param probNetElement
	 * @param linksElement
	 */
	protected void getLinks(ProbNet probNet, Element probNetElement, Element linksElement) {
		List<Link<Node>> links = probNet.getLinks();
		if (links.size() > 0) {
			for (Link<Node> link : links) {
				Element linkElement = new Element(XMLTags.LINK.toString());
				Element variableElement1 = new Element(XMLTags.VARIABLE.toString());
				Variable variable1 = link.getNode1().getVariable();
				writeVariableName(variable1, variableElement1);
				Element variableElement2 = new Element(XMLTags.VARIABLE.toString());
				Variable variable2 = link.getNode2().getVariable();
				writeVariableName(variable2, variableElement2);
				linkElement.addContent(variableElement1);
				linkElement.addContent(variableElement2);
				/*
				 * linkElement.setAttribute( XMLAttributes.VAR1.toString(), ( (
				 * Node )link.getNode1().getObject() ).getName() );
				 * linkElement.setAttribute( XMLAttributes.VAR2.toString(), ( (
				 * Node)link.getNode2().getObject() ).getName() );
				 */
				linkElement.setAttribute(XMLAttributes.DIRECTED.toString(), String.valueOf(link.isDirected()));
				if (link.hasRestrictions()) {
					getLinkRestriction(link, linkElement);
				}
				if (link.hasRevealingConditions()) {
					getRevelationConditions(link, linkElement);
				}
				// linkElement.addContent(varaibleElement)
				// TODO Write comment
				// TODO Write label
				// TODO Write additional additionalProperties
				linksElement.addContent(linkElement);
			}
			probNetElement.addContent(linksElement);
		}
	}

	/****
	 * Writes the revelation conditions for the link.
	 *
	 * @param link
	 * @param linkElement
	 */
	protected void getRevelationConditions(Link<Node> link, Element linkElement) {
		Node node = link.getNode1();
		VariableType varType = node.getVariable().getVariableType();
		Element revelationConditions = new Element(XMLTags.REVELATION_CONDITIONS.toString());
		if (varType == VariableType.NUMERIC) {
			List<PartitionedInterval> intervals = link.getRevealingIntervals();
			for (PartitionedInterval partitionedInterval : intervals) {
				if (partitionedInterval.getLimits().length > 0) {
					int i = 0;
					for (double limit : partitionedInterval.getLimits()) {
					    getThresholdElement(limit, revelationConditions, partitionedInterval, i++);
					}
				}
			}
		} else {

			List<State> states = link.getRevealingStates();
			for (State state : states) {
				Element stateElement = new Element(XMLTags.STATE.toString());
				stateElement.setAttribute(XMLAttributes.NAME.toString(), state.getName());
				revelationConditions.addContent(stateElement);
			}
		}
		linkElement.addContent(revelationConditions);
	}

	/*****
	 * Writes the link restriction
	 *
	 * @param link
	 * @param linkElement
	 */
	protected void getLinkRestriction(Link<Node> link, Element linkElement) {
		double[] table = ((TablePotential) link.getRestrictionsPotential()).values;

		boolean hasRestriction = false;
		for (int i = 0; i < table.length; i++) {
			if (table[i] == 0.0) {
				hasRestriction = true;
			}
		}
		if (hasRestriction) {
			Potential potential = link.getRestrictionsPotential();
			Element restrictionPotential = new Element(XMLTags.POTENTIAL.toString());
			String potentialType = potential.getClass().getAnnotation(PotentialType.class).name();
			PotentialRole potentialRole = PotentialRole.LINK_RESTRICTION;
			restrictionPotential.setAttribute(XMLAttributes.TYPE.toString(), potentialType);
			restrictionPotential.setAttribute(XMLAttributes.ROLE.toString(), potentialRole.toString());
			Element variables = new Element(XMLTags.VARIABLES.toString());
			Element variable1 = new Element(XMLTags.VARIABLE.toString());
			variable1.setAttribute(XMLAttributes.NAME.toString(), potential.getVariable(0).getName());
			Element variable2 = new Element(XMLTags.VARIABLE.toString());
			variable2.setAttribute(XMLAttributes.NAME.toString(), potential.getVariable(1).getName());
			variables.addContent(variable1);
			variables.addContent(variable2);
			Element valuesElement = new Element(XMLTags.VALUES.toString())
					.setText(getValuesInAString(((TablePotential) link.getRestrictionsPotential()).values));
			restrictionPotential.addContent(variables);
			restrictionPotential.addContent(valuesElement);
			linkElement.addContent(restrictionPotential);
		}
	}

	/**
	 * @param probNet
	 * @param probNetElement
	 * @param potentialsElement
	 */
	protected void getPotentials(ProbNet probNet, Element probNetElement, Element potentialsElement) {
		// HashMap of declared TablePotentials
		List<Potential> potentials = probNet.getPotentials();
		for (Potential potential : potentials) {
			Variable potentialVariable = potential.getVariable(0);
			// Do not write here policies
			if ((probNet.getNode(potentialVariable).getNodeType() != NodeType.DECISION)
					&& (potential.getPotentialRole() != PotentialRole.POLICY)) {
				Element potentialElement = new Element(XMLTags.POTENTIAL.toString());
				getPotential(probNet, potential, potentialElement);
				setPotentialRole(potentialElement, potential); // PGMX 0.2 difference with the next version: potential role is written in the potential.
				potentialsElement.addContent(potentialElement);
			}
		}
		probNetElement.addContent(potentialsElement);
	}

	/**
	 * Writes potential role in PGMX description.
	 * @param potentialElement
	 * @param potential
	 */
	protected void setPotentialRole(Element potentialElement, Potential potential) {
		PotentialRole potentialRole = potential.getPotentialRole();
		String oldPotentialRoleString = potentialRole.toString();
		if (potentialRole.equals(PotentialRole.UNSPECIFIED) || potentialRole.equals("")) {
			oldPotentialRoleString = "utility";
		}
		potentialElement.setAttribute(XMLAttributes.ROLE.toString(), oldPotentialRoleString);
	}

	/**
	 * @param probNet
	 * @param potential
	 * @param potentialElement
	 */
	protected void getPotential(ProbNet probNet, Potential potential, Element potentialElement) {
		getPotentialAttributesAndVariables(probNet, potential, potentialElement);
		getPotentialBody(probNet, potential, potentialElement);
	}

	/**
	 *
	 * @param probNet
	 * @param potential
	 * @param potentialElement
	 */
	protected void getPotentialAttributesAndVariables(ProbNet probNet, Potential potential, Element potentialElement) {
		// TODO - Change in new version. compatibility with this version of pgmx  format
		if (potential instanceof ExactDistrPotential) {
			ExactDistrPotential exactDistrPotential = (ExactDistrPotential) potential;
			potential = exactDistrPotential.getTablePotential();
			Variable utilityVariable = exactDistrPotential.getChildVariable(); // it could be null in Branches potentials
			getUtilityElement(potentialElement, utilityVariable);

		} else if (potential instanceof SumPotential || potential instanceof ProductPotential) {
			if (!potential.getPotentialRole().equals(PotentialRole.CONDITIONAL_PROBABILITY)) {
				Variable utilityVariable = potential.getVariable(0); // it could be null in Branches potentials
				potential.removeVariable(utilityVariable);
				getUtilityElement(potentialElement, utilityVariable);
			}
		}

		String potentialType = potential.getClass().getAnnotation(PotentialType.class).name();
		potentialElement.setAttribute(XMLAttributes.TYPE.toString(), potentialType);

		// TODO add function attribute

		getPotentialComment(potential, potentialElement);

		// TODO add aditionalProperties child

		getPotentialVariables(potential, potentialElement, probNet);
	}

	/**
	 * Adds an utility element when utility the utility variable exists.
	 * @param potentialElement
	 * @param utilityVariable
	 */
	protected void getUtilityElement(Element potentialElement, Variable utilityVariable) {
		if (utilityVariable != null) {
			Element utilityElement = new Element(XMLTags.UTILITY_VARIABLE.toString());
			writeVariableName(utilityVariable, utilityElement);
			potentialElement.addContent(utilityElement);
		}
	}

	/**
	 * @param potential
	 * @param potentialElement
	 */
	protected void getPotentialComment(Potential potential, Element potentialElement) {
		if (potential.getComment() != null && !potential.getComment().isEmpty()) {
			Element commentElement = new Element(XMLTags.COMMENT.toString());
			CDATA cdata = new CDATA(potential.getComment());
			potentialElement.addContent(commentElement.setContent(cdata));
		}
	}

	/**
	 * Add the variables to the potentialElement and, whether it exists, the utility variable
	 * @param potential
	 * @param potentialElement
	 * @param probNet
	 */
	protected void getPotentialVariables(Potential potential, Element potentialElement, ProbNet probNet) {
		List<Variable> potentialVariables = potential.getVariables();
		if (!potentialVariables.isEmpty()
				&& probNet.getNode(potentialVariables.get(0)).getNodeType().equals(NodeType.UTILITY)) {
			Variable utilityVariable = potential.getVariable(0);
			//potential.removeVariable(utilityVariable);
			// it could be null in Branches potentials
			if (utilityVariable != null) {
				Element utilityElement = new Element(XMLTags.UTILITY_VARIABLE.toString());
				writeVariableName(utilityVariable, utilityElement);
				potentialElement.addContent(utilityElement);
			}
			potentialVariables.remove(utilityVariable);
		}

		if (!potentialVariables.isEmpty()) {
			writePotentialVariables(potentialVariables, potentialElement);
		}
	}

	/**
	 *
	 * @param probNet
	 * @param potential
	 * @param potentialElement
	 */
	protected void getPotentialBody(ProbNet probNet, Potential potential, Element potentialElement) {
		if (potential instanceof ExactDistrPotential) {
			getValuesTablePotential(((ExactDistrPotential) potential).getTablePotential(), potentialElement);
		} else if (potential instanceof TablePotential) {
			getValuesTablePotential((TablePotential)potential, potentialElement);
		} else if (potential instanceof TreeADDPotential) {
			getTreeAddPotential((TreeADDPotential)potential, potentialElement, probNet);
		} else if (potential instanceof ICIPotential) {
			getICIPotential((ICIPotential) potential, potentialElement);
		} else if (potential instanceof FunctionPotential) {
			potentialElement.addContent(getFunctionElement(((FunctionPotential) potential).getFunction()));
		} else if (potential instanceof GLMPotential) { // Another type of GLMPotential, distinct from FunctionPotential
			if (potential instanceof WeibullHazardPotential) {
				if (potential instanceof ExponentialHazardPotential) {
					getLogHazardPotential(potentialElement, (ExponentialHazardPotential) potential);
				} else {
					getWeibullHazardPotential((WeibullHazardPotential) potential, potentialElement);
				}
			}
			getGLMPotential(potentialElement, (GLMPotential) potential);
		} else if (potential instanceof DeltaPotential) {
			getDeltaPotential(potentialElement, potential);
		} else if (potential instanceof BinomialPotential) {
			getBinomialPotential(potentialElement, potential);
		}
	}


	/**
	 * @param potential
	 * @param potentialElement
	 */
	protected void getValuesTablePotential(TablePotential potential, Element potentialElement) {
		Element valuesElement;
		valuesElement = new Element(XMLTags.VALUES.toString());
		valuesElement.setText(getValuesInAString(((TablePotential) potential).values));
		// Write table values to the XML file
		potentialElement.addContent(valuesElement);
		if (((TablePotential) potential).getUncertainValues() != null) {
			Element uncertainValuesElement = getUncertainValuesElement(potential);
			potentialElement.addContent(uncertainValuesElement);
		}
	}

	/**
	 * @param treeADDPotential
	 * @param potentialElement
	 * @param probNet
	 */
	protected void getTreeAddPotential(TreeADDPotential treeADDPotential, Element potentialElement, ProbNet probNet) {
		// Root variable
		Variable rootVariable = treeADDPotential.getRootVariable();
		Element topVarElement = new Element(XMLTags.TOP_VARIABLE.toString());
		writeVariableName(rootVariable, topVarElement); // Write the root variable at the potential's top
		potentialElement.addContent(topVarElement);

		// Branches
		Element branchesElement = new Element(XMLTags.BRANCHES.toString());
		for (TreeADDBranch branch : treeADDPotential.getBranches()) {
			branchesElement.addContent(getTreeADDBranch(branch, rootVariable, probNet)); // Recursive branch writing
		}
		potentialElement.addContent(branchesElement);
	}

	/**
	 * @param iciPotential
	 * @param potentialElement
	 */
	protected void getICIPotential(ICIPotential iciPotential, Element potentialElement) {
		potentialElement.setAttribute(XMLAttributes.TYPE.toString(), "ICIModel");
		// Model Element
		Element modelElement = new Element(XMLTags.MODEL.toString());
		modelElement.setText(iciPotential.getClass().getAnnotation(PotentialType.class).name());
		potentialElement.addContent(modelElement);
		// Subpotentials element
		Element subpotentialsElement = new Element(XMLTags.SUBPOTENTIALS.toString());
		Variable conditionedVariable = iciPotential.getVariables().get(0);
		// Noisy parameters
		for (int i = 1; i < iciPotential.getNumVariables(); ++i) {
			Variable parentVariable = iciPotential.getVariables().get(i);
			Element potentialChildElement = new Element(XMLTags.POTENTIAL.toString());
			potentialChildElement.setAttribute("type", "Table");
			Element variablesElement = new Element(XMLTags.VARIABLES.toString());
			Element conditionedVariableElement = new Element(XMLTags.VARIABLE.toString());
			conditionedVariableElement.setAttribute("name", conditionedVariable.getName());
			Element parentVariableElement = new Element(XMLTags.VARIABLE.toString());
			parentVariableElement.setAttribute("name", parentVariable.getName());
			variablesElement.addContent(conditionedVariableElement);
			variablesElement.addContent(parentVariableElement);
			Element parameterValuesElement = new Element(XMLTags.VALUES.toString());
			parameterValuesElement.setText(getValuesInAString(iciPotential.getNoisyParameters(parentVariable)));
			potentialChildElement.addContent(variablesElement);
			potentialChildElement.addContent(parameterValuesElement);
			subpotentialsElement.addContent(potentialChildElement);
		}
		// Leaky parameters
		Element potentialChildElement = new Element(XMLTags.POTENTIAL.toString());
		potentialChildElement.setAttribute("type", "Table");
		Element variablesElement = new Element(XMLTags.VARIABLES.toString());
		Element conditionedVariableElement = new Element(XMLTags.VARIABLE.toString());
		conditionedVariableElement.setAttribute("name", conditionedVariable.getName());
		variablesElement.addContent(conditionedVariableElement);
		Element parameterValuesElement = new Element(XMLTags.VALUES.toString());
		parameterValuesElement.setText(getValuesInAString(iciPotential.getLeakyParameters()));
		potentialChildElement.addContent(variablesElement);
		potentialChildElement.addContent(parameterValuesElement);
		subpotentialsElement.addContent(potentialChildElement);
		potentialElement.addContent(subpotentialsElement);
	}

	/**
	 * @param weibullPotential
	 * @param potentialElement
	 */
	protected void getWeibullHazardPotential(WeibullHazardPotential weibullPotential, Element potentialElement) {
		Variable timeVariable = weibullPotential.getTimeVariable();
		if (timeVariable != null) {
			Element timeVariableElement = new Element(XMLTags.TIME_VARIABLE.toString());
			timeVariableElement.setAttribute(XMLAttributes.NAME.toString(), timeVariable.getBaseName() + "");
			timeVariableElement.setAttribute(XMLAttributes.TIMESLICE.toString(), timeVariable.getTimeSlice() + "");
			potentialElement.addContent(timeVariableElement);
		}
		if (!weibullPotential.isLog()) {
			Element logElement = new Element(XMLTags.LOG.toString());
			logElement.addContent("" + weibullPotential.isLog());
			potentialElement.addContent(logElement);
		}
	}

	/**
	 * @param potentialElement
	 * @param exponentialHazardPotential
	 */
	protected void getLogHazardPotential(Element potentialElement, ExponentialHazardPotential exponentialHazardPotential) {
		if (!exponentialHazardPotential.isLog()) {
			Element logElement = new Element(XMLTags.LOG.toString());
			logElement.addContent("" + exponentialHazardPotential.isLog());
			potentialElement.addContent(logElement);
		}
	}

	/**
	 * @param potentialElement
	 * @param potential
	 */
	protected void getDeltaPotential(Element potentialElement, Potential potential) {
		DeltaPotential deltaPotential = (DeltaPotential) potential;
		if (deltaPotential.getConditionedVariable().getVariableType() == VariableType.NUMERIC) {
			Element numericValueElement = new Element(XMLTags.NUMERIC_VALUE.toString());
			numericValueElement.setText(String.valueOf(deltaPotential.getNumericValue()));
			potentialElement.addContent(numericValueElement);
		} else {
			Element stateElement = new Element(XMLTags.STATE.toString());
			stateElement.setText(deltaPotential.getState().getName());
			potentialElement.addContent(stateElement);
		}
	}

	protected void getBinomialPotential(Element potentialElement, Potential potential) {
		BinomialPotential binomialPotential = (BinomialPotential) potential;
		Element NumberOfCasesElement = new Element(XMLTags.NUMBER_OF_CASES.toString());
		NumberOfCasesElement.setText(String.valueOf(binomialPotential.getN()));
		potentialElement.addContent(NumberOfCasesElement);
		Element thetaElement = new Element(XMLTags.THETA.toString());
		thetaElement.setText(String.valueOf(binomialPotential.gettheta()));
		potentialElement.addContent(thetaElement);
	}

	/**
	 * @param coefficients
	 * @return new Element
	 */
	protected Element getCoefficientsElement(double[] coefficients) {
		Element coefficientsElement = new Element(XMLTags.COEFFICIENTS.toString());
		coefficientsElement.setText(getValuesInAString(coefficients));
		return coefficientsElement;
	}

	/**
	 * @param covariates
	 * @return new Element
	 */
	protected Element getCovariatesElement(String[] covariates) {
		Element covariatesElement = new Element(XMLTags.COVARIATES.toString());
		for (String covariate : covariates) {
			Element covariateElement = new Element(XMLTags.COVARIATE.toString());
			covariateElement.setText(covariate);
			covariatesElement.addContent(covariateElement);
		}
		return covariatesElement;
	}

	/**
	 * @param function
	 * @return new Element
	 */
	protected Element getFunctionElement(String function) {
		Element functionElement = new Element(XMLTags.FUNCTION.toString());
		functionElement.setText(function);
		return functionElement;
	}

	/**
	 * @param covarianceMatrix
	 * @return
	 */
	protected Element getCovarianceMatrixElement(double[] covarianceMatrix) {
		Element covarianceMatrixElement = new Element(XMLTags.COVARIANCE_MATRIX.toString());
		covarianceMatrixElement.setText(getValuesInAString(covarianceMatrix));
		return covarianceMatrixElement;
	}

	/**
	 * @param choleskyDecomposition
	 * @return
	 */
	protected Element getCholeskyDecompositionElement(double[] choleskyDecomposition) {
		Element choleskyDecompositionElement = new Element(XMLTags.CHOLESKY_DECOMPOSITION.toString());
		choleskyDecompositionElement.setText(getValuesInAString(choleskyDecomposition));
		return choleskyDecompositionElement;
	}

	/**
	 *
	 * @param xmlElement
	 * @param potential
	 */
	protected void getGLMPotential(Element xmlElement, GLMPotential potential) {
		xmlElement.addContent(getCoefficientsElement(potential.getCoefficients()));
		xmlElement.addContent(getCovariatesElement(potential.getCovariates()));
		if (potential.getCovarianceMatrix() != null) {
			xmlElement.addContent(getCovarianceMatrixElement(potential.getCovarianceMatrix()));
		} else if (potential.getCholeskyDecomposition() != null) {
			xmlElement.addContent(getCholeskyDecompositionElement(potential.getCholeskyDecomposition()));
		}
	}

	/**
	 * @param potential
	 * @return Element
	 */
	protected Element getUncertainValuesElement(Potential potential) {
		Element uncertainValuesElement = new Element(XMLTags.UNCERTAIN_VALUES.toString());
		UncertainValue[] table = ((TablePotential) potential).getUncertainValues();
		int size = table.length;
		for (int i = 0; i < size; i++) {
			UncertainValue auxValue = table[i];
			Element auxUncertain = getUncertainValueElement(auxValue);
			uncertainValuesElement.addContent(auxUncertain);
		}
		return uncertainValuesElement;
	}

	/**
	 * @param uncertainValue
	 * @return Element
	 */
	protected Element getUncertainValueElement(UncertainValue uncertainValue) {
		Element element = new Element(XMLTags.VALUE.toString());
		if (uncertainValue != null) {
			ProbDensFunction function = uncertainValue.getProbDensFunction();
			String functionName = function.getClass().getAnnotation(ProbDensFunctionType.class).name();
			element.setAttribute(XMLAttributes.DISTRIBUTION.toString(), functionName);
			String nameParam = uncertainValue.getName();
			if (nameParam != null) {
				element.setAttribute(XMLAttributes.NAME.toString(), nameParam);
			}
			element.setText(getValuesInAString(uncertainValue.getProbDensFunction().getParameters()));
		}
		return element;
	}

	/**
	 * @param potentialVariables
	 * @param targetElement
	 */
	protected void writePotentialVariables(List<Variable> potentialVariables, Element targetElement) {
		if (potentialVariables.size() > 0) {
			Element variablesElement = new Element(XMLTags.VARIABLES.toString());
			for (Variable variable : potentialVariables) {
				Element variableElement = new Element(XMLTags.VARIABLE.toString());
				writeVariableName(variable, variableElement);
				variablesElement.addContent(variableElement);
			}
			// Write var names of the table potential to the XML file
			targetElement.addContent(variablesElement);
		}
	}

	/**
	 * Sets variable information in xmlElement
	 * @param variable
	 * @param xmlElement
	 */
	protected void writeVariableName(Variable variable, Element xmlElement) {
		xmlElement.setAttribute(XMLAttributes.NAME.toString(), variable.getBaseName());
		if (variable.getTimeSlice() >= 0) {
			xmlElement.setAttribute(XMLAttributes.TIMESLICE.toString(), String.valueOf(variable.getTimeSlice()));
		}
	}

	/**
	 * @param branch
	 * @param topVariable
	 * @return Element
	 */
	protected Element getTreeADDBranch(TreeADDBranch branch, Variable topVariable, ProbNet probNet) {
		Element branchElement = new Element(XMLTags.BRANCH.toString());
		// Read the branch element

		if (topVariable.getVariableType() == VariableType.NUMERIC) {
			Element intervalElement = new Element(XMLTags.THRESHOLDS.toString());
			// Export the left and right values of the interval. Closed/Open
			// attribute must be taken into account
			Element minThresholdElement = new Element(XMLTags.THRESHOLD.toString());
			minThresholdElement.setAttribute(XMLAttributes.VALUE.toString(),
					String.valueOf(branch.getLowerBound().getLimit()));
			minThresholdElement.setAttribute(XMLAttributes.BELONGS_TO.toString(),
					branch.getLowerBound().belongsToLeft() ? XMLValues.LEFT.toString() : XMLValues.RIGHT.toString());
			intervalElement.addContent(minThresholdElement);
			Element maxThresholdElement = new Element(XMLTags.THRESHOLD.toString());
			maxThresholdElement.setAttribute(XMLAttributes.VALUE.toString(),
					String.valueOf(branch.getUpperBound().getLimit()));
			maxThresholdElement.setAttribute(XMLAttributes.BELONGS_TO.toString(),
					branch.getUpperBound().belongsToLeft() ? XMLValues.LEFT.toString() : XMLValues.RIGHT.toString());
			intervalElement.addContent(maxThresholdElement);

			// Append the information to the element to be exported
			branchElement.addContent(intervalElement);
		} else if (topVariable.getVariableType() == VariableType.FINITE_STATES
				|| topVariable.getVariableType() == VariableType.DISCRETIZED) {
			// TODO: test cardinality of variable states and InnerNode
			List<State> branchStates = branch.getBranchStates();

			Element states = new Element(XMLTags.STATES.toString());
			for (State state : branchStates) {
				String varStateName = state.getName();
				states.addContent(new Element(XMLTags.STATE.toString()).setAttribute(XMLAttributes.NAME.toString(),
						varStateName));
			}
			branchElement.addContent(states);
		}

		// label
		if (branch.getLabel() != null) {
			Element labelElement = new Element(XMLTags.LABEL.toString());
			labelElement.setText(branch.getLabel());
			branchElement.addContent(labelElement);
		}
		if (branch.getReference() != null) {
			Element labelElement = new Element(XMLTags.REFERENCE.toString());
			labelElement.setText(branch.getReference());
			branchElement.addContent(labelElement);

		} else {
			Potential potential = branch.getPotential();
			Element potentialElement = new Element(XMLTags.POTENTIAL.toString());
			// In trees potential role is assumed to be the same as the tree
			// role
			// potential, so it is not necessary to indicate it
			getPotential(probNet, potential, potentialElement);
			branchElement.addContent(potentialElement);
		}
		return branchElement;
	}

	/**
	 * @param probNet
	 * @param inferenceOptionsElement
	 */
	protected void getMulticriteriaOptions(ProbNet probNet, Element inferenceOptionsElement) {
		if (probNet.getInferenceOptions().getMultiCriteriaOptions() != null) {
			Element multicriteriaOptions = new Element(XMLTags.MULTICRITERIA_OPTIONS.toString());
			Element multiCriteriaType = new Element(XMLTags.SELECTED_ANALYSIS_TYPE.toString());
			multiCriteriaType.addContent(
					probNet.getInferenceOptions().getMultiCriteriaOptions().getMulticriteriaType().toString());
			multicriteriaOptions.addContent(multiCriteriaType);

			getUnicriterionOptions(probNet, multicriteriaOptions);
			getCostEffectivenessOptions(probNet, multicriteriaOptions);

			inferenceOptionsElement.addContent(multicriteriaOptions);
		}
	}

	/**
	 * @param probNet
	 * @param multicriteriaOptions
	 */
	protected void getUnicriterionOptions(ProbNet probNet, Element multicriteriaOptions) {
		boolean haveSomeData = false;

		Element unicriterionOptions = new Element(XMLTags.UNICRITERION.toString());
		if (!probNet.getInferenceOptions().getMultiCriteriaOptions().getMainUnit().equals(" ")) {
			Element mainUnit = new Element(XMLTags.UNIT.toString());
			mainUnit.addContent(probNet.getInferenceOptions().getMultiCriteriaOptions().getMainUnit());
			unicriterionOptions.addContent(mainUnit);
			haveSomeData = true;
		}

		if (probNet.getDecisionCriteria() != null && !probNet.getDecisionCriteria().isEmpty()) {
			Element scales = new Element(XMLTags.SCALES.toString());
			for (Criterion criterion : probNet.getDecisionCriteria()) {
				Element scale = new Element(XMLTags.SCALE.toString());
				scale.setAttribute(XMLTags.CRITERION.toString(), criterion.getCriterionName());
                scale.setAttribute(XMLTags.VALUE.toString(), String.valueOf(criterion.getUnicriterizationScale()));
				scales.addContent(scale);
			}
			unicriterionOptions.addContent(scales);
			haveSomeData = true;
		}
		if (haveSomeData) {
			multicriteriaOptions.addContent(unicriterionOptions);
		}
	}

	/**
	 * Writes Cost-Effectivenes Options of the Multricriteria Analysys
	 * @param probNet
	 * @param multicriteriaOptions
	 */
	protected void getCostEffectivenessOptions(ProbNet probNet, Element multicriteriaOptions) {
		if (probNet.getDecisionCriteria() != null && !probNet.getDecisionCriteria().isEmpty()) {
			Element costEffectivenessOptions = new Element(XMLTags.COSTEFFECTIVENESS.toString());
			Element costEffectivenessCriteria = new Element(XMLTags.CE_CRITERIA.toString());

			if (probNet.getDecisionCriteria() != null && !probNet.getDecisionCriteria().isEmpty()) {
				Element scales = new Element(XMLTags.SCALES.toString());
				for (Criterion criterion : probNet.getDecisionCriteria()) {
					Element scale = new Element(XMLTags.SCALE.toString());
					scale.setAttribute(XMLTags.CRITERION.toString(), criterion.getCriterionName());
					scale.setAttribute(XMLTags.VALUE.toString(), String.valueOf(criterion.getCeScale()));
					scales.addContent(scale);
				}
				costEffectivenessOptions.addContent(scales);
			}

			for (Criterion criterion : probNet.getDecisionCriteria()) {
				Element costEffectivenessCriterion = new Element(XMLTags.CE_CRITERION.toString());
				costEffectivenessCriterion.setAttribute(XMLTags.CRITERION.toString(), criterion.getCriterionName());
				costEffectivenessCriterion.setAttribute(XMLTags.VALUE.toString(),
						String.valueOf(criterion.getCECriterion()));
				costEffectivenessCriteria.addContent(costEffectivenessCriterion);
			}
			costEffectivenessOptions.addContent(costEffectivenessCriteria);
			multicriteriaOptions.addContent(costEffectivenessOptions);
		}

	}

	/**
	 * Writes Temporal Options in the PGMX
	 * @param probNet
	 * @param inferenceOptionsElement
	 */
	protected void getTemporalOptions(ProbNet probNet, Element inferenceOptionsElement) {
		Element temporalOptions = new Element(XMLTags.TEMPORAL_OPTIONS.toString());

		Element slices = new Element(XMLTags.SLICES.toString());
		slices.addContent(String.valueOf(probNet.getInferenceOptions().getTemporalOptions().getHorizon()));
		temporalOptions.addContent(slices);

		Element transitionTime = new Element(XMLTags.TRANSITION.toString());
		transitionTime.addContent(probNet.getInferenceOptions().getTemporalOptions().getTransition().toString());
		temporalOptions.addContent(transitionTime);

		Element discounts = new Element(XMLTags.DISCOUNT_RATES.toString());
		for (Criterion criterion : probNet.getDecisionCriteria()) {
			Element discount = new Element(XMLTags.DISCOUNT_RATE.toString());
			discount.setAttribute(XMLTags.CRITERION.toString(), criterion.getCriterionName());
			discount.setAttribute(XMLAttributes.VALUE.toString(), String.valueOf(criterion.getDiscount()));
			discount.setAttribute(XMLAttributes.UNIT.toString(), criterion.getDiscountUnit().toString());
			discounts.addContent(discount);
		}

		temporalOptions.addContent(discounts);
		inferenceOptionsElement.addContent(temporalOptions);
	}

	/**
	 * @param table array of double
	 * @return String
	 */
	protected String getValuesInAString(double[] table) {
		StringBuffer stringBuffer = new StringBuffer();
		for (double value : table) {
			stringBuffer.append(String.valueOf(value) + " ");
		}
		return stringBuffer.toString();
	}

	/**
	 * @param probNet
	 * @return String
	 */
	protected String getXMLNetworkType(ProbNet probNet) {
		NetworkTypeManager networkTypeManager = new NetworkTypeManager();
		return networkTypeManager.getName(probNet.getNetworkType());
	}

	/**
	 * transform a HTML string in a pure String by a full substitution of the
	 * special HTML characters "&lt;" and "&gt;" in the equivalent "SymbolLT" and
	 * "SymbolGT" Please, pay attention that the equivalent format "&amp;lt;" and
	 * "&amp;gt;" are not used here as JDOM is using the character "&amp;" to start a
	 * definition of an entity Ref class, so we need to avoid it.
	 */
	protected String htmlToText(String htmlSection) {
		String result = htmlSection;
		result = result.replaceAll("<", "SymbolLT");
		result = result.replaceAll(">", "SymbolGT");
		return result;
	}

	// TODO OOPN start
	protected void getOOPN(OOPNet OOPNet, Element probNetElement, Element oonElement) {
		if (OOPNet.getClasses().size() > 0) {
			Element classesElement = new Element(XMLTags.CLASSES.toString());
			for (String className : ((OOPNet) OOPNet).getClasses().keySet()) {
				Element classElement = new Element(XMLTags.CLASS.toString());
				classElement.setAttribute(new Attribute("name", className));
				writeXMLProbNet(((OOPNet) OOPNet).getClasses().get(className), classElement);
				classesElement.addContent(classElement);
			}
			oonElement.addContent(classesElement);
		}

		if (OOPNet.getInstances().size() > 0) {
			Element instancesElement = new Element(XMLTags.INSTANCES.toString());
			for (Instance instance : ((OOPNet) OOPNet).getInstances().values()) {
				Element instanceElement = new Element(XMLTags.INSTANCE.toString());
				instanceElement.setAttribute(new Attribute("name", instance.getName()));
				instanceElement.setAttribute(new Attribute("class", instance.getClassNet().getName()));
				instanceElement.setAttribute(new Attribute("isInput", Boolean.toString(instance.isInput())));
				instanceElement.setAttribute(new Attribute("arity", instance.getArity().toString()));
				instancesElement.addContent(instanceElement);
			}
			oonElement.addContent(instancesElement);
			if (OOPNet.getReferenceLinks().size() > 0) {
				Element instanceLinksElement = new Element(XMLTags.REFERENCE_LINKS.toString());
				for (ReferenceLink link : OOPNet.getReferenceLinks()) {
					Element linkElement = new Element(XMLTags.REFERENCE_LINK.toString());
					if (link instanceof InstanceReferenceLink) {
						InstanceReferenceLink instanceLink = (InstanceReferenceLink) link;
						linkElement.setAttribute(new Attribute("type", "instance"));
						linkElement.setAttribute(new Attribute("source", instanceLink.getSourceInstance().getName()));
						linkElement
								.setAttribute(new Attribute("destination", instanceLink.getDestInstance().getName()));
						linkElement
								.setAttribute(new Attribute("parameter", instanceLink.getDestSubInstance().getName()));
					} else if (link instanceof NodeReferenceLink) {
						NodeReferenceLink nodeLink = (NodeReferenceLink) link;
						linkElement.setAttribute(new Attribute("type", "node"));
						linkElement.setAttribute(new Attribute("source", nodeLink.getSourceNode().getName()));
						linkElement.setAttribute(new Attribute("destination", nodeLink.getDestinationNode().getName()));
					}
					instanceLinksElement.addContent(linkElement);
				}
				oonElement.addContent(instanceLinksElement);
			}
		}
		probNetElement.addContent(oonElement);
	}
	// TODO OOPN end

}