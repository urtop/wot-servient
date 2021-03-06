package city.sane;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PairTest {
    @Test
    public void first() {
        Pair pair = new Pair<>(10, "beers");

        assertEquals(10, pair.first());
    }

    @Test
    public void second() {
        Pair pair = new Pair<>(10, "beers");

        assertEquals("beers", pair.second());
    }

    @Test
    public void testEquals() {
        Pair pairA = new Pair(5, "beers");
        Pair pairB = new Pair(5, "beers");
        Pair pairC = new Pair(10, "beeers");

        assertTrue(pairA.equals(pairB));
        assertTrue(pairB.equals(pairA));
        assertFalse(pairA.equals(pairC));
        assertFalse(pairC.equals(pairA));
    }
}