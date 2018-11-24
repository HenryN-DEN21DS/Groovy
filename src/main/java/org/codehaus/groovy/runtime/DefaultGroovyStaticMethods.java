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
package org.codehaus.groovy.runtime;

import groovy.lang.Closure;
import org.codehaus.groovy.reflection.ReflectionUtils;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.collectingAndThen;

/**
 * This class defines all the new static groovy methods which appear on normal
 * JDK classes inside the Groovy environment. Static methods are used with the
 * first parameter as the destination class.
 */
public class DefaultGroovyStaticMethods {

    /**
     * Start a Thread with the given closure as a Runnable instance.
     *
     * @param self    placeholder variable used by Groovy categories; ignored for default static methods
     * @param closure the Runnable closure
     * @return the started thread
     * @since 1.0
     */
    public static Thread start(Thread self, Closure closure) {
        return createThread(null, false, closure);
    }

    /**
     * Start a Thread with a given name and the given closure
     * as a Runnable instance.
     *
     * @param self    placeholder variable used by Groovy categories; ignored for default static methods
     * @param name    the name to give the thread
     * @param closure the Runnable closure
     * @return the started thread
     * @since 1.6
     */
    public static Thread start(Thread self, String name, Closure closure) {
        return createThread(name, false, closure);
    }

    /**
     * Start a daemon Thread with the given closure as a Runnable instance.
     *
     * @param self    placeholder variable used by Groovy categories; ignored for default static methods
     * @param closure the Runnable closure
     * @return the started thread
     * @since 1.0
     */
    public static Thread startDaemon(Thread self, Closure closure) {
        return createThread(null, true, closure);
    }

    /**
     * Start a daemon Thread with a given name and the given closure as
     * a Runnable instance.
     *
     * @param self    placeholder variable used by Groovy categories; ignored for default static methods
     * @param name    the name to give the thread
     * @param closure the Runnable closure
     * @return the started thread
     * @since 1.6
     */
    public static Thread startDaemon(Thread self, String name, Closure closure) {
        return createThread(name, true, closure);
    }

    private static Thread createThread(String name, boolean daemon, Closure closure) {
        Thread thread = name != null ? new Thread(closure, name) : new Thread(closure);
        if (daemon) thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Get the last hidden matcher that the system used to do a match.
     *
     * @param self placeholder variable used by Groovy categories; ignored for default static methods
     * @return the last regex matcher
     * @since 1.0
     */
    public static Matcher getLastMatcher(Matcher self) {
        return RegexSupport.getLastMatcher();
    }

    /**
     * This method is used by both sleep() methods to implement sleeping
     * for the given time even if interrupted
     *
     * @param millis  the number of milliseconds to sleep
     * @param closure optional closure called when interrupted
     *                as long as the closure returns false the sleep continues
     */
    private static void sleepImpl(long millis, Closure closure) {
        long start = System.currentTimeMillis();
        long rest = millis;
        long current;
        while (rest > 0) {
            try {
                Thread.sleep(rest);
                rest = 0;
            } catch (InterruptedException e) {
                if (closure != null) {
                    if (DefaultTypeTransformation.castToBoolean(closure.call(e))) {
                        return;
                    }
                }
                current = System.currentTimeMillis(); // compensate for closure's time
                rest = millis + start - current;
            }
        }
    }

    /**
     * Sleep for so many milliseconds, even if interrupted.
     *
     * @param self         placeholder variable used by Groovy categories; ignored for default static methods
     * @param milliseconds the number of milliseconds to sleep
     * @since 1.0
     */
    public static void sleep(Object self, long milliseconds) {
        sleepImpl(milliseconds, null);
    }

    /**
     * Sleep for so many milliseconds, using a given closure for interrupt processing.
     *
     * @param self         placeholder variable used by Groovy categories; ignored for default static methods
     * @param milliseconds the number of milliseconds to sleep
     * @param onInterrupt  interrupt handler, InterruptedException is passed to the Closure
     *                     as long as it returns false, the sleep continues
     * @since 1.0
     */
    public static void sleep(Object self, long milliseconds, Closure onInterrupt) {
        sleepImpl(milliseconds, onInterrupt);
    }

    @Deprecated
    public static Date parse(Date self, String format, String input) throws ParseException {
        return new SimpleDateFormat(format).parse(input);
    }

    @Deprecated
    public static Date parse(Date self, String format, String input, TimeZone zone) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(zone);
        return sdf.parse(input);
    }

    @Deprecated
    public static Date parseToStringDate(Date self, String dateToString) throws ParseException {
        return new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US).parse(dateToString);
    }

    /**
     * Works exactly like ResourceBundle.getBundle(String).  This is needed
     * because the java method depends on a particular stack configuration that
     * is not guaranteed in Groovy when calling the Java method.
     *
     * @param self       placeholder variable used by Groovy categories; ignored for default static methods
     * @param bundleName the name of the bundle.
     * @return the resource bundle
     * @see java.util.ResourceBundle#getBundle(java.lang.String)
     * @since 1.6.0
     */
    public static ResourceBundle getBundle(ResourceBundle self, String bundleName) {
        return getBundle(self, bundleName, Locale.getDefault());
    }

    /**
     * Works exactly like ResourceBundle.getBundle(String, Locale).  This is needed
     * because the java method depends on a particular stack configuration that
     * is not guaranteed in Groovy when calling the Java method.
     *
     * @param self       placeholder variable used by Groovy categories; ignored for default static methods
     * @param bundleName the name of the bundle.
     * @param locale     the specific locale
     * @return the resource bundle
     * @see java.util.ResourceBundle#getBundle(java.lang.String, java.util.Locale)
     * @since 1.6.0
     */
    public static ResourceBundle getBundle(ResourceBundle self, String bundleName, Locale locale) {
        Class c = ReflectionUtils.getCallingClass();
        ClassLoader targetCL = c != null ? c.getClassLoader() : null;
        if (targetCL == null) targetCL = ClassLoader.getSystemClassLoader();
        return ResourceBundle.getBundle(bundleName, locale, targetCL);
    }

    public static File createTempDir(File self) throws IOException {
        return createTempDir(self, "groovy-generated-", "-tmpdir");
    }

    public static File createTempDir(File self, final String prefix, final String suffix) throws IOException {
        final int MAXTRIES = 3;
        int accessDeniedCounter = 0;
        File tempFile=null;
        for (int i=0; i<MAXTRIES; i++) {
            try {
                tempFile = File.createTempFile(prefix, suffix);
                tempFile.delete();
                tempFile.mkdirs();
                break;
            } catch (IOException ioe) {
                if (ioe.getMessage().startsWith("Access is denied")) {
                    accessDeniedCounter++;
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                }
                if (i==MAXTRIES-1) {
                    if (accessDeniedCounter==MAXTRIES) {
                        String msg =
                                "Access is denied.\nWe tried " +
                                        + accessDeniedCounter+
                                        " times to create a temporary directory"+
                                        " and failed each time. If you are on Windows"+
                                        " you are possibly victim to"+
                                        " http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6325169. "+
                                        " this is no bug in Groovy.";
                        throw new IOException(msg);
                    } else {
                        throw ioe;
                    }
                }
            }
        }
        return tempFile;
    }

    /**
     * Get the current time in seconds
     *
     * @param self   placeholder variable used by Groovy categories; ignored for default static methods
     * @return  the difference, measured in seconds, between
     *          the current time and midnight, January 1, 1970 UTC.
     * @see     System#currentTimeMillis()
     */
    public static long currentTimeSeconds(System self){
    return System.currentTimeMillis() / 1000;
  }

    /**
     * Returns a {@link Collector} that gets the first element.
     *
     * @return a {@link Collector} which implements the first operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Optional<T>> first(Collectors self) {
        return Collectors.reducing((v1, v2) -> v1);
    }

    /**
     * Returns a {@link Collector} that gets the last element.
     *
     * @return a {@link Collector} which implements the last operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Optional<T>> last(Collectors self) {
        return Collectors.reducing((v1, v2) -> v2);
    }

    /**
     * Returns a {@link Collector} that calculates the count
     *
     * @return a {@link Collector} which implements the count operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Long> count(Collectors self) {
        return Collectors.counting();
    }

    /**
     * Returns a {@link Collector} that calculates the distinct count
     *
     * @return a {@link Collector} which implements the distinct count operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Long> countDistinct(Collectors self) {
        return countDistinctBy(self, t -> t);
    }

    /**
     * Returns a {@link Collector} that calculates the distinct count by function
     *
     * @return a {@link Collector} which implements the distinct count operation by function
     * @since 3.0.0
     */
    public static <T, U> Collector<T, ?, Long> countDistinctBy(Collectors self, Function<? super T, ? extends U> function) {
        return Collector.of(
                () -> new HashSet<U>(),
                (s, v) -> s.add(function.apply(v)),
                (s1, s2) -> {
                    s1.addAll(s2);
                    return s1;
                },
                s -> (long) s.size(),
                Collector.Characteristics.UNORDERED
        );
    }

    /**
     * Returns a {@link Collector} that calculates sum for any type of {@link Number}.
     *
     * @return a {@link Collector} which implements the sum operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Optional<T>> sum(Collectors self) {
        return sumBy(self, t -> t);
    }

    /**
     * Returns a {@link Collector} that calculates sum for any type of {@link Number} by function
     *
     * @return a {@link Collector} which implements the sum operation by function
     * @since 3.0.0
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, U> Collector<T, ?, Optional<U>> sumBy(Collectors self, Function<? super T, ? extends U> function) {
        return Collector.of(() -> (Sum<U>[]) new Sum[1],
                (s, v) -> {
                    if (s[0] == null)
                        s[0] = Sum.create(function.apply(v));
                    else
                        s[0].add(function.apply(v));
                },
                (s1, s2) -> {
                    s1[0].add(s2[0]);
                    return s1;
                },
                s -> s[0] == null ? Optional.empty() : Optional.of(s[0].result()),
                Collector.Characteristics.UNORDERED
        );
    }

    /**
     * Returns a {@link Collector} that calculates avg for any type of {@link Number}.
     *
     * @return a {@link Collector} which implements the avg operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Optional<T>> avg(Collectors self) {
        return avgBy(self, t -> t);
    }

    /**
     * Returns a {@link Collector} that calculates avg for any type of {@link Number} by function
     *
     * @return a {@link Collector} which implements the avg operation by function
     * @since 3.0.0
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, U> Collector<T, ?, Optional<U>> avgBy(Collectors self, Function<? super T, ? extends U> function) {
        return Collector.of(
                () -> (Sum<U>[]) new Sum[1],
                (s, v) -> {
                    if (s[0] == null)
                        s[0] = Sum.create(function.apply(v));
                    else
                        s[0].add(function.apply(v));
                },
                (s1, s2) -> {
                    s1[0].add(s2[0]);
                    return s1;
                },
                s -> s[0] == null ? Optional.empty() : Optional.of(s[0].avg()),
                Collector.Characteristics.UNORDERED
        );
    }

    /**
     * Returns a {@link Collector} that calculates min
     *
     * @return a {@link Collector} which implements the min operation
     * @since 3.0.0
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, Optional<T>> min(Collectors self) {
        return minBy(self, naturalOrder(), t -> t);
    }

    /**
     * Returns a {@link Collector} that calculates min by the order specified by the comparator
     *
     * @return a {@link Collector} which implements the min operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Optional<T>> min(Collectors self, Comparator<? super T> comparator) {
        return minBy(self, comparator, t -> t);
    }

    /**
     * Returns a {@link Collector} that calculates min by the function and the order specified by the comparator
     *
     * @return a {@link Collector} which implements the min operation
     * @since 3.0.0
     */
    public static <T, U> Collector<T, ?, Optional<T>> minBy(Collectors self, Comparator<? super U> comparator, Function<? super T, ? extends U> function) {
        return maxBy(self, comparator.reversed(), function);
    }

    /**
     * Returns a {@link Collector} that calculates max
     *
     * @return a {@link Collector} which implements the max operation
     * @since 3.0.0
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, Optional<T>> max(Collectors self) {
        return maxBy(self, naturalOrder(), t -> t);
    }

    /**
     * Returns a {@link Collector} that calculates max by the order specified by the comparator
     *
     * @return a {@link Collector} which implements the max operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Optional<T>> max(Collectors self, Comparator<? super T> comparator) {
        return maxBy(self, comparator, t -> t);
    }

    /**
     * Returns a {@link Collector} that calculates max by the function and the order specified by the comparator
     *
     * @return a {@link Collector} which implements the max operation
     * @since 3.0.0
     */
    public static <T, U> Collector<T, ?, Optional<T>> maxBy(Collectors self, Comparator<? super U> comparator, Function<? super T, ? extends U> function) {
        class Accumulator {
            T t;
            U u;
        }

        return Collector.of(
                () -> new Accumulator(),
                (a, t) -> {
                    U u = function.apply(t);

                    if (a.u == null || comparator.compare(a.u, u) < 0) {
                        a.t = t;
                        a.u = u;
                    }
                },
                (a1, a2) ->
                        a1.u == null
                                ? a2
                                : a2.u == null
                                ? a1
                                : comparator.compare(a1.u, a2.u) < 0
                                ? a2
                                : a1,
                a -> Optional.ofNullable(a.t)
        );
    }

    /**
     * Returns a {@link Collector} that calculates whether all match
     *
     * @return a {@link Collector} which implements the all match operation
     * @since 3.0.0
     */
    public static Collector<Boolean, ?, Boolean> allMatch(Collectors self) {
        return allMatchBy(self, t -> t);
    }

    /**
     * Returns a {@link Collector} that calculates whether all match
     *
     * @return a {@link Collector} which implements the all match operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Boolean> allMatchBy(Collectors self, Predicate<? super T> predicate) {
        return Collector.of(
                () -> new Boolean[1],
                (a, t) -> {
                    if (a[0] == null)
                        a[0] = predicate.test(t);
                    else
                        a[0] = a[0] && predicate.test(t);
                },
                (a1, a2) -> {
                    a1[0] = a1[0] && a2[0];
                    return a1;
                },
                a -> a[0] == null || a[0],
                Collector.Characteristics.UNORDERED
        );
    }

    /**
     * Returns a {@link Collector} that calculates whether any match
     *
     * @return a {@link Collector} which implements the any match operation
     * @since 3.0.0
     */
    public static Collector<Boolean, ?, Boolean> anyMatch(Collectors self) {
        return anyMatchBy(self, t -> t);
    }

    /**
     * Returns a {@link Collector} that calculates whether any match
     *
     * @return a {@link Collector} which implements the any match operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Boolean> anyMatchBy(Collectors self, Predicate<? super T> predicate) {
        return collectingAndThen(noneMatchBy(self, predicate), t -> !t);
    }

    /**
     * Returns a {@link Collector} that calculates whether none match
     *
     * @return a {@link Collector} which implements the none match operation
     * @since 3.0.0
     */
    public static Collector<Boolean, ?, Boolean> noneMatch(Collectors self) {
        return noneMatchBy(self, t -> t);
    }

    /**
     * Returns a {@link Collector} that calculates whether none match
     *
     * @return a {@link Collector} which implements the none match operation
     * @since 3.0.0
     */
    public static <T> Collector<T, ?, Boolean> noneMatchBy(Collectors self, Predicate<? super T> predicate) {
        return allMatchBy(self, predicate.negate());
    }


    private static abstract class Sum<N> {
        long count;

        void add(Sum<N> sum) {
            add0(sum.result());
            count += sum.count;
        }

        void add(N value) {
            add0(value);
            count += 1;
        }

        void and(Sum<N> sum) {
            and0(sum.result());
        }

        void and(N value) {
            and0(value);
        }

        void or(Sum<N> sum) {
            or0(sum.result());
        }

        void or(N value) {
            or0(value);
        }

        abstract void add0(N value);

        abstract void and0(N value);

        abstract void or0(N value);

        abstract N result();

        abstract N avg();

        @SuppressWarnings({"unchecked", "rawtypes"})
        static <N> Sum<N> create(N value) {
            Sum<N> result;

            if (value instanceof Byte)
                result = (Sum) new OfByte();
            else if (value instanceof Short)
                result = (Sum) new OfShort();
            else if (value instanceof Integer)
                result = (Sum) new OfInt();
            else if (value instanceof Long)
                result = (Sum) new OfLong();
            else if (value instanceof Float)
                result = (Sum) new OfFloat();
            else if (value instanceof Double)
                result = (Sum) new OfDouble();
            else if (value instanceof BigInteger)
                result = (Sum) new OfBigInteger();
            else if (value instanceof BigDecimal)
                result = (Sum) new OfBigDecimal();
            else
                throw new IllegalArgumentException("Cannot calculate sums for value : " + value);

            result.add(value);
            return result;
        }

        static class OfByte extends Sum<Byte> {
            byte result;

            @Override
            void add0(Byte value) {
                result += value;
            }

            @Override
            Byte result() {
                return result;
            }

            @Override
            void and0(Byte value) {
                result &= value;
            }

            @Override
            void or0(Byte value) {
                result |= value;
            }

            @Override
            Byte avg() {
                return (byte) (result / count);
            }
        }

        static class OfShort extends Sum<Short> {
            short sum;

            @Override
            void add0(Short value) {
                sum += value;
            }

            @Override
            void and0(Short value) {
                sum &= value;
            }

            @Override
            void or0(Short value) {
                sum |= value;
            }

            @Override
            Short result() {
                return sum;
            }

            @Override
            Short avg() {
                return (short) (sum / count);
            }
        }

        static class OfInt extends Sum<Integer> {
            int sum;

            @Override
            void add0(Integer value) {
                sum += value;
            }

            @Override
            void and0(Integer value) {
                sum &= value;
            }

            @Override
            void or0(Integer value) {
                sum |= value;
            }

            @Override
            Integer result() {
                return sum;
            }

            @Override
            Integer avg() {
                return (int) (sum / count);
            }
        }

        static class OfLong extends Sum<Long> {
            long sum;

            @Override
            void add0(Long value) {
                sum += value;
            }

            @Override
            void and0(Long value) {
                sum &= value;
            }

            @Override
            void or0(Long value) {
                sum |= value;
            }

            @Override
            Long result() {
                return sum;
            }

            @Override
            Long avg() {
                return sum / count;
            }
        }

        static class OfFloat extends Sum<Float> {
            float sum;

            @Override
            void add0(Float value) {
                sum += value;
            }

            @Override
            void and0(Float value) {
                throw new UnsupportedOperationException();
            }

            @Override
            void or0(Float value) {
                throw new UnsupportedOperationException();
            }

            @Override
            Float result() {
                return sum;
            }

            @Override
            Float avg() {
                return sum / (float) count;
            }
        }

        static class OfDouble extends Sum<Double> {
            double sum;

            @Override
            void add0(Double value) {
                sum += value;
            }

            @Override
            void and0(Double value) {
                throw new UnsupportedOperationException();
            }

            @Override
            void or0(Double value) {
                throw new UnsupportedOperationException();
            }

            @Override
            Double result() {
                return sum;
            }

            @Override
            Double avg() {
                return sum / (double) count;
            }
        }

        static class OfBigInteger extends Sum<BigInteger> {
            BigInteger sum = BigInteger.ZERO;

            @Override
            void add0(BigInteger value) {
                sum = sum.add(value);
            }

            @Override
            void and0(BigInteger value) {
                throw new UnsupportedOperationException();
            }

            @Override
            void or0(BigInteger value) {
                throw new UnsupportedOperationException();
            }

            @Override
            BigInteger result() {
                return sum;
            }

            @Override
            BigInteger avg() {
                return sum.divide(BigInteger.valueOf(count));
            }
        }

        static class OfBigDecimal extends Sum<BigDecimal> {
            BigDecimal sum = BigDecimal.ZERO;

            @Override
            void add0(BigDecimal value) {
                sum = sum.add(value);
            }

            @Override
            void and0(BigDecimal value) {
                throw new UnsupportedOperationException();
            }

            @Override
            void or0(BigDecimal value) {
                throw new UnsupportedOperationException();
            }

            @Override
            BigDecimal result() {
                return sum;
            }

            @Override
            BigDecimal avg() {
                return sum.divide(BigDecimal.valueOf(count), RoundingMode.HALF_EVEN);
            }
        }
    }
}
