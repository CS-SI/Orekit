/* Copyright 2002-2017 CS Systèmes d'Information
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
import org.orekit.errors.OrekitException;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

public class ParameterDriversListTest {

    @Test
    public void testDownwardAndUpwardSettings() throws OrekitException {

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

        ParameterDriversList.DelegatingDriver delegating11 = select("p1", list1);
        ParameterDriversList.DelegatingDriver delegating21 = select("p2", list1);
        ParameterDriversList.DelegatingDriver delegating12 = select("p1", list2);
        ParameterDriversList.DelegatingDriver delegating22 = select("p2", list2);

        // downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setValue(0.5);
        Assert.assertEquals(0.5, p1A.getValue(),          1.0e-15);
        Assert.assertEquals(0.5, p1B.getValue(),          1.0e-15);
        Assert.assertEquals(0.5, delegating12.getValue(), 1.0e-15);

        // upward settings, starting from raw drivers
        p2A.setValue(-0.5);
        Assert.assertEquals(-0.5, p2B.getValue(),          1.0e-15);
        Assert.assertEquals(-0.5, delegating21.getValue(), 1.0e-15);
        Assert.assertEquals(-0.5, delegating22.getValue(), 1.0e-15);

        // downward settings, from top delegating driver to raw drivers and back to other list top
        delegating11.setName("q1");
        Assert.assertEquals("q1", p1A.getName());
        Assert.assertEquals("q1", p1B.getName());
        Assert.assertEquals("q1", delegating12.getName());

        // upward settings, starting from raw drivers
        p2A.setName("q2");
        Assert.assertEquals("q2", p2B.getName());
        Assert.assertEquals("q2", delegating21.getName());
        Assert.assertEquals("q2", delegating22.getName());

    }

    private DelegatingDriver select(String name, ParameterDriversList list) {
        for (ParameterDriversList.DelegatingDriver d : list.getDrivers()) {
            if (name.equals(d.getName())) {
                return d;
            }
        }
        return null;
    }

}
