/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.io;

import java.util.List;

import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;

/**
 * Contains a ProbNet and a list of Evidence Cases
 */
public class ProbNetInfo {
	private ProbNet probNet;
	private List<EvidenceCase> evidence;

	/**
	 * Constructor for ProbNetInfo.
	 *
	 * @param probNet Network
	 * @param evidence Evidence
	 */
	public ProbNetInfo(ProbNet probNet, List<EvidenceCase> evidence) {
		this.probNet = probNet;
		this.evidence = evidence;
	}

	public int hashCode() {
		int hashProbNet = probNet != null ? probNet.hashCode() : 0;
		int hashEvidence = evidence != null ? evidence.hashCode() : 0;

		return (hashProbNet + hashEvidence) * hashEvidence + hashProbNet;
	}

	public boolean equals(Object other) {
		boolean result = false;
		if (other instanceof ProbNetInfo) {
			ProbNetInfo otherProbNetInfo = (ProbNetInfo) other;
			result =(this.probNet == otherProbNetInfo.probNet
							||
							(this.probNet != null
									&&
									otherProbNetInfo.probNet != null
									&&
									this.probNet.equals(otherProbNetInfo.probNet)
							)
					)
					&&
					(this.evidence == otherProbNetInfo.evidence
							||
							(this.evidence != null
									&&
									otherProbNetInfo.evidence != null
									&&
									this.evidence.equals(otherProbNetInfo.evidence))
					);
		}

		return result;
	}

	public String toString() {
		return "(" + probNet + ", " + evidence + ")";
	}

	/**
	 * Returns the probNet.
	 *
	 * @return the probNet.
	 */
	public ProbNet getProbNet() {
		return probNet;
	}

	/**
	 * Returns the evidence.
	 *
	 * @return the evidence.
	 */
	public List<EvidenceCase> getEvidence() {
		return evidence;
	}

}