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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.orekit.OrekitMatchers.doubleArrayContaining;

/**
 * test {@link DoubleArrayList}.
 *
 * @author Evan M. Ward
 *
 */
public class DoubleArrayListTest {

    /** array of values in {@link #list} */
    private double[] array;
    /** subject under test */
    private DoubleList list;

    /** set up */
    @BeforeEach
    public void setUp() {
        array = new double[]{1, 2, 3};
        list = new DoubleArrayList(5);
        list.addAll(DoubleArrayList.wrap(array));
    }

    /** check {@link DoubleArrayList#of(double[])} */
    @Test
    public void testOf() {
        // action
        DoubleList list = DoubleArrayList.of(array);

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 2, 3));
        assertThat(list.size(), is(3));
    }

    /** check {@link DoubleArrayList#emptyList()} */
    @Test
    public void testEmptyList() {
        // action
        DoubleList list = DoubleArrayList.emptyList();

        // verify
        assertThat(list.size(), is(0));
    }

    /** check constructors {@link DoubleArrayList#DoubleArrayList(int)}. */
    @Test
    public void testDoubleArrayList() {
        DoubleList list = new DoubleArrayList();
        assertThat(list.size(), is(0));
        list = new DoubleArrayList(5);
        assertThat(list.size(), is(0));
    }

    /** Check {@link DoubleArrayList#DoubleArrayList(DoubleList)} */
    @Test
    public void testDoubleArrayListCopy() {
        DoubleList copy = new DoubleArrayList(list);
        assertThat(copy.toArray(), doubleArrayContaining(1, 2, 3));
        assertThat(copy.size(), is(3));
        // check modification has no effect on copy
        list.set(0, 5);
        assertThat(copy.toArray(), doubleArrayContaining(1, 2, 3));
        assertThat(copy.size(), is(3));
    }

    /** check {@link DoubleArrayList#size()} */
    @Test
    public void testSize() {
        assertThat(list.size(), is(3));
    }

    /** Check {@link DoubleArrayList#isEmpty()} */
    @Test
    public void testIsEmpty() {
        assertThat(list.isEmpty(), is(false));
        list.clear();
        assertThat(list.isEmpty(), is(true));
    }

    /** check {@link DoubleArrayList#toArray()} */
    @Test
    public void testToArray() {
        // action
        double[] arr = list.toArray();

        // verify
        assertThat(arr, doubleArrayContaining(array));
        // a copy of internal state
        list.set(0, 5);
        assertThat(arr, doubleArrayContaining(array));
    }

    /** Check {@link DoubleArrayList#stream()}. */
    @Test
    public void testStream() {
        assertThat(list.stream().toArray(), doubleArrayContaining(array));
    }

    /** check {@link DoubleArrayList#add(double)} */
    @Test
    public void testAdd() {
        // action
        list.add(4);

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 2, 3, 4));
        assertThat(list.size(), is(4));
    }

    /** check {@link DoubleArrayList#add(int, double)} */
    @Test
    public void testAddIntDouble() {
        // action
        list.add(1, 1.5);
        // verify
        assertThat(list.size(), is(4));
        assertThat(list.toArray(), doubleArrayContaining(1, 1.5, 2, 3));

        // test exception
        try {
            list.add(5, 5);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /** check {@link DoubleArrayList#addAll(DoubleList)} */
    @Test
    public void testAddAll() {
        // action
        list.addAll(DoubleArrayList.of(4, 5));

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 2, 3, 4, 5));
        assertThat(list.size(), is(5));
    }

    /** check {@link DoubleArrayList#addAll(int, DoubleList)} */
    @Test
    public void testAddAllIntDouble() {
        // action
        list.addAll(1, DoubleArrayList.of(4, 5));

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 4, 5, 2, 3));
        assertThat(list.size(), is(5));

        // check non-specialized branch
        list.addAll(3, DoubleArrayList.of(6, 7).subList(0, 1));
        assertThat(list.toArray(), doubleArrayContaining(1, 4, 5, 6, 2, 3));
        assertThat(list.size(), is(6));

        // test exception
        try {
            list.addAll(7, DoubleArrayList.emptyList());
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            list.addAll(-1, DoubleArrayList.emptyList());
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /** check {@link DoubleArrayList#clear()} */
    @Test
    public void testClear() {
        // action
        list.clear();

        // verify
        assertThat(list.size(), is(0));
        assertThat(list.toArray().length, is(0));
    }

    /** Check {@link DoubleArrayList#remove(int)} */
    @Test
    public void testRemove() {
        // action
        list.remove(1);

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 3));
        assertThat(list.size(), is(2));

        // test exception
        try {
            list.remove(2);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            list.remove(-1);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /** check {@link DoubleArrayList#removeRange(int, int)} */
    @Test
    public void testRemoveRange() {
        list.removeRange(1, 2);

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 3));
        assertThat(list.size(), is(2));

        // test exception
        try {
            list.removeRange(2, 1);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        // test exception
        try {
            list.removeRange(0, 3);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        // test exception
        try {
            list.removeRange(-1, 1);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /** check {@link DoubleArrayList#get(int)} */
    @Test
    public void testGet() {
        // verify nominal
        assertThat(list.get(2), is(3.0));

        // verify index out of range
        try {
            list.get(3);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /** check {@link DoubleArrayList#getFirst()} */
    @Test
    public void testGetFirst() {
        assertThat(list.getFirst(), is(1.0));

        // check exception
        list.clear();
        try {
            list.getFirst();
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    /** check {@link DoubleArrayList#getLast()} */
    @Test
    public void testgetLast() {
        assertThat(list.getLast(), is(3.0));

        // check exception
        list.clear();
        try {
            list.getLast();
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    /** check {@link DoubleArrayList#set(int, double)} */
    @Test
    public void testSet() {
        // nominal action + verify
        list.set(2, 5);
        assertThat(list.toArray(), doubleArrayContaining(1, 2, 5));

        // out of bounds action + verify
        try {
            list.set(3, 5);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /** check {@link DoubleArrayList#equals(Object)} */
    @Test
    public void testEquals() {
        // setup
        // equivalent list
        DoubleList same = Mockito.mock(DoubleList.class);
        when(same.size()).thenReturn(3);
        when(same.get(0)).thenReturn(1.0);
        when(same.get(1)).thenReturn(2.0);
        when(same.get(2)).thenReturn(3.0);
        // different values
        DoubleList value = Mockito.mock(DoubleList.class);
        when(value.size()).thenReturn(3);
        when(value.get(0)).thenReturn(1.0);
        when(value.get(1)).thenReturn(2.0);
        when(value.get(2)).thenReturn(5.0);
        // different length
        DoubleList size = Mockito.mock(DoubleList.class);
        when(size.size()).thenReturn(4);

        // action + verify
        assertThat(list, is(same));
        assertThat(list, is(not(value)));
        assertThat(list, is(not(size)));
        assertThat(list, is(not(new Object())));
    }

    /** check {@link DoubleArrayList#hashCode()} */
    @Test
    public void testHashcode() {
        // 31 * (31 * (31 + 0x3ff00000) + 0x40000000) + 0x40080000
        assertThat(list.hashCode(), is(0x03F8745F));
    }

    /** check {@link DoubleArrayList#subList(int, int)} */
    @Test
    public void testSubList() {
        // action
        DoubleList subList = list.subList(2, 3);
        // verify
        assertThat(subList.size(), is(1));
        assertThat(subList.toArray(), doubleArrayContaining(3));

        // verify exceptions
        // test exception
        try {
            list.subList(2, 1);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        // test exception
        try {
            list.subList(0, 4);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /** check that a ZERO length sublist can be taken off the end. */
    @Test
    public void testSubListZeroLength() {
        assertThat(list.subList(list.size(), list.size()).size(), is(0));
    }

    /** check {@link DoubleArrayList#toString()} */
    @Test
    public void testToString() {
        assertThat(list.toString(), is("[1.0, 2.0, 3.0]"));
        assertThat(list.subList(1, 3).toString(), is("[2.0, 3.0]"));
    }
}
