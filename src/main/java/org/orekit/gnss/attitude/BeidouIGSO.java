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
package org.orekit.gnss.attitude;

import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/**
 * Attitude providers for Beidou inclined geosynchronous orbit navigation satellites.
 * <p>
 * WARNING: as of release 9.2, this feature is still considered experimental
 * </p>
 * @author Luc Maisonobe Java translation
 * @since 9.2
 */
public class BeidouIGSO extends AbstractGNSSAttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20171114L;

    /** Constant for Beidou turns. */
    private static final double BETA_0 = FastMath.toRadians(2.0);

    /** Simple constructor.
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     * @param inertialFrame inertial frame where velocity are computed
     */
    public BeidouIGSO(final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                      final ExtendedPVCoordinatesProvider sun, final Frame inertialFrame) {
        super(validityStart, validityEnd, sun, inertialFrame);
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedAngularCoordinates correctedYaw(final GNSSAttitudeContext context)
        throws OrekitException {

        if (FastMath.abs(context.getBeta()) < 2 * BETA_0) {
            // when Sun is close to orbital plane, attitude is in Orbit Normal (ON) yaw
            return context.orbitNormalYaw();
        }

        // in nominal yaw mode
        return context.getNominalYaw();

    }

    /** {@inheritDoc} */
    @Override
    protected <T extends RealFieldElement<T>> TimeStampedFieldAngularCoordinates<T> correctedYaw(final GNSSFieldAttitudeContext<T> context)
        throws OrekitException {

        if (FastMath.abs(context.getBeta()).getReal() < 2 * BETA_0) {
            // when Sun is close to orbital plane, attitude is in Orbit Normal (ON) yaw
            return context.orbitNormalYaw();
        }

        // in nominal yaw mode
        return context.getNominalYaw();

    }

}
