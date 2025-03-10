package au.org.ala.names.builder;

import au.org.ala.bayesian.Network;
import au.org.ala.bayesian.Observable;
import au.org.ala.names.generated.SimpleLinnaeanFactory;
import au.org.ala.names.lucene.LuceneClassifier;
import au.org.ala.util.TestUtils;
import org.gbif.dwc.terms.DwcTerm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test cases for {@link CSVSource}
 */
public class CSVSourceTest {
    private Network network;
    private SimpleLinnaeanFactory factory;
    private Annotator annotator;
    private TestLoadStore store;

    @Before
    public void setUp() throws Exception {
        this.network = new Network();
        this.factory = SimpleLinnaeanFactory.instance();
        this.annotator = new TestAnnotator();
        this.store = new TestLoadStore(this.annotator);
    }

    @After
    public void tearDown() throws Exception {
        this.store.close();
    }

    @Test
    public void testLoad1() throws Exception {
        Reader reader = TestUtils.getResourceReader(this.getClass(), "source-1.csv");
        CSVSource source = new CSVSource(DwcTerm.Taxon, reader, this.factory, this.network.getObservables());
        source.load(this.store, null);

        assertEquals(11, this.store.getStore().size());
        LuceneClassifier value = this.store.get(DwcTerm.Taxon, SimpleLinnaeanFactory.taxonId, "S-1");
        assertNotNull(value);
        assertEquals("Artemia franciscana", value.get(SimpleLinnaeanFactory.scientificName));
        assertEquals("species", value.get(SimpleLinnaeanFactory.taxonRank));
        assertEquals("accepted", value.get(SimpleLinnaeanFactory.taxonomicStatus));
        source.close();
    }

    @Test
    public void testLoad2() throws Exception {
        URL url = this.getClass().getResource("source-1.csv");
        CSVSource source = new CSVSource(DwcTerm.Taxon, url, this.factory, this.network.getObservables());
        source.load(this.store, Arrays.asList(SimpleLinnaeanFactory.taxonId, SimpleLinnaeanFactory.scientificName));

        assertEquals(11, this.store.getStore().size());
        LuceneClassifier value = this.store.get(DwcTerm.Taxon, SimpleLinnaeanFactory.taxonId, "S-1");
        assertNotNull(value);
        assertEquals("Artemia franciscana", value.get(SimpleLinnaeanFactory.scientificName));
        assertNull(value.get(SimpleLinnaeanFactory.taxonRank));
        assertNull(value.get(SimpleLinnaeanFactory.taxonomicStatus));
        source.close();
    }
}
