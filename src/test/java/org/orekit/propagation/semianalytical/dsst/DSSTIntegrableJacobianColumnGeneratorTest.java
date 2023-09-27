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
package org.orekit.propagation.semianalytical.dsst;

import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.HarrisPriester;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.io.IOException;
import java.text.ParseException;

/** Unit tests for {@link DSSTIntegrableJacobianColumnGenerator}. */
public class DSSTIntegrableJacobianColumnGeneratorTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
    }

    @Test
    public void testDragParametersDerivatives() throws ParseException, IOException {
        doTestParametersDerivatives(DragSensitive.DRAG_COEFFICIENT,
                                    2.4e-3,
                                    PropagationType.MEAN,
                                    OrbitType.EQUINOCTIAL);
    }

    @Test
    public void testMuParametersDerivatives() throws ParseException, IOException {
        doTestParametersDerivatives(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                    5.e-3,
                                    PropagationType.MEAN,
                                    OrbitType.EQUINOCTIAL);
    }

    private void doTestParametersDerivatives(String parameterName, double tolerance,
                                             PropagationType type,
                                             OrbitType... orbitTypes) {

        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);

        DSSTForceModel drag = new DSSTAtmosphericDrag(new HarrisPriester(CelestialBodyFactory.getSun(), earth),
                                                      new IsotropicDrag(2.5, 1.2),
                                                      provider.getMu());

        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                         4, 4, 4, 8, 4, 4, 2);
        final DSSTForceModel zonal = new DSSTZonal(provider, 4, 3, 9);

        Orbit baseOrbit =
                new KeplerianOrbit(7000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   provider.getMu());

        double dt = 900;
        double dP = 1.0;
        for (OrbitType orbitType : orbitTypes) {
            final Orbit initialOrbit = orbitType.convertType(baseOrbit);

            DSSTPropagator propagator =
                            setUpPropagator(type, initialOrbit, dP, orbitType, zonal, tesseral, drag);
            propagator.setMu(provider.getMu());
            for (final DSSTForceModel forceModel : propagator.getAllForceModels()) {
                for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                    driver.setValue(driver.getReferenceValue());
                    driver.setSelected(driver.getName().equals(parameterName));
                }
            }

            final SpacecraftState initialState = new SpacecraftState(initialOrbit);
            propagator.setInitialState(initialState, PropagationType.MEAN);
            PickUpHandler pickUp = new PickUpHandler(propagator, null, null, DragSensitive.DRAG_COEFFICIENT);
            propagator.setStepHandler(pickUp);
            propagator.propagate(initialState.getDate().shiftedBy(dt));

            // compute reference Jacobian using finite differences
            double[][] dYdPRef = new double[6][1];
            DSSTPropagator propagator2 = setUpPropagator(type, initialOrbit, dP, orbitType, zonal, tesseral, drag);
            propagator2.setMu(provider.getMu());
            ParameterDriversList bound = new ParameterDriversList();
            for (final DSSTForceModel forceModel : propagator2.getAllForceModels()) {
                for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                    if (driver.getName().equals(parameterName)) {
                        driver.setSelected(true);
                        bound.add(driver);
                    } else {
                        driver.setSelected(false);
                    }
                }
            }
            ParameterDriver selected = bound.getDrivers().get(0);
            double p0 = selected.getReferenceValue();
            double h  = selected.getScale();
            selected.setValue(p0 - 4 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sM4h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 - 3 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sM3h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 - 2 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sM2h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 - 1 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sM1h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 + 1 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sP1h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 + 2 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sP2h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 + 3 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sP3h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 + 4 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sP4h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            fillJacobianColumn(dYdPRef, 0, orbitType, h,
                               sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);

            for (int i = 0; i < 6; ++i) {
                Assertions.assertEquals(dYdPRef[i][0], pickUp.getdYdP().getEntry(i, 0), FastMath.abs(dYdPRef[i][0] * tolerance));
            }

        }

    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, double h,
                                    SpacecraftState sM4h, SpacecraftState sM3h,
                                    SpacecraftState sM2h, SpacecraftState sM1h,
                                    SpacecraftState sP1h, SpacecraftState sP2h,
                                    SpacecraftState sP3h, SpacecraftState sP4h) {
        double[] aM4h = stateToArray(sM4h, orbitType)[0];
        double[] aM3h = stateToArray(sM3h, orbitType)[0];
        double[] aM2h = stateToArray(sM2h, orbitType)[0];
        double[] aM1h = stateToArray(sM1h, orbitType)[0];
        double[] aP1h = stateToArray(sP1h, orbitType)[0];
        double[] aP2h = stateToArray(sP2h, orbitType)[0];
        double[] aP3h = stateToArray(sP3h, orbitType)[0];
        double[] aP4h = stateToArray(sP4h, orbitType)[0];
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (aP4h[i] - aM4h[i]) +
                                    32 * (aP3h[i] - aM3h[i]) -
                                   168 * (aP2h[i] - aM2h[i]) +
                                   672 * (aP1h[i] - aM1h[i])) / (840 * h);
        }
    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType) {
          double[][] array = new double[2][6];

          orbitType.mapOrbitToArray(state.getOrbit(), PositionAngleType.MEAN, array[0], array[1]);
          return array;
      }

      private SpacecraftState arrayToState(double[][] array, OrbitType orbitType,
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
          EquinoctialOrbit orbit = (EquinoctialOrbit) orbitType.mapArrayToOrbit(array[0], array[1], PositionAngleType.MEAN, date, mu, frame);
          return new SpacecraftState(orbit, attitude);
      }

    private DSSTPropagator setUpPropagator(PropagationType type, Orbit orbit, double dP,
                                           OrbitType orbitType,
                                           DSSTForceModel... models) {

        final double minStep = 6000.0;
        final double maxStep = 86400.0;

        double[][] tol = NumericalPropagator.tolerances(dP, orbit, orbitType);
        DSSTPropagator propagator =
            new DSSTPropagator(new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]), type);
        for (DSSTForceModel model : models) {
            propagator.addForceModel(model);
        }
        return propagator;
    }

}
