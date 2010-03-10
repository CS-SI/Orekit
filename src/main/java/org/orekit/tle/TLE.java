/* Copyright 2002-2010 CS Communication & Systèmes
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;

/** This class is a container for a single set of TLE data.
 *
 * <p>TLE sets can be built either by providing directly the two lines, in
 * which case parsing is performed internally or by providing the already
 * parsed elements.</p>
 * <p>TLE are not transparently convertible to {@link org.orekit.orbits.Orbit Orbit}
 * instances. They are significant only with respect to their dedicated {@link
 * TLEPropagator propagator}, which also computes position and velocity coordinates.
 * Any attempt to directly use orbital parameters like {@link #getE() eccentricity},
 * {@link #getI() inclination}, etc. without any reference to the {@link TLEPropagator
 * TLE propagator} is prone to errors.</p>
 * <p>More information on the TLE format can be found on the
 * <a href="http://www.celestrak.com/">CelesTrak website.</a></p>
 * @author Fabien Maussion
 * @author Luc Maisonobe
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
        "wrong checksum of TLE line {0}, expected {1} but got {2} ({3})";

    /** International symbols for parsing. */
    private static final DecimalFormatSymbols SYMBOLS =
        new DecimalFormatSymbols(Locale.US);

    /** Serializable UID. */
    private static final long serialVersionUID = -1596648022319057689L;

    /** The satellite number. */
    private final int satelliteNumber;

    /** Classification (U for unclassified). */
    private final char classification;

    /** Launch year. */
    private final int launchYear;

    /** Launch number. */
    private final int launchNumber;

    /** Piece of launch (from "A" to "ZZZ"). */
    private final String launchPiece;

    /** Type of ephemeris. */
    private final int ephemerisType;

    /** Element number. */
    private final int elementNumber;

    /** the TLE current date. */
    private final AbsoluteDate epoch;

    /** Mean motion (rad/s). */
    private final double meanMotion;

    /** Mean motion first derivative (rad/s<sup>2</sup>). */
    private final double meanMotionFirstDerivative;

    /** Mean motion second derivative (rad/s<sup>3</sup>). */
    private final double meanMotionSecondDerivative;

    /** Eccentricity. */
    private final double eccentricity;

    /** Inclination (rad). */
    private final double inclination;

    /** Argument of perigee (rad). */
    private final double pa;

    /** Right Ascension of the Ascending node (rad). */
    private final double raan;

    /** Mean anomaly (rad). */
    private final double meanAnomaly;

    /** Revolution number at epoch. */
    private final int revolutionNumberAtEpoch;

    /** Ballistic coefficient. */
    private final double bStar;

    /** First line. */
    private String line1;

    /** Second line. */
    private String line2;

    /** Simple constructor from unparsed two lines.
     * <p>The static method {@link #isFormatOK(String, String)} should be called
     * before trying to build this object.<p>
     * @param line1 the first element (69 char String)
     * @param line2 the second element (69 char String)
     * @exception OrekitException if some format error occurs or lines are inconsistent
     */
    public TLE(final String line1, final String line2) throws OrekitException {

        // identification
        satelliteNumber = parseInteger(line1, 2, 5);
        final int satNum2 = parseInteger(line2, 2, 5);
        if (satelliteNumber != satNum2) {
            throw new OrekitException("TLE lines do not refer to the same object:\n{0}\n{1}",
                                      line1, line2);
        }
        classification  = line1.charAt(7);
        launchYear      = parseYear(line1, 9);
        launchNumber    = parseInteger(line1, 11, 3);
        launchPiece     = line1.substring(14, 17).trim();
        ephemerisType   = parseInteger(line1, 62, 1);
        elementNumber   = parseInteger(line1, 64, 4);

        // Date format transform:
        final DateComponents date = new DateComponents(parseYear(line1, 18), 1, 1);
        final double dayNb = parseDouble(line1, 20, 12);
        epoch = new AbsoluteDate(date, TimeComponents.H00,
                                 TimeScalesFactory.getUTC()).shiftedBy((dayNb - 1) * Constants.JULIAN_DAY); //-1 is due to TLE date definition

        // mean motion development
        // converted from rev/day, 2 * rev/day^2 and 6 * rev/day^3 to rad/s, rad/s^2 and rad/s^3
        meanMotion                 = parseDouble(line2, 52, 11) * Math.PI / 43200.0;
        meanMotionFirstDerivative  = parseDouble(line1, 33, 10) * Math.PI / 1.86624e9;
        meanMotionSecondDerivative = Double.parseDouble((line1.substring(44, 45) + '.' +
                                                         line1.substring(45, 50) + 'e' +
                                                         line1.substring(50, 52)).replace(' ', '0')) *
                                     Math.PI / 5.3747712e13;

        eccentricity = Double.parseDouble("." + line2.substring(26, 33).replace(' ', '0'));
        inclination  = Math.toRadians(parseDouble(line2, 8, 8));
        pa           = Math.toRadians(parseDouble(line2, 34, 8));
        raan         = Math.toRadians(Double.parseDouble(line2.substring(17, 25).replace(' ', '0')));
        meanAnomaly  = Math.toRadians(parseDouble(line2, 43, 8));

        revolutionNumberAtEpoch = parseInteger(line2, 63, 5);
        bStar = Double.parseDouble((line1.substring(53, 54) + '.' +
                                    line1.substring(54, 59) + 'e' +
                                    line1.substring(59, 61)).replace(' ', '0'));

        // save the lines
        this.line1 = line1;
        this.line2 = line2;

    }

    /** Simple constructor from already parsed elements.
     * @param satelliteNumber satellite number
     * @param classification classification (U for unclassified)
     * @param launchYear launch year (all digits)
     * @param launchNumber launch number
     * @param launchPiece launch piece
     * @param ephemerisType type of ephemeris
     * @param elementNumber element number
     * @param epoch elements epoch
     * @param meanMotion mean motion (rad/s)
     * @param meanMotionFirstDerivative mean motion first derivative (rad/s<sup>2</sup>)
     * @param meanMotionSecondDerivative mean motion second derivative (rad/s<sup>3</sup>)
     * @param e eccentricity
     * @param i inclination (rad)
     * @param pa argument of perigee (rad)
     * @param raan right ascension of ascending node (rad)
     * @param meanAnomaly mean anomaly (rad)
     * @param revolutionNumberAtEpoch revolution number at epoch
     * @param bStar ballistic coefficient
     */
    public TLE(final int satelliteNumber, final char classification,
               final int launchYear, final int launchNumber, final String launchPiece,
               final int ephemerisType, final int elementNumber, final AbsoluteDate epoch,
               final double meanMotion, final double meanMotionFirstDerivative,
               final double meanMotionSecondDerivative, final double e, final double i,
               final double pa, final double raan, final double meanAnomaly,
               final int revolutionNumberAtEpoch, final double bStar) {

        // identification
        this.satelliteNumber = satelliteNumber;
        this.classification  = classification;
        this.launchYear      = launchYear;
        this.launchNumber    = launchNumber;
        this.launchPiece     = launchPiece;
        this.ephemerisType   = ephemerisType;
        this.elementNumber   = elementNumber;

        // orbital parameters
        this.epoch                      = epoch;
        this.meanMotion                 = meanMotion;
        this.meanMotionFirstDerivative  = meanMotionFirstDerivative;
        this.meanMotionSecondDerivative = meanMotionSecondDerivative;
        this.inclination                = i;
        this.raan                       = raan;
        this.eccentricity               = e;
        this.pa                         = pa;
        this.meanAnomaly                = meanAnomaly;

        this.revolutionNumberAtEpoch = revolutionNumberAtEpoch;
        this.bStar                   = bStar;

        // don't build the line until really needed
        this.line1 = null;
        this.line2 = null;

    }

    /** Get the first line.
     * @return first line
     * @exception OrekitException if UTC conversion cannot be done
     */
    public String getLine1()
        throws OrekitException {
        if (line1 == null) {
            buildLine1();
        }
        return line1;
    }

    /** Get the second line.
     * @return second line
     */
    public String getLine2() {
        if (line2 == null) {
            buildLine2();
        }
        return line2;
    }

    /** Build the line 1 from the parsed elements.
     * @exception OrekitException if UTC conversion cannot be done
     */
    private void buildLine1()
        throws OrekitException {

        final StringBuffer buffer = new StringBuffer();
        final DecimalFormat f38  = new DecimalFormat("##0.00000000", SYMBOLS);
        final DecimalFormat fExp = new DecimalFormat(".00000E0", SYMBOLS);

        buffer.append('1');

        buffer.append(' ');
        buffer.append(addPadding(satelliteNumber, '0', 5, true));
        buffer.append(classification);

        buffer.append(' ');
        buffer.append(addPadding(launchYear % 100, '0', 2, true));
        buffer.append(addPadding(launchNumber, '0', 3, true));
        buffer.append(addPadding(launchPiece, ' ', 3, false));

        buffer.append(' ');
        final TimeScale utc = TimeScalesFactory.getUTC();
        final int year = epoch.getComponents(utc).getDate().getYear();
        buffer.append(addPadding(year % 100, '0', 2, true));
        final double day = 1.0 + epoch.durationFrom(new AbsoluteDate(year, 1, 1, utc)) / Constants.JULIAN_DAY;
        buffer.append(f38.format(day));

        buffer.append(' ');
        final double n1 = meanMotionFirstDerivative * 1.86624e9 / Math.PI;
        final String sn1 = addPadding(new DecimalFormat(".00000000", SYMBOLS).format(n1), ' ', 10, true);
        buffer.append(sn1);

        buffer.append(' ');
        final double n2 = meanMotionSecondDerivative * 5.3747712e13 / Math.PI;
        final String doubleDash = "--";
        final String sn2 = fExp.format(n2).replace('E', '-').replace(doubleDash, "-").replace(".", "");
        buffer.append(addPadding(sn2, ' ', 8, true));

        buffer.append(' ');
        final String sB = fExp.format(bStar).replace('E', '-').replace(doubleDash, "-").replace(".", "");
        buffer.append(addPadding(sB, ' ', 8, true));

        buffer.append(' ');
        buffer.append(ephemerisType);

        buffer.append(' ');
        buffer.append(addPadding(elementNumber, ' ', 4, true));

        buffer.append(Integer.toString(checksum(buffer)));

        line1 = buffer.toString();

    }

    /** Build the line 2 from the parsed elements.
     */
    private void buildLine2() {

        final StringBuffer buffer = new StringBuffer();
        final DecimalFormat f34   = new DecimalFormat("##0.0000", SYMBOLS);
        final DecimalFormat f211  = new DecimalFormat("#0.00000000", SYMBOLS);

        buffer.append('2');

        buffer.append(' ');
        buffer.append(addPadding(satelliteNumber, '0', 5, true));

        buffer.append(' ');
        buffer.append(addPadding(f34.format(Math.toDegrees(inclination)), ' ', 8, true));
        buffer.append(' ');
        buffer.append(addPadding(f34.format(Math.toDegrees(raan)), ' ', 8, true));
        buffer.append(' ');
        buffer.append(addPadding((int) Math.rint(eccentricity * 1.0e7), '0', 7, true));
        buffer.append(' ');
        buffer.append(addPadding(f34.format(Math.toDegrees(pa)), ' ', 8, true));
        buffer.append(' ');
        buffer.append(addPadding(f34.format(Math.toDegrees(meanAnomaly)), ' ', 8, true));

        buffer.append(' ');
        buffer.append(addPadding(f211.format(meanMotion * 43200.0 / Math.PI), ' ', 11, true));
        buffer.append(addPadding(revolutionNumberAtEpoch, ' ', 5, true));

        buffer.append(Integer.toString(checksum(buffer)));

        line2 = buffer.toString();

    }

    /** Add padding characters before an integer.
     * @param k integer to pad
     * @param c padding character
     * @param size desired size
     * @param rightJustified if true, the resulting string is
     * right justified (i.e. space are added to the left)
     * @return padded string
     */
    private String addPadding(final int k, final char c,
                              final int size, final boolean rightJustified) {
        return addPadding(Integer.toString(k), c, size, rightJustified);
    }

    /** Add padding characters to a string.
     * @param string string to pad
     * @param c padding character
     * @param size desired size
     * @param rightJustified if true, the resulting string is
     * right justified (i.e. space are added to the left)
     * @return padded string
     */
    private String addPadding(final String string, final char c,
                              final int size, final boolean rightJustified) {

        final StringBuffer padding = new StringBuffer();
        for (int i = 0; i < size; ++i) {
            padding.append(c);
        }

        if (rightJustified) {
            final String concatenated = padding + string;
            final int l = concatenated.length();
            return concatenated.substring(l - size, l);
        }

        return (string + padding).substring(0, size);

    }

    /** Parse a double.
     * @param line line to parse
     * @param start start index of the first character
     * @param length length of the string
     * @return value of the double
     */
    private double parseDouble(final String line, final int start, final int length) {
        return Double.parseDouble(line.substring(start, start + length).replace(' ', '0'));
    }

    /** Parse an integer.
     * @param line line to parse
     * @param start start index of the first character
     * @param length length of the string
     * @return value of the integer
     */
    private int parseInteger(final String line, final int start, final int length) {
        return Integer.parseInt(line.substring(start, start + length).replace(' ', '0'));
    }

    /** Parse a year written on 2 digits.
     * @param line line to parse
     * @param start start index of the first character
     * @return value of the year
     */
    private int parseYear(final String line, final int start) {
        final int year = 2000 + parseInteger(line, start, 2);
        return (year > 2056) ? (year - 100) : year;
    }

    /** Get the satellite id.
     * @return the satellite number
     */
    public int getSatelliteNumber() {
        return satelliteNumber;
    }

    /** Get the classification.
     * @return classification
     */
    public char getClassification() {
        return classification;
    }

    /** Get the launch year.
     * @return the launch year
     */
    public int getLaunchYear() {
        return launchYear;
    }

    /** Get the launch number.
     * @return the launch number
     */
    public int getLaunchNumber() {
        return launchNumber;
    }

    /** Get the launch piece.
     * @return the launch piece
     */
    public String getLaunchPiece() {
        return launchPiece;
    }

    /** Get the type of ephemeris.
     * @return the ephemeris type (one of {@link #DEFAULT}, {@link #SGP},
     * {@link #SGP4}, {@link #SGP8}, {@link #SDP4}, {@link #SDP8})
     */
    public int getEphemerisType() {
        return ephemerisType;
    }

    /** Get the element number.
     * @return the element number
     */
    public int getElementNumber() {
        return elementNumber;
    }

    /** Get the TLE current date.
     * @return the epoch
     */
    public AbsoluteDate getDate() {
        return epoch;
    }

    /** Get the mean motion.
     * @return the mean motion (rad/s)
     */
    public double getMeanMotion() {
        return meanMotion;
    }

    /** Get the mean motion first derivative.
     * @return the mean motion first derivative (rad/s<sup>2</sup>)
     */
    public double getMeanMotionFirstDerivative() {
        return meanMotionFirstDerivative;
    }

    /** Get the mean motion second derivative.
     * @return the mean motion second derivative (rad/s<sup>3</sup>)
     */
    public double getMeanMotionSecondDerivative() {
        return meanMotionSecondDerivative;
    }

    /** Get the eccentricity.
     * @return the eccentricity
     */
    public double getE() {
        return eccentricity;
    }

    /** Get the inclination.
     * @return the inclination (rad)
     */
    public double getI() {
        return inclination;
    }

    /** Get the argument of perigee.
     * @return omega (rad)
     */
    public double getPerigeeArgument() {
        return pa;
    }

    /** Get Right Ascension of the Ascending node.
     * @return the raan (rad)
     */
    public double getRaan() {
        return raan;
    }

    /** Get the mean anomaly.
     * @return the mean anomaly (rad)
     */
    public double getMeanAnomaly() {
        return meanAnomaly;
    }

    /** Get the revolution number.
     * @return the revolutionNumberAtEpoch
     */
    public int getRevolutionNumberAtEpoch() {
        return revolutionNumberAtEpoch;
    }

    /** Get the ballistic coefficient.
     * @return bStar
     */
    public double getBStar() {
        return bStar;
    }

    /** Get a string representation of this TLE set.
     * <p>The representation is simply the two lines separated by the
     * platform line separator.</p>
     * @return string representation of this TLE set
     */
    public String toString() {
        try {
            return getLine1() + System.getProperty("line.separator") + getLine2();
        } catch (OrekitException oe) {
            throw OrekitException.createInternalError(oe);
        }
    }

    /** Check the lines format validity.
     * @param line1 the first element
     * @param line2 the second element
     * @return true if format is recognized (non null lines, 69 characters length,
     * line content), false if not
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
        final int checksum1 = checksum(line1);
        if (Integer.parseInt(line1.substring(68)) != (checksum1 % 10)) {
            throw new OrekitException(CHECKSUM_MESSAGE,
                                      1, line1.substring(68) ,
                                      checksum1 % 10, line1);
        }

        final int checksum2 = checksum(line2);
        if (Integer.parseInt(line2.substring(68)) != (checksum2 % 10)) {
            throw new OrekitException(CHECKSUM_MESSAGE,
                                      2, line2.substring(68) ,
                                          checksum2 % 10, line2);
        }

        return true;

    }

    /** Compute the checksum of the first 68 characters of a line.
     * @param line line to check
     * @return checksum
     */
    private static int checksum(final CharSequence line) {
        int sum = 0;
        for (int j = 0; j < 68; j++) {
            final char c = line.charAt(j);
            if (Character.isDigit(c)) {
                sum += Character.digit(c, 10);
            } else if (c == '-') {
                ++sum;
            }
        }
        return sum % 10;
    }

}
