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
package org.orekit.files.ccsds;

import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.HelmertTransformation;
import org.orekit.frames.LOFType;
import org.orekit.utils.IERSConventions;

/** Frames used in CCSDS Orbit Data Messages.
 * @author Steven Ports
 * @since 6.1
 */
public enum CCSDSFrame {

    /** Earth Mean Equator and Equinox of J2000. */
    EME2000(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            return FramesFactory.getEME2000();
        }

    },

    /** Geocentric Celestial Reference Frame. */
    GCRF(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            return FramesFactory.getGCRF();
        }

    },

    /** Greenwich Rotating Coordinates. */
    GRC(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return FramesFactory.getITRFEquinox(conventions, simpleEOP);
        }

    },

    /** International Celestial Reference Frame. */
    ICRF(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            return FramesFactory.getICRF();
        }

    },

    /** International Celestial Reference Frame 2000. */
    ITRF2000(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            final Frame itrf2008 = FramesFactory.getITRF(conventions, simpleEOP);
            final HelmertTransformation.Predefined predefinedHT =
                    HelmertTransformation.Predefined.ITRF_2008_TO_ITRF_2000;
            return predefinedHT.createTransformedITRF(itrf2008, toString());
        }

    },

    /** International Celestial Reference Frame 1993. */
    ITRF93(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            final Frame itrf2008 = FramesFactory.getITRF(conventions, simpleEOP);
            final HelmertTransformation.Predefined predefinedHT =
                    HelmertTransformation.Predefined.ITRF_2008_TO_ITRF_93;
            return predefinedHT.createTransformedITRF(itrf2008, toString());
        }

    },

    /** International Celestial Reference Frame 1997. */
    ITRF97(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            final Frame itrf2008 = FramesFactory.getITRF(conventions, simpleEOP);
            final HelmertTransformation.Predefined predefinedHT =
                    HelmertTransformation.Predefined.ITRF_2008_TO_ITRF_97;
            return predefinedHT.createTransformedITRF(itrf2008, toString());
        }

    },

    /** Mars Centered Inertial. */
    MCI(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            return CelestialBodyFactory.getMars().getInertiallyOrientedFrame();
        }

    },

    /** True of Date, Rotating. */
    TDR(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return FramesFactory.getGTOD(conventions, simpleEOP);
        }

    },

    /**
     * True Equator Mean Equinox.
     * TEME may be used only for OMMs based on NORAD
     * Two Line Element sets, and in no other circumstances.
     */
    TEME(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            return FramesFactory.getTEME();
        }

    },

    /** True of Date. */
    TOD(null) {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
            throws OrekitException {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return FramesFactory.getTOD(conventions, simpleEOP);
        }

    },

    /** Radial, Transverse (along-track) and Normal. */
    RTN(LOFType.QSW),

    /** Another name for Radial, Transverse (along-track) and Normal. */
    RSW(LOFType.QSW),

    /** TNW : x-axis along the velocity vector, W along the orbital angular momentum vector and
    N completes the right handed system. */
    TNW(LOFType.TNW);

    /** Type of Local Orbital Frame (may be null). */
    private final LOFType lofType;

    /** Simple constructor.
     * @param lofType type of Local Orbital Frame (null if frame is not a Local Orbital Frame)
     */
    CCSDSFrame(final LOFType lofType) {
        this.lofType = lofType;
    }

    /** Check if the frame is a Local Orbital frame.
     * @return true if the frame is a Local Orbital Frame
     */
    public boolean isLof() {
        return lofType != null;
    }

    /** Get the type of Local Orbital frame.
     * <p>
     * If the frame is not a Local Orbital frame (i.e. if this method returns null),
     * then the {@link #getFrame(IERSConventions, boolean) getFrame} method must be used to
     * retrieve the absolute frame.
     * </p>
     * @return type of Local Orbital Frame, or null if the frame is not a local orbital frame
     * @see #isLof()
     */
    public LOFType getLofType() {
        return lofType;
    }

    /**
     * Get the frame corresponding to the CCSDS constant.
     * @param conventions IERS conventions to use
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return frame corresponding to the CCSDS constant
     * @exception OrekitException if the frame cannot be retrieved or if it
     * is a Local Orbital Frame
     * @see #isLof()
     */
    public Frame getFrame(final IERSConventions conventions, final boolean simpleEOP)
        throws OrekitException {
        throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, toString());
    }

}
