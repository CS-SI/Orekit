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


public class BeidouMeoTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new BeidouMeo(validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testLargeNegativeBeta() throws OrekitException {
        doTestAxes("beta-large-negative-BEIDOU-2M.txt", 1.1e-15, 1.1e-15, 5.6e-16);
    }

    @Test
    public void testSmallNegativeBeta() throws OrekitException {
        // the "large" error for the X axis (a few tenth of a degree)
        // are due to the reference fortran program assuming perfect circular orbits
        // whereas Orekit takes the non-orthogonality of position and velocity into account
        // this can be seen as the Y and Z axes are almost perfect, hence the reference X
        // does not really correspond to an orthogonal frame
        doTestAxes("beta-small-negative-BEIDOU-2M.txt", 0.0014, 8.9e-16, 4.8e-16);
    }

    @Test
    public void testCrossingBeta() throws OrekitException {
        // the "large" error for the X axis (a few tenth of a degree)
        // are due to the reference fortran program assuming perfect circular orbits
        // whereas Orekit takes the non-orthogonality of position and velocity into account
        // this can be seen as the Y and Z axes are almost perfect, hence the reference X
        // does not really correspond to an orthogonal frame
        doTestAxes("beta-crossing-BEIDOU-2M.txt", 0.0018, 6.2e-16, 3.7e-16);
    }

    @Test
    public void testSmallPositiveBeta() throws OrekitException {
        // the "large" error for the X axis (a few tenth of a degree)
        // are due to the reference fortran program assuming perfect circular orbits
        // whereas Orekit takes the non-orthogonality of position and velocity into account
        // this can be seen as the Y and Z axes are almost perfect, hence the reference X
        // does not really correspond to an orthogonal frame
        doTestAxes("beta-small-positive-BEIDOU-2M.txt", 0.0013, 7.2e-16, 4.1e-16);
    }

    @Test
    public void testLargePositiveBeta() throws OrekitException {
        doTestAxes("beta-large-positive-BEIDOU-2M.txt", 9.0e-16, 8.6e-16, 3.1e-16);
    }

}
