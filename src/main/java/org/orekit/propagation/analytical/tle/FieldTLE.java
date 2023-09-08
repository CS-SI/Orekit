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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.ArithmeticUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.tle.generation.TleGenerationAlgorithm;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
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
 * @author Thomas Paulet (field translation)
 * @since 11.0
 * @param <T> type of the field elements
 */
public class FieldTLE<T extends CalculusFieldElement<T>> implements FieldTimeStamped<T>, Serializable, ParameterDriversProvider {

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
    private final transient FieldAbsoluteDate<T> epoch;

    /** Mean motion (rad/s). */
    private final T meanMotion;

    /** Mean motion first derivative (rad/s²). */
    private final T meanMotionFirstDerivative;

    /** Mean motion second derivative (rad/s³). */
    private final T meanMotionSecondDerivative;

    /** Eccentricity. */
    private final T eccentricity;

    /** Inclination (rad). */
    private final T inclination;

    /** Argument of perigee (rad). */
    private final T pa;

    /** Right Ascension of the Ascending node (rad). */
    private final T raan;

    /** Mean anomaly (rad). */
    private final T meanAnomaly;

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
     * @param field field utilized by default
     * @param line1 the first element (69 char String)
     * @param line2 the second element (69 char String)
     * @see #FieldTLE(Field, String, String, TimeScale)
     */
    @DefaultDataContext
    public FieldTLE(final Field<T> field, final String line1, final String line2) {
        this(field, line1, line2, DataContext.getDefault().getTimeScales().getUTC());
    }

    /** Simple constructor from unparsed two lines using the given time scale as UTC.
     *
     *<p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * <p>The static method {@link #isFormatOK(String, String)} should be called
     * before trying to build this object.</p>
     * @param field field utilized by default
     * @param line1 the first element (69 char String)
     * @param line2 the second element (69 char String)
     * @param utc the UTC time scale.
     */
    public FieldTLE(final Field<T> field, final String line1, final String line2, final TimeScale utc) {

        // zero and pi for fields
        final T zero = field.getZero();
        final T pi   = zero.getPi();

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
        epoch = new FieldAbsoluteDate<>(field, new DateComponents(year, dayInYear),
                                 new TimeComponents(secondsA, secondsB),
                                 utc);

        // mean motion development
        // converted from rev/day, 2 * rev/day^2 and 6 * rev/day^3 to rad/s, rad/s^2 and rad/s^3
        meanMotion                 = pi.multiply(ParseUtils.parseDouble(line2, 52, 11)).divide(43200.0);
        meanMotionFirstDerivative  = pi.multiply(ParseUtils.parseDouble(line1, 33, 10)).divide(1.86624e9);
        meanMotionSecondDerivative = pi.multiply(Double.parseDouble((line1.substring(44, 45) + '.' +
                                                                     line1.substring(45, 50) + 'e' +
                                                                     line1.substring(50, 52)).replace(' ', '0'))).divide(5.3747712e13);

        eccentricity = zero.add(Double.parseDouble("." + line2.substring(26, 33).replace(' ', '0')));
        inclination  = zero.add(FastMath.toRadians(ParseUtils.parseDouble(line2, 8, 8)));
        pa           = zero.add(FastMath.toRadians(ParseUtils.parseDouble(line2, 34, 8)));
        raan         = zero.add(FastMath.toRadians(Double.parseDouble(line2.substring(17, 25).replace(' ', '0'))));
        meanAnomaly  = zero.add(FastMath.toRadians(ParseUtils.parseDouble(line2, 43, 8)));

        revolutionNumberAtEpoch = ParseUtils.parseInteger(line2, 63, 5);
        final double bStarValue = Double.parseDouble((line1.substring(53, 54) + '.' +
                        line1.substring(54, 59) + 'e' +
                        line1.substring(59, 61)).replace(' ', '0'));

        // save the lines
        this.line1 = line1;
        this.line2 = line2;
        this.utc = utc;

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
     * @see #FieldTLE(int, char, int, int, String, int, int, FieldAbsoluteDate, CalculusFieldElement, CalculusFieldElement,
     * CalculusFieldElement, CalculusFieldElement, CalculusFieldElement, CalculusFieldElement, CalculusFieldElement, CalculusFieldElement, int, double, TimeScale)
     */
    @DefaultDataContext
    public FieldTLE(final int satelliteNumber, final char classification,
               final int launchYear, final int launchNumber, final String launchPiece,
               final int ephemerisType, final int elementNumber, final FieldAbsoluteDate<T> epoch,
               final T meanMotion, final T meanMotionFirstDerivative,
               final T meanMotionSecondDerivative, final T e, final T i,
               final T pa, final T raan, final T meanAnomaly,
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
     */
    public FieldTLE(final int satelliteNumber, final char classification,
               final int launchYear, final int launchNumber, final String launchPiece,
               final int ephemerisType, final int elementNumber, final FieldAbsoluteDate<T> epoch,
               final T meanMotion, final T meanMotionFirstDerivative,
               final T meanMotionSecondDerivative, final T e, final T i,
               final T pa, final T raan, final T meanAnomaly,
               final int revolutionNumberAtEpoch, final double bStar,
               final TimeScale utc) {

        // pi for fields
        final T pi = e.getPi();

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
        this.raan = MathUtils.normalizeAngle(raan, pi);

        // Checking eccentricity range
        this.eccentricity = e;

        // Normalizing PA in [0,2pi] interval
        this.pa = MathUtils.normalizeAngle(pa, pi);

        // Normalizing mean anomaly in [0,2pi] interval
        this.meanAnomaly = MathUtils.normalizeAngle(meanAnomaly, pi);

        this.revolutionNumberAtEpoch = revolutionNumberAtEpoch;
        this.bStarParameterDriver = new ParameterDriver(B_STAR, bStar, B_STAR_SCALE,
                                                       Double.NEGATIVE_INFINITY,
                                                       Double.POSITIVE_INFINITY);

        // don't build the line until really needed
        this.line1 = null;
        this.line2 = null;
        this.utc = utc;

    }

    /**
     * Get the UTC time scale used to create this TLE.
     *
     * @return UTC time scale.
     */
    TimeScale getUtc() {
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
        final double n1 = meanMotionFirstDerivative.divide(pa.getPi()).multiply(1.86624e9).getReal();
        final String sn1 = ParseUtils.addPadding("meanMotionFirstDerivative",
                                                 new DecimalFormat(".00000000", SYMBOLS).format(n1),
                                                 ' ', 10, true, satelliteNumber);
        buffer.append(sn1);

        buffer.append(' ');
        final double n2 = meanMotionSecondDerivative.divide(pa.getPi()).multiply(5.3747712e13).getReal();
        buffer.append(formatExponentMarkerFree("meanMotionSecondDerivative", n2, 5, ' ', 8, true));

        buffer.append(' ');
        buffer.append(formatExponentMarkerFree("B*", getBStar(), 5, ' ', 8, true));

        buffer.append(' ');
        buffer.append(ephemerisType);

        buffer.append(' ');
        buffer.append(ParseUtils.addPadding("elementNumber", elementNumber, ' ', 4, true, satelliteNumber));

        buffer.append(Integer.toString(checksum(buffer)));

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
        final String sMantissa = ParseUtils.addPadding(name, (int) mantissa,
                                                       '0', mantissaSize, true, satelliteNumber);
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
        buffer.append(ParseUtils.addPadding(INCLINATION, f34.format(FastMath.toDegrees(inclination).getReal()), ' ', 8, true, satelliteNumber));
        buffer.append(' ');
        buffer.append(ParseUtils.addPadding("raan", f34.format(FastMath.toDegrees(raan).getReal()), ' ', 8, true, satelliteNumber));
        buffer.append(' ');
        buffer.append(ParseUtils.addPadding(ECCENTRICITY, (int) FastMath.rint(eccentricity.getReal() * 1.0e7), '0', 7, true, satelliteNumber));
        buffer.append(' ');
        buffer.append(ParseUtils.addPadding("pa", f34.format(FastMath.toDegrees(pa).getReal()), ' ', 8, true, satelliteNumber));
        buffer.append(' ');
        buffer.append(ParseUtils.addPadding("meanAnomaly", f34.format(FastMath.toDegrees(meanAnomaly).getReal()), ' ', 8, true, satelliteNumber));

        buffer.append(' ');
        buffer.append(ParseUtils.addPadding(MEAN_MOTION, f211.format(meanMotion.divide(pa.getPi()).multiply(43200.0).getReal()), ' ', 11, true, satelliteNumber));
        buffer.append(ParseUtils.addPadding("revolutionNumberAtEpoch", revolutionNumberAtEpoch,
                                            ' ', 5, true, satelliteNumber));

        buffer.append(Integer.toString(checksum(buffer)));

        line2 = buffer.toString();

    }

    /** {@inheritDoc}.
     * <p>Get the drivers for TLE propagation SGP4 and SDP4.
     * @return drivers for SGP4 and SDP4 model parameters
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(bStarParameterDriver);
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
    public FieldAbsoluteDate<T> getDate() {
        return epoch;
    }

    /** Get the mean motion.
     * @return the mean motion (rad/s)
     */
    public T getMeanMotion() {
        return meanMotion;
    }

    /** Get the mean motion first derivative.
     * @return the mean motion first derivative (rad/s²)
     */
    public T getMeanMotionFirstDerivative() {
        return meanMotionFirstDerivative;
    }

    /** Get the mean motion second derivative.
     * @return the mean motion second derivative (rad/s³)
     */
    public T getMeanMotionSecondDerivative() {
        return meanMotionSecondDerivative;
    }

    /** Get the eccentricity.
     * @return the eccentricity
     */
    public T getE() {
        return eccentricity;
    }

    /** Get the inclination.
     * @return the inclination (rad)
     */
    public T getI() {
        return inclination;
    }

    /** Get the argument of perigee.
     * @return omega (rad)
     */
    public T getPerigeeArgument() {
        return pa;
    }

    /** Get Right Ascension of the Ascending node.
     * @return the raan (rad)
     */
    public T getRaan() {
        return raan;
    }

    /** Get the mean anomaly.
     * @return the mean anomaly (rad)
     */
    public T getMeanAnomaly() {
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
        return bStarParameterDriver.getValue(getDate().toAbsoluteDate());
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
            throw new OrekitInternalError(oe);
        }
    }

    /**
     * Convert Spacecraft State into TLE.
     *
     * @param state Spacecraft State to convert into TLE
     * @param templateTLE first guess used to get identification and estimate new TLE
     * @param generationAlgorithm TLE generation algorithm
     * @param <T> type of the element
     * @return a generated TLE
     * @since 12.0
     */
    public static <T extends CalculusFieldElement<T>> FieldTLE<T> stateToTLE(final FieldSpacecraftState<T> state, final FieldTLE<T> templateTLE,
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
        return TLE.isFormatOK(line1, line2);
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

    /**
     * Convert FieldTLE into TLE.
     * @return TLE
     */
    public TLE toTLE() {
        final TLE regularTLE = new TLE(getSatelliteNumber(), getClassification(), getLaunchYear(), getLaunchNumber(), getLaunchPiece(), getEphemerisType(),
                                       getElementNumber(), getDate().toAbsoluteDate(), getMeanMotion().getReal(), getMeanMotionFirstDerivative().getReal(),
                                       getMeanMotionSecondDerivative().getReal(), getE().getReal(), getI().getReal(), getPerigeeArgument().getReal(),
                                       getRaan().getReal(), getMeanAnomaly().getReal(), getRevolutionNumberAtEpoch(), getBStar(), getUtc());

        for (int k = 0; k < regularTLE.getParametersDrivers().size(); ++k) {
            regularTLE.getParametersDrivers().get(k).setSelected(getParametersDrivers().get(k).isSelected());
        }

        return regularTLE;

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
        if (!(o instanceof FieldTLE)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        final FieldTLE<T> tle = (FieldTLE<T>) o;
        return satelliteNumber == tle.satelliteNumber &&
                classification == tle.classification &&
                launchYear == tle.launchYear &&
                launchNumber == tle.launchNumber &&
                Objects.equals(launchPiece, tle.launchPiece) &&
                ephemerisType == tle.ephemerisType &&
                elementNumber == tle.elementNumber &&
                Objects.equals(epoch, tle.epoch) &&
                meanMotion.getReal() == tle.meanMotion.getReal() &&
                meanMotionFirstDerivative.getReal() == tle.meanMotionFirstDerivative.getReal() &&
                meanMotionSecondDerivative.getReal() == tle.meanMotionSecondDerivative.getReal() &&
                eccentricity.getReal() == tle.eccentricity.getReal() &&
                inclination.getReal() == tle.inclination.getReal() &&
                pa.getReal() == tle.pa.getReal() &&
                raan.getReal() == tle.raan.getReal() &&
                meanAnomaly.getReal() == tle.meanAnomaly.getReal() &&
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

}
