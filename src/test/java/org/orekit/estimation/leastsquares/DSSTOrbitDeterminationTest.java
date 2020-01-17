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
package org.orekit.estimation.leastsquares;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.KeyValueFileParser;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.common.AbstractOrbitDetermination;
import org.orekit.estimation.common.ParameterKey;
import org.orekit.estimation.common.ResultBatchLeastSquares;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

public class DSSTOrbitDeterminationTest extends AbstractOrbitDetermination<DSSTPropagatorBuilder> {

    /** Gravity field. */
    private UnnormalizedSphericalHarmonicsProvider gravityField;

    /** {@inheritDoc} */
    @Override
    protected void createGravityField(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {

        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = FastMath.min(degree, parser.getInt(ParameterKey.CENTRAL_BODY_ORDER));
        gravityField = GravityFieldFactory.getUnnormalizedProvider(degree, order);
    }

    /** {@inheritDoc} */
    @Override
    protected double getMu() {
        return gravityField.getMu();
    }

    /** {@inheritDoc} */
    @Override
    protected DSSTPropagatorBuilder createPropagatorBuilder(final Orbit referenceOrbit,
                                                            final ODEIntegratorBuilder builder,
                                                            final double positionScale) {
        final EquinoctialOrbit equiOrbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(referenceOrbit);
        return new DSSTPropagatorBuilder(equiOrbit, builder, positionScale,
                                         PropagationType.MEAN, PropagationType.MEAN);
    }

    /** {@inheritDoc} */
    @Override
    protected void setMass(final DSSTPropagatorBuilder propagatorBuilder,
                                final double mass) {
        propagatorBuilder.setMass(mass);
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setGravity(final DSSTPropagatorBuilder propagatorBuilder,
                                           final OneAxisEllipsoid body) {

        // tesseral terms
        final DSSTForceModel tesseral = new DSSTTesseral(body.getBodyFrame(),
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityField,
                                                         gravityField.getMaxDegree(), gravityField.getMaxOrder(), 4, 12,
                                                         gravityField.getMaxDegree(), gravityField.getMaxOrder(), 4);
        propagatorBuilder.addForceModel(tesseral);

        // zonal terms
        final DSSTForceModel zonal = new DSSTZonal(gravityField, gravityField.getMaxDegree(), 4,
                                                   2 * gravityField.getMaxDegree() + 1);
        propagatorBuilder.addForceModel(zonal);

        // gather all drivers
        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.addAll(Arrays.asList(tesseral.getParametersDrivers()));
        drivers.addAll(Arrays.asList(tesseral.getParametersDrivers()));
        return drivers.toArray(new ParameterDriver[0]);

    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setOceanTides(final DSSTPropagatorBuilder propagatorBuilder,
                                              final IERSConventions conventions,
                                              final OneAxisEllipsoid body,
                                              final int degree, final int order) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Ocean tides not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setSolidTides(final DSSTPropagatorBuilder propagatorBuilder,
                                              final IERSConventions conventions,
                                              final OneAxisEllipsoid body,
                                              final CelestialBody[] solidTidesBodies) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                  "Solid tides not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setThirdBody(final DSSTPropagatorBuilder propagatorBuilder,
                                             final CelestialBody thirdBody) {
        final DSSTForceModel thirdBodyModel = new DSSTThirdBody(thirdBody, gravityField.getMu());
        propagatorBuilder.addForceModel(thirdBodyModel);
        return thirdBodyModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setDrag(final DSSTPropagatorBuilder propagatorBuilder,
                                        final Atmosphere atmosphere, final DragSensitive spacecraft) {
        final DSSTForceModel dragModel = new DSSTAtmosphericDrag(atmosphere, spacecraft, gravityField.getMu());
        propagatorBuilder.addForceModel(dragModel);
        return dragModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setSolarRadiationPressure(final DSSTPropagatorBuilder propagatorBuilder, final CelestialBody sun,
                                                          final double equatorialRadius, final RadiationSensitive spacecraft) {
        final DSSTForceModel srpModel = new DSSTSolarRadiationPressure(sun, equatorialRadius,
                                                                       spacecraft, gravityField.getMu());
        propagatorBuilder.addForceModel(srpModel);
        return srpModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setRelativity(final DSSTPropagatorBuilder propagatorBuilder) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Relativity not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setPolynomialAcceleration(final DSSTPropagatorBuilder propagatorBuilder,
                                                          final String name, final Vector3D direction, final int degree) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Polynomial acceleration not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected void setAttitudeProvider(final DSSTPropagatorBuilder propagatorBuilder,
                                       final AttitudeProvider attitudeProvider) {
        propagatorBuilder.setAttitudeProvider(attitudeProvider);
    }

    @Test
    // Orbit determination using only mean elements for Lageos2 based on SLR (range) measurements
    // For better accuracy, adding short period terms is necessary
    public void testLageos2()
        throws URISyntaxException, IllegalArgumentException, IOException,
               OrekitException, ParseException {

        // input in resources directory
        final String inputPath = DSSTOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/Lageos2/dsst_od_test_Lageos2.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data access
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        //orbit determination run.
        ResultBatchLeastSquares odLageos2 = runBLS(input, false);

        //test
        //definition of the accuracy for the test
        final double distanceAccuracy = 579;
        final double velocityAccuracy = 1.4;

        //test on the convergence
        final int numberOfIte  = 9;
        final int numberOfEval = 9;

        Assert.assertEquals(numberOfIte, odLageos2.getNumberOfIteration());
        Assert.assertEquals(numberOfEval, odLageos2.getNumberOfEvaluation());

        //test on the estimated position and velocity
        final Vector3D estimatedPos = odLageos2.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = odLageos2.getEstimatedPV().getVelocity();

        //final Vector3D refPos = new Vector3D(-5532124.989973327, 10025700.01763335, -3578940.840115321);
        //final Vector3D refVel = new Vector3D(-3871.2736402553, -607.8775965705, 4280.9744110925);
        final Vector3D refPos = new Vector3D(-5532131.956902, 10025696.592156, -3578940.040009);
        final Vector3D refVel = new Vector3D(-3871.275109, -607.880985, 4280.972530);
        Assert.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), distanceAccuracy);
        Assert.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), velocityAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 258;
        //final double[] RefStatRange = { -2.795816, 6.171529, 0.310848, 1.657809 };
        final double[] RefStatRange = { -2.431135, 2.218644, 0.038483, 0.982017 };
        Assert.assertEquals(nbRange, odLageos2.getRangeStat().getN());
        Assert.assertEquals(RefStatRange[0], odLageos2.getRangeStat().getMin(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[1], odLageos2.getRangeStat().getMax(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[2], odLageos2.getRangeStat().getMean(),              distanceAccuracy);
        Assert.assertEquals(RefStatRange[3], odLageos2.getRangeStat().getStandardDeviation(), distanceAccuracy);

    }

    @Test
    // Orbit determination using only mean elements for GNSS satellite based on range measurements
    // For better accuracy, adding short period terms is necessary
    public void testGNSS()
        throws URISyntaxException, IllegalArgumentException, IOException,
               OrekitException, ParseException {

        // input in resources directory
        final String inputPath = DSSTOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/GNSS/dsst_od_test_GPS07.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data access
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        //orbit determination run.
        ResultBatchLeastSquares odGNSS = runBLS(input, false);

        //test
        //definition of the accuracy for the test
        final double distanceAccuracy = 59;
        final double velocityAccuracy = 0.23;

        //test on the convergence
        final int numberOfIte  = 2;
        final int numberOfEval = 3;

        Assert.assertEquals(numberOfIte, odGNSS.getNumberOfIteration());
        Assert.assertEquals(numberOfEval, odGNSS.getNumberOfEvaluation());

        //test on the estimated position and velocity (reference from IGS-MGEX file com18836.sp3)
        final Vector3D estimatedPos = odGNSS.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = odGNSS.getEstimatedPV().getVelocity();
        final Vector3D refPos = new Vector3D(-2747606.680868164, 22572091.30648564, 13522761.402325712);
        final Vector3D refVel = new Vector3D(-2729.5151218788005, 1142.6629459030657, -2523.9055974487947);
        Assert.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), distanceAccuracy);
        Assert.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), velocityAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 4009;
        final double[] RefStatRange = { -83.945, 59.365, 0.0, 20.857 };
        Assert.assertEquals(nbRange, odGNSS.getRangeStat().getN());
        Assert.assertEquals(RefStatRange[0], odGNSS.getRangeStat().getMin(),               1.0e-3);
        Assert.assertEquals(RefStatRange[1], odGNSS.getRangeStat().getMax(),               1.0e-3);
        Assert.assertEquals(RefStatRange[2], odGNSS.getRangeStat().getMean(),              0.23);
        Assert.assertEquals(RefStatRange[3], odGNSS.getRangeStat().getStandardDeviation(), 1.0e-3);

    }

}
