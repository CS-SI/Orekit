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

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OccultationEngine;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class SolarRadiationPressureTest extends AbstractLegacyForceModelTest {

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel,
                                                                         final FieldSpacecraftState<DerivativeStructure> state) {
        try {
            final AbsoluteDate                       date     = state.getDate().toAbsoluteDate();
            final FieldVector3D<DerivativeStructure> position = state.getPVCoordinates().getPosition();
            java.lang.reflect.Field kRefField = SolarRadiationPressure.class.getDeclaredField("kRef");
            kRefField.setAccessible(true);
            double kRef = kRefField.getDouble(forceModel);
            java.lang.reflect.Field sunField = SolarRadiationPressure.class.getDeclaredField("sun");
            sunField.setAccessible(true);
            PVCoordinatesProvider sun = (PVCoordinatesProvider) sunField.get(forceModel);
            java.lang.reflect.Field spacecraftField = SolarRadiationPressure.class.getDeclaredField("spacecraft");
            spacecraftField.setAccessible(true);
            RadiationSensitive spacecraft = (RadiationSensitive) spacecraftField.get(forceModel);

            final Field<DerivativeStructure> field = position.getX().getField();
            final FieldVector3D<DerivativeStructure> sunSatVector = position.subtract(sun.getPosition(date, state.getFrame()));
            final DerivativeStructure r2  = sunSatVector.getNormSq();

            // compute flux
            final DerivativeStructure ratio = ((SolarRadiationPressure) forceModel).getLightingRatio(state);
            final DerivativeStructure rawP = ratio.multiply(kRef).divide(r2);
            final FieldVector3D<DerivativeStructure> flux = new FieldVector3D<>(rawP.divide(r2.sqrt()), sunSatVector);

            // compute acceleration with all its partial derivatives
            return spacecraft.radiationPressureAcceleration(state, flux, forceModel.getParameters(field));
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    @Override
    protected FieldVector3D<Gradient> accelerationDerivativesGradient(final ForceModel forceModel,
                                                                      final FieldSpacecraftState<Gradient> state) {
        try {
            final AbsoluteDate                       date     = state.getDate().toAbsoluteDate();
            final FieldVector3D<Gradient> position = state.getPVCoordinates().getPosition();
            java.lang.reflect.Field kRefField = SolarRadiationPressure.class.getDeclaredField("kRef");
            kRefField.setAccessible(true);
            double kRef = kRefField.getDouble(forceModel);
            java.lang.reflect.Field sunField = SolarRadiationPressure.class.getDeclaredField("sun");
            sunField.setAccessible(true);
            PVCoordinatesProvider sun = (PVCoordinatesProvider) sunField.get(forceModel);
            java.lang.reflect.Field spacecraftField = SolarRadiationPressure.class.getDeclaredField("spacecraft");
            spacecraftField.setAccessible(true);
            RadiationSensitive spacecraft = (RadiationSensitive) spacecraftField.get(forceModel);

            final Field<Gradient> field = position.getX().getField();
            final FieldVector3D<Gradient> sunSatVector = position.subtract(sun.getPosition(date, state.getFrame()));
            final Gradient r2  = sunSatVector.getNormSq();

            // compute flux
            final Gradient ratio = ((SolarRadiationPressure) forceModel).getLightingRatio(state);
            final Gradient rawP = ratio.multiply(kRef).divide(r2);
            final FieldVector3D<Gradient> flux = new FieldVector3D<>(rawP.divide(r2.sqrt()), sunSatVector);

            // compute acceleration with all its partial derivatives
            return spacecraft.radiationPressureAcceleration(state, flux, forceModel.getParameters(field));
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    @Test
    public void testLightingInterplanetary() throws ParseException {
        // Initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 3, 21),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit = new KeplerianOrbit(1.0e11, 0.1, 0.2, 0.3, 0.4, 0.5, PositionAngleType.TRUE,
                                         FramesFactory.getICRF(), date, Constants.JPL_SSD_SUN_GM);
        ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        SolarRadiationPressure srp =
            new SolarRadiationPressure(sun, new OneAxisEllipsoid(1.0e-10, 0.0, FramesFactory.getICRF()),
                                       new IsotropicRadiationClassicalConvention(50.0, 0.5, 0.5));
        Assertions.assertFalse(srp.dependsOnPositionOnly());

        // Test lighting ratio method with double
        Assertions.assertEquals(1.0,
                                srp.getLightingRatio(new SpacecraftState(orbit)),
                                1.0e-15);

        // Test lighting ratio method with field
        final FieldSpacecraftState<Binary64> fieldSc = new FieldSpacecraftState<>(Binary64Field.getInstance(),
                        new SpacecraftState(orbit));
        Assertions.assertEquals(1.0,
                                srp.getLightingRatio(fieldSc).getReal(),
                                1.0e-15);
    }

    @Test
    public void testLighting() throws ParseException {

        // Given
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 3, 21),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3,
                                           FastMath.tan(0.001745329)*FastMath.cos(2*FastMath.PI/3), FastMath.tan(0.001745329)*FastMath.sin(2*FastMath.PI/3),
                                           0.1, PositionAngleType.TRUE, FramesFactory.getEME2000(), date, mu);
        CelestialBody sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth =
                        new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                             FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        SolarRadiationPressure SRP =
                        new SolarRadiationPressure(sun, earth,
                                                   new IsotropicRadiationCNES95Convention(50.0, 0.5, 0.5));

        double period = 2*FastMath.PI*FastMath.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/orbit.getMu());
        Assertions.assertEquals(86164, period, 1);

        // creation of the propagator
        KeplerianPropagator k = new KeplerianPropagator(orbit);

        // intermediate variables
        AbsoluteDate currentDate;
        double changed = 1;
        int count=0;

        // When: Test first lighting ration method
        for (int t=1;t<3*period;t+=1000) {
            currentDate = date.shiftedBy(t);
            try {

                double ratio = SRP.getLightingRatio(k.propagate(currentDate));

                if (FastMath.floor(ratio)!=changed) {
                    changed = FastMath.floor(ratio);
                    if (changed == 0) {
                        count++;
                    }
                }
            } catch (OrekitException e) {
                e.printStackTrace();
            }
        }
        Assertions.assertEquals(3, count);
    }

    @Test
    public void testGlobalStateJacobianIsotropicSingle() {

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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                           new IsotropicRadiationSingleCoefficient(2.5, 0.7));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 3.2e-5);

    }

    @Test
    public void testLocalJacobianIsotropicClassicalVs80Implementation() {

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
        final SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                           new IsotropicRadiationClassicalConvention(2.5, 0.7, 0.2));

        checkStateJacobianVs80Implementation(new SpacecraftState(orbit), forceModel,
                                             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS),
                                             1.0e-15, false);

    }

    @Test
    public void testLocalJacobianIsotropicClassicalVs80ImplementationGradient() {

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
        final SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                           new IsotropicRadiationClassicalConvention(2.5, 0.7, 0.2));

        checkStateJacobianVs80ImplementationGradient(new SpacecraftState(orbit), forceModel,
                                             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS),
                                             1.0e-15, false);

    }

    @Test
    public void testLocalJacobianIsotropicClassicalVsFiniteDifferencesFullLight() {
        // here, lighting ratio is exactly 1 for all points used for finite differences
        doTestLocalJacobianIsotropicClassicalVsFiniteDifferences(250.0, 1000.0, 3.0e-8, false);
    }

    @Test
    public void testLocalJacobianIsotropicClassicalVsFiniteDifferencesGradientFullLight() {
        // here, lighting ratio is exactly 1 for all points used for finite differences
        doTestLocalJacobianIsotropicClassicalVsFiniteDifferencesGradient(250.0, 1000.0, 3.0e-8, false);
    }

    @Test
    public void testLocalJacobianIsotropicClassicalVsFiniteDifferencesPenumbra() {
        // here, lighting ratio is about 0.57,
        // and remains strictly between 0 and 1 for all points used for finite differences
        doTestLocalJacobianIsotropicClassicalVsFiniteDifferences(275.5, 100.0, 8.0e-7, false);
    }

    @Test
    public void testLocalJacobianIsotropicClassicalVsFiniteDifferencesGradientPenumbra() {
        // here, lighting ratio is about 0.57,
        // and remains strictly between 0 and 1 for all points used for finite differences
        doTestLocalJacobianIsotropicClassicalVsFiniteDifferencesGradient(275.5, 100.0, 8.0e-7, false);
    }

    @Test
    public void testLocalJacobianIsotropicClassicalVsFiniteDifferencesEclipse() {
        // here, lighting ratio is exactly 0 for all points used for finite differences
        doTestLocalJacobianIsotropicClassicalVsFiniteDifferences(300.0, 1000.0, 1.0e-50, false);
    }

    @Test
    public void testLocalJacobianIsotropicClassicalVsFiniteDifferencesGradientEclipse() {
        // here, lighting ratio is exactly 0 for all points used for finite differences
        doTestLocalJacobianIsotropicClassicalVsFiniteDifferencesGradient(300.0, 1000.0, 1.0e-50, false);
    }

    private void doTestLocalJacobianIsotropicClassicalVsFiniteDifferences(double deltaT, double dP,
                                                                          double checkTolerance,
                                                                          boolean print) {

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
        final SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                           new IsotropicRadiationClassicalConvention(2.5, 0.7, 0.2));

        checkStateJacobianVsFiniteDifferences(new SpacecraftState(orbit.shiftedBy(deltaT)), forceModel,
                                              Utils.defaultLaw(), dP, checkTolerance, print);

    }

    private void doTestLocalJacobianIsotropicClassicalVsFiniteDifferencesGradient(double deltaT, double dP,
                                                                                  double checkTolerance,
                                                                                  boolean print) {

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
        final SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                           new IsotropicRadiationClassicalConvention(2.5, 0.7, 0.2));

        checkStateJacobianVsFiniteDifferencesGradient(new SpacecraftState(orbit.shiftedBy(deltaT)), forceModel,
                                              Utils.defaultLaw(), dP, checkTolerance, print);

    }

    @Test
    public void testGlobalStateJacobianIsotropicClassical() {

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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                           new IsotropicRadiationClassicalConvention(2.5, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e6, tolerances[0], 4.2e-5);

    }

    @Test
    public void testGlobalStateJacobianIsotropicCnes() {

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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                           new IsotropicRadiationCNES95Convention(2.5, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 3.0e-5);

    }

    @Test
    public void testParameterDerivativeBox() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                               new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.0, 0.7, 0.2));

        checkParameterDerivative(state, forceModel, RadiationSensitive.GLOBAL_RADIATION_FACTOR, 0.25,  8.8e-16);

    }

    @Test
    public void testParameterDerivativeGradientBox() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                               new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.0, 0.7, 0.2));

        checkParameterDerivativeGradient(state, forceModel, RadiationSensitive.GLOBAL_RADIATION_FACTOR, 0.25, 8.8e-16);

    }

    @Test
    public void testGlobalStateJacobianBox() {

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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                           new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                                          Vector3D.PLUS_J, 1.2, 0.0, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 5.0e-4);

    }

    @Test
    public void testRoughOrbitalModifs() {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 7, 1),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3,
                                           FastMath.tan(0.001745329)*FastMath.cos(2*FastMath.PI/3),
                                           FastMath.tan(0.001745329)*FastMath.sin(2*FastMath.PI/3),
                                           0.1, PositionAngleType.TRUE, FramesFactory.getEME2000(), date, mu);
        final double period = orbit.getKeplerianPeriod();
        Assertions.assertEquals(86164, period, 1);
        ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        // creation of the force model
        OneAxisEllipsoid earth =
            new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        SolarRadiationPressure SRP =
            new SolarRadiationPressure(sun, earth, new IsotropicRadiationCNES95Convention(500.0, 0.7, 0.7));

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

    public static void checkRadius(double radius , double min , double max) {
        Assertions.assertTrue(radius >= min);
        Assertions.assertTrue(radius <= max);
    }

    private double mu = 3.98600E14;

    private static class SolarStepHandler implements OrekitFixedStepHandler {

        @Override
        public void handleStep(SpacecraftState currentState) {
            final double dex = currentState.getEquinoctialEx() - 0.01071166;
            final double dey = currentState.getEquinoctialEy() - 0.00654848;
            final double alpha = FastMath.toDegrees(FastMath.atan2(dey, dex));
            Assertions.assertTrue(alpha > 100.0);
            Assertions.assertTrue(alpha < 112.0);
            checkRadius(FastMath.sqrt(dex * dex + dey * dey), 0.003524, 0.003541);
        }

    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldIsotropicTest() {
        // Initial field Keplerian orbit
        // The variables are the six orbital parameters
        DSFactory factory = new DSFactory(6, 5);
        DerivativeStructure a_0 = factory.variable(0, 7e7);
        DerivativeStructure e_0 = factory.variable(1, 0.4);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
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
        final OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(10.0, FKO.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);

        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        // Field and classical numerical propagators
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        // Set up the force model to test
        OneAxisEllipsoid earth =
            new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        SolarRadiationPressure forceModel =
            new SolarRadiationPressure(sun, earth, new IsotropicRadiationCNES95Convention(500.0, 0.7, 0.7));

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        checkRealFieldPropagation(FKO, PositionAngleType.MEAN, 1000., NP, FNP,
                                  1.0e-30, 5.0e-10, 3.0e-11, 3.0e-10,
                                  1, false);
    }

    /**Same test as the previous one but not adding the ForceModel to the NumericalPropagator
    it is a test to validate the previous test.
    (to test if the ForceModel it's actually
    doing something in the Propagator and the FieldPropagator)*/
    @Test
    public void RealFieldExpectErrorTest() {
        DSFactory factory = new DSFactory(6, 0);
        DerivativeStructure a_0 = factory.variable(0, 7e7);
        DerivativeStructure e_0 = factory.variable(1, 0.4);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();

        final OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setInitialState(iSR);
        ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        // creation of the force model
        OneAxisEllipsoid earth =
            new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        SolarRadiationPressure forceModel =
            new SolarRadiationPressure(sun, earth, new IsotropicRadiationCNES95Convention(500.0, 0.7, 0.7));

        FNP.addForceModel(forceModel);
        //NOT ADDING THE FORCE MODEL TO THE NUMERICAL PROPAGATOR   NP.addForceModel(forceModel);

        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getX() - finPVC_R.getPosition().getX()) < FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getY() - finPVC_R.getPosition().getY()) < FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getZ() - finPVC_R.getPosition().getZ()) < FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);
    }

    @Test
    public void testFlatteningLEO() {

        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", false));
        NormalizedSphericalHarmonicsProvider gravityField = GravityFieldFactory.getNormalizedProvider(20, 20);

        final AbsoluteDate initialDate = new AbsoluteDate("2000-02-14T00:00:00.000Z", TimeScalesFactory.getUTC());
        final Orbit initialOrbit =
                        new CircularOrbit(6992986.636088, -5.3589198032e-04, 1.2502577102e-03,
                                          FastMath.toRadians(97.827521740), FastMath.toRadians(124.587089845),
                                          FastMath.toRadians(0.0), PositionAngleType.MEAN,
                                          FramesFactory.getMOD(IERSConventions.IERS_2010),
                                          initialDate, gravityField.getMu());
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, 30.0);

        final CelestialBody      sun            = CelestialBodyFactory.getSun();
        final CelestialBody      moon           = CelestialBodyFactory.getMoon();
        final RadiationSensitive spacecraft     = new IsotropicRadiationClassicalConvention(0.12, 0.5, 0.5);
        final OneAxisEllipsoid   oblateEarth    = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                       Constants.WGS84_EARTH_FLATTENING,
                                                                       FramesFactory.getITRF(IERSConventions.IERS_2010, false));
        final OneAxisEllipsoid   sphericalEarth = new OneAxisEllipsoid(oblateEarth.getEquatorialRadius(),
                                                                       0.0,
                                                                       oblateEarth.getBodyFrame());

        PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(Arrays.asList(createLeoPropagator(sun, moon, oblateEarth,
                                                                                      gravityField, spacecraft, initialState),
                                                                  createLeoPropagator(sun, moon, sphericalEarth,
                                                                                      gravityField, spacecraft, initialState)),
                                                    new DistanceCheckerHandler(10.0, 0.816805));
        parallelizer.propagate(initialDate, initialDate.shiftedBy(7 * Constants.JULIAN_DAY));

    }

    private NumericalPropagator createLeoPropagator(final CelestialBody sun, final CelestialBody moon, final OneAxisEllipsoid earth,
                                                    final NormalizedSphericalHarmonicsProvider gravityField,
                                                    final RadiationSensitive spacecraft, SpacecraftState initialState) {
        final double[][] tol = NumericalPropagator.tolerances(1.0e-6, initialState.getOrbit(), OrbitType.CIRCULAR);
        final NumericalPropagator propagator =
                        new NumericalPropagator(new DormandPrince853Integrator(1.0e-9, 60.0, tol[0], tol[1]));
        propagator.setOrbitType(OrbitType.CIRCULAR);
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(earth.getBodyFrame(), gravityField));
        propagator.addForceModel(new ThirdBodyAttraction(sun));
        propagator.addForceModel(new ThirdBodyAttraction(moon));
        propagator.addForceModel(new SolarRadiationPressure(sun, earth, spacecraft));
        propagator.setInitialState(initialState);
        return propagator;
    }

    private static class DistanceCheckerHandler implements MultiSatStepHandler {

        private final double samplingStep;
        private final double expectedMax;
        private double       currentMax;
        private AbsoluteDate nextSample;

        DistanceCheckerHandler(final double samplingStep, final double expectedMax) {
            this.samplingStep = samplingStep;
            this.expectedMax  = expectedMax;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final List<SpacecraftState> states0, final AbsoluteDate t) {
            currentMax = 0.0;
            nextSample = states0.get(0).getDate().shiftedBy(samplingStep);
        }

        public void handleStep(List<OrekitStepInterpolator> interpolators) {
            while (interpolators.get(0).getPreviousState().getDate().isBefore(nextSample) &&
                   interpolators.get(0).getCurrentState().getDate().isAfterOrEqualTo(nextSample)) {
                final SpacecraftState state0 = interpolators.get(0).getInterpolatedState(nextSample);
                final SpacecraftState state1 = interpolators.get(1).getInterpolatedState(nextSample);
                final double distance = Vector3D.distance(state0.getPosition(),
                                                          state1.getPosition());
                currentMax = FastMath.max(currentMax, distance);
                nextSample = nextSample.shiftedBy(samplingStep);
            }
        }

        public void finish(final List<SpacecraftState> finalStates) {
            Assertions.assertEquals(expectedMax, currentMax, 1.0e-3 * expectedMax);
        }

    }

    /** Testing if eclipses due to Moon are considered.
     * Reference values are presented in "A Study of Solar Radiation Pressure acting on GPS Satellites"
     * written by Laurent Olivier Froideval in 2009.
     * Modifications of the step handler and time span able to print lighting ratios other a year and get a reference like graph.
     */
    @Test
    public void testMoonPenumbra() {
        final AbsoluteDate date  = new AbsoluteDate(2007, 1, 19, 5,  0, 0, TimeScalesFactory.getGPS());
        final Vector3D     p     = new Vector3D(12538484.957505366, 15515522.98001655, -17884023.51839292);
        final Vector3D     v     = new Vector3D(-3366.9009055533616, 769.5389825219049,  -1679.3840677789601);
        final Orbit        orbit = new CartesianOrbit(new TimeStampedPVCoordinates(date, p, v),
                                                      FramesFactory.getGCRF(), Constants.EIGEN5C_EARTH_MU);
        doTestMoonEarth(orbit, 7200.0, 1.0, 0, 0, 0, 1513, 0.0);
    }

    @Test
    public void testEarthPenumbraOnly() {
        final AbsoluteDate date  = new AbsoluteDate(2007, 3, 13, 17, 14, 0, TimeScalesFactory.getGPS());
        final Vector3D     p     = new Vector3D(-26168647.4977583, -1516554.3304749255, -3206794.210706205);
        final Vector3D     v     = new Vector3D(-213.65557094060222, -2377.3633988328584,  3079.4740070013495);
        final Orbit        orbit = new CartesianOrbit(new TimeStampedPVCoordinates(date, p, v),
                                                      FramesFactory.getGCRF(), Constants.EIGEN5C_EARTH_MU);
        doTestMoonEarth(orbit, 720.0, 1.0, 0, 525, 0, 0, 2.192e-3);
    }

    @Test
    public void testEarthPenumbraAndUmbra() {
        final AbsoluteDate date  = new AbsoluteDate(2007, 3, 14,  5,  8, 0, TimeScalesFactory.getGPS());
        final Vector3D     p     = new Vector3D(-26101379.998276696, -947280.678355501, -3940992.754483608);
        final Vector3D     v     = new Vector3D(-348.8911736753223, -2383.738528546711, 3060.9815784341567);
        final Orbit        orbit = new CartesianOrbit(new TimeStampedPVCoordinates(date, p, v),
                                                      FramesFactory.getGCRF(), Constants.EIGEN5C_EARTH_MU);
        doTestMoonEarth(orbit, 3600.0, 1.0, 534, 1003, 0, 0, 11.689e-3);
    }

    private void doTestMoonEarth(Orbit orbit, double duration, double step,
                                 int expectedEarthUmbraSteps, int expectedEarthPenumbraSteps,
                                 int expectedMoonUmbraSteps, int expectedMoonPenumbraSteps,
                                 double expectedDistance) {

        Utils.setDataRoot("2007");
        final ExtendedPVCoordinatesProvider sun  = CelestialBodyFactory.getSun();
        final ExtendedPVCoordinatesProvider moon = CelestialBodyFactory.getMoon();
        final SpacecraftState initialState = new SpacecraftState(orbit);

        // Create SRP perturbation with Moon and Earth
        SolarRadiationPressure srpWithFlattening =
                        new SolarRadiationPressure(sun,
                                                   new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                        Constants.WGS84_EARTH_FLATTENING,
                                                                        FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                                   new IsotropicRadiationClassicalConvention(50.0, 0.5, 0.5));
        srpWithFlattening.addOccultingBody(moon, Constants.MOON_EQUATORIAL_RADIUS);
        SolarRadiationPressure srpWithoutFlattening =
                        new SolarRadiationPressure(sun,
                                                   new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                        0.0,
                                                                        FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                                   new IsotropicRadiationClassicalConvention(50.0, 0.5, 0.5));
        srpWithoutFlattening.addOccultingBody(moon, Constants.MOON_EQUATORIAL_RADIUS);

        // creation of the propagator
        final OrbitType type = OrbitType.CARTESIAN;
        double[][] tol = NumericalPropagator.tolerances(1.0e-6, orbit, type);

        final NumericalPropagator propagatorWithFlattening =
                        new NumericalPropagator(new DormandPrince853Integrator(1.0e-9, 300, tol[0], tol[1]));
        propagatorWithFlattening.setOrbitType(type);
        propagatorWithFlattening.setInitialState(initialState);
        propagatorWithFlattening.addForceModel(srpWithFlattening);
        MoonEclipseStepHandler handler = new MoonEclipseStepHandler(moon, sun, srpWithFlattening);
        propagatorWithFlattening.setStepHandler(step, handler);
        final SpacecraftState withFlattening = propagatorWithFlattening.propagate(orbit.getDate().shiftedBy(duration));
        Assertions.assertEquals(expectedEarthUmbraSteps,    handler.earthUmbraSteps);
        Assertions.assertEquals(expectedEarthPenumbraSteps, handler.earthPenumbraSteps);
        Assertions.assertEquals(expectedMoonUmbraSteps,     handler.moonUmbraSteps);
        Assertions.assertEquals(expectedMoonPenumbraSteps,  handler.moonPenumbraSteps);

        final NumericalPropagator propagatorWithoutFlattening =
                        new NumericalPropagator(new DormandPrince853Integrator(1.0e-9, 300, tol[0], tol[1]));
        propagatorWithoutFlattening.setOrbitType(type);
        propagatorWithoutFlattening.setInitialState(initialState);
        propagatorWithoutFlattening.addForceModel(srpWithoutFlattening);
        final SpacecraftState withoutFlattening = propagatorWithoutFlattening.propagate(orbit.getDate().shiftedBy(duration));

        Assertions.assertEquals(expectedDistance,
                                Vector3D.distance(withFlattening.getPosition(),
                                                  withoutFlattening.getPosition()),
                                1.0e-6);

    }

    /** Specialized step handler.
     * <p>This class extends the step handler in order to print on the output stream at the given step.<p>
     * @author Thomas Paulet
     */
    private static class MoonEclipseStepHandler implements OrekitFixedStepHandler {

        final ExtendedPVCoordinatesProvider moon;
        final ExtendedPVCoordinatesProvider sun;
        final SolarRadiationPressure srp;
        int earthUmbraSteps;
        int earthPenumbraSteps;
        int moonUmbraSteps;
        int moonPenumbraSteps;

        /** Simple constructor.
         */
        MoonEclipseStepHandler(final ExtendedPVCoordinatesProvider moon, final ExtendedPVCoordinatesProvider sun,
                               final SolarRadiationPressure srp) {
            this.moon = moon;
            this.sun  = sun;
            this.srp  = srp;

        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
            earthUmbraSteps    = 0;
            earthPenumbraSteps = 0;
            moonUmbraSteps     = 0;
            moonPenumbraSteps  = 0;
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final SpacecraftState currentState) {
            final AbsoluteDate date = currentState.getDate();
            final Frame frame = currentState.getFrame();

            final Vector3D moonPos = moon.getPosition(date, frame);
            final Vector3D sunPos = sun.getPosition(date, frame);
            final Vector3D statePos = currentState.getPosition();

            // Moon umbra and penumbra conditions
            final double[] moonAngles = srp.getGeneralEclipseAngles(statePos, moonPos, Constants.MOON_EQUATORIAL_RADIUS,
                                                                    sunPos, Constants.SUN_RADIUS);
            final double moonUmbra = moonAngles[0] - moonAngles[1] + moonAngles[2];
            final boolean isInMoonUmbra = (moonUmbra < 1.0e-10);
            if (isInMoonUmbra) {
                ++moonUmbraSteps;
            }

            final double moonPenumbra = moonAngles[0] - moonAngles[1] - moonAngles[2];
            final boolean isInMoonPenumbra = (moonPenumbra < -1.0e-10);
            if (isInMoonPenumbra) {
                ++moonPenumbraSteps;
            }

            // Earth umbra and penumbra conditions
            final OccultationEngine.OccultationAngles angles = srp.getOccultingBodies().get(0).angles(currentState);

            final double earthUmbra = angles.getSeparation() - angles.getLimbRadius() + angles.getOccultedApparentRadius();
            final boolean isInEarthUmbra = (earthUmbra < 1.0e-10);
            if (isInEarthUmbra) {
                ++earthUmbraSteps;
            }

            final double earthPenumbra = angles.getSeparation() - angles.getLimbRadius() - angles.getOccultedApparentRadius();
            final boolean isInEarthPenumbra = (earthPenumbra < -1.0e-10);
            if (isInEarthPenumbra) {
                ++earthPenumbraSteps;
            }


            // Compute lighting ratio
            final double lightingRatio = srp.getLightingRatio(currentState);

            // Check behaviour
            if (isInMoonUmbra || isInEarthUmbra) {
                Assertions.assertEquals(0.0, lightingRatio, 1e-8);
            }
            else if (isInMoonPenumbra || isInEarthPenumbra) {
                Assertions.assertTrue(lightingRatio < 1.0);
                Assertions.assertTrue(lightingRatio > 0.0);
            }
            else {
                Assertions.assertEquals(1.0, lightingRatio, 1e-8);
            }
        }
    }

    /** Testing if eclipses due to Moon are considered.
     * Reference values are presented in "A Study of Solar Radiation Pressure acting on GPS Satellites"
     * written by Laurent Olivier Froideval in 2009.
     * Modifications of the step handler and time span able to print lighting ratios other a year and get a reference like graph.
     */
    @Test
    public void testFieldMoonPenumbra() {
        Field<Binary64> field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date  = new FieldAbsoluteDate<>(field, 2007, 1, 19, 5,  0, 0, TimeScalesFactory.getGPS());
        final FieldVector3D<Binary64>     p     = new FieldVector3D<>(field, new Vector3D(12538484.957505366, 15515522.98001655, -17884023.51839292));
        final FieldVector3D<Binary64>     v     = new FieldVector3D<>(field, new Vector3D(-3366.9009055533616, 769.5389825219049,  -1679.3840677789601));
        final FieldVector3D<Binary64>     a     = FieldVector3D.getZero(field);
        final FieldOrbit<Binary64>        orbit = new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(date, p, v, a),
                                                                             FramesFactory.getGCRF(),
                                                                             field.getZero().newInstance(Constants.EIGEN5C_EARTH_MU));
        doTestFieldMoonEarth(orbit, field.getZero().newInstance(7200.0), field.getZero().newInstance(1.0), 0, 0, 0, 1513, 0.0);
    }

    @Test
    public void testFieldEarthPenumbraOnly() {
        Field<Binary64> field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date  = new FieldAbsoluteDate<>(field, 2007, 3, 13, 17, 14, 0, TimeScalesFactory.getGPS());
        final FieldVector3D<Binary64>     p     = new FieldVector3D<>(field, new Vector3D(-26168647.4977583, -1516554.3304749255, -3206794.210706205));
        final FieldVector3D<Binary64>     v     = new FieldVector3D<>(field, new Vector3D(-213.65557094060222, -2377.3633988328584,  3079.4740070013495));
        final FieldVector3D<Binary64>     a     = FieldVector3D.getZero(field);
        final FieldOrbit<Binary64>        orbit = new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(date, p, v, a),
                                                                             FramesFactory.getGCRF(),
                                                                             field.getZero().newInstance(Constants.EIGEN5C_EARTH_MU));
        doTestFieldMoonEarth(orbit, field.getZero().newInstance(720.0), field.getZero().newInstance(1.0), 0, 525, 0, 0, 2.193e-3);
    }

    @Test
    public void testFieldEarthPenumbraAndUmbra() {
        Field<Binary64> field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date  = new FieldAbsoluteDate<>(field, 2007, 3, 14,  5,  8, 0, TimeScalesFactory.getGPS());
        final FieldVector3D<Binary64>     p     = new FieldVector3D<>(field, new Vector3D(-26101379.998276696, -947280.678355501, -3940992.754483608));
        final FieldVector3D<Binary64>     v     = new FieldVector3D<>(field, new Vector3D(-348.8911736753223, -2383.738528546711, 3060.9815784341567));
        final FieldVector3D<Binary64>     a     = FieldVector3D.getZero(field);
        final FieldOrbit<Binary64>        orbit = new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(date, p, v, a),
                                                                             FramesFactory.getGCRF(),
                                                                             field.getZero().newInstance(Constants.EIGEN5C_EARTH_MU));
        doTestFieldMoonEarth(orbit, field.getZero().newInstance(3600.0), field.getZero().newInstance(1.0), 534, 1003, 0, 0, 11.611e-3);
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldMoonEarth(FieldOrbit<T> orbit, T duration, T step,
                                                                          int expectedEarthUmbraSteps, int expectedEarthPenumbraSteps,
                                                                          int expectedMoonUmbraSteps, int expectedMoonPenumbraSteps,
                                                                          double expectedDistance) {

        Utils.setDataRoot("2007");
        final Field<T> field = orbit.getDate().getField();
        final ExtendedPVCoordinatesProvider sun  = CelestialBodyFactory.getSun();
        final ExtendedPVCoordinatesProvider moon = CelestialBodyFactory.getMoon();
        final FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);

        // Create SRP perturbation with Moon and Earth
        SolarRadiationPressure srpWithFlattening =
                        new SolarRadiationPressure(sun,
                                                   new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                        Constants.WGS84_EARTH_FLATTENING,
                                                                        FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                                   new IsotropicRadiationClassicalConvention(50.0, 0.5, 0.5));
        srpWithFlattening.addOccultingBody(moon, Constants.MOON_EQUATORIAL_RADIUS);
        SolarRadiationPressure srpWithoutFlattening =
                        new SolarRadiationPressure(sun,
                                                   new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                        0.0,
                                                                        FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                                   new IsotropicRadiationClassicalConvention(50.0, 0.5, 0.5));
        srpWithoutFlattening.addOccultingBody(moon, Constants.MOON_EQUATORIAL_RADIUS);

        // creation of the propagator
        final OrbitType type = OrbitType.CARTESIAN;
        double[][] tol = FieldNumericalPropagator.tolerances(field.getZero().newInstance(1.0e-6), orbit, type);
        final FieldNumericalPropagator<T> propagatorWithFlattening =
                        new FieldNumericalPropagator<>(field,
                                        new DormandPrince853FieldIntegrator<>(field, 1.0e-9, 300, tol[0], tol[1]));
        propagatorWithFlattening.setOrbitType(type);
        propagatorWithFlattening.setInitialState(initialState);
        propagatorWithFlattening.addForceModel(srpWithFlattening);
        FieldMoonEclipseStepHandler<T> handler = new FieldMoonEclipseStepHandler<>(moon, sun, srpWithFlattening);
        propagatorWithFlattening.setStepHandler(step, handler);
        final FieldSpacecraftState<T> withFlattening = propagatorWithFlattening.propagate(orbit.getDate().shiftedBy(duration));
        Assertions.assertEquals(expectedEarthUmbraSteps,    handler.earthUmbraSteps);
        Assertions.assertEquals(expectedEarthPenumbraSteps, handler.earthPenumbraSteps);
        Assertions.assertEquals(expectedMoonUmbraSteps,     handler.moonUmbraSteps);
        Assertions.assertEquals(expectedMoonPenumbraSteps,  handler.moonPenumbraSteps);

        final FieldNumericalPropagator<T> propagatorWithoutFlattening =
                        new FieldNumericalPropagator<>(field,
                                        new DormandPrince853FieldIntegrator<>(field, 1.0e-9, 300, tol[0], tol[1]));
        propagatorWithoutFlattening.setOrbitType(type);
        propagatorWithoutFlattening.setInitialState(initialState);
        propagatorWithoutFlattening.addForceModel(srpWithoutFlattening);
        final FieldSpacecraftState<T> withoutFlattening = propagatorWithoutFlattening.propagate(orbit.getDate().shiftedBy(duration));

        Assertions.assertEquals(expectedDistance,
                                FieldVector3D.distance(withFlattening.getPosition(),
                                                       withoutFlattening.getPosition()).getReal(),
                                1.0e-6);

    }

    /** Specialized step handler.
     * <p>This class extends the step handler in order to print on the output stream at the given step.<p>
     * @author Thomas Paulet
     */
    private static class FieldMoonEclipseStepHandler<T extends CalculusFieldElement<T>> implements FieldOrekitFixedStepHandler<T> {

        final ExtendedPVCoordinatesProvider moon;
        final ExtendedPVCoordinatesProvider sun;
        final SolarRadiationPressure srp;
        int earthUmbraSteps;
        int earthPenumbraSteps;
        int moonUmbraSteps;
        int moonPenumbraSteps;

        /** Simple constructor.
         */
        FieldMoonEclipseStepHandler(final ExtendedPVCoordinatesProvider moon, final ExtendedPVCoordinatesProvider sun,
                                    final SolarRadiationPressure srp) {
            this.moon = moon;
            this.sun  = sun;
            this.srp  = srp;

        }

        /** {@inheritDoc} */
        @Override
        public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t, final T step) {
            earthUmbraSteps    = 0;
            earthPenumbraSteps = 0;
            moonUmbraSteps     = 0;
            moonPenumbraSteps  = 0;
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final FieldSpacecraftState<T> currentState) {
            final FieldAbsoluteDate<T> date = currentState.getDate();
            final Frame frame = currentState.getFrame();

            final FieldVector3D<T> moonPos = moon.getPosition(date, frame);
            final FieldVector3D<T> sunPos = sun.getPosition(date, frame);
            final FieldVector3D<T> statePos = currentState.getPosition();

            // Moon umbra and penumbra conditions
            final T[] moonAngles = srp.getGeneralEclipseAngles(statePos, moonPos,
                                                               date.getField().getZero().newInstance(Constants.MOON_EQUATORIAL_RADIUS),
                                                               sunPos,
                                                               date.getField().getZero().newInstance(Constants.SUN_RADIUS));
            final T moonUmbra = moonAngles[0].subtract(moonAngles[1]).add(moonAngles[2]);
            final boolean isInMoonUmbra = (moonUmbra.getReal() < 1.0e-10);
            if (isInMoonUmbra) {
                ++moonUmbraSteps;
            }

            final T moonPenumbra = moonAngles[0].subtract(moonAngles[1]).subtract(moonAngles[2]);
            final boolean isInMoonPenumbra = (moonPenumbra.getReal() < -1.0e-10);
            if (isInMoonPenumbra) {
                ++moonPenumbraSteps;
            }

            // Earth umbra and penumbra conditions
            final OccultationEngine.FieldOccultationAngles<T> angles = srp.getOccultingBodies().get(0).angles(currentState);

            final T earthUmbra = angles.getSeparation().subtract(angles.getLimbRadius()).add(angles.getOccultedApparentRadius());
            final boolean isInEarthUmbra = (earthUmbra.getReal() < 1.0e-10);
            if (isInEarthUmbra) {
                ++earthUmbraSteps;
            }

            final T earthPenumbra = angles.getSeparation().subtract(angles.getLimbRadius()).subtract(angles.getOccultedApparentRadius());
            final boolean isInEarthPenumbra = (earthPenumbra.getReal() < -1.0e-10);
            if (isInEarthPenumbra) {
                ++earthPenumbraSteps;
            }

            // Compute lighting ratio
            final T lightingRatio = srp.getLightingRatio(currentState);

            // Check behaviour
            if (isInMoonUmbra || isInEarthUmbra) {
                Assertions.assertEquals(0.0, lightingRatio.getReal(), 1e-8);
            }
            else if (isInMoonPenumbra || isInEarthPenumbra) {
                Assertions.assertTrue(lightingRatio.getReal() < 1.0);
                Assertions.assertTrue(lightingRatio.getReal() > 0.0);
            }
            else {
                Assertions.assertEquals(1.0, lightingRatio.getReal(), 1e-8);
            }
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }
}
