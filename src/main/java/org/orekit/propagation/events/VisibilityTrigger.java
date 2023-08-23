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
package org.orekit.propagation.events;

import org.hipparchus.util.FastMath;

/** Enumerate for triggering visibility of spherical bodies.
 * @see FieldOfViewDetector
 * @author Luc Maisonobe
 * @since 10.0
 */
public enum VisibilityTrigger {

    /** Trigger for bodies that are considered visible only when fully inside Field Of View. */
    VISIBLE_ONLY_WHEN_FULLY_IN_FOV(+1.0),

    /** Trigger for bodies that are considered visible as soon as a part touches Field Of View. */
    VISIBLE_AS_SOON_AS_PARTIALLY_IN_FOV(-1.0);

    /** Sign of the radius correction. */
    private final double sign;

    /** Simple constructor.
     * @param sign sign of the radius correction
     */
    VisibilityTrigger(final double sign) {
        this.sign = sign;
    }

    /** Apply radius correction.
     * @param angularRadius target body angular radius
     * @return corrected radius
     * @since 10.1
     */
    public double radiusCorrection(final double angularRadius) {
        return FastMath.copySign(angularRadius, sign);
    }

}
