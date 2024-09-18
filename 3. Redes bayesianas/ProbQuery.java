package es.uniovi.ssii.rb;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.inference.likelihoodWeighting.LikelihoodWeighting;
import org.openmarkov.io.probmodel.reader.PGMXReader_0_2;

public class ProbQuery {

	private ProbNet probNet;
	private Long seed;
	private Random rnd;

	public ProbQuery(String fileName) throws Exception {
		String path = "src/main/resources/networks/" + fileName;
		PGMXReader_0_2 pgmxReader = new PGMXReader_0_2();
		probNet = pgmxReader.loadProbNet(fileName, new FileInputStream(path));
		seed = null;
		rnd = new Random();
	}

	public ProbNet getProbNet() {
		return probNet;
	}

	public void setProbNet(ProbNet probNet) {
		this.probNet = probNet;
	}

	public Long getSeed() {
		return seed;
	}

	public void setSeed(Long seed) {
		this.seed = seed;
		rnd.setSeed(seed);
	}

	public double DoInference(List<Variable> variablesOfInterest, EvidenceCase evidence) {
		Map<Variable, TablePotential> posteriorProbabilities = null;
		// TODO Add the code to perform the inference with the algorithm requested over
		// the variable of interest for the given evidence

		LikelihoodWeighting propagation = null;
		try {
			propagation = new LikelihoodWeighting(probNet);
		} catch (NotEvaluableNetworkException e) {
			e.printStackTrace();
		}
		propagation.setSampleSize(5000);
		propagation.setVariablesOfInterest(variablesOfInterest);

		propagation.setPostResolutionEvidence(evidence);

		System.out.println("LikelihoodWeighting");
		System.out.println("\tP(Has bronchitis=no | Has lung cancer=yes, Has tuberculosis=yes)");
		try {
			posteriorProbabilities = propagation.getPosteriorValues();

		} catch (IncompatibleEvidenceException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
		
		return getProbability(evidence, variablesOfInterest, posteriorProbabilities);
	}
	
	
	/**
	 * This method assumes that there is only one variable of interest, i.e. the
	 * list has only one element. It will extract the probability of that variable
	 * and return it
	 * 
	 * @param evidence
	 * @param variablesOfInterest
	 * @param posteriorProbabilities
	 */
	private static double getProbability(EvidenceCase evidence, List<Variable> variablesOfInterest,
			Map<Variable, TablePotential> posteriorProbabilities) {

		Variable variable = variablesOfInterest.get(0);
		TablePotential posteriorProbabilitiesPotential = posteriorProbabilities.get(variable);
		return posteriorProbabilitiesPotential.values[0];

	}

	public static void main(String[] args) throws Exception {

		ProbQuery obj = new ProbQuery("asia.pgmx");

		EvidenceCase evidence = new EvidenceCase();
		// TODO Add the code to define the evidence requested
		evidence.addFinding(obj.probNet, "Has lung cancer", "yes");
		evidence.addFinding(obj.probNet, "Has tuberculosis", "yes");	
		
		List<Variable> variablesOfInterest = new ArrayList<Variable>();
		// TODO Add the code to set the variable of interest requested
		variablesOfInterest = Collections.singletonList(obj.probNet.getVariable("Has bronchitis"));

		System.out.format("%f", obj.DoInference(variablesOfInterest, evidence));

	}

}
