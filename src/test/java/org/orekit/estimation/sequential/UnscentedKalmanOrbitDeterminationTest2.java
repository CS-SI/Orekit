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
package org.orekit.estimation.sequential;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.KeyValueFileParser;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.common.AbstractOrbitDetermination;
import org.orekit.estimation.common.ParameterKey;
import org.orekit.estimation.common.ResultKalman;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.empirical.AccelerationModel;
import org.orekit.forces.empirical.ParametricAcceleration;
import org.orekit.forces.empirical.PolynomialAccelerationModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.OceanTides;
import org.orekit.forces.gravity.Relativity;
import org.orekit.forces.gravity.SolidTides;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.KnockeRediffusedForceModel;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

public class UnscentedKalmanOrbitDeterminationTest2 extends AbstractOrbitDetermination<NumericalPropagatorBuilder> {
    /** Gravity field. */
    private NormalizedSphericalHarmonicsProvider gravityField;
    
    /** {@inheritDoc} */
    @Override
    protected void createGravityField(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {

        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = FastMath.min(degree, parser.getInt(ParameterKey.CENTRAL_BODY_ORDER));
        gravityField = GravityFieldFactory.getNormalizedProvider(degree, order);

    }

    /** {@inheritDoc} */
    @Override
    protected double getMu() {
        return gravityField.getMu();
    }

    /** {@inheritDoc} */
    @Override
    protected NumericalPropagatorBuilder createPropagatorBuilder(final Orbit referenceOrbit,
                                                                 final ODEIntegratorBuilder builder,
                                                                 final double positionScale) {
        return new NumericalPropagatorBuilder(referenceOrbit, builder, PositionAngleType.MEAN,
                                              positionScale);
    }

    /** {@inheritDoc} */
    @Override
    protected void setMass(final NumericalPropagatorBuilder propagatorBuilder,
                                final double mass) {
        propagatorBuilder.setMass(mass);
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setGravity(final NumericalPropagatorBuilder propagatorBuilder,
                                               final OneAxisEllipsoid body) {
        final ForceModel gravityModel = new HolmesFeatherstoneAttractionModel(body.getBodyFrame(), gravityField);
        propagatorBuilder.addForceModel(gravityModel);
        return gravityModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setOceanTides(final NumericalPropagatorBuilder propagatorBuilder,
                                                  final IERSConventions conventions,
                                                  final OneAxisEllipsoid body,
                                                  final int degree, final int order) {
        final ForceModel tidesModel = new OceanTides(body.getBodyFrame(),
                                                     gravityField.getAe(), gravityField.getMu(),
                                                     degree, order, conventions,
                                                     TimeScalesFactory.getUT1(conventions, true));
        propagatorBuilder.addForceModel(tidesModel);
        return tidesModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setSolidTides(final NumericalPropagatorBuilder propagatorBuilder,
                                                  final IERSConventions conventions,
                                                  final OneAxisEllipsoid body,
                                                  final CelestialBody[] solidTidesBodies) {
        final ForceModel tidesModel = new SolidTides(body.getBodyFrame(),
                                                     gravityField.getAe(), gravityField.getMu(),
                                                     gravityField.getTideSystem(), conventions,
                                                     TimeScalesFactory.getUT1(conventions, true),
                                                     solidTidesBodies);
        propagatorBuilder.addForceModel(tidesModel);
        return tidesModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setThirdBody(final NumericalPropagatorBuilder propagatorBuilder,
                                                 final CelestialBody thirdBody) {
        final ForceModel thirdBodyModel = new ThirdBodyAttraction(thirdBody);
        propagatorBuilder.addForceModel(thirdBodyModel);
        return thirdBodyModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setDrag(final NumericalPropagatorBuilder propagatorBuilder,
                                            final Atmosphere atmosphere, final DragSensitive spacecraft) {
        final ForceModel dragModel = new DragForce(atmosphere, spacecraft);
        propagatorBuilder.addForceModel(dragModel);
        return dragModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setSolarRadiationPressure(final NumericalPropagatorBuilder propagatorBuilder, final CelestialBody sun,
                                                              final OneAxisEllipsoid body, final RadiationSensitive spacecraft) {
        final ForceModel srpModel = new SolarRadiationPressure(sun, body, spacecraft);
        propagatorBuilder.addForceModel(srpModel);
        return srpModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setAlbedoInfrared(final NumericalPropagatorBuilder propagatorBuilder,
                                                      final CelestialBody sun, final double equatorialRadius,
                                                      final double angularResolution,
                                                      final RadiationSensitive spacecraft) {
        final ForceModel albedoIR = new KnockeRediffusedForceModel(sun, spacecraft, equatorialRadius, angularResolution);
        propagatorBuilder.addForceModel(albedoIR);
        return albedoIR.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setRelativity(final NumericalPropagatorBuilder propagatorBuilder) {
        final ForceModel relativityModel = new Relativity(gravityField.getMu());
        propagatorBuilder.addForceModel(relativityModel);
        return relativityModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setPolynomialAcceleration(final NumericalPropagatorBuilder propagatorBuilder,
                                                              final String name, final Vector3D direction, final int degree) {
        final AccelerationModel accModel = new PolynomialAccelerationModel(name, null, degree);
        final ForceModel polynomialModel = new ParametricAcceleration(direction, true, accModel);
        propagatorBuilder.addForceModel(polynomialModel);
        return polynomialModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected void setAttitudeProvider(final NumericalPropagatorBuilder propagatorBuilder,
                                       final AttitudeProvider attitudeProvider) {
        propagatorBuilder.setAttitudeProvider(attitudeProvider);
    }

    @Test
    // Orbit determination for Lageos2 based on SLR (range) measurements
    public void testLageos2Slr() throws URISyntaxException, IOException {

        // Print results on console
        final boolean print = false;
        
        // input in resources directory
        final String inputPath = UnscentedKalmanOrbitDeterminationTest2.class.getClassLoader().getResource("orbit-determination/Lageos2/unscented_kalman_od_test_Lageos2.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data acces
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        // Choice of an orbit type to use
        // Default for test is Cartesian
        final OrbitType orbitType = OrbitType.CARTESIAN;
        
        // Initial orbital Cartesian covariance matrix
        // These covariances are derived from the deltas between initial and reference orbits
        // So in a way they are "perfect"...
        // Cartesian covariance matrix initialization
        final RealMatrix cartesianOrbitalP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e4, 4e3, 1, 5e-3, 6e-5, 1e-4
        });
        // Orbital Cartesian process noise matrix (Q)
        final RealMatrix cartesianOrbitalQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
               3e-4, 3e-4, 3e-4, 3e-7, 3e-7, 3e-7
        });
        
        // Initial measurement covariance matrix and process noise matrix
        final RealMatrix measurementP = MatrixUtils.createRealDiagonalMatrix(new double [] {
           1., 1., 1., 1. 
        });
        final RealMatrix measurementQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-6, 1e-6, 1e-6, 1e-6
        });

        // Kalman orbit determination run.
        ResultKalman kalmanLageos2 = runKalman(input, orbitType, print,
                                               cartesianOrbitalP, cartesianOrbitalQ,
                                               null, null,
                                               measurementP, measurementQ, true);

        // Definition of the reference parameters for the tests
        
        final double distanceAccuracy = 1.68;
        final double velocityAccuracy = 2.71e-3;
        final double[] RefStatRange = { -1.698412, 1.529126, 0.029547, 0.338275 };

        // Tests
        // Note: The reference initial orbit is the same as in the batch LS tests
        // -----
        
        // Number of measurements processed
        final int numberOfMeas  = 258;
        Assertions.assertEquals(numberOfMeas, kalmanLageos2.getNumberOfMeasurements());

        // Estimated position and velocity
        final Vector3D estimatedPos = kalmanLageos2.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = kalmanLageos2.getEstimatedPV().getVelocity();

        // Reference position and velocity at initial date (same as in batch LS test)
        final Vector3D refPos0 = new Vector3D(-5532131.956902, 10025696.592156, -3578940.040009);
        final Vector3D refVel0 = new Vector3D(-3871.275109, -607.880985, 4280.972530);
        
        // Run the reference until Kalman last date
        final Orbit refOrbit = runReference(input, orbitType, refPos0, refVel0, null,
                                            kalmanLageos2.getEstimatedPV().getDate());
        final Vector3D refPos = refOrbit.getPosition();
        final Vector3D refVel = refOrbit.getPVCoordinates().getVelocity();
        
        // Check distances
        final double dP = Vector3D.distance(refPos, estimatedPos);
        final double dV = Vector3D.distance(refVel, estimatedVel);
        Assertions.assertEquals(0.0, dP, distanceAccuracy);
        Assertions.assertEquals(0.0, dV, velocityAccuracy);
        
        // Print orbit deltas
        if (print) {
            System.out.println("Test performances:");
            System.out.format("\t%-30s\n",
                            "ΔEstimated / Reference");
            System.out.format(Locale.US, "\t%-10s %20.6f\n",
                              "ΔP [m]", dP);
            System.out.format(Locale.US, "\t%-10s %20.6f\n",
                              "ΔV [m/s]", dV);
        }


        //test on statistic for the range residuals
        final long nbRange = 258;
        Assertions.assertEquals(nbRange, kalmanLageos2.getRangeStat().getN());
        Assertions.assertEquals(RefStatRange[0], kalmanLageos2.getRangeStat().getMin(),               distanceAccuracy);
        Assertions.assertEquals(RefStatRange[1], kalmanLageos2.getRangeStat().getMax(),               distanceAccuracy);
        Assertions.assertEquals(RefStatRange[2], kalmanLageos2.getRangeStat().getMean(),              distanceAccuracy);
        Assertions.assertEquals(RefStatRange[3], kalmanLageos2.getRangeStat().getStandardDeviation(), distanceAccuracy);

    }

}
