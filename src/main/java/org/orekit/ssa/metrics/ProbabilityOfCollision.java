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
package org.orekit.ssa.metrics;

import org.hipparchus.util.MathUtils;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.Alfriend1999Max;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.Laas2015;

/**
 * Container for values relative to the probability of collision :
 * <ul>
 *     <li>Value of the probability of collision.</li>
 *     <li>Name of the method with which it was computed.</li>
 *     <li>Upper and lower limit of the value if the method provides them (such as {@link Laas2015} for example).</li>
 *     <li>Flag defining if the probability was maximized in any way (such as {@link Alfriend1999Max} for example).</li>
 * </ul>
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public class ProbabilityOfCollision {

    /** Value of the probability of collision. */
    private final double value;

    /**
     * Lower limit of the probability of collision.
     * <p>
     * 0 by default.
     */
    private final double lowerLimit;

    /**
     * Upper limit of the probability of collision.
     * <p>
     * 0 by default.
     */
    private final double upperLimit;

    /** Name of the probability computing method with which this probability was calculated. */
    private final String probabilityOfCollisionMethodName;

    /**
     * Defines if this probability of collision can be considered a maximum probability of collision.
     * <p>
     * It depends on what method was used to compute this probability.
     * <p>
     * False by default.
     */
    private final boolean isMaxProbability;

    /**
     * Constructor with default values of 0 for the upper/lower limits and default false flag for maximum probability.
     *
     * @param value value of the probability of collision
     * @param probabilityOfCollisionMethodName name of the probability computing method with which this probability was
     * computed
     */
    public ProbabilityOfCollision(final double value, final String probabilityOfCollisionMethodName) {
        this(value, 0., 0., probabilityOfCollisionMethodName, false);
    }

    /**
     * Constructor with default values of 0 for the upper and lower limits.
     *
     * @param value value of the probability of collision
     * @param probabilityOfCollisionMethodName name of the probability computing method with which this probability was
     * computed
     * @param isMaxProbability flag defining if it has been computed using a maximum probability of collision method
     */
    public ProbabilityOfCollision(final double value, final String probabilityOfCollisionMethodName,
                                  final boolean isMaxProbability) {
        this(value, 0., 0., probabilityOfCollisionMethodName, isMaxProbability);
    }

    /**
     * Constructor.
     *
     * @param value value of the probability of collision
     * @param lowerLimit lower limit of the probability of collision
     * @param upperLimit upper limit of the probability of collision
     * @param probabilityOfCollisionMethodName name of the probability computing method with which this probability was
     * computed
     * @param isMaxProbability flag indicating if this method computes a maximum probability of collision
     */
    public ProbabilityOfCollision(final double value, final double lowerLimit, final double upperLimit,
                                  final String probabilityOfCollisionMethodName, final boolean isMaxProbability) {

        // Check that inputs are valid
        MathUtils.checkRangeInclusive(value, 0, 1);
        MathUtils.checkRangeInclusive(lowerLimit, 0, 1);
        MathUtils.checkRangeInclusive(upperLimit, 0, 1);

        // initialization
        this.value                            = value;
        this.lowerLimit                       = lowerLimit;
        this.upperLimit                       = upperLimit;
        this.probabilityOfCollisionMethodName = probabilityOfCollisionMethodName;
        this.isMaxProbability                 = isMaxProbability;
    }

    /** Get value of the probability of collision.
     * @return value of the probability of collision
     */
    public double getValue() {
        return value;
    }

    /** Get lower limit of the probability of collision value.
     * @return lower limit of the probability of collision value, 0 by default
     */
    public double getLowerLimit() {
        return lowerLimit;
    }

    /** Get upper limit of the probability of collision value.
     * @return upper limit of the probability of collision value, 0 by default
     */
    public double getUpperLimit() {
        return upperLimit;
    }

    /** Get name of the probability computing method with which this probability was computed.
     * @return name of the probability computing method with which this probability was computed
     */
    public String getProbabilityOfCollisionMethodName() {
        return probabilityOfCollisionMethodName;
    }

    /** Get flag that defines if this probability of collision can be considered a maximum probability of collision.
     * @return flag that defines if this probability of collision can be considered a maximum probability of collision
     */
    public boolean isMaxProbability() {
        return isMaxProbability;
    }

}
