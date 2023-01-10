/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** String → double[] mapping, for small number of keys.
 * <p>
 * This class is a low overhead for a very small number of keys.
 * It is based on simple array and string comparison. It plays
 * the same role a {@code Map<String, double[]>} but with reduced
 * features and not intended for large number of keys. For such
 * needs the regular {@code Map<String, double[]>} should be preferred.
 * </p>
 * @since 11.1
 */
public class DoubleArrayDictionary implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20211121L;

    /** Default capacity. */
    private static final int DEFAULT_INITIAL_CAPACITY = 4;

    /** Data container. */
    private final List<Entry> data;

    /** Constructor with {@link #DEFAULT_INITIAL_CAPACITY default initial capacity}.
     */
    public DoubleArrayDictionary() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /** Constructor with specified capacity.
     * @param initialCapacity initial capacity
     */
    public DoubleArrayDictionary(final int initialCapacity) {
        data = new ArrayList<>(initialCapacity);
    }

    /** Constructor from another dictionary.
     * @param dictionary dictionary to use for initializing entries
     */
    public DoubleArrayDictionary(final DoubleArrayDictionary dictionary) {
        // take care to call dictionary.getData() and not use dictionary.data,
        // otherwise we get an empty dictionary when using a DoubleArrayDictionary.view
        this(DEFAULT_INITIAL_CAPACITY + dictionary.getData().size());
        for (final Entry entry : dictionary.getData()) {
            // we don't call put(key, value) to avoid the overhead of the unneeded call to remove(key)
            data.add(new Entry(entry.getKey(), entry.getValue()));
        }
    }

    /** Constructor from a map.
     * @param map map to use for initializing entries
     */
    public DoubleArrayDictionary(final Map<String, double[]> map) {
        this(map.size());
        for (final Map.Entry<String, double[]> entry : map.entrySet()) {
            // we don't call put(key, value) to avoid the overhead of the unneeded call to remove(key)
            data.add(new Entry(entry.getKey(), entry.getValue()));
        }
    }

    /** Get an unmodifiable view of the dictionary entries.
     * @return unmodifiable view of the dictionary entries
     */
    public List<Entry> getData() {
        return Collections.unmodifiableList(data);
    }

    /** Get the number of dictionary entries.
     * @return number of dictionary entries
     */
    public int size() {
        return data.size();
    }

    /** Create a map from the instance.
     * <p>
     * The map contains a copy of the instance data
     * </p>
     * @return copy of the dictionary, as an independent map
     */
    public Map<String, double[]> toMap() {
        final Map<String, double[]> map = new HashMap<>(data.size());
        for (final Entry entry : data) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    /** Remove all entries.
     */
    public void clear() {
        data.clear();
    }

    /** Add an entry.
     * <p>
     * If an entry with the same key already exists, it will be removed first.
     * </p>
     * <p>
     * The new entry is always put at the end.
     * </p>
     * @param key entry key
     * @param value entry value
     */
    public void put(final String key, final double[] value) {
        remove(key);
        data.add(new Entry(key, value));
    }

    /** Put all the entries from the map in the dictionary.
     * @param map map to copy into the instance
     */
    public void putAll(final Map<String, double[]> map) {
        for (final Map.Entry<String, double[]> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /** Put all the entries from another dictionary.
     * @param dictionary dictionary to copy into the instance
     */
    public void putAll(final DoubleArrayDictionary dictionary) {
        for (final Entry entry : dictionary.data) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /** Get the value corresponding to a key.
     * @param key entry key
     * @return copy of the value corresponding to the key or null if key not present
     */
    public double[] get(final String key) {
        final Entry entry = getEntry(key);
        return entry == null ? null : entry.getValue();
    }

    /** Get a complete entry.
     * @param key entry key
     * @return entry with key if it exists, null otherwise
     */
    public Entry getEntry(final String key) {
        for (final Entry entry : data) {
            if (entry.getKey().equals(key)) {
                return entry;
            }
        }
        return null;
    }

    /** Remove an entry.
     * @param key key of the entry to remove
     * @return true if an entry has been removed, false if the key was not present
     */
    public boolean remove(final String key) {
        final Iterator<Entry> iterator = data.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getKey().equals(key)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    /** Get an unmodifiable view of the dictionary.
     * <p>
     * The return dictionary is backed by the original instance and offers {@code read-only}
     * access to it, but all operations that modify it throw an {@link UnsupportedOperationException}.
     * </p>
     * @return unmodifiable view of the dictionary
     */
    public DoubleArrayDictionary unmodifiableView() {
        return new View();
    }

    /** Get a string representation of the dictionary.
     * <p>
     * This string representation is intended for improving displays in debuggers only.
     * </p>
     * @return string representation of the dictionary
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append('{');
        for (int i = 0; i < data.size(); ++i) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(data.get(i).getKey());
            builder.append('[');
            builder.append(data.get(i).getValue().length);
            builder.append(']');
        }
        builder.append('}');
        return builder.toString();
    }

    /** Entry in a dictionary. */
    public static class Entry implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20211121L;

        /** Key. */
        private final String key;

        /** Value. */
        private final double[] value;

        /** Simple constructor.
         * @param key key
         * @param value value
         */
        Entry(final String key, final double[] value) {
            this.key   = key;
            this.value = value.clone();
        }

        /** Get the entry key.
         * @return entry key
         */
        public String getKey() {
            return key;
        }

        /** Get the value.
         * @return a copy of the value (independent from internal array)
         */
        public double[] getValue() {
            return value.clone();
        }

        /** Get the size of the value array.
         * @return size of the value array
         */
        public int size() {
            return value.length;
        }

        /** Increment the value.
         * <p>
         * For the sake of performance, no checks are done on argument.
         * </p>
         * @param increment increment to apply to the entry value
         */
        public void increment(final double[] increment) {
            for (int i = 0; i < increment.length; ++i) {
                value[i] += increment[i];
            }
        }

        /** Increment the value with another scaled entry.
         * <p>
         * Each component {@code value[i]} will be replaced by {@code value[i] + factor * raw.value[i]}.
         * </p>
         * <p>
         * For the sake of performance, no checks are done on arguments.
         * </p>
         * @param factor multiplicative factor for increment
         * @param raw raw increment to be multiplied by {@code factor} and then added
         * @since 11.1.1
         */
        public void scaledIncrement(final double factor, final Entry raw) {
            for (int i = 0; i < raw.value.length; ++i) {
                value[i] += factor * raw.value[i];
            }
        }

        /** Reset the value to zero.
         */
        public void zero() {
            Arrays.fill(value, 0.0);
        }

    }

    /** Unmodifiable view of the dictionary. */
    private class View extends DoubleArrayDictionary {

        /** {@link Serializable} UID. */
        private static final long serialVersionUID = 20211121L;

        /**  {@inheritDoc} */
        @Override
        public List<Entry> getData() {
            return DoubleArrayDictionary.this.getData();
        }

        /**  {@inheritDoc} */
        @Override
        public int size() {
            return DoubleArrayDictionary.this.size();
        }

        /**  {@inheritDoc} */
        @Override
        public Map<String, double[]> toMap() {
            return DoubleArrayDictionary.this.toMap();
        }

        /**  {@inheritDoc} */
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        /**  {@inheritDoc} */
        @Override
        public void put(final String key, final double[] value) {
            throw new UnsupportedOperationException();
        }

        /**  {@inheritDoc} */
        @Override
        public void putAll(final Map<String, double[]> map) {
            throw new UnsupportedOperationException();
        }

        /**  {@inheritDoc} */
        @Override
        public void putAll(final DoubleArrayDictionary dictionary) {
            throw new UnsupportedOperationException();
        }

        /**  {@inheritDoc} */
        @Override
        public double[] get(final String key) {
            return DoubleArrayDictionary.this.get(key);
        }

        /**  {@inheritDoc} */
        @Override
        public Entry getEntry(final String key) {
            return DoubleArrayDictionary.this.getEntry(key);
        }

        /**  {@inheritDoc} */
        @Override
        public boolean remove(final String key) {
            throw new UnsupportedOperationException();
        }

    }

}
