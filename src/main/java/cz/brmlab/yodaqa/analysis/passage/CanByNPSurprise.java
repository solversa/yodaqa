package cz.brmlab.yodaqa.analysis.passage;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.NP;

/**
 * Create CandidateAnswers for all NP constituents (noun phrases) that do not
 * contain supplied clues.
 *
 * This is pretty naive but should generate some useful answers. */

@SofaCapability(
	inputSofas = { "Question", "Passage" },
	outputSofas = { "Passage" }
)

public class CanByNPSurprise extends CandidateGenerator {
	public CanByNPSurprise() {
		logger = LoggerFactory.getLogger(CanByNPSurprise.class);
	}

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, passageView;
		try {
			questionView = jcas.getView("Question");
			passageView = jcas.getView("Passage");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		ResultInfo ri = JCasUtil.selectSingle(passageView, ResultInfo.class);

		for (NP np : JCasUtil.select(passageView, NP.class)) {
			String text = np.getCoveredText();

			/* TODO: This can be optimized a lot. */
			boolean matches = false;
			for (Clue clue : JCasUtil.select(questionView, Clue.class)) {
				if (text.endsWith(clue.getLabel())) {
					matches = true;
					break;
				}
			}

			Passage p = JCasUtil.selectCovering(Passage.class, np).get(0);
			AnswerFV fv = new AnswerFV(ri.getAnsfeatures());
			fv.merge(new AnswerFV(p.getAnsfeatures()));
			fv.setFeature(AF.OriginPsgNP, 1.0);
			if (!matches) {
				/* Surprise! */
				fv.setFeature(AF.OriginPsgSurprise, 1.0);
			}

			addCandidateAnswer(passageView, p, np, fv);
		}
	}
}
