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
package org.orekit.propagation.analytical.tle;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.hipparchus.util.ArithmeticUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.generation.TleGenerationAlgorithm;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;

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
 * <a href="https://www.celestrak.com/">CelesTrak website.</a></p>
 * @author Fabien Maussion
 * @author Luc Maisonobe
 */
public class TLE implements TimeStamped, Serializable, ParameterDriversProvider {

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

    /** Identifier for default type of ephemeris (SGP4/SDP4). */
    public static final int DEFAULT = 0;

    /** Parameter name for B* coefficient. */
    public static final String B_STAR = "BSTAR";

    /** B* scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double B_STAR_SCALE = FastMath.scalb(1.0, -20);

    /** Name of the mean motion parameter. */
    private static final String MEAN_MOTION = "meanMotion";

    /** Name of the inclination parameter. */
    private static final String INCLINATION = "inclination";

    /** Name of the eccentricity parameter. */
    private static final String ECCENTRICITY = "eccentricity";

    /** Pattern for line 1. */
    private static final Pattern LINE_1_PATTERN =
        Pattern.compile("1 [ 0-9A-Z&&[^IO]][ 0-9]{4}[A-Z] [ 0-9]{5}[ A-Z]{3} [ 0-9]{5}[.][ 0-9]{8} (?:(?:[ 0+-][.][ 0-9]{8})|(?: [ +-][.][ 0-9]{7})) " +
                        "[ +-][ 0-9]{5}[+-][ 0-9] [ +-][ 0-9]{5}[+-][ 0-9] [ 0-9] [ 0-9]{4}[ 0-9]");

    /** Pattern for line 2. */
    private static final Pattern LINE_2_PATTERN =
        Pattern.compile("2 [ 0-9A-Z&&[^IO]][ 0-9]{4} [ 0-9]{3}[.][ 0-9]{4} [ 0-9]{3}[.][ 0-9]{4} [ 0-9]{7} " +
                        "[ 0-9]{3}[.][ 0-9]{4} [ 0-9]{3}[.][ 0-9]{4} [ 0-9]{2}[.][ 0-9]{13}[ 0-9]");

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

    /** Mean motion first derivative (rad/s²). */
    private final double meanMotionFirstDerivative;

    /** Mean motion second derivative (rad/s³). */
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

    /** First line. */
    private String line1;

    /** Second line. */
    private String line2;

    /** The UTC scale. */
    private final TimeScale utc;

    /** Driver for ballistic coefficient parameter. */
    private final transient ParameterDriver bStarParameterDriver;


    /** Simple constructor from unparsed two lines. This constructor uses the {@link
     * DataContext#getDefault() default data context}.
     *
     * <p>The static method {@link #isFormatOK(String, String)} should be called
     * before trying to build this object.</p>
     * @param line1 the first element (69 char String)
     * @param line2 the second element (69 char String)
     * @see #TLE(String, String, TimeScale)
     */
    @DefaultDataContext
    public TLE(final String line1, final String line2) {
        this(line1, line2, DataContext.getDefault().getTimeScales().getUTC());
    }

    /** Simple constructor from unparsed two lines using the given time scale as UTC.
     *
     * <p>The static method {@link #isFormatOK(String, String)} should be called
     * before trying to build this object.</p>
     * @param line1 the first element (69 char String)
     * @param line2 the second element (69 char String)
     * @param utc the UTC time scale.
     * @since 10.1
     */
    public TLE(final String line1, final String line2, final TimeScale utc) {

        // identification
        satelliteNumber = ParseUtils.parseSatelliteNumber(line1, 2, 5);
        final int satNum2 = ParseUtils.parseSatelliteNumber(line2, 2, 5);
        if (satelliteNumber != satNum2) {
            throw new OrekitException(OrekitMessages.TLE_LINES_DO_NOT_REFER_TO_SAME_OBJECT,
                                      line1, line2);
        }
        classification  = line1.charAt(7);
        launchYear      = ParseUtils.parseYear(line1, 9);
        launchNumber    = ParseUtils.parseInteger(line1, 11, 3);
        launchPiece     = line1.substring(14, 17).trim();
        ephemerisType   = ParseUtils.parseInteger(line1, 62, 1);
        elementNumber   = ParseUtils.parseInteger(line1, 64, 4);

        // Date format transform (nota: 27/31250 == 86400/100000000)
        final int    year      = ParseUtils.parseYear(line1, 18);
        final int    dayInYear = ParseUtils.parseInteger(line1, 20, 3);
        final long   df        = 27l * ParseUtils.parseInteger(line1, 24, 8);
        final int    secondsA  = (int) (df / 31250l);
        final double secondsB  = (df % 31250l) / 31250.0;
        epoch = new AbsoluteDate(new DateComponents(year, dayInYear),
                                 new TimeComponents(secondsA, secondsB),
                                 utc);

        // mean motion development
        // converted from rev/day, 2 * rev/day^2 and 6 * rev/day^3 to rad/s, rad/s^2 and rad/s^3
        meanMotion                 = ParseUtils.parseDouble(line2, 52, 11) * FastMath.PI / 43200.0;
        meanMotionFirstDerivative  = ParseUtils.parseDouble(line1, 33, 10) * FastMath.PI / 1.86624e9;
        meanMotionSecondDerivative = Double.parseDouble((line1.substring(44, 45) + '.' +
                                                         line1.substring(45, 50) + 'e' +
                                                         line1.substring(50, 52)).replace(' ', '0')) *
                                     FastMath.PI / 5.3747712e13;

        eccentricity = Double.parseDouble("." + line2.substring(26, 33).replace(' ', '0'));
        inclination  = FastMath.toRadians(ParseUtils.parseDouble(line2, 8, 8));
        pa           = FastMath.toRadians(ParseUtils.parseDouble(line2, 34, 8));
        raan         = FastMath.toRadians(Double.parseDouble(line2.substring(17, 25).replace(' ', '0')));
        meanAnomaly  = FastMath.toRadians(ParseUtils.parseDouble(line2, 43, 8));

        revolutionNumberAtEpoch = ParseUtils.parseInteger(line2, 63, 5);
        final double bStarValue = Double.parseDouble((line1.substring(53, 54) + '.' +
                                    line1.substring(54, 59) + 'e' +
                                    line1.substring(59, 61)).replace(' ', '0'));

        // save the lines
        this.line1 = line1;
        this.line2 = line2;
        this.utc = utc;

        // create model parameter drivers
        this.bStarParameterDriver = new ParameterDriver(B_STAR, bStarValue, B_STAR_SCALE,
                                                        Double.NEGATIVE_INFINITY,
                                                        Double.POSITIVE_INFINITY);

    }

    /**
     * <p>
     * Simple constructor from already parsed elements. This constructor uses the
     * {@link DataContext#getDefault() default data context}.
     * </p>
     *
     * <p>
     * The mean anomaly, the right ascension of ascending node Ω and the argument of
     * perigee ω are normalized into the [0, 2π] interval as they can be negative.
     * After that, a range check is performed on some of the orbital elements:
     *
     * <pre>
     *     meanMotion &gt;= 0
     *     0 &lt;= i &lt;= π
     *     0 &lt;= Ω &lt;= 2π
     *     0 &lt;= e &lt;= 1
     *     0 &lt;= ω &lt;= 2π
     *     0 &lt;= meanAnomaly &lt;= 2π
     * </pre>
     *
     * @param satelliteNumber satellite number
     * @param classification classification (U for unclassified)
     * @param launchYear launch year (all digits)
     * @param launchNumber launch number
     * @param launchPiece launch piece (3 char String)
     * @param ephemerisType type of ephemeris
     * @param elementNumber element number
     * @param epoch elements epoch
     * @param meanMotion mean motion (rad/s)
     * @param meanMotionFirstDerivative mean motion first derivative (rad/s²)
     * @param meanMotionSecondDerivative mean motion second derivative (rad/s³)
     * @param e eccentricity
     * @param i inclination (rad)
     * @param pa argument of perigee (rad)
     * @param raan right ascension of ascending node (rad)
     * @param meanAnomaly mean anomaly (rad)
     * @param revolutionNumberAtEpoch revolution number at epoch
     * @param bStar ballistic coefficient
     * @see #TLE(int, char, int, int, String, int, int, AbsoluteDate, double, double,
     * double, double, double, double, double, double, int, double, TimeScale)
     */
    @DefaultDataContext
    public TLE(final int satelliteNumber, final char classification,
               final int launchYear, final int launchNumber, final String launchPiece,
               final int ephemerisType, final int elementNumber, final AbsoluteDate epoch,
               final double meanMotion, final double meanMotionFirstDerivative,
               final double meanMotionSecondDerivative, final double e, final double i,
               final double pa, final double raan, final double meanAnomaly,
               final int revolutionNumberAtEpoch, final double bStar) {
        this(satelliteNumber, classification, launchYear, launchNumber, launchPiece,
                ephemerisType, elementNumber, epoch, meanMotion,
                meanMotionFirstDerivative, meanMotionSecondDerivative, e, i, pa, raan,
                meanAnomaly, revolutionNumberAtEpoch, bStar,
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * <p>
     * Simple constructor from already parsed elements using the given time scale as
     * UTC.
     * </p>
     *
     * <p>
     * The mean anomaly, the right ascension of ascending node Ω and the argument of
     * perigee ω are normalized into the [0, 2π] interval as they can be negative.
     * After that, a range check is performed on some of the orbital elements:
     *
     * <pre>
     *     meanMotion &gt;= 0
     *     0 &lt;= i &lt;= π
     *     0 &lt;= Ω &lt;= 2π
     *     0 &lt;= e &lt;= 1
     *     0 &lt;= ω &lt;= 2π
     *     0 &lt;= meanAnomaly &lt;= 2π
     * </pre>
     *
     * @param satelliteNumber satellite number
     * @param classification classification (U for unclassified)
     * @param launchYear launch year (all digits)
     * @param launchNumber launch number
     * @param launchPiece launch piece (3 char String)
     * @param ephemerisType type of ephemeris
     * @param elementNumber element number
     * @param epoch elements epoch
     * @param meanMotion mean motion (rad/s)
     * @param meanMotionFirstDerivative mean motion first derivative (rad/s²)
     * @param meanMotionSecondDerivative mean motion second derivative (rad/s³)
     * @param e eccentricity
     * @param i inclination (rad)
     * @param pa argument of perigee (rad)
     * @param raan right ascension of ascending node (rad)
     * @param meanAnomaly mean anomaly (rad)
     * @param revolutionNumberAtEpoch revolution number at epoch
     * @param bStar ballistic coefficient
     * @param utc the UTC time scale.
     * @since 10.1
     */
    public TLE(final int satelliteNumber, final char classification,
               final int launchYear, final int launchNumber, final String launchPiece,
               final int ephemerisType, final int elementNumber, final AbsoluteDate epoch,
               final double meanMotion, final double meanMotionFirstDerivative,
               final double meanMotionSecondDerivative, final double e, final double i,
               final double pa, final double raan, final double meanAnomaly,
               final int revolutionNumberAtEpoch, final double bStar,
               final TimeScale utc) {

        // identification
        this.satelliteNumber = satelliteNumber;
        this.classification  = classification;
        this.launchYear      = launchYear;
        this.launchNumber    = launchNumber;
        this.launchPiece     = launchPiece;
        this.ephemerisType   = ephemerisType;
        this.elementNumber   = elementNumber;

        // orbital parameters
        this.epoch = epoch;
        // Checking mean motion range
        this.meanMotion = meanMotion;
        this.meanMotionFirstDerivative = meanMotionFirstDerivative;
        this.meanMotionSecondDerivative = meanMotionSecondDerivative;

        // Checking inclination range
        this.inclination = i;

        // Normalizing RAAN in [0,2pi] interval
        this.raan = MathUtils.normalizeAngle(raan, FastMath.PI);

        // Checking eccentricity range
        this.eccentricity = e;

        // Normalizing PA in [0,2pi] interval
        this.pa = MathUtils.normalizeAngle(pa, FastMath.PI);

        // Normalizing mean anomaly in [0,2pi] interval
        this.meanAnomaly = MathUtils.normalizeAngle(meanAnomaly, FastMath.PI);

        this.revolutionNumberAtEpoch = revolutionNumberAtEpoch;


        // don't build the line until really needed
        this.line1 = null;
        this.line2 = null;
        this.utc = utc;

        // create model parameter drivers
        this.bStarParameterDriver = new ParameterDriver(B_STAR, bStar, B_STAR_SCALE,
                                                        Double.NEGATIVE_INFINITY,
                                                        Double.POSITIVE_INFINITY);

    }

    /**
     * Get the UTC time scale used to create this TLE.
     *
     * @return UTC time scale.
     */
    public TimeScale getUtc() {
        return utc;
    }

    /** Get the first line.
     * @return first line
     */
    public String getLine1() {
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
     */
    private void buildLine1() {

        final StringBuilder buffer = new StringBuilder();

        buffer.append('1');

        buffer.append(' ');
        buffer.append(ParseUtils.buildSatelliteNumber(satelliteNumber, "satelliteNumber-1"));
        buffer.append(classification);

        buffer.append(' ');
        buffer.append(ParseUtils.addPadding("launchYear",   launchYear % 100, '0', 2, true, satelliteNumber));
        buffer.append(ParseUtils.addPadding("launchNumber", launchNumber, '0', 3, true, satelliteNumber));
        buffer.append(ParseUtils.addPadding("launchPiece",  launchPiece, ' ', 3, false, satelliteNumber));

        buffer.append(' ');
        DateTimeComponents dtc = epoch.getComponents(utc);
        int fraction = (int) FastMath.rint(31250 * dtc.getTime().getSecondsInUTCDay() / 27.0);
        if (fraction >= 100000000) {
            dtc =  epoch.shiftedBy(Constants.JULIAN_DAY).getComponents(utc);
            fraction -= 100000000;
        }
        buffer.append(ParseUtils.addPadding("year", dtc.getDate().getYear() % 100, '0', 2, true, satelliteNumber));
        buffer.append(ParseUtils.addPadding("day",  dtc.getDate().getDayOfYear(),  '0', 3, true, satelliteNumber));
        buffer.append('.');
        // nota: 31250/27 == 100000000/86400

        buffer.append(ParseUtils.addPadding("fraction", fraction,  '0', 8, true, satelliteNumber));

        buffer.append(' ');
        final double n1 = meanMotionFirstDerivative * 1.86624e9 / FastMath.PI;
        final String sn1 = ParseUtils.addPadding("meanMotionFirstDerivative",
                                                 new DecimalFormat(".00000000", SYMBOLS).format(n1),
                                                 ' ', 10, true, satelliteNumber);
        buffer.append(sn1);

        buffer.append(' ');
        final double n2 = meanMotionSecondDerivative * 5.3747712e13 / FastMath.PI;
        buffer.append(formatExponentMarkerFree("meanMotionSecondDerivative", n2, 5, ' ', 8, true));

        buffer.append(' ');
        buffer.append(formatExponentMarkerFree("B*", getBStar(), 5, ' ', 8, true));

        buffer.append(' ');
        buffer.append(ephemerisType);

        buffer.append(' ');
        buffer.append(ParseUtils.addPadding("elementNumber", elementNumber, ' ', 4, true, satelliteNumber));

        buffer.append(checksum(buffer));

        line1 = buffer.toString();

    }

    /** Format a real number without 'e' exponent marker.
     * @param name parameter name
     * @param d number to format
     * @param mantissaSize size of the mantissa (not counting initial '-' or ' ' for sign)
     * @param c padding character
     * @param size desired size
     * @param rightJustified if true, the resulting string is
     * right justified (i.e. space are added to the left)
     * @return formatted and padded number
     */
    private String formatExponentMarkerFree(final String name, final double d, final int mantissaSize,
                                            final char c, final int size, final boolean rightJustified) {
        final double dAbs = FastMath.abs(d);
        int exponent = (dAbs < 1.0e-9) ? -9 : (int) FastMath.ceil(FastMath.log10(dAbs));
        long mantissa = FastMath.round(dAbs * FastMath.pow(10.0, mantissaSize - exponent));
        if (mantissa == 0) {
            exponent = 0;
        } else if (mantissa > (ArithmeticUtils.pow(10, mantissaSize) - 1)) {
            // rare case: if d has a single digit like d = 1.0e-4 with mantissaSize = 5
            // the above computation finds exponent = -4 and mantissa = 100000 which
            // doesn't fit in a 5 digits string
            exponent++;
            mantissa = FastMath.round(dAbs * FastMath.pow(10.0, mantissaSize - exponent));
        }
        final String sMantissa = ParseUtils.addPadding(name, (int) mantissa, '0', mantissaSize, true, satelliteNumber);
        final String sExponent = Integer.toString(FastMath.abs(exponent));
        final String formatted = (d <  0 ? '-' : ' ') + sMantissa + (exponent <= 0 ? '-' : '+') + sExponent;

        return ParseUtils.addPadding(name, formatted, c, size, rightJustified, satelliteNumber);

    }

    /** Build the line 2 from the parsed elements.
     */
    private void buildLine2() {

        final StringBuilder buffer = new StringBuilder();
        final DecimalFormat f34   = new DecimalFormat("##0.0000", SYMBOLS);
        final DecimalFormat f211  = new DecimalFormat("#0.00000000", SYMBOLS);

        buffer.append('2');

        buffer.append(' ');
        buffer.append(ParseUtils.buildSatelliteNumber(satelliteNumber, "satelliteNumber-2"));

        buffer.append(' ');
        buffer.append(ParseUtils.addPadding(INCLINATION, f34.format(FastMath.toDegrees(inclination)), ' ', 8, true, satelliteNumber));
        buffer.append(' ');
        buffer.append(ParseUtils.addPadding("raan", f34.format(FastMath.toDegrees(raan)), ' ', 8, true, satelliteNumber));
        buffer.append(' ');
        buffer.append(ParseUtils.addPadding(ECCENTRICITY, (int) FastMath.rint(eccentricity * 1.0e7), '0', 7, true, satelliteNumber));
        buffer.append(' ');
        buffer.append(ParseUtils.addPadding("pa", f34.format(FastMath.toDegrees(pa)), ' ', 8, true, satelliteNumber));
        buffer.append(' ');
        buffer.append(ParseUtils.addPadding("meanAnomaly", f34.format(FastMath.toDegrees(meanAnomaly)), ' ', 8, true, satelliteNumber));

        buffer.append(' ');
        buffer.append(ParseUtils.addPadding(MEAN_MOTION, f211.format(meanMotion * 43200.0 / FastMath.PI), ' ', 11, true, satelliteNumber));
        buffer.append(ParseUtils.addPadding("revolutionNumberAtEpoch", revolutionNumberAtEpoch, ' ', 5, true, satelliteNumber));

        buffer.append(checksum(buffer));

        line2 = buffer.toString();

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
     * @return the mean motion first derivative (rad/s²)
     */
    public double getMeanMotionFirstDerivative() {
        return meanMotionFirstDerivative;
    }

    /** Get the mean motion second derivative.
     * @return the mean motion second derivative (rad/s³)
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

    /** Get the ballistic coefficient at tle date.
     * @return bStar
     */
    public double getBStar() {
        return bStarParameterDriver.getValue(getDate());
    }

    /** Get the ballistic coefficient at a specific date.
     * @param date at which the ballistic coefficient wants to be known.
     * @return bStar
     */
    public double getBStar(final AbsoluteDate date) {
        return bStarParameterDriver.getValue(date);
    }

    /** Get a string representation of this TLE set.
     * <p>The representation is simply the two lines separated by the
     * platform line separator.</p>
     * @return string representation of this TLE set
     */
    public String toString() {
        return getLine1() + System.getProperty("line.separator") + getLine2();
    }

    /**
     * Convert Spacecraft State into TLE.
     *
     * @param state Spacecraft State to convert into TLE
     * @param templateTLE first guess used to get identification and estimate new TLE
     * @param generationAlgorithm TLE generation algorithm
     * @return a generated TLE
     * @since 12.0
     */
    public static TLE stateToTLE(final SpacecraftState state, final TLE templateTLE,
                                 final TleGenerationAlgorithm generationAlgorithm) {
        return generationAlgorithm.generate(state, templateTLE);
    }

    /** Check the lines format validity.
     * @param line1 the first element
     * @param line2 the second element
     * @return true if format is recognized (non null lines, 69 characters length,
     * line content), false if not
     */
    public static boolean isFormatOK(final String line1, final String line2) {

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
            throw new OrekitException(OrekitMessages.TLE_CHECKSUM_ERROR,
                                      1, Integer.toString(checksum1 % 10), line1.substring(68), line1);
        }

        final int checksum2 = checksum(line2);
        if (Integer.parseInt(line2.substring(68)) != (checksum2 % 10)) {
            throw new OrekitException(OrekitMessages.TLE_CHECKSUM_ERROR,
                                      2, Integer.toString(checksum2 % 10), line2.substring(68), line2);
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

    /** Check if this tle equals the provided tle.
     * <p>Due to the difference in precision between object and string
     * representations of TLE, it is possible for this method to return false
     * even if string representations returned by {@link #toString()}
     * are equal.</p>
     * @param o other tle
     * @return true if this tle equals the provided tle
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TLE)) {
            return false;
        }
        final TLE tle = (TLE) o;
        return satelliteNumber == tle.satelliteNumber &&
                classification == tle.classification &&
                launchYear == tle.launchYear &&
                launchNumber == tle.launchNumber &&
                Objects.equals(launchPiece, tle.launchPiece) &&
                ephemerisType == tle.ephemerisType &&
                elementNumber == tle.elementNumber &&
                Objects.equals(epoch, tle.epoch) &&
                meanMotion == tle.meanMotion &&
                meanMotionFirstDerivative == tle.meanMotionFirstDerivative &&
                meanMotionSecondDerivative == tle.meanMotionSecondDerivative &&
                eccentricity == tle.eccentricity &&
                inclination == tle.inclination &&
                pa == tle.pa &&
                raan == tle.raan &&
                meanAnomaly == tle.meanAnomaly &&
                revolutionNumberAtEpoch == tle.revolutionNumberAtEpoch &&
                getBStar() == tle.getBStar();
    }

    /** Get a hashcode for this tle.
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(satelliteNumber,
                classification,
                launchYear,
                launchNumber,
                launchPiece,
                ephemerisType,
                elementNumber,
                epoch,
                meanMotion,
                meanMotionFirstDerivative,
                meanMotionSecondDerivative,
                eccentricity,
                inclination,
                pa,
                raan,
                meanAnomaly,
                revolutionNumberAtEpoch,
                getBStar());
    }

    /** Get the drivers for TLE propagation SGP4 and SDP4.
     * @return drivers for SGP4 and SDP4 model parameters
     */
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(bStarParameterDriver);
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(line1, line2, utc);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -1596648022319057689L;

        /** First line. */
        private String line1;

        /** Second line. */
        private String line2;

        /** The UTC scale. */
        private final TimeScale utc;

        /** Simple constructor.
         * @param line1 the first element (69 char String)
         * @param line2 the second element (69 char String)
         * @param utc the UTC time scale
         */
        DataTransferObject(final String line1, final String line2, final TimeScale utc) {
            this.line1 = line1;
            this.line2 = line2;
            this.utc   = utc;
        }

        /** Replace the deserialized data transfer object with a {@link TLE}.
         * @return replacement {@link TLE}
         */
        private Object readResolve() {
            return new TLE(line1, line2, utc);
        }

    }

}
