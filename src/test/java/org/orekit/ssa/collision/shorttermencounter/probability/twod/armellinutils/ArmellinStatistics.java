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
package org.orekit.ssa.collision.shorttermencounter.probability.twod.armellinutils;

import org.hipparchus.Field;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldStateCovariance;
import org.orekit.propagation.StateCovariance;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.Alfano2005;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.Alfriend1999;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.Alfriend1999Max;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.FieldShortTermEncounter2DDefinition;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.ShortTermEncounter2DDefinition;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.ShortTermEncounter2DPOCMethod;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

import java.io.IOException;
import java.util.List;

/**
 * Prepare specific statistics for specific test which use the Armellin's data derived from the ESA collision avoidance
 * challenge available <a href="https://github.com/arma1978/conjunction">here</a>.
 */
public class ArmellinStatistics {

    private ArmellinStatistics() {
        // Empty constructor
    }

    /**
     * Get the statistics about the relative difference of the maximum probability of collision from Armellin's data
     * (computed with Alfano method) and the {@link Alfano2005}.
     *
     * @param armellinDataRowList list of row loaded from Armellin data file
     *
     * @return Statistics about the relative difference of the probability of collision from Armellin's data (computed * with
     * Alfano method) and the {@link Alfano2005}
     */
    public static DescriptiveStatistics getMaxProbabilityOfCollisionRelativeDifferenceStatistics(
            final List<ArmellinDataRow> armellinDataRowList) {

        final ShortTermEncounter2DPOCMethod method = new Alfriend1999Max();

        final DescriptiveStatistics statistics = new DescriptiveStatistics();

        for (final ArmellinDataRow row : armellinDataRowList) {

            final ShortTermEncounter2DDefinition collision = getCollisionFromArmellinDataRow(row);

            final ProbabilityOfCollision result = method.compute(collision);

            final double absoluteDifference = FastMath.abs(result.getValue() - row.getProbabilityOfCollisionMax());

            final double relativeDifference = absoluteDifference / row.getProbabilityOfCollisionMax();

            statistics.addValue(relativeDifference);
        }

        return statistics;
    }

    /**
     * Get a {@link ShortTermEncounter2DDefinition} instance from an Armellin's data row.
     *
     * @param row Armellin's data row.
     *
     * @return {@link ShortTermEncounter2DDefinition} instance from an Armellin's data row
     */
    private static ShortTermEncounter2DDefinition getCollisionFromArmellinDataRow(final ArmellinDataRow row) {

        // Define the default frame, time of closest approach and mu
        final Frame defaultFrame = FramesFactory.getEME2000();

        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();

        final double mu = Constants.IERS2010_EARTH_MU;

        final double combinedRadius = row.getCombinedRadius();

        // Define the primary collision object according to current row in Armellin's data
        final double primaryRadius = combinedRadius / 2;
        final Orbit primary = new CartesianOrbit(row.getPrimaryPVCoordinates(), defaultFrame,
                                                 timeOfClosestApproach, mu);
        final RealMatrix primaryCovarianceMatrix = row.getPrimaryCovarianceMatrixInPrimaryRTN();
        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrix, timeOfClosestApproach,
                                                                      LOFType.QSW_INERTIAL);

        // Define the secondary collision object according to current row in Armellin's data
        final double secondaryRadius = combinedRadius / 2;
        final Orbit secondary = new CartesianOrbit(row.getSecondaryPVCoordinates(), defaultFrame,
                                                   timeOfClosestApproach, mu);
        final RealMatrix secondaryCovarianceMatrix = row.getSecondaryCovarianceMatrixInSecondaryRTN();
        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrix,
                                                                        timeOfClosestApproach,
                                                                        LOFType.QSW_INERTIAL);

        return new ShortTermEncounter2DDefinition(primary, primaryCovariance, primaryRadius,
                                                  secondary, secondaryCovariance, secondaryRadius);
    }

    /**
     * Get the statistics about the relative difference of the maximum probability of collision from Armellin's data
     * (computed with Alfano method) and the {@link Alfano2005}.
     *
     * @param armellinDataRowList list of row loaded from Armellin data file
     *
     * @return Statistics about the relative difference of the probability of collision from Armellin's data (computed * with
     * Alfano method) and the {@link Alfano2005}
     */
    public static DescriptiveStatistics getMaxFieldProbabilityOfCollisionRelativeDifferenceStatistics(
            final List<ArmellinDataRow> armellinDataRowList) {

        final ShortTermEncounter2DPOCMethod method = new Alfriend1999Max();

        final DescriptiveStatistics statistics = new DescriptiveStatistics();

        for (final ArmellinDataRow row : armellinDataRowList) {

            final FieldShortTermEncounter2DDefinition<Binary64> collision = getFieldCollisionFromArmellinDataRow(row);

            final FieldProbabilityOfCollision<Binary64> result = method.compute(collision);

            final double absoluteDifference = FastMath.abs(result.getValue().getReal() - row.getProbabilityOfCollisionMax());

            final double relativeDifference = absoluteDifference / row.getProbabilityOfCollisionMax();

            statistics.addValue(relativeDifference);
        }

        return statistics;
    }

    /**
     * Get a {@link ShortTermEncounter2DDefinition} instance from an Armellin's data row.
     *
     * @param row Armellin's data row.
     *
     * @return {@link ShortTermEncounter2DDefinition} instance from an Armellin's data row
     */
    private static FieldShortTermEncounter2DDefinition<Binary64> getFieldCollisionFromArmellinDataRow(
            final ArmellinDataRow row) {

        // Define the default frame, time of closest approach and mu
        final Field<Binary64> field = Binary64Field.getInstance();

        final Frame defaultFrame = FramesFactory.getEME2000();

        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);

        final Binary64 mu = new Binary64(Constants.IERS2010_EARTH_MU);

        final Binary64 combinedRadius = new Binary64(row.getCombinedRadius());

        // Define the primary collision object according to current row in Armellin's data
        final Binary64 primaryRadius = combinedRadius.multiply(0.5);
        final FieldOrbit<Binary64> primary =
                new FieldCartesianOrbit<>(new FieldPVCoordinates<>(field, row.getPrimaryPVCoordinates()), defaultFrame,
                                          timeOfClosestApproach, mu);
        final FieldMatrix<Binary64> primaryCovarianceMatrix = row.getPrimaryFieldCovarianceMatrixInPrimaryRTN();
        final FieldStateCovariance<Binary64> primaryCovariance = new FieldStateCovariance<>(primaryCovarianceMatrix,
                                                                                            timeOfClosestApproach,
                                                                                            LOFType.QSW_INERTIAL);

        // Define the secondary collision object according to current row in Armellin's data
        final Binary64 secondaryRadius = combinedRadius.multiply(0.5);
        final FieldOrbit<Binary64> secondary =
                new FieldCartesianOrbit<>(new FieldPVCoordinates<>(field, row.getSecondaryPVCoordinates()), defaultFrame,
                                          timeOfClosestApproach, mu);
        final FieldMatrix<Binary64> secondaryCovarianceMatrix = row.getSecondaryFieldCovarianceMatrixInPrimaryRTN();
        final FieldStateCovariance<Binary64> secondaryCovariance = new FieldStateCovariance<>(secondaryCovarianceMatrix,
                                                                                              timeOfClosestApproach,
                                                                                              LOFType.QSW_INERTIAL);

        return new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance, primaryRadius,
                                                         secondary, secondaryCovariance, secondaryRadius);
    }

    /**
     * Get the statistics about the relative difference of the probability of collision from Armellin's data (computed with
     * Alfano method) and {@link Alfano2005}.
     *
     * @param armellinDataRowList list of row loaded from Armellin data file
     *
     * @return Statistics about the relative difference of the probability of collision from Armellin's data (computed * with
     * Alfano method) and {@link Alfano2005}
     */
    public static DescriptiveStatistics getAlfanoProbabilityOfCollisionRelativeDifferenceStatistics(
            final List<ArmellinDataRow> armellinDataRowList) {

        final ShortTermEncounter2DPOCMethod method = new Alfano2005();

        final DescriptiveStatistics statistics = new DescriptiveStatistics();

        for (final ArmellinDataRow row : armellinDataRowList) {

            final ShortTermEncounter2DDefinition collision = getCollisionFromArmellinDataRow(row);

            final ProbabilityOfCollision result = method.compute(collision);

            final double absoluteDifference = FastMath.abs(result.getValue() - row.getAlfanoProbabilityOfcollision());

            final double relativeDifference = absoluteDifference / row.getAlfanoProbabilityOfcollision();

            statistics.addValue(relativeDifference);
        }

        return statistics;
    }

    /**
     * Get the statistics about the relative difference of the probability of collision from Armellin's data (computed with
     * simple method) and {@link Alfriend1999}.
     *
     * @param armellinDataRowList list of row loaded from Armellin data file
     *
     * @return Statistics about the relative difference of the probability of collision from Armellin's data (computed * with
     * simple method) and {@link Alfriend1999}
     */
    public static DescriptiveStatistics getAlfriend1999ProbabilityOfCollisionRelativeDifferenceStatistics(
            final List<ArmellinDataRow> armellinDataRowList) {

        final ShortTermEncounter2DPOCMethod method = new Alfriend1999();

        final DescriptiveStatistics statistics = new DescriptiveStatistics();

        for (final ArmellinDataRow row : armellinDataRowList) {

            final ShortTermEncounter2DDefinition collision = getCollisionFromArmellinDataRow(row);

            final ProbabilityOfCollision result = method.compute(collision);

            final double absoluteDifference = FastMath.abs(result.getValue() - row.getProbabilityOfCollisionApprox());

            final double relativeDifference = absoluteDifference / row.getProbabilityOfCollisionApprox();

            statistics.addValue(relativeDifference);
        }

        return statistics;
    }

    /**
     * Get the statistics about the relative difference of the probability of collision from Armellin's data (computed with
     * simple method) and {@link Alfriend1999}.
     *
     * @param armellinDataRowList list of row loaded from Armellin data file
     *
     * @return Statistics about the relative difference of the probability of collision from Armellin's data (computed * with
     * simple method) and {@link Alfriend1999}
     */
    public static DescriptiveStatistics getAlfriend1999FieldProbabilityOfCollisionRelativeDifferenceStatistics(
            final List<ArmellinDataRow> armellinDataRowList) {

        final ShortTermEncounter2DPOCMethod method = new Alfriend1999();

        final DescriptiveStatistics statistics = new DescriptiveStatistics();

        for (final ArmellinDataRow row : armellinDataRowList) {

            final FieldShortTermEncounter2DDefinition<Binary64> collision = getFieldCollisionFromArmellinDataRow(row);

            final FieldProbabilityOfCollision<Binary64> result = method.compute(collision);

            final double absoluteDifference =
                    FastMath.abs(result.getValue().getReal() - row.getProbabilityOfCollisionApprox());

            final double relativeDifference = absoluteDifference / row.getProbabilityOfCollisionApprox();

            statistics.addValue(relativeDifference);
        }

        return statistics;
    }

    /**
     * Get the statistics about the relative difference of the Mahalanobis distance from Armellin's data and
     * {@link ShortTermEncounter2DDefinition}.
     *
     * @return Statistics about the relative difference of the Mahalanobis distance from Armellin's data and
     * {@link ShortTermEncounter2DDefinition}
     *
     * @throws IOException If Armellin's data file can't be read.
     */
    public static DescriptiveStatistics getMahalanobisDistanceRelativeDifferenceStatistics() throws
            IOException {

        final double DEFAULT_ZERO_THRESHOLD = 1e-15;

        final List<ArmellinDataRow> armellinDataRowList = ArmellinDataLoader.load();

        final DescriptiveStatistics statistics = new DescriptiveStatistics();

        for (final ArmellinDataRow row : armellinDataRowList) {

            final ShortTermEncounter2DDefinition collision = getCollisionFromArmellinDataRow(row);

            final double result = collision.computeMahalanobisDistance(DEFAULT_ZERO_THRESHOLD);

            final double absoluteDifference = FastMath.abs(result - row.getMahalanobisDistance());

            final double relativeDifference = absoluteDifference / row.getMahalanobisDistance();

            statistics.addValue(relativeDifference);
        }

        return statistics;
    }
}
