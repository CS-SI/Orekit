/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.bodies;

import org.hipparchus.util.FastMath;

/** Container for sexagesimal angle.
 * <p>Instance of this class are guaranteed to be immutable.</p>
 * @see GeodeticPoint
 * @author Luc Maisonobe
 * @since 13.0
 */
public class SexagesimalAngle {

    /** Sexagesimal base. */
    private static final double SIXTY = 60.0;

    /** Sign. */
    private final int sign;

    /** Degree part of the angle. */
    private final int degree;

    /** Arc-minute part of the angle. */
    private final int arcMinute;

    /** Arc-second part of the angle. */
    private final double arcSecond;

    /** Simple constructor.
     * @param sign sign
     * @param degree degree part of the angle
     * @param arcMinute arc-minute part of the angle
     * @param arcSecond arc-second part of the angle
     */
    public SexagesimalAngle(final int sign, final int degree, final int arcMinute, final double arcSecond) {
        this.sign      = sign;
        this.degree    = degree;
        this.arcMinute = arcMinute;
        this.arcSecond = arcSecond;
    }

    /** Simple constructor.
     * @param angle angle in radians
     */
    public SexagesimalAngle(final double angle) {
        this.sign      = angle < 0 ? -1 : 1;
        final double d = FastMath.toDegrees(FastMath.abs(angle));
        this.degree    = (int) FastMath.floor(d);
        final double m = (d - degree) * SIXTY;
        this.arcMinute = (int) FastMath.floor(m);
        this.arcSecond = (m - arcMinute) * SIXTY;
    }

    /** Get sign
     * @return sign
     */
    public int getSign() {
        return sign;
    }

    /** Get degree part of the angle
     * @return degree part of the angle
     */
    public int getDegree() {
        return degree;
    }

    /** Get arc-minute part of the angle
     * @return arc-minute part of the angle
     */
    public int getArcMinute() {
        return arcMinute;
    }

    /** Get arc-second part of the angle
     * @return arc-second part of the angle
     */
    public double getArcSecond() {
        return arcSecond;
    }

    /** Get the corresponding angle in radians.
     * @return angle in radians
     */
    public double getAngle() {
        return FastMath.toRadians(sign * (degree + (arcMinute + arcSecond / SIXTY) / SIXTY));
    }

}
