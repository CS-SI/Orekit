/* Copyright 2002-2022 CS GROUP
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
package org.orekit.files.ccsds.definitions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;

public class BodyFacadeTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testIssue898Earth() {

        // Create the body facade
        final BodyFacade body = BodyFacade.create(CenterName.EARTH);

        // Verify
        Assert.assertEquals("EARTH", body.getName());
        Assert.assertEquals("Earth", body.getBody().getName());

    }

    @Test
    public void testIssue898Sun() {

        // Create the body facade
        final BodyFacade body = BodyFacade.create(CenterName.SUN);

        // Verify
        Assert.assertEquals("SUN", body.getName());
        Assert.assertEquals("Sun", body.getBody().getName());

    }

}
