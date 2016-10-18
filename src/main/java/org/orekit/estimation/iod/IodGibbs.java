/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation.iod;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.estimation.measurements.PV;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/**
 * Gibbs initial orbit determination.
 * An orbit is determined from three position vectors.
 *
 * Reference:
 *  Vallado, D., Fundamentals of Astrodynamics and Applications
 *
 * @author Joris Olympio
 * @since 8.0
 *
 */
public class IodGibbs {

    /** Threshold for checking coplanr vectors. */
    private static final double COPLANAR_THRESHOLD = FastMath.toRadians(5.);

    /** gravitationnal constant. */
    private final double mu;

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
     * @param frame measure frame
     * @param pv1 PV measure 1 taken in frame
     * @param pv2 PV measure 2 taken in frame
     * @param pv3 PV measure 3 taken in frame
     * @return an initial orbit estimation
     */
    public KeplerianOrbit estimate(final Frame frame, final PV pv1, final PV pv2, final PV pv3) {

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
     * @return an initial orbit estimation
     */
    public KeplerianOrbit estimate(final Frame frame,
                                   final Vector3D r1, final AbsoluteDate date1,
                                   final Vector3D r2, final AbsoluteDate date2,
                                   final Vector3D r3, final AbsoluteDate date3) {
        // Checks measures are not at the same date
        if (date1.equals(date2) || date1.equals(date3) || date2.equals(date3)) {
            //throw new OrekitException("The measures are not different!");
        }

        // Checks measures are in the same plane
        final double num = r1.normalize().dotProduct(r2.normalize().crossProduct(r3.normalize()));
        final double alpha = FastMath.PI / 2.0 - FastMath.acos(num);
        if (FastMath.abs(alpha) > COPLANAR_THRESHOLD) {
            // throw something
            //throw new OrekitException("Non coplanar points!");
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
        final double vm = FastMath.sqrt(mu / (N.getNorm() * D.getNorm()));
        final Vector3D vlEci = B.scalarMultiply(vm / r2.getNorm()).add(S.scalarMultiply(vm));

        // compile a new middle point with position, velocity
        final PVCoordinates pv = new PVCoordinates(r2, vlEci);
        final AbsoluteDate date = date2;

        // compute the equivalent keplerian orbit
        return new KeplerianOrbit(pv, frame, date, mu);
    }
}
