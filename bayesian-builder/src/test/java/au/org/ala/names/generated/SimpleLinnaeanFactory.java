package au.org.ala.names.generated;

import au.org.ala.bayesian.ClassificationMatcher;
import au.org.ala.bayesian.ClassifierSearcher;
import au.org.ala.bayesian.Analyser;
import au.org.ala.bayesian.NetworkFactory;
import au.org.ala.bayesian.Normaliser;
import au.org.ala.bayesian.Observable;
import au.org.ala.bayesian.Observable.Multiplicity;
import static au.org.ala.bayesian.ExternalContext.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;

import au.org.ala.bayesian.analysis.StringAnalysis;
import au.org.ala.bayesian.Analyser;
import au.org.ala.bayesian.analysis.DoubleAnalysis;
import au.org.ala.bayesian.ClassificationMatcher;

public class SimpleLinnaeanFactory implements NetworkFactory<SimpleLinnaeanClassification, SimpleLinnaeanInferencer, SimpleLinnaeanFactory> {
    private static SimpleLinnaeanFactory instance = null;

  public static final Normaliser lowerCaseNormaliser = new au.org.ala.util.BasicNormaliser("lower_case_normaliser", true, true, true, true, true);
  public static final Normaliser simpleNormaliser = new au.org.ala.util.BasicNormaliser("simple_normaliser", true, true, true, true, false);

  public static final Observable acceptedNameUsageId = new Observable(
      "acceptedNameUsageID",
      URI.create("http://rs.tdwg.org/dwc/terms/acceptedNameUsageID"),
      String.class,
      Observable.Style.CANONICAL,
      null,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable altScientificName = new Observable(
      "altScientificName",
      URI.create("http://id.ala.org.au/terms/1.0/altScientificName"),
      String.class,
      Observable.Style.CANONICAL,
      null,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable class_ = new Observable(
      "class",
      URI.create("http://rs.tdwg.org/dwc/terms/class"),
      String.class,
      Observable.Style.CANONICAL,
      simpleNormaliser,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable family = new Observable(
      "family",
      URI.create("http://rs.tdwg.org/dwc/terms/family"),
      String.class,
      Observable.Style.CANONICAL,
      simpleNormaliser,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable genus = new Observable(
      "genus",
      URI.create("http://rs.tdwg.org/dwc/terms/genus"),
      String.class,
      Observable.Style.CANONICAL,
      simpleNormaliser,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable kingdom = new Observable(
      "kingdom",
      URI.create("http://rs.tdwg.org/dwc/terms/kingdom"),
      String.class,
      Observable.Style.CANONICAL,
      simpleNormaliser,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable order = new Observable(
      "order",
      URI.create("http://rs.tdwg.org/dwc/terms/order"),
      String.class,
      Observable.Style.CANONICAL,
      simpleNormaliser,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable parentNameUsageId = new Observable(
      "parentNameUsageID",
      URI.create("http://rs.tdwg.org/dwc/terms/parentNameUsageID"),
      String.class,
      Observable.Style.CANONICAL,
      null,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable phylum = new Observable(
      "phylum",
      URI.create("http://rs.tdwg.org/dwc/terms/phylum"),
      String.class,
      Observable.Style.CANONICAL,
      simpleNormaliser,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable scientificName = new Observable(
      "scientificName",
      URI.create("http://rs.tdwg.org/dwc/terms/scientificName"),
      String.class,
      Observable.Style.CANONICAL,
      simpleNormaliser,
      new StringAnalysis(),
      Multiplicity.MANY
    );
  public static final Observable scientificNameAuthorship = new Observable(
      "scientificNameAuthorship",
      URI.create("http://rs.tdwg.org/dwc/terms/scientificNameAuthorship"),
      String.class,
      Observable.Style.CANONICAL,
      simpleNormaliser,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable soundexScientificName = new Observable(
      "soundexScientificName",
      URI.create("http://id.ala.org.au/terms/1.0/soundexScientificName"),
      String.class,
      Observable.Style.CANONICAL,
      null,
      new StringAnalysis(),
      Multiplicity.MANY
    );
  public static final Observable specificEpithet = new Observable(
      "specificEpithet",
      URI.create("http://rs.tdwg.org/dwc/terms/specificEpithet"),
      String.class,
      Observable.Style.CANONICAL,
      simpleNormaliser,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable taxonId = new Observable(
      "taxonID",
      URI.create("http://rs.tdwg.org/dwc/terms/taxonID"),
      String.class,
      Observable.Style.CANONICAL,
      null,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable taxonRank = new Observable(
      "taxonRank",
      URI.create("http://rs.tdwg.org/dwc/terms/taxonRank"),
      String.class,
      Observable.Style.CANONICAL,
      lowerCaseNormaliser,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable taxonomicStatus = new Observable(
      "taxonomicStatus",
      URI.create("http://rs.tdwg.org/dwc/terms/taxonomicStatus"),
      String.class,
      Observable.Style.CANONICAL,
      simpleNormaliser,
      new StringAnalysis(),
      Multiplicity.OPTIONAL
    );
  public static final Observable weight = new Observable(
      "weight",
      URI.create("http://id.ala.org.au/bayesian/1.0/weight"),
      Double.class,
      Observable.Style.CANONICAL,
      null,
      new DoubleAnalysis(),
      Multiplicity.OPTIONAL
    );

  public static List<Observable> OBSERVABLES = Collections.unmodifiableList(Arrays.asList(
    acceptedNameUsageId,
    altScientificName,
    class_,
    family,
    genus,
    kingdom,
    order,
    parentNameUsageId,
    phylum,
    scientificName,
    scientificNameAuthorship,
    soundexScientificName,
    specificEpithet,
    taxonId,
    taxonRank,
    taxonomicStatus,
    weight
  ));

  public static final TermFactory TERM_FACTORY = TermFactory.instance();

  public static final List<Class> VOCABULARIES = Collections.unmodifiableList(Arrays.asList(
    au.org.ala.vocab.BayesianTerm.class
  ));

  public static final Term CONCEPT = TERM_FACTORY.findTerm("http://rs.tdwg.org/dwc/terms/Taxon");

  /** Issue removed_rank 
      <p>Ignored supplied rank to find a match.</p>
  */
  public static final Term REMOVED_RANK = TERM_FACTORY.findTerm("http://id.ala.org.au/issues/1.0/removedRank");
  /** Issue removed_phylum 
      <p>Ignored supplied phylum to find a match.</p>
  */
  public static final Term REMOVED_PHYLUM = TERM_FACTORY.findTerm("http://id.ala.org.au/issues/1.0/removedPhylum");
  /** Issue removed_class 
      <p>Ignored supplied class to find a match.</p>
  */
  public static final Term REMOVED_CLASS = TERM_FACTORY.findTerm("http://id.ala.org.au/issues/1.0/removedClass");
  /** Issue removed_order 
      <p>Ignored supplied order to find a match.</p>
  */
  public static final Term REMOVED_ORDER = TERM_FACTORY.findTerm("http://id.ala.org.au/issues/1.0/removedOrder");

  static {
    acceptedNameUsageId.setExternal(LUCENE, "dwc_acceptedNameUsageID");
    altScientificName.setExternal(LUCENE, "altScientificName");
    class_.setExternal(LUCENE, "dwc_class");
    family.setExternal(LUCENE, "dwc_family");
    genus.setExternal(LUCENE, "dwc_genus");
    kingdom.setExternal(LUCENE, "dwc_kingdom");
    order.setExternal(LUCENE, "dwc_order");
    parentNameUsageId.setExternal(LUCENE, "dwc_parentNameUsageID");
    phylum.setExternal(LUCENE, "dwc_phylum");
    scientificName.setExternal(LUCENE, "dwc_scientificName");
    scientificName.setExternal(LUCENE_VARIANT, "dwc_scientificName_variant");
    scientificNameAuthorship.setExternal(LUCENE, "dwc_scientificNameAuthorship");
    soundexScientificName.setExternal(LUCENE, "soundexScientificName");
    soundexScientificName.setExternal(LUCENE_VARIANT, "soundexScientificName_variant");
    specificEpithet.setExternal(LUCENE, "dwc_specificEpithet");
    taxonId.setExternal(LUCENE, "dwc_taxonID");
    taxonRank.setExternal(LUCENE, "dwc_taxonRank");
    taxonomicStatus.setExternal(LUCENE, "dwc_taxonomicStatus");
    weight.setExternal(LUCENE, "bayesian_weight");
  }

  @Override
  public List<Observable> getObservables() {
      return OBSERVABLES;
  }

  @Override
  public Optional<Observable> getIdentifier() {
    return Optional.of(taxonId);
  }

  @Override
  public Optional<Observable> getName() {
    return Optional.of(scientificName);
  }

  @Override
  public Optional<Observable> getParent() {
    return Optional.of(parentNameUsageId);
  }

  @Override
  public Optional<Observable> getAccepted() {
    return Optional.of(acceptedNameUsageId);
  }

  @Override
  public SimpleLinnaeanClassification createClassification() {
      return new SimpleLinnaeanClassification();
  }

  @Override
  public SimpleLinnaeanInferencer createInferencer() {
      return new SimpleLinnaeanInferencer();
  }

  @Override
  public Analyser<SimpleLinnaeanClassification> createAnalyser() {
        return new au.org.ala.bayesian.NullAnalyser<>();
  }

  @Override
  public ClassificationMatcher<SimpleLinnaeanClassification, SimpleLinnaeanInferencer, SimpleLinnaeanFactory> createMatcher(ClassifierSearcher searcher){
        return new ClassificationMatcher<>(this, searcher);
  }

  public static SimpleLinnaeanFactory instance() {
      if (instance == null) {
          synchronized (SimpleLinnaeanFactory.class) {
              if (instance == null) {
                  instance = new SimpleLinnaeanFactory();
              }
          }
      }
      return instance;
  }
}
