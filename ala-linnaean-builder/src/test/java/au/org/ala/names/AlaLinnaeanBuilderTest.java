package au.org.ala.names;

import au.org.ala.bayesian.*;
import au.org.ala.names.builder.IndexBuilder;
import au.org.ala.names.builder.IndexBuilderConfiguration;
import au.org.ala.names.builder.LoadStore;
import au.org.ala.names.builder.Source;
import au.org.ala.names.lucene.LuceneClassifier;
import au.org.ala.names.lucene.LuceneClassifierSearcher;
import au.org.ala.util.TestUtils;
import au.org.ala.vocab.ALATerm;
import au.org.ala.vocab.TaxonomicStatus;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.nameparser.api.Rank;
import org.junit.*;

import javax.swing.tree.AbstractLayoutCache;
import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class AlaLinnaeanBuilderTest extends TestUtils {
    private static File work;
    private static File output;
    private static IndexBuilder builder;

    private AlaLinnaeanFactory factory;
    private LuceneClassifierSearcher searcher;
    private ClassificationMatcher<AlaLinnaeanClassification, AlaLinnaeanInferencer, AlaLinnaeanFactory> matcher;

    @BeforeClass
    public static void setUpClass() throws Exception {
        work = makeTmpDir("work");
        output = makeTmpDir("output");
        IndexBuilderConfiguration config = new IndexBuilderConfiguration();
        config.setBuilderClass(AlaLinnaeanBuilder.class);
        config.setNetwork(AlaLinnaeanBuilder.class.getResource("/ala-linnaean.json"));
        config.setWork(work);
        config.setFactoryClass(AlaLinnaeanFactory.class);
        builder = new IndexBuilder(config);
        Source source = Source.create(AlaLinnaeanBuilderTest.class.getResource("/sample-1.zip"), AlaLinnaeanFactory.instance(), AlaLinnaeanFactory.instance().getObservables(), config.getTypes());
        builder.load(source);
        builder.build();
        builder.buildIndex(output);
   }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (builder != null)
            builder.close();
        if (output != null)
            deleteAll(output);
        if (work != null)
            deleteAll(work);
    }

    @Before
    public void setUp() throws Exception {
        this.factory = AlaLinnaeanFactory.instance();
        this.searcher = new LuceneClassifierSearcher(this.output);
        this.matcher = this.factory.createMatcher(this.searcher);
    }

    @After
    public void tearDown() throws Exception {
        if (this.searcher != null)
            this.searcher.close();
    }

    @Test
    public void testLoadBuild1() throws Exception {
        LoadStore store = this.builder.getParameterisedStore();
        Classifier doc = store.get(DwcTerm.Taxon, AlaLinnaeanFactory.taxonId, "urn:lsid:indexfungorum.org:names:90156");
        assertNotNull(doc);
        assertEquals("FFFFFFT", doc.getSignature());
        assertEquals("Fungi", doc.get(AlaLinnaeanFactory.scientificName));
        doc = store.get(DwcTerm.Taxon, AlaLinnaeanFactory.taxonId, "https://id.biodiversity.org.au/node/apni/2904909");
        assertEquals("TTTTTTT", doc.getSignature());
        assertEquals("Canarium acutifolium var. acutifolium", doc.get(AlaLinnaeanFactory.scientificName));
        assertEquals("CANARIM ACITIFALIM ACITIFALA", doc.get(AlaLinnaeanFactory.soundexScientificName));
        assertEquals("Canarium", doc.get(AlaLinnaeanFactory.genus));
        assertEquals("https://id.biodiversity.org.au/node/apni/2918714", doc.get(AlaLinnaeanFactory.genusId));
        assertEquals(TaxonomicStatus.accepted, doc.get(AlaLinnaeanFactory.taxonomicStatus));
        AlaLinnaeanParameters_TTTTTTT params = new AlaLinnaeanParameters_TTTTTTT();
        doc.loadParameters(params);
        AlaLinnaeanInferencer_TTTTTTT inference = new AlaLinnaeanInferencer_TTTTTTT();
        AlaLinnaeanInferencer.Evidence evidence = new AlaLinnaeanInferencer.Evidence();
        evidence.e$scientificName = true;
        Inference prob = inference.probability(evidence, params);
        assertEquals(1.0, prob.getPosterior(), 0.00001);
        evidence.e$genus = false;
        prob = inference.probability(evidence, params);
        assertEquals(0.0, prob.getPosterior(), 0.00001);
        evidence.e$soundexGenus = true;
        prob = inference.probability(evidence, params);
        assertEquals(0.0, prob.getPosterior(), 0.00001); // Zero because genus is still false see modifiers
        evidence.e$soundexGenus = false;
        prob = inference.probability(evidence, params);
        assertEquals(0.0, prob.getPosterior(), 0.00001);
        evidence.e$scientificName = false;
        prob = inference.probability(evidence, params);
        assertEquals(0.0, prob.getPosterior(), 0.00001);
        evidence.e$scientificName = true;
        evidence.e$genus = true;
        evidence.e$soundexGenus = true;
        prob = inference.probability(evidence, params);
        assertEquals(0.00781, prob.getEvidence(), 0.00001);
        assertEquals(1.0, prob.getPosterior(), 0.00001);
    }


    @Test
    public void testLoadBuild2() throws Exception {
        LoadStore store = this.builder.getParameterisedStore();
        Classifier doc = store.get(DwcTerm.Taxon, AlaLinnaeanFactory.taxonId, "9c237030-9af9-41d2-bc3c-3a18adcd43ac");
        assertEquals("FFFFFFT", doc.getSignature());
        assertEquals("Coniosporium sacchari", doc.get(AlaLinnaeanFactory.scientificName));
        assertEquals("CANIASPARIM SACARI", doc.get(AlaLinnaeanFactory.soundexScientificName));
        assertNull(doc.get(AlaLinnaeanFactory.genus));
        assertEquals(TaxonomicStatus.synonym, doc.get(AlaLinnaeanFactory.taxonomicStatus));
        assertNull(doc.get(AlaLinnaeanFactory.genusId));
        assertNull(doc.get(AlaLinnaeanFactory.familyId));
        assertNull(doc.get(AlaLinnaeanFactory.orderId));
        assertNull(doc.get(AlaLinnaeanFactory.classId));
        assertNull(doc.get(AlaLinnaeanFactory.phylumId));
        assertEquals("Fungi", doc.get(AlaLinnaeanFactory.kingdom));
        assertEquals("urn:lsid:indexfungorum.org:names:90156", doc.get(AlaLinnaeanFactory.kingdomId));
        assertEquals(TaxonomicStatus.synonym, doc.get(AlaLinnaeanFactory.taxonomicStatus));
        assertEquals("b465f067-a2c1-4c6c-88cc-e394c23e4f87", doc.get(AlaLinnaeanFactory.acceptedNameUsageId));
        AlaLinnaeanParameters_FFFFFFT params = new AlaLinnaeanParameters_FFFFFFT();
        doc.loadParameters(params);
        assertEquals(1.0, params.inf_kingdom_t$t_tt, 0.00001);
        AlaLinnaeanInferencer_FFFFFFT inference = new AlaLinnaeanInferencer_FFFFFFT();
        AlaLinnaeanInferencer.Evidence evidence = new AlaLinnaeanInferencer.Evidence();
        evidence.e$scientificName = true;
        Inference prob = inference.probability(evidence, params);
        assertEquals(1.0, prob.getPosterior(), 0.00001);
        evidence.e$kingdom = false;
        evidence.e$soundexKingdom = false;
        prob = inference.probability(evidence, params);
        assertEquals(0.0, prob.getEvidence(), 0.00001);
        assertEquals(0.0, prob.getConditional(), 0.00001);
        assertEquals(0.0, prob.getPosterior(), 0.00001);
        evidence.e$kingdom = true;
        evidence.e$soundexKingdom = true;
        prob = inference.probability(evidence, params);
        assertEquals(0.00781, prob.getEvidence(), 0.00001);
        assertEquals(1.0, prob.getConditional(), 0.00001);
        assertEquals(1.0, prob.getPosterior(), 0.00001);
    }


    @Test
    public void testLoadBuild3() throws Exception {
        LoadStore store = this.builder.getParameterisedStore();
        Classifier doc = store.get(DwcTerm.Taxon, AlaLinnaeanFactory.taxonId, "x-homonym-1");
        assertEquals("FTTTTTT", doc.getSignature());
        assertEquals("Homonymia", doc.get(AlaLinnaeanFactory.scientificName));
        assertEquals("HAMANIMA", doc.get(AlaLinnaeanFactory.soundexScientificName));
        assertEquals(TaxonomicStatus.accepted, doc.get(AlaLinnaeanFactory.taxonomicStatus));
        assertEquals("ANIMALIA", doc.get(AlaLinnaeanFactory.kingdom));
        AlaLinnaeanParameters_FTTTTTT params = new AlaLinnaeanParameters_FTTTTTT();
        doc.loadParameters(params);
        assertEquals(1.0, params.inf_kingdom_t$t_t, 0.00001);
        AlaLinnaeanInferencer_FTTTTTT inference = new AlaLinnaeanInferencer_FTTTTTT();
        AlaLinnaeanInferencer.Evidence evidence = new AlaLinnaeanInferencer.Evidence();
        evidence.e$scientificName = true;
        evidence.e$soundexScientificName = true;
        Inference prob = inference.probability(evidence, params);
        assertEquals(0.5, prob.getPosterior(), 0.00001);
        evidence.e$kingdom = false;
        evidence.e$soundexKingdom = false;
        prob = inference.probability(evidence, params);
        assertEquals(0.00452, prob.getEvidence(), 0.00001);
        assertEquals(0.0, prob.getConditional(), 0.00001);
        assertEquals(0.0, prob.getPosterior(), 0.00001);
    }

    @Test
    public void testSearch1() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Canarium acutifolium var. acutifolium";
        List<LuceneClassifier> classifiers = this.searcher.search(classification);
        assertNotNull(classifiers);
        assertEquals(20, classifiers.size());
        assertEquals("https://id.biodiversity.org.au/node/apni/2904909", classifiers.get(0).get(AlaLinnaeanFactory.taxonId));
    }

    @Test
    public void testSearch2() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Canarium acutifolium acutifolium";
        List<LuceneClassifier> classifiers = this.searcher.search(classification);
        assertNotNull(classifiers);
        assertEquals(20, classifiers.size());
        assertEquals("https://id.biodiversity.org.au/node/apni/2904909", classifiers.get(0).get(AlaLinnaeanFactory.taxonId));
    }

    @Test
    public void testMatch1() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Canarium acutifolium var. acutifolium";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2904909", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
        classification.scientificName = "Canarium acutifolium acutifolium";
        match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2904909", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }

    @Test
    public void testMatch2() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.genus = "Canarium";
        classification.specificEpithet = "acutifolium";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2901022", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }

    @Test
    public void testMatch3() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.genus = "Canarium";
        classification.specificEpithet = "acutifolium";
        classification.taxonRank = Rank.SPECIES;
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2901022", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }


    @Test
    public void testMatch4() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Plantae";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51337710", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }


    @Test
    public void testMatch5() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Canarium acutifolium";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2901022", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }

    // Higher order
    @Test
    public void testMatch6() throws Exception {
        AlaLinnaeanClassification template = new AlaLinnaeanClassification();
        template.scientificName = "Canarium notanameum";
        template.family = "Burseraceae";
        Match<AlaLinnaeanClassification> result = matcher.findMatch(template);
        assertTrue(result.isValid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2918714", result.getMatch().taxonId);
        assertEquals(Rank.GENUS, result.getMatch().taxonRank);
        assertTrue(result.getIssues().contains(AlaLinnaeanFactory.HIGHER_ORDER_MATCH));
    }

    // Higher order
    @Test
    public void testMatch7() throws Exception {
        AlaLinnaeanClassification template = new AlaLinnaeanClassification();
        template.scientificName = "Othernama notanamea";
        template.family = "Burseraceae";
        Match<AlaLinnaeanClassification> result = matcher.findMatch(template);
        assertTrue(result.isValid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2900189", result.getMatch().taxonId);
        assertEquals(Rank.FAMILY, result.getMatch().taxonRank);
        assertTrue(result.getIssues().contains(AlaLinnaeanFactory.HIGHER_ORDER_MATCH));
    }

    // Genus with odd sub-taxa
    @Test
    public void testMatch8() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Canarium";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2918714", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }

    // Phrase name
    @Test
    public void testMatch9() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Sida sp. Walhallow Station (C.Edgood 28/Oct/94)";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2898305", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }


    // Cultivar name
    @Test
    public void testMatch10() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Canarium sp. 'Testing'";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("x-cultivar-1", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }


    // Cultivar name
    @Test
    public void testMatch11() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Canarium 'Testing'";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("x-cultivar-1", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }

    // Cultivar name
    @Test
    public void testMatch12() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Sida sp. nov. 'Testing'";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("x-cultivar-2", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }

    // Cultivar name
    @Test
    public void testMatch13() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Sida sp 'Testing'";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("x-cultivar-2", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
    }

    // Cultivar name
    @Test
    public void testMatch14() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Sida phaeotricha 'Testing'";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("x-cultivar-3", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
        assertEquals(Issues.of(AlaLinnaeanFactory.REMOVED_RANK), match.getIssues()); // Source rank is species, inferred rank is cultivar
    }


    // Embedded author
    @Test
    public void testMatch15() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Sordariomycetidae O.E. Erikss. & Winka";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("urn:lsid:indexfungorum.org:names:90351", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
        assertEquals(Issues.of(AlaLinnaeanFactory.CANONICAL_NAME), match.getIssues());
    }

    // Embedded author
    @Test
    public void testMatch16() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Sordariomycetidae Erikss. and Winka";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("urn:lsid:indexfungorum.org:names:90351", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
        assertEquals(Issues.of(AlaLinnaeanFactory.CANONICAL_NAME), match.getIssues());
    }

    // Separated author
    @Test
    public void testMatch17() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Canarium longiflorum";
        classification.scientificNameAuthorship = "Zipp. ex Miq.";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/instance/apni/832902", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
        assertEquals(Issues.of(), match.getIssues());
    }

    // Separated author
    @Test
    public void testMatch18() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Canarium longiflorum";
        classification.scientificNameAuthorship = "Zipp.";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/instance/apni/832902", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
        assertEquals(Issues.of(AlaLinnaeanFactory.REMOVED_AUTHORSHIP), match.getIssues());
    }

    // Separated misspelled author
    @Test
    public void testMatch19() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Canarium longiflorum";
        classification.scientificNameAuthorship = "A.N.Other";
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("https://id.biodiversity.org.au/instance/apni/832902", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
        assertEquals(Issues.of(AlaLinnaeanFactory.REMOVED_AUTHORSHIP), match.getIssues());
    }


    // Rank within fuzzy limits
    @Test
    public void testMatch20() throws Exception {
        AlaLinnaeanClassification classification = new AlaLinnaeanClassification();
        classification.scientificName = "Melanogaster";
        classification.taxonRank = Rank.SERIES;
        Match<AlaLinnaeanClassification> match = matcher.findMatch(classification);
        assertTrue(match.isValid());
        assertEquals("urn:lsid:indexfungorum.org:names:19214", match.getMatch().taxonId);
        assertEquals(0.00781, match.getProbability().getEvidence(), 0.00001);
        assertEquals(1.0, match.getProbability().getPosterior(), 0.00001);
        assertEquals(Issues.of(), match.getIssues());
    }

}
