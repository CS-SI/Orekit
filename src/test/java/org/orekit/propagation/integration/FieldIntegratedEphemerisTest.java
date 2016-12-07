/* Copyright 2002-2016 CS Systèmes d'Information
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
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;


public class FieldIntegratedEphemerisTest {

    @Test
    public void test() throws OrekitException{
        testNormalKeplerIntegration(Decimal64Field.getInstance());
    }

    public <T extends RealFieldElement<T>> void testNormalKeplerIntegration(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        FieldAbsoluteDate<T> initDate = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit =
            new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                 FramesFactory.getEME2000(), initDate, mu);
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<T>(field, 0.001, 500, absTolerance, relTolerance);
        integrator.setInitialStepSize(zero.add(100));
        FieldNumericalPropagator<T> numericalPropagator = new FieldNumericalPropagator<T>(field, integrator);
        // Keplerian propagator definition
        FieldKeplerianPropagator<T> keplerEx = new FieldKeplerianPropagator<T>(initialOrbit);

        // Integrated ephemeris

        // Propagation
        FieldAbsoluteDate<T> finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.setInitialState(new FieldSpacecraftState<T>(initialOrbit));
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
//
//    public <T extends RealFieldElement<T>>  void testPartialDerivativesIssue16(Field<T> field) throws OrekitException {
//
//        final String eqName = "derivatives";
//        numericalPropagator.setEphemerisMode();
//        numericalPropagator.setOrbitType(OrbitType.CARTESIAN);
//        final PartialDerivativesEquations derivatives =
//            new PartialDerivativesEquations(eqName, numericalPropagator);
//        final FieldSpacecraftState<T> initialState =
//                derivatives.setInitialJacobians(new FieldSpacecraftState<T>(initialOrbit), 6, 0);
//        final FieldJacobiansMapper<T> mapper = derivatives.getMapper();
//        numericalPropagator.setInitialState(initialState);
//        numericalPropagator.propagate(initialOrbit.getDate().shiftedBy(3600.0));
//        FieldBoundedPropagator<T> ephemeris = numericalPropagator.getGeneratedEphemeris();
//        ephemeris.setMasterMode(new OrekitStepHandler() {
//
//            private final Array2DRowFieldMatrix<T> dYdY0 = new Array2DRowFieldMatrix<T>(6, 6);
//
//            public void handleStep(OrekitStepInterpolator interpolator, boolean isLast)
//                throws OrekitException {
//                FieldSpacecraftState<T> state = interpolator.getCurrentState();
//                Assert.assertEquals(mapper.getAdditionalStateDimension(),
//                                    state.getAdditionalState(eqName).length);
//                mapper.getStateJacobian(state, dYdY0.getDataRef());
//                mapper.getParametersJacobian(state, null); // no parameters, this is a no-op and should work
//                FieldMatrix<T> deltaId = dYdY0.subtract(MatrixUtils.createRealIdentityMatrix(6));
//                Assert.assertTrue(deltaId.getNorm() >  100);
//                Assert.assertTrue(deltaId.getNorm() < 3100);
//            }
//
//        });
//
//        ephemeris.propagate(initialOrbit.getDate().shiftedBy(1800.0));
//
//    }
//
//    public <T extends RealFieldElement<T>>  void testGetFrame(Field<T> field) throws OrekitException {
//        // setup
//        FieldAbsoluteDate<T> finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
//        numericalPropagator.setEphemerisMode();
//        numericalPropagator.setInitialState(new FieldSpacecraftState<T>(initialOrbit));
//        numericalPropagator.propagate(finalDate);
//        Assert.assertTrue(numericalPropagator.getCalls() < 3200);
//        FieldBoundedPropagator<T> ephemeris = numericalPropagator.getGeneratedEphemeris();
//
//        //action
//        Assert.assertNotNull(ephemeris.getFrame());
//        Assert.assertSame(ephemeris.getFrame(), numericalPropagator.getFrame());
//    }

    @Before
    public void setUp() {
        mu = 3.9860047e14;
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
    }
    double[] absTolerance= {
                            0.0001, 1.0e-11, 1.0e-11, 1.0e-8, 1.0e-8, 1.0e-8, 0.001
                        };
    double[] relTolerance = {
        1.0e-8, 1.0e-8, 1.0e-8, 1.0e-9, 1.0e-9, 1.0e-9, 1.0e-7
    };
    private double mu;

}
