/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package groovy.lang;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author James Strachan
 */
public class ObjectRangeTest extends TestCase {

    public void testSize() {
        Range r = createRange(0, 10);
        assertEquals("Size of " + r, 11, r.size());
        r = createRange(0, 1);
        assertEquals("Size of " + r, 2, r.size());
        r = createRange(1, 0);
        assertEquals("Size of " + r, 2, r.size());
        r = createRange(0, 0);
        assertEquals("Size of " + r, 1, r.size());

        r = createRange(new BigDecimal("2.1"), new BigDecimal("10.0"));
        assertEquals("Size of " + r, 8, r.size());
        r = createRange(new BigDecimal("10"), new BigDecimal("2.1"));
        assertEquals("Size of " + r, 8, r.size());

        r = createRange("a", "d");
        assertEquals("Size of " + r, 4, r.size());
        r = createRange("d", "a");
        assertEquals("Size of " + r, 4, r.size());

        r = createRange("aa1", "aa4");
        assertEquals("Size of " + r, 4, r.size());
        r = createRange("aa4", "aa1");
        assertEquals("Size of " + r, 4, r.size());
        r = createRange('7',  ';');
        assertEquals(5, r.size());

        // '7', '8', '9', ':', ';'
        Range mixed = createRange('7',  ';');
        assertEquals(5, mixed.size());
        mixed = createRange('7',  59.5);
        assertEquals(5, mixed.size());
        mixed = createRange('7',  59);
        assertEquals(5, mixed.size());
        mixed = createRange('7',  new BigInteger("59"));
        assertEquals(5, mixed.size());
        mixed = createRange('7',  new BigDecimal("59.5"));
        assertEquals(5, mixed.size());
    }

    public void testProperties() {
        Range r = createRange(0, 10);
        assertEquals("from", 0, r.getFrom());
        assertEquals("to", 10, r.getTo());

        r = createRange(10, 0);
        assertEquals("from", 0, r.getFrom());
        assertEquals("to", 10, r.getTo());
    }

    public void testGet() {
        Range r = createRange(10, 20);
        for (int i = 0; i < 10; i++) {
            Integer value = (Integer) r.get(i);
            assertEquals("Item at index: " + i, i + 10, value.intValue());
        }

        r = createRange(new BigDecimal("3.2"), new BigDecimal("9.9"));
        for (int i = 0; i < r.size(); i++) {
            BigDecimal value = (BigDecimal) r.get(i);
            assertEquals("Item at index: " + i, new BigDecimal("3.2").add(new BigDecimal("" + i)), value);
        }

        r = new ObjectRange(10, 20, false);
        for (int i = 0; i < 10; i++) {
            Integer value = (Integer) r.get(i);
            assertEquals("Item at index: " + i, i + 10, value.intValue());
        }

        r = new ObjectRange(10, 20, true);
        for (int i = 0; i < 10; i++) {
            Integer value = (Integer) r.get(i);
            assertEquals("Item at index: " + i, 20 - i, value.intValue());
        }
    }

    public void testNullForFromOrToIsIllegal() {
        Comparable dontcare = new Integer(0);
        try {
            new ObjectRange((Comparable)null, dontcare);
            fail("Should have thrown IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            // worked
        }
    }

    public void testGetOutOfRange() {
        Range r = createRange(1, 1);
        assertEquals("Item at index: 0", 1, r.get(0));

        try {
            r.get(-1);
            fail("Should have thrown IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
            // worked
        }

        try {
            r.get(1);
            fail("Should have thrown IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
            // worked
        }

        r = createRange(10, 20);

        try {
            r.get(-1);
            fail("Should have thrown IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
            // worked
        }
        try {
            r.get(11);
            fail("Should have thrown IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
            // worked
        }

        r = createRange(new BigDecimal("-4.3"), new BigDecimal("1.4"));

        try {
            r.get(-1);
            fail("Should have thrown IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
            // worked
        }
        try {
            r.get(7);
            fail("Should have thrown IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
            // worked
        }

    }

    public void testContains() {
        Range r = createRange(10, 20);

        assertTrue("contains 11", r.contains(new Integer(11)));
        assertTrue("contains 10", r.contains(new Integer(10)));
        assertTrue("contains 19", r.contains(new Integer(19)));
        assertFalse("contains 9", r.contains(new Integer(9)));
        assertFalse("contains 21", r.contains(new Integer(21)));
        assertFalse("contains 100", r.contains(new Integer(100)));
        assertFalse("contains -1", r.contains(new Integer(-1)));

        r = createRange(new BigDecimal("2.1"), new BigDecimal("10.0"));

        assertTrue("contains 9.1", r.contains(new BigDecimal("9.1")));
        assertFalse("contains 10.1", r.contains(new BigDecimal("10.1")));
        assertFalse("contains 8.0", r.contains(new BigDecimal("8.0")));
        assertTrue("containsWithinBounds 8.0", r.containsWithinBounds(new BigDecimal("8.0")));
        assertTrue("containsWithinBounds 9.9999", r.containsWithinBounds(new BigDecimal("9.9999")));
        assertTrue("containsWithinBounds 10.0", r.containsWithinBounds(new BigDecimal("10.0")));
        assertFalse("containsWithinBounds 10.0001", r.containsWithinBounds(new BigDecimal("10.0001")));
    }

    public void testContainsWithLikeNumbers() {
        Range r = new ObjectRange(new Integer(1), new Short((short)3));
        assertTrue("contains 2", r.contains(new Integer(2)));
        r = new ObjectRange(new Float(1.0), new Double(3.0));
        assertTrue("contains 2.0d", r.contains(new Double(2.0)));
        assertTrue("contains 2.0g", r.contains(new BigDecimal(2.0)));
        r = new ObjectRange(new BigDecimal(1.0), new BigDecimal(3.0));
        assertTrue("contains 2.0d", r.contains(new Double(2.0)));
        assertTrue("contains 2.0f", r.contains(new Float(2.0)));
    }

    public void testContainsWithIncompatibleType() {
        Range r = new ObjectRange(new Integer(1), new Short((short)3));
        assertFalse("shouldn't contain string", r.contains("String"));
    }

    public void testSubList() {
        Range r = createRange(10, 20);
        assertEquals("from", 10, r.getFrom());
        assertEquals("to", 20, r.getTo());
        assertEquals("size", 11, r.size());

        List s = r.subList(2, 4);

        Range sr = (Range) s;

        assertEquals("from", 12, sr.getFrom());
        assertEquals("to", 13, sr.getTo());
        assertEquals("size", 2, sr.size());

        s = r.subList(0, 11);

        sr = (Range) s;

        assertEquals("from", 10, sr.getFrom());
        assertEquals("to", 20, sr.getTo());
        assertEquals("size", 11, sr.size());

        try {
            r.subList(-2, 4);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // pass
        }

        try {
            r.subList(5, 12);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // pass
        }

        r = createRange(new BigDecimal("0.5"), new BigDecimal("8.5"));
        assertEquals("size", 9, r.size());
        s = r.subList(2, 5);
        sr = (Range) s;

        assertEquals("from", new BigDecimal("2.5"), sr.getFrom());
        assertEquals("to", new BigDecimal("4.5"), sr.getTo());
        assertTrue("contains 4.5", sr.contains(new BigDecimal("4.5")));
        assertFalse("contains 5.5", sr.contains(new BigDecimal("5.5")));
        assertEquals("size", 3, sr.size());

    }

    public void testHashCodeAndEquals() {
        Range a = createRange(1, 11);
        Range b = createRange(1, 11);
        Range c = createRange(2, 11);

        assertEquals("hashcode", a.hashCode(), b.hashCode());
        assertTrue("hashcode", a.hashCode() != c.hashCode());

        assertEquals("a and b", a, b);
        assertFalse("a != c", a.equals(c));
    }

    public void testIteratorAndStep1() {
        Range r = createRange(5, 11);

        int i = 4;
        for (Iterator it = r.iterator(); it.hasNext();) {
            i++;
            assertEquals("equals to " + i, new Integer(i), (Integer) (it.next()));
        }
        assertEquals(11, i);

        i = 4;
        for (Iterator it = r.step(1).iterator(); it.hasNext();) {
            i++;
            assertEquals("equals to " + i, new Integer(i), (Integer) (it.next()));
        }
        assertEquals(11, i);

        r = createRange(new BigDecimal("5.0"), new BigDecimal("11.0"));
        BigDecimal one = new BigDecimal("1.0");

        BigDecimal val = new BigDecimal("5.0");
        for (Iterator it = r.iterator(); it.hasNext();) {
            assertEquals("equals to " + val, val, (BigDecimal) (it.next()));
            val = val.add(one);
        }
        assertEquals(11, i);

        val = new BigDecimal("5.0");
        for (Iterator it = r.step(1).iterator(); it.hasNext();) {
            assertEquals("equals to " + val, val, (BigDecimal) (it.next()));
            val = val.add(one);
        }
        assertEquals(11, i);

        r = createRange(new Character('a'), new Character('z'));
        char valChar = 'a';
        for (Iterator it = r.iterator(); it.hasNext();) {
            assertEquals("equals to " + valChar, valChar, ((Character) it.next()).charValue());
            if (it.hasNext()) {
                valChar = (char) (((int) valChar) + 1);
            }
        }
        assertEquals('z', valChar);

        valChar = 'a';
        for (Iterator it = r.step(1).iterator(); it.hasNext();) {
            assertEquals("equals to " + valChar, valChar, ((Character) it.next()).charValue());
            if (it.hasNext()) {
                valChar = (char) (((int) valChar) + 1);
            }
        }
        assertEquals('z', valChar);
    }

    protected Range createRange(int from, int to) {
        return new ObjectRange(new Integer(from), new Integer(to));
    }

    protected Range createRange(Comparable from, Comparable to) {
        return new ObjectRange(from, to);
    }


    protected void assertEquals(String msg, int expected, Object value) {
        assertEquals(msg, new Integer(expected), value);
    }

}
