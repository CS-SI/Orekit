/* Copyright 2002-2024 CS GROUP
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.frames.LOFType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrbitRelativeFrameTest {
    @Test
    @DisplayName("Test OrbitRelativeFrame definitions and their LOFType equivalent")
    void testOrbitRelativeFrameDefinitionsAndLOFTypeEquivalent() {
        // Given
        final OrbitRelativeFrame eqwInertial  = OrbitRelativeFrame.EQW_INERTIAL;
        final OrbitRelativeFrame lvlhRotating = OrbitRelativeFrame.LVLH_ROTATING;
        final OrbitRelativeFrame lvlhInertial = OrbitRelativeFrame.LVLH_INERTIAL;
        final OrbitRelativeFrame lvlh         = OrbitRelativeFrame.LVLH;
        final OrbitRelativeFrame nswRotating  = OrbitRelativeFrame.NSW_ROTATING;
        final OrbitRelativeFrame nswInertial  = OrbitRelativeFrame.NSW_INERTIAL;
        final OrbitRelativeFrame ntwRotating  = OrbitRelativeFrame.NTW_ROTATING;
        final OrbitRelativeFrame ntwInertial  = OrbitRelativeFrame.NTW_INERTIAL;
        final OrbitRelativeFrame pqwInertial  = OrbitRelativeFrame.PQW_INERTIAL;
        final OrbitRelativeFrame rswRotating  = OrbitRelativeFrame.RSW_ROTATING;
        final OrbitRelativeFrame rswInertial  = OrbitRelativeFrame.RSW_INERTIAL;
        final OrbitRelativeFrame rsw          = OrbitRelativeFrame.RSW;
        final OrbitRelativeFrame ric          = OrbitRelativeFrame.RIC;
        final OrbitRelativeFrame rtn          = OrbitRelativeFrame.RTN;
        final OrbitRelativeFrame qsw          = OrbitRelativeFrame.QSW;
        final OrbitRelativeFrame tnwRotating  = OrbitRelativeFrame.TNW_ROTATING;
        final OrbitRelativeFrame tnwInertial  = OrbitRelativeFrame.TNW_INERTIAL;
        final OrbitRelativeFrame tnw          = OrbitRelativeFrame.TNW;
        final OrbitRelativeFrame sezRotating  = OrbitRelativeFrame.SEZ_ROTATING;
        final OrbitRelativeFrame sezInertial  = OrbitRelativeFrame.SEZ_INERTIAL;
        final OrbitRelativeFrame vncRotating  = OrbitRelativeFrame.VNC_ROTATING;
        final OrbitRelativeFrame vncInertial  = OrbitRelativeFrame.VNC_INERTIAL;

        // When
        final LOFType eqwInertialLOF  = eqwInertial.getLofType();
        final LOFType lvlhRotatingLOF = lvlhRotating.getLofType();
        final LOFType lvlhInertialLOF = lvlhInertial.getLofType();
        final LOFType lvlhLOF         = lvlh.getLofType();
        final LOFType nswRotatingLOF  = nswRotating.getLofType();
        final LOFType nswInertialLOF  = nswInertial.getLofType();
        final LOFType ntwRotatingLOF  = ntwRotating.getLofType();
        final LOFType ntwInertialLOF  = ntwInertial.getLofType();
        final LOFType pqwInertialLOF  = pqwInertial.getLofType();
        final LOFType rswRotatingLOF  = rswRotating.getLofType();
        final LOFType rswInertialLOF  = rswInertial.getLofType();
        final LOFType rswLOF          = rsw.getLofType();
        final LOFType ricLOF          = ric.getLofType();
        final LOFType rtnLOF          = rtn.getLofType();
        final LOFType qswLOF          = qsw.getLofType();
        final LOFType tnwRotatingLOF  = tnwRotating.getLofType();
        final LOFType tnwInertialLOF  = tnwInertial.getLofType();
        final LOFType tnwLOF          = tnw.getLofType();
        final LOFType sezRotatingLOF  = sezRotating.getLofType();
        final LOFType sezInertialLOF  = sezInertial.getLofType();
        final LOFType vncRotatingLOF  = vncRotating.getLofType();
        final LOFType vncInertialLOF  = vncInertial.getLofType();

        // Then
        assertEquals(eqwInertialLOF.isQuasiInertial(), eqwInertialLOF.isQuasiInertial());
        assertEquals(lvlhRotating.isQuasiInertial(), lvlhRotatingLOF.isQuasiInertial());
        assertEquals(lvlhInertial.isQuasiInertial(), lvlhInertialLOF.isQuasiInertial());
        assertEquals(lvlh.isQuasiInertial(), lvlhLOF.isQuasiInertial());
        assertNull(nswRotatingLOF);
        assertNull(nswInertialLOF);
        assertEquals(ntwRotating.isQuasiInertial(), ntwRotatingLOF.isQuasiInertial());
        assertEquals(ntwInertialLOF.isQuasiInertial(), ntwInertialLOF.isQuasiInertial());
        assertNull(pqwInertialLOF);
        assertEquals(rswRotating.isQuasiInertial(), rswRotatingLOF.isQuasiInertial());
        assertEquals(rswInertial.isQuasiInertial(), rswInertialLOF.isQuasiInertial());
        assertEquals(rsw.isQuasiInertial(), rswLOF.isQuasiInertial());
        assertEquals(ric.isQuasiInertial(), ricLOF.isQuasiInertial());
        assertEquals(rtn.isQuasiInertial(), rtnLOF.isQuasiInertial());
        assertEquals(qsw.isQuasiInertial(), qswLOF.isQuasiInertial());
        assertEquals(tnwRotating.isQuasiInertial(), tnwRotatingLOF.isQuasiInertial());
        assertEquals(tnwInertial.isQuasiInertial(), tnwInertialLOF.isQuasiInertial());
        assertEquals(tnw.isQuasiInertial(), tnwLOF.isQuasiInertial());
        assertNull(sezRotatingLOF);
        assertNull(sezInertialLOF);
        assertEquals(vncRotating.isQuasiInertial(), vncRotatingLOF.isQuasiInertial());
        assertEquals(vncInertial.isQuasiInertial(), vncInertialLOF.isQuasiInertial());
    }

    @Test
    @DisplayName("test conversion from/to orbit relative frame to/from orekit local orbital frame")
    void testConversionFromToOrbitRelativeFrameToFromLOFType() {
        for (OrbitRelativeFrame ccsdsFrame : OrbitRelativeFrame.values()) {
            final LOFType orekitEquivalentLOF = ccsdsFrame.getLofType();

            // Bypass cases where there is no Orekit equivalent
            if (orekitEquivalentLOF != null) {
                // Assert that we come back to the initial CCSDS frame
                final OrbitRelativeFrame ccsdsFrameFromOrekitLOF = orekitEquivalentLOF.toOrbitRelativeFrame();

                // Filter out special cases for RTN local orbital frame
                if (!isRTNNotInertialEquivalent(ccsdsFrame.name(), ccsdsFrameFromOrekitLOF.name())) {
                    // Cases where we start from a "_ROTATING" enum and go back to the equivalent version without "_ROTATION" are
                    // necessary equivalent
                    if (!(ccsdsFrameFromOrekitLOF.name() + "_ROTATING").equals(ccsdsFrame.name())) {
                        assertEquals(ccsdsFrame, ccsdsFrameFromOrekitLOF);
                    }
                }

            }
        }
    }

    private boolean isRTNNotInertialEquivalent(final String ccsdsFrameName, final String ccsdsFrameFromOrekitLOFName) {
        return !ccsdsFrameFromOrekitLOFName.contains("_INERTIAL") && ccsdsFrameFromOrekitLOFName.contains("QSW")
                && (ccsdsFrameName.contains("RSW") || ccsdsFrameName.contains("RTN") || ccsdsFrameName.contains("RIC"));
    }

    @Test
    @DisplayName("test error thrown when trying to convert LVLH LOFType to its OrbitRelativeFrame equivalent")
    void testErrorThrownWhenTryingToConvertLVLHLOFTypeToOrbitRelativeFrame() {
        // Given
        final LOFType lvlh         = LOFType.LVLH;
        final LOFType lvlhInertial = LOFType.LVLH_INERTIAL;

        // When
        final Exception firstException  = assertThrows(OrekitException.class, lvlh::toOrbitRelativeFrame);
        final Exception secondException = assertThrows(OrekitException.class, lvlhInertial::toOrbitRelativeFrame);

        // Then
        final String expectedErrorMessage =
                "this LVLH local orbital frame uses a different definition, please use LVLH_CCSDS instead";

        assertEquals(expectedErrorMessage, firstException.getMessage());
        assertEquals(expectedErrorMessage, secondException.getMessage());

    }
}
