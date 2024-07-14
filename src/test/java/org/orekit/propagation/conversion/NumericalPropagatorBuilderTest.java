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

package org.orekit.propagation.conversion;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.List;

import static org.orekit.propagation.conversion.AbstractPropagatorBuilderTest.assertPropagatorBuilderIsACopy;

public class NumericalPropagatorBuilderTest {

    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("regular-data:potential");
    }

    @Test
    void testClone() {

        // Given
        final ODEIntegratorBuilder integratorBuilder = new ClassicalRungeKuttaIntegratorBuilder(60);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(
                new Vector3D(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS + 400000, 0, 0),
                new Vector3D(0, 7668.6, 0)), FramesFactory.getGCRF(),
                new AbsoluteDate(), Constants.EIGEN5C_EARTH_MU);

        final NumericalPropagatorBuilder builder =
                new NumericalPropagatorBuilder(orbit, integratorBuilder, PositionAngleType.MEAN, 1.0, Utils.defaultLaw());

        builder.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                GravityFieldFactory.getNormalizedProvider(2, 0)));

        // When
        final NumericalPropagatorBuilder copyBuilder = (NumericalPropagatorBuilder) builder.clone();

        // Then
        assertNumericalPropagatorBuilderIsACopy(builder, copyBuilder);

    }

    private void assertNumericalPropagatorBuilderIsACopy(final NumericalPropagatorBuilder expected,
                                                         final NumericalPropagatorBuilder actual) {
        assertPropagatorBuilderIsACopy(expected, actual);

        // Assert force models
        final List<ForceModel> expectedForceModelList = expected.getAllForceModels();
        final List<ForceModel> actualForceModelList   = actual.getAllForceModels();
        Assertions.assertEquals(expectedForceModelList.size(), actualForceModelList.size());
        for (int i = 0; i < expectedForceModelList.size(); i++) {
            Assertions.assertEquals(expectedForceModelList.get(i).getClass(), actualForceModelList.get(i).getClass());
        }

        // Assert integrator builder
        Assertions.assertEquals(expected.getIntegratorBuilder().getClass(), actual.getIntegratorBuilder().getClass());

        // Assert mass
        Assertions.assertEquals(expected.getMass(), actual.getMass());
    }

}
