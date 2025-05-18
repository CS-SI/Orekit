/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.*;

class LinearKeplerianCovarianceMapperTest {

    @ParameterizedTest
    @EnumSource(OrbitType.class)
    void testMapIdentity(final OrbitType orbitType) {
        // GIVEN
        final Orbit equinoctialOrbit = new EquinoctialOrbit(7e6, 0.01, 0.001, 1, 2, 3, PositionAngleType.TRUE, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final Orbit orbit = orbitType.convertType(equinoctialOrbit);
        final RealMatrix covarianceMatrix = MatrixUtils.createRealIdentityMatrix(6);
        final LOFType lofType = LOFType.QSW;
        final StateCovariance covariance = new StateCovariance(covarianceMatrix, orbit.getDate(), lofType);
        final LinearKeplerianCovarianceMapper mapper = new LinearKeplerianCovarianceMapper(orbit, covariance);
        // WHEN
        final StateCovariance mappedCovariance = mapper.map(orbit);
        // THEN
        assertEquals(lofType, mappedCovariance.getLOF());
        for (int i = 0; i < covarianceMatrix.getRowDimension(); i++) {
            assertArrayEquals(covarianceMatrix.getRow(i), mappedCovariance.getMatrix().getRow(i), 1e-5);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {1e2, 1e3, 1e4, 1e5})
    void testMapEquinoctial() {
        // GIVEN
        final double dt = 1e2;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final EquinoctialOrbit orbit = new EquinoctialOrbit(7e6, 0.01, 0.001, 1, 2, 3, positionAngleType, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final RealMatrix covarianceMatrix = MatrixUtils.createRealIdentityMatrix(6).scalarMultiply(1e-2);
        covarianceMatrix.setEntry(0, 0, 10);
        final StateCovariance covariance = new StateCovariance(covarianceMatrix, orbit.getDate(), orbit.getFrame(),
                orbit.getType(), orbit.getCachedPositionAngleType());
        final LinearKeplerianCovarianceMapper mapper = new LinearKeplerianCovarianceMapper(orbit, covariance);
        final Orbit shitedOrbit = orbit.shiftedBy(dt);
        // WHEN
        final StateCovariance mappedCovariance = mapper.map(shitedOrbit);
        // THEN
        final RealMatrix stm = MatrixUtils.createRealIdentityMatrix(6);
        final double contribution = orbit.getMeanAnomalyDotWrtA() * dt;
        stm.setEntry(5, 0, contribution);
        final RealMatrix expectedCovarianceMatrix = stm.multiply(covarianceMatrix.multiplyTransposed(stm));
        assertEquals(shitedOrbit.getDate(), mappedCovariance.getDate());
        assertEquals(shitedOrbit.getFrame(), mappedCovariance.getFrame());
        assertEquals(OrbitType.EQUINOCTIAL, mappedCovariance.getOrbitType());

        assertArrayEquals(expectedCovarianceMatrix.getRow(0), mappedCovariance.getMatrix().getRow(0), 1e-2);
        for (int i = 1; i < covarianceMatrix.getRowDimension(); i++) {
            assertArrayEquals(expectedCovarianceMatrix.getRow(i), mappedCovariance.getMatrix().getRow(i), 1e-7);
        }
    }
}
