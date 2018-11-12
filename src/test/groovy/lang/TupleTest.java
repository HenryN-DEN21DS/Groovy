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

import groovy.util.function.Consumer0;
import groovy.util.function.Consumer1;
import groovy.util.function.Consumer2;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static groovy.lang.Tuple.collectors;
import static groovy.lang.Tuple.tuple;
import static java.util.stream.Collectors.averagingInt;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;

/**
 * @author James Strachan
 */
public class TupleTest extends TestCase {

    final Object[] data = {"a", "b", "c"};
    final Tuple t = new Tuple(data);

    public void testSize() {
        assertEquals("Size of " + t, 3, t.size());

        assertEquals("get(0)", "a", t.get(0));
        assertEquals("get(1)", "b", t.get(1));
    }

    public void testGetOutOfTuple() {
        try {
            t.get(-1);
            fail("Should have thrown IndexOut");
        } catch (IndexOutOfBoundsException e) {
            // worked
        }
        try {
            t.get(10);
            fail("Should have thrown IndexOut");
        } catch (IndexOutOfBoundsException e) {
            // worked
        }

    }

    public void testContains() {
        assertTrue("contains a", t.contains("a"));
        assertTrue("contains b", t.contains("b"));
    }

    public void testSubList() {
        List s = t.subList(1, 2);

        assertTrue("is a Tuple", s instanceof Tuple);

        assertEquals("size", 1, s.size());
    }

    public void testSubTuple() {
        Tuple s = t.subTuple(1, 2);

        assertTrue("is a Tuple", s instanceof Tuple);

        assertEquals("size", 1, s.size());
    }

    public void testHashCodeAndEquals() {
        Tuple a = new Tuple(new Object[]{"a", "b", "c"});
        Tuple b = new Tuple(new Object[]{"a", "b", "c"});
        Tuple c = new Tuple(new Object[]{"d", "b", "c"});
        Tuple d = new Tuple(new Object[]{"a", "b"});
        Tuple2<String, String> e = new Tuple2<String, String>("a", "b");
        Tuple2<String, String> f = new Tuple2<String, String>("a", "c");

        assertEquals("hashcode", a.hashCode(), b.hashCode());
        assertTrue("hashcode", a.hashCode() != c.hashCode());

        assertEquals("a and b", a, b);
        assertFalse("a != c", a.equals(c));

        assertFalse("!a.equals(null)", a.equals(null));

        assertTrue("d.equals(e)", d.equals(e));
        assertTrue("e.equals(d)", e.equals(d));
        assertFalse("!e.equals(f)", e.equals(f));
        assertFalse("!f.equals(e)", f.equals(e));
    }

    public void testIterator() {
    }

    public void testTuple1() {
        Tuple1<Integer> t = new Tuple1<>(1);

        assertEquals(1, t.size());

        assertEquals(new Integer(1), t.getFirst());
        assertEquals(1, t.get(0));

        assertEquals(t, t.subTuple(0, t.size()));
    }

    public void testTuple2() {
        Tuple2<Integer, Integer> t = new Tuple2<>(1, 2);

        assertEquals(2, t.size());

        assertEquals(new Integer(1), t.getFirst());
        assertEquals(1, t.get(0));

        assertEquals(new Integer(2), t.getSecond());
        assertEquals(2, t.get(1));

        assertEquals(t, t.subTuple(0, t.size()));
    }

    public void testTuple3() {
        Tuple3<Integer, Integer, Integer> t = new Tuple3<>(1, 2, 3);

        assertEquals(3, t.size());

        assertEquals(new Integer(1), t.getFirst());
        assertEquals(1, t.get(0));

        assertEquals(new Integer(2), t.getSecond());
        assertEquals(2, t.get(1));

        assertEquals(new Integer(3), t.getThird());
        assertEquals(3, t.get(2));

        assertEquals(t, t.subTuple(0, t.size()));
    }

    public void testTuple4() {
        Tuple4<Integer, Integer, Integer, Integer> t = new Tuple4<>(1, 2, 3, 4);

        assertEquals(4, t.size());

        assertEquals(new Integer(1), t.getFirst());
        assertEquals(1, t.get(0));

        assertEquals(new Integer(2), t.getSecond());
        assertEquals(2, t.get(1));

        assertEquals(new Integer(3), t.getThird());
        assertEquals(3, t.get(2));

        assertEquals(new Integer(4), t.getFourth());
        assertEquals(4, t.get(3));

        assertEquals(t, t.subTuple(0, t.size()));
    }

    public void testTuple5() {
        Tuple5<Integer, Integer, Integer, Integer, Integer> t = new Tuple5<>(1, 2, 3, 4, 5);

        assertEquals(5, t.size());

        assertEquals(new Integer(1), t.getFirst());
        assertEquals(1, t.get(0));

        assertEquals(new Integer(2), t.getSecond());
        assertEquals(2, t.get(1));

        assertEquals(new Integer(3), t.getThird());
        assertEquals(3, t.get(2));

        assertEquals(new Integer(4), t.getFourth());
        assertEquals(4, t.get(3));

        assertEquals(new Integer(5), t.getFifth());
        assertEquals(5, t.get(4));

        assertEquals(t, t.subTuple(0, t.size()));
    }

    public void testTuple6() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t = new Tuple6<>(1, 2, 3, 4, 5, 6);

        assertEquals(6, t.size());

        assertEquals(new Integer(1), t.getFirst());
        assertEquals(1, t.get(0));

        assertEquals(new Integer(2), t.getSecond());
        assertEquals(2, t.get(1));

        assertEquals(new Integer(3), t.getThird());
        assertEquals(3, t.get(2));

        assertEquals(new Integer(4), t.getFourth());
        assertEquals(4, t.get(3));

        assertEquals(new Integer(5), t.getFifth());
        assertEquals(5, t.get(4));

        assertEquals(new Integer(6), t.getSixth());
        assertEquals(6, t.get(5));

        assertEquals(t, t.subTuple(0, t.size()));
    }

    public void testTuple7() {
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> t = new Tuple7<>(1, 2, 3, 4, 5, 6, 7);

        assertEquals(7, t.size());

        assertEquals(new Integer(1), t.getFirst());
        assertEquals(1, t.get(0));

        assertEquals(new Integer(2), t.getSecond());
        assertEquals(2, t.get(1));

        assertEquals(new Integer(3), t.getThird());
        assertEquals(3, t.get(2));

        assertEquals(new Integer(4), t.getFourth());
        assertEquals(4, t.get(3));

        assertEquals(new Integer(5), t.getFifth());
        assertEquals(5, t.get(4));

        assertEquals(new Integer(6), t.getSixth());
        assertEquals(6, t.get(5));

        assertEquals(new Integer(7), t.getSeventh());
        assertEquals(7, t.get(6));

        assertEquals(t, t.subTuple(0, t.size()));
    }

    public void testTuple8() {
        Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t = new Tuple8<>(1, 2, 3, 4, 5, 6, 7, 8);

        assertEquals(8, t.size());

        assertEquals(new Integer(1), t.getFirst());
        assertEquals(1, t.get(0));

        assertEquals(new Integer(2), t.getSecond());
        assertEquals(2, t.get(1));

        assertEquals(new Integer(3), t.getThird());
        assertEquals(3, t.get(2));

        assertEquals(new Integer(4), t.getFourth());
        assertEquals(4, t.get(3));

        assertEquals(new Integer(5), t.getFifth());
        assertEquals(5, t.get(4));

        assertEquals(new Integer(6), t.getSixth());
        assertEquals(6, t.get(5));

        assertEquals(new Integer(7), t.getSeventh());
        assertEquals(7, t.get(6));

        assertEquals(new Integer(8), t.getEighth());
        assertEquals(8, t.get(7));

        assertEquals(t, t.subTuple(0, t.size()));
    }

    public void testTuple9() {
        Tuple9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t = new Tuple9<>(1, 2, 3, 4, 5, 6, 7, 8, 9);

        assertEquals(9, t.size());

        assertEquals(new Integer(1), t.getFirst());
        assertEquals(1, t.get(0));

        assertEquals(new Integer(2), t.getSecond());
        assertEquals(2, t.get(1));

        assertEquals(new Integer(3), t.getThird());
        assertEquals(3, t.get(2));

        assertEquals(new Integer(4), t.getFourth());
        assertEquals(4, t.get(3));

        assertEquals(new Integer(5), t.getFifth());
        assertEquals(5, t.get(4));

        assertEquals(new Integer(6), t.getSixth());
        assertEquals(6, t.get(5));

        assertEquals(new Integer(7), t.getSeventh());
        assertEquals(7, t.get(6));

        assertEquals(new Integer(8), t.getEighth());
        assertEquals(8, t.get(7));

        assertEquals(new Integer(9), t.getNinth());
        assertEquals(9, t.get(8));

        assertEquals(t, t.subTuple(0, t.size()));
    }

    public void testEqualsHashCode() {
        Set<Tuple2<Integer, String>> set = new HashSet<>();

        set.add(tuple(1, "abc"));
        assertEquals(1, set.size());
        set.add(tuple(1, "abc"));
        assertEquals(1, set.size());
        set.add(tuple(null, null));
        assertEquals(2, set.size());
        set.add(tuple(null, null));
        assertEquals(2, set.size());
        set.add(tuple(1, null));
        assertEquals(3, set.size());
        set.add(tuple(1, null));
        assertEquals(3, set.size());
    }

    public void testEqualsNull() {
        assertFalse(tuple(1).equals(null));
        assertFalse(tuple(1, 2).equals(null));
        assertFalse(tuple(1, 2, 3).equals(null));
    }

    public void testToMap() {
        Map<Integer, Object> m = new LinkedHashMap<>();
        m.put(0, 1);
        m.put(1, "a");
        m.put(2, null);
        assertEquals(m, tuple(1, "a", null).toMap(i -> i));
    }

    public void testSwap() {
        assertEquals(tuple(1, "a"), tuple("a", 1).swap());
        assertEquals(tuple(1, "a"), tuple(1, "a").swap().swap());
    }

    public void testConcat() {
        assertEquals(tuple(1, "a"), tuple(1).concat("a"));
        assertEquals(tuple(1, "a", 2), tuple(1).concat("a").concat(2));

        assertEquals(tuple(1, "a"), tuple(1).concat(tuple("a")));
        assertEquals(tuple(1, "a", 2, "b", 3, "c", 4, "d"), tuple(1).concat(tuple("a", 2, "b").concat(tuple(3).concat(tuple("c", 4, "d")))));
    }

    public void testCompareTo() {
        Set<Tuple2<Integer, String>> set = new TreeSet<>();

        set.add(tuple(2, "a"));
        set.add(tuple(1, "b"));
        set.add(tuple(1, "a"));
        set.add(tuple(2, "a"));

        assertEquals(3, set.size());
        assertEquals(Arrays.asList(tuple(1, "a"), tuple(1, "b"), tuple(2, "a")), new ArrayList<>(set));
    }

    public void testCompareToWithNulls() {
        Set<Tuple2<Integer, String>> set = new TreeSet<>();

        set.add(tuple(2, "a"));
        set.add(tuple(1, "b"));
        set.add(tuple(1, null));
        set.add(tuple(null, "a"));
        set.add(tuple(null, "b"));
        set.add(tuple(null, null));

        assertEquals(6, set.size());
        assertEquals(Arrays.asList(tuple(1, "b"), tuple(1, null), tuple(2, "a"), tuple(null, "a"), tuple(null, "b"), tuple(null, null)), new ArrayList<>(set));
    }

    public void testIterable() {
        LinkedList<Object> list = new LinkedList<>(tuple(1, "b", null));
        for (Object o : tuple(1, "b", null)) {
            assertEquals(list.poll(), o);
        }
    }

    public void testFunctions() {
        assertEquals("[1, b, null]", tuple(1, "b", null).map((v1, v2, v3) -> tuple(v1, v2, v3).toString()));
        assertEquals("1-b", tuple(1, "b", null).map((v1, v2, v3) -> v1 + "-" + v2));
    }

    public void testMapN() {
        assertEquals(tuple(1, "a", 2, "b"), tuple(1, null, 2, null).map2(v -> "a").map4(v -> "b"));
    }

    public void testOverlaps() {
        assertTrue(Tuple2.overlaps(tuple(1, 3), tuple(1, 3)));
        assertTrue(Tuple2.overlaps(tuple(1, 3), tuple(2, 3)));
        assertTrue(Tuple2.overlaps(tuple(1, 3), tuple(2, 4)));
        assertTrue(Tuple2.overlaps(tuple(1, 3), tuple(3, 4)));
        assertFalse(Tuple2.overlaps(tuple(1, 3), tuple(4, 5)));
        assertFalse(Tuple2.overlaps(tuple(1, 1), tuple(2, 2)));
    }

    public void testIntersect() {
        assertEquals(Optional.of(tuple(2, 3)), Tuple2.intersect(tuple(1, 3), tuple(2, 4)));
        assertEquals(Optional.of(tuple(3, 3)), Tuple2.intersect(tuple(1, 3), tuple(3, 5)));
        assertEquals(Optional.empty(), Tuple2.intersect(tuple(1, 3), tuple(4, 5)));
    }

    public void testCollectors() {
        assertEquals(
                tuple(3L),
                Stream.of(1, 2, 3)
                        .collect(collectors(counting()))
        );

        assertEquals(
                tuple(3L, "1, 2, 3"),
                Stream.of(1, 2, 3)
                        .collect(collectors(
                                counting(),
                                mapping(Object::toString, joining(", "))
                        ))
        );

        assertEquals(
                tuple(3L, "1, 2, 3", 2.0),
                Stream.of(1, 2, 3)
                        .collect(collectors(
                                counting(),
                                mapping(Object::toString, joining(", ")),
                                averagingInt(Integer::intValue)
                        ))
        );
    }

    public void testLimit() {
        assertEquals(
                tuple(),
                tuple(1, "A", 2, "B").limit0()
        );
        assertEquals(
                tuple(1),
                tuple(1, "A", 2, "B").limit1()
        );
        assertEquals(
                tuple(1, "A"),
                tuple(1, "A", 2, "B").limit2()
        );
        assertEquals(
                tuple(1, "A", 2),
                tuple(1, "A", 2, "B").limit3()
        );
        assertEquals(
                tuple(1, "A", 2, "B"),
                tuple(1, "A", 2, "B").limit4()
        );
    }

    public void testSkip() {
        assertEquals(
                tuple(),
                tuple(1, "A", 2, "B").skip4()
        );
        assertEquals(
                tuple("B"),
                tuple(1, "A", 2, "B").skip3()
        );
        assertEquals(
                tuple(2, "B"),
                tuple(1, "A", 2, "B").skip2()
        );
        assertEquals(
                tuple("A", 2, "B"),
                tuple(1, "A", 2, "B").skip1()
        );
        assertEquals(
                tuple(1, "A", 2, "B"),
                tuple(1, "A", 2, "B").skip0()
        );
    }

    public void testSplit() {
        assertEquals(
                tuple(
                        tuple(),
                        tuple(1, "A", 2, "B")
                ),
                tuple(1, "A", 2, "B").split0()
        );
        assertEquals(
                tuple(
                        tuple(1),
                        tuple("A", 2, "B")
                ),
                tuple(1, "A", 2, "B").split1()
        );
        assertEquals(
                tuple(
                        tuple(1, "A"),
                        new Tuple2<>(2, "B")
                ),
                tuple(1, "A", 2, "B").split2()
        );
        assertEquals(
                tuple(
                        tuple(1, "A", 2),
                        tuple("B")
                ),
                tuple(1, "A", 2, "B").split3()
        );
        assertEquals(
                tuple(
                        tuple(1, "A", 2, "B"),
                        tuple()
                ),
                tuple(1, "A", 2, "B").split4()
        );
    }

    int result;
    public void testConsumers() {
        Consumer0 c0 = () -> { result = 1; };
        Runnable r = c0.toRunnable();
        Consumer0 c0a = Consumer0.from(r);

        result = 0;
        c0.accept();
        assertEquals(1, result);

        result = 0;
        c0.accept(Tuple.tuple());
        assertEquals(1, result);

        result = 0;
        r.run();
        assertEquals(1, result);

        result = 0;
        c0a.accept();
        assertEquals(1, result);

        Consumer1<Integer> c1 = i -> { result = i; };
        Consumer<Integer> c1a = c1.toConsumer();
        Consumer1<Integer> c1b = Consumer1.from(c1a);

        result = 0;
        c1.accept(1);
        assertEquals(1, result);

        result = 0;
        c1.accept(Tuple.tuple(1));
        assertEquals(1, result);

        result = 0;
        c1a.accept(1);
        assertEquals(1, result);

        result = 0;
        c1b.accept(1);
        assertEquals(1, result);

        Consumer2<Integer, Integer> c2 = (i, j) -> { result = i + j; };
        BiConsumer<Integer, Integer> c2a = c2.toBiConsumer();
        Consumer2<Integer, Integer> c2b = Consumer2.from(c2a);

        result = 0;
        c2.accept(1, 2);
        assertEquals(3, result);

        result = 0;
        c2.accept(Tuple.tuple(1, 2));
        assertEquals(3, result);

        result = 0;
        c2a.accept(1, 2);
        assertEquals(3, result);

        result = 0;
        c2b.accept(1, 2);
        assertEquals(3, result);
    }

}
