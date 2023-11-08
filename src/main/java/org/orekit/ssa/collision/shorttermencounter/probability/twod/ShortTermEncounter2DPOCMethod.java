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

import org.hipparchus.CalculusFieldElement;
import org.orekit.files.ccsds.ndm.cdm.Cdm;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldStateCovariance;
import org.orekit.propagation.StateCovariance;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;

/**
 * Interface common to all short-term encounter probability of collision computing methods.
 * <p>
 * All the methods implementing this interface will at least assume the followings :
 * <ul>
 *     <li>Short term encounter leading to a linear relative motion.</li>
 *     <li>Spherical collision object.</li>
 *     <li>Uncorrelated positional covariance.</li>
 *     <li>Gaussian distribution of the position uncertainties.</li>
 *     <li>Deterministic velocity i.e. no velocity uncertainties.</li>
 * </ul>
 * As listed in the assumptions, methods implementing this interface are to be used in short encounter,
 * meaning that there must be a high relative velocity. For ease of computation, the resulting swept volume
 * is extended to infinity so that the integral becomes bivariate instead of trivariate (conservative hypothesis).
 * <p>
 * Consequently and if we consider Earth, methods implementing this interface are <u><b>recommended</b></u> for
 * collision happening in Low/Medium Earth Orbit (LEO and MEO) but are <u><b>not recommended</b></u> for collision
 * happening in Geostationary Earth Orbit (GEO).
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public interface ShortTermEncounter2DPOCMethod {

    /** Threshold below which values are considered equal to zero. */
    double DEFAULT_ZERO_THRESHOLD = 1e-15;

    /**
     * Compute the probability of collision using a Conjunction Data Message (CDM).
     *
     * @param cdm conjunction data message input
     * @param primaryRadius primary collision object equivalent sphere radius (m)
     * @param secondaryRadius secondary collision object equivalent sphere radius (m)
     *
     * @return probability of collision
     */
    default ProbabilityOfCollision compute(Cdm cdm, double primaryRadius, double secondaryRadius) {
        return compute(cdm, primaryRadius + secondaryRadius);
    }

    /**
     * Compute the probability of collision using a Conjunction Data Message (CDM).
     *
     * @param cdm conjunction data message input
     * @param combinedRadius combined radius (m)
     *
     * @return probability of collision
     */
    ProbabilityOfCollision compute(Cdm cdm, double combinedRadius);

    /**
     * Compute the probability of collision using a Conjunction Data Message (CDM).
     *
     * @param cdm conjunction data message input
     * @param primaryRadius primary collision object equivalent sphere radius (m)
     * @param secondaryRadius secondary collision object equivalent sphere radius (m)
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    default <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(Cdm cdm,
                                                                                       T primaryRadius,
                                                                                       T secondaryRadius) {
        return compute(cdm, primaryRadius.add(secondaryRadius));
    }

    /**
     * Compute the probability of collision using a Conjunction Data Message (CDM).
     *
     * @param cdm conjunction data message input
     * @param combinedRadius combined radius (m)
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    default <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(Cdm cdm,
                                                                                       T combinedRadius) {
        return compute(cdm, combinedRadius, DEFAULT_ZERO_THRESHOLD);
    }

    /**
     * Compute the probability of collision using a Conjunction Data Message (CDM).
     *
     * @param cdm conjunction data message input
     * @param combinedRadius combined radius (m)
     * @param zeroThreshold threshold below which values are considered equal to zero
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(Cdm cdm, T combinedRadius,
                                                                               double zeroThreshold);

    /**
     * Compute the probability of collision using parameters necessary for creating a
     * {@link ShortTermEncounter2DDefinition collision definition} instance.
     *
     * @param primaryAtTCA primary collision object spacecraft state at time of closest approach
     * @param primaryCovariance primary collision object covariance
     * @param primaryRadius primary collision object equivalent sphere radius (m)
     * @param secondaryAtTCA secondary collision object spacecraft state at time of closest approach
     * @param secondaryCovariance secondary collision object covariance
     * @param secondaryRadius secondary collision object equivalent sphere radius (m)
     *
     * @return probability of collision
     */
    default ProbabilityOfCollision compute(Orbit primaryAtTCA,
                                           StateCovariance primaryCovariance,
                                           double primaryRadius,
                                           Orbit secondaryAtTCA,
                                           StateCovariance secondaryCovariance,
                                           double secondaryRadius) {
        return compute(primaryAtTCA, primaryCovariance, secondaryAtTCA, secondaryCovariance,
                       primaryRadius + secondaryRadius);
    }

    /**
     * Compute the probability of collision using parameters necessary for creating a
     * {@link ShortTermEncounter2DDefinition collision definition} instance.
     *
     * @param primaryAtTCA primary collision object spacecraft state at time of closest approach
     * @param primaryCovariance primary collision object covariance
     * @param secondaryAtTCA secondary collision object spacecraft state at time of closest approach
     * @param secondaryCovariance secondary collision object covariance
     * @param combinedRadius combined radius (m)
     *
     * @return probability of collision
     */
    default ProbabilityOfCollision compute(Orbit primaryAtTCA,
                                           StateCovariance primaryCovariance,
                                           Orbit secondaryAtTCA,
                                           StateCovariance secondaryCovariance,
                                           double combinedRadius) {
        return compute(primaryAtTCA, primaryCovariance, secondaryAtTCA, secondaryCovariance,
                       combinedRadius, DEFAULT_ZERO_THRESHOLD);
    }

    /**
     * Compute the probability of collision using parameters necessary for creating a
     * {@link ShortTermEncounter2DDefinition collision definition} instance.
     *
     * @param primaryAtTCA primary collision object spacecraft state at time of closest approach
     * @param primaryCovariance primary collision object covariance
     * @param secondaryAtTCA secondary collision object spacecraft state at time of closest approach
     * @param secondaryCovariance secondary collision object covariance
     * @param combinedRadius combined radius (m)
     * @param zeroThreshold threshold below which values are considered equal to zero
     *
     * @return probability of collision
     */
    ProbabilityOfCollision compute(Orbit primaryAtTCA, StateCovariance primaryCovariance,
                                   Orbit secondaryAtTCA, StateCovariance secondaryCovariance,
                                   double combinedRadius, double zeroThreshold);

    /**
     * Compute the probability of collision using parameters necessary for creating a
     * {@link ShortTermEncounter2DDefinition collision definition} instance.
     *
     * @param primaryAtTCA primary collision object spacecraft state at time of closest approach
     * @param primaryCovariance primary collision object covariance
     * @param primaryRadius primary collision object equivalent sphere radius (m)
     * @param secondaryAtTCA secondary collision object spacecraft state at time of closest approach
     * @param secondaryCovariance secondary collision object covariance
     * @param secondaryRadius secondary collision object equivalent sphere radius (m)
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    default <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(FieldOrbit<T> primaryAtTCA,
                                                                                       FieldStateCovariance<T> primaryCovariance,
                                                                                       T primaryRadius,
                                                                                       FieldOrbit<T> secondaryAtTCA,
                                                                                       FieldStateCovariance<T> secondaryCovariance,
                                                                                       T secondaryRadius) {
        return compute(primaryAtTCA, primaryCovariance, secondaryAtTCA, secondaryCovariance,
                       primaryRadius.add(secondaryRadius));
    }

    /**
     * Compute the probability of collision using parameters necessary for creating a
     * {@link ShortTermEncounter2DDefinition collision definition} instance.
     *
     * @param primaryAtTCA primary collision object spacecraft state at time of closest approach
     * @param primaryCovariance primary collision object covariance
     * @param secondaryAtTCA secondary collision object spacecraft state at time of closest approach
     * @param secondaryCovariance secondary collision object covariance
     * @param combinedRadius secondary collision object equivalent sphere radius (m)
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    default <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(FieldOrbit<T> primaryAtTCA,
                                                                                       FieldStateCovariance<T> primaryCovariance,
                                                                                       FieldOrbit<T> secondaryAtTCA,
                                                                                       FieldStateCovariance<T> secondaryCovariance,
                                                                                       T combinedRadius) {
        return compute(primaryAtTCA, primaryCovariance, secondaryAtTCA, secondaryCovariance,
                       combinedRadius, DEFAULT_ZERO_THRESHOLD);
    }

    /**
     * Compute the probability of collision using parameters necessary for creating a
     * {@link ShortTermEncounter2DDefinition collision definition} instance.
     *
     * @param primaryAtTCA primary collision object spacecraft state at time of closest approach
     * @param primaryCovariance primary collision object covariance
     * @param secondaryAtTCA secondary collision object spacecraft state at time of closest approach
     * @param secondaryCovariance secondary collision object covariance
     * @param combinedRadius combined radius (m)
     * @param zeroThreshold threshold below which values are considered equal to zero
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(FieldOrbit<T> primaryAtTCA,
                                                                               FieldStateCovariance<T> primaryCovariance,
                                                                               FieldOrbit<T> secondaryAtTCA,
                                                                               FieldStateCovariance<T> secondaryCovariance,
                                                                               T combinedRadius,
                                                                               double zeroThreshold);

    /**
     * Compute the probability of collision using given collision definition.
     *
     * @param encounter encounter definition between a primary and a secondary collision object
     *
     * @return probability of collision
     */
    default ProbabilityOfCollision compute(ShortTermEncounter2DDefinition encounter) {
        return compute(encounter, DEFAULT_ZERO_THRESHOLD);
    }

    /**
     * Compute the probability of collision using given collision definition.
     *
     * @param encounter encounter definition between a primary and a secondary collision object
     * @param zeroThreshold threshold below which values are considered equal to zero
     *
     * @return probability of collision
     */
    ProbabilityOfCollision compute(ShortTermEncounter2DDefinition encounter, double zeroThreshold);

    /**
     * Compute the probability of collision using given collision definition.
     *
     * @param encounter encounter definition between a primary and a secondary collision object
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    default <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(
            FieldShortTermEncounter2DDefinition<T> encounter) {
        return compute(encounter, DEFAULT_ZERO_THRESHOLD);
    }

    /**
     * Compute the probability of collision using given collision definition.
     *
     * @param encounter encounter definition between a primary and a secondary collision object
     * @param zeroThreshold threshold below which values are considered equal to zero
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(
            FieldShortTermEncounter2DDefinition<T> encounter,
            double zeroThreshold);

    /**
     * Compute the probability of collision using arguments specific to the rotated encounter frame.
     * <p>
     * The rotated encounter frame is define by the initial encounter frame (defined in
     * {@link ShortTermEncounter2DDefinition}) rotated by the rotation matrix which is used to diagonalize the combined
     * covariance matrix.
     * </p>
     *
     * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
     * (m)
     * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
     * (m)
     * @param sigmaX square root of the x-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param sigmaY square root of the y-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     *
     * @return probability of collision
     */
    ProbabilityOfCollision compute(double xm, double ym, double sigmaX, double sigmaY, double radius);

    /**
     * Compute the probability of collision using arguments specific to the rotated encounter frame.
     * <p>
     * The rotated encounter frame is define by the initial encounter frame (defined in
     * {@link ShortTermEncounter2DDefinition}) rotated by the rotation matrix which is used to diagonalize the combined
     * covariance matrix.
     * </p>
     *
     * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
     * (m)
     * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
     * (m)
     * @param sigmaX square root of the x-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param sigmaY square root of the y-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(T xm, T ym, T sigmaX, T sigmaY, T radius);

    /** Get type of the method.
     * @return type of the method
     */
    ShortTermEncounter2DPOCMethodType getType();

    /** Get name of the method.
     * @return name of the method
     */
    String getName();

    /** Get flag that defines if the method is a maximum probability of collision computing method.
     * @return flag that defines if the method is a maximum probability of collision computing method
     */
    boolean isAMaximumProbabilityOfCollisionMethod();
}
