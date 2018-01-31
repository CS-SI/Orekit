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
import org.hipparchus.util.SinCos;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * Attitude providers for Galileo navigation satellites.
 * <p>
 * This class is based on the May 2017 version of J. Kouba eclips.f
 * subroutine available at <a href="http://acc.igs.org/orbits">IGS Analysis
 * Center Coordinator site</a>. The eclips.f code itself is not used ; its
 * hard-coded data are used and its low level models are used, but the
 * structure of the code and the API have been completely rewritten.
 * </p>
 * @author J. Kouba original fortran routine
 * @author Luc Maisonobe Java translation
 * @since 9.2
 */
public class Galileo extends AbstractGNSSAttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20171114L;

    /** Constants for Galileo turns. */
    private static final double BETA_X = FastMath.toRadians(15.0);

    /** Constants for Galileo turns. */
    private static final double BETA_Y = FastMath.toRadians(2.0);

    /** Limit for the noon turn. */
    private static final double COS_NOON = FastMath.cos(BETA_X);

    /** Limit for the night turn. */
    private static final double COS_NIGHT = -COS_NOON;

    /** No margin on turn end for Galileo. */
    private final double END_MARGIN = 0.0;

    /** Simple constructor.
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     */
    public Galileo(final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                   final PVCoordinatesProvider sun) {
        super(validityStart, validityEnd, sun);
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedAngularCoordinates correctYaw(final GNSSAttitudeContext context) {

        if (FastMath.abs(context.getBeta()) < BETA_Y &&
            context.setUpTurnRegion(COS_NIGHT, COS_NOON)) {

            context.setHalfSpan(context.inSunSide() ?
                                BETA_X :
                                context.inOrbitPlaneAngle(BETA_X));
            if (context.inTurnTimeRange(context.getDate(), END_MARGIN)) {

                // handling both noon and midnight turns at once
                final double beta   = context.getBeta();
                final SinCos scBeta = FastMath.sinCos(beta);
                final double sinY   = FastMath.copySign(FastMath.sin(BETA_Y), context.getSecuredBeta());
                final double sd     = FastMath.copySign(FastMath.sin(context.getDelta()), context.getSVBcos());
                final double c      = sd * scBeta.cos();
                final double shy    = 0.5 * ((-sinY - scBeta.sin()) +
                                             (-sinY + scBeta.sin()) *
                                             FastMath.cos(FastMath.PI * FastMath.abs(c) / FastMath.sin(BETA_X)));
                final double phi    = FastMath.atan2(shy, c);

                // TODO
                return null;
            }

        }

        // in nominal yaw mode
        return context.getNominalYaw();

    }

}
