/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.integration;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;


public class FieldIntegratedEphemerisTest {

    @Test
    public void testNormalKeplerIntegration() {
        doTestNormalKeplerIntegration(Decimal64Field.getInstance());
    }

    @Test
    public void testGetFrame() {
        doTestGetFrame(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestNormalKeplerIntegration(Field<T> field) {
        FieldOrbit<T> initialOrbit = createOrbit(field);
        FieldNumericalPropagator<T> numericalPropagator = createPropagator(field);
        // Keplerian propagator definition
        FieldKeplerianPropagator<T> keplerEx = new FieldKeplerianPropagator<>(initialOrbit);

        // Integrated ephemeris

        // Propagation
        FieldAbsoluteDate<T> finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.setInitialState(new FieldSpacecraftState<>(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assert.assertTrue(numericalPropagator.getCalls() < 3200);
        FieldBoundedPropagator<T> ephemeris = numericalPropagator.getGeneratedEphemeris();

        // tests
        for (int i = 1; i <= Constants.JULIAN_DAY; i++) {
            FieldAbsoluteDate<T> intermediateDate = initialOrbit.getDate().shiftedBy(i);
            FieldSpacecraftState<T> keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
            FieldSpacecraftState<T> numericIntermediateOrbit = ephemeris.propagate(intermediateDate);
            FieldVector3D<T> kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
            FieldVector3D<T> numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();

            Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm().getReal(), 0.06);
        }

        // test inv
        FieldAbsoluteDate<T> intermediateDate = initialOrbit.getDate().shiftedBy(41589);
        FieldSpacecraftState<T> keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
        FieldSpacecraftState<T> state = keplerEx.propagate(finalDate);
        numericalPropagator.setInitialState(state);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.propagate(initialOrbit.getDate());
        FieldBoundedPropagator<T> invEphemeris = numericalPropagator.getGeneratedEphemeris();
        FieldSpacecraftState<T> numericIntermediateOrbit = invEphemeris.propagate(intermediateDate);
        FieldVector3D<T> kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
        FieldVector3D<T> numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();

        Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm().getReal(), 10e-2);

    }

    private <T extends RealFieldElement<T>>  void doTestGetFrame(Field<T> field) {
        FieldOrbit<T> initialOrbit = createOrbit(field);
        FieldNumericalPropagator<T> numericalPropagator = createPropagator(field);
        // setup
        FieldAbsoluteDate<T> finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.setInitialState(new FieldSpacecraftState<>(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assert.assertTrue(numericalPropagator.getCalls() < 3200);
        FieldBoundedPropagator<T> ephemeris = numericalPropagator.getGeneratedEphemeris();

        //action
        Assert.assertNotNull(ephemeris.getFrame());
        Assert.assertSame(ephemeris.getFrame(), numericalPropagator.getFrame());
    }

    @Before
    public void setUp() {
        mu = 3.9860047e14;
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
    }

    private <T extends RealFieldElement<T>> FieldNumericalPropagator<T> createPropagator(Field<T> field) {
        double[] absTolerance= {
            0.0001, 1.0e-11, 1.0e-11, 1.0e-8, 1.0e-8, 1.0e-8, 0.001
        };
        double[] relTolerance = {
            1.0e-8, 1.0e-8, 1.0e-8, 1.0e-9, 1.0e-9, 1.0e-9, 1.0e-7
        };
        OrbitType type = OrbitType.EQUINOCTIAL;
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 500, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(100));
        FieldNumericalPropagator<T> numericalPropagator = new FieldNumericalPropagator<>(field, integrator);
        numericalPropagator.setOrbitType(type);
        return numericalPropagator;
    }

    private <T extends RealFieldElement<T>> FieldOrbit<T> createOrbit(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        double mu = 3.9860047e14;
        FieldAbsoluteDate<T> initDate = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        return new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                           FramesFactory.getEME2000(), initDate, zero.add(mu));
    }

    double mu;
}
