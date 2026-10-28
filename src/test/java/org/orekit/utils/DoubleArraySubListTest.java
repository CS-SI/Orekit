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
 * Checks for {@link DoubleArrayList#subList(int, int)}
 *
 * @author Evan Ward
 *
 */
public class DoubleArraySubListTest {
    /** parent */
    private DoubleList list;
    /** list of {2,3} */
    private DoubleList sList;
    /** list of {2} */
    private DoubleList ssList;

    /** set up */
    @BeforeEach
    public void setUp() {
        list = DoubleArrayList.of(1, 2, 3);
        sList = list.subList(1, 3);
        ssList = sList.subList(0, 1);
    }

    /** check sublist size */
    @Test
    public void testSize() {
        assertThat(sList.size(), is(2));
        assertThat(ssList.size(), is(1));
    }

    /** Check sublist is empty. */
    @Test
    public void testIsEmpty() {
        assertThat(sList.isEmpty(), is(false));
        assertThat(ssList.isEmpty(), is(false));

        ssList.clear();
        assertThat(sList.isEmpty(), is(false));
        assertThat(ssList.isEmpty(), is(true));

        sList.clear();
        assertThat(sList.isEmpty(), is(true));
        assertThat(ssList.isEmpty(), is(true));
    }

    /** check sublist to array */
    @Test
    public void testToArray() {
        assertThat(sList.toArray(), doubleArrayContaining(2, 3));
        assertThat(ssList.toArray(), doubleArrayContaining(2));
    }

    /** Check sublist stream. */
    @Test
    public void testStream() {
        assertThat(sList.stream().toArray(), doubleArrayContaining(2, 3));
        assertThat(ssList.stream().toArray(), doubleArrayContaining(2));
    }

    /** check sublist add */
    @Test
    public void testAddDouble() {
        // action
        ssList.add(4);
        sList.add(5);

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 2, 4, 3, 5));
        assertThat(sList.toArray(), doubleArrayContaining(2, 4, 3, 5));
        assertThat(ssList.toArray(), doubleArrayContaining(2, 4));
        assertThat(list.size(), is(5));
        assertThat(sList.size(), is(4));
        assertThat(ssList.size(), is(2));
    }

    /** check sublist add at index */
    @Test
    public void testAddIntDouble() {
        // action
        ssList.add(0, 1.5);
        sList.add(2, 2.5);

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 1.5, 2, 2.5, 3));
        assertThat(sList.toArray(), doubleArrayContaining(1.5, 2, 2.5, 3));
        assertThat(ssList.toArray(), doubleArrayContaining(1.5, 2));
        assertThat(list.size(), is(5));
        assertThat(sList.size(), is(4));
        assertThat(ssList.size(), is(2));
    }

    /** check sublist add all */
    @Test
    public void testAddAllDoubleArray() {
        // action
        ssList.addAll(DoubleArrayList.of(4, 5));

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 2, 4, 5, 3));
        assertThat(sList.toArray(), doubleArrayContaining(2, 4, 5, 3));
        assertThat(ssList.toArray(), doubleArrayContaining(2, 4, 5));
        assertThat(list.size(), is(5));
        assertThat(sList.size(), is(4));
        assertThat(ssList.size(), is(3));
    }

    /** check sublist add at index */
    @Test
    public void testAddAllIntDoubleArray() {
        // action
        ssList.addAll(0, DoubleArrayList.of(4, 5));

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 4, 5, 2, 3));
        assertThat(sList.toArray(), doubleArrayContaining(4, 5, 2, 3));
        assertThat(ssList.toArray(), doubleArrayContaining(4, 5, 2));
        assertThat(list.size(), is(5));
        assertThat(sList.size(), is(4));
        assertThat(ssList.size(), is(3));
    }

    /** check sublist clear */
    @Test
    public void testClear() {
        // action
        ssList.clear();

        // verify
        assertThat(list.toArray(), doubleArrayContaining(1, 3));
        assertThat(sList.toArray(), doubleArrayContaining(3));
        assertThat(ssList.size(), is(0));

        // test exception
        try {
            sList.removeRange(1, 0);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        // test exception
        try {
            ssList.removeRange(0, 1);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /** Check sublist remove element. */
    @Test
    public void testRemove() {
        // action
        ssList.remove(0);

        // verify
        assertThat(ssList.size(), is(0));
        assertThat(sList.toArray(), doubleArrayContaining(3));
        assertThat(sList.size(), is(1));
        assertThat(list.toArray(), doubleArrayContaining(1, 3));
        assertThat(list.size(), is(2));
    }

    /** check sublist get */
    @Test
    public void testGet() {
        // verify
        assertThat(ssList.get(0), is(2.0));
        assertThat(sList.get(1), is(3.0));

        // test exception
        try {
            ssList.get(1);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        // test exception
        try {
            ssList.get(-1);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /** check sublist get first. */
    @Test
    public void testGetFirst() {
        assertThat(ssList.getFirst(), is(2.0));
        assertThat(sList.getFirst(), is(2.0));

        // check exceptions
        ssList.clear();
        assertThat(sList.getFirst(), is(3.0));
        try {
            ssList.getFirst();
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // expected
        }

        sList.clear();
        try {
            sList.getFirst();
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // expected
        }
        try {
            ssList.getFirst();
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    /** check sublist get first. */
    @Test
    public void testgetLast() {
        assertThat(ssList.getLast(), is(2.0));
        assertThat(sList.getLast(), is(3.0));

        // check exceptions
        ssList.clear();
        assertThat(sList.getLast(), is(3.0));
        try {
            ssList.getLast();
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // expected
        }

        sList.clear();
        try {
            sList.getLast();
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // expected
        }
        try {
            ssList.getLast();
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    /** check sublist set */
    @Test
    public void testSet() {
        // action
        ssList.set(0, 5);

        // verify
        assertThat(ssList.toArray(), doubleArrayContaining(5));
        assertThat(sList.toArray(), doubleArrayContaining(5, 3));
        assertThat(list.toArray(), doubleArrayContaining(1, 5, 3));

        // test exception
        try {
            ssList.set(1, 5);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        // test exception
        try {
            ssList.set(-1, 5);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /** check sub list sublist */
    @Test
    public void testSubList1() {
        // verify exceptions
        // test exception
        try {
            sList.subList(1, 0);
            fail("Expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        // test exception
        try {
            ssList.subList(0, 2);
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
        when(same.size()).thenReturn(1);
        when(same.get(0)).thenReturn(2.0);
        // different values
        DoubleList value = Mockito.mock(DoubleList.class);
        when(value.size()).thenReturn(1);
        when(value.get(0)).thenReturn(5.0);
        // different length
        DoubleList size = Mockito.mock(DoubleList.class);
        when(size.size()).thenReturn(4);

        // action + verify
        assertThat(ssList, is(same));
        assertThat(ssList, is(not(value)));
        assertThat(ssList, is(not(size)));
        assertThat(ssList, is(not(new Object())));
    }

    /** check {@link DoubleArrayList#hashCode()} */
    @Test
    public void testHashcode() {
        // 31 * (31 + 0x40000000) + 0x40080000
        assertThat(sList.hashCode(), is(0x000803c1));
        // 31 + 0x40000000
        assertThat(ssList.hashCode(), is(0x4000001f));
    }
}
