/* Copyright 2002-2025 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** String → Object mapping, for small number of keys.
 * <p>
 * This class is a low overhead for a very small number of keys.
 * It is based on simple array and string comparison. It plays
 * the same role a {@code Map<String, Object>} but with reduced
 * features and not intended for large number of keys. For such
 * needs the regular {@code Map<String, Object>} should be preferred.
 * </p>
 *
 * @see FieldArrayDictionary
 * @author Bryan Cazabonne
 * @since 13.0
 */
public class FieldDataDictionary<T extends CalculusFieldElement<T>> {

    /** Default capacity. */
    private static final int DEFAULT_INITIAL_CAPACITY = 4;

    /** Field to which elements belong. */
    private final Field<T> field;

    /** Data container. */
    private final List<Entry> data;

    /** Constructor with {@link #DEFAULT_INITIAL_CAPACITY default initial capacity}.
     * @param field field to which elements belong
     */
    public FieldDataDictionary(final Field<T> field) {
        this(field, DEFAULT_INITIAL_CAPACITY);
    }

    /** Constructor from a map.
     * @param field field to which the elements belong
     * @param map map to use for initializing entries
     */
    public FieldDataDictionary(final Field<T> field, final Map<String, Object> map) {
        this(field, map.size());
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            // we don't call put(key, value) to avoid the overhead of the unneeded call to remove(key)
            data.add(new Entry(entry.getKey(), entry.getValue()));
        }
    }

    /** Constructor from another dictionary.
     * @param dictionary dictionary to use for initializing entries
     */
    public FieldDataDictionary(final FieldDataDictionary<T> dictionary) {
        // take care to call dictionary.getData() and not use dictionary.data
        this(dictionary.getField(), DEFAULT_INITIAL_CAPACITY + dictionary.getData().size());
        for (final Entry entry : dictionary.getData()) {
            // we don't call put(key, value) to avoid the overhead of the unneeded call to remove(key)
            data.add(new Entry(entry.getKey(), entry.getValue()));
        }
    }

    /** Constructor with specified capacity.
     * @param field field to which elements belong
     * @param initialCapacity initial capacity
     */
    public FieldDataDictionary(final Field<T> field, final int initialCapacity) {
        this.data = new ArrayList<>(initialCapacity);
        this.field = field;
    }

    /** Creates a "field" values dictionary.
     * <p>
     * Creates a DoubleArrayDictionary with all double[] values
     * contained in the instance.
     * </p>
     * @return a double values dictionary
     */
    @SuppressWarnings("unchecked") // cast including generic type is checked and unitary tested
    public FieldArrayDictionary<T> toFieldArrayDictionary() {
        final FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        for (final Entry entry : data) {
            if (entry.getValue() instanceof CalculusFieldElement[]) {
                dictionary.put(entry.getKey(), (T[]) entry.getValue());
            }
        }
        return dictionary;
    }

    /** Create a map from the instance.
     * <p>
     * The map contains a copy of the instance data
     * </p>
     * @return copy of the dictionary, as an independent map
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new HashMap<>(data.size());
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

    /** Get the number of dictionary entries.
     * @return number of dictionary entries
     */
    public int size() {
        return data.size();
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
    public void put(final String key, final Object value) {
        remove(key);
        data.add(new Entry(key, value));
    }

    /** Put all the T[] entries from the map in the dictionary.
     * @param map map to copy into the instance
     */
    public void putAllFields(final Map<String, T[]> map) {
        for (final Map.Entry<String,  T[]> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }


    /** Put all the entries from the map in the dictionary.
     * @param map map to copy into the instance
     */
    public void putAll(final Map<String, Object> map) {
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /** Put all the entries from another dictionary.
     * @param dictionary dictionary to copy into the instance
     */
    public void putAll(final FieldDataDictionary<T> dictionary) {
        for (final FieldDataDictionary<T>.Entry entry : dictionary.data) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /** Get the value corresponding to a key.
     * @param key entry key
     * @return copy of the value corresponding to the key or null if key not present
     */
    public Object get(final String key) {
        final FieldDataDictionary<T>.Entry entry = getEntry(key);
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

    /**
     * Get an unmodifiable view of the dictionary entries.
     *
     * @return unmodifiable view of the dictionary entries
     */
    public List<Entry> getData() {
        return Collections.unmodifiableList(data);
    }

    /**
     * Get the field to which elements belong.
     * @return the field to which elements belong
     */
    public Field<T> getField() {
        return field;
    }

    /** Get a string representation of the dictionary.
     * <p>
     * This string representation is intended for improving displays in debuggers only.
     * </p>
     * @return string representation of the dictionary
     */
    @Override
    public String toString() {
        return DataDictionary.toString(toMap());
    }

    /** Entry in a dictionary. */
    public class Entry {

        /** Key. */
        private final String key;

        /** Value. */
        private final Object value;

        /** Simple constructor.
         * @param key key
         * @param value value
         */
        Entry(final String key, final Object value) {
            this.key   = key;
            this.value = value;
        }

        /** Get the entry key.
         * @return entry key
         */
        public String getKey() {
            return key;
        }

        /** Get the value.
         * @return a copy of the value (independent from internal array if it is a double array)
         */
        @SuppressWarnings("unchecked")
        public Object getValue() {
            return value instanceof CalculusFieldElement[] ? ((T[]) value).clone() : value;
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
         */
        @SuppressWarnings("unchecked")
        public void scaledIncrement(final double factor, final FieldArrayDictionary<T>.Entry raw) {
            if (value instanceof CalculusFieldElement[]) {
                for (int i = 0; i < raw.getValue().length; ++i) {
                    ((T[]) value)[i] = ((T[]) value)[i].add(raw.getValue()[i].multiply(factor));
                }
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
         */
        @SuppressWarnings("unchecked")
        public void scaledIncrement(final T factor, final FieldArrayDictionary<T>.Entry raw) {
            if (value instanceof CalculusFieldElement[]) {
                for (int i = 0; i < raw.getValue().length; ++i) {
                    ((T[]) value)[i] = ((T[]) value)[i].add(raw.getValue()[i].multiply(factor));
                }
            }
        }

    }

}
