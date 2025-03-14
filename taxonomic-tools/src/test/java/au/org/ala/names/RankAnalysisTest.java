package au.org.ala.names;

import org.gbif.nameparser.api.Rank;
import org.junit.Test;

import static org.junit.Assert.*;

public class RankAnalysisTest {
    RankAnalysis analysis = new RankAnalysis();

    @Test
    public void testType1() {
        assertEquals(Rank.class, analysis.getType());
    }

    @Test
    public void testAnalyse1() throws Exception{
        assertNull(analysis.analyse(null));
    }

    @Test
    public void testAnalyse2() throws Exception {
        assertEquals(Rank.SPECIES, analysis.analyse(Rank.SPECIES));
        assertEquals(Rank.CLASS, analysis.analyse(Rank.CLASS));
        assertEquals(Rank.UNRANKED, analysis.analyse(Rank.UNRANKED));
    }

    @Test
    public void testAnalyse3() throws Exception {
        assertEquals(Rank.SUBSPECIES, analysis.analyse(Rank.SUBSPECIES));
        assertEquals(Rank.VARIETY, analysis.analyse(Rank.VARIETY));
    }

    @Test
    public void testToStore1() throws Exception {
        assertNull(analysis.toStore(null));
        assertEquals("class", analysis.toStore(Rank.CLASS));
        assertEquals("subfamily", analysis.toStore(Rank.SUBFAMILY));
    }

    @Test
    public void testFromString1() throws Exception {
        assertNull(analysis.fromString(null));
        assertNull(analysis.fromString(""));
        assertNull(analysis.fromString("  "));
        assertEquals(Rank.CLASS, analysis.fromString("class"));
        assertEquals(Rank.CLASS, analysis.fromString("CLASS"));
        assertEquals(Rank.FAMILY, analysis.fromString("Family"));
        assertEquals(Rank.SPECIES, analysis.fromString("sp"));
    }

    @Test
    public void testEquivalent1() throws Exception {
        assertNull(analysis.equivalent(null, Rank.FAMILY));
        assertNull(analysis.equivalent(Rank.FAMILY, null));
    }

    @Test
    public void testEquivalent2() throws Exception {
        assertTrue(analysis.equivalent(Rank.FAMILY, Rank.FAMILY));
        assertTrue(analysis.equivalent(Rank.SPECIES, Rank.SPECIES));
    }

    @Test
    public void testEquivalent3() throws Exception {
        assertTrue(analysis.equivalent(Rank.FAMILY, Rank.SUBFAMILY));
        assertTrue(analysis.equivalent(Rank.GENUS, Rank.SUBGENUS));
        assertTrue(analysis.equivalent(Rank.GENUS, Rank.INFRAFAMILY));
        assertTrue(analysis.equivalent(Rank.SUBSPECIES, Rank.FORM));
    }

    @Test
    public void testEquivalent4() throws Exception {
        assertFalse(analysis.equivalent(Rank.FAMILY, Rank.GENUS));
        assertFalse(analysis.equivalent(Rank.CLASS, Rank.SUBGENUS));
        assertFalse(analysis.equivalent(Rank.INFRAGENUS, Rank.SUBFAMILY));
     }


}
