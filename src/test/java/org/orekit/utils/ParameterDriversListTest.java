/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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


import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

public class ParameterDriversListTest {

    @Test
    public void testDownwardAndUpwardSettings() {

        // this test used to generate an infinite recursion ending with StackOverFlowError
        ParameterDriver p1A = new ParameterDriver("p1", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver p1B = new ParameterDriver("p1", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver p2A = new ParameterDriver("p2", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver p2B = new ParameterDriver("p2", 0.0, 1.0, -1.0, +1.0);

        ParameterDriversList list1 = new ParameterDriversList();
        list1.add(p1A);
        list1.add(p1B);
        list1.add(p2A);
        list1.add(p2B);
        ParameterDriversList list2 = new ParameterDriversList();
        list2.add(p1A);
        list2.add(p1B);
        list2.add(p2A);
        list2.add(p2B);

        ParameterDriversList.DelegatingDriver delegating11 = list1.findByName("p1");
        ParameterDriversList.DelegatingDriver delegating21 = list1.findByName("p2");
        ParameterDriversList.DelegatingDriver delegating12 = list2.findByName("p1");
        ParameterDriversList.DelegatingDriver delegating22 = list2.findByName("p2");

        // Value: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setValue(0.5);
        Assert.assertEquals(0.5, p1A.getValue(),          1.0e-15);
        Assert.assertEquals(0.5, p1B.getValue(),          1.0e-15);
        Assert.assertEquals(0.5, delegating12.getValue(), 1.0e-15);

        // Value: upward settings, starting from raw drivers
        p2A.setValue(-0.5);
        Assert.assertEquals(-0.5, p2B.getValue(),          1.0e-15);
        Assert.assertEquals(-0.5, delegating21.getValue(), 1.0e-15);
        Assert.assertEquals(-0.5, delegating22.getValue(), 1.0e-15);

        // Name: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setName("q1");
        Assert.assertEquals("q1", p1A.getName());
        Assert.assertEquals("q1", p1B.getName());
        Assert.assertEquals("q1", delegating12.getName());

        // Name: upward settings, starting from raw drivers
        p2A.setName("q2");
        Assert.assertEquals("q2", p2B.getName());
        Assert.assertEquals("q2", delegating21.getName());
        Assert.assertEquals("q2", delegating22.getName());
        
        // Reference value: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setReferenceValue(0.5);
        Assert.assertEquals(0.5, p1A.getReferenceValue(),          1.0e-15);
        Assert.assertEquals(0.5, p1B.getReferenceValue(),          1.0e-15);
        Assert.assertEquals(0.5, delegating12.getReferenceValue(), 1.0e-15);

        // Reference value: upward settings, starting from raw drivers
        p2A.setReferenceValue(-0.5);
        Assert.assertEquals(-0.5, p2B.getReferenceValue(),          1.0e-15);
        Assert.assertEquals(-0.5, delegating21.getReferenceValue(), 1.0e-15);
        Assert.assertEquals(-0.5, delegating22.getReferenceValue(), 1.0e-15);
        
        // Scale: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setScale(2.);
        Assert.assertEquals(2., p1A.getScale(),          1.0e-15);
        Assert.assertEquals(2., p1B.getScale(),          1.0e-15);
        Assert.assertEquals(2., delegating12.getScale(), 1.0e-15);

        // Scale: upward settings, starting from raw drivers
        p2A.setScale(3.);
        Assert.assertEquals(3., p2B.getScale(),          1.0e-15);
        Assert.assertEquals(3., delegating21.getScale(), 1.0e-15);
        Assert.assertEquals(3., delegating22.getScale(), 1.0e-15);
        
        // Min value: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setMinValue(-2.);
        Assert.assertEquals(-2., p1A.getMinValue(),          1.0e-15);
        Assert.assertEquals(-2., p1B.getMinValue(),          1.0e-15);
        Assert.assertEquals(-2., delegating12.getMinValue(), 1.0e-15);
        
        // Min value: upward settings, starting from raw drivers
        p2A.setMinValue(-0.25);
        Assert.assertEquals(-0.25, p2B.getMinValue(),          1.0e-15);
        Assert.assertEquals(-0.25, delegating21.getMinValue(), 1.0e-15);
        Assert.assertEquals(-0.25, delegating22.getMinValue(), 1.0e-15);
        // Check that value is set to min as it was out of boundaries
        Assert.assertEquals(-0.25, p2B.getValue(),          1.0e-15);
        Assert.assertEquals(-0.25, delegating21.getValue(), 1.0e-15);
        Assert.assertEquals(-0.25, delegating22.getValue(), 1.0e-15);
        
        // Max value: downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setMaxValue(0.25);
        Assert.assertEquals(0.25, p1A.getMaxValue(),          1.0e-15);
        Assert.assertEquals(0.25, p1B.getMaxValue(),          1.0e-15);
        Assert.assertEquals(0.25, delegating12.getMaxValue(), 1.0e-15);
        // Check that value is set to max as it was out of boundaries
        Assert.assertEquals(0.25, p1A.getValue(),          1.0e-15);
        Assert.assertEquals(0.25, p1B.getValue(),          1.0e-15);
        Assert.assertEquals(0.25, delegating12.getValue(), 1.0e-15);
        
        // Max value: upward settings, starting from raw drivers
        p2A.setMaxValue(2.);
        Assert.assertEquals(2., p2B.getMaxValue(),          1.0e-15);
        Assert.assertEquals(2., delegating21.getMaxValue(), 1.0e-15);
        Assert.assertEquals(2., delegating22.getMaxValue(), 1.0e-15);
    }

    @Test
    public void testEmbeddedList() {
        ParameterDriver pA1 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver pA2 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver pA3 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver pB1 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver pB2 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriversList listA = new ParameterDriversList();
        listA.add(pA1);
        pA1.setSelected(true);
        listA.add(pA2);
        listA.add(pA3);
        ParameterDriversList listB = new ParameterDriversList();
        listB.add(pB1);
        listB.add(pB2);

        listA.add(listB.getDrivers().get(0));

        pA1.setValue(0.5);
        for (ParameterDriver pd : new ParameterDriver[] { pA1, pA2, pA3, pB1, pB2 }) {
            Assert.assertEquals(0.5, pd.getValue(), 1.0e-15);
            Assert.assertTrue(pd.isSelected());
        }

        pB2.setValue(-0.5);
        for (ParameterDriver pd : new ParameterDriver[] { pA1, pA2, pA3, pB1, pB2 }) {
            Assert.assertEquals(-0.5, pd.getValue(), 1.0e-15);
        }

        for (final ParameterDriversList list : new ParameterDriversList[] { listA, listB }) {
            Assert.assertEquals(1, list.getNbParams());
            Assert.assertEquals(5, list.getDrivers().get(0).getRawDrivers().size());
            Assert.assertSame(pA1, list.getDrivers().get(0).getRawDrivers().get(0));
            Assert.assertSame(pA2, list.getDrivers().get(0).getRawDrivers().get(1));
            Assert.assertSame(pA3, list.getDrivers().get(0).getRawDrivers().get(2));
            Assert.assertSame(pB1, list.getDrivers().get(0).getRawDrivers().get(3));
            Assert.assertSame(pB2, list.getDrivers().get(0).getRawDrivers().get(4));
        }

        // this should be a no-op
        listB.add(listA.getDrivers().get(0));

        for (final ParameterDriversList list : new ParameterDriversList[] { listA, listB }) {
            Assert.assertEquals(1, list.getNbParams());
            Assert.assertEquals(5, list.getDrivers().get(0).getRawDrivers().size());
            Assert.assertSame(pA1, list.getDrivers().get(0).getRawDrivers().get(0));
            Assert.assertSame(pA2, list.getDrivers().get(0).getRawDrivers().get(1));
            Assert.assertSame(pA3, list.getDrivers().get(0).getRawDrivers().get(2));
            Assert.assertSame(pB1, list.getDrivers().get(0).getRawDrivers().get(3));
            Assert.assertSame(pB2, list.getDrivers().get(0).getRawDrivers().get(4));
        }

        listB.findByName("p").setValue(0.0);
        for (ParameterDriver pd : new ParameterDriver[] { pA1, pA2, pA3, pB1, pB2 }) {
            Assert.assertEquals(0.0, pd.getValue(), 1.0e-15);
        }

    }

    @Test
    public void testMerge() {
        ParameterDriver pA1 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver pA2 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver pA3 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver pB1 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver pB2 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver pC1 = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver qA1 = new ParameterDriver("q", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver qA2 = new ParameterDriver("q", 0.0, 1.0, -1.0, +1.0);
        ParameterDriver qB1 = new ParameterDriver("q", 0.0, 1.0, -1.0, +1.0);
        final AtomicBoolean called = new AtomicBoolean(false);
        qB1.addObserver((previous, driver) -> called.set(true));
        ParameterDriversList listA = new ParameterDriversList();
        listA.add(pA1);
        listA.add(pA2);
        listA.add(pA3);
        listA.add(qA1);
        listA.add(qA2);
        ParameterDriversList listB = new ParameterDriversList();
        listB.add(pB1);
        for (int i = 0; i < 3; ++i) {
            pB2.setSelected(true);
            listB.add(pB2);
        }
        listB.add(qB1);

        ParameterDriversList.DelegatingDriver oldDelegating = listB.getDrivers().get(0);
        listA.add(oldDelegating);
        listA.add(qB1);
        new ParameterDriversList().add(pC1);
        listB.add(pC1);
        listA.sort();

        pA1.setValue(0.5);
        for (ParameterDriver pd : new ParameterDriver[] { pA1, pA2, pA3, pB1, pB2, pC1 }) {
            Assert.assertEquals(0.5, pd.getValue(), 1.0e-15);
            Assert.assertTrue(pd.isSelected());
        }
        qA2.setValue(0.25);
        for (ParameterDriver pd : new ParameterDriver[] { qA1, qA2, qB1 }) {
            Assert.assertEquals(0.25, pd.getValue(), 1.0e-15);
            Assert.assertFalse(pd.isSelected());
        }
        Assert.assertTrue(called.get());

        listB.filter(false);
        Assert.assertEquals(2, listA.getNbParams());
        Assert.assertEquals(6, listA.getDrivers().get(0).getRawDrivers().size());
        Assert.assertSame(pA1, listA.getDrivers().get(0).getRawDrivers().get(0));
        Assert.assertSame(pA2, listA.getDrivers().get(0).getRawDrivers().get(1));
        Assert.assertSame(pA3, listA.getDrivers().get(0).getRawDrivers().get(2));
        Assert.assertSame(pB1, listA.getDrivers().get(0).getRawDrivers().get(3));
        Assert.assertSame(pB2, listA.getDrivers().get(0).getRawDrivers().get(4));
        Assert.assertSame(pC1, listA.getDrivers().get(0).getRawDrivers().get(5));
        Assert.assertEquals(3, listA.getDrivers().get(1).getRawDrivers().size());
        Assert.assertSame(qA1, listA.getDrivers().get(1).getRawDrivers().get(0));
        Assert.assertSame(qA2, listA.getDrivers().get(1).getRawDrivers().get(1));
        Assert.assertSame(qB1, listA.getDrivers().get(1).getRawDrivers().get(2));
        Assert.assertEquals(1, listB.getNbParams());
        Assert.assertEquals(3, listB.getDrivers().get(0).getRawDrivers().size());
        Assert.assertSame(qA1, listB.getDrivers().get(0).getRawDrivers().get(0));
        Assert.assertSame(qA2, listB.getDrivers().get(0).getRawDrivers().get(1));
        Assert.assertSame(qB1, listB.getDrivers().get(0).getRawDrivers().get(2));

        Assert.assertNotSame(oldDelegating, listB.getDrivers().get(0));
        Assert.assertEquals(6, oldDelegating.getRawDrivers().size());
        Assert.assertSame(pA1, oldDelegating.getRawDrivers().get(0));
        Assert.assertSame(pA2, oldDelegating.getRawDrivers().get(1));
        Assert.assertSame(pA3, oldDelegating.getRawDrivers().get(2));
        Assert.assertSame(pB1, oldDelegating.getRawDrivers().get(3));
        Assert.assertSame(pB2, oldDelegating.getRawDrivers().get(4));
        Assert.assertSame(pC1, listA.getDrivers().get(0).getRawDrivers().get(5));

    }

}
