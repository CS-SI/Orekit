/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.estimation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.estimation.measurements.MeasurementCreator;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.gravity.potential.AstronomicalAmplitudeReader;
import org.orekit.forces.gravity.potential.FESCHatEpsilonReader;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.OceanLoadDeformationCoefficients;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/** Utility class for orbit determination tests. */
public class EstimationTestUtils {

    public static Context eccentricContext() throws OrekitException {

        Utils.setDataRoot("regular-data:potential:tides");
        Context context = new Context();
        context.conventions = IERSConventions.IERS_2010;
        context.earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(context.conventions, true));
        context.sun = CelestialBodyFactory.getSun();
        context.moon = CelestialBodyFactory.getMoon();
        context.spacecraft = new SphericalSpacecraft(2.0, 1.2, 0.2, 0.8);
        context.utc = TimeScalesFactory.getUTC();
        context.ut1 = TimeScalesFactory.getUT1(context.conventions, true);
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        AstronomicalAmplitudeReader aaReader =
                        new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        context.gravity = GravityFieldFactory.getNormalizedProvider(20, 20);
        context.initialOrbit = new KeplerianOrbit(15000000.0, 0.125, 1.25,
                                                  0.250, 1.375, 0.0625, PositionAngle.TRUE,
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate(2000, 2, 24, 11, 35,47.0, context.utc),
                                                  context.gravity.getMu());

        context.stations = Arrays.asList(//context.createStation(-18.59146, -173.98363,   76.0, "Leimatu`a"),
                                         context.createStation(-53.05388,  -75.01551, 1750.0, "Isla Desolación"),
                                         context.createStation( 62.29639,   -7.01250,  880.0, "Slættaratindur")
                                         //context.createStation( -4.01583,  103.12833, 3173.0, "Gunung Dempo")
                        );

        return context;

    }

    public static List<Measurement> createMeasurements(final Context context, final PropagatorBuilder propagatorBuilder,
                                                       final MeasurementCreator creator,
                                                       final double startPeriod, final double endPeriod,
                                                       final double step)
        throws OrekitException {

        final int          nbOrbitalParameters      = 6;
        final List<String> propagatorParameters     = propagatorBuilder.getFreeParameters();
        final int          nbPropagatorParameters   = propagatorParameters.size();
        final int dimension = nbOrbitalParameters + nbPropagatorParameters;

        final double[] parameters = new double[dimension];
        propagatorBuilder.getOrbitType().mapOrbitToArray(context.initialOrbit,
                                                         propagatorBuilder.getPositionAngle(),
                                                         parameters);
        int index = nbOrbitalParameters;
        for (final String propagatorParameter : propagatorParameters) {
            parameters[index++] = propagatorBuilder.getParameter(propagatorParameter);
        }
        propagatorBuilder.getOrbitType().mapOrbitToArray(context.initialOrbit,
                                                         propagatorBuilder.getPositionAngle(),
                                                         parameters);

        final Propagator propagator = propagatorBuilder.buildPropagator(context.initialOrbit.getDate(), parameters);
        propagator.setMasterMode(step, creator);
        final double       period = context.initialOrbit.getKeplerianPeriod();
        final AbsoluteDate start  = context.initialOrbit.getDate().shiftedBy(startPeriod * period);
        final AbsoluteDate end    = context.initialOrbit.getDate().shiftedBy(endPeriod   * period);
        propagator.propagate(start, end);

        return creator.getMeasurements();

    }

    public static void checkFit(final Context context, final BatchLSEstimator estimator,
                                final int iterations,
                                final double expectedRMS,      final double rmsEps,
                                final double expectedMax,      final double maxEps,
                                final double expectedDeltaPos, final double posEps,
                                final double expectedDeltaVel, final double velEps)
        throws OrekitException {

        // estimate orbit, starting from a wrong point
        final Vector3D initialPosition = context.initialOrbit.getPVCoordinates().getPosition();
        final Vector3D initialVelocity = context.initialOrbit.getPVCoordinates().getVelocity();
        final Vector3D wrongPosition   = initialPosition.add(new Vector3D(1000.0, 0, 0));
        final Vector3D wrongVelocity   = initialVelocity.add(new Vector3D(0, 0, 0.01));
        final Orbit   wrongOrbit       = new CartesianOrbit(new PVCoordinates(wrongPosition, wrongVelocity),
                                                            context.initialOrbit.getFrame(),
                                                            context.initialOrbit.getDate(),
                                                            context.initialOrbit.getMu());
        final Orbit estimatedOrbit = estimator.estimate(wrongOrbit);
        final Vector3D estimatedPosition = estimatedOrbit.getPVCoordinates().getPosition();
        final Vector3D estimatedVelocity = estimatedOrbit.getPVCoordinates().getVelocity();

        Assert.assertEquals(iterations, estimator.getIterations());

        int    k   = 0;
        double sum = 0;
        double max = 0;
        for (final Map.Entry<Measurement, Evaluation> entry :
             estimator.getLastEvaluations().entrySet()) {
            final Measurement m           = entry.getKey();
            final Evaluation  e           = entry.getValue();
            final double[]    weight      = m.getBaseWeight();
            final double[]    sigma       = m.getTheoreticalStandardDeviation();
            final double[]    observed    = m.getObservedValue();
            final double[]    theoretical = e.getValue();
            for (int i = 0; i < m.getDimension(); ++i) {
                final double weightedResidual = weight[i] * (theoretical[i] - observed[i]) / sigma[i];
                ++k;
                sum += weightedResidual * weightedResidual;
                max = FastMath.max(max, FastMath.abs(weightedResidual));
            }
        }

        System.out.println(FastMath.sqrt(sum / k) + " " + max + " " +
                Vector3D.distance(initialPosition, estimatedPosition) + " " +
                Vector3D.distance(initialVelocity, estimatedVelocity));
        Assert.assertEquals(expectedRMS,
                            FastMath.sqrt(sum / k),
                            rmsEps);
        Assert.assertEquals(expectedMax,
                            max,
                            maxEps);
        Assert.assertEquals(expectedDeltaPos,
                            Vector3D.distance(initialPosition, estimatedPosition),
                            posEps);
        Assert.assertEquals(expectedDeltaVel,
                            Vector3D.distance(initialVelocity, estimatedVelocity),
                            velEps);

    }

}


