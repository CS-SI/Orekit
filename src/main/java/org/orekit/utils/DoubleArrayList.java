/* Contributed in the public domain.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.stream.DoubleStream;

/**
 * Similar to {@link ArrayList}, but for {@code double} values.
 *
 * @author Evan M. Ward
 * @see DoubleList
 * @since 14.0
 */
public class DoubleArrayList implements DoubleList {

    /**
     * A sublist of a {@link DoubleList}.
     *
     * @author Evan M. Ward
     */
    private static class SubList implements DoubleList {

        /** Holder of the backing array. Used for efficient {@link #toArray()}. */
        private final DoubleArrayList root;
        /** the parent list. */
        private final DoubleList parent;
        /** the offset to this list's index 0 in {@link #root}. */
        private final int offsetInRoot;
        /** the offset of this lists' index 0 in {@link #parent}. */
        private final int offsetInParent;
        /** this length of this list. */
        private int size;

        /**
         * Create a sub list view.
         *
         * @param parent the list it was created from
         * @param from   from this index in the parent
         * @param to     to this index in the parent
         */
        private SubList(final DoubleArrayList parent,
                        final int from,
                        final int to) {
            this.parent = parent;
            this.root = parent;
            this.offsetInRoot = from;
            this.offsetInParent = from;
            this.size = to - from;
        }

        /**
         * Create a sub list view.
         *
         * @param parent the list it was created from
         * @param from   from this index in the parent
         * @param to     to this index in the parent
         */
        private SubList(final SubList parent,
                        final int from,
                        final int to) {
            this.parent = parent;
            this.offsetInParent = from;
            this.size = to - from;
            this.root = parent.root;
            this.offsetInRoot = parent.offsetInRoot + from;
        }

        @Override
        public int size() {
            return this.size;
        }

        @Override
        public boolean isEmpty() {
            return this.size == 0;
        }

        @Override
        public double[] toArray() {
            return Arrays.copyOfRange(this.root.data, this.offsetInRoot,
                    this.offsetInRoot + this.size);
        }

        @Override
        public DoubleStream stream() {
            return Arrays.stream(this.root.data, this.offsetInRoot,
                    this.offsetInRoot + this.size);
        }

        @Override
        public void add(final double d) {
            this.parent.add(this.offsetInParent + this.size, d);
            this.size = this.size + 1;
        }

        @Override
        public void add(final int index, final double element) {
            final int newSize = this.size + 1;
            rangeCheck(index, newSize);
            this.parent.add(this.offsetInParent + index, element);
            this.size = newSize;
        }

        @Override
        public void addAll(final DoubleList d) {
            this.parent.addAll(this.offsetInParent + this.size, d);
            this.size = this.size + d.size();
        }

        @Override
        public void addAll(final int index, final DoubleList d) {
            final int newSize = this.size + d.size();
            rangeCheck(index, newSize);
            this.parent.addAll(this.offsetInParent + index, d);
            this.size = newSize;
        }

        @Override
        public void clear() {
            this.parent.removeRange(this.offsetInParent,
                    this.offsetInParent + this.size);
            this.size = 0;
        }

        @Override
        public double remove(final int index) {
            rangeCheck(index, this.size);
            final double old = this.parent.remove(this.offsetInParent + index);
            this.size = this.size - 1;
            return old;
        }

        @Override
        public void removeRange(final int fromIndex, final int toIndex) {
            // check parameters
            rangeCheck(fromIndex, toIndex, this.size);
            // remove in parent
            this.parent.removeRange(this.offsetInParent + fromIndex,
                    this.offsetInParent + toIndex);
            // adjust size
            this.size = this.size - toIndex + fromIndex;
        }

        @Override
        public double get(final int index) {
            rangeCheck(index, this.size);
            return this.parent.get(this.offsetInParent + index);
        }

        @Override
        public double getFirst() {
            emptyCheck(this);
            return this.parent.get(this.offsetInParent);
        }

        @Override
        public double getLast() {
            emptyCheck(this);
            return this.parent.get(this.offsetInParent + this.size - 1);
        }

        @Override
        public double set(final int index, final double element) {
            rangeCheck(index, this.size);
            return this.parent.set(this.offsetInParent + index, element);
        }

        @Override
        public DoubleList subList(final int fromIndex, final int toIndex) {
            rangeCheck(fromIndex, toIndex, this.size);
            return new SubList(this, fromIndex, toIndex);
        }

        @Override
        public boolean equals(final Object obj) {
            return DoubleList.equals(this, obj);
        }

        @Override
        public int hashCode() {
            return DoubleList.hashCode(this);
        }

        @Override
        public String toString() {
            return DoubleList.toString(this);
        }
    }

    /** The storage array. */
    private double[] data;
    /** The current size of the list. */
    private int size;

    /** Default constructor. An empty list. */
    public DoubleArrayList() {
        this(5);
    }

    /**
     * Create an internal storage array with the given capacity.
     *
     * @param incapacityCapacity of the list.
     */
    public DoubleArrayList(final int incapacityCapacity) {
        this.data = new double[incapacityCapacity];
        this.size = 0;
    }

    /**
     * Copy constructor. Creates an independent list with the same elements as
     * {@code list}.
     *
     * @param list to copy.
     */
    public DoubleArrayList(final DoubleList list) {
        this.data = list.toArray();
        this.size = this.data.length;
    }


    /**
     * Wrapper constructor. This list will be backed by {@code data}.
     *
     * @param data to wrap.
     */
    private DoubleArrayList(final double[] data) {
        this.data = data;
        this.size = data.length;
    }

    /**
     * Create a copy of an array.
     *
     * @param array the values
     * @return a {@link DoubleArrayList} initialized to contain {@code array}.
     * The array is copied so the instances are independent.
     */
    public static DoubleArrayList of(final double... array) {
        return new DoubleArrayList(array.clone());
    }

    /**
     * Wraps the given array in a list. The returned list is backed by the given
     * {@code array}.
     *
     * @param array to wrap.
     * @return a list backed by the given {@code array}.
     * @see Arrays#asList(Object[])
     */
    public static DoubleArrayList wrap(final double... array) {
        return new DoubleArrayList(array);
    }

    /**
     * Create an empty list.
     *
     * @return an empty {@link DoubleArrayList}.
     */
    public static DoubleArrayList emptyList() {
        return new DoubleArrayList(new double[0]);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public double[] toArray() {
        return Arrays.copyOf(this.data, this.size);
    }

    public DoubleStream stream() {
        return Arrays.stream(data, 0, size);
    }

    @Override
    public void add(final double d) {
        ensureCapacity(this.size + 1);
        this.data[this.size] = d;
        this.size = this.size + 1;
    }

    @Override
    public void add(final int index, final double element) {
        // range check
        if (index > this.size) {
            throw new IndexOutOfBoundsException("" + index);
        }

        ensureCapacity(this.size + 1);
        // shift elements up by one
        System.arraycopy(this.data, index, this.data, index + 1, size - index);
        this.data[index] = element;
        this.size = this.size + 1;
    }

    @Override
    public void addAll(final DoubleList d) {
        addAll(this.size, d);
    }

    @Override
    public void addAll(final int index, final DoubleList d) {
        rangeCheck(index, size + 1);
        final int dSize = d.size();
        final int newSize = this.size + dSize;
        ensureCapacity(newSize);
        // shift elements up by length
        System.arraycopy(this.data, index, this.data, index + dSize,
                size - index);

        // d converted to an array
        final double[] dArray;
        if (d instanceof DoubleArrayList) {
            dArray = ((DoubleArrayList) d).data;
        } else {
            dArray = d.toArray();
        }
        // copy in new array
        System.arraycopy(dArray, 0, this.data, index, dSize);
        this.size = newSize;
    }

    @Override
    public void clear() {
        this.size = 0;
    }

    @Override
    public double remove(final int index) {
        rangeCheck(index, this.size);
        final double old = this.data[index];
        System.arraycopy(this.data, index + 1, this.data, index,
                this.size - index - 1);
        this.size = this.size - 1;
        return old;
    }

    @Override
    public void removeRange(final int fromIndex, final int toIndex) {
        // check indices
        rangeCheck(fromIndex, toIndex, this.size);
        // shift elements down by length
        System.arraycopy(this.data, toIndex, this.data, fromIndex,
                this.size - toIndex);
        // adjust length
        this.size = this.size - toIndex + fromIndex;
    }

    @Override
    public double get(final int index) {
        rangeCheck(index, this.size);
        return this.data[index];
    }

    @Override
    public double getFirst() {
        emptyCheck(this);
        return this.data[0];
    }

    @Override
    public double getLast() {
        emptyCheck(this);
        return this.data[this.size - 1];
    }

    @Override
    public double set(final int index, final double element) {
        rangeCheck(index, this.size);
        final double old = this.data[index];
        this.data[index] = element;
        return old;
    }

    @Override
    public DoubleList subList(final int fromIndex, final int toIndex) {
        rangeCheck(fromIndex, toIndex, this.size);
        return new SubList(this, fromIndex, toIndex);
    }

    @Override
    public boolean equals(final Object obj) {
        return DoubleList.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return DoubleList.hashCode(this);
    }

    @Override
    public String toString() {
        return DoubleList.toString(this);
    }

    /**
     * throw an exception if {@code index} is not a valid index to an array of
     * length {@code size}.
     *
     * @param index into array.
     * @param size  of array.
     */
    private static void rangeCheck(final int index, final int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("" + index);
        }
    }

    /**
     * throw an exception {@code from} or {@code to} are not valid indices for
     * an array of the given {@code size}.
     *
     * @param from the "from" index, inclusive
     * @param to   the to "index", exclusive
     * @param size the size of the array
     */
    private static void rangeCheck(final int from,
                                   final int to,
                                   final int size) {
        if (from < 0) {
            throw new IndexOutOfBoundsException("from: " + from);
        }
        if (to > size) {
            throw new IndexOutOfBoundsException("to: " + to);
        }
        if (from > to) {
            throw new IndexOutOfBoundsException(from + ">" + to);
        }
    }

    /**
     * Check if the list has at least one element.
     *
     * @param list to check
     */
    private static void emptyCheck(final DoubleList list) {
        if (list.isEmpty()) {
            throw new NoSuchElementException("Empty list");
        }
    }

    /**
     * Check that {@code data.length >= newSize}.
     *
     * @param newSize to ensure.
     */
    private void ensureCapacity(final int newSize) {
        if (newSize > this.data.length) {
            this.data = Arrays.copyOf(this.data, newSize * 2);
        } // else do nothing
    }

}
