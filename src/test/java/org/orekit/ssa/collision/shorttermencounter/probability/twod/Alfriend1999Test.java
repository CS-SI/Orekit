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
package org.orekit.ssa.collision.shorttermencounter.probability.twod;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.BlockFieldMatrix;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldStateCovariance;
import org.orekit.propagation.StateCovariance;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.armellinutils.ArmellinDataLoader;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.armellinutils.ArmellinDataRow;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.armellinutils.ArmellinStatistics;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

import java.io.IOException;
import java.util.List;

class Alfriend1999Test {

    /**
     * Simple method to compute probability of collision assuming a constant density of probability of collision.
     */
    private final ShortTermEncounter2DPOCMethod method = new Alfriend1999();

    /**
     * This method use the data from the appendix (p.13) of "Armellin, R. (2021). Collision Avoidance Maneuver Optimization
     * with a Multiple-Impulse Convex Formulation."
     */
    @Test
    @DisplayName("Test this method on Armellin appendix test case")
    void testComputeExpectedApproximatedProbabilityOfCollisionFromArmellinAppendix() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Value of combined radius from Armellin's paper appendix (m)
        final double combinedRadius = 29.71;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();
        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(2.33052185175137e3, -1.10370451050201e6, 7.10588764299718e6),
                                  new Vector3D(-7.44286282871773e3, -6.13734743652660e-1, 3.95136139293349e0)),
                primaryInertialFrame, timeOfClosestApproach, mu);
        final RealMatrix primaryCovarianceMatrixInPrimaryRTN = new BlockRealMatrix(
                new double[][] { { 9.31700905887535e1, -2.623398113500550e2, 2.360382173935300e1, 0, 0, 0 },
                                 { -2.623398113500550e2, 1.77796454279511e4, -9.331225387386501e1, 0, 0, 0 },
                                 { 2.360382173935300e1, -9.331225387386501e1, 1.917372231880040e1, 0, 0, 0 },
                                 { 0, 0, 0, 0, 0, 0 },
                                 { 0, 0, 0, 0, 0, 0 },
                                 { 0, 0, 0, 0, 0, 0 } });
        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrixInPrimaryRTN,
                                                                      timeOfClosestApproach, LOFType.QSW);
        final double primaryRadius = combinedRadius / 2;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();
        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(2.333465506263321e3, -1.103671212478364e6, 7.105914958099038e6),
                                  new Vector3D(7.353740487126315e3, -1.142814049765362e3, -1.982472259113771e2)),
                secondaryInertialFrame, timeOfClosestApproach, mu);
        final RealMatrix secondaryCovarianceMatrixInSecondaryRTN = new BlockRealMatrix(
                new double[][] { { 6.346570910720371e2, -1.962292216245289e3, 7.077413655227660e1, 0, 0, 0 },
                                 { -1.962292216245289e3, 8.199899363150306e5, 1.139823810584350e3, 0, 0, 0 },
                                 { 7.077413655227660e1, 1.139823810584350e3, 2.510340829074070e2, 0, 0, 0 },
                                 { 0, 0, 0, 0, 0, 0 },
                                 { 0, 0, 0, 0, 0, 0 },
                                 { 0, 0, 0, 0, 0, 0 } });
        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrixInSecondaryRTN,
                                                                        timeOfClosestApproach, LOFType.QSW);
        final double secondaryRadius = combinedRadius / 2;

        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN
        final ProbabilityOfCollision result = method.compute(collision);

        // THEN
        Assertions.assertEquals(0.147559, result.getValue(), 1e-6);
    }

    @Test
    @DisplayName("Test this method on Armellin's data and compare statistics")
    void testReturnAcceptableStatisticsAboutMaximumProbabilityOfCollisionWithArmellinData()
            throws IOException {
        // GIVEN
        final List<ArmellinDataRow> armellinDataRowList = ArmellinDataLoader.load();

        // WHEN
        final DescriptiveStatistics statistics =
                ArmellinStatistics.getAlfriend1999ProbabilityOfCollisionRelativeDifferenceStatistics(
                        armellinDataRowList);

        // THEN
        Assertions.assertTrue(statistics.getMean() <= 8.843564833687099E-10);
        Assertions.assertTrue(statistics.getStandardDeviation() <= 3.607777228462353E-9);
    }

    /**
     * This method use the data from the appendix (p.13) of "Armellin, R. (2021). Collision Avoidance Maneuver Optimization
     * with a Multiple-Impulse Convex Formulation."
     */
    @Test
    @DisplayName("Test this method on Armellin appendix test case")
    void testComputeExpectedApproximatedProbabilityOfCollisionFromArmellinAppendixField() {

        // GIVEN
        // Define the time of closest approach and mu
        final Field<Binary64> field = Binary64Field.getInstance();

        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);

        final Binary64 mu = new Binary64(Constants.IERS2010_EARTH_MU);

        // Value of combined radius from Armellin's paper appendix (m)
        final Binary64 combinedRadius = new Binary64(29.71);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(
                        new FieldVector3D<>(new Binary64(2.33052185175137e3), new Binary64(-1.10370451050201e6),
                                            new Binary64(7.10588764299718e6)),
                        new FieldVector3D<>(new Binary64(-7.44286282871773e3), new Binary64(-6.13734743652660e-1),
                                            new Binary64(3.95136139293349e0))),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> primaryCovarianceMatrixInPrimaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] { { new Binary64(9.31700905887535e1), new Binary64(-2.623398113500550e2),
                                     new Binary64(2.360382173935300e1), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(-2.623398113500550e2), new Binary64(1.77796454279511e4),
                                     new Binary64(-9.331225387386501e1), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(2.360382173935300e1), new Binary64(-9.331225387386501e1),
                                     new Binary64(1.917372231880040e1), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) } });

        final FieldStateCovariance<Binary64> primaryCovariance =
                new FieldStateCovariance<>(primaryCovarianceMatrixInPrimaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 primaryRadius = combinedRadius.multiply(0.5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(
                        new FieldVector3D<>(new Binary64(2.333465506263321e3), new Binary64(-1.103671212478364e6),
                                            new Binary64(7.105914958099038e6)),
                        new FieldVector3D<>(new Binary64(7.353740487126315e3), new Binary64(-1.142814049765362e3),
                                            new Binary64(-1.982472259113771e2))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> secondaryCovarianceMatrixInSecondaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] { { new Binary64(6.346570910720371e2), new Binary64(-1.962292216245289e3),
                                     new Binary64(7.077413655227660e1), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(-1.962292216245289e3), new Binary64(8.199899363150306e5),
                                     new Binary64(1.139823810584350e3), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(7.077413655227660e1), new Binary64(1.139823810584350e3),
                                     new Binary64(2.510340829074070e2), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) } });

        final FieldStateCovariance<Binary64> secondaryCovariance =
                new FieldStateCovariance<>(secondaryCovarianceMatrixInSecondaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 secondaryRadius = combinedRadius.multiply(0.5);

        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(collision);

        // THEN
        Assertions.assertEquals(0.147559, result.getValue().getReal(), 1e-6);
    }

    @Test
    @DisplayName("Test this method on Armellin's data and compare statistics")
    void testReturnAcceptableStatisticsAboutMaximumProbabilityOfCollisionWithArmellinDataField()
            throws IOException {
        // GIVEN
        final List<ArmellinDataRow> armellinDataRowList = ArmellinDataLoader.load();

        // WHEN
        final DescriptiveStatistics statistics =
                ArmellinStatistics.getAlfriend1999FieldProbabilityOfCollisionRelativeDifferenceStatistics(
                        armellinDataRowList);

        // THEN
        Assertions.assertTrue(statistics.getMean() <= 8.844620688058309E-10);
        Assertions.assertTrue(statistics.getStandardDeviation() <= 3.606826996118531E-9);
    }

    /**
     * This method use the data from the appendix (p.13) of "Armellin, R. (2021). Collision Avoidance Maneuver Optimization
     * with a Multiple-Impulse Convex Formulation."
     */
    @Test
    @DisplayName("Test this method on Armellin appendix test case with taylor approximation")
    void testReturnExpectedValueWithDerivative() {
        // GIVEN
        final DSFactory factory = new DSFactory(5, 5);

        // Data extracted from other test
        final double xmNominal     = 20.711983607206943;
        final double ymNominal     = -37.87548026356126;
        final double sigmaXNominal = 26.841611626440486;
        final double sigmaYNominal = 72.06451864988387;
        final double radiusNominal = 29.71;

        final double dxm     = 10;
        final double dym     = 18;
        final double dSigmaX = 13;
        final double dSigmaY = 30;
        final double dRadius = 15;

        final DerivativeStructure xm     = factory.variable(0, xmNominal);
        final DerivativeStructure ym     = factory.variable(1, ymNominal);
        final DerivativeStructure sigmaX = factory.variable(2, sigmaXNominal);
        final DerivativeStructure sigmaY = factory.variable(3, sigmaYNominal);
        final DerivativeStructure radius = factory.variable(4, radiusNominal);

        // WHEN
        final FieldProbabilityOfCollision<DerivativeStructure> resultNominal =
                method.compute(xm, ym, sigmaX, sigmaY, radius);
        final double taylorResult =
                resultNominal.getValue().taylor(dxm, dym, dSigmaX, dSigmaY, dRadius);
        final double exactResult = method.compute(xmNominal + dxm, ymNominal + dym, sigmaXNominal + dSigmaX,
                                                  sigmaYNominal + dSigmaY, radiusNominal + dRadius).getValue();

        // THEN
        Assertions.assertEquals(0.147559, resultNominal.getValue().getReal(), 1e-6);
        Assertions.assertEquals(exactResult, taylorResult, 1e-3);
    }

}
