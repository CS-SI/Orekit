/* Copyright 2013 Applied Defense Solutions, Inc.
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

import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

        Assertions.assertNotEquals(0, defaultSlots);

        int setSlots = 105;

        OrekitConfiguration.setCacheSlotsNumber(setSlots);

        int getSlots = OrekitConfiguration.getCacheSlotsNumber();

        Assertions.assertEquals(setSlots, getSlots);

    }

    @Test
    public void testVersions() {
        final Pattern pattern = Pattern.compile("unknown|[0-9.]*(?:-SNAPSHOT)?");
        assertTrue(pattern.matcher(MathUtils.getHipparchusVersion()).matches());
        assertTrue(pattern.matcher(OrekitConfiguration.getOrekitVersion()).matches());
    }

}
