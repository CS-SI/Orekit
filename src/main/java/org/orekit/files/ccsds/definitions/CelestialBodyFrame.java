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
package org.orekit.files.ccsds.definitions;

import java.util.regex.Pattern;

import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.VersionedITRF;
import org.orekit.utils.IERSConventions;

/** Frames used in CCSDS Orbit Data Messages.
 * @author Steven Ports
 * @since 6.1
 */
public enum CelestialBodyFrame {

    /** Earth Mean Equator and Equinox of J2000. */
    EME2000 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            return dataContext.getFrames().getEME2000();
        }

    },

    /** Earth Mean Equator and Equinox of J2000. */
    J2000 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            return dataContext.getFrames().getEME2000();
        }

    },

    /** Geocentric Celestial Reference Frame. */
    GCRF {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            return dataContext.getFrames().getGCRF();
        }

    },

    /** Greenwich Rotating Coordinates. */
    GRC {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRFEquinox(conventions, simpleEOP);
        }

    },

    /** Greenwich True Of Date.
     * @since 11.0
     */
    GTOD {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getGTOD(conventions, simpleEOP);
        }

    },

    /** International Celestial Reference Frame. */
    ICRF {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            return dataContext.getFrames().getICRF();
        }

    },

    /** Latest International Terrestrial Reference Frame. */
    ITRF {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            return ITRF2020.getFrame(conventions, simpleEOP, dataContext);
        }

    },

    /** International Terrestrial Reference Frame 2020. */
    ITRF2020 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_2020, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 2014. */
    ITRF2014 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_2014, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 2008. */
    ITRF2008 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_2008, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 2005. */
    ITRF2005 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_2005, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 2000. */
    ITRF2000 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_2000, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 1997. */
    ITRF1997 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_1997, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 1996. */
    ITRF1996 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_1996, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 1994. */
    ITRF1994 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_1994, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 1993. */
    ITRF1993 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_1993, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 1992. */
    ITRF1992 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_1992, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 1991. */
    ITRF1991 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_1991, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 1990. */
    ITRF1990 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_1990, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 1989. */
    ITRF1989 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_1989, conventions, simpleEOP);
        }

    },

    /** International Terrestrial Reference Frame 1988. */
    ITRF1988 {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getITRF(ITRFVersion.ITRF_1988, conventions, simpleEOP);
        }

    },

    /** Mars Centered Inertial. */
    MCI {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            return dataContext.getCelestialBodies().getMars().getInertiallyOrientedFrame();
        }

    },

    /** True of Date, Rotating. */
    TDR {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getGTOD(conventions, simpleEOP);
        }

    },

    /**
     * True Equator Mean Equinox.
     * TEME may be used only for OMMs based on NORAD
     * Two Line Element sets, and in no other circumstances.
     */
    TEME {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            return dataContext.getFrames().getTEME();
        }

    },

    /** True of Date. */
    TOD {

        /** {@inheritDoc} */
        @Override
        public Frame getFrame(final IERSConventions conventions,
                              final boolean simpleEOP,
                              final DataContext dataContext) {
            if (conventions == null) {
                throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
            }
            return dataContext.getFrames().getTOD(conventions, simpleEOP);
        }

    };

    /** Pattern for dash. */
    private static final Pattern DASH = Pattern.compile("-");

    /** Suffix of the name of the inertial frame attached to a planet. */
    private static final String INERTIAL_FRAME_SUFFIX = "/inertial";

    /** Substring common to all ITRF frames. */
    private static final String ITRF_SUBSTRING = "ITRF";

    /**
     * Get the frame corresponding to the CCSDS constant.
     * @param conventions IERS conventions to use
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext to use when creating the frame.
     * @return frame corresponding to the CCSDS constant
     * @since 10.1
     */
    public abstract Frame getFrame(IERSConventions conventions, boolean simpleEOP, DataContext dataContext);

    /**
     * Get the name of celestial body frame.
     * @return the name of celestial body frame
     * @since 11.1
     */
    public String getName() {
        return name();
    }

    /** Parse a CCSDS frame.
     * @param frameName name of the frame, as the value of a CCSDS key=value line
     * @return CCSDS frame corresponding to the name
     */
    public static CelestialBodyFrame parse(final String frameName) {
        String canonicalized = DASH.matcher(frameName).replaceAll("");
        if (canonicalized.startsWith(ITRF_SUBSTRING) && canonicalized.length() > 4) {
            final int year = Integer.parseInt(canonicalized.substring(4));
            if (year > 87 && year < 100) {
                // convert two-digits specifications like ITRF97 into ITRF1997
                canonicalized = ITRF_SUBSTRING + (year + 1900);
            }
        }
        return CelestialBodyFrame.valueOf(canonicalized);
    }

    /**
     * Map an Orekit frame to a CCSDS frame.
     *
     * <p> The goal of this method is to perform the opposite mapping of {@link
     * #getFrame(IERSConventions, boolean, DataContext)}.
     *
     * @param frame a reference frame.
     * @return the CCSDSFrame corresponding to the Orekit frame
     */
    public static CelestialBodyFrame map(final Frame frame) {
        // Try to determine the CCSDS name from Annex A by examining the Orekit name.
        final String name = frame.getName();
        try {
            // should handle J2000, GCRF, TEME, and some frames created by OEMParser.
            return CelestialBodyFrame.valueOf(name);
        } catch (IllegalArgumentException iae) {
            if (frame instanceof ModifiedFrame) {
                return ((ModifiedFrame) frame).getRefFrame();
            } else if ((CelestialBodyFactory.MARS + INERTIAL_FRAME_SUFFIX).equals(name)) {
                return MCI;
            } else if ((CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER + INERTIAL_FRAME_SUFFIX).equals(name)) {
                return ICRF;
            } else if (name.contains("GTOD")) {
                return GTOD;
            } else if (name.contains("TOD")) { // check after GTOD
                return TOD;
            } else if (name.contains("Equinox") && name.contains(ITRF_SUBSTRING)) {
                return GRC;
            } else if (frame instanceof VersionedITRF) {
                try {
                    final ITRFVersion itrfVersion = ((VersionedITRF) frame).getITRFVersion();
                    return CelestialBodyFrame.valueOf(itrfVersion.name().replace("_", ""));
                } catch (IllegalArgumentException iae2) {
                    // this should never happen
                    throw new OrekitInternalError(iae2);
                }
            } else if (name.contains("CIO") && name.contains(ITRF_SUBSTRING)) {
                return ITRF2014;
            }
            throw new OrekitException(iae, OrekitMessages.CCSDS_INVALID_FRAME, name);
        }
    }

    /**
     * Guesses names from ODM Table 5-3 and Annex A.
     *
     * <p> The goal of this method is to perform the opposite mapping of {@link
     * #getFrame(IERSConventions, boolean, DataContext)}.
     *
     * @param frame a reference frame.
     * @return the string to use in the OEM file to identify {@code frame}.
     */
    public static String guessFrame(final Frame frame) {
        try {
            return map(frame).name();
        } catch (OrekitException oe) {
            // we were unable to find a match
            return frame.getName();
        }
    }

}
