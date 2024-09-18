/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.writer;

import java.util.List;

import org.jdom2.Element;
import org.openmarkov.core.exception.WriterException;
import org.openmarkov.core.io.ProbNetWriter;
import org.openmarkov.core.io.format.annotation.FormatType;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.AugmentedTable;
import org.openmarkov.core.model.network.potential.AugmentedTablePotential;
import org.openmarkov.core.model.network.potential.ExactDistrPotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.UnivariateDistrPotential;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;
import org.openmarkov.io.probmodel.strings.XMLAttributes;
import org.openmarkov.io.probmodel.strings.XMLTags;

/**
 * @author Manuel Arias
 * @version 0.2
 */
@FormatType(name = "PGMXWriter0_2",  version = "0.5", extension = "pgmx", description = "OpenMarkov.0.5", role = "Writer")
public class PGMXWriter_0_5 extends PGMXWriter_0_2 implements ProbNetWriter {

	// Methods
	/**
	 * @param netName = path + network name + extension
	 * @param probNet
	 */
	public void writeProbNet(String netName, ProbNet probNet) throws WriterException {
		formatVersionNumber= "0.5.0";
		super.writeProbNet(netName, probNet);
	}

	/**
	 * @param netName = path + network name + extension
	 * @param probNet
	 * @param evidences list of evidence cases
	 */
	public void writeProbNet(String netName, ProbNet probNet, List<EvidenceCase> evidences) throws WriterException {
		formatVersionNumber= "0.5.0";
		super.writeProbNet(netName, probNet, evidences);
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
					policiesElement.addContent(potentialElement);
				}
			}
		}
		if (existsAtLeastOnePolicy) {
			root.addContent(policiesElement);
		}
	}

	/**
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
			restrictionPotential.setAttribute(XMLAttributes.TYPE.toString(), potentialType);
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
				potentialsElement.addContent(potentialElement);
			}
		}
		probNetElement.addContent(potentialsElement);
	}

	/**
	 * @param potential
	 * @param potentialElement
	 * @param potentialElement
	 */
	protected void getPotential(ProbNet probNet, Potential potential, Element potentialElement) {
		getPotentialAttributesAndVariables(potential, potentialElement);
		getPotentialBody(probNet, potential, potentialElement);
	}

	/**
	 *
	 * @param potential
	 * @param potentialElement
	 */
	protected void getPotentialAttributesAndVariables(Potential potential, Element potentialElement) {
		TablePotential tablePotential = null;
		/*
		 * if (potential instanceof ExactDistrPotential) { ExactDistrPotential
		 * exactDistrPotential = (ExactDistrPotential) potential; tablePotential =
		 * exactDistrPotential.getTablePotential(); } Potential wrapped = tablePotential
		 * == null ? potential : tablePotential;
		 */
		Potential wrapped = potential;

		String potentialType = wrapped.getClass().getAnnotation(PotentialType.class).name();
		if (potentialType.equals("AugmentedTable")){
			potentialType = "Table";
		}

		// This is a patch. TODO modify XML files to add Strings and other improvements
		if (wrapped.getClass() == TablePotential.class || wrapped.getClass() == ExactDistrPotential.class) {
			potentialType = "UnivariateDistr";
			String distribution = "exact";
			potentialElement.setAttribute(XMLAttributes.TYPE.toString(), potentialType);
			potentialElement.setAttribute(XMLAttributes.DISTRIBUTION.toString(), distribution);
		} else {
			potentialElement.setAttribute(XMLAttributes.TYPE.toString(), potentialType);
		}

		// TODO add function attribute

		getPotentialComment(wrapped, potentialElement);

		// TODO add aditionalProperties child

		getPotentialVariables(potential, potentialElement);
	}

	/**
	 *
	 * @param potential
	 * @param potentialElement
	 */
	protected void getPotentialVariables(Potential potential, Element potentialElement) {
		List<Variable> potentialVariables = potential.getVariables();
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
		super.getPotentialBody(probNet, potential, potentialElement);
		if (potential instanceof UnivariateDistrPotential) { // New from the previous version of the writer
			getUnivariateDistrPotential(potentialElement, (UnivariateDistrPotential) potential);
		} else if (potential instanceof AugmentedTablePotential) { // New from the previous version of the writer
			getAugmentedTablePotential(potentialElement, ((AugmentedTablePotential) potential).getAugmentedTable());
		}
	}

	/**
	 * Adds an UnivariateDistrPotential to the XML structure
	 * @param xmlElement 
	 * @param potential
	 */
	protected void getUnivariateDistrPotential(Element xmlElement, UnivariateDistrPotential potential){
		xmlElement.setAttribute(XMLAttributes.DISTRIBUTION.toString(), potential.getProbDensFunctionUnivariateName());
		xmlElement.setAttribute(XMLAttributes.PARAMETRIZATION.toString(), potential.getProbDensFunctionParametrizationName());
		Element parametersElement = new Element(XMLTags.PARAMETERS.toString());
		parametersElement.setText(getValuesInAString(potential.getDistributionTable().getValues()));
		getAugmentedTablePotential(xmlElement, potential.getAugmentedTable());
		// Write table values to the XML file
		xmlElement.addContent(parametersElement);		
	}

	/**
	 *
	 * @param xmlElement
	 * @param augmentedTable
	 */
	protected void getAugmentedTablePotential(Element xmlElement, AugmentedTable augmentedTable){
        Element parametersElement = new Element(XMLTags.UNCERTAIN_VALUES.toString());
        
        String[] functionValues = augmentedTable.getFunctionValues();
        for (String function:functionValues){
            Element uncertParamElement = new Element(XMLTags.UNCERT_PARAM.toString());
            uncertParamElement.setAttribute( XMLAttributes.TYPE.toString(), XMLTags.FUNCTION.toString());
            uncertParamElement.addContent( function );
            parametersElement.addContent( uncertParamElement);
        }
        // Write table values to the XML file
        xmlElement.addContent(parametersElement);       
    }

}