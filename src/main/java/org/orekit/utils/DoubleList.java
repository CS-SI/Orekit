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

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.DoubleStream;

/**
 * An interface similar to {@link List} that operates with {@code double} values
 * without auto-boxing.
 *
 * @author Evan M. Ward
 * @see List
 * @see java.nio.DoubleBuffer
 * @see DoubleArrayList#of(double...)
 * @see DoubleArrayList#wrap(double...)
 * @since 14.0
 */
public interface DoubleList {

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     * @see List#size()
     */
    int size();

    /**
     * Returns whether this list is empty. A list is empty if its
     * {@link #size()} is zero.
     *
     * @return {@code size() == 0}
     * @see List#isEmpty()
     */
    boolean isEmpty();

    /**
     * Returns an array containing all the elements.
     *
     * <p>The returned array is a copy of this list in that subsequent
     * modifications of the array do not modify this list and vice versa.
     *
     * @return copy of this list as an array.
     * @see List#toArray()
     */
    double[] toArray();

    /**
     * Stream the elements in this list.
     *
     * @return stream of this list.
     * @see List#stream()
     */
    DoubleStream stream();

    /**
     * Adds the specified element to the end of this list.
     *
     * @param d element to be appended to this list
     * @throws UnsupportedOperationException if the <tt>add</tt> operation is
     *                                       not supported by this list
     * @see List#add(Object)
     */
    void add(double d);

    /**
     * Inserts the given element at the given position in this list. Following
     * elements are shifted to the right.
     *
     * @param index   index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws UnsupportedOperationException if the <tt>add</tt> operation is
     *                                       not supported by this list
     * @throws IndexOutOfBoundsException     if the index is out of range
     * @see List#add(int, Object)
     */
    void add(int index, double element);

    /**
     * Appends all the given elements to the end of this list.
     *
     * <p>Passing {@code this} or a {@link #subList(int, int) sublist} of this
     * list as {@code d} results in undefined behavior.
     *
     * @param d elements to be added to this list
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation is
     *                                       not supported by this list
     * @see #add(double)
     * @see List#addAll(Collection)
     */
    void addAll(DoubleList d);

    /**
     * Inserts all the given elements at the given position in this list.
     *
     * <p>Passing {@code this} or a {@link #subList(int, int) sublist} of this
     * list as {@code d} results in undefined behavior.
     *
     * @param index index at which to insert the first element
     * @param d     elements to be added to this list
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation is
     *                                       not supported by this list
     * @throws IndexOutOfBoundsException     if the index is out of range
     * @see List#addAll(int, Collection)
     */
    void addAll(int index, DoubleList d);

    /**
     * Removes all the elements from this list.
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation is
     *                                       not supported by this list
     * @see List#clear()
     */
    void clear();

    /**
     * Remove the element at the given index. Following elements shift left.
     *
     * @param index to remove.
     * @return removed element.
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation is
     *                                       not supported by this list
     * @throws IndexOutOfBoundsException     if the index is out of range
     * @see List#remove(int)
     */
    double remove(int index);

    /**
     * Remove elements selected by the given indices.
     *
     * @param fromIndex inclusive.
     * @param toIndex   exclusive.
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation is
     *                                       not supported by this list
     * @throws IndexOutOfBoundsException     if {@code fromIndex} or
     *                                       {@code toIndex} is out of range
     */
    void removeRange(int fromIndex, int toIndex);

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range (
     *                                   <tt>index &lt; 0 || index &gt;=
     *                                   size()</tt>)
     * @see List#get(int)
     */
    double get(int index);

    /**
     * Get the first element in the list.
     *
     * @return first element.
     * @throws NoSuchElementException if this list is empty.
     * @see List#getFirst()
     */
    double getFirst();

    /**
     * Get the last element in the list.
     *
     * @return last element.
     * @throws NoSuchElementException if this list is empty.
     * @see List#getLast()
     */
    double getLast();

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @param index   of the element to replace
     * @param element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws UnsupportedOperationException if the <tt>set</tt> operation is
     *                                       not supported by this list
     * @throws IndexOutOfBoundsException     if the index is out of range (
     *                                       <tt>index &lt; 0 || index &gt;=
     *                                       size()</tt>)
     * @see List#set(int, Object)
     */
    double set(int index, double element);

    /**
     * Returns a view of the portion of this list between the specified
     * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive. The
     * returned list is backed by this list, so non-structural changes in the
     * returned list are reflected in this list, and vice versa. The returned
     * list supports all the optional list operations supported by this list.
     *
     * <p>The semantics of the list returned by this method become undefined if
     * the backing list (i.e., this list) is <i>structurally modified</i> in any
     * way other than via the returned list. (Structural modifications are those
     * that change the size of this list, or otherwise perturb it in such a
     * fashion that iterations in progress may yield incorrect results.)
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex   high endpoint (exclusive) of the subList
     * @return a view of the specified range within this list
     * @throws IndexOutOfBoundsException for an illegal endpoint index value (
     *                                   <tt>fromIndex &lt; 0 || toIndex &gt;
     *                                   size || fromIndex &gt; toIndex</tt>)
     * @see List#subList(int, int)
     */
    DoubleList subList(int fromIndex, int toIndex);

    /**
     * {@inheritDoc}
     *
     * <p>Returns <tt>true</tt> if and only if the specified object is also a
     * list, both lists have the same size, and all corresponding pairs of
     * elements in the two lists are <i>equal</i>. In other words, two lists are
     * defined to be equal if they contain the same elements in the same order.
     * This definition ensures that the equals method works properly across
     * different implementations of the {@link DoubleList} interface.
     *
     * @param o the object to be compared for equality with this list
     * @return <tt>true</tt> if the specified object is equal to this list
     * @see List#equals(Object)
     * @see #equals(DoubleList, Object)
     */
    @Override
    boolean equals(Object o);

    /**
     * {@inheritDoc}
     *
     * <p>Returns the hash code value for this list. The hash code of a list is
     * defined to be the result of the following calculation:
     * <p/>
     * <pre>
     * int hashCode = 1;
     * for (double e : list)
     *     hashCode = 31 * hashCode + Double.hashcode(e);
     * </pre>
     * <p/>
     * This ensures that <tt>list1.equals(list2)</tt> implies that
     * <tt>list1.hashCode()==list2.hashCode()</tt> for any two lists,
     * <tt>list1</tt> and
     * <tt>list2</tt>, as required by the general contract of
     * {@link Object#hashCode}.
     *
     * @return the hash code value for this list
     * @see #equals(Object)
     * @see List#hashCode()
     * @see #hashCode(DoubleList)
     */
    @Override
    int hashCode();

    /* Utility methods */

    /**
     * Equality check according to {@link #equals(Object)}.
     *
     * @param self first object of comparison.
     * @param obj  second object of comparison.
     * @return if self equals obj according to
     * {@link DoubleList#equals(Object)}.
     */
    static boolean equals(final DoubleList self, final Object obj) {
        if (obj instanceof DoubleList) {
            final DoubleList other = (DoubleList) obj;
            // check size
            if (self.size() != other.size()) {
                return false;
            }
            // check elements
            for (int i = 0; i < self.size(); i++) {
                // include NaN
                if (Double.compare(self.get(i), other.get(i)) != 0) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Hash code of a list according to {@link #hashCode()}.
     *
     * @param self the list to hash
     * @return hashCode of self according to {@link DoubleList#hashCode()}.
     */
    static int hashCode(final DoubleList self) {
        int hashCode = 1;
        for (int i = 0; i < self.size(); i++) {
            hashCode = 31 * hashCode + Double.hashCode(self.get(i));
        }
        return hashCode;
    }

    /**
     * Convert a {@link DoubleList} to a string, suitable for implementing
     * {@link Object#toString()} on a {@link DoubleList}.
     *
     * @param list to convert to a string.
     * @return a string form of {@code list}
     */
    static String toString(final DoubleList list) {
        final StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (int i = 0; i < list.size(); i++) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(list.get(i));
        }
        return sb.append("]").toString();
    }

}
