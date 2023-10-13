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
import org.hipparchus.util.FastMath;

/**
 * Compute the probability of collision assuming the worst case described in : "Kyle Alfriend, Maruthi Akella, Joseph
 * Frisbee, James Foster, Deok-Jin Lee, and Matthew Wilkins. Probability of ProbabilityOfCollision Error Analysis. Space
 * Debris, 1(1):21–35, 1999.".
 * <p>It assumes:
 *     <ul>
 *         <li>Short encounter leading to a linear relative motion.</li>
 *         <li>Spherical collision object.</li>
 *         <li>Uncorrelated positional covariance.</li>
 *         <li>Gaussian distribution of the position uncertainties.</li>
 *         <li>Deterministic velocity i.e. no velocity uncertainties.</li>
 *         <li>Both objects are in circular orbits (eq 14).</li>
 *         <li>Probability density function is constant over the collision circle (eq 18).</li>
 *         <li>Covariance multiplied by a coefficient KSquared = MahalanobisDistanceSquared / 2 (eq 19-20).</li>
 *     </ul>
 * <p>
 * By assuming a constant probability density function over the collision circle this method will,
 * <b>most of the time</b>, give much higher probability of collision than other regular methods.
 * That is why it is qualified as a maximum probability of collision computing method.
 *
 * @author Vincent Cucchietti
 * @see <a href="https://en.wikipedia.org/wiki/Mahalanobis_distance">Mahalanobis distance.</a>
 * @since 12.0
 */
public class Alfriend1999Max extends AbstractAlfriend1999 {

    /** Empty constructor. */
    public Alfriend1999Max() {
        super(ShortTermEncounter2DPOCMethodType.ALFRIEND_1999_MAX.name());
    }

    /**
     * Compute the value of the probability of collision.
     *
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     * @param squaredMahalanobisDistance squared Mahalanobis distance
     * @param covarianceMatrixDeterminant covariance matrix determinant
     *
     * @return value of the probability of collision
     */
    @Override
    public double computeValue(final double radius, final double squaredMahalanobisDistance,
                               final double covarianceMatrixDeterminant) {
        return radius * radius / (squaredMahalanobisDistance * FastMath.sqrt(covarianceMatrixDeterminant) * FastMath.E);
    }

    /**
     * Compute the value of the probability of collision.
     *
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     * @param squaredMahalanobisDistance squared Mahalanobis distance
     * @param covarianceMatrixDeterminant covariance matrix determinant
     * @param <T> type of the field elements
     *
     * @return value of the probability of collision
     */
    @Override
    public <T extends CalculusFieldElement<T>> T computeValue(final T radius, final T squaredMahalanobisDistance,
                                                              final T covarianceMatrixDeterminant) {
        return radius.multiply(radius).divide(squaredMahalanobisDistance.multiply(covarianceMatrixDeterminant.sqrt())
                                                                        .multiply(FastMath.E));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAMaximumProbabilityOfCollisionMethod() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public ShortTermEncounter2DPOCMethodType getType() {
        return ShortTermEncounter2DPOCMethodType.ALFRIEND_1999_MAX;
    }
}
