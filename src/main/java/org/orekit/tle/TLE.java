/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.tle;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.TimeStamped;
import org.orekit.time.UTCScale;

/** This class converts and contains TLE data.
 *
 * An instance of TLE is created with the two lines string representation,
 * conversion of the data is made internally for easier retrieval and
 * future extrapolation.
 * All the values provided by a TLE only have sense when translated by the correspondent
 * {@link TLEPropagator propagator}. Even when no extrapolation in time is needed,
 * state information (position and velocity coordinates) can only be computed threw
 * the propagator. Untreated values like inclination, RAAN, mean Motion, etc. can't
 * be used by themselves without loosing precision.
 * <p>
 * More information on the TLE format can be found on the
 * <a href="http://www.celestrak.com/">CelesTrak website.</a>
 * </p>
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class TLE implements TimeStamped, Serializable {

    /** Identifier for default type of ephemeris (SGP4/SDP4). */
    public static final int DEFAULT = 0;

    /** Identifier for SGP type of ephemeris. */
    public static final int SGP = 1;

    /** Identifier for SGP4 type of ephemeris. */
    public static final int SGP4 = 2;

    /** Identifier for SDP4 type of ephemeris. */
    public static final int SDP4 = 3;

    /** Identifier for SGP8 type of ephemeris. */
    public static final int SGP8 = 4;

    /** Identifier for SDP8 type of ephemeris. */
    public static final int SDP8 = 5;

    /** Pattern for line 1. */
    private static final Pattern LINE_1_PATTERN =
        Pattern.compile("1 [ 0-9]{5}U [ 0-9]{5}[ A-Z]{3} [ 0-9]{5}[.][ 0-9]{8} [ +-][.][ 0-9]{8} " +
                        "[ +-][ 0-9]{5}[+-][ 0-9] [ +-][ 0-9]{5}[+-][ 0-9] [ 0-9] [ 0-9]{4}[ 0-9]");

    /** Pattern for line 2. */
    private static final Pattern LINE_2_PATTERN =
        Pattern.compile("2 [ 0-9]{5} [ 0-9]{3}[.][ 0-9]{4} [ 0-9]{3}[.][ 0-9]{4} [ 0-9]{7} " +
                        "[ 0-9]{3}[.][ 0-9]{4} [ 0-9]{3}[.][ 0-9]{4} [ 0-9]{2}[.][ 0-9]{13}[ 0-9]");

    /** Checksum error message. */
    private static final String CHECKSUM_MESSAGE =
        "wrong cheksum of TLE line {0}, expected {1} but got {2} ({3})";

    /** Serializable UID. */
    private static final long serialVersionUID = 493162737271645055L;

    /** The satellite id. */
    private final int satelliteNumber;

    /** International designator. */
    private final String internationalDesignator;

    /** the TLE current date. */
    private final AbsoluteDate epoch;

    /** Ballistic coefficient. */
    private final double bStar;

    /** Type of ephemeris. */
    private final int ephemerisType;

    /** Line element number. */
    private final int elementNumber;

    /** Inclination (rad). */
    private final double i;

    /** Right Ascension of the Ascending node (rad). */
    private final double raan;

    /** Eccentricity. */
    private final double e;

    /** Argument of perigee (rad). */
    private final double pa;

    /** Mean anomaly (rad). */
    private final double meanAnomaly;

    /** Mean Motion (rad/sec). */
    private final double meanMotion;

    /** revolution number at epoch. */
    private final int revolutionNumberAtEpoch;

    /** Simple constructor with one TLE.
     * <p> The static method {@link #isFormatOK(String, String)} should be called
     * before trying to build this object. <p>
     * @param line1 the first element (69 char String)
     * @param line2 the second element (69 char String)
     * @exception OrekitException if some format error occurs
     */
    public TLE(final String line1, final String line2) throws OrekitException {

        satelliteNumber = Integer.parseInt(line1.substring(2, 7).replace(' ', '0'));
        internationalDesignator = line1.substring(9, 17);

        // Date format transform :
        int year = 2000 + Integer.parseInt(line1.substring(18, 20).replace(' ', '0'));
        if (year > 2056) {
            year -= 100;
        }
        final ChunkedDate date = new ChunkedDate(year, 1, 1);
        final double dayNb = Double.parseDouble(line1.substring(20, 32).replace(' ', '0'));
        epoch = new AbsoluteDate(new AbsoluteDate(date, ChunkedTime.H00, UTCScale.getInstance()),
                                 (dayNb - 1) * 86400); //-1 is due to TLE date definition
        // Fields transform :
        bStar = Double.parseDouble(line1.substring(53, 54).replace(' ', '0') +
                                   "." + line1.substring(54, 59).replace(' ', '0') +
                                   "e" + line1.substring(59, 61).replace(' ', '0'));
        ephemerisType = Integer.parseInt(line1.substring(62, 63).replace(' ', '0'));
        elementNumber = Integer.parseInt(line1.substring(64, 68).replace(' ', '0'));
        i = Math.toRadians(Double.parseDouble(line2.substring(8, 16).replace(' ', '0')));
        raan = Math.toRadians(Double.parseDouble(line2.substring(17, 25).replace(' ', '0')));
        e = Double.parseDouble("." + line2.substring(26, 33).replace(' ', '0'));
        pa = Math.toRadians(Double.parseDouble(line2.substring(34, 42).replace(' ', '0')));
        meanAnomaly = Math.toRadians(Double.parseDouble(line2.substring(43, 51).replace(' ', '0')));
        meanMotion = Math.PI * Double.parseDouble(line2.substring(52, 63).replace(' ', '0')) / 43200.0;
        revolutionNumberAtEpoch = Integer.parseInt(line2.substring(63, 68).replace(' ', '0'));

    }

    /** Gets the ballistic coefficient.
     * @return bStar
     */
    public double getBStar() {
        return bStar;
    }

    /** Gets the eccentricity.
     * @return the eccentricity
     */
    public double getE() {
        return e;
    }

    /** Gets the type of ephemeris.
     * @return the ephemeris type (one of {@link #DEFAULT}, {@link #SGP},
     * {@link #SGP4}, {@link #SGP8}, {@link #SDP4}, {@link #SDP8})
     */
    public int getEphemerisType() {
        return ephemerisType;
    }

    /** Gets the TLE current date.
     * @return the epoch
     */
    public AbsoluteDate getDate() {
        return epoch;
    }

    /** Gets the inclination.
     * @return the inclination (rad)
     */
    public double getI() {
        return i;
    }

    /** Gets the international designator.
     * @return the internationalDesignator
     */
    public String getInternationalDesignator() {
        return internationalDesignator;
    }

    /** Gets the  element number.
     * @return the element number
     */
    public int getElementNumber() {
        return elementNumber;
    }

    /** Gets the mean anomaly.
     * @return the mean anomaly  (rad)
     */
    public double getMeanAnomaly() {
        return meanAnomaly;
    }

    /** Gets the mean motion.
     * @return the meanMotion (rad/s)
     */
    public double getMeanMotion() {
        return meanMotion;
    }

    /** Gets the argument of perigee.
     * @return omega (rad)
     */
    public double getPerigeeArgument() {
        return pa;
    }

    /** Gets Right Ascension of the Ascending node.
     * @return the raan (rad)
     */
    public double getRaan() {
        return raan;
    }

    /** Gets the revolution number.
     * @return the revolutionNumberAtEpoch
     */
    public int getRevolutionNumberAtEpoch() {
        return revolutionNumberAtEpoch;
    }

    /** Gets the satellite id.
     * @return the satellite number
     */
    public int getSatelliteNumber() {
        return satelliteNumber;
    }

    /** Check the entries to determine if the element format is correct.
     * @param line1 the first element (69 char String)
     * @param line2 the second element (69 char String)
     * @return true if format is recognised, false if not
     * @exception OrekitException if checksum is not valid
     */
    public static boolean isFormatOK(final String line1, final String line2)
        throws OrekitException {

        if (line1 == null || line1.length() != 69 ||
            line2 == null || line2.length() != 69) {
            return false;
        }

        if (!(LINE_1_PATTERN.matcher(line1).matches() &&
               LINE_2_PATTERN.matcher(line2).matches())) {
            return false;
        }

        // check sums
        int chksum1  = 0;
        int chksum2  = 0;

        for (int j = 0; j < 68; j++) {
            final String x = line1.substring(j, j + 1);
            final String y = line2.substring(j, j + 1);
            try {
                chksum1 += Integer.parseInt(x);
            } catch (NumberFormatException nb) {
                if (x.equals("-")) {
                    chksum1++;
                }
            }
            try {
                chksum2 += Integer.parseInt(y);
            } catch (NumberFormatException nb) {
                if (y.equals("-")) {
                    chksum2++;
                }
            }
        }

        if (Integer.parseInt(line1.substring(68)) != (chksum1 % 10)) {
            throw new OrekitException(CHECKSUM_MESSAGE,
                                      new Object[] {
                                          Integer.valueOf(1), line1.substring(68) ,
                                          Integer.valueOf(chksum1 % 10), line1
                                      });
        }

        if (Integer.parseInt(line2.substring(68)) != (chksum2 % 10)) {
            throw new OrekitException(CHECKSUM_MESSAGE,
                                      new Object[] {
                                          Integer.valueOf(2), line2.substring(68) ,
                                          Integer.valueOf(chksum2 % 10), line2
                                      });
        }

        return true;

    }

}
