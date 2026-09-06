/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.utils.drivers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.TimeInterval;

import java.util.concurrent.atomic.AtomicBoolean;

public class FieldParameterDriversListTest {

    @Test
    void testDownwardAndUpwardSettings() {
        doTestDownwardAndUpwardSettings(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestDownwardAndUpwardSettings(final Field<T> field) {
        // this test used to generate an infinite recursion ending with StackOverFlowError
        FieldParameterDriver<T> p1A = new FieldParameterDriver<>("p1", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> p1B = new FieldParameterDriver<>("p1", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> p2A = new FieldParameterDriver<>("p2", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> p2B = new FieldParameterDriver<>("p2", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);

        FieldParameterDriversList<T> list1 = new FieldParameterDriversList<>();
        list1.add(p1A);
        list1.add(p1B);
        list1.add(p2A);
        list1.add(p2B);
        FieldParameterDriversList<T> list2 = new FieldParameterDriversList<>();
        list2.add(p1A);
        list2.add(p1B);
        list2.add(p2A);
        list2.add(p2B);

        FieldParameterDriversList.FieldDelegatingDriver<T> delegating11 = list1.findByName("p1");
        FieldParameterDriversList.FieldDelegatingDriver<T> delegating21 = list1.findByName("p2");
        FieldParameterDriversList.FieldDelegatingDriver<T> delegating12 = list2.findByName("p1");
        FieldParameterDriversList.FieldDelegatingDriver<T> delegating22 = list2.findByName("p2");

        // Value: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setValue(field.getZero().newInstance(0.5));
        Assertions.assertEquals(0.5, p1A.getValue().getReal(),          1.0e-15);
        Assertions.assertEquals(0.5, p1B.getValue().getReal(),          1.0e-15);
        Assertions.assertEquals(0.5, delegating12.getValue().getReal(), 1.0e-15);

        // Value: upward settings, starting from raw drivers
        p2A.setValue(field.getZero().newInstance(-0.5));
        Assertions.assertEquals(-0.5, p2B.getValue().getReal(),          1.0e-15);
        Assertions.assertEquals(-0.5, delegating21.getValue().getReal(), 1.0e-15);
        Assertions.assertEquals(-0.5, delegating22.getValue().getReal(), 1.0e-15);

        // Name: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setName("q1");
        Assertions.assertEquals("q1", p1A.getName());
        Assertions.assertEquals("q1", p1B.getName());
        Assertions.assertEquals("q1", delegating12.getName());

        // Name: upward settings, starting from raw drivers
        p2A.setName("q2");
        Assertions.assertEquals("q2", p2B.getName());
        Assertions.assertEquals("q2", delegating21.getName());
        Assertions.assertEquals("q2", delegating22.getName());

        // Reference value: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setReferenceValue(field.getZero().newInstance(0.5));
        Assertions.assertEquals(0.5, p1A.getReferenceValue().getReal(),          1.0e-15);
        Assertions.assertEquals(0.5, p1B.getReferenceValue().getReal(),          1.0e-15);
        Assertions.assertEquals(0.5, delegating12.getReferenceValue().getReal(), 1.0e-15);

        // Reference value: upward settings, starting from raw drivers
        p2A.setReferenceValue(field.getZero().newInstance(-0.5));
        Assertions.assertEquals(-0.5, p2B.getReferenceValue().getReal(),          1.0e-15);
        Assertions.assertEquals(-0.5, delegating21.getReferenceValue().getReal(), 1.0e-15);
        Assertions.assertEquals(-0.5, delegating22.getReferenceValue().getReal(), 1.0e-15);

        // Scale: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setScale(2.);
        Assertions.assertEquals(2., p1A.getScale(),          1.0e-15);
        Assertions.assertEquals(2., p1B.getScale(),          1.0e-15);
        Assertions.assertEquals(2., delegating12.getScale(), 1.0e-15);

        // Scale: upward settings, starting from raw drivers
        p2A.setScale(3.);
        Assertions.assertEquals(3., p2B.getScale(),          1.0e-15);
        Assertions.assertEquals(3., delegating21.getScale(), 1.0e-15);
        Assertions.assertEquals(3., delegating22.getScale(), 1.0e-15);

        // Min value: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setMinValue(-2.);
        Assertions.assertEquals(-2., p1A.getMinValue(),          1.0e-15);
        Assertions.assertEquals(-2., p1B.getMinValue(),          1.0e-15);
        Assertions.assertEquals(-2., delegating12.getMinValue(), 1.0e-15);

        // Min value: upward settings, starting from raw drivers
        p2A.setMinValue(-0.25);
        Assertions.assertEquals(-0.25, p2B.getMinValue(),          1.0e-15);
        Assertions.assertEquals(-0.25, delegating21.getMinValue(), 1.0e-15);
        Assertions.assertEquals(-0.25, delegating22.getMinValue(), 1.0e-15);
        // Check that value is set to min as it was out of boundaries
        Assertions.assertEquals(-0.25, p2B.getValue().getReal(),          1.0e-15);
        Assertions.assertEquals(-0.25, delegating21.getValue().getReal(), 1.0e-15);
        Assertions.assertEquals(-0.25, delegating22.getValue().getReal(), 1.0e-15);

        // Max value: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setMaxValue(0.25);
        Assertions.assertEquals(0.25, p1A.getMaxValue(),          1.0e-15);
        Assertions.assertEquals(0.25, p1B.getMaxValue(),          1.0e-15);
        Assertions.assertEquals(0.25, delegating12.getMaxValue(), 1.0e-15);
        // Check that value is set to max as it was out of boundaries
        Assertions.assertEquals(0.25, p1A.getValue().getReal(),          1.0e-15);
        Assertions.assertEquals(0.25, p1B.getValue().getReal(),          1.0e-15);
        Assertions.assertEquals(0.25, delegating12.getValue().getReal(), 1.0e-15);

        // Max value: upward settings, starting from raw drivers
        p2A.setMaxValue(2.);
        Assertions.assertEquals(2., p2B.getMaxValue(),          1.0e-15);
        Assertions.assertEquals(2., delegating21.getMaxValue(), 1.0e-15);
        Assertions.assertEquals(2., delegating22.getMaxValue(), 1.0e-15);
    }

    @Test
    public void testEmbeddedList() {
        doTestEmbeddedList(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestEmbeddedList(final Field<T> field) {
        FieldParameterDriver<T> pA1 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> pA2 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> pA3 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> pB1 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> pB2 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriversList<T> listA = new FieldParameterDriversList<>();
        listA.add(pA1);
        pA1.setSelected(true);
        listA.add(pA2);
        listA.add(pA3);
        FieldParameterDriversList<T> listB = new FieldParameterDriversList<>();
        listB.add(pB1);
        listB.add(pB2);

        listA.add(listB.getDrivers().getFirst());

        pA1.setValue(field.getZero().newInstance(0.5));
        for (FieldParameterDriver<?> pd : new FieldParameterDriver<?>[] { pA1, pA2, pA3, pB1, pB2 }) {
            Assertions.assertEquals(0.5, pd.getValue().getReal(), 1.0e-15);
            Assertions.assertTrue(pd.isSelected());
        }

        pB2.setValue(field.getZero().newInstance(-0.5));
        for (FieldParameterDriver<?> pd : new FieldParameterDriver<?>[] { pA1, pA2, pA3, pB1, pB2 }) {
            Assertions.assertEquals(-0.5, pd.getValue().getReal(), 1.0e-15);
        }

        for (final FieldParameterDriversList<?> list : new FieldParameterDriversList<?>[] { listA, listB }) {
            Assertions.assertEquals(1, list.getNbParams());
            Assertions.assertEquals(5, list.getDrivers().getFirst().getRawDrivers().size());
            Assertions.assertSame(pA1, list.getDrivers().getFirst().getRawDrivers().getFirst());
            Assertions.assertSame(pA2, list.getDrivers().getFirst().getRawDrivers().get(1));
            Assertions.assertSame(pA3, list.getDrivers().getFirst().getRawDrivers().get(2));
            Assertions.assertSame(pB1, list.getDrivers().getFirst().getRawDrivers().get(3));
            Assertions.assertSame(pB2, list.getDrivers().getFirst().getRawDrivers().get(4));
        }

        // this should be a no-op
        listB.add(listA.getDrivers().getFirst());

        for (final FieldParameterDriversList<?> list : new FieldParameterDriversList<?>[] { listA, listB }) {
            Assertions.assertEquals(1, list.getNbParams());
            Assertions.assertEquals(5, list.getDrivers().getFirst().getRawDrivers().size());
            Assertions.assertSame(pA1, list.getDrivers().getFirst().getRawDrivers().getFirst());
            Assertions.assertSame(pA2, list.getDrivers().getFirst().getRawDrivers().get(1));
            Assertions.assertSame(pA3, list.getDrivers().getFirst().getRawDrivers().get(2));
            Assertions.assertSame(pB1, list.getDrivers().getFirst().getRawDrivers().get(3));
            Assertions.assertSame(pB2, list.getDrivers().getFirst().getRawDrivers().get(4));
        }

        listB.findByName("p").setValue(field.getZero().newInstance(0.0));
        for (FieldParameterDriver<?> pd : new FieldParameterDriver<?>[] { pA1, pA2, pA3, pB1, pB2 }) {
            Assertions.assertEquals(0.0, pd.getValue().getReal(), 1.0e-15);
        }

    }

    @Test
    public void testMerge() {
        doTestMerge(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestMerge(final Field<T> field) {
        FieldParameterDriver<T> pA1 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> pA2 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> pA3 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> pB1 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> pB2 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> pC1 = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> qA1 = new FieldParameterDriver<>("q", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> qA2 = new FieldParameterDriver<>("q", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> qB1 = new FieldParameterDriver<>("q", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        final AtomicBoolean called = new AtomicBoolean(false);
        qB1.addObserver((previousValue, driver) -> called.set(true));
        FieldParameterDriversList<T> listA = new FieldParameterDriversList<>();
        listA.add(pA1);
        listA.add(pA2);
        listA.add(pA3);
        listA.add(qA1);
        listA.add(qA2);
        FieldParameterDriversList<T> listB = new FieldParameterDriversList<>();
        listB.add(pB1);
        for (int i = 0; i < 3; ++i) {
            pB2.setSelected(true);
            listB.add(pB2);
        }
        listB.add(qB1);

        FieldParameterDriversList.FieldDelegatingDriver<T> oldDelegating = listB.getDrivers().getFirst();
        listA.add(oldDelegating);
        listA.add(qB1);
        final FieldParameterDriversList<T> other = new FieldParameterDriversList<>();
        other.add(pC1);
        listB.add(pC1);
        listA.sort();

        pA1.setValue(field.getZero().newInstance(0.5));
        for (FieldParameterDriver<?> pd : new FieldParameterDriver<?>[] { pA1, pA2, pA3, pB1, pB2, pC1 }) {
            Assertions.assertEquals(0.5, pd.getValue().getReal(), 1.0e-15);
            Assertions.assertTrue(pd.isSelected());
        }
        qA2.setValue(field.getZero().newInstance(0.25));
        for (FieldParameterDriver<?> pd : new FieldParameterDriver<?>[] { qA1, qA2, qB1 }) {
            Assertions.assertEquals(0.25, pd.getValue().getReal(), 1.0e-15);
            Assertions.assertFalse(pd.isSelected());
        }
        Assertions.assertTrue(called.get());

        listB.filter(false);
        Assertions.assertEquals(2, listA.getNbParams());
        Assertions.assertEquals(6, listA.getDrivers().getFirst().getRawDrivers().size());
        Assertions.assertSame(pA1, listA.getDrivers().getFirst().getRawDrivers().getFirst());
        Assertions.assertSame(pA2, listA.getDrivers().getFirst().getRawDrivers().get(1));
        Assertions.assertSame(pA3, listA.getDrivers().getFirst().getRawDrivers().get(2));
        Assertions.assertSame(pB1, listA.getDrivers().getFirst().getRawDrivers().get(3));
        Assertions.assertSame(pB2, listA.getDrivers().getFirst().getRawDrivers().get(4));
        Assertions.assertSame(pC1, listA.getDrivers().getFirst().getRawDrivers().get(5));
        Assertions.assertEquals(3, listA.getDrivers().get(1).getRawDrivers().size());
        Assertions.assertSame(qA1, listA.getDrivers().get(1).getRawDrivers().getFirst());
        Assertions.assertSame(qA2, listA.getDrivers().get(1).getRawDrivers().get(1));
        Assertions.assertSame(qB1, listA.getDrivers().get(1).getRawDrivers().get(2));
        Assertions.assertEquals(1, listB.getNbParams());
        Assertions.assertEquals(3, listB.getDrivers().getFirst().getRawDrivers().size());
        Assertions.assertSame(qA1, listB.getDrivers().getFirst().getRawDrivers().getFirst());
        Assertions.assertSame(qA2, listB.getDrivers().getFirst().getRawDrivers().get(1));
        Assertions.assertSame(qB1, listB.getDrivers().getFirst().getRawDrivers().get(2));

        Assertions.assertNotSame(oldDelegating, listB.getDrivers().getFirst());
        Assertions.assertEquals(6, oldDelegating.getRawDrivers().size());
        Assertions.assertSame(pA1, oldDelegating.getRawDrivers().getFirst());
        Assertions.assertSame(pA2, oldDelegating.getRawDrivers().get(1));
        Assertions.assertSame(pA3, oldDelegating.getRawDrivers().get(2));
        Assertions.assertSame(pB1, oldDelegating.getRawDrivers().get(3));
        Assertions.assertSame(pB2, oldDelegating.getRawDrivers().get(4));
        Assertions.assertSame(pC1, listA.getDrivers().getFirst().getRawDrivers().get(5));

    }

    @Test
    public void testAddSameDriver() {
        doTestAddSameDriver(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestAddSameDriver(final Field<T> field) {
        FieldParameterDriver<T> p = new FieldParameterDriver<>("p", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> q = new FieldParameterDriver<>("q", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriver<T> r = new FieldParameterDriver<>("r", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        FieldParameterDriversList<T> list = new FieldParameterDriversList<>();

        // first add the drivers once each
        list.add(p);
        list.add(q);
        list.add(r);
        Assertions.assertEquals(3, list.getDrivers().size());
        Assertions.assertEquals(1, list.getDrivers().getFirst().getRawDrivers().size());
        Assertions.assertSame(p, list.getDrivers().getFirst().getRawDrivers().getFirst());
        Assertions.assertEquals(1, list.getDrivers().get(1).getRawDrivers().size());
        Assertions.assertSame(q, list.getDrivers().get(1).getRawDrivers().getFirst());
        Assertions.assertEquals(1, list.getDrivers().get(2).getRawDrivers().size());
        Assertions.assertSame(r, list.getDrivers().get(2).getRawDrivers().getFirst());

        // then add the same ones several times more, this should be a no-op
        list.add(p);
        list.add(q);
        list.add(r);
        list.add(r);
        list.add(r);
        list.add(p);
        list.add(q);
        list.add(p);
        list.add(r);
        Assertions.assertEquals(3, list.getDrivers().size());
        Assertions.assertEquals(1, list.getDrivers().getFirst().getRawDrivers().size());
        Assertions.assertSame(p, list.getDrivers().getFirst().getRawDrivers().getFirst());
        Assertions.assertEquals(1, list.getDrivers().get(1).getRawDrivers().size());
        Assertions.assertSame(q, list.getDrivers().get(1).getRawDrivers().getFirst());
        Assertions.assertEquals(1, list.getDrivers().get(2).getRawDrivers().size());
        Assertions.assertSame(r, list.getDrivers().get(2).getRawDrivers().getFirst());

        // then add a new driver for the second parameter
        FieldParameterDriver<T> newQ = new FieldParameterDriver<>("q", field.getZero(), 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);
        list.add(newQ);
        Assertions.assertEquals(3, list.getDrivers().size());
        Assertions.assertEquals(1, list.getDrivers().getFirst().getRawDrivers().size());
        Assertions.assertSame(p, list.getDrivers().getFirst().getRawDrivers().getFirst());
        Assertions.assertEquals(2, list.getDrivers().get(1).getRawDrivers().size());
        Assertions.assertSame(q, list.getDrivers().get(1).getRawDrivers().getFirst());
        Assertions.assertSame(newQ, list.getDrivers().get(1).getRawDrivers().get(1));
        Assertions.assertEquals(1, list.getDrivers().get(2).getRawDrivers().size());
        Assertions.assertSame(r, list.getDrivers().get(2).getRawDrivers().getFirst());

    }

}
