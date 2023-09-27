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
package org.orekit.forces.gravity;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

public class DeSitterRelativityTest extends AbstractLegacyForceModelTest {

    /** speed of light */
    private static final double c = Constants.SPEED_OF_LIGHT;
    /** arbitrary date */
    private static final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    @Override
    protected FieldVector3D<DerivativeStructure>
        accelerationDerivatives(ForceModel forceModel, FieldSpacecraftState<DerivativeStructure> state) {

        final DeSitterRelativity model = (DeSitterRelativity) forceModel;

        // Useful constant
        final double c2 = c * c;

        // Sun's gravitational parameter
        final double gm = model.getParametersDrivers().get(0).getValue(state.getDate().toAbsoluteDate());

        // Coordinates of the Earth with respect to the Sun
        final FieldPVCoordinates<DerivativeStructure> pvEarth = model.getEarth().getPVCoordinates(state.getDate(), model.getSun().getInertiallyOrientedFrame());
        final FieldVector3D<DerivativeStructure> pEarth = pvEarth.getPosition();
        final FieldVector3D<DerivativeStructure> vEarth = pvEarth .getVelocity();

        // Radius
        final DerivativeStructure r  = pEarth.getNorm();
        final DerivativeStructure r3 = r.multiply(r).multiply(r);

        // Eq. 10.12
        return new FieldVector3D<>(r3.multiply(c2).reciprocal().multiply(-3.0 * gm),
                        vEarth.crossProduct(pEarth).crossProduct(state.getPVCoordinates().getVelocity()));
    }

    @Override
    protected FieldVector3D<Gradient>
        accelerationDerivativesGradient(ForceModel forceModel,
                                        FieldSpacecraftState<Gradient> state) {

        final DeSitterRelativity model = (DeSitterRelativity) forceModel;

        // Useful constant
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;

        // Sun's gravitational parameter
        final double gm = model.getParametersDrivers().get(0).getValue(state.getDate().toAbsoluteDate());

        // Coordinates of the Earth with respect to the Sun
        final FieldPVCoordinates<Gradient> pvEarth = model.getEarth().getPVCoordinates(state.getDate(), model.getSun().getInertiallyOrientedFrame());
        final FieldVector3D<Gradient> pEarth = pvEarth.getPosition();
        final FieldVector3D<Gradient> vEarth = pvEarth .getVelocity();

        // Radius
        final Gradient r  = pEarth.getNorm();
        final Gradient r3 = r.multiply(r).multiply(r);

        // Eq. 10.12
        return new FieldVector3D<>(r3.multiply(c2).reciprocal().multiply(-3.0 * gm),
                        vEarth.crossProduct(pEarth).crossProduct(state.getPVCoordinates().getVelocity()));
    }

    /**
     * Check against prediction in
     *
     * "C IUFOLINI, Ignazio, MATZNER, Richard, GURZADYAN, Vahe, et al.
     * A new laser-ranged satellite for General Relativityand space geodesy:
     * III. De Sitter effect and the LARES 2 space experiment.
     * The European Physical Journal C, 2017, vol. 77, no 12, p. 819"
     *
     * They predict a precession of the orbital plane at a rate of the order
     * of -19.2 milliarcsecs per year.
     *
     * As this effect is very small, a propagation of several years is needed to
     * find the same value. However, it takes a lot of time to be performed.
     * As a result, we propose a propagation of 60 days for which the impact is
     * equal to -20.2 milliarcsecs per year.
     */
    @Test
    public void testSmallEffectOnOrbit() {
        // Setup
        final double gm = Constants.EIGEN5C_EARTH_MU;
	final Frame frame = FramesFactory.getGCRF();
        Orbit orbit =
                new KeplerianOrbit(7000000.0, 0.01, FastMath.toRadians(80.), FastMath.toRadians(80.), FastMath.toRadians(20.),
                                   FastMath.toRadians(40.), PositionAngleType.MEAN,
                                   frame, date, gm
                );
        double[][] tol = NumericalPropagator.tolerances(0.1, orbit, OrbitType.KEPLERIAN);
        AbstractIntegrator integrator = new DormandPrince853Integrator(1, 3600, tol[0], tol[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.addForceModel(new DeSitterRelativity());
        propagator.setInitialState(new SpacecraftState(orbit));

        // Action: propagate a period
        AbsoluteDate end = orbit.getDate().shiftedBy(60 * Constants.JULIAN_DAY);
        PVCoordinates actual = propagator.getPVCoordinates(end, frame);

        // Verify
        KeplerianOrbit endOrbit = new KeplerianOrbit(actual, frame, end, gm);
        KeplerianOrbit startOrbit = new KeplerianOrbit(orbit);
        double dp = startOrbit.getRightAscensionOfAscendingNode() - endOrbit.getRightAscensionOfAscendingNode();
        double dtYears = end.durationFrom(orbit.getDate()) / Constants.JULIAN_YEAR;
        double dpDeg = FastMath.toDegrees(dp);
        // change in right ascension of the ascending node in milliarcseconds per year
        double milliArcsecPerYear = 1.0e3 * dpDeg * 3600 / dtYears;
        Assertions.assertEquals(-19.2, milliArcsecPerYear, 1.0);
    }

    @Test
    public void testGlobalStateJacobian()
        {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        NumericalPropagator propagator =
                new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                       tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        DeSitterRelativity relativity = new DeSitterRelativity();
        propagator.addForceModel(relativity);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e4, tolerances[0], 2.5e-8);

    }

    @Test
    public void testParameterDerivativeGradient() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        DeSitterRelativity relativity = new DeSitterRelativity();
        Assertions.assertFalse(relativity.dependsOnPositionOnly());
        final String name = relativity.getSun().getName() + ThirdBodyAttraction.ATTRACTION_COEFFICIENT_SUFFIX;
        checkParameterDerivativeGradient(state, relativity, name, 1.0, 1.0e-15);

    }

    @Test
    public void RealFieldGradientTest() {

        final int freeParameters = 6;
        Gradient a_0 = Gradient.variable(freeParameters, 0, 7e7);
        Gradient e_0 = Gradient.variable(freeParameters, 1, 0.4);
        Gradient i_0 = Gradient.variable(freeParameters, 2, 85 * FastMath.PI / 180);
        Gradient R_0 = Gradient.variable(freeParameters, 3, 0.7);
        Gradient O_0 = Gradient.variable(freeParameters, 4, 0.5);
        Gradient n_0 = Gradient.variable(freeParameters, 5, 0.1);
        Gradient mu  = Gradient.constant(freeParameters, Constants.EIGEN5C_EARTH_MU);

        Field<Gradient> field = a_0.getField();

        FieldAbsoluteDate<Gradient> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<Gradient> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                      PositionAngleType.MEAN,
                                                                      EME,
                                                                      J2000,
                                                                      mu);

        FieldSpacecraftState<Gradient> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();
        OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<Gradient> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<Gradient> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        DeSitterRelativity relativity = new DeSitterRelativity();

        FNP.addForceModel(relativity);
        NP.addForceModel(relativity);

        // Do the test
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, 1005., NP, FNP,
                                  1.0e-15, 1.3e-2, 2.9e-4, 1.4e-3,
                                  1, false);
    }

    @Test
    public void testJacobianVs80ImplementationGradient() {
        double gm = Constants.EIGEN5C_EARTH_MU;
        DeSitterRelativity relativity = new DeSitterRelativity();
        final Vector3D p = new Vector3D(3777828.75000531, -5543949.549783845, 2563117.448578311);
        final Vector3D v = new Vector3D(489.0060271721, -2849.9328929417, -6866.4671013153);
        SpacecraftState s = new SpacecraftState(new CartesianOrbit(
                new PVCoordinates(p, v),
                FramesFactory.getGCRF(),
                date,
                gm
        ));

        checkStateJacobianVs80ImplementationGradient(s, relativity,
                                             new LofOffset(s.getFrame(), LOFType.LVLH_CCSDS),
                                             1.0e-15, false);
    }

    @Test
    public void testJacobianVs80Implementation() {
        double gm = Constants.EIGEN5C_EARTH_MU;
        DeSitterRelativity relativity = new DeSitterRelativity();
        final Vector3D p = new Vector3D(3777828.75000531, -5543949.549783845, 2563117.448578311);
        final Vector3D v = new Vector3D(489.0060271721, -2849.9328929417, -6866.4671013153);
        SpacecraftState s = new SpacecraftState(new CartesianOrbit(
                new PVCoordinates(p, v),
                FramesFactory.getGCRF(),
                date,
                gm
        ));

        checkStateJacobianVs80Implementation(s, relativity,
                                             new LofOffset(s.getFrame(), LOFType.LVLH_CCSDS),
                                             1.0e-50, false);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
