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

package org.orekit.errors;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

/**
 * Unit tests for {@link UnsupportedParameterException}.
 *
 * @since 12.0
 * @author Maxime Journot
 */
public class UnsupportedParameterExceptionTest {

    /** Check building message for empty parameter list. */
    @Test
    public void testNoParameter() {

        // Given empty parameter list
        final List<ParameterDriver> drivers = new ArrayList<>();

        // When
        final String paramName = "dummy-param";
        final OrekitException exception = new UnsupportedParameterException(paramName, drivers);

        // Then
        Assertions.assertEquals(paramName, exception.getParts()[0]);
        Assertions.assertEquals(UnsupportedParameterException.NO_PARAMETER, exception.getParts()[1]);
    }

    /** Check building message for one parameter list. */
    @Test
    public void testOneParameter() {

        // Given one parameter list
        final List<ParameterDriver> drivers = new ArrayList<>();
        final ParameterDriver param1 = new ParameterDriver("param-1", 0., 1., -1., 1.);
        drivers.add(param1);

        // When
        final String paramName = "dummy-param";
        final OrekitException exception = new UnsupportedParameterException(paramName, drivers);

        // Then
        Assertions.assertEquals(paramName, exception.getParts()[0]);
        Assertions.assertEquals(param1.getName(), exception.getParts()[1]);
    }

    /** Check building message for multiple parameters list. */
    @Test
    public void testMultipleParameters() {

        // Given multiple parameter list
        final List<ParameterDriver> drivers = new ArrayList<>();
        final ParameterDriver param1 = new ParameterDriver("param-1", 0., 1., -1., 1.);
        final ParameterDriver param2 = new ParameterDriver("param-2", 0., 1., -1., 1.);
        final ParameterDriver param3 = new ParameterDriver("param-3", 0., 1., -1., 1.);
        // Add some time spans to param3 to check message
        param3.addSpans(AbsoluteDate.ARBITRARY_EPOCH, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(Constants.JULIAN_DAY), 3600.);
        drivers.add(param1);
        drivers.add(param2);
        drivers.add(param3);


        // When
        final String paramName = "dummy-param";
        final OrekitException exception = new UnsupportedParameterException(paramName, drivers);

        // Then
        final String supportedParameters = param1.getName() + UnsupportedParameterException.COMMA_SEP + 
                        param2.getName() + UnsupportedParameterException.COMMA_SEP + param3.getName();
        Assertions.assertEquals(paramName, exception.getParts()[0]);
        Assertions.assertEquals(supportedParameters, exception.getParts()[1]);
    }
}
