/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements.generation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;


/** Scheduling predicate taking care of satellite being lit by Sun.
 * <p>
 * This predicate is mainly useful for passive optical measurements
 * (telescope observation of satellites against the stars background).
 * </p>
 * @author Luc Maisonobe
 * @since 9.3
 */
public class Sunlit implements SchedulingPredicate {

    /** Provider for Sun position. */
    private final PVCoordinatesProvider sun;

    /** Sun body radius (m). */
    private final double sunRadius;

    /** Earth body radius (m). */
    private final double earthRadius;

    /** Flag for considering penumbra as sunlit or not. */
    private final boolean sunlitInPenumbra;

    /** Index of the propagator related to this predicate. */
    private final int propagatorIndex;

    /** Simple constructor.
     * <p>
     * The values for Earth and Sun radius are set to the IAU 2015 resolution B3
     * nominal values ({@link Constants#IAU_2015_NOMINAL_SOLAR_RADIUS} and
     * {@link Constants#IAU_2015_NOMINAL_EARTH_EQUATORIAL_RADIUS})
     * </p>
     * @param sun provider for Sun position
     * @param sunlitInPenumbra if true, satellite is considered to be sunlit when in penumbra
     * @param propagatorIndex index of the propagator related to this predicate
     */
    public Sunlit(final PVCoordinatesProvider sun, final boolean sunlitInPenumbra,
                  final int propagatorIndex) {
        this(sun,
             Constants.IAU_2015_NOMINAL_SOLAR_RADIUS,
             Constants.IAU_2015_NOMINAL_EARTH_EQUATORIAL_RADIUS,
             sunlitInPenumbra, propagatorIndex);
    }

    /** Simple constructor.
     * @param sun provider for Sun position
     * @param sunRadius the radius of the occulted Sun body (m)
     * @param earthRadius the radius of the occulting Earth body (m)
     * @param sunlitInPenumbra if true, satellite is considered to be sunlit when in penumbra
     * @param propagatorIndex index of the propagator related to this predicate
     */
    public Sunlit(final PVCoordinatesProvider sun,
                  final double sunRadius, final double earthRadius,
                  final boolean sunlitInPenumbra, final int propagatorIndex) {
        this.sun              = sun;
        this.sunRadius        = sunRadius;
        this.earthRadius      = earthRadius;
        this.sunlitInPenumbra = sunlitInPenumbra;
        this.propagatorIndex  = propagatorIndex;
    }

    /** {@inheritDoc} */
    @Override
    public boolean feasibleMeasurement(final SpacecraftState... states) {

        final AbsoluteDate  date  = states[propagatorIndex].getDate();
        final Frame         frame = states[propagatorIndex].getFrame();
        final Vector3D      pSun  = sun.getPVCoordinates(date, frame).getPosition();

        final Vector3D pSat = states[propagatorIndex].getPVCoordinates().getPosition();
        final Vector3D ps   = pSun.subtract(pSat);
        final Vector3D pe   = pSat.negate();
        final double angle  = Vector3D.angle(ps, pe);
        final double rs     = FastMath.asin(sunRadius / ps.getNorm());
        final double re     = FastMath.asin(earthRadius / pe.getNorm());
        return angle - re > (sunlitInPenumbra ? -rs : rs);

    }

}
