/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.gnss.attitude;

import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;


public class GenericGNCCTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new GenericGNSS(validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testLargeNegativeBetaGalileo() throws OrekitException {
        doTestAxes("beta-large-negative-GALILEO.txt", 1.3e-15, 1.2e-15, 5.5e-16);
    }

    @Test
    public void testLargePositiveBetaGalileo() throws OrekitException {
        doTestAxes("beta-large-positive-GALILEO.txt", 1.4e-15, 5.5e-16, 7.1e-16);
    }

    @Test
    public void testLargeNegativeBetaBlonass() throws OrekitException {
        doTestAxes("beta-large-negative-GLONASS.txt", 1.5e-15, 1.1e-15, 3.1e-16);
    }

    @Test
    public void testLargePositiveBetaGLONASS() throws OrekitException {
        doTestAxes("beta-large-positive-GLONASS.txt", 1.3e-15, 7.7e-16, 5.4e-16);
    }

    @Test
    public void testLargeNegativeBetaBlockIIA() throws OrekitException {
        doTestAxes("beta-large-negative-BLOCK-IIA.txt", 1.1e-15, 8.4e-15, 4.0e-16);
    }

    @Test
    public void testLargePositiveBetaBlockIIA() throws OrekitException {
        doTestAxes("beta-large-positive-BLOCK-IIA.txt", 9.0e-16, 1.2e-15, 8.0e-16);
    }

    @Test
    public void testLargeNegativeBetaBlockIIF() throws OrekitException {
        doTestAxes("beta-large-negative-BLOCK-IIF.txt", 1.1e-15, 8.9e-16, 4.2e-16);
    }

    @Test
    public void testLargePositiveBetaBlockIIF() throws OrekitException {
        doTestAxes("beta-large-positive-BLOCK-IIF.txt", 1.1e-15, 7.7e-16, 3.2e-16);
    }

    @Test
    public void testLargeNegativeBetaBlockIIR() throws OrekitException {
        doTestAxes("beta-large-negative-BLOCK-IIR.txt", 1.5e-15, 1.2e-15, 8.8e-16);
    }

    @Test
    public void testLargePositiveBetaBlockIIR() throws OrekitException {
        doTestAxes("beta-large-positive-BLOCK-IIR.txt",  1.3e-15, 7.0e-15, 8.5e-16);
    }

}
