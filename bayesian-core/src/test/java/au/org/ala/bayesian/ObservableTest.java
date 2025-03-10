package au.org.ala.bayesian;

import au.org.ala.util.JsonUtils;
import au.org.ala.util.TestUtils;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.junit.Test;

import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ObservableTest {
    private static URI URI1 = URI.create("http://id.ala.org.au/terms/test_1");

    @Test
    public void testToJson1() throws Exception {
        Observable observable = new Observable("test_1");
        ObjectMapper mapper = JsonUtils.createMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, observable);
        TestUtils.compareNoSpaces(TestUtils.getResource(this.getClass(), "observable-1.json"), writer.toString());
    }

    @Test
    public void testToJson2() throws Exception {
        Observable observable = new Observable("test_1");
        observable.setDescription("A description");
        observable.setUri(URI1);
        ObjectMapper mapper = JsonUtils.createMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, observable);
        TestUtils.compareNoSpaces(TestUtils.getResource(this.getClass(), "observable-2.json"), writer.toString());
    }

    @Test
    public void testToJson3() throws Exception {
        Observable observable = new Observable("test_1");
        List<Observable> vertices = Arrays.asList(observable, observable);
        ObjectMapper mapper = JsonUtils.createMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, vertices);
        TestUtils.compareNoSpaces(TestUtils.getResource(this.getClass(), "observable-3.json"), writer.toString());
    }

    @Test
    public void testToJson4() throws Exception {
        Term property1 = TermFactory.instance().findPropertyTerm("property_1");
        Term property2 = TermFactory.instance().findPropertyTerm("property_2");
        Term property3 = TermFactory.instance().findPropertyTerm("property_3");
        Term property4 = TermFactory.instance().findPropertyTerm("property_4");
        Term property5 = TermFactory.instance().findPropertyTerm("property_5");
        Observable observable = new Observable("test_1");
        observable.setProperty(property1, true);
        observable.setProperty(property2, 1.0);
        observable.setProperty(property3, 11);
        observable.setProperty(property4, "Hello");
        observable.setProperty(property5, null);
        ObjectMapper mapper = JsonUtils.createMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, observable);
        TestUtils.compareNoSpaces(TestUtils.getResource(this.getClass(), "observable-4.json"), writer.toString());
    }

    @Test
    public void testToJson5() throws Exception {
        Observable observable = new Observable("test_5");
        observable.setType(Integer.class);
        ObjectMapper mapper = JsonUtils.createMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, observable);
        TestUtils.compareNoSpaces(TestUtils.getResource(this.getClass(), "observable-5.json"), writer.toString());
    }

    @Test
    public void testToJson6() throws Exception {
        Observable observable = new Observable("test_6");
        observable.setMultiplicity(Observable.Multiplicity.REQUIRED);
        ObjectMapper mapper = JsonUtils.createMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, observable);
        TestUtils.compareNoSpaces(TestUtils.getResource(this.getClass(), "observable-6.json"), writer.toString());
    }

    @Test
    public void testToJson7() throws Exception {
        Observable observable = new Observable("test_7");
        observable.setMultiplicity(Observable.Multiplicity.MANY);
        ObjectMapper mapper = JsonUtils.createMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, observable);
        TestUtils.compareNoSpaces(TestUtils.getResource(this.getClass(), "observable-7.json"), writer.toString());
    }

    @Test
    public void testFromJson1() throws Exception {
        ObjectMapper mapper = JsonUtils.createMapper();
        Observable observable = mapper.readValue(TestUtils.getResource(this.getClass(), "observable-1.json"), Observable.class);
        assertEquals("test_1", observable.getId());
        assertNull(observable.getDescription());
        assertEquals(Observable.Multiplicity.OPTIONAL, observable.getMultiplicity());
    }

    @Test
    public void testFromJson2() throws Exception {
        ObjectMapper mapper = JsonUtils.createMapper();
        Observable observable = mapper.readValue(TestUtils.getResource(this.getClass(), "observable-2.json"), Observable.class);
        assertEquals("test_1", observable.getId());
        assertEquals("A description", observable.getDescription());
        assertEquals(URI1, observable.getUri());
    }

    @Test
    public void testFromJson3() throws Exception {
        ObjectMapper mapper = JsonUtils.createMapper();
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Observable.class);
        List<Observable> vertices = mapper.readValue(TestUtils.getResource(this.getClass(), "observable-3.json"), type);
        assertEquals(2, vertices.size());
        assertEquals("test_1", vertices.get(0).getId());
        assertSame(vertices.get(0), vertices.get(1));
    }

    @Test
    public void testFromJson4() throws Exception {
        Term property1 = TermFactory.instance().findPropertyTerm("property_1");
        Term property2 = TermFactory.instance().findPropertyTerm("property_2");
        Term property3 = TermFactory.instance().findPropertyTerm("property_3");
        Term property4 = TermFactory.instance().findPropertyTerm("property_4");
        Term property5 = TermFactory.instance().findPropertyTerm("property_5");
        ObjectMapper mapper = JsonUtils.createMapper();
        Observable observable = mapper.readValue(TestUtils.getResource(this.getClass(), "observable-4.json"), Observable.class);
        assertEquals("test_1", observable.getId());
        assertTrue(observable.hasProperty(property1, true));
        assertFalse(observable.hasProperty(property1, false));
        assertTrue(observable.hasProperty(property2, 1.0));
        assertTrue(observable.hasProperty(property3, 11));
        assertTrue(observable.hasProperty(property4, "Hello"));
        assertTrue(observable.hasProperty(property5, null));
    }

    @Test
    public void testFromJson5() throws Exception {
        ObjectMapper mapper = JsonUtils.createMapper();
        Observable observable = mapper.readValue(TestUtils.getResource(this.getClass(), "observable-5.json"), Observable.class);
        assertEquals("test_5", observable.getId());
        assertEquals(Integer.class, observable.getType());
    }

    @Test
    public void testFromJson6() throws Exception {
        ObjectMapper mapper = JsonUtils.createMapper();
        Observable observable = mapper.readValue(TestUtils.getResource(this.getClass(), "observable-6.json"), Observable.class);
        assertEquals("test_6", observable.getId());
        assertEquals(Observable.Multiplicity.REQUIRED, observable.getMultiplicity());
    }

    @Test
    public void testFromJson7() throws Exception {
        ObjectMapper mapper = JsonUtils.createMapper();
        Observable observable = mapper.readValue(TestUtils.getResource(this.getClass(), "observable-7.json"), Observable.class);
        assertEquals("test_7", observable.getId());
        assertEquals(Observable.Multiplicity.MANY, observable.getMultiplicity());
    }

    @Test
    public void testGetTerm1() {
        Observable observable = new Observable("scientificName");
        Term term = observable.getTerm();
        assertEquals(DwcTerm.scientificName, term);
    }

    @Test
    public void testGetTerm2() {
        Observable observable = new Observable("variety");
        Term term = observable.getTerm();
        assertEquals("http://unknown.org/variety", term.qualifiedName());
    }

    @Test
    public void testGetTerm3() {
        Observable observable = new Observable("variety");
        observable.setUri(URI.create("http://id.ala.org.au/terms/nameComplete"));
        Term term = observable.getTerm();
        assertEquals("http://id.ala.org.au/terms/nameComplete", term.qualifiedName());
    }

    @Test
    public void testGetExternal1() throws Exception {
        Observable observable = new Observable("vertex1");
        assertEquals("vertex1", observable.getExternal(ExternalContext.LUCENE));
    }

    @Test
    public void testGetExternal2() throws Exception {
        Observable observable = new Observable("vertex1");
        observable.setExternal(ExternalContext.LUCENE, "v_1");
        assertEquals("v_1", observable.getExternal(ExternalContext.LUCENE));
    }

    @Test
    public void testGetProperty1() throws Exception {
        Term property1 = TermFactory.instance().findPropertyTerm("property_1");
        Observable observable = new Observable("vertex1");
        observable.setProperty(property1, 45);
        assertEquals(45, observable.getProperty(property1));
    }

}
