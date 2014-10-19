/* Copyright 2013 Applied Defense Solutions, Inc.
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

/**
 * @author Hank Grabowski
 *
 */
public class OrekitConfigurationTest {

    /**
     * Test method for {@link org.orekit.utils.OrekitConfiguration#setCacheSlotsNumber(int)}.
     */
    @Test
    public void testGetSetCacheSlotsNumber() {
        int defaultSlots = OrekitConfiguration.getCacheSlotsNumber();

        Assert.assertNotEquals(defaultSlots, 0);

        int setSlots = 105;

        OrekitConfiguration.setCacheSlotsNumber(setSlots);

        int getSlots = OrekitConfiguration.getCacheSlotsNumber();

        Assert.assertEquals(getSlots, setSlots);

    }
}
