package au.org.ala.names.generated;

import au.org.ala.bayesian.Classifier;
import au.org.ala.bayesian.InferenceException;
import au.org.ala.bayesian.ParameterAnalyser;
import au.org.ala.bayesian.Parameters;
import au.org.ala.bayesian.StoreException;
import au.org.ala.names.builder.Builder;

import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;

import au.org.ala.bayesian.analysis.StringAnalysis;
import au.org.ala.bayesian.analysis.DoubleAnalysis;

public class SimpleLinnaeanBuilder_TT implements Builder {
  public final static String SIGNATURE = "TT";

  public  SimpleLinnaeanBuilder_TT() {
  }

  @Override
  public String getSignature() {
    return SIGNATURE;
  }

  @Override
  public void generate(Classifier classifier) throws InferenceException, StoreException {
    throw new UnsupportedOperationException("Sub-builders do not support this operation");
  }

  @Override
  public void infer(Classifier classifier) throws InferenceException, StoreException {
    throw new UnsupportedOperationException("Sub-builders do not support this operation");
  }

  @Override
  public void expand(Classifier classifier, Deque<Classifier> parents) throws InferenceException, StoreException {
    throw new UnsupportedOperationException("Sub-builders do not support this operation");
  }

  @Override
  public String buildSignature(Classifier classifier) {
    return SIGNATURE;
  }

  @Override
  public Parameters calculate(ParameterAnalyser analyser, Classifier classifier) throws InferenceException, StoreException {
    SimpleLinnaeanParameters_TT parameters = new SimpleLinnaeanParameters_TT();
    parameters.prior_taxonId_t = analyser.computePrior(analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier));
    parameters.inf_taxonRank_t$t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.taxonRank, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier));
    parameters.inf_taxonRank_t$f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.taxonRank, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier));
    parameters.inf_specificEpithet_t$t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.specificEpithet, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier));
    parameters.inf_specificEpithet_t$f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.specificEpithet, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier));
    parameters.inf_scientificNameAuthorship_t$t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.scientificNameAuthorship, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier));
    parameters.inf_scientificNameAuthorship_t$f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.scientificNameAuthorship, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier));
    parameters.inf_scientificName_t$t_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.scientificName, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.specificEpithet, classifier));
    parameters.inf_scientificName_t$t_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.scientificName, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.specificEpithet, classifier));
    parameters.inf_scientificName_t$f_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.scientificName, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.specificEpithet, classifier));
    parameters.inf_scientificName_t$f_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.scientificName, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.specificEpithet, classifier));
    parameters.inf_soundexScientificName_t$t_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.soundexScientificName, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.scientificName, classifier));
    parameters.inf_soundexScientificName_t$t_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.soundexScientificName, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.scientificName, classifier));
    parameters.inf_soundexScientificName_t$f_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.soundexScientificName, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.scientificName, classifier));
    parameters.inf_soundexScientificName_t$f_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.soundexScientificName, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.scientificName, classifier));
    parameters.inf_genus_t$t_tt = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.genus, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.scientificName, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.soundexScientificName, classifier));
    parameters.inf_genus_t$t_tf = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.genus, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.scientificName, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.soundexScientificName, classifier));
    parameters.inf_genus_t$t_ft = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.genus, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.scientificName, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.soundexScientificName, classifier));
    parameters.inf_genus_t$t_ff = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.genus, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.scientificName, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.soundexScientificName, classifier));
    parameters.inf_genus_t$f_tt = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.genus, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.scientificName, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.soundexScientificName, classifier));
    parameters.inf_genus_t$f_tf = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.genus, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.scientificName, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.soundexScientificName, classifier));
    parameters.inf_genus_t$f_ft = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.genus, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.scientificName, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.soundexScientificName, classifier));
    parameters.inf_genus_t$f_ff = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.genus, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.scientificName, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.soundexScientificName, classifier));
    parameters.inf_family_t$t_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.family, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.genus, classifier));
    parameters.inf_family_t$t_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.family, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.genus, classifier));
    parameters.inf_family_t$f_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.family, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.genus, classifier));
    parameters.inf_family_t$f_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.family, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.genus, classifier));
    parameters.inf_order_t$t_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.order, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.family, classifier));
    parameters.inf_order_t$t_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.order, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.family, classifier));
    parameters.inf_order_t$f_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.order, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.family, classifier));
    parameters.inf_order_t$f_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.order, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.family, classifier));
    parameters.inf_class__t$t_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.class_, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.order, classifier));
    parameters.inf_class__t$t_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.class_, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.order, classifier));
    parameters.inf_class__t$f_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.class_, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.order, classifier));
    parameters.inf_class__t$f_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.class_, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.order, classifier));
    parameters.inf_phylum_t$t_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.phylum, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.class_, classifier));
    parameters.inf_phylum_t$t_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.phylum, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.class_, classifier));
    parameters.inf_phylum_t$f_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.phylum, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.class_, classifier));
    parameters.inf_phylum_t$f_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.phylum, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.class_, classifier));
    parameters.inf_kingdom_t$t_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.kingdom, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.phylum, classifier));
    parameters.inf_kingdom_t$t_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.kingdom, classifier) , analyser.getObservation(true, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.phylum, classifier));
    parameters.inf_kingdom_t$f_t = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.kingdom, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(true, SimpleLinnaeanFactory.phylum, classifier));
    parameters.inf_kingdom_t$f_f = analyser.computeConditional(analyser.getObservation(true, SimpleLinnaeanFactory.kingdom, classifier) , analyser.getObservation(false, SimpleLinnaeanFactory.taxonId, classifier), analyser.getObservation(false, SimpleLinnaeanFactory.phylum, classifier));
    return parameters;
  }
}
