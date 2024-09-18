/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.ParserException;
import org.openmarkov.core.inference.MulticriteriaOptions;
import org.openmarkov.core.inference.TransitionTime;
import org.openmarkov.core.io.ProbNetInfo;
import org.openmarkov.core.io.ProbNetReader;
import org.openmarkov.core.io.format.annotation.FormatType;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.CycleLength;
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
import org.openmarkov.core.model.network.constraint.OnlyChanceNodes;
import org.openmarkov.core.model.network.constraint.PNConstraint;
import org.openmarkov.core.model.network.modelUncertainty.ProbDensFunction;
import org.openmarkov.core.model.network.modelUncertainty.ProbDensFunctionManager;
import org.openmarkov.core.model.network.modelUncertainty.UncertainValue;
import org.openmarkov.core.model.network.potential.BinomialPotential;
import org.openmarkov.core.model.network.potential.CycleLengthShift;
import org.openmarkov.core.model.network.potential.DeltaPotential;
import org.openmarkov.core.model.network.potential.ExactDistrPotential;
import org.openmarkov.core.model.network.potential.ExponentialHazardPotential;
import org.openmarkov.core.model.network.potential.ExponentialPotential;
import org.openmarkov.core.model.network.potential.FunctionPotential;
import org.openmarkov.core.model.network.potential.GLMPotential;
import org.openmarkov.core.model.network.potential.LinearCombinationPotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.ProductPotential;
import org.openmarkov.core.model.network.potential.SameAsPrevious;
import org.openmarkov.core.model.network.potential.SumPotential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.UniformPotential;
import org.openmarkov.core.model.network.potential.WeibullHazardPotential;
import org.openmarkov.core.model.network.potential.canonical.ICIPotential;
import org.openmarkov.core.model.network.potential.canonical.MaxPotential;
import org.openmarkov.core.model.network.potential.canonical.MinPotential;
import org.openmarkov.core.model.network.potential.canonical.TuningPotential;
import org.openmarkov.core.model.network.potential.plugin.PotentialManager;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;
import org.openmarkov.core.model.network.potential.treeadd.Threshold;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDBranch;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDPotential;
import org.openmarkov.core.model.network.type.NetworkType;
import org.openmarkov.core.model.network.type.plugin.NetworkTypeManager;
import org.openmarkov.core.oopn.Instance;
import org.openmarkov.core.oopn.InstanceReferenceLink;
import org.openmarkov.core.oopn.NodeReferenceLink;
import org.openmarkov.core.oopn.OOPNet;
import org.openmarkov.core.oopn.ReferenceLink;
import org.openmarkov.core.oopn.exception.InstanceAlreadyExistsException;
import org.openmarkov.io.probmodel.exception.PGMXParserException;
import org.openmarkov.io.probmodel.strings.XMLAttributes;
import org.openmarkov.io.probmodel.strings.XMLTags;
import org.openmarkov.io.probmodel.strings.XMLValues;

/**
 * @author Manuel Arias
 */
@FormatType(name = "PGMXReader", version = "0.2", extension = "pgmx", description = "OpenMarkov.0.2", role = "Reader")
public class PGMXReader_0_2 implements ProbNetReader {

    // Methods
    @Override
    /**
     * @param netName = path + network name + extension. <code>String</code>
     * @param inputStream InputStream[]
     * @throws PGMXParserException
     */
    public ProbNet loadProbNet(String netName, InputStream... inputStream ) throws ParserException {
        ProbNetInfo probNetInfo = loadProbNetInfo( netName, inputStream );
        if ( probNetInfo == null ) {
            throw new ParserException( "No ProbNet in ProbNetInfo." );
        }
        return probNetInfo.getProbNet();
    }

    /**
     * @param netName = path + network name + extension. <code>String</code>
     * @return The <code>ProbNet</code> readed or <code>null</code>
     * @throws PGMXParserException
     */
    public ProbNetInfo loadProbNetInfo( String netName, InputStream... inputStream ) throws ParserException {

        InputStream stream = getStream(netName, inputStream);
        Element root = getRootElement(stream, netName);

        return loadProbNetInfo( root, netName );
    }

    /**
     * @param root
     * @param netName
     * @return ProbNetInfo
     * @throws PGMXParserException
     */
    public ProbNetInfo loadProbNetInfo( Element root, String netName ) throws ParserException {

        String strVersion = root.getAttributeValue( XMLAttributes.FORMAT_VERSION.toString() );
        PGMXReader_0_2 reader = ReaderFactory.getReader( strVersion );
        ProbNet probNet = reader.getProbNet( root, netName );
        reader.getInferenceOptions( root, probNet );
        List<EvidenceCase> evidence = reader.getEvidence( root, probNet );
        reader.getPolicies( root, probNet );
        return new ProbNetInfo( probNet, evidence );
    }

    /**
     * @param netName
     * @param inputStream InputStream[]
     * @return network version in a String
     * @throws ParserException
     */
    public String getVersion( String netName, InputStream... inputStream ) throws ParserException {
        Element root = getRootElement(getStream(netName, inputStream), netName);
        return root.getAttributeValue( XMLAttributes.FORMAT_VERSION.toString() );
    }

    /**
     * Gets the root element of a PGMX file
     * @param stream
     * @param netName
     * @return root Element
     * @throws ParserException
     */
    private Element getRootElement(InputStream stream, String netName) throws ParserException {
        SAXBuilder builder = new SAXBuilder();
        builder.setJDOMFactory( new LocatedJDOMFactory() );
        Document document = null;
        try {
            document = builder.build( stream );
        }
        catch ( JDOMException e ) {
            throw new ParserException( "Can not parse XML document " + netName + ":" + e.getMessage() );
        }
        catch ( IOException e ) {
            throw new ParserException( "Error trying to open " + netName + ".\n" + e.getMessage() );
        }
        return document.getRootElement();
    }

    /**
     * Get file if not included
     * @param netName
     * @param inputStream
     * @return InputStream
     * @throws ParserException
     */
    private InputStream getStream(String netName, InputStream... inputStream) throws ParserException {
        InputStream stream = null;
        if ( inputStream.length == 0 ) {
            try {
                stream = new FileInputStream( netName );
            }
            catch ( FileNotFoundException e ) {
                throw new ParserException( "File " + netName + " not found." );
            }
        }
        else {
            if ( inputStream.length > 1 ) {
                throw new ParserException( "Only is allowed to open ONE InputStream, not " + inputStream.length + "." );
            }
            stream = inputStream[0];
        }
        return stream;
    }

    /**
     * @param root Root element
     * @param netName Network name
     * @return ProbNet
     * @throws PGMXParserException
     */
    public ProbNet getProbNet( Element root, String netName ) throws PGMXParserException {
        return getProbNet( root, netName, new HashMap<String, ProbNet>() );
    }

    /**
     * @param root Root element
     * @param netName Network name
     * @param classes Classes
     * @return ProbNet
     * @throws PGMXParserException
     */
    protected ProbNet getProbNet( Element root, String netName, Map<String, ProbNet> classes ) throws PGMXParserException {
        ProbNet probNet = null;
        Element xMLProbNet = root.getChild( getStringTagNetwork() );
        if ( xMLProbNet != null ) { // Read prob net if the xml file exists
            probNet = initializeProbNet( xMLProbNet, netName );
            getVariablesLinksAndPotentials( xMLProbNet, probNet );
            getNetworkAdvancedInformation( xMLProbNet, probNet, netName, classes );
        }
        return probNet;
    }

    /**
     * Reads agents, temporal unit, additional properties and OOPN
     * @param xMLProbNet
     * @param probNet
     * @param netName
     * @param classes
     * @throws PGMXParserException
     */
    protected void getNetworkAdvancedInformation( Element xMLProbNet, ProbNet probNet, String netName,
                                                  Map<String, ProbNet> classes ) throws PGMXParserException {
        getAgents( xMLProbNet, probNet );
        getTemporaUnit( xMLProbNet, probNet );
        getAdditionalProperties( xMLProbNet, probNet );
        getOOPN( netName, xMLProbNet, probNet, classes );
    }

    protected void getTemporaUnit( Element xMLProbNet, ProbNet probNet )
    {
        Element temporalUnit = xMLProbNet.getChild( XMLTags.TIME_UNIT.toString() );
        if ( temporalUnit != null )
        {
            CycleLength temporalUnitProbNet = new CycleLength();
            try
            {
                String scaleAttribute = temporalUnit.getAttributeValue( XMLTags.VALUE.toString() );
                if ( scaleAttribute != null )
                {
                    double scale = Double.parseDouble( scaleAttribute );
                    temporalUnitProbNet.setValue( scale );
                }
            }
            catch ( NumberFormatException e )
            {
                // TODO - Check this exception
            }
            String unitAttribute = temporalUnit.getAttributeValue( XMLTags.UNIT.toString() );
            if ( unitAttribute != null )
            {
                CycleLength.Unit unit = CycleLength.Unit.valueOf( unitAttribute );
                temporalUnitProbNet.setUnit( unit );
            }

            probNet.setCycleLength( temporalUnitProbNet );
        }
        else
        {
            CycleLength defaultTemporalUnit = new CycleLength();
            probNet.setCycleLength( defaultTemporalUnit );

        }

    }

    protected void getVariablesLinksAndPotentials( Element xMLProbNet, ProbNet probNet )
            throws PGMXParserException
    {
        //CMI Proposed behaviour: if there is no variables exit silently and keep the network empty
        //getVariables( xMLProbNet, probNet );
        try {
            getVariables(xMLProbNet, probNet);
        } catch (PGMXParserException e){
            System.err.println(e.getMessage());
            return;
        }
        //CMF
        getLinks( xMLProbNet, probNet );
        getPotentials( xMLProbNet, probNet );
    }

    /**
     * @return The string of the tag encloses the network
     */
    protected String getStringTagNetwork()
    {
        return XMLTags.PROB_NET.toString();
    }

    protected ProbNet initializeProbNet( Element xMLProbNet, String netName )
            throws PGMXParserException
    {
        ProbNet probNet;
        // =
        // xMLProbNet.getAttribute(XMLAttributes.TYPE.toString());
        NetworkType networkType = getNetworkType( xMLProbNet );
        // OOPN start
        if ( xMLProbNet.getChild( XMLTags.OOPN.toString() ) != null )
        {
            probNet = new OOPNet( networkType );
        }
        else
        {
            // OOPN end
            probNet = new ProbNet( networkType );
        }
        getAdditionalConstraints( probNet, xMLProbNet );
        // TODO Read Inference options
        // TODO Read Policies
        probNet = getConstraints( xMLProbNet, probNet );
        probNet.setComment( getProbNetComment( xMLProbNet, probNet ) );
        probNet.setName( FilenameUtils.getName( netName ) );
        getDecisionCriterion( xMLProbNet, probNet );
        return probNet;
    }

    protected void getAdditionalProperties( Element root, ProbNet probNet )
    {
        Element xmlAdditionalProperties = root.getChild( XMLTags.ADDITIONAL_PROPERTIES.toString() );
        if ( xmlAdditionalProperties != null )
        {
            List<Element> propertiesListElement = getXMLChildren( xmlAdditionalProperties );
            if ( propertiesListElement != null && propertiesListElement.size() > 0 )
            {
                for ( Element propertyElement : propertiesListElement )
                {
                    String propertyName = getElementName( propertyElement );
                    String propertyValue = propertyElement.getAttributeValue( XMLAttributes.VALUE.toString() );
                    probNet.additionalProperties.put( propertyName, propertyValue );
                }
            }
        }
    }

    /**
     * @param root . <code>Element</code>
     * @param probNet . <code>ProbNet</code>
     */
    protected void getAgents( Element root, ProbNet probNet )
    {
        Element xmlAgentsRoot = root.getChild( XMLTags.AGENTS.toString() );
        if ( xmlAgentsRoot != null )
        {
            List<Element> xmlAgents = getXMLChildren( xmlAgentsRoot );
            ArrayList<StringWithProperties> agents = new ArrayList<>();
            for ( Element agentElement : xmlAgents )
            {
                String agentName = getElementName( agentElement );
                StringWithProperties agent = new StringWithProperties( agentName );
                Properties agentProperties = getAdditionalProperties( agentElement );
                agent.put( agentProperties );
                agents.add( agent );
            }
            probNet.setAgents( agents );
        }
    }

    /**
     * @param root . <code>Element</code>
     * @param probNet . <code>ProbNet</code>
     */
    protected void getDecisionCriterion( Element root, ProbNet probNet )
    {
        Element xmlCriteronRoot = root.getChild( XMLTags.DECISION_CRITERIA.toString() );
        if ( xmlCriteronRoot != null )
        {
            List<Element> xmlCriterion = getXMLChildren( xmlCriteronRoot );
            List<Criterion> criteria = new ArrayList<>();
            for ( Element criterionElement : xmlCriterion )
            {
                String criterionName = getElementName( criterionElement );
                // Properties criterionProperties = getAdditionalProperties(criterionElement);
                String criterionUnit = criterionElement.getAttributeValue( XMLAttributes.UNIT.toString() );
                Criterion decisionCriterion = new Criterion( criterionName, criterionUnit );
                /*
                 * if (criterionProperties != null) { decisionCriterion.put(criterionProperties); }
                 */
                criteria.add( decisionCriterion );
                // criterions.put(criterionName, criterionProperties);
            }
            probNet.setDecisionCriteria( criteria );
            // If the probNet has not the OnlyChanceNodes constraint
            // we create a default criterion
        }
        else if ( !probNet.hasConstraint( OnlyChanceNodes.class ) )
        {
            List<Criterion> criteria = new ArrayList<>();
            Criterion decisionCriterion = new Criterion();
            criteria.add( decisionCriterion );
            probNet.setDecisionCriteria( criteria );
        }
    }

    /**
     * @param agentElement . <code>Element</code>
     * @return <code>Properties</code>
     */
    protected Properties getAdditionalProperties(Element agentElement )
    {
        Properties properties = null;
        List<Element> propertiesListElement = getXMLChildren( agentElement );
        if ( propertiesListElement != null && propertiesListElement.size() > 0 )
        {
            properties = new Properties();
            for ( Element propertyElement : propertiesListElement )
            {
                String propertyName = getElementName( propertyElement );
                String propertyValue = propertyElement.getAttributeValue( XMLAttributes.VALUE.toString() );
                properties.put( propertyName, propertyValue );
            }
        }
        return properties;
    }

    /**
     * Get the Inference Options from the PGMX
     *
     * @param root Root element
     * @param probNet ProbNet
     */
    protected void getInferenceOptions( Element root, ProbNet probNet )
    {
        Element inferenceOptions = root.getChild( XMLTags.INFERENCE_OPTIONS.toString() );
        if ( inferenceOptions != null )
        {
            getMulticriteriaOptions( inferenceOptions, probNet );
            getTemporalOptions( inferenceOptions, probNet );
        }

    }

    /**
     * Get the Multicriteria Options from the PGMX
     *
     * @param inferenceOptions Inference options
     * @param probNet ProbNet
     */
    protected void getMulticriteriaOptions( Element inferenceOptions, ProbNet probNet )
    {
        Element multicriteriaOptions = inferenceOptions.getChild( XMLTags.MULTICRITERIA_OPTIONS.toString() );
        if ( multicriteriaOptions != null )
        {
            Element multiCriteriaType = multicriteriaOptions.getChild( XMLTags.SELECTED_ANALYSIS_TYPE.toString() );

            probNet.getInferenceOptions().getMultiCriteriaOptions().setMulticriteriaType( MulticriteriaOptions.Type.valueOf( multiCriteriaType.getValue() ) );

            Element unicriteria = multicriteriaOptions.getChild( XMLTags.UNICRITERION.toString() );
            if ( unicriteria != null )
            {

                Element scalesTag = unicriteria.getChild( XMLTags.SCALES.toString() );
                if ( scalesTag != null )
                {
                    List<Element> scales = getXMLChildren( scalesTag );
                    for ( Element element : scales )
                    {
                        for ( Criterion probNetCriterion : probNet.getDecisionCriteria() )
                        {
                            String criterionVariableString =
                                    element.getAttribute( XMLTags.CRITERION.toString() ).getValue();
                            if ( criterionVariableString.equals( probNetCriterion.getCriterionName() ) )
                            {
                                probNetCriterion.setUnicriterizationScale(Double.parseDouble(element.getAttributeValue(XMLTags.VALUE.toString())));
                            }
                        }
                    }
                }

                Element mainUnit = unicriteria.getChild( XMLTags.UNIT.toString() );
                if ( mainUnit != null )
                {
                    probNet.getInferenceOptions().getMultiCriteriaOptions().setMainUnit( mainUnit.getValue() );
                }
            }

            Element ceOptions = multicriteriaOptions.getChild( XMLTags.COSTEFFECTIVENESS.toString() );
            if ( ceOptions != null )
            {
                Element scalesTag = ceOptions.getChild( XMLTags.SCALES.toString() );
                if ( scalesTag != null )
                {
                    List<Element> scales = getXMLChildren( scalesTag );
                    for ( Element element : scales )
                    {
                        for ( Criterion criterion : probNet.getDecisionCriteria() )
                        {
                            if ( element.getAttribute( XMLTags.CRITERION.toString() ).getValue().equals( criterion.getCriterionName() ) )
                            {
                                criterion.setCeScale( Double.parseDouble( element.getAttributeValue( XMLTags.VALUE.toString() ) ) );
                            }
                        }
                    }
                }

                Element ceCriteria = ceOptions.getChild( XMLTags.CE_CRITERIA.toString() );
                if ( ceCriteria != null )
                {
                    List<Element> ce_criterion = getXMLChildren( ceCriteria );
                    for ( Element element : ce_criterion )
                    {
                        for ( Criterion criterion : probNet.getDecisionCriteria() )
                        {
                            if ( element.getAttribute( XMLTags.CRITERION.toString() ).getValue().equals( criterion.getCriterionName() ) )
                            {
                                criterion.setCECriterion( Criterion.CECriterion.valueOf( element.getAttributeValue( XMLTags.VALUE.toString() ) ) );
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get Temporal Evolution Options from the PGMX
     *
     * @param inferenceOptions Inference Options
     * @param probNet ProbNet
     */
    protected void getTemporalOptions( Element inferenceOptions, ProbNet probNet )
    {
        Element temporalOptions = inferenceOptions.getChild( XMLTags.TEMPORAL_OPTIONS.toString() );
        if ( temporalOptions != null )
        {

            Element slices = temporalOptions.getChild( XMLTags.SLICES.toString() );
            probNet.getInferenceOptions().getTemporalOptions().setHorizon( Integer.parseInt( slices.getValue() ) );

            Element transition = temporalOptions.getChild( XMLTags.TRANSITION.toString() );
            probNet.getInferenceOptions().getTemporalOptions().setTransition( TransitionTime.valueOf( transition.getText() ) );

            Element discountsTag = temporalOptions.getChild( XMLTags.DISCOUNT_RATES.toString() );
            List<Element> discounts = getXMLChildren( discountsTag );
            for ( Element element : discounts )
            {
                for ( Criterion criterion : probNet.getDecisionCriteria() )
                {
                    if ( element.getAttribute( XMLTags.CRITERION.toString() ).getValue().equals( criterion.getCriterionName() ) )
                    {
                        criterion.setDiscount( Double.parseDouble( element.getAttributeValue( XMLAttributes.VALUE.toString() ) ) );
                        criterion.setDiscountUnit( CycleLength.DiscountUnit.valueOf( element.getAttributeValue( XMLAttributes.UNIT.toString() ) ) );
                    }
                }
            }

        }

    }

    /**
     * Reads evidence
     *
     * @param root Root element
     * @param probNet ProbNet
     * @return List of evidence case
     * @throws PGMXParserException
     */
    protected List<EvidenceCase> getEvidence( Element root, ProbNet probNet )
            throws PGMXParserException
    {
        Element xMLEvidence = root.getChild( XMLTags.EVIDENCE.toString() );
        List<EvidenceCase> evidence = new ArrayList<>();
        if ( xMLEvidence != null )
        {
            List<Element> xmlEvidenceCases = getXMLChildren( xMLEvidence );
            for ( Element xmlEvidenceCase : xmlEvidenceCases )
            {
                EvidenceCase evidenceCase = new EvidenceCase();
                List<Element> xmlFindings = getXMLChildren( xmlEvidenceCase );
                for ( Element xmlFinding : xmlFindings )
                {
                    try
                    {
                        Variable variable = probNet.getVariable( xmlFinding.getAttributeValue( "variable" ) );
                        Finding finding;
                        if ( variable.getVariableType() == VariableType.FINITE_STATES )
                        {
                            String stateName = xmlFinding.getAttributeValue( "state" );
                            finding = new Finding( variable, variable.getStateIndex( stateName ) );
                        }
                        else
                        {
                            double numericalValue =
                                    Double.parseDouble( xmlFinding.getAttributeValue( "numericValue" ) );
                            finding = new Finding( variable, numericalValue );
                        }
                        evidenceCase.addFinding( finding );
                    }
                    catch ( Exception e )
                    {
                        throw new PGMXParserException( e.getMessage(), xmlFinding );
                    }
                }
                evidence.add( evidenceCase );
            }
        }
        return evidence;
    }

    protected void getAdditionalConstraints( ProbNet probNet, Element xMLProbNet )
    {
        if ( parseXMLElement( xMLProbNet, XMLTags.ADDITIONAL_CONSTRAINTS ) )
        {
            // TODO - Get the additional constraints
        }
    }

    protected boolean parseXMLElement( Element xMLRoot, XMLTags additionalConstraints ) {
        return false;
    }

    protected NetworkType getNetworkType( Element xMLProbNet )
            throws PGMXParserException
    {
        String sType = getStringXMLPotentialType( xMLProbNet );
        if ( sType == null || sType.isEmpty() )
        {
            throw new PGMXParserException( "No network type found", xMLProbNet );
        }
        NetworkTypeManager networkTypeManager = new NetworkTypeManager();
        NetworkType networkType = networkTypeManager.getNetworkType( sType );
        if ( networkType == null )
        {
            throw new PGMXParserException( "Unknown network type: " + sType, xMLProbNet );
        }
        return networkType;
    }

    /**
     * @param root . <code>Element</code>
     * @param probNet . <code>ProbNet</code>
     */
    protected ProbNet getConstraints( Element root, ProbNet probNet )
            throws PGMXParserException
    {
        // ProbNet network = null;
        Element xmlConstraintsRoot = root.getChild( XMLTags.ADDITIONAL_CONSTRAINTS.toString() );
        if ( xmlConstraintsRoot != null )
        {
            List<Element> xmlConstraints = getXMLChildren( xmlConstraintsRoot );
            for ( Element constraintElement : xmlConstraints )
            {
                String constraintName = getElementName( constraintElement );
                try
                {
                    probNet.addConstraint( (PNConstraint) Class.forName( constraintName ).newInstance() );
                }
                catch ( Exception e )
                {
                    throw new PGMXParserException( "Can not create an instance " + "of constraint: " + constraintName,
                            root );
                }
            }
        }
        return probNet;
    }

    /**
     * @param root . <code>Element</code>
     */
    protected String getProbNetComment( Element root, ProbNet probNet )
    {
        Element xmlComments = root.getChild( XMLTags.COMMENT.toString() );
        if ( xmlComments != null )
        {
            String showCommentWhenOpening = xmlComments.getAttributeValue( XMLAttributes.SHOW_COMMENT.toString() );
            probNet.setShowCommentWhenOpening( Boolean.valueOf( showCommentWhenOpening ) );
            String comment = xmlComments.getText();
            return textToHtml( comment );
        }
        return "";
    }

    /**
     * @param root . <code>Element</code>
     */
    protected String getComment( Element root )
    {
        Element xmlComments = root.getChild( XMLTags.COMMENT.toString() );
        if ( xmlComments != null )
        {
            String comment = xmlComments.getText();
            return textToHtml( comment );
        }
        return "";
    }

    /**
     * Reads Nodes:
     *
     * @param root <code>Element</code>
     * @param probNet <code>ProbNet</code>
     * @throws PGMXParserException
     */
    protected void getVariables( Element root, ProbNet probNet ) throws PGMXParserException {
        Element xmlVariablesRoot = getXMLVariables( root );
        if (xmlVariablesRoot != null) {
            List<Element> xmlVariables = getVariablesElements( xmlVariablesRoot );
            loadVariables( xmlVariables, probNet );
        }
    }

    protected List<Element> getVariablesElements( Element xmlVariablesRoot ) {
        return xmlVariablesRoot.getChildren();
    }

    protected void loadVariables( List<Element> xmlVariables, ProbNet probNet )
            throws PGMXParserException
    {
        for ( Element variableElement : xmlVariables )
        { // Variables
            loadVariable( variableElement, probNet );
        }
    }

    protected void loadVariable( Element variableElement, ProbNet probNet )
            throws PGMXParserException
    {
        VariableType variableType = getXMLVariableType( variableElement );
        NodeType nodeType = getXMLNodeType( variableElement );
        String variableName = getVariableName( variableElement );

        loadVariableAdvancedInformation( variableElement, probNet, variableType, nodeType, variableName );
    }

    protected String getVariableName( Element variableElement )
    {
        return variableElement.getAttributeValue( XMLAttributes.NAME.toString() );
    }

    protected Element getXMLRootStates( Element variableElement )
    {
        return variableElement.getChild( XMLTags.STATES.toString() );
    }

    protected void loadVariableAdvancedInformation( Element variableElement, ProbNet probNet, VariableType variableType,
                                                    NodeType nodeType, String variableName )
            throws PGMXParserException
    {
        String stringTimeSlice = variableElement.getAttributeValue( XMLAttributes.TIMESLICE.toString() );
        if ( stringTimeSlice != null )
        {
            variableName = variableName.replace( " [" + stringTimeSlice + "]", "" );
        }
        Variable variable;
        // Coordinates
        int x = getXMLXCoordinate( variableElement );
        int y = getXMLYCoordinate( variableElement );
        Double precision = getXMLPrecision( variableElement );
        // States & intervals
        State[] states = null;
        if ( ( nodeType == NodeType.CHANCE ) || ( nodeType == NodeType.DECISION ) )
        {
            if ( ( variableType == VariableType.FINITE_STATES ) || ( variableType == VariableType.DISCRETIZED ) )
            {
                Element statesElement = getXMLRootStates( variableElement );
                if ( statesElement != null )
                { // jlgozalo. 25/10/2009
                    states = getXMLStates( statesElement ); // previously
                    // without null control
                }
                else
                {
                    throw new PGMXParserException( "States list not found in finite states variable " + variableName,
                            variableElement );
                }
            }
            if ( variableType == VariableType.FINITE_STATES )
            {
                variable = new Variable( variableName, states );
            }
            else
            {
                // Common part to CONTINUOUS and DISCRETIZED
                try
                {
                    if ( variableType == VariableType.NUMERIC )
                    {
                        variable = getXMLContinuousVariable( variableElement, variableName );
                    }
                    else
                    { // DISCRETIZED variable. Read sub-intervals
                        Element thresholdsElement = variableElement.getChild( XMLTags.THRESHOLDS.toString() );
                        variable = getXMLDiscretizedVariable( thresholdsElement, states, variableName );
                    }
                }
                catch ( DataConversionException e )
                {
                    throw new PGMXParserException( "Data conversion " + "problem with variable " + variableName + ".",
                            variableElement );
                }
            }
        }
        else
        { // utility only??
            variable = new Variable( variableName );
            // decision criterion
            if ( nodeType == NodeType.UTILITY )
            {
                Element decisionCriteria = variableElement.getChild( XMLTags.CRITERION.toString() );
                if ( decisionCriteria != null )
                {
                    for ( Criterion criterion : probNet.getDecisionCriteria() )
                    {
                        if ( criterion.getCriterionName().equals( decisionCriteria.getAttributeValue( XMLAttributes.NAME.toString() ) ) )
                        {
                            variable.setDecisionCriterion( criterion );
                            break;
                        }
                    }
                    // If the node has not have Decision Criteria, set the first of the probNet.
                }
                else
                {
                    variable.setDecisionCriterion( probNet.getDecisionCriteria().get( 0 ) );
                }
            }
        }
        // Set timeSlice
        if ( stringTimeSlice != null )
        {
            variable.setTimeSlice( Integer.parseInt( stringTimeSlice ) );
        }
        // Set unit
        Element xMLUnit = variableElement.getChild( XMLTags.UNIT.toString() );
        if ( xMLUnit != null )
        {
            String unit = xMLUnit.getText();
            variable.setUnit( new StringWithProperties( unit ) );
        }
        // other additionalProperties
        HashMap<String, String> properties = getProperties( variableElement );

        // Remove obsolete properties
        if (properties.get(XMLTags.TITLE) != null) {
            properties.remove(XMLTags.TITLE);
        }
        if (properties.get(XMLTags.RELEVANCE) != null) {
            String relevance = properties.get(XMLTags.RELEVANCE);
            if (Double.parseDouble(relevance) == Node.defaultRelevance) {
                properties.remove(XMLTags.RELEVANCE);
            }
        }

        Node node = probNet.addNode( variable, nodeType );
        // always Observed property
        Element alwaysObserved = variableElement.getChild( XMLTags.ALWAYS_OBSERVED.toString() );
        if ( alwaysObserved != null )
        {
            node.setAlwaysObserved( true );
        }
        if ( properties.get( XMLTags.PURPOSE.toString() ) != null )
        {
            node.setPurpose( properties.get( XMLTags.PURPOSE.toString() ) );
            properties.remove( XMLTags.PURPOSE.toString() );
        }
        // TODO revisar el posible error al convertir un string a número
        if ( properties.get( XMLTags.RELEVANCE.toString() ) != null )
        {
            node.setRelevance( Double.valueOf( properties.get( XMLTags.RELEVANCE.toString() ) ) );
            properties.remove( XMLTags.RELEVANCE.toString() );
        }
        // OOPN start
        if ( variableElement.getAttribute( XMLAttributes.IS_INPUT.toString() ) != null )
        {
            boolean isInput =
                    Boolean.parseBoolean( variableElement.getAttribute( XMLAttributes.IS_INPUT.toString() ).getValue() );
            node.setInput( isInput );
        }
        // OOPN end
        node.setComment( getComment( variableElement ) );
        // with the created node, put position (x, y)
        node.setCoordinateX( x );
        node.setCoordinateY( y );
        if ( precision != null )
        {
            node.getVariable().setPrecision( precision );
        }
        if ( properties != null )
        {
            for ( String key : new ArrayList<>( properties.keySet() ) )
            {
                node.additionalProperties.put( key, properties.get( key ) );
            }
        }
    }

    protected Variable getVariable( Element element, ProbNet probNet )
            throws PGMXParserException
    {
        String variableName = getElementName( element );
        // strip the name from the time slice for backwards compatibility
        String timeSlice = element.getAttributeValue( XMLAttributes.TIMESLICE.toString() );
        variableName = variableName.replace( " [" + timeSlice + "]", "" );
        Variable variable = null;
        try
        {
            variable = ( timeSlice == null ) ? probNet.getVariable( variableName )
                    : probNet.getVariable( variableName, Integer.parseInt( timeSlice ) );
        }
        catch ( NodeNotFoundException e )
        {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append( "Unknown variable name " );
            errorMessage.append( variableName );
            if ( timeSlice != null )
            {
                errorMessage.append( " [" + timeSlice + "]" );
            }
            throw new PGMXParserException( errorMessage.toString(), element );
        }
        return variable;
    }

    protected String getElementName( Element element )
    {
        return element.getAttributeValue( XMLAttributes.NAME.toString() );
    }

    protected Double getXMLPrecision( Element variableElement )
    {
        Element precisionElement = variableElement.getChild( XMLTags.PRECISION.toString() );
        Double precision = null;
        if ( precisionElement != null )
        {
            String pString = precisionElement.getText();
            precision = new Double( pString );
        }
        return precision;
    }

    protected String getXMLPurpose( Element aditionalElement )
    {
        String purpose = "";
        if ( aditionalElement != null )
        {
            Element purposeElement = aditionalElement.getChild( XMLTags.PURPOSE.toString() );
            if ( purposeElement != null )
            {
                purpose = purposeElement.getText();
            }
        }
        return purpose;
    }

    /**
     * @param aditionalElement . <code>Element</code>
     * @return Double
     */
    protected Double getXMLRelevance( Element aditionalElement )
    {
        double relevance = Node.defaultRelevance;
        if ( aditionalElement != null )
        {
            Element relevanceElement = aditionalElement.getChild( XMLTags.RELEVANCE.toString() );
            if ( relevanceElement != null )
            {
                String relevanceString = relevanceElement.getText();
                relevance = new Double( relevanceString );
            }
        }
        return relevance;
    }

    /**
     * Reads additionalProperties that have not a clear classification.
     *
     * @param variableElement . <code>Element</code>
     * @return A <code>HashMap</code> with <code>key = String</code> and <code>value = Object</code>
     */
    protected HashMap<String, String> getProperties( Element variableElement )
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        Element others = variableElement.getChild( XMLTags.ADDITIONAL_PROPERTIES.toString() );
        if ( others != null )
        {
            List<Element> xmlProperties = getXMLChildren( others );
            for ( Element xmlProperty : xmlProperties )
            { // additionalProperties
                String key = getElementName( xmlProperty );
                String value = xmlProperty.getAttributeValue( XMLAttributes.VALUE.toString() );
                // try to discover the property type (double, boolean...)
                try
                { // try parse double
                    Double.parseDouble( value );
                    properties.put( key, value );
                }
                catch ( NumberFormatException nd )
                {
                    try
                    { // try parse int
                        Integer.parseInt( value );
                        properties.put( key, value );
                    }
                    catch ( NumberFormatException ni )
                    { // try parse boolean
                        Boolean bTrue = Boolean.parseBoolean( value );
                        boolean bFalse = value.equalsIgnoreCase( "false" );
                        if ( bTrue || bFalse )
                        {
                            properties.put( key, bTrue.toString() );
                        }
                        else
                        { // Nor double nor integer nor boolean -> String
                            properties.put( key, value );
                        }
                    }
                }
            }
        }
        return properties;
    }

    /**
     * @param variableElement . <code>Element</code>
     * @return variable type. <code>VariableType</code>
     */
    protected VariableType getXMLVariableType( Element variableElement )
    {
        VariableType variableType = null;
        String role = getStringXMLPotentialType( variableElement );
        if ( role.contentEquals( VariableType.FINITE_STATES.toString() ) )
        {
            variableType = VariableType.FINITE_STATES;
        }
        else if ( role.contentEquals( VariableType.NUMERIC.toString() ) )
        {
            variableType = VariableType.NUMERIC;
        }
        else if ( role.contentEquals( VariableType.DISCRETIZED.toString() ) )
        {
            variableType = VariableType.DISCRETIZED;
        }
        return variableType;
    }

    /**
     * @param variableElement . <code>Element</code>
     * @return node type. <code>NodeType</code>
     */
    protected NodeType getXMLNodeType( Element variableElement )
    {
        NodeType nodeType = null;
        // int i=0;
        String type = variableElement.getAttributeValue( XMLAttributes.ROLE.toString() );
        for ( NodeType iNodeType : NodeType.values() )
        {
            if ( type.contentEquals( iNodeType.toString() ) )
            {
                nodeType = iNodeType;
                break;
            }
        }
        return nodeType;
    }

    /**
     * @param variableElement . <code>Element</code>
     * @return Node X coordinate. <code>int</code>
     */
    protected int getXMLXCoordinate( Element variableElement )
    {
        Element coordinatesElement = variableElement.getChild( XMLTags.COORDINATES.toString() );
        int xCoordinate = 0;
        if ( coordinatesElement != null )
        {
            String xString = coordinatesElement.getAttributeValue( XMLAttributes.X.toString() );
            xCoordinate = Integer.parseInt( xString );
        }
        return xCoordinate;
    }

    /**
     * @param variableElement . <code>Element</code>
     * @return Node Y coordinate. <code>int</code>
     */
    protected int getXMLYCoordinate( Element variableElement )
    {
        Element coordinatesElement = variableElement.getChild( XMLTags.COORDINATES.toString() );
        int yCoordinate = 0;
        if ( coordinatesElement != null )
        {
            String yString = coordinatesElement.getAttributeValue( XMLAttributes.Y.toString() );
            yCoordinate = Integer.parseInt( yString );
        }
        return yCoordinate;
    }

    /**
     * @param variableElement . <code>Element</code>
     * @return variable states. <code>String[]</code>
     */
    protected State[] getXMLStates( Element variableElement )
    {
        List<Element> variableStatesElements = getStatesElements( variableElement );
        State[] states = new State[variableStatesElements.size()];
        int i = 0;
        for ( Element stateElement : variableStatesElements )
        {
            String stateName = getStateName( stateElement );
            states[i++] = new State( stateName );
        }
        return states;
    }

    protected String getStateName( Element stateElement )
    {
        return stateElement.getAttributeValue( XMLAttributes.NAME.toString() );
    }

    protected List<Element> getStatesElements( Element rootStates )
    {
        return rootStates.getChildren();
    }

    /**
     * @param variableElement . <code>Element</code>
     * @param variableName . <code>String</code>
     * @return A continuous variable. <code>Variable</code>
     */
    protected Variable getXMLContinuousVariable( Element variableElement, String variableName )
            throws DataConversionException
    {
        // Get thresholds
        List<Element> thresholds = null;
        Element thresholdsElement = variableElement.getChild( XMLTags.THRESHOLDS.toString() );
        if ( thresholdsElement != null )
        {
            thresholds = thresholdsElement.getChildren( XMLTags.THRESHOLD.toString() );
        }
        boolean leftClosedDefined = false;
        boolean leftClosed = false;
        boolean rightClosedDefined = false;
        boolean rightClosed = false;
        double min = Double.NEGATIVE_INFINITY;
        double max = Double.POSITIVE_INFINITY;
        if ( ( thresholds != null ) && ( thresholds.size() > 1 ) )
        {
            Element leftThreshold = thresholds.get( 0 );
            String minString = leftThreshold.getAttributeValue( XMLAttributes.VALUE.toString() );
            if ( minString.contentEquals( "-Infinity" ) )
            {
                min = Double.NEGATIVE_INFINITY;
                leftClosedDefined = true;
                leftClosed = false;
            }
            else
            {
                min = Double.parseDouble( minString );
            }
            if ( !leftClosedDefined )
            {
                String leftClosedString = leftThreshold.getAttributeValue( XMLAttributes.BELONGS_TO.toString() );
                if ( leftClosedString != null )
                {
                    if ( leftClosedString.contentEquals( XMLValues.LEFT.toString() ) )
                    {
                        leftClosed = true;
                    }
                    else if ( leftClosedString.contentEquals( XMLValues.RIGHT.toString() ) )
                    {
                        leftClosed = false;
                    }
                }
            }
            Element rightThreshold = thresholds.get( 1 );
            String maxString = rightThreshold.getAttributeValue( XMLAttributes.VALUE.toString() );
            if ( maxString.contentEquals( "+Infinity" ) )
            {
                max = Double.POSITIVE_INFINITY;
                rightClosedDefined = true;
                rightClosed = false;
            }
            else
            {
                max = Double.parseDouble( maxString );
            }
            if ( !rightClosedDefined )
            {
                String rightClosedString = rightThreshold.getAttributeValue( XMLAttributes.BELONGS_TO.toString() );
                if ( rightClosedString != null )
                {
                    if ( rightClosedString.contentEquals( XMLValues.LEFT.toString() ) )
                    {
                        rightClosed = true;
                    }
                    else if ( rightClosedString.contentEquals( XMLValues.RIGHT.toString() ) )
                    {
                        rightClosed = false;
                    }
                }
            }
        }
        double precision = 0.0;
        Element xMLPrecision = variableElement.getChild( XMLTags.PRECISION.toString() );
        if ( xMLPrecision != null )
        {
            precision = Double.parseDouble( xMLPrecision.getText() );
        }
        Variable variable = new Variable( variableName, leftClosed, min, max, rightClosed, precision );
        return variable;
    }

    /**
     * @param states
     * @param variableElement . <code>Element</code>
     * @param variableName . <code>String</code>
     * @return A discretized continuous variable. <code>Variable</code>
     * @throws DataConversionException
     */
    protected Variable getXMLDiscretizedVariable( Element variableElement, State[] states, String variableName )
            throws DataConversionException
    {
        Variable variable;
        // Continuous part. (continuous interval information is infered from
        // sub-intervals in discretized part (further on)
        if ( variableElement != null )
        {
            List<Element> subIntervals = getXMLChildren( variableElement );
            int numSubIntervals = subIntervals.size();
            double[] limits = new double[numSubIntervals];
            boolean[] belongsToLeftSide = new boolean[numSubIntervals];
            int numInterval = 0;
            for ( Element subInterval : subIntervals )
            {
                limits[numInterval] = Double.valueOf( subInterval.getAttributeValue( XMLAttributes.VALUE.toString() ) );
                if ( subInterval.getAttributeValue( XMLAttributes.BELONGS_TO.toString() ).contentEquals( "left" ) )
                    belongsToLeftSide[numInterval] = true;
                else
                    belongsToLeftSide[numInterval] = false;
                numInterval++;
                // TODO Seguir por aqui leyendo los thresholds
            }
            PartitionedInterval partitionedInterval = new PartitionedInterval( limits, belongsToLeftSide );
            variable = new Variable( variableName, states );
            // partitionedInterval, precision);
            // the order of the next two statement are important
            variable.setVariableType( VariableType.DISCRETIZED );
            variable.setPartitionedInterval( partitionedInterval );
        }
        else
            variable = new Variable( variableName, states );
        return variable;
    }

    /**
     * @param root . <code>Element</code>
     * @param probNet . <code>ProbNet</code>
     * @throws PGMXParserException PGMXParserException
     */
    protected void getLinks( Element root, ProbNet probNet )
            throws PGMXParserException
    {
        Element xmlLinksRoot = root.getChild( XMLTags.LINKS.toString() );
        if ( xmlLinksRoot != null )
        {
            List<Element> xmlLinks = getXMLChildren( xmlLinksRoot );
            for ( Element xmlLink : xmlLinks )
            {
                // get link information from xmlLink
                List<Element> variablesElement = xmlLink.getChildren( XMLTags.VARIABLE.toString() );
                // for the time being the name contains the time slice
                try
                {
                    Variable variable1 = getVariable( variablesElement.get( 0 ), probNet );
                    Variable variable2 = getVariable( variablesElement.get( 1 ), probNet );
                    Node node1 = probNet.getNode( variable1 );
                    Node node2 = probNet.getNode( variable2 );
                    boolean directed = xmlLink.getAttribute( XMLAttributes.DIRECTED.toString() ).getBooleanValue();
                    // create link
                    probNet.addLink( variable1, variable2, directed );
                    // read link restriction potential
                    Element xmlPotential = xmlLink.getChild( XMLTags.POTENTIAL.toString() );
                    if ( xmlPotential != null )
                    {
                        Potential potential = getPotential( xmlPotential, probNet );
                        Link<Node> link = probNet.getLink( node1, node2, directed );
                        link.initializesRestrictionsPotential();
                        link.setRestrictionsPotential( potential );
                    }
                    Element xmlRevelationCondition = xmlLink.getChild( XMLTags.REVELATION_CONDITIONS.toString() );
                    if ( xmlRevelationCondition != null )
                    {
                        Link<Node> link = probNet.getLink( node1, node2, directed );
                        getRevelationConditions( xmlRevelationCondition, link );
                    }
                }
                catch ( DataConversionException e )
                {
                    throw new PGMXParserException( "Data conversion exception in PGMXReader.getLinks()", xmlLink );
                }
                catch ( NodeNotFoundException e )
                {
                    throw new PGMXParserException( "Node not found in PGMXReader.getLinks()", xmlLink );
                }
            }
        }
    }

    protected void getRevelationConditions( Element root, Link<Node> link )
            throws PGMXParserException
    {
        Node node = link.getNode1();
        Variable var = node.getVariable();
        List<Element> xmlStates = root.getChildren( XMLTags.STATE.toString() );
        for ( Element elementState : xmlStates )
        {
            String stateName = getElementName( elementState );
            int stateIndex;
            try
            {
                stateIndex = var.getStateIndex( stateName );
                link.addRevealingState( var.getStates()[stateIndex] );
            }
            catch ( InvalidStateException e )
            {
                throw new PGMXParserException( "Invalid state \"" + stateName + "\" for variable \"" + var.getName()
                        + "\"", elementState );
            }
        }
        List<Element> xmlThresholds = root.getChildren( XMLTags.THRESHOLD.toString() );
        if ( xmlThresholds.size() > 0 )
        {
            for ( int i = 0; i < xmlThresholds.size(); i += 2 )
            {
                double[] limits = new double[2];
                boolean[] belongsToLeftSide = new boolean[2];
                for ( int index = 0; index < 2; index++ )
                {
                    Element subInterval = xmlThresholds.get( i + index );
                    limits[index] = Double.valueOf( subInterval.getAttributeValue( XMLAttributes.VALUE.toString() ) );
                    belongsToLeftSide[index] =
                            subInterval.getAttributeValue( XMLAttributes.BELONGS_TO.toString() ).contentEquals( "left" );
                }
                PartitionedInterval partitionedInterval = new PartitionedInterval( limits, belongsToLeftSide );
                link.addRevealingInterval( partitionedInterval );
            }
        }
    }

    /**
     * @param root . <code>Element</code>
     * @param probNet . <code>ProbNet</code>
     * @throws PGMXParserException
     */
    protected void getPotentials( Element root, ProbNet probNet )
            throws PGMXParserException
    {
        // Pool of names for those potentials declared in this net

        Element xmlPotentialsRoot = getRootPotentials( root );
        if ( xmlPotentialsRoot != null )
        {

            List<Element> xmlPotentials = getPotentialsElements( xmlPotentialsRoot );
            for ( Element xmlPotential : xmlPotentials )
            {
                Potential potential = getPotential( xmlPotential, probNet );
                probNet.addPotential( potential );
            }
        }
    }

    protected List<Element> getPotentialsElements( Element xmlPotentialsRoot )
    {
        return xmlPotentialsRoot.getChildren();
    }

    protected Element getRootPotentials( Element root )
    {
        return root.getChild( XMLTags.POTENTIALS.toString() );
    }

    protected List<Variable> getReferencedVariables( Element xmlPotential, ProbNet probNet )
            throws PGMXParserException
    {
        // get variables
        int a;
        Element xmlRootVariables = getXMLPotentialVariables( xmlPotential );
        List<Variable> variables = new ArrayList<Variable>();
        // List of variables referenced in this potential
        if ( xmlRootVariables != null )
        {
            List<Element> xmlVariables = getXMLChildren( xmlRootVariables );
            int numVariables = xmlVariables.size();
            for ( int i = 0; i < numVariables; i++ )
            {
                Variable variable = getVariable( xmlVariables.get( i ), probNet );
                if ( !variables.contains( variable ) )
                {
                    variables.add( variable );
                }
            }
        }

        return variables;
    }

    /**
     * @author myebra
     * @param xmlPotential
     * @param probNet
     * @return
     * @throws PGMXParserException
     */
    protected List<TreeADDBranch> getTreeADDBranches(Element xmlPotential, ProbNet probNet, Variable rootVariable,
                                                     PotentialRole xmlRole, List<Variable> variables )
            throws PGMXParserException
    {
        // get branches
        Element xmlRootBranches = xmlPotential.getChild( XMLTags.BRANCHES.toString() );
        List<TreeADDBranch> branches = new ArrayList<>();
        List<Variable> parentVariables = new ArrayList<>( variables );
        // List of variables referenced in this potential
        if ( xmlRootBranches != null )
        {
            List<Element> xmlBranches = getXMLChildren( xmlRootBranches );
            int numBranches = xmlBranches.size();
            for ( int i = 0; i < numBranches; i++ )
            {
                Element xmlBranch = xmlBranches.get( i );
                Element xmlSubpotential = xmlBranch.getChild( XMLTags.POTENTIAL.toString() );
                Element xmlReference = xmlBranch.getChild( XMLTags.REFERENCE.toString() );
                Element xmlLabel = xmlBranch.getChild( XMLTags.LABEL.toString() );
                Potential potential = null;
                String reference = null;
                if ( xmlSubpotential != null )
                {
                    potential = getPotential( xmlSubpotential, probNet, xmlRole );
                    // Hack for backwards compatibility of nested TreeADDPotentials
                    // that don't specify a list of variables
                    if ( potential instanceof TreeADDPotential && potential.getVariables().isEmpty()
                            && !parentVariables.isEmpty() )
                    {
                        potential.setVariables( parentVariables );
                    }
                }
                else if ( xmlReference != null )
                {
                    reference = xmlReference.getText();
                }
                else
                {
                    throw new PGMXParserException( "A TreeADD branch should specify either a potential or a reference",
                            xmlBranch );
                }
                TreeADDBranch branch = null;
                if ( rootVariable.getVariableType() == VariableType.FINITE_STATES
                        || rootVariable.getVariableType() == VariableType.DISCRETIZED )
                {
                    List<State> states = getBranchStates( xmlBranch, rootVariable );
                    branch =
                            ( potential != null ) ? new TreeADDBranch( states, rootVariable, potential, parentVariables )
                                    : new TreeADDBranch( states, rootVariable, reference, parentVariables );
                }
                else if ( rootVariable.getVariableType() == VariableType.NUMERIC )
                {
                    List<Threshold> thresholds = getThresholds( xmlBranch );
                    branch = ( potential != null )
                            ? new TreeADDBranch( thresholds.get( 0 ), thresholds.get( 1 ), rootVariable,
                            potential, parentVariables )
                            : new TreeADDBranch( thresholds.get( 0 ), thresholds.get( 1 ), rootVariable,
                            reference, parentVariables );
                }
                if ( xmlLabel != null )
                {
                    branch.setLabel( xmlLabel.getText() );
                }
                branches.add( branch );
            }
        }
        return branches;
    }

    /**
     * @author myebra
     * @param xmlBranch
     * @return
     * @throws PGMXParserException
     */
    protected List<Threshold> getThresholds( Element xmlBranch )
            throws PGMXParserException
    {
        List<Threshold> thresholds = new ArrayList<Threshold>();
        Element xmlRootThresholds = xmlBranch.getChild( XMLTags.THRESHOLDS.toString() );
        if ( xmlRootThresholds != null )
        {
            List<Element> xmlThresholds = getXMLChildren( xmlRootThresholds );
            int numThresholds = xmlThresholds.size();
            if ( numThresholds != 2 )
                throw new PGMXParserException( "A TreeADDD branch can only have two thresholds", xmlBranch );
            for ( int i = 0; i < numThresholds; i++ )
            {
                Element xmlThreshold = xmlThresholds.get( i );
                Float value = Float.parseFloat( xmlThreshold.getAttributeValue( XMLAttributes.VALUE.toString() ) );
                String belongsTo = xmlThreshold.getAttributeValue( XMLAttributes.BELONGS_TO.toString() );
                boolean belongsToLeft = false;
                if ( belongsTo.equals( "left" ) )
                {
                    belongsToLeft = true;
                }
                else if ( belongsTo.equals( "right" ) )
                {
                    belongsToLeft = false;
                }
                Threshold threshold = new Threshold( value, belongsToLeft );
                thresholds.add( threshold );
            }
        }
        return thresholds;
    }

    /**
     * author mayebra
     *
     * @param xmlBranch
     * @param topVariable
     * @return
     * @throws PGMXParserException
     */
    protected List<State> getBranchStates( Element xmlBranch, Variable topVariable )
            throws PGMXParserException
    {
        List<State> states = new ArrayList<State>();
        Element xmlRootStates = xmlBranch.getChild( XMLTags.STATES.toString() );
        if ( xmlRootStates != null )
        {
            List<Element> xmlStates = getXMLChildren( xmlRootStates );
            int numStates = xmlStates.size();
            for ( int i = 0; i < numStates; i++ )
            {
                Element xmlState = xmlStates.get( i );
                String stateName = getElementName( xmlState );
                int stateIndex = -1;
                try
                {
                    stateIndex = topVariable.getStateIndex( stateName );
                }
                catch ( InvalidStateException e )
                {
                    throw new PGMXParserException( "Unknown state name +'" + stateName + "'", xmlState );
                }
                states.add( topVariable.getStates()[stateIndex] );
            }
        }
        return states;
    }

    /**
     * @param xmlPotential
     * @param probNet
     * @param xmlRole
     * @param variables
     * @return UniformPotential
     */
    protected UniformPotential getUniformPotential(Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                                   List<Variable> variables ) {
        return new UniformPotential( variables, xmlRole );
    }

    /**
     * @param xmlPotential
     * @param probNet
     * @param xmlRole
     * @param variables
     * @return ProductPotential
     */
    protected ProductPotential getProductPotential(Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                                   List<Variable> variables ) {
        return new ProductPotential( variables, xmlRole );
    }

    /**
     * @param xmlPotential
     * @param probNet
     * @param xmlRole
     * @param variables
     * @return
     */
    protected TablePotential getTablePotential(Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                               List<Variable> variables ) {
        Element xmlRootTable = getXMLRootTable( xmlPotential );
        double[] table = parseDoubles( xmlRootTable.getTextNormalize() );
        TablePotential tablePotential = new TablePotential( variables, xmlRole, table );

        Element xmlRootUncertainValues = xmlPotential.getChild( XMLTags.UNCERTAIN_VALUES.toString() );
        if ( xmlRootUncertainValues != null ) {
            tablePotential.setUncertainValues( getUncertainValues( xmlRootUncertainValues ) );
        }
        return tablePotential;
    }

    /**
     * @param xmlPotential
     * @param probNet
     * @param xmlRole
     * @param variables
     * @return
     */
    protected ExactDistrPotential getExactDistrPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                                          List<Variable> variables ) {
        Element xmlRootTable = getXMLRootTable( xmlPotential );
        double[] table = parseDoubles( xmlRootTable.getTextNormalize() );
        ExactDistrPotential exactDistrPotential = new ExactDistrPotential( variables, xmlRole, table );

        Element xmlRootUncertainValues = xmlPotential.getChild( XMLTags.UNCERTAIN_VALUES.toString() );
        if ( xmlRootUncertainValues != null ) {
            exactDistrPotential.setUncertainValues( getUncertainValues( xmlRootUncertainValues ) );
        }
        return exactDistrPotential;
    }

    /**
     * @param xmlPotential
     * @return
     */
    protected Element getXMLRootTable( Element xmlPotential ) {
        return xmlPotential.getChild( XMLTags.VALUES.toString() );
    }

    /**
     * @param xmlRootUncertainTable
     * @return
     */
    protected UncertainValue[] getUncertainValues(Element xmlRootUncertainTable ) {
        int valuesSize;
        List<Element> values;
        values = xmlRootUncertainTable.getChildren();
        valuesSize = values.size();
        UncertainValue[] uncertainTable = new UncertainValue[valuesSize];
        for ( int i = 0; i < valuesSize; i++ )
        {
            Element xmlUncertainValue = values.get( i );
            uncertainTable[i] = getUncertainValue( xmlUncertainValue );
        }
        return uncertainTable;
    }

    /**
     * @param xmlUncertainValue
     * @return
     */
    protected UncertainValue getUncertainValue( Element xmlUncertainValue ) {
        UncertainValue auxUncertainValue = null;
        String functionName = xmlUncertainValue.getAttributeValue( XMLAttributes.DISTRIBUTION.toString() );
        if ( functionName != null ) {
            String name = getElementName( xmlUncertainValue );
            String[] arguments = xmlUncertainValue.getTextNormalize().split( " " );
            double[] parameters = new double[arguments.length];
            for ( int i = 0; i < parameters.length; ++i ) {
                parameters[i] = Double.parseDouble( arguments[i] );
            }
            ProbDensFunction function = ProbDensFunctionManager.getUniqueInstance().newInstance(functionName, parameters);
            auxUncertainValue = new UncertainValue( function, name );
        }
        return auxUncertainValue;
    }

    /**
     * @param probNet . <code>ProbNet</code>
     * @param xmlPotential . <code>Element</code>
     * @return <code>Potential</code>
     * @throws PGMXParserException
     */
    protected Potential getPotential( Element xmlPotential, ProbNet probNet )
            throws PGMXParserException {
        PotentialRole xmlRole = getPotentialRole( xmlPotential );
        return getPotential( xmlPotential, probNet, xmlRole );
    }

    /**
     * @param xmlPotential
     * @return
     */
    protected PotentialRole getPotentialRole( Element xmlPotential ) {
        String xmlPotentialRole = xmlPotential.getAttributeValue( XMLAttributes.ROLE.toString() );
        PotentialRole xmlRole;
        if ( xmlPotentialRole.equalsIgnoreCase( "utility" ) ) {
            xmlRole = PotentialRole.UNSPECIFIED;
        }
        else {
            xmlRole = PotentialRole.getEnumMember( xmlPotentialRole );
        }
        return xmlRole;
    }

    /**
     * @param probNet . <code>ProbNet</code>
     * @param xmlPotential . <code>Element</code>
     * @return <code>Potential</code>
     * @throws PGMXParserException
     */
    protected Potential getPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole )
            throws PGMXParserException {
        Potential potential = null;
        // get type and role of potential
        String sXmlPotentialType = getStringXMLPotentialType( xmlPotential );
        List<Variable> variables = getReferencedVariables( xmlPotential, probNet );
        Element xmlUtilityVariable = xmlPotential.getChild( XMLTags.UTILITY_VARIABLE.toString() );
        boolean utilityVariableElement=false;
        if ( xmlUtilityVariable != null ) {
            Variable utilityVariable = getVariable( xmlUtilityVariable, probNet );
            variables.add( 0, utilityVariable  );
            utilityVariableElement=true;
        }
        if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( UniformPotential.class ) ) ) {
            potential = getUniformPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( TablePotential.class ) ) ) {
            // Compatibility with old utility variable use
            if (utilityVariableElement) {
                potential = getExactDistrPotential( xmlPotential, probNet, xmlRole, variables );
            }
            else {
                potential = getTablePotential( xmlPotential, probNet, xmlRole, variables );
            }
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( TreeADDPotential.class ) ) ) {
            potential = getTreeADDPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( CycleLengthShift.class ) ) )
        {
            potential = getCycleLengthShiftPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( SameAsPrevious.class ) ) ) {
            potential = getSameAsPrevious( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( SumPotential.class ) ) ) {
            if (utilityVariableElement) {
                xmlRole = PotentialRole.UNSPECIFIED;
                potential = getSumPotential( xmlPotential, probNet, xmlRole, variables );
            }
            else {
                xmlRole = PotentialRole.CONDITIONAL_PROBABILITY;
                potential = getSumPotential( xmlPotential, probNet, xmlRole, variables );
            }
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( ProductPotential.class ) ) ) {
            if (utilityVariableElement) {
                xmlRole = PotentialRole.UNSPECIFIED;
                potential = getProductPotential( xmlPotential, probNet, xmlRole, variables );
            }
            else {
                xmlRole = PotentialRole.CONDITIONAL_PROBABILITY;
                potential = getProductPotential( xmlPotential, probNet, xmlRole, variables );
            }
        }
        else if ( sXmlPotentialType.equals( "ICIModel" ) ) {
            potential = getICIPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( WeibullHazardPotential.class ) ) ) {
            potential = getWeibullPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( ExponentialHazardPotential.class ) ) ) {
            potential = getExponentialHazardPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( LinearCombinationPotential.class ) )
                || PotentialManager.getAlternativeNames( LinearCombinationPotential.class ).contains( sXmlPotentialType ) ) {
            potential = getLinearRegressionPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( FunctionPotential.class ) ) ) {
            potential = getFunctionPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( DeltaPotential.class ) ) ) {
            potential = getDeltaPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( ExponentialPotential.class ) ) ) {
            potential = getExponentialPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( BinomialPotential.class ) ) ) {
            potential = getBinomialPotential( xmlPotential, probNet, xmlRole, variables );
        }
        else if ( sXmlPotentialType.equals( PotentialManager.getPotentialName( ExactDistrPotential.class ) ) ) {
            potential = getExactDistrPotential( xmlPotential, probNet, xmlRole, variables );
        } else {
            throw new PGMXParserException( "Potential type " + sXmlPotentialType + " not supported", xmlPotential );
        }

        Element xmlComment = xmlPotential.getChild( XMLTags.COMMENT.toString() );
        if ( xmlComment != null ) {
            potential.setComment( xmlComment.getText() );
        }
        return potential;
    }

    protected String getStringXMLPotentialType( Element xmlPotential ) {
        return xmlPotential.getAttributeValue( XMLAttributes.TYPE.toString() );
    }

    /**
     * @author myebra
     * @param xmlPotential
     * @param probNet
     * @param xmlRole
     * @param variables
     * @return
     * @throws PGMXParserException
     */
    protected TreeADDPotential getTreeADDPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                                    List<Variable> variables )
            throws PGMXParserException
    {
        Variable topVariable = null;
        // // Read the PotentialRole: innerPotentials of this TreeADD must have the
        // // same PotentialRole
        // if (xmlRole != PotentialRole.UTILITY && xmlRole != PotentialRole.CONDITIONAL_PROBABILITY) {
        // throw new PGMXParserException("Potential role "
        // + xmlRole.toString()
        // + "not supported inside TreeADD potential", xmlPotential);
        // }
        Element xmlTopVariable = xmlPotential.getChild( XMLTags.TOP_VARIABLE.toString() );
        if ( xmlTopVariable != null )
        {
            topVariable = getVariable( xmlTopVariable, probNet );
        }
        // Recursive reading of tree structure
        List<TreeADDBranch> branches = getTreeADDBranches( xmlPotential, probNet, topVariable, xmlRole, variables );
        // Builds the tree potential from the graph
        TreeADDPotential treeADDPotential = new TreeADDPotential( variables, topVariable, xmlRole, branches );

        return treeADDPotential;
    }

    /**
     * Creates an instance of ICIPotential given an XML node
     *
     * @param xmlPotential
     * @param probNet
     * @param xmlRole
     * @param variables
     * @return
     * @throws PGMXParserException
     */
    protected Potential getICIPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                         List<Variable> variables )
            throws PGMXParserException
    {
        Element xmlModel = xmlPotential.getChild( XMLTags.MODEL.toString() );
        ICIPotential iciPotential = null;
        if ( xmlModel.getText().equals( MaxPotential.class.getAnnotation( PotentialType.class ).name() )
                || xmlModel.getText().equals( "GeneralizedMax" ) )
        {
            iciPotential = new MaxPotential( variables );
        }
        else if ( xmlModel.getText().equals( MinPotential.class.getAnnotation( PotentialType.class ).name() )
                || xmlModel.getText().equals( "GeneralizedMin" ) )
        {
            iciPotential = new MinPotential( variables );
        }
        else if ( xmlModel.getText().equals( TuningPotential.class.getAnnotation( PotentialType.class ).name() ) )
        {
            iciPotential = new TuningPotential( variables );
        }
        for ( Element subpotential : xmlPotential.getChild( XMLTags.SUBPOTENTIALS.toString() ).getChildren() )
        {
            List<Element> subpotentialVariables = subpotential.getChild( XMLTags.VARIABLES.toString() ).getChildren();
            double[] values = parseDoubles( subpotential.getChild( XMLTags.VALUES.toString() ).getTextNormalize() );
            if ( subpotentialVariables.size() > 1 )
            {
                Variable variable = getVariable( subpotentialVariables.get( 1 ), probNet );
                iciPotential.setNoisyParameters( variable, values );
            }
            else
            {
                getVariable( subpotentialVariables.get( 0 ), probNet );
                iciPotential.setLeakyParameters( values );
            }
        }
        return iciPotential;
    }

    /**
     * @param xmlPotential
     * @param probNet
     * @param xmlRole
     * @param variables
     * @return
     * @throws PGMXParserException
     */
    protected Potential getWeibullPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                             List<Variable> variables )
            throws PGMXParserException
    {
        WeibullHazardPotential potential = new WeibullHazardPotential( variables, xmlRole );
        Element xmlTimeVariable = xmlPotential.getChild( XMLTags.TIME_VARIABLE.toString() );
        if ( xmlTimeVariable != null )
        {
            String variableName = getElementName( xmlTimeVariable );
            String timeSlice = xmlTimeVariable.getAttributeValue( XMLAttributes.TIMESLICE.toString() );
            try
            {
                Variable timeVariable = probNet.getVariable( variableName, Integer.parseInt( timeSlice ) );
                potential.setTimeVariable( timeVariable );
            }
            catch ( NodeNotFoundException e )
            {
                e.printStackTrace();
                throw new PGMXParserException( e.getMessage(), xmlTimeVariable );
            }
        }
        Element xmlLog = xmlPotential.getChild( XMLTags.LOG.toString() );
        potential.setLog( xmlLog == null || Boolean.parseBoolean( xmlLog.getValue() ) );
        getRegressionPotential( xmlPotential, potential );
        return potential;
    }

    protected Potential getExponentialHazardPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                                       List<Variable> variables )
    {
        ExponentialHazardPotential potential = new ExponentialHazardPotential( variables, xmlRole );
        Element xmlLog = xmlPotential.getChild( XMLTags.LOG.toString() );
        potential.setLog( xmlLog == null || Boolean.parseBoolean( xmlLog.getValue() ) );
        getRegressionPotential( xmlPotential, potential );
        return potential;
    }

    protected Potential getExponentialPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                                 List<Variable> variables )
    {
        ExponentialPotential potential = new ExponentialPotential( variables, xmlRole );
        getRegressionPotential( xmlPotential, potential );
        return potential;
    }

    protected Potential getLinearRegressionPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                                      List<Variable> variables )
    {
        LinearCombinationPotential potential = new LinearCombinationPotential( variables, xmlRole );
        getRegressionPotential( xmlPotential, potential );
        return potential;
    }

    protected void getRegressionPotential( Element xmlPotential, GLMPotential potential )
    {
        Element xmlCoefficients = xmlPotential.getChild( XMLTags.COEFFICIENTS.toString() );
        potential.setCoefficients( parseDoubles( xmlCoefficients.getText() ) );

        Element xmlCovariates = xmlPotential.getChild( XMLTags.COVARIATES.toString() );
        if ( xmlCovariates != null )
        {
            potential.setCovariates( getCovariates( xmlCovariates ) );
        }

        Element xmlCovarianceMatrix = xmlPotential.getChild( XMLTags.COVARIANCE_MATRIX.toString() );
        Element xmlCholeskyDecomposition = xmlPotential.getChild( XMLTags.CHOLESKY_DECOMPOSITION.toString() );
        if ( xmlCovarianceMatrix != null )
        {
            potential.setCovarianceMatrix( parseDoubles( xmlCovarianceMatrix.getText() ) );
        }
        else if ( xmlCholeskyDecomposition != null )
        {
            potential.setCholeskyDecomposition( parseDoubles( xmlCholeskyDecomposition.getText() ) );
        }
    }

    // CMI Function
    protected Potential getFunctionPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                              List<Variable> variables )
    {
        FunctionPotential potential = new FunctionPotential( variables, xmlRole );
        Element xmlFunction = xmlPotential.getChild( XMLTags.FUNCTION.toString() );
        potential.setFunction( xmlFunction.getText() );
        return potential;
    }

    // CMF

    /**
     * @param xmlPotential
     * @param probNet
     * @param role
     * @param variables
     * @return
     * @throws PGMXParserException
     */
    protected Potential getDeltaPotential( Element xmlPotential, ProbNet probNet, PotentialRole role,
                                           List<Variable> variables )
            throws PGMXParserException
    {
        DeltaPotential deltaPotential = null;

        Element xmlNumericValue = xmlPotential.getChild( XMLTags.NUMERIC_VALUE.toString() );
        Element xmlState = xmlPotential.getChild( XMLTags.STATE.toString() );
        Element xmlStateIndex = xmlPotential.getChild( XMLTags.STATE_INDEX.toString() );
        if ( xmlNumericValue != null )
        {
            double value = Double.parseDouble( xmlNumericValue.getText() );
            deltaPotential = new DeltaPotential( variables, role, value );
        }
        else if ( xmlState != null )
        {
            State state = null;
            try {
                state = variables.get(0).getState( xmlState.getText() );
            } catch (InvalidStateException e) {
                e.printStackTrace();
            }
            deltaPotential = new DeltaPotential( variables, role, state );
        }
        else if ( xmlStateIndex != null )
        {
            int stateIndex = Integer.parseInt( xmlStateIndex.getText() );
            State state = variables.get( 0 ).getStates()[stateIndex];
            deltaPotential = new DeltaPotential( variables, role, state );
        }
        else
        {
            throw new PGMXParserException( "A delta potential has to specify either a State, a StateIndex or a NumericValue",
                    xmlPotential );
        }
        return deltaPotential;
    }

    /**
     * @author carmenyago
     * @param xmlPotential
     * @param probNet
     * @param role
     * @param variables
     * @return
     * @throws PGMXParserException
     */
    protected Potential getBinomialPotential( Element xmlPotential, ProbNet probNet, PotentialRole role,
                                              List<Variable> variables )
            throws PGMXParserException
    {
        BinomialPotential binomialPotential = null;

        Element xmlNumberOfCases = xmlPotential.getChild( XMLTags.NUMBER_OF_CASES.toString() );
        Element xmlTheta = xmlPotential.getChild( XMLTags.THETA.toString() );

        if ( ( xmlNumberOfCases != null ) && ( xmlTheta != null ) )
        {
            int N = Integer.parseInt( xmlNumberOfCases.getText() );
            double theta = Double.parseDouble( xmlTheta.getText() );
            binomialPotential = new BinomialPotential( variables, role, N, theta );
        }
        else
        {
            throw new PGMXParserException( "A binomial potential has to specify a Number of Cases and a probability theta",
                    xmlPotential );
        }
        return binomialPotential;
    }

    protected Potential getConditionalGaussianPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                                         List<Variable> variables )
            throws PGMXParserException
    {
        // TODO - Descomentar
        return null;
        // ConditionalGaussianPotential potential = new ConditionalGaussianPotential(variables, xmlRole);
        // Element xmlSubpotentialsElement = xmlPotential.getChild(XMLTags.SUBPOTENTIALS.toString());
        // List<Element> xmlSubpotentialElements = xmlSubpotentialsElement.getChildren();
        // Potential meanPotential = getPotential(xmlSubpotentialElements.get(0), probNet);
        // meanPotential.setUtilityVariable(new Variable("Mean"));
        // meanPotential.setPotentialRole(PotentialRole.UTILITY);
        // Potential variancePotential = getPotential(xmlSubpotentialElements.get(1), probNet);
        // variancePotential.setUtilityVariable(new Variable("Variance"));
        // variancePotential.setPotentialRole(PotentialRole.UTILITY);
        // potential.setMean(meanPotential);
        // potential.setVariance(variancePotential);
        // return potential;
    }

    protected Potential getDiscretizedCauchyPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                                       List<Variable> variables )
            throws PGMXParserException
    {
        // TODO - Descomentar
        return null;
        // DiscretizedCauchyPotential potential = new DiscretizedCauchyPotential(variables, xmlRole);
        // Element xmlSubpotentialsElement = xmlPotential.getChild(XMLTags.SUBPOTENTIALS.toString());
        // List<Element> xmlSubpotentialElements = xmlSubpotentialsElement.getChildren();
        // Potential medianPotential = getPotential(xmlSubpotentialElements.get(0), probNet);
        // medianPotential.setUtilityVariable(new Variable("Median"));
        // medianPotential.setPotentialRole(PotentialRole.UTILITY);
        // Potential scalePotential = getPotential(xmlSubpotentialElements.get(1), probNet);
        // scalePotential.setUtilityVariable(new Variable("Scale"));
        // scalePotential.setPotentialRole(PotentialRole.UTILITY);
        // potential.setMedian(medianPotential);
        // potential.setScale(scalePotential);
        // return potential;
    }

    protected Potential getSumPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                         List<Variable> variables )
    {
        return new SumPotential( variables, xmlRole );
    }

    protected Potential getSameAsPrevious( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                           List<Variable> variables )
            throws PGMXParserException
    {
        SameAsPrevious sameAsPrevious = null;
        Variable variable = variables.get( 0 );
        if ( variable.isTemporal() )
        {
            sameAsPrevious = new SameAsPrevious( variables );
        }
        else
        {
            throw new PGMXParserException( "XMLReader exception: "
                    + "can not assign a SameAsPrevious potential to static variable", xmlPotential );
        }
        return sameAsPrevious;
    }

    /***
     * Gets the <code>CycleLengthShift</code> potential for temporal chance variable
     *
     * @param xmlPotential
     * @param probNet
     * @param xmlRole
     * @param variables
     * @return
     * @throws PGMXParserException
     */
    protected Potential getCycleLengthShiftPotential( Element xmlPotential, ProbNet probNet, PotentialRole xmlRole,
                                                      List<Variable> variables )
            throws PGMXParserException
    {
        Potential cycleLengthShift;

        Variable variable = variables.get( 0 );
        if ( variable.isTemporal() )
        {
            cycleLengthShift = new CycleLengthShift( variables, probNet.getCycleLength() );
        }
        else
        {
            throw new PGMXParserException( "XMLReader exception: " + "can not assign a CycleLengthShift potential: "
                    + variables + " to a static variable: " + variable, xmlPotential );
        }

        return cycleLengthShift;
    }

    protected double[] parseDoubles( String string )
    {
        String[] sValues = string.split( " " );
        double[] table = new double[sValues.length];
        int i = 0;
        for ( String sValue : sValues )
        {
            table[i++] = Double.parseDouble( sValue );
        }
        return table;
    }

    protected String[] getCovariates( Element xmlCovariates )
    {
        String[] covariates = new String[xmlCovariates.getChildren().size()];
        int i = 0;
        for ( Element xmlCovariate : xmlCovariates.getChildren() )
        {
            covariates[i++] = xmlCovariate.getText();
        }
        return covariates;
    }

    /**
     * transform a text in a HTML string, if possible by a full substitution of the special characters "SymbolLT" and
     * "SymbolGT" in the equivalent "&lt;" and "&gt;" Please, pay attention that the equivalent format "&amp;lt;" and "&amp;gt;" are not
     * used here as JDOM is using the character "&amp;" to start a definition of an entity Ref class, so we need to avoid
     * it.
     */
    protected String textToHtml( String htmlSection )
    {
        String result = htmlSection;
        result = result.replaceAll( "SymbolLT", "<" );
        result = result.replaceAll( "SymbolGT", ">" );
        return result;
    }

    // OOPN start
    /**
     * @param netName
     * @param root . <code>Element</code>
     * @param probNet . <code>ProbNet</code>
     * @param classes
     * @throws PGMXParserException
     */
    protected void getOOPN( String netName, Element root, ProbNet probNet, Map<String, ProbNet> classes )
            throws PGMXParserException
    {
        if ( probNet instanceof OOPNet )
        {
            OOPNet ooNet = (OOPNet) probNet;
            Element xmlOONRoot = root.getChild( XMLTags.OOPN.toString() );
            if ( xmlOONRoot != null )
            {
                LinkedHashMap<String, ProbNet> localClasses = new LinkedHashMap<>();
                Element xmlClassesRoot = xmlOONRoot.getChild( XMLTags.CLASSES.toString() );
                if ( xmlClassesRoot != null )
                {
                    List<Element> xmlClasses = getXMLChildren( xmlClassesRoot );
                    for ( Element xmlClass : xmlClasses )
                    {
                        String name = xmlClass.getAttributeValue( "name" );
                        localClasses.put( name, getProbNet( xmlClass, name, localClasses ) );
                    }
                    ooNet.setClasses( localClasses );
                }
                classes.putAll( localClasses );
                Element xmlInstancesRoot = xmlOONRoot.getChild( XMLTags.INSTANCES.toString() );
                if ( xmlInstancesRoot != null )
                {
                    List<Element> xmlInstances = getXMLChildren( xmlInstancesRoot );
                    for ( Element xmlInstance : xmlInstances )
                    {
                        String name = xmlInstance.getAttributeValue( "name" );
                        boolean isInput = Boolean.parseBoolean( xmlInstance.getAttributeValue( "isInput" ) );
                        String folder = new File( netName ).getParent();
                        String className = xmlInstance.getAttributeValue( "class" );
                        if ( !classes.containsKey( className ) )
                        {
                            try
                            {
                                classes.put( className, loadProbNetInfo( folder + "\\" + className ).getProbNet() );
                            }
                            catch ( ParserException e )
                            {
                                throw new PGMXParserException( e.getMessage(), xmlInstance );
                            }
                        }
                        ProbNet classNet = classes.get( className );
                        List<Node> instanceNodes = new ArrayList<Node>();
                        // build this list from current node list and classNet
                        for ( Node node : classNet.getNodes() )
                        {
                            try
                            {
                                instanceNodes.add( probNet.getNode( name + "." + node.getName() ) );
                            }
                            catch ( NodeNotFoundException e )
                            {
                                throw new PGMXParserException( e.getMessage(), xmlInstance );
                            }
                        }
                        Instance instance = new Instance( name, classNet, instanceNodes, isInput );
                        try
                        {
                            ooNet.addInstance( instance );
                        }
                        catch ( InstanceAlreadyExistsException ignore )
                        {
                        }
                        if ( xmlInstance.getAttributeValue( "arity" ) != null )
                        {
                            Instance.ParameterArity arity =
                                    Instance.ParameterArity.parseArity( xmlInstance.getAttributeValue( "arity" ) );
                            instance.setArity( arity );
                        }
                    }
                    Element xmlReferenceLinksRoot = xmlOONRoot.getChild( XMLTags.REFERENCE_LINKS.toString() );
                    if ( xmlReferenceLinksRoot != null )
                    {
                        List<Element> xmlReferenceLinks = getXMLChildren( xmlReferenceLinksRoot );
                        for ( Element xmlReferenceLink : xmlReferenceLinks )
                        {
                            String source = xmlReferenceLink.getAttributeValue( "source" );
                            String destination = xmlReferenceLink.getAttributeValue( "destination" );
                            String type = xmlReferenceLink.getAttributeValue( "type" );
                            ReferenceLink link = null;
                            if ( type.equalsIgnoreCase( "instance" ) )
                            {
                                String paramName = xmlReferenceLink.getAttributeValue( "parameter" );
                                link =
                                        new InstanceReferenceLink( ooNet.getInstances().get( source ),
                                                ooNet.getInstances().get( destination ),
                                                ooNet.getInstances().get( destination ).getSubInstances().get( paramName ) );
                            }
                            else if ( type.equalsIgnoreCase( "node" ) )
                            {
                                try
                                {
                                    link =
                                            new NodeReferenceLink( ooNet.getNode( source ), ooNet.getNode( destination ) );
                                }
                                catch ( NodeNotFoundException e )
                                {
                                    e.printStackTrace();
                                }
                            }
                            ooNet.addReferenceLink( link );
                        }
                    }
                }
            }
        }
    }

    // OOPN end
    /**
     * @param root
     * @param probNet
     * @throws PGMXParserException
     */
    protected void getPolicies( Element root, ProbNet probNet )
            throws PGMXParserException
    {
        Element policiesRoot = root.getChild( XMLTags.POLICIES.toString() );
        if ( policiesRoot != null )
        {
            List<Element> xmlPotentialPolicies = getXMLChildren( policiesRoot );
            for ( Element xmlPotential : xmlPotentialPolicies )
            {
                Potential potential = getPotential( xmlPotential, probNet );
                probNet.addPotential( potential );
            }
        }
    }

    protected Element getXMLVariables( Element rootNetwork )
    {
        return rootNetwork.getChild( XMLTags.VARIABLES.toString() );
    }

    protected Element getXMLPotentialVariables( Element xmlPotential )
    {
        return xmlPotential.getChild( XMLTags.VARIABLES.toString() );
    }

    protected List<Element> getXMLChildren( Element xmlRootVariables )
    {
        return xmlRootVariables.getChildren();
    }
}