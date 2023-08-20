/* Copyright 2002-2023 CS GROUP
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
package org.orekit.models.earth.atmosphere.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.orekit.OrekitMatchers.closeTo;

import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;


public class JB2008SpaceEnvironmentDataTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere");
    }

    @Test
    public void testIssue1116() throws URISyntaxException {
        final URL urlSolfsmy = JB2008SpaceEnvironmentDataTest.class.getClassLoader().getResource("atmosphere/SOLFSMY_trunc.txt");
        final URL urlDtc = JB2008SpaceEnvironmentDataTest.class.getClassLoader().getResource("atmosphere/DTCFILE_trunc.TXT");
        JB2008SpaceEnvironmentData JBData = new JB2008SpaceEnvironmentData(new DataSource(urlSolfsmy.toURI()), new DataSource(urlDtc.toURI()));
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453006, 0, TimeScalesFactory.getUTC());
        assertThat(120.6, closeTo(JBData.getF10B(julianDate), 1e-10));
    }

}
