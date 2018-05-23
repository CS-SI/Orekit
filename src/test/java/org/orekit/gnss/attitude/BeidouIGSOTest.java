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


public class BeidouIGSOTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new BeidouIGSO(validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testLargeNegativeBeta() throws OrekitException {
        // the "large" error for the X axis (a few tenth of a degree)
        // are due to the reference fortran program assuming perfect circular orbits
        // whereas Orekit takes the non-orthogonality of position and velocity into account
        // this can be seen as the Y and Z axes are almost perfect, hence the reference X
        // does not really correspond to an orthogonal frame
        doTestAxes("beta-large-negative-BEIDOU-2I.txt", 1.4e-15, 1.4e-15, 4.8e-16);
    }

    @Test
    public void testSmallNegativeBeta() throws OrekitException {
        // the "large" error for the X axis (a few tenth of a degree)
        // are due to the reference fortran program assuming perfect circular orbits
        // whereas Orekit takes the non-orthogonality of position and velocity into account
        // this can be seen as the Y and Z axes are almost perfect, hence the reference X
        // does not really correspond to an orthogonal frame
        doTestAxes("beta-small-negative-BEIDOU-2I.txt", 0.0033, 7.6e-16, 4.6e-16);
    }

    @Test
    public void testCrossingBeta() throws OrekitException {
        // the "large" error for the X axis (a few tenth of a degree)
        // are due to the reference fortran program assuming perfect circular orbits
        // whereas Orekit takes the non-orthogonality of position and velocity into account
        // this can be seen as the Y and Z axes are almost perfect, hence the reference X
        // does not really correspond to an orthogonal frame
        doTestAxes("beta-crossing-BEIDOU-2I.txt", 0.0032, 7.6e-16, 3.7e-16);
    }

    @Test
    public void testSmallPositiveBeta() throws OrekitException {
        // the "large" error for the X axis (a few tenth of a degree)
        // are due to the reference fortran program assuming perfect circular orbits
        // whereas Orekit takes the non-orthogonality of position and velocity into account
        // this can be seen as the Y and Z axes are almost perfect, hence the reference X
        // does not really correspond to an orthogonal frame
        doTestAxes("beta-small-positive-BEIDOU-2I.txt", 0.0032, 6.8e-16, 7.5e-16);
    }

    @Test
    public void testLargePositiveBeta() throws OrekitException {
        // the "large" error for the X axis (a few tenth of a degree)
        // are due to the reference fortran program assuming perfect circular orbits
        // whereas Orekit takes the non-orthogonality of position and velocity into account
        // this can be seen as the Y and Z axes are almost perfect, hence the reference X
        // does not really correspond to an orthogonal frame
        doTestAxes("beta-large-positive-BEIDOU-2I.txt", 9.2e-16, 7.8e-16, 4.8e-16);
    }

}
