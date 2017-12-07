/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.gnss.attitude;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Attitude providers for Glonass navigation satellites.
 * <p>
 * This class and the corresponding classes for other spacecrafts
 * in the same package are based on the May 2017 version of J. Kouba eclips.f
 * subroutine available at <a href="http://acc.igs.org/orbits">IGS Analysis
 * Center Coordinator site</a>. The eclips.f code itself is not used ; its
 * hard-coded data are used and its low level models are used, but the
 * structure of the code and the API have been completely rewritten.
 * </p>
 * @author J. Kouba original fortran routine
 * @author Luc Maisonobe Java translation
 * @since 9.2
 */
public class Glonass extends AbstractGNSSAttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20171114L;

    /** Satellite-Sun angle limit for a midnight turn maneuver. */
    private static final double NIGHT_TURN_LIMIT = FastMath.toRadians(180.0 - 14.20);

    /** Yaw rates for all spacecrafts. */
    private static final double YAW_RATE = FastMath.toRadians(0.250);

    /** Initial yaw end at iterative search start. */
    private static final double YAW_END_ZERO = FastMath.toRadians(75.0);

    /** Simple constructor.
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     */
    public Glonass(final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                   final PVCoordinatesProvider sun) {
        super(validityStart, validityEnd, sun);
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedAngularCoordinates correctYaw(final TimeStampedPVCoordinates pv, final double beta,
                                                       final double svbCos, final TimeStampedAngularCoordinates nominalYaw) {

        // noon beta angle limit from yaw rate
        final double muRate = pv.getVelocity().getNorm() / pv.getPosition().getNorm();
        double       aNoon  = FastMath.atan(muRate / YAW_RATE);
        if (FastMath.abs(beta) < aNoon) {
            double       yawEnd = YAW_END_ZERO;
            final double tan    = FastMath.tan(beta);
            for (int i = 0; i < 3; ++i) {
                final double sin = FastMath.sin(muRate * yawEnd / YAW_RATE);
                yawEnd = 0.5 * FastMath.abs(FastMath.atan2(-tan,  sin) - FastMath.atan2(-tan, -sin));
            }
            aNoon = muRate * yawEnd / YAW_RATE;
        }

        final double cNoon  = FastMath.cos(aNoon);
        final double cNight = FastMath.cos(NIGHT_TURN_LIMIT);

        if (svbCos < cNight) {
            // in eclipse turn mode
            // TODO
            return null;
        } else if (svbCos > cNoon) {
            // in noon turn mode
            // TODO
            return null;
        } else {
            // in nominal yaw mode
            return nominalYaw;
        }

    }

}
