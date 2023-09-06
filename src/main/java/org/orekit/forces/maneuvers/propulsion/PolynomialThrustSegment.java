/* Copyright 2023 Luc Maisonobe
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

package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** One polynomial segment of a thrust profile.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class PolynomialThrustSegment {

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Thrust along X direction (N). */
    private final PolynomialFunction xThrust;

    /** Thrust along Y direction (N). */
    private final PolynomialFunction yThrust;

    /** Thrust along Z direction (N). */
    private final PolynomialFunction zThrust;

    /** Simple constructor.
     * @param referenceDate reference date of the polynomials
     * @param xThrust thrust along X direction (N)
     * @param yThrust thrust along Y direction (N)
     * @param zThrust thrust along Z direction (N)
     */
    public PolynomialThrustSegment(final AbsoluteDate referenceDate,
                                   final PolynomialFunction xThrust,
                                   final PolynomialFunction yThrust,
                                   final PolynomialFunction zThrust) {
        this.referenceDate = referenceDate;
        this.xThrust       = xThrust;
        this.yThrust       = yThrust;
        this.zThrust       = zThrust;
    }

    /** Get thrust vector at a specified date.
     * @param date date to consider
     * @return thrust at {@code date} (N)
     */
    public Vector3D getThrustVector(final AbsoluteDate date) {
        final double dt = date.durationFrom(referenceDate);
        return new Vector3D(xThrust.value(dt), yThrust.value(dt), zThrust.value(dt));
    }

    /** Get thrust vector at a specified date.
     * @param <T> type of the field elements
     * @param date date to consider
     * @return thrust at {@code date} (N)
     */
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final FieldAbsoluteDate<T> date) {
        final T dt = date.durationFrom(referenceDate);
        return new FieldVector3D<>(xThrust.value(dt), yThrust.value(dt), zThrust.value(dt));
    }

}
