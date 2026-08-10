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

import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/**
 * Orekit's default implementation of {@link CcsdsFrameMapper}.
 *
 * @author Evan M. Ward
 * @since 13.1.5
 */
public class OrekitCcsdsFrameMapper implements CcsdsFrameMapper {

    /** Message indicating no reference frame. */
    private static final String NO_REFERENCE_FRAME = "No reference frame";

    /** Simple constructor. */
    public OrekitCcsdsFrameMapper() {
        // nothing to do
    }

    @Override
    public Frame buildCcsdsFrame(final FrameFacade orientation,
                                 final AbsoluteDate frameEpoch) {
        if (orientation == null) {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, NO_REFERENCE_FRAME);
        }
        return orientation.asFrame().
            orElseThrow(() -> new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, orientation.getName()));

    }

    @Override
    public Frame buildCcsdsFrame(final BodyFacade center,
                                 final FrameFacade orientation,
                                 final AbsoluteDate frameEpoch) {
        if (center == null) {
            throw new OrekitException(OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY, "No Orbit center name");
        }
        final CelestialBody body =
            center.
                getBody().
                orElseThrow(() -> new OrekitException(OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY,
                                                      center.getName()));
        if (orientation == null) {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, NO_REFERENCE_FRAME);
        }
        final Frame frame =
            orientation.asFrame().
                orElseThrow(() -> new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, orientation.getName()));
        // Just return frame if we don't need to shift the center based on CENTER_NAME
        // MCI and ICRF are the only non-Earth centered frames specified in Annex A.
        final CelestialBodyFrame celestialBodyFrame = orientation.asCelestialBodyFrame().orElse(null);
        final boolean isMci = celestialBodyFrame == CelestialBodyFrame.MCI;
        final boolean isIcrf = celestialBodyFrame == CelestialBodyFrame.ICRF;
        final String centerName = center.getBody().get().getName();
        final boolean isCenterEarth = CelestialBodyFactory.EARTH.equals(centerName);
        final boolean isCenterMars = CelestialBodyFactory.MARS.equals(centerName);
        if (isIcrf) {
            // special case so Earth-centered ICRF is GCRF, #1914
            return body.getIcrfAlignedFrame();
        }
        if (!isMci && isCenterEarth || isMci && isCenterMars) {
            // ICRF and MCI are the only two frames in CelestialBodyFrame
            // that are not Earth-centered. If that changes then this code would
            // also need to be updated, perhaps by adding a getCenter() method
            // to CelestialBodyFrame or Frame.
            return frame;
        }
        // else, translate frame to specified center.
        return new ModifiedFrame(frame, celestialBodyFrame,
                body, center.getName());
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
