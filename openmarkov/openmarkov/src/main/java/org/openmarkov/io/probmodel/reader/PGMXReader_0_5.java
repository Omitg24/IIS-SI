/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.reader;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.openmarkov.core.io.ProbNetReader;
import org.openmarkov.core.io.format.annotation.FormatType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.AugmentedTable;
import org.openmarkov.core.model.network.potential.AugmentedTablePotential;
import org.openmarkov.core.model.network.potential.BinomialPotential;
import org.openmarkov.core.model.network.potential.CycleLengthShift;
import org.openmarkov.core.model.network.potential.DeltaPotential;
import org.openmarkov.core.model.network.potential.ExactDistrPotential;
import org.openmarkov.core.model.network.potential.ExponentialHazardPotential;
import org.openmarkov.core.model.network.potential.ExponentialPotential;
import org.openmarkov.core.model.network.potential.FunctionPotential;
import org.openmarkov.core.model.network.potential.LinearCombinationPotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.ProductPotential;
import org.openmarkov.core.model.network.potential.SameAsPrevious;
import org.openmarkov.core.model.network.potential.SumPotential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.UniformPotential;
import org.openmarkov.core.model.network.potential.UnivariateDistrPotential;
import org.openmarkov.core.model.network.potential.WeibullHazardPotential;
import org.openmarkov.core.model.network.potential.plugin.PotentialManager;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDPotential;
import org.openmarkov.io.probmodel.exception.PGMXParserException;
import org.openmarkov.io.probmodel.strings.XMLAttributes;
import org.openmarkov.io.probmodel.strings.XMLTags;

/**
 * @author Manuel Arias
 */
@FormatType(name = "PGMXReader", version = "0.5", extension = "pgmx", description = "OpenMarkov.0.5", role = "Reader")
public class PGMXReader_0_5 extends PGMXReader_0_2 implements ProbNetReader {

    /**
     * @param probNet
     * @param xmlPotential
     * @return <code>Potential</code>
     * @throws PGMXParserException
     */
    protected Potential getPotential(Element xmlPotential, ProbNet probNet)
            throws PGMXParserException {
        PotentialRole xmlRole = PotentialRole.CONDITIONAL_PROBABILITY;
        return getPotential(xmlPotential, probNet, xmlRole);
    }
    
    /**
     * @param probNet
     * @param eXMLPotential
     * @return <code>Potential</code>
     * @throws PGMXParserException
     */
    protected Potential getPotential(Element eXMLPotential, ProbNet probNet, PotentialRole xmlRole)
            throws PGMXParserException {
        Potential potential = null;
        // get type and role of potential
        String sXMLPotentialType = getStringXMLPotentialType(eXMLPotential);
        List<Variable> variables = getReferencedVariables(eXMLPotential, probNet);
        if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(UniformPotential.class))) {
            potential = getUniformPotential(eXMLPotential, probNet, xmlRole, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(TablePotential.class))) {
            try {
                Element child = eXMLPotential.getChild(XMLTags.UNCERTAIN_VALUES.toString());
                if (child != null) {
                List<Element> uncertainParametersList = eXMLPotential.getChild(XMLTags.UNCERTAIN_VALUES.toString()).
                       getChildren(XMLTags.UNCERT_PARAM.toString());
                if (uncertainParametersList != null) {
                    for (Element uncertainParameter : uncertainParametersList) {
                        if ((uncertainParameter.getAttributeValue(XMLAttributes.TYPE.toString())).equals("Function")) {
                            potential = getAugmentedTablePotential(eXMLPotential, xmlRole, variables);
                            break;
                        }
                    }
                }
                }
            } catch(Exception e) {
                throw new PGMXParserException("Exception en getPotential", eXMLPotential);
            } finally {
                if (potential == null) {
                    potential = getTablePotential(eXMLPotential, probNet, xmlRole, variables);
                }
            }
            //CMF
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(TreeADDPotential.class))) {
            potential = getTreeADDPotential(eXMLPotential, probNet, xmlRole, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(CycleLengthShift.class))) {
            potential = getCycleLengthShiftPotential(eXMLPotential, probNet, xmlRole, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(SameAsPrevious.class))) {
            potential = getSameAsPrevious(eXMLPotential, probNet, xmlRole, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(SumPotential.class))) {
            potential = getSumPotential(eXMLPotential, probNet, PotentialRole.CONDITIONAL_PROBABILITY, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(ProductPotential.class))) {
        	potential = getProductPotential(eXMLPotential, probNet, PotentialRole.CONDITIONAL_PROBABILITY, variables);
		} else if (sXMLPotentialType.equalsIgnoreCase("ICIModel")){
			potential = getICIPotential(eXMLPotential, probNet, xmlRole, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(WeibullHazardPotential.class))) {
            potential = getWeibullPotential(eXMLPotential, probNet, xmlRole, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(ExponentialHazardPotential.class))) {
            potential = getExponentialHazardPotential(eXMLPotential, probNet, xmlRole, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(LinearCombinationPotential.class))
        		|| PotentialManager.getAlternativeNames(LinearCombinationPotential.class).contains(sXMLPotentialType)) {
            potential = getLinearRegressionPotential(eXMLPotential, probNet, xmlRole, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(FunctionPotential.class))) {
            potential = getFunctionPotential(eXMLPotential, probNet, xmlRole, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(UnivariateDistrPotential.class))) {
            potential = getUnivariateDistrPotential(eXMLPotential, xmlRole, variables);
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(DeltaPotential.class))) {
            potential = getDeltaPotential(eXMLPotential, probNet, xmlRole, variables); 
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(ExponentialPotential.class))) {
            potential = getExponentialPotential(eXMLPotential, probNet, xmlRole, variables); 
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(BinomialPotential.class))) {
            potential = getBinomialPotential(eXMLPotential, probNet, xmlRole, variables);     
        } else if (sXMLPotentialType.equalsIgnoreCase(PotentialManager.getPotentialName(ExactDistrPotential.class))) {
                potential = getExactDistrPotential(eXMLPotential, probNet, xmlRole, variables);
        } else {
            throw new PGMXParserException("Potential type " + sXMLPotentialType + " not supported", eXMLPotential);
        }

        Element xmlComment = eXMLPotential.getChild(XMLTags.COMMENT.toString());
        if(xmlComment != null) {
            potential.setComment(xmlComment.getText());
        }
        return potential;
    }

    /**
     * @param xmlPotential
     * @return PotentialRole
     */
    protected PotentialRole getPotentialRole(Element xmlPotential) {
		String xmlPotentialRole = xmlPotential.getAttributeValue(XMLAttributes.ROLE.toString());
        PotentialRole xmlRole;
        if (xmlPotentialRole.equalsIgnoreCase("utility")) {
            xmlRole = PotentialRole.UNSPECIFIED;
        } else {
            xmlRole = PotentialRole.getEnumMember(xmlPotentialRole);
        }
		return xmlRole;
	}
    
	/**
	 * Reads an UnivariateDistrPotential from the Element xmlPotential
	 * @param xmlPotential
	 * @param xmlRole
	 * @param variables
	 * @return the UnivariateDistrPotential read
     * @throws PGMXParserException
	 */
    protected Potential getUnivariateDistrPotential(Element xmlPotential, PotentialRole xmlRole, List<Variable> variables) throws PGMXParserException {
    	String univariateName= xmlPotential.getAttributeValue(XMLAttributes.DISTRIBUTION.toString());
    	String parametrization =xmlPotential.getAttributeValue(XMLAttributes.PARAMETRIZATION.toString());
        Element xmlRootTable = xmlPotential.getChild(XMLTags.PARAMETERS.toString());
        double[] table = parseDoubles(xmlRootTable.getTextNormalize());     

        UnivariateDistrPotential potential; 
        try {
        	potential = new UnivariateDistrPotential(variables,univariateName,parametrization,xmlRole);
        	List<Variable> parameterVariables = potential.getParameterVariables();
             
            List<Variable> vDistributionTable= new ArrayList<Variable>(potential.getFiniteStatesVariables());
            vDistributionTable.add(0,potential.getPseudoVariableDistribution());
            potential.getAugmentedTable().setValues(table);
        	potential.setDistributionTable(getAugmentedTable(xmlPotential, xmlRole,vDistributionTable, parameterVariables));
        } catch(Exception e) {
            throw new PGMXParserException("Exception in getUnivariateDistrPotential.", xmlPotential);
        }
    	return potential;
    	
    }

    /**
     * @param xmlPotential
     * @param xmlRole
     * @param variables
     * @return Potential
     * @throws PGMXParserException
     */
    protected Potential getAugmentedTablePotential(Element xmlPotential, PotentialRole xmlRole, List<Variable> variables) throws PGMXParserException {
        List<Variable>  finiteStatesVariables;
        List<Variable>  parameterVariables;
        AugmentedTablePotential potential;

        try{
            potential = new AugmentedTablePotential(variables,xmlRole);
            parameterVariables = potential.getParameterVariables();
            finiteStatesVariables = potential.getFiniteStatesVariables();
        } catch(Exception e) {
            throw new PGMXParserException("Exception in getAugmentedTablePotential", xmlPotential);
        }
        potential.setAugmentedTable( getAugmentedTable(xmlPotential, xmlRole, finiteStatesVariables, parameterVariables));
        return potential;
    }

    /**
     * @param xmlPotential
     * @param xmlRole
     * @param finiteStatesVariables
     * @param parameterVariables
     * @return AugmentedTable
     */
    protected AugmentedTable getAugmentedTable(Element xmlPotential, PotentialRole xmlRole,
                                               List<Variable> finiteStatesVariables, List<Variable> parameterVariables){
        List<Element> uncertainParametersList = xmlPotential.getChild(XMLTags.UNCERTAIN_VALUES.toString()).
                getChildren(XMLTags.UNCERT_PARAM.toString());
        String[] functionValues = new String[uncertainParametersList.size()];
        int i = 0;      
        for (Element uncertainParameter:uncertainParametersList){
            functionValues[i++]= uncertainParameter.getText();                
        } 
        return new AugmentedTable( finiteStatesVariables, xmlRole, functionValues);
    }

}	
	
	
	
	
 