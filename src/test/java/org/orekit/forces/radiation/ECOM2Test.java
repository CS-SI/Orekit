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
package org.orekit.forces.radiation;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.io.FileNotFoundException;
import java.text.ParseException;

public class ECOM2Test extends AbstractForceModelTest {

    private static final AttitudeProvider DEFAULT_LAW = Utils.defaultLaw();

    @BeforeEach
    public void setUp() throws OrekitException {
        Utils.setDataRoot("potential/shm-format:regular-data");
    }

    private NumericalPropagator setUpPropagator(Orbit orbit, double dP,
                                                OrbitType orbitType, PositionAngleType angleType,
                                                ForceModel... models)
        {

        final double minStep = 0.001;
        final double maxStep = 1000;

        double[][] tol = NumericalPropagator.tolerances(dP, orbit, orbitType);
        NumericalPropagator propagator =
            new NumericalPropagator(new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]));
        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(angleType);
        for (ForceModel model : models) {
            propagator.addForceModel(model);
        }
        return propagator;
    }

    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType, PositionAngleType angleType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType, angleType, true);
        array[0][column] += delta;

        return arrayToState(array, orbitType, angleType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }

    private SpacecraftState arrayToState(double[][] array, OrbitType orbitType, PositionAngleType angleType,
                                         Frame frame, AbsoluteDate date, double mu,
                                         Attitude attitude) {
        Orbit orbit = orbitType.mapArrayToOrbit(array[0], array[1], angleType, date, mu, frame);
        return (array.length > 6) ?
               new SpacecraftState(orbit, attitude) :
               new SpacecraftState(orbit, attitude, array[0][6]);
    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType, PositionAngleType angleType,
                                    boolean withMass) {
          double[][] array = new double[2][withMass ? 7 : 6];
          orbitType.mapOrbitToArray(state.getOrbit(), angleType, array[0], array[1]);
          if (withMass) {
              array[0][6] = state.getMass();
          }
          return array;
    }

    private void fillJacobianModelColumn(double[][] jacobian, int column,
                                         OrbitType orbitType, PositionAngleType angleType, double h,
                                         Vector3D sM4h, Vector3D sM3h,
                                         Vector3D sM2h, Vector3D sM1h,
                                         Vector3D sP1h, Vector3D sP2h,
                                         Vector3D sP3h, Vector3D sP4h) {

        jacobian[0][column] = ( -3 * (sP4h.getX() - sM4h.getX()) +
                        32 * (sP3h.getX() - sM3h.getX()) -
                       168 * (sP2h.getX() - sM2h.getX()) +
                       672 * (sP1h.getX() - sM1h.getX())) / (840 * h);
        jacobian[1][column] = ( -3 * (sP4h.getY() - sM4h.getY()) +
                        32 * (sP3h.getY() - sM3h.getY()) -
                       168 * (sP2h.getY() - sM2h.getY()) +
                       672 * (sP1h.getY() - sM1h.getY())) / (840 * h);
        jacobian[2][column] = ( -3 * (sP4h.getZ() - sM4h.getZ()) +
                        32 * (sP3h.getZ() - sM3h.getZ()) -
                       168 * (sP2h.getZ() - sM2h.getZ()) +
                       672 * (sP1h.getZ() - sM1h.getZ())) / (840 * h);
    }

    @Test
    public void testJacobianModelMatrix() {
        final DSFactory factory = new DSFactory(6, 1);
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(2, 0);
        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);

        //Values for the orbit definition
        final DerivativeStructure x         = factory.variable(0, -2747600.0);
        final DerivativeStructure y         = factory.variable(1, 22572100.0);
        final DerivativeStructure z         = factory.variable(2, 13522760.0);
        final DerivativeStructure xDot      = factory.variable(3,  -2729.5);
        final DerivativeStructure yDot      = factory.variable(4, 1142.7);
        final DerivativeStructure zDot      = factory.variable(5, -2523.9);

        //Build Orbit and spacecraft state
        final Field<DerivativeStructure>                field   = x.getField();
        final DerivativeStructure                       one     = field.getOne();
        final FieldVector3D<DerivativeStructure>        pos     = new FieldVector3D<DerivativeStructure>(x, y, z);
        final FieldVector3D<DerivativeStructure>        vel     = new FieldVector3D<DerivativeStructure>(xDot, yDot, zDot);
        final FieldPVCoordinates<DerivativeStructure>   dsPV    = new FieldPVCoordinates<DerivativeStructure>(pos, vel);
        final FieldAbsoluteDate<DerivativeStructure>    dsDate  = new FieldAbsoluteDate<>(field, new AbsoluteDate(2003, 2, 13, 2, 31, 30, TimeScalesFactory.getUTC()));
        final DerivativeStructure                       mu      = one.multiply(Constants.EGM96_EARTH_MU);
        final FieldOrbit<DerivativeStructure>           dsOrbit = new FieldCartesianOrbit<DerivativeStructure>(dsPV, FramesFactory.getEME2000(), dsDate, mu);
        final FieldSpacecraftState<DerivativeStructure> dsState = new FieldSpacecraftState<DerivativeStructure>(dsOrbit);

        //Build the forceModel
        final ECOM2 forceModel = new ECOM2(2, 2, 1e-7, CelestialBodyFactory.getSun(), Constants.EGM96_EARTH_EQUATORIAL_RADIUS);

        //Compute acceleration with state derivatives
        final FieldVector3D<DerivativeStructure> acc = forceModel.acceleration(dsState, forceModel.getParameters(field));
        final double[] accX = acc.getX().getAllDerivatives();
        final double[] accY = acc.getY().getAllDerivatives();
        final double[] accZ = acc.getZ().getAllDerivatives();

        //Build the non-field element from the field element
        final Orbit           orbit     = dsOrbit.toOrbit();
        final SpacecraftState state     = dsState.toSpacecraftState();
        final double[][] refDeriv = new double[3][6];
        final OrbitType     orbitType = orbit.getType();
        final PositionAngleType angleType = PositionAngleType.MEAN;
        double dP = 0.001;
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        AbstractIntegratedPropagator propagator = setUpPropagator(orbit, dP, orbitType, angleType, gravityField, forceModel);

        //Compute derivatives with finite-difference method
        for(int i = 0; i < 6; i++) {
            propagator.resetInitialState(shiftState(state, orbitType, angleType, -4 * steps[i], i));
            SpacecraftState sM4h = propagator.propagate(state.getDate());
            Vector3D accM4 = forceModel.acceleration(sM4h, forceModel.getParameters()); 
            
            propagator.resetInitialState(shiftState(state, orbitType, angleType, -3 * steps[i], i));
            SpacecraftState sM3h = propagator.propagate(state.getDate());
            Vector3D accM3 = forceModel.acceleration(sM3h, forceModel.getParameters()); 
            
            propagator.resetInitialState(shiftState(state, orbitType, angleType, -2 * steps[i], i));
            SpacecraftState sM2h = propagator.propagate(state.getDate());
            Vector3D accM2 = forceModel.acceleration(sM2h, forceModel.getParameters()); 
 
            propagator.resetInitialState(shiftState(state, orbitType, angleType, -1 * steps[i] , i));
            SpacecraftState sM1h = propagator.propagate(state.getDate());
            Vector3D accM1 = forceModel.acceleration(sM1h, forceModel.getParameters()); 
           
            propagator.resetInitialState(shiftState(state, orbitType, angleType, 1 * steps[i], i));
            SpacecraftState  sP1h = propagator.propagate(state.getDate());
            Vector3D accP1 = forceModel.acceleration(sP1h, forceModel.getParameters()); 
            
            propagator.resetInitialState(shiftState(state, orbitType, angleType, 2 * steps[i], i));
            SpacecraftState sP2h = propagator.propagate(state.getDate());
            Vector3D accP2 = forceModel.acceleration(sP2h, forceModel.getParameters()); 
            
            propagator.resetInitialState(shiftState(state, orbitType, angleType, 3 * steps[i], i));
            SpacecraftState sP3h = propagator.propagate(state.getDate());
            Vector3D accP3 = forceModel.acceleration(sP3h, forceModel.getParameters()); 
            
            propagator.resetInitialState(shiftState(state, orbitType, angleType, 4 * steps[i], i));
            SpacecraftState sP4h = propagator.propagate(state.getDate());
            Vector3D accP4 = forceModel.acceleration(sP4h, forceModel.getParameters()); 
            fillJacobianModelColumn(refDeriv, i, orbitType, angleType, steps[i],
                               accM4, accM3, accM2, accM1,
                               accP1, accP2, accP3, accP4);
        }

        //Compare state derivatives with finite-difference ones.
        for (int i = 0; i < 6; i++) {
            final double errorX = (accX[i + 1] - refDeriv[0][i]) / refDeriv[0][i];
            Assertions.assertEquals(0, errorX, 1.0e-10);
            final double errorY = (accY[i + 1] - refDeriv[1][i]) / refDeriv[1][i];
            Assertions.assertEquals(0, errorY, 1.5e-10);
            final double errorZ = (accZ[i + 1] - refDeriv[2][i]) / refDeriv[2][i];
            Assertions.assertEquals(0, errorZ, 1.0e-10);
        }

    }

    @Test
    public void testRealField() {

        // Initial field Keplerian orbit
        // The variables are the six orbital parameters
        DSFactory factory = new DSFactory(6, 4);
        DerivativeStructure a_0 = factory.variable(0, 7e6);
        DerivativeStructure e_0 = factory.variable(1, 0.01);
        DerivativeStructure i_0 = factory.variable(2, 1.2);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        // Initial date = J2000 epoch
        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        // J2000 frame
        Frame EME = FramesFactory.getEME2000();

        // Create initial field Keplerian orbit
        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        // Initial field and classical S/Cs
        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);
        SpacecraftState iSR = initialState.toSpacecraftState();

        // Field integrator and classical integrator
        ClassicalRungeKuttaFieldIntegrator<DerivativeStructure> integrator =
                        new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(6));
        ClassicalRungeKuttaIntegrator RIntegrator =
                        new ClassicalRungeKuttaIntegrator(6);
        OrbitType type = OrbitType.EQUINOCTIAL;

        // Field and classical numerical propagators
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        // Set up the force model to test
        final ECOM2 forceModel = new ECOM2(0, 0, 1e-7, CelestialBodyFactory.getSun(), Constants.EGM96_EARTH_EQUATORIAL_RADIUS);

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        checkRealFieldPropagation(FKO, PositionAngleType.MEAN, 300., NP, FNP,
                                  1.0e-30, 1.3e-8, 6.7e-11, 1.4e-10,
                                  1, false);
    }

    @Test
    public void testRealFieldGradient() {

        // Initial field Keplerian orbit
        // The variables are the six orbital parameters
        int freeParameters = 6;
        Gradient a_0 = Gradient.variable(freeParameters, 0, 7e6);
        Gradient e_0 = Gradient.variable(freeParameters, 1, 0.01);
        Gradient i_0 = Gradient.variable(freeParameters, 2, 1.2);
        Gradient R_0 = Gradient.variable(freeParameters, 3, 0.7);
        Gradient O_0 = Gradient.variable(freeParameters, 4, 0.5);
        Gradient n_0 = Gradient.variable(freeParameters, 5, 0.1);

        Field<Gradient> field = a_0.getField();
        Gradient zero = field.getZero();

        // Initial date = J2000 epoch
        FieldAbsoluteDate<Gradient> J2000 = new FieldAbsoluteDate<>(field);

        // J2000 frame
        Frame EME = FramesFactory.getEME2000();

        // Create initial field Keplerian orbit
        FieldKeplerianOrbit<Gradient> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                      PositionAngleType.MEAN,
                                                                      EME,
                                                                      J2000,
                                                                      zero.add(Constants.EIGEN5C_EARTH_MU));

        // Initial field and classical S/Cs
        FieldSpacecraftState<Gradient> initialState = new FieldSpacecraftState<>(FKO);
        SpacecraftState iSR = initialState.toSpacecraftState();

        // Field integrator and classical integrator
        ClassicalRungeKuttaFieldIntegrator<Gradient> integrator =
                        new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(6));
        ClassicalRungeKuttaIntegrator RIntegrator =
                        new ClassicalRungeKuttaIntegrator(6);
        OrbitType type = OrbitType.EQUINOCTIAL;

        // Field and classical numerical propagators
        FieldNumericalPropagator<Gradient> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        // Set up the force model to test
        final ECOM2 forceModel = new ECOM2(0, 0, 1e-7, CelestialBodyFactory.getSun(), Constants.EGM96_EARTH_EQUATORIAL_RADIUS);

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, 300., NP, FNP,
                                  1.0e-30, 1.3e-2, 9.6e-5, 1.4e-4,
                                  1, false);
    }

    @Test
    public void testParameterDerivative() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        //Build the forceModel
        final ECOM2 forceModel = new ECOM2(0, 0, 1e-7, CelestialBodyFactory.getSun(), Constants.EGM96_EARTH_EQUATORIAL_RADIUS);

        Assertions.assertFalse(forceModel.dependsOnPositionOnly());

        checkParameterDerivative(state, forceModel, ECOM2.ECOM_COEFFICIENT + " B0", 1.0e-4, 3.0e-12);
        checkParameterDerivative(state, forceModel, ECOM2.ECOM_COEFFICIENT + " D0", 1.0e-4, 3.0e-12);
        checkParameterDerivative(state, forceModel, ECOM2.ECOM_COEFFICIENT + " Y0", 1.0e-4, 3.0e-12);
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

        //Build the forceModel
        final ECOM2 forceModel = new ECOM2(0, 0, 1e-7, CelestialBodyFactory.getSun(), Constants.EGM96_EARTH_EQUATORIAL_RADIUS);

        Assertions.assertFalse(forceModel.dependsOnPositionOnly());

        checkParameterDerivativeGradient(state, forceModel, ECOM2.ECOM_COEFFICIENT + " B0", 1.0e-4, 3.0e-12);
        checkParameterDerivativeGradient(state, forceModel, ECOM2.ECOM_COEFFICIENT + " D0", 1.0e-4, 3.0e-12);
        checkParameterDerivativeGradient(state, forceModel, ECOM2.ECOM_COEFFICIENT + " Y0", 1.0e-4, 3.0e-12);
    }

    @Test
    public void testJacobianVsFiniteDifferences() {

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

        final ECOM2 forceModel = new ECOM2(2, 2, 1e-7, CelestialBodyFactory.getSun(), Constants.EGM96_EARTH_EQUATORIAL_RADIUS);

        SpacecraftState state = new SpacecraftState(orbit,
                                                    DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferences(state, forceModel, DEFAULT_LAW, 1.0, 5.0e-6, false);

    }

    @Test
    public void testJacobianVsFiniteDifferencesGradient() {

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

        final ECOM2 forceModel = new ECOM2(2, 2, 1e-7, CelestialBodyFactory.getSun(), Constants.EGM96_EARTH_EQUATORIAL_RADIUS);

        SpacecraftState state = new SpacecraftState(orbit,
                                                    DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferencesGradient(state, forceModel, DEFAULT_LAW, 1.0, 5.0e-6, false);

    }

    @Test
    public void testRoughOrbitalModifs() throws ParseException, OrekitException, FileNotFoundException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 7, 1),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3,
                                           FastMath.tan(0.001745329)*FastMath.cos(2*FastMath.PI/3),
                                           FastMath.tan(0.001745329)*FastMath.sin(2*FastMath.PI/3),
                                           0.1, PositionAngleType.TRUE, FramesFactory.getEME2000(), date, Constants.WGS84_EARTH_MU);
        final double period = orbit.getKeplerianPeriod();
        Assertions.assertEquals(86164, period, 1);
        ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        // creation of the force model
        OneAxisEllipsoid earth =
            new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final ECOM2 SRP = new ECOM2(2, 2, 1e-7, sun, earth.getEquatorialRadius());

        // creation of the propagator
        double[] absTolerance = {
            0.1, 1.0e-9, 1.0e-9, 1.0e-5, 1.0e-5, 1.0e-5, 0.001
        };
        double[] relTolerance = {
            1.0e-4, 1.0e-4, 1.0e-4, 1.0e-6, 1.0e-6, 1.0e-6, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(900.0, 60000, absTolerance, relTolerance);
        integrator.setInitialStepSize(3600);
        final NumericalPropagator calc = new NumericalPropagator(integrator);
        calc.addForceModel(SRP);

        // Step Handler
        calc.setStepHandler(FastMath.floor(period), new SolarStepHandler());
        AbsoluteDate finalDate = date.shiftedBy(10 * period);
        calc.setInitialState(new SpacecraftState(orbit, 1500.0));
        calc.propagate(finalDate);
        Assertions.assertTrue(calc.getCalls() < 7100);
    }

    @Test
    public void testRealAndFieldComparison() {

        // Orbital parameters from GNSS almanac
        final int freeParameters = 6;
        final Gradient sma  = Gradient.variable(freeParameters, 0, 26559614.1);
        final Gradient ecc  = Gradient.variable(freeParameters, 1, 0.00522136);
        final Gradient inc  = Gradient.variable(freeParameters, 2, 0.963785748);
        final Gradient aop  = Gradient.variable(freeParameters, 3, 0.451712027);
        final Gradient raan = Gradient.variable(freeParameters, 4, -1.159458779);
        final Gradient lm   = Gradient.variable(freeParameters, 4, -2.105941778);

        // Field and zero
        final Field<Gradient> field = sma.getField();

        // Epoch
        final FieldAbsoluteDate<Gradient> epoch = FieldAbsoluteDate.getJ2000Epoch(field);

        // Create a Keplerian orbit
        FieldKeplerianOrbit<Gradient> orbit = new FieldKeplerianOrbit<>(sma, ecc, inc, aop, raan, lm,
                                                                        PositionAngleType.MEAN,
                                                                        FramesFactory.getEME2000(),
                                                                        epoch,
                                                                        field.getZero().add(Constants.EIGEN5C_EARTH_MU));

        // Model
        final ECOM2 forceModel = new ECOM2(2, 2, 1e-7, CelestialBodyFactory.getSun(), Constants.EGM96_EARTH_EQUATORIAL_RADIUS);

        // Field acceleration
        final FieldVector3D<Gradient> accField = forceModel.acceleration(new FieldSpacecraftState<>(orbit), forceModel.getParameters(field, epoch));

        // Real acceleration
        final Vector3D accReal = forceModel.acceleration(new SpacecraftState(orbit.toOrbit()), forceModel.getParameters(epoch.toAbsoluteDate()));

        // Verify
        Assertions.assertEquals(0.0, accReal.distance(accField.toVector3D()), 1.0e-20);

    }

    private static class SolarStepHandler implements OrekitFixedStepHandler {

        public void handleStep(SpacecraftState currentState) {
            final double dex = currentState.getEquinoctialEx() - 0.01071166;
            final double dey = currentState.getEquinoctialEy() - 0.00654848;
            final double alpha = FastMath.toDegrees(FastMath.atan2(dey, dex));
            Assertions.assertTrue(alpha > 100.0);
            Assertions.assertTrue(alpha < 112.0);
            checkRadius(FastMath.sqrt(dex * dex + dey * dey), 0.003469, 0.003529);
        }

    }

    public static void checkRadius(double radius , double min , double max) {
        Assertions.assertTrue(radius >= min);
        Assertions.assertTrue(radius <= max);
    }


}
