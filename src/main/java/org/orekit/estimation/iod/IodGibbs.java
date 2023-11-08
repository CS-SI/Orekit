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
package org.orekit.estimation.iod;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/**
 * Gibbs position-based Initial Orbit Determination (IOD) algorithm.
 * <p>
 * An orbit is determined from three position vectors. This method requires
 * the vectors to be coplanar. Orekit uses a {@link IodGibbs#COPLANAR_THRESHOLD
 * default coplanar threshold of 5°}.
 *
 * Reference:
 *  Vallado, D., Fundamentals of Astrodynamics and Applications
 * </p>
 * @author Joris Olympio
 * @since 8.0
 */
public class IodGibbs {

    /** Gravitational constant. **/
    private final double mu;

    /** Threshold for checking coplanar vectors. */
    private final double COPLANAR_THRESHOLD = FastMath.toRadians(5.);

    /** Creator.
     *
     * @param mu gravitational constant
     */
    public IodGibbs(final double mu) {
        this.mu = mu;
    }

    /** Give an initial orbit estimation, assuming Keplerian motion.
     * All observations should be from the same location.
     *
     * @param frame measurements frame
     * @param p1 First position measurement
     * @param p2 Second position measurement
     * @param p3 Third position measurement
     * @return an initial orbit estimation at the central date
     *         (i.e., date of the second position measurement)
     * @since 11.0
     */
    public Orbit estimate(final Frame frame, final Position p1, final Position p2, final Position p3) {
        return estimate(frame,
                        p1.getPosition(), p1.getDate(),
                        p2.getPosition(), p2.getDate(),
                        p3.getPosition(), p3.getDate());
    }

    /** Give an initial orbit estimation, assuming Keplerian motion.
     * All observations should be from the same location.
     *
     * @param frame measure frame
     * @param pv1 PV measure 1 taken in frame
     * @param pv2 PV measure 2 taken in frame
     * @param pv3 PV measure 3 taken in frame
     * @return an initial orbit estimation at the central date
     *         (i.e., date of the second PV measurement)
     */
    public Orbit estimate(final Frame frame, final PV pv1, final PV pv2, final PV pv3) {
        return estimate(frame,
                        pv1.getPosition(), pv1.getDate(),
                        pv2.getPosition(), pv2.getDate(),
                        pv3.getPosition(), pv3.getDate());
    }

    /** Give an initial orbit estimation, assuming Keplerian motion.
     * All observations should be from the same location.
     *
     * @param frame measure frame
     * @param r1 position 1 measured in frame
     * @param date1 date of measure 1
     * @param r2 position 2 measured in frame
     * @param date2 date of measure 2
     * @param r3 position 3 measured in frame
     * @param date3 date of measure 3
     * @return an initial orbit estimation at the central date
     *         (i.e., date of the second position measurement)
     */
    public Orbit estimate(final Frame frame,
                          final Vector3D r1, final AbsoluteDate date1,
                          final Vector3D r2, final AbsoluteDate date2,
                          final Vector3D r3, final AbsoluteDate date3) {
        // Checks measures are not at the same date
        if (date1.equals(date2) || date1.equals(date3) || date2.equals(date3)) {
            throw new OrekitException(OrekitMessages.NON_DIFFERENT_DATES_FOR_OBSERVATIONS, date1, date2, date3,
                    date2.durationFrom(date1), date3.durationFrom(date1), date3.durationFrom(date2));
        }

        // Checks measures are in the same plane
        final double num = r1.normalize().dotProduct(r2.normalize().crossProduct(r3.normalize()));
        final double alpha = FastMath.PI / 2.0 - FastMath.acos(num);
        if (FastMath.abs(alpha) > COPLANAR_THRESHOLD) {
            throw new OrekitException(OrekitMessages.NON_COPLANAR_POINTS);
        }

        final Vector3D D = r1.crossProduct(r2).add(r2.crossProduct(r3).add(r3.crossProduct(r1)));

        final Vector3D N = (r2.crossProduct(r3)).scalarMultiply(r1.getNorm())
                .add((r3.crossProduct(r1)).scalarMultiply(r2.getNorm()))
                .add((r1.crossProduct(r2)).scalarMultiply(r3.getNorm()));

        final Vector3D B = D.crossProduct(r2);

        final Vector3D S = r1.scalarMultiply(r2.getNorm() - r3.getNorm())
                .add(r2.scalarMultiply(r3.getNorm() - r1.getNorm())
                     .add(r3.scalarMultiply(r1.getNorm() - r2.getNorm())));

        // middle velocity
        final double   vm    = FastMath.sqrt(mu / (N.getNorm() * D.getNorm()));
        final Vector3D vlEci = B.scalarMultiply(vm / r2.getNorm()).add(S.scalarMultiply(vm));

        // compile a new middle point with position, velocity
        final PVCoordinates pv   = new PVCoordinates(r2, vlEci);

        // compute the equivalent Cartesian orbit
        final CartesianOrbit orbit = new CartesianOrbit(pv, frame, date2, mu);

        //define the reverse orbit
        final PVCoordinates pv2 = new PVCoordinates(r2, vlEci.scalarMultiply(-1));
        final CartesianOrbit orbit2 = new CartesianOrbit(pv2, frame, date2, mu);

        //check which orbit is correct
        final Vector3D estP3 = orbit.shiftedBy(date3.durationFrom(date2)).
                                    getPosition();
        final double dist = estP3.subtract(r3).getNorm();
        final Vector3D estP3_2 = orbit2.shiftedBy(date3.durationFrom(date2)).
                                       getPosition();
        final double dist2 = estP3_2.subtract(r3).getNorm();

        if (dist <= dist2) {
            return orbit;
        } else {
            return orbit2;
        }
    }
}
