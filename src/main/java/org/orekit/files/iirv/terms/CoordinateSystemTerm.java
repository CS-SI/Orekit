/* Copyright 2024 The Johns Hopkins University Applied Physics Laboratory
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
package org.orekit.files.iirv.terms;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.iirv.terms.base.LongValuedIIRVTerm;
import org.orekit.frames.Frame;
import org.orekit.utils.IERSConventions;

/**
 * 1-character representing the coordinate system associated with the state variables.
 * <p>
 * Valid values:
 * <ul>
 * <li> 1 = Geocentric True-of-Date Rotating
 * <li> 2 = Geocentric mean of 1950.0 (B1950.0)
 * <li> 3 = Heliocentric B1950.0
 * <li> 4 = Reserved for JPL use (non-GSFC)
 * <li> 5 = Reserved for JPL use (non-GSFC)
 * <li> 6 = Geocentric mean of 2000.0 (J2000.0)
 * <li> 7 = Heliocentric J2000.0
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class CoordinateSystemTerm extends LongValuedIIRVTerm {
    /**
     * Geocentric True-of-Date Rotating (GTOD) CoordinateSystemTerm.
     * <p>
     * Also known as True of Date Rotating frame (TDR) or Greenwich Rotating Coordinate frame (GCR).
     */
    public static final CoordinateSystemTerm GEOCENTRIC_TRUE_OF_DATE_ROTATING = new CoordinateSystemTerm("1");

    /** Geocentric mean of 1950.0 (B1950.0) CoordinateSystemTerm. */
    public static final CoordinateSystemTerm GEOCENTRIC_MEAN_B1950 = new CoordinateSystemTerm("2");

    /** Heliocentric B1950.0 CoordinateSystemTerm. */
    public static final CoordinateSystemTerm HELIOCENTRIC_B1950 = new CoordinateSystemTerm("3");

    /** Reserved for JPL use (non-GSFC) CoordinateSystemTerm. */
    public static final CoordinateSystemTerm JPL_RESERVED_1 = new CoordinateSystemTerm("4");

    /** Reserved for JPL use (non-GSFC) CoordinateSystemTerm. */
    public static final CoordinateSystemTerm JPL_RESERVED_2 = new CoordinateSystemTerm("5");

    /** Geocentric mean of 2000.0 (J2000.0) CoordinateSystemTerm. */
    public static final CoordinateSystemTerm GEOCENTRIC_MEAN_OF_J2000 = new CoordinateSystemTerm("6");

    /** Heliocentric J2000.0 CoordinateSystemTerm. */
    public static final CoordinateSystemTerm HELIOCENTRIC_J2000 = new CoordinateSystemTerm("7");

    /** The length of the IIRV term within the message. */
    public static final int COORDINATE_SYSTEM_TERM_LENGTH = 1;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String COORDINATE_SYSTEM_TERM_PATTERN = "[1-7]";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param value value of the coordinate system term
     */
    public CoordinateSystemTerm(final String value) {
        super(COORDINATE_SYSTEM_TERM_PATTERN, value, COORDINATE_SYSTEM_TERM_LENGTH, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the coordinate system term
     */
    public CoordinateSystemTerm(final long value) {
        super(COORDINATE_SYSTEM_TERM_PATTERN, value, COORDINATE_SYSTEM_TERM_LENGTH, false);
    }

    /**
     * Returns the {@link Frame} specified within the IIRV.
     *
     * @param context data context used to retrieve frames
     * @return coordinate system
     */
    public Frame getFrame(final DataContext context) {
        final String encodedString = toEncodedString();
        switch (toEncodedString()) {
            case "1":
                return context.getFrames().getGTOD(IERSConventions.IERS_2010, true);
            case "2":
                throw new OrekitException(OrekitMessages.IIRV_UNMAPPED_COORDINATE_SYSTEM,
                    encodedString, "Geocentric mean of 1950.0 (B1950.0)");
            case "3":
                throw new OrekitException(OrekitMessages.IIRV_UNMAPPED_COORDINATE_SYSTEM,
                    encodedString, "B1950.0");
            case "4":
            case "5":
                throw new OrekitException(OrekitMessages.IIRV_UNMAPPED_COORDINATE_SYSTEM,
                    encodedString, "Reserved for JPL");
            case "6":
                return context.getFrames().getEME2000();
            case "7":
                return context.getCelestialBodies().getSun().getInertiallyOrientedFrame();
            default:
                // this should never happen
                throw new OrekitInternalError(null);
        }
    }

    /**
     * Returns the {@link Frame} specified within the IIRV using the {@link DataContext#getDefault() default data context}.
     *
     * @return coordinate system
     */
    @DefaultDataContext
    public Frame getFrame() {
        return getFrame(DataContext.getDefault());
    }

}
