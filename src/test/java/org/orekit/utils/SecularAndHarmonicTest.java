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
package org.orekit.utils;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.solvers.BaseUnivariateSolver;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolverUtils;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

public class SecularAndHarmonicTest {

    private TimeScale utc;
    private NormalizedSphericalHarmonicsProvider gravityField;
    private BodyShape earth;
    private TimeFunction<DerivativeStructure> gmst;

    @Before
    public void setUp() throws OrekitException {

        Utils.setDataRoot("regular-data:potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", false));
        utc = TimeScalesFactory.getUTC();
        gravityField = GravityFieldFactory.getNormalizedProvider(8, 8);
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getGTOD(IERSConventions.IERS_2010, true));
        TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        gmst = IERSConventions.IERS_2010.getGMSTFunction(ut1);
    }

    @Test
    public void testSunSynchronization() throws OrekitException {

        int nbOrbits = 143;
        double mst = 10.5;

        // this test has been extracted from a more complete tutorial
        // on Low Earth Orbit phasing, which can be found in the tutorials
        // folder of the Orekit source distribution
        CircularOrbit initialGuessedOrbit =
                new CircularOrbit(7169867.824275421,
                                  0.0, 0.0010289683741791197,
                                  FastMath.toRadians(98.5680307986701),
                                  FastMath.toRadians(-189.6132856166402),
                                  FastMath.PI, PositionAngle.TRUE,
                                  FramesFactory.getEME2000(),
                                  new AbsoluteDate(2003, 4, 5, utc),
                                  gravityField.getMu());
        final double[] initialMSTModel = fitGMST(initialGuessedOrbit, nbOrbits, mst);

        // the initial guess is very far from the desired phasing parameters
        Assert.assertTrue(FastMath.abs(mst - initialMSTModel[0]) * 3600.0 > 0.4);
        Assert.assertTrue(FastMath.abs(initialMSTModel[1]) * 3600 > 0.5 / Constants.JULIAN_DAY);

        CircularOrbit finalOrbit =
                new CircularOrbit(7173353.364197798,
                                  -3.908629707615073E-4, 0.0013502004064500472,
                                  FastMath.toRadians(98.56430772945006),
                                  FastMath.toRadians(-189.61151932993425),
                                  FastMath.PI, PositionAngle.TRUE,
                                  FramesFactory.getEME2000(),
                                  new AbsoluteDate(2003, 4, 5, utc),
                                  gravityField.getMu());
        final double[] finalMSTModel = fitGMST(finalOrbit, nbOrbits, mst);

        // the final orbit is much closer to the desired phasing parameters
        Assert.assertTrue(FastMath.abs(mst - finalMSTModel[0]) * 3600.0 < 0.0012);
        Assert.assertTrue(FastMath.abs(finalMSTModel[1]) * 3600 < 0.0004 / Constants.JULIAN_DAY);

    }

    @Test
    public void testReset() throws OrekitException {
        final double SUN_PULSATION = 4.0 * FastMath.PI / Constants.JULIAN_YEAR; // Period = 6 months

        // Generate two random datasets
        final RandomGenerator random = new Well19937a(0x8de2c5d0e210588dl);
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        final double[][] data = new double[2][200];
        for (int iD = 0; iD < data[0].length; ++iD) {
            data[0][iD] = random.nextDouble();
            data[1][iD] = random.nextDouble();
        }

        // Generate three SAH models: the first two will fit each dataset
        // independently, while the third parses one dataset and then the other
        // after being reset. Fitted parameters should match in both cases.
        final double[] initialGuess = { 0.0, 0.0, 0.0, 0.0 };
        final SecularAndHarmonic[] indepModel = new SecularAndHarmonic[2];
        final SecularAndHarmonic resettingModel = new SecularAndHarmonic(1, SUN_PULSATION);
        for (int iM = 0; iM < indepModel.length; ++iM) {
            indepModel[iM] = new SecularAndHarmonic(1, SUN_PULSATION);
            indepModel[iM].resetFitting(t0, initialGuess);
            resettingModel.resetFitting(t0, initialGuess);

            for (int iD = 0; iD < data[0].length; ++iD) {
                final AbsoluteDate t = t0.shiftedBy(iD);
                indepModel[iM].addPoint(t, data[iM][iD]);
                resettingModel.addPoint(t, data[iM][iD]);
            }
            indepModel[iM].fit();
            resettingModel.fit();

            Assert.assertArrayEquals(indepModel[iM].getFittedParameters(),
                                     resettingModel.getFittedParameters(),
                                     1e-14);
        }

    }

    private double[] fitGMST(CircularOrbit orbit, int nbOrbits, double mst)
        throws OrekitException {

        double period = orbit.getKeplerianPeriod();
        double duration = nbOrbits * period;
        NumericalPropagator propagator = createPropagator(orbit);
        SecularAndHarmonic mstModel = new SecularAndHarmonic(2,
                                                             2.0 * FastMath.PI / Constants.JULIAN_YEAR,
                                                             4.0 * FastMath.PI / Constants.JULIAN_YEAR,
                                                             2.0 * FastMath.PI / Constants.JULIAN_DAY,
                                                             4.0 * FastMath.PI / Constants.JULIAN_DAY);
        mstModel.resetFitting(orbit.getDate(), new double[] {
            mst, -1.0e-10, -1.0e-17,
            1.0e-3, 1.0e-3, 1.0e-5, 1.0e-5, 1.0e-5, 1.0e-5, 1.0e-5, 1.0e-5
        });

        // first descending node
        SpacecraftState crossing =
                findFirstCrossing(0.0, false, orbit.getDate(),
                                  orbit.getDate().shiftedBy(2 * period),
                                  0.01 * period, propagator);

        while (crossing != null &&
               crossing.getDate().durationFrom(orbit.getDate()) < (nbOrbits * period)) {
            final AbsoluteDate previousDate = crossing.getDate();
            crossing = findLatitudeCrossing(0.0, previousDate.shiftedBy(period),
                                            previousDate.shiftedBy(2 * period),
                                            0.01 * period, period / 8, propagator);
            if (crossing != null) {

                // store current point
                mstModel.addPoint(crossing.getDate(), meanSolarTime(crossing.getOrbit()));

                // use the same time separation to pinpoint next crossing
                period = crossing.getDate().durationFrom(previousDate);

            }

        }

        // fit the mean solar time to a parabolic model, taking care the main
        // periods are properly removed
        mstModel.fit();
        return mstModel.approximateAsPolynomialOnly(1, orbit.getDate(), 2, 2,
                                                    orbit.getDate(),  orbit.getDate().shiftedBy(duration),
                                                    0.01 * period);

    }

    private NumericalPropagator createPropagator(CircularOrbit orbit)
        throws OrekitException {
        OrbitType type = OrbitType.CIRCULAR;
        double[][] tolerances = NumericalPropagator.tolerances(0.1, orbit, type);
        DormandPrince853Integrator integrator =
                new DormandPrince853Integrator(1.0, 600, tolerances[0], tolerances[1]);
        integrator.setInitialStepSize(60.0);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(earth.getBodyFrame(), gravityField));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));
        propagator.setOrbitType(type);
        propagator.resetInitialState(new SpacecraftState(orbit));
        return propagator;
    }

    private SpacecraftState findFirstCrossing(final double latitude, final boolean ascending,
                                              final AbsoluteDate searchStart, final AbsoluteDate end,
                                              final double stepSize, final Propagator propagator)
        throws OrekitException {

        double previousLatitude = Double.NaN;
        for (AbsoluteDate date = searchStart; date.compareTo(end) < 0; date = date.shiftedBy(stepSize)) {
            final PVCoordinates pv       = propagator.propagate(date).getPVCoordinates(earth.getBodyFrame());
            final double currentLatitude = earth.transform(pv.getPosition(), earth.getBodyFrame(), date).getLatitude();
            if (((previousLatitude <= latitude) && (currentLatitude >= latitude) &&  ascending) ||
                ((previousLatitude >= latitude) && (currentLatitude <= latitude) && !ascending)) {
                return findLatitudeCrossing(latitude, date.shiftedBy(-0.5 * stepSize), end,
                                            0.5 * stepSize, 2 * stepSize, propagator);
            }
            previousLatitude = currentLatitude;
        }

        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                  "latitude " + FastMath.toDegrees(latitude) + " never crossed");

    }

    private SpacecraftState findLatitudeCrossing(final double latitude,
                                                 final AbsoluteDate guessDate, final AbsoluteDate endDate,
                                                 final double shift, final double maxShift,
                                                 final Propagator propagator)
        throws OrekitException, MathRuntimeException {

        // function evaluating to 0 at latitude crossings
        final UnivariateFunction latitudeFunction = new UnivariateFunction() {
            /** {@inheritDoc} */
            public double value(double x) {
                try {
                    final SpacecraftState state = propagator.propagate(guessDate.shiftedBy(x));
                    final Vector3D position = state.getPVCoordinates(earth.getBodyFrame()).getPosition();
                    final GeodeticPoint point = earth.transform(position, earth.getBodyFrame(), state.getDate());
                    return point.getLatitude() - latitude;
                } catch (OrekitException oe) {
                    throw new RuntimeException(oe);
                }
            }
        };

        // try to bracket the encounter
        double span;
        if (guessDate.shiftedBy(shift).compareTo(endDate) > 0) {
            // Take a 1e-3 security margin
            span = endDate.durationFrom(guessDate) - 1e-3;
        } else {
            span = shift;
        }

        while (!UnivariateSolverUtils.isBracketing(latitudeFunction, -span, span)) {

            if (2 * span > maxShift) {
                // let the Hipparchus exception be thrown
                UnivariateSolverUtils.verifyBracketing(latitudeFunction, -span, span);
            } else if (guessDate.shiftedBy(2 * span).compareTo(endDate) > 0) {
                // Out of range :
                return null;
            }

            // expand the search interval
            span *= 2;

        }

        // find the encounter in the bracketed interval
        final BaseUnivariateSolver<UnivariateFunction> solver =
                new BracketingNthOrderBrentSolver(0.1, 5);
        final double dt = solver.solve(1000, latitudeFunction,-span, span);
        return propagator.propagate(guessDate.shiftedBy(dt));

    }

    private double meanSolarTime(final Orbit orbit)
            throws OrekitException {

        // compute angle between Sun and spacecraft in the equatorial plane
        final Vector3D position = orbit.getPVCoordinates().getPosition();
        final double time       = orbit.getDate().getComponents(TimeScalesFactory.getUTC()).getTime().getSecondsInUTCDay();
        final double theta      = gmst.value(orbit.getDate()).getValue();
        final double sunAlpha   = theta + FastMath.PI * (1 - time / (Constants.JULIAN_DAY * 0.5));
        final double dAlpha     = MathUtils.normalizeAngle(position.getAlpha() - sunAlpha, 0);

        // convert the angle to solar time
        return 12.0 * (1.0 + dAlpha / FastMath.PI);

    }

}
