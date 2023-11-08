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
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;

/**
 * Compute the probability of collision using the method described in : <br> "Chan, K. “Collision Probability Analyses for
 * Earth Orbiting Satellites.” In Space Cooperation into the 21st Century: 7th AAS/JRS/CSA Symposium, International Space
 * Conference of Pacific-Basin Societies (ISCOPS; formerly PISSTA) (July 15-18, 1997, Nagasaki, Japan), edited by Peter M.
 * Bainum, et al., 1033-1048. Advances in the Astronautical Sciences Series 96. San Diego, California: Univelt, 1997. (Zeroth
 * order analytical expression).
 * <p>
 * This method is also described in depth in : "CHAN, F. Kenneth, et al. Spacecraft collision probability. El Segundo, CA :
 * Aerospace Press, 2008."
 * <p>
 * It assumes :
 * <ul>
 *     <li>Short encounter leading to a linear relative motion.</li>
 *     <li>Spherical collision object.</li>
 *     <li>Uncorrelated positional covariance.</li>
 *     <li>Gaussian distribution of the position uncertainties.</li>
 *     <li>Deterministic velocity i.e. no velocity uncertainties.</li>
 *     <li>Approximate ellipse by a disk</li>
 * </ul>
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public class Chan1997 extends AbstractShortTermEncounter2DPOCMethod {

    /** Empty constructor. */
    public Chan1997() {
        super(ShortTermEncounter2DPOCMethodType.CHAN_1997.name());
    }

    /** {@inheritDoc} */
    public ProbabilityOfCollision compute(final double xm, final double ym,
                                          final double sigmaX, final double sigmaY,
                                          final double radius) {

        // Intermediary terms u and v
        final double u = radius * radius / (sigmaX * sigmaY);
        final double v = (xm * xm / (sigmaX * sigmaX)) + (ym * ym / (sigmaY * sigmaY));

        // Number of terms M recommended by Chan
        final int M;

        if (u <= 0.01 || v <= 1) {
            M = 3;
        } else if (u > 0.01 && u <= 1 || v > 1 && v <= 9) {
            M = 10;
        } else if (u > 1 && u <= 25 || v > 9 && v <= 25) {
            M = 20;
        } else {
            M = 60;
        }

        double t   = 1.0;
        double s   = 1.0;
        double sum = 1.0;

        // first iteration
        double value = FastMath.exp(-v * 0.5) * t - FastMath.exp(-(u + v) * 0.5) * t * sum;

        // iterative expression
        for (int i = 1; i < M; i++) {
            t     = (v * 0.5) / i * t;
            s     = (u * 0.5) / i * s;
            sum   = sum + s;
            value = MathArrays.linearCombination(1, value, FastMath.exp(-v * 0.5), t) -
                    FastMath.exp(-(u + v) * 0.5) * t * sum;
        }

        return new ProbabilityOfCollision(value, getName(), isAMaximumProbabilityOfCollisionMethod());
    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(final T xm, final T ym,
                                                                                      final T sigmaX, final T sigmaY,
                                                                                      final T radius) {

        // Intermediary terms u and v
        final T u = radius.pow(2).divide(sigmaX.multiply(sigmaY));
        final T v = xm.divide(sigmaX).pow(2).add(ym.divide(sigmaY).pow(2));

        // Number of terms M recommended by Chan
        final int M;

        if (u.getReal() <= 0.01 || v.getReal() <= 1) {
            M = 3;
        } else if (u.getReal() > 0.01 && u.getReal() <= 1 || v.getReal() > 1 && v.getReal() <= 9) {
            M = 10;
        } else if (u.getReal() > 1 && u.getReal() <= 25 || v.getReal() > 9 && v.getReal() <= 25) {
            M = 20;
        } else {
            M = 60;
        }

        final Field<T> field = radius.getField();

        T t   = field.getOne();
        T s   = field.getOne();
        T sum = field.getOne();

        // first iteration
        T value = v.multiply(-0.5).exp().multiply(t)
                   .subtract(u.add(v).multiply(-0.5).exp().multiply(t).multiply(sum));

        // iterative expression
        for (int i = 1; i < M; i++) {
            t   = v.multiply(0.5).divide(i).multiply(t);
            s   = u.multiply(0.5).divide(i).multiply(s);
            sum = sum.add(s);

            value = value.add(v.multiply(-0.5).exp().multiply(t)
                               .subtract(u.add(v).multiply(-0.5).exp().multiply(t).multiply(sum)));
        }

        return new FieldProbabilityOfCollision<>(value, getName(), isAMaximumProbabilityOfCollisionMethod());
    }

    /** {@inheritDoc} */
    @Override
    public ShortTermEncounter2DPOCMethodType getType() {
        return ShortTermEncounter2DPOCMethodType.CHAN_1997;
    }

}
