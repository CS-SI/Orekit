/* Contributed in the public domain.
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
package org.orekit.files.ccsds.definitions;

import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/**
 * Orekit's default implementation of {@link CcsdsFrameMapper}.
 *
 * @author Evan M. Ward
 * @since 14.0
 */
public class OrekitCcsdsFrameMapper implements CcsdsFrameMapper {

    @Override
    public Frame buildCcsdsFrame(final FrameFacade orientation,
                                 final AbsoluteDate frameEpoch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Frame buildCcsdsFrame(final BodyFacade center,
                                 final FrameFacade orientation,
                                 final AbsoluteDate frameEpoch) {
        if (center == null) {
            throw new OrekitException(OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY, "No Orbit center name");
        }
        if (center.getBody() == null) {
            throw new OrekitException(OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY, center.getName());
        }
        if (orientation == null) {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, "No reference frame");
        }
        if (orientation.asFrame() == null) {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, orientation.getName());
        }
        // Just return frame if we don't need to shift the center based on CENTER_NAME
        // MCI and ICRF are the only non-Earth centered frames specified in Annex A.
        final boolean isMci = orientation.asCelestialBodyFrame() == CelestialBodyFrame.MCI;
        final boolean isIcrf = orientation.asCelestialBodyFrame() == CelestialBodyFrame.ICRF;
        final boolean isSolarSystemBarycenter =
                CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER.equals(center.getBody().getName());
        if (!(isMci || isIcrf) && CelestialBodyFactory.EARTH.equals(center.getBody().getName()) ||
                isMci && CelestialBodyFactory.MARS.equals(center.getBody().getName()) ||
                isIcrf && isSolarSystemBarycenter) {
            return orientation.asFrame();
        }
        // else, translate frame to specified center.
        return new ModifiedFrame(orientation.asFrame(), orientation.asCelestialBodyFrame(),
                center.getBody(), center.getName());
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || this.getClass() == OrekitCcsdsFrameMapper.class &&
                obj.getClass() == OrekitCcsdsFrameMapper.class;
    }

}
