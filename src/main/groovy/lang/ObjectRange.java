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

import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.IteratorClosureAdapter;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents an inclusive list of objects from a value to a value using
 * comparators.
 *
 * Note: This class is similar to {@link IntRange}. If you make any changes to this
 * class, you might consider making parallel changes to {@link IntRange}.
 */
public class ObjectRange extends AbstractList implements Range {

    /**
     * The first value in the range.
     */
    private final Comparable from;

    /**
     * The last value in the range.
     */
    private final Comparable to;

    /**
     * The cached size, or -1 if not yet computed
     */
    private int size = -1;

    /**
     * <code>true</code> if the range counts backwards from <code>to</code> to <code>from</code>.
     */
    private final boolean reverse;

    /**
     * Creates a new {@link ObjectRange}. Creates a reversed range if
     * <code>from</code> &lt; <code>to</code>.
     *
     * @param from the first value in the range.
     * @param to   the last value in the range.
     */
    public ObjectRange(Comparable from, Comparable to) {
        this(from, to, null);
    }

    /**
     * Creates a new {@link ObjectRange} assumes smaller &lt;&#61; larger, else behavior is undefined.
     * Caution: Prefer the other constructor when in doubt.
     *
     * Optimized Constructor avoiding initial computation of comparison.
     */
    public ObjectRange(Comparable smaller, Comparable larger, boolean reverse) {
        this(smaller, larger, Boolean.valueOf(reverse));
    }

    /**
     * Constructs a Range, computing reverse if not provided. When providing reverse,
     * 'smaller' must not be larger than 'larger'.
     *
     * @param smaller start of the range, must no be larger than to when reverse != null
     * @param larger  end of the range, must be larger than from when reverse != null
     * @param reverse direction of the range. If null, causes direction to be computed (can be expensive).
     */
    private ObjectRange(Comparable smaller, Comparable larger, Boolean reverse) {
        if (smaller == null) {
            throw new IllegalArgumentException("Must specify a non-null value for the 'from' index in a Range");
        } else if (larger == null) {
            throw new IllegalArgumentException("Must specify a non-null value for the 'to' index in a Range");
        }
        if (reverse == null) {
            boolean computedReverse = areReversed(smaller, larger);
            // ensure invariant from <= to
            if (computedReverse) {
                Comparable temp = larger;
                larger = smaller;
                smaller = temp;
            }
            this.reverse = computedReverse;
        } else {
            this.reverse = reverse;
        }

        if (smaller instanceof Short) {
            smaller = ((Short) smaller).intValue();
        } else if (smaller instanceof Float) {
            smaller = ((Float) smaller).doubleValue();
        }
        if (larger instanceof Short) {
            larger = ((Short) larger).intValue();
        } else if (larger instanceof Float) {
            larger = ((Float) larger).doubleValue();
        }

        if (smaller instanceof Integer && larger instanceof Long) {
            smaller = ((Integer) smaller).longValue();
        } else if (larger instanceof Integer && smaller instanceof Long) {
            larger = ((Integer) larger).longValue();
        }

        // TODO: should we care about different types here?
        if (smaller.getClass() == larger.getClass()) {
            this.from = smaller;
            this.to = larger;
        } else {
            this.from = normaliseStringType(smaller);
            this.to = normaliseStringType(larger);
        }
        checkBoundaryCompatibility();
    }

    /**
     * throws IllegalArgumentException if to and from are incompatible, meaning they e.g. (likely) produce infinite sequences.
     * Called at construction time, subclasses may override cautiously (using only members to and from).
     */
    protected void checkBoundaryCompatibility() {
        if (from instanceof String || to instanceof String) {
            // this test depends deeply on the String.next implementation
            // 009.next is 00:, not 010
            String start = from.toString();
            String end = to.toString();
            if (start.length() > end.length()) {
                throw new IllegalArgumentException("Incompatible Strings for Range: starting String is longer than ending string");
            }
            int length = Math.min(start.length(), end.length());
            int i;
            for (i = 0; i < length; i++) {
                if (start.charAt(i) != end.charAt(i)) break;
            }
            if (i < length - 1) {
                throw new IllegalArgumentException("Incompatible Strings for Range: String#next() will not reach the expected value");
            }
        }
    }

    private static boolean areReversed(Comparable from, Comparable to) {
        try {
            return ScriptBytecodeAdapter.compareGreaterThan(from, to);
        } catch (ClassCastException cce) {
            throw new IllegalArgumentException("Unable to create range due to incompatible types: " + from.getClass().getSimpleName() + ".." + to.getClass().getSimpleName() + " (possible missing brackets around range?)", cce);
        }
    }

    public boolean equals(Object that) {
        return (that instanceof ObjectRange) ? equals((ObjectRange) that) : super.equals(that);
    }

    /**
     * Compares an {@link ObjectRange} to another {@link ObjectRange}.
     *
     * @param that the object to check equality with
     * @return <code>true</code> if the ranges are equal
     */
    public boolean equals(ObjectRange that) {
        return that != null
                && this.reverse == that.reverse
                && DefaultTypeTransformation.compareEqual(this.from, that.from)
                && DefaultTypeTransformation.compareEqual(this.to, that.to);
    }

    public Comparable getFrom() {
        return from;
    }

    public Comparable getTo() {
        return to;
    }

    public boolean isReverse() {
        return reverse;
    }

    public Object get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + " should not be negative");
        }

        StepIterator iter = new StepIterator(this, 1);

        Object value = iter.next();
        for (int i = 0; i < index; i++) {
            if (!iter.hasNext()) {
                throw new IndexOutOfBoundsException("Index: " + index + " is too big for range: " + this);
            }
            value = iter.next();
        }
        return value;
    }


    /**
     * Checks whether a value is between the from and to values of a Range
     *
     * @param value the value of interest
     * @return true if the value is within the bounds
     */
    public boolean containsWithinBounds(Object value) {
        if (value instanceof Comparable) {
            int result = compareTo(from, (Comparable) value);
            return result == 0 || result < 0 && compareTo(to, (Comparable) value) >= 0;
        }
        return contains(value);
    }

    protected int compareTo(Comparable first, Comparable second) {
        return DefaultGroovyMethods.numberAwareCompareTo(first, second);
    }

    /**
     * protection against calls from Groovy
     */
    private void setSize(int size) {
        throw new UnsupportedOperationException("size must not be changed");
    }

    public int size() {
        if (size == -1) {
            int tempsize = 0;
            if ((from instanceof Integer || from instanceof Long)
                    && (to instanceof Integer || to instanceof Long)) {
                // let's fast calculate the size
                BigInteger fromNum = new BigInteger(from.toString());
                BigInteger toNum = new BigInteger(to.toString());
                BigInteger sizeNum = toNum.subtract(fromNum).add(new BigInteger("1"));
                tempsize = sizeNum.intValue();
                if (!BigInteger.valueOf(tempsize).equals(sizeNum)) {
                    tempsize = Integer.MAX_VALUE;
                }
            } else if (from instanceof Character && to instanceof Character) {
                // let's fast calculate the size
                char fromNum = (Character) from;
                char toNum = (Character) to;
                tempsize = toNum - fromNum + 1;
            } else if (from instanceof BigDecimal || to instanceof BigDecimal ||
                    from instanceof BigInteger || to instanceof BigInteger) {
                // let's fast calculate the size
                BigDecimal fromNum = new BigDecimal(from.toString());
                BigDecimal toNum = new BigDecimal(to.toString());
                BigInteger sizeNum = toNum.subtract(fromNum).add(new BigDecimal(1.0)).toBigInteger();
                tempsize = sizeNum.intValue();
                if (!BigInteger.valueOf(tempsize).equals(sizeNum)) {
                    tempsize = Integer.MAX_VALUE;
                }
            } else {
                // let's brute-force calculate the size by iterating start to end
                Iterator iter = new StepIterator(this, 1);

                while (iter.hasNext()) {
                    tempsize++;
                    // integer overflow
                    if (tempsize < 0) {
                        break;
                    }
                    iter.next();
                }

            }
            // integer overflow
            if (tempsize < 0) {
                tempsize = Integer.MAX_VALUE;
            }
            size = tempsize;
        }
        return size;
    }

    public List subList(int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex == toIndex) {
            return new EmptyRange(from);
        }

        // Performance detail:
        // not using get(fromIndex), get(toIndex) in the following to avoid stepping over elements twice
        StepIterator iter = new StepIterator(this, 1);

        Object value = iter.next();
        int i = 0;
        for (; i < fromIndex; i++) {
            if (!iter.hasNext()) {
                throw new IndexOutOfBoundsException("Index: " + i + " is too big for range: " + this);
            }
            value = iter.next();
        }
        Object fromValue = value;
        for (;i < toIndex - 1; i++) {
            if (!iter.hasNext()) {
                throw new IndexOutOfBoundsException("Index: " + i + " is too big for range: " + this);
            }
            value = iter.next();
        }
        Object toValue = value;

        return new ObjectRange((Comparable) fromValue, (Comparable) toValue, reverse);
    }

    public String toString() {
        return reverse ? "" + to + ".." + from : "" + from + ".." + to;
    }

    public String inspect() {
        String toText = InvokerHelper.inspect(to);
        String fromText = InvokerHelper.inspect(from);
        return reverse ? "" + toText + ".." + fromText : "" + fromText + ".." + toText;
    }

    /**
     * iterates over all values and returns true if one value matches.
     * Also see containsWithinBounds.
     */
    public boolean contains(Object value) {
        Iterator it = new StepIterator(this, 1);
        if (value == null) return false;
        while (it.hasNext()) {
            try {
                if (DefaultTypeTransformation.compareEqual(value, it.next())) return true;
            } catch (ClassCastException e) {
                return false;
            }
        }
        return false;
    }

    public void step(int step, Closure closure) {
        if (step == 0 && compareTo(from, to) == 0) {
            return; // from == to and step == 0, nothing to do, so return
        }
        StepIterator iter = new StepIterator(this, step);

        while (iter.hasNext()) {
            closure.call(iter.next());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator iterator() {
        // non thread-safe iterator
        final Iterator innerIterator = new StepIterator(this, 1);
        Iterator safeIterator = new Iterator() {
            @Override
            public synchronized boolean hasNext() {
                return innerIterator.hasNext();
            }

            @Override
            public synchronized Object next() {
                return innerIterator.next();
            }

            @Override
            public void remove() {
                innerIterator.remove();
            }
        };
        return safeIterator;
    }

    /**
     * convenience class to serve in other methods.
     * It's not thread-safe, and lazily produces the next element only on calls of hasNext() or next()
     */
    private static class StepIterator implements Iterator {
        // actual step, can be +1 when desired step is -1 and direction is from high to low
        private final int step;
        private final ObjectRange range;
        private int index = -1;
        private Comparable value;
        private boolean nextFetched = true;

        private StepIterator(ObjectRange range, final int desiredStep) {
            if (desiredStep == 0 && range.compareTo(range.getFrom(), range.getTo()) != 0) {
                throw new GroovyRuntimeException("Infinite loop detected due to step size of 0");
            }

            this.range = range;

            if (range.isReverse()) {
                this.step = -desiredStep;
            } else {
                this.step = desiredStep;
            }
            if (this.step > 0) {
                value = range.getFrom();
            } else {
                value = range.getTo();
            }
        }

        public void remove() {
            this.range.remove(index);
        }

        public Object next() {
            // not thread safe
            if (!nextFetched) {
                value = peek();
                nextFetched = true;
            }
            nextFetched = false;
            index++;
            return value;
        }

        public boolean hasNext() {
            // not thread safe
            if (!nextFetched) {
                value = peek();
                nextFetched = true;
            }
            return value != null;
        }

        private Comparable peek() {
            if (step > 0) {
                Comparable peekValue = value;
                for (int i = 0; i < step; i++) {
                    peekValue = (Comparable) range.increment(peekValue);
                    // handle back to beginning due to modulo incrementing
                    if (range.compareTo(peekValue, range.from) <= 0) return null;
                }
                if (range.compareTo(peekValue, range.to) <= 0) {
                    return peekValue;
                }
            } else {
                int positiveStep = -step;
                Comparable peekValue = value;

                for (int i = 0; i < positiveStep; i++) {
                    peekValue = (Comparable) range.decrement(peekValue);
                    // handle back to beginning due to modulo decrementing
                    if (range.compareTo(peekValue, range.to) >= 0) return null;
                }
                if (range.compareTo(peekValue, range.from) >= 0) {
                    return peekValue;
                }
            }
            return null;
        }
    }

    public List step(int step) {
        IteratorClosureAdapter adapter = new IteratorClosureAdapter(this);
        step(step, adapter);
        return adapter.asList();
    }

    /**
     * Increments by one
     *
     * @param value the value to increment
     * @return the incremented value
     */
    protected Object increment(Object value) {
        return InvokerHelper.invokeMethod(value, "next", null);
    }

    /**
     * Decrements by one
     *
     * @param value the value to decrement
     * @return the decremented value
     */
    protected Object decrement(Object value) {
        return InvokerHelper.invokeMethod(value, "previous", null);
    }

    private static Comparable normaliseStringType(final Comparable operand) {
        if (operand instanceof Character) {
            return (int) (Character) operand;
        } else if (operand instanceof String) {
            final String string = (String) operand;

            if (string.length() == 1)
                return (int) string.charAt(0);
            else
                return string;
        } else {
            return operand;
        }
    }
}
