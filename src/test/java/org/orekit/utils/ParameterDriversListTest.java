/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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


import org.junit.Assert;
import org.junit.Test;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

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

        DelegatingDriver delegating11 = list1.findByName("p1");
        DelegatingDriver delegating21 = list1.findByName("p2");
        DelegatingDriver delegating12 = list2.findByName("p1");
        DelegatingDriver delegating22 = list2.findByName("p2");

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
}
