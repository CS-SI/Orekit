/* Copyright 2002-2026 CS GROUP
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
package org.orekit.models.earth.ionosphere;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldLine;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/**
 * Global Ionosphere Map (GIM) model.
 * The ionospheric delay is computed according to the formulas:
 * <pre>
 *           40.3
 *    δ =  --------  *  STEC      with, STEC = VTEC * F(elevation)
 *            f²
 * </pre>
 * With:
 * <ul>
 * <li>f: The frequency of the signal in Hz.</li>
 * <li>STEC: The Slant Total Electron Content in TECUnits.</li>
 * <li>VTEC: The Vertical Total Electron Content in TECUnits.</li>
 * <li>F(elevation): A mapping function which depends on satellite elevation.</li>
 * </ul>
 * The VTEC is read from a IONEX file. A file contains, for a given day,
 * VTEC maps corresponding to snapshots at some sampling hours within the day.
 * VTEC maps are TEC Values on regular latitude, longitude grids (typically
 * global 2.5° x 5.0° grids).
 * <p>
 * A bilinear interpolation is performed the case of the user initialize the latitude and the
 * longitude with values that are not contained in the stream.
 * </p><p>
 * A temporal interpolation is also performed to compute the VTEC at the desired date.
 * </p><p>
 * IONEX files are obtained from
 * <a href="https://cddis.nasa.gov/gnss/products/ionex/">Crustal Dynamics Data Information System</a>.
 * </p><p>
 * The files have to be extracted to UTF-8 text files before being read by this loader.
 * </p><p>
 * Example of file:
 * </p>
 * <pre>
 *      1.0            IONOSPHERE MAPS     GPS                 IONEX VERSION / TYPE
 * BIMINX V5.3         AIUB                16-JAN-19 07:26     PGM / RUN BY / DATE
 * BROADCAST IONOSPHERE MODEL FOR DAY 015, 2019                COMMENT
 *   2019     1    15     0     0     0                        EPOCH OF FIRST MAP
 *   2019     1    16     0     0     0                        EPOCH OF LAST MAP
 *   3600                                                      INTERVAL
 *     25                                                      # OF MAPS IN FILE
 *   NONE                                                      MAPPING FUNCTION
 *      0.0                                                    ELEVATION CUTOFF
 *                                                             OBSERVABLES USED
 *   6371.0                                                    BASE RADIUS
 *      2                                                      MAP DIMENSION
 *    350.0 350.0   0.0                                        HGT1 / HGT2 / DHGT
 *     87.5 -87.5  -2.5                                        LAT1 / LAT2 / DLAT
 *   -180.0 180.0   5.0                                        LON1 / LON2 / DLON
 *     -1                                                      EXPONENT
 * TEC/RMS values in 0.1 TECU; 9999, if no value available     COMMENT
 *                                                             END OF HEADER
 *      1                                                      START OF TEC MAP
 *   2019     1    15     0     0     0                        EPOCH OF CURRENT MAP
 *     87.5-180.0 180.0   5.0 350.0                            LAT/LON1/LON2/DLON/H
 *    92   92   92   92   92   92   92   92   92   92   92   92   92   92   92   92
 *    92   92   92   92   92   92   92   92   92   92   92   92   92   92   92   92
 *    92   92   92   92   92   92   92   92   92   92   92   92   92   92   92   92
 *    92   92   92   92   92   92   92   92   92   92   92   92   92   92   92   92
 *    92   92   92   92   92   92   92   92   92
 *    ...
 * </pre>
 * <p>
 * Note that this model {@link IonosphericModel#pathDelay(SpacecraftState, org.orekit.utils.PVCoordinatesProvider, double,
 * double[]) pathDelay} methods <em>requires</em> the {@link TopocentricFrame topocentric frame}
 * to lie on a {@link OneAxisEllipsoid} body shape, because the single layer on which
 * pierce point is computed must be an ellipsoidal shape at some altitude.
 * </p>
 * @see "Schaer, S., W. Gurtner, and J. Feltens, 1998, IONEX: The IONosphere Map EXchange
 *       Format Version 1, February 25, 1998, Proceedings of the IGS AC Workshop
 *       Darmstadt, Germany, February 9–11, 1998"
 *
 * @author Bryan Cazabonne
 *
 */
public class GlobalIonosphereMapModel extends AbstractIonosphericModel  {

    /** Map of interpolable TEC. */
    private final TimeSpanMap<TECMapPair> tecMap;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Loaded IONEX files.
     * @since 12.0
     */
    private String names;

    /** Interpolation method.
     * @since 13.1.1
     */
    private final TimeInterpolator interpolator;

    /**
     * Constructor with supported names given by user. This constructor uses the {@link
     * DataContext#getDefault() default data context}.
     *
     * @param earth earth body shape
     * @param supportedNames regular expression that matches the names of the IONEX files
     *                       to be loaded. See {@link DataProvidersManager#feed(String,
     *                       DataLoader)}.
     * @see #GlobalIonosphereMapModel(OneAxisEllipsoid, String, DataProvidersManager, TimeScale, TimeInterpolator)
     * @since 14.0
     */
    @DefaultDataContext
    public GlobalIonosphereMapModel(final OneAxisEllipsoid earth, final String supportedNames) {
        this(earth, supportedNames,
             DataContext.getDefault().getDataProvidersManager(),
             DataContext.getDefault().getTimeScales().getUTC(),
             TimeInterpolator.SIMPLE_LINEAR);
    }

    /**
     * Constructor that uses user defined supported names and data context.
     *
     * @param earth       earth body shape
     * @param supportedNames       regular expression that matches the names of the IONEX
     *                             files to be loaded. See {@link DataProvidersManager#feed(String,
     *                             DataLoader)}.
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc                  UTC time scale.
     * @since 14.0
     * @deprecated as of 13.1.1, replaced by
     * {@link #GlobalIonosphereMapModel(OneAxisEllipsoid, String, DataProvidersManager, TimeScale, TimeInterpolator)}
     */
    @Deprecated
    public GlobalIonosphereMapModel(final OneAxisEllipsoid earth,
                                    final String supportedNames,
                                    final DataProvidersManager dataProvidersManager,
                                    final TimeScale utc) {
        this(earth, supportedNames, dataProvidersManager, utc, TimeInterpolator.SIMPLE_LINEAR);
    }

    /**
     * Constructor that uses user defined supported names and data context.
     *
     * @param earth       earth body shape
     * @param supportedNames       regular expression that matches the names of the IONEX
     *                             files to be loaded. See {@link DataProvidersManager#feed(String,
     *                             DataLoader)}.
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc                  UTC time scale.
     * @param interpolator         interpolator to use
     * @since 14.0
     */
    public GlobalIonosphereMapModel(final OneAxisEllipsoid earth,
                                    final String supportedNames,
                                    final DataProvidersManager dataProvidersManager,
                                    final TimeScale utc,
                                    final TimeInterpolator interpolator) {
        super(earth);
        this.utc          = utc;
        this.tecMap       = new TimeSpanMap<>(null);
        this.names        = "";
        this.interpolator = interpolator;

        // Read files
        dataProvidersManager.feed(supportedNames, new Parser());

    }

    /**
     * Constructor that uses user defined data sources.
     *
     * @param earth earth body shape
     * @param utc            UTC time scale.
     * @param ionex          sources for the IONEX files
     * @since 14.0
     * @deprecated as of 13.1.1, replaced by
     * {@link #GlobalIonosphereMapModel(OneAxisEllipsoid, TimeScale, TimeInterpolator, DataSource...)}
     */
    @Deprecated
    public GlobalIonosphereMapModel(final OneAxisEllipsoid earth, final TimeScale utc, final DataSource... ionex) {
        this(earth, utc, TimeInterpolator.SIMPLE_LINEAR, ionex);
    }

    /**
     * Constructor that uses user defined data sources.
     *
     * @param earth earth body shape
     * @param utc            UTC time scale.
     * @param interpolator   interpolator to use
     * @param ionex          sources for the IONEX files
     * @since 14.0
     */
    public GlobalIonosphereMapModel(final OneAxisEllipsoid earth,
                                    final TimeScale utc,
                                    final TimeInterpolator interpolator,
                                    final DataSource... ionex) {
        super(earth);
        try {
            this.utc            = utc;
            this.tecMap         = new TimeSpanMap<>(null);
            this.names          = "";
            this.interpolator   = interpolator;
            final Parser parser = new Parser();
            for (final DataSource source : ionex) {
                try (InputStream is  = source.getOpener().openStreamOnce();
                    BufferedInputStream bis = new BufferedInputStream(is)) {
                    parser.loadData(bis, source.getName());
                }
            }
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /**
     * Get the time interpolator used.
     * @return time interpolator used
     * @since 13.1.1
     */
    public TimeInterpolator getInterpolator() {
        return interpolator;
    }

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite traversing ionosphere single layer at some pierce point.
     * <p>
     * The path delay can be computed for any elevation angle.
     * </p>
     * @param date current date
     * @param piercePoint ionospheric pierce point
     * @param elevation elevation of the satellite from receiver point in radians
     * @param frequency frequency of the signal in Hz
     * @return the path delay due to the ionosphere in m
     */
    private double pathDelayAtIPP(final AbsoluteDate date, final GeodeticPoint piercePoint,
                                  final double elevation, final double frequency) {
        // TEC in TECUnits
        final TECMapPair pair = getPairAtDate(date);
        final double tec = interpolator.interpolateTEC(pair.first, pair.second, date, piercePoint);
        // Square of the frequency
        final double freq2 = frequency * frequency;
        // "Slant" Total Electron Content
        final double stec;
        // Check if a mapping factor is needed
        if (pair.mapping) {
            stec = tec;
        } else {
            // Mapping factor
            final double fz = mappingFunction(elevation, pair.r0, pair.h);
            stec = tec * fz;
        }
        // Delay computation
        final double alpha  = 40.3e16 / freq2;
        return alpha * stec;
    }

    @Override
    public double pathDelay(final Vector3D localP1, final Vector3D localP2,
                            final TopocentricFrame baseFrame, final AbsoluteDate receptionDate,
                            final double frequency, final double[] parameters) {

        final Frame           bodyFrame       = getEarth().getFrame();
        final StaticTransform baseFrameToBody = baseFrame.getStaticTransformTo(bodyFrame, receptionDate);
        final double          baseAlt         = baseFrame.getPoint().getAltitude();

        // Lambda function for calculating path delay for each side of the link
        final DelayCalculator calculateDelay = position -> {

            // Position of object in Earth frame
            final Vector3D bodyP1 = baseFrameToBody.transformPosition(position);

            // Elevation of position w.r.t the base frame
            final double elevation = position.getDelta();

            if (checkIfPathIsValid(position, localP1, localP2, baseAlt)) {

                // Normalized Line Of Sight in body frame
                final Vector3D los = bodyP1.subtract(baseFrame.getCartesianPoint()).normalize();
                try {

                    // ionosphere Pierce Point
                    final GeodeticPoint ipp = piercePoint(receptionDate, baseFrame.getCartesianPoint(), los,
                                                        baseFrame.getParentShape());

                    // delay
                    return pathDelayAtIPP(receptionDate, ipp, elevation, frequency);

                } catch (final OrekitException oe) {
                    if (oe.getSpecifier() == OrekitMessages.LINE_NEVER_CROSSES_ALTITUDE ||
                        oe.getSpecifier() == LocalizedCoreFormats.CONVERGENCE_FAILED) {
                        // we don't cross ionosphere layer (or we just skim it)
                        return 0.0;
                    } else {
                        // this is an unexpected error
                        throw oe;
                    }
                }
            }

            return 0.0;

        };

        return calculateDelay.apply(localP1) + calculateDelay.apply(localP2);
    }

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite traversing ionosphere single layer at some pierce point.
     * <p>
     * The path delay can be computed for any elevation angle.
     * </p>
     * @param <T> type of the elements
     * @param date current date
     * @param piercePoint ionospheric pierce point
     * @param elevation elevation of the satellite from receiver point in radians
     * @param frequency frequency of the signal in Hz
     * @return the path delay due to the ionosphere in m
     */
    private <T extends CalculusFieldElement<T>> T pathDelayAtIPP(final FieldAbsoluteDate<T> date,
                                                                 final FieldGeodeticPoint<T> piercePoint,
                                                                 final T elevation, final double frequency) {
        // TEC in TECUnits
        final TECMapPair pair = getPairAtDate(date.toAbsoluteDate());
        final T tec = interpolator.interpolateTEC(pair.first, pair.second, date, piercePoint);
        // Square of the frequency
        final double freq2 = frequency * frequency;
        // "Slant" Total Electron Content
        final T stec;
        // Check if a mapping factor is needed
        if (pair.mapping) {
            stec = tec;
        } else {
            // Mapping factor
            final T fz = mappingFunction(elevation, pair.r0, pair.h);
            stec = tec.multiply(fz);
        }
        // Delay computation
        final double alpha  = 40.3e16 / freq2;
        return stec.multiply(alpha);
    }

    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final FieldVector3D<T> localP1, final FieldVector3D<T> localP2,
                                                           final TopocentricFrame baseFrame, final FieldAbsoluteDate<T> receptionDate,
                                                           final double frequency, final T[] parameters) {

        final Frame                   bodyFrame       = getEarth().getFrame();
        final FieldStaticTransform<T> baseFrameToBody = baseFrame.getStaticTransformTo(bodyFrame, receptionDate);
        final double                  baseAlt         = baseFrame.getPoint().getAltitude();

        // Lambda function for calculating path delay for each side of the link
        final FieldDelayCalculator<T> calculateFieldDelay = position -> {

            // Position of object in earth body frame
            final FieldVector3D<T> satPoint  = baseFrameToBody.transformPosition(position);

            // Elevation of object in radians w.r.t. minimum altitude point
            final T elevation = position.getDelta();

            if (checkIfPathIsValid(position, localP1, localP2, baseAlt)) {
                // Normalized Line Of Sight in body frame
                final FieldVector3D<T> los = satPoint.subtract(baseFrame.getCartesianPoint()).normalize();
                try {

                    // ionosphere Pierce Point
                    final FieldGeodeticPoint<T> ipp = piercePoint(receptionDate, baseFrame.getCartesianPoint(),
                                                                los, baseFrame.getParentShape());

                    // delay
                    return pathDelayAtIPP(receptionDate, ipp, elevation, frequency);

                } catch (final OrekitException oe) {
                    if (oe.getSpecifier() == OrekitMessages.LINE_NEVER_CROSSES_ALTITUDE ||
                        oe.getSpecifier() == LocalizedCoreFormats.CONVERGENCE_FAILED) {
                        // we don't cross ionosphere layer (or we just skim it)
                        return elevation.getField().getZero();
                    } else {
                        // this is an unexpected error
                        throw oe;
                    }
                }
            }

            return elevation.getField().getZero();
        };

        return calculateFieldDelay.apply(localP1).add( calculateFieldDelay.apply(localP2) );
    }

    /** Get the pair valid at date.
     * @param date computation date
     * @return pair valid at date
     * @since 12.0
     */
    private TECMapPair getPairAtDate(final AbsoluteDate date) {
        final TECMapPair pair = tecMap.get(date);
        if (pair == null) {
            final TimeSpanMap.Transition<TECMapPair> lastTransition = tecMap.getLastTransition();
            if (lastTransition != null && lastTransition.getDate().equals(date)) {
                // we consider the transition date is in the validity range of the last span
                return lastTransition.getBefore();
            }
            throw new OrekitException(OrekitMessages.NO_TEC_DATA_IN_FILES_FOR_DATE,
                                      names, date);
        }
        return pair;
    }

    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /**
     * Computes the ionospheric mapping function.
     * @param elevation the elevation of the satellite in radians
     * @param r0 mean Earth radius
     * @param h height of the ionospheric layer
     * @return the mapping function
     */
    private double mappingFunction(final double elevation,
                                   final double r0, final double h) {
        // Calculate the zenith angle from the elevation
        final double z = FastMath.abs(0.5 * FastMath.PI - elevation);
        // Distance ratio
        final double ratio = r0 / (r0 + h);
        // Mapping function
        final double coef = FastMath.sin(z) * ratio;
        return 1.0 / FastMath.sqrt(1.0 - coef * coef);
    }

    /**
     * Computes the ionospheric mapping function.
     * @param <T> type of the elements
     * @param elevation the elevation of the satellite in radians
     * @param r0 mean Earth radius
     * @param h height of the ionospheric layer
     * @return the mapping function
     */
    private <T extends CalculusFieldElement<T>> T mappingFunction(final T elevation,
                                                                  final double r0, final double h) {
        // Calculate the zenith angle from the elevation
        final T z = FastMath.abs(elevation.getPi().multiply(0.5).subtract(elevation));
        // Distance ratio
        final double ratio = r0 / (r0 + h);
        // Mapping function
        final T coef = FastMath.sin(z).multiply(ratio);
        return FastMath.sqrt(coef.multiply(coef).negate().add(1.0)).reciprocal();
    }

    /** Compute Ionospheric Pierce Point.
     * <p>
     * The pierce point is computed assuming a spherical ionospheric shell above mean Earth radius.
     * </p>
     * @param date computation date
     * @param recPoint point at receiver station in body frame
     * @param los normalized line of sight in body frame
     * @param bodyShape shape of the body
     * @return pierce point, or null if recPoint is above ionosphere single layer
     * @since 12.0
     */
    private GeodeticPoint piercePoint(final AbsoluteDate date,
                                      final Vector3D recPoint, final Vector3D los,
                                      final BodyShape bodyShape) {
        if (bodyShape instanceof OneAxisEllipsoid) {

            // pierce point of ellipsoidal shape
            final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) bodyShape;
            final Line line = new Line(recPoint, new Vector3D(1.0, recPoint, 1.0e6, los), 1.0e-12);
            final double h = getPairAtDate(date).h;
            final Vector3D ipp = ellipsoid.pointAtAltitude(line, h, recPoint, bodyShape.getBodyFrame(), date);

            // convert to geocentric (NOT geodetic) coordinates
            return new GeodeticPoint(ipp.getDelta(), ipp.getAlpha(), h);

        } else {
            throw new OrekitException(OrekitMessages.BODY_SHAPE_MUST_BE_A_ONE_AXIS_ELLIPSOID);
        }
    }

    /** Compute Ionospheric Pierce Point.
     * <p>
     * The pierce point is computed assuming a spherical ionospheric shell above mean Earth radius.
     * </p>
     * @param <T> type of th field elements
     * @param date computation date
     * @param recPoint point at receiver station in body frame
     * @param los normalized line of sight in body frame
     * @param bodyShape shape of the body
     * @return pierce point, or null if recPoint is above ionosphere single layer
     * @since 13.1.1
     */
    private <T extends CalculusFieldElement<T>> FieldGeodeticPoint<T> piercePoint(final FieldAbsoluteDate<T> date,
                                                                                  final Vector3D recPoint,
                                                                                  final FieldVector3D<T> los,
                                                                                  final BodyShape bodyShape) {
        if (bodyShape instanceof OneAxisEllipsoid) {

            // pierce point of ellipsoidal shape
            final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) bodyShape;
            final FieldVector3D<T> recPointF = new FieldVector3D<>(date.getField(), recPoint);
            final FieldLine<T> line = new FieldLine<>(recPointF,
                                                      new FieldVector3D<>(1.0, recPointF, 1.0e6, los),
                                                      1.0e-12);
            final T h = date.getField().getZero().newInstance(getPairAtDate(date.toAbsoluteDate()).h);
            final FieldVector3D<T> ipp = ellipsoid.pointAtAltitude(line, h, recPointF, bodyShape.getBodyFrame(), date);

            // convert to geocentric (NOT geodetic) coordinates
            return new FieldGeodeticPoint<>(ipp.getDelta(), ipp.getAlpha(), h);

        } else {
            throw new OrekitException(OrekitMessages.BODY_SHAPE_MUST_BE_A_ONE_AXIS_ELLIPSOID);
        }
    }

    /** Parser for IONEX files. */
    private class Parser implements DataLoader {

        /** String for the end of a TEC map. */
        private static final String END = "END OF TEC MAP";

        /** String for the epoch of a TEC map. */
        private static final String EPOCH = "EPOCH OF CURRENT MAP";

        /** Index of label in data lines. */
        private static final int LABEL_START = 60;

        /** Kilometers to meters conversion factor. */
        private static final double KM_TO_M = 1000.0;

        /** Header of the IONEX file. */
        private IONEXHeader header;

        @Override
        public boolean stillAcceptsData() {
            return true;
        }

        @Override
        public void loadData(final InputStream input, final String name)
            throws IOException {

            final List<TECMap> maps = new ArrayList<>();

            // Open stream and parse data
            int   lineNumber = 0;
            String line      = null;
            try (InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
                 BufferedReader    br  = new BufferedReader(isr)) {

                // Placeholders for parsed data
                int               nbOfMaps    = 1;
                int               exponent    = -1;
                double            baseRadius  = Double.NaN;
                double            hIon        = Double.NaN;
                boolean           mappingF    = false;
                boolean           inTEC       = false;
                double[]          latitudes   = null;
                double[]          longitudes  = null;
                AbsoluteDate      firstEpoch  = null;
                AbsoluteDate      lastEpoch   = null;
                AbsoluteDate      epoch       = firstEpoch;
                ArrayList<Double> values      = new ArrayList<>();

                for (line = br.readLine(); line != null; line = br.readLine()) {
                    ++lineNumber;
                    if (line.length() > LABEL_START) {
                        switch (line.substring(LABEL_START).trim()) {
                            case "EPOCH OF FIRST MAP" :
                                firstEpoch = parseDate(line);
                                break;
                            case "EPOCH OF LAST MAP" :
                                lastEpoch = parseDate(line);
                                break;
                            case "INTERVAL" :
                                // ignored;
                                break;
                            case "# OF MAPS IN FILE" :
                                nbOfMaps = parseInt(line, 2, 4);
                                break;
                            case "BASE RADIUS" :
                                // Value is in kilometers
                                baseRadius = parseDouble(line, 2, 6) * KM_TO_M;
                                break;
                            case "MAPPING FUNCTION" :
                                mappingF = !parseString(line, 2, 4).equals("NONE");
                                break;
                            case "EXPONENT" :
                                exponent = parseInt(line, 4, 2);
                                break;
                            case "HGT1 / HGT2 / DHGT" :
                                if (parseDouble(line, 17, 3) == 0.0) {
                                    // Value is in kilometers
                                    hIon = parseDouble(line, 3, 5) * KM_TO_M;
                                }
                                break;
                            case "LAT1 / LAT2 / DLAT" :
                                latitudes = parseCoordinate(line);
                                break;
                            case "LON1 / LON2 / DLON" :
                                longitudes = parseCoordinate(line);
                                break;
                            case "END OF HEADER" :
                                // Check that latitude and longitude boundaries were found
                                if (latitudes == null || longitudes == null) {
                                    throw new OrekitException(OrekitMessages.NO_LATITUDE_LONGITUDE_BOUNDARIES_IN_IONEX_HEADER, name);
                                }
                                // Check that first and last epochs were found
                                if (firstEpoch == null || lastEpoch == null) {
                                    throw new OrekitException(OrekitMessages.NO_EPOCH_IN_IONEX_HEADER, name);
                                }
                                // At the end of the header, we build the IONEXHeader object
                                header = new IONEXHeader(nbOfMaps, baseRadius, hIon, mappingF);
                                break;
                            case "START OF TEC MAP" :
                                inTEC = true;
                                break;
                            case END :
                                final BilinearInterpolatingFunction tec = interpolateTEC(values, exponent, latitudes, longitudes);
                                final TECMap map = new TECMap(epoch, tec);
                                maps.add(map);
                                // Reset parameters
                                inTEC  = false;
                                values = new ArrayList<>();
                                epoch  = null;
                                break;
                            default :
                                if (inTEC) {
                                    // Date
                                    if (line.endsWith(EPOCH)) {
                                        epoch = parseDate(line);
                                    }
                                    // Fill TEC values list
                                    if (!line.endsWith("LAT/LON1/LON2/DLON/H") &&
                                        !line.endsWith(END) &&
                                        !line.endsWith(EPOCH)) {
                                        for (int fieldStart = 0; fieldStart < line.length(); fieldStart += 5) {
                                            values.add((double) Integer.parseInt(line.substring(fieldStart, fieldStart + 5).trim()));
                                        }
                                    }
                                }
                                break;
                        }
                    } else {
                        if (inTEC) {
                            // Here, we are parsing the last line of TEC data for a given latitude
                            // The size of this line is lower than 60.
                            for (int fieldStart = 0; fieldStart < line.length(); fieldStart += 5) {
                                values.add((double) Integer.parseInt(line.substring(fieldStart, fieldStart + 5).trim()));
                            }
                        }
                    }

                }

                // Close the stream after reading
                input.close();

            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

            // TEC map
            if (maps.size() != header.getTECMapsNumer()) {
                throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_TEC_MAPS_IN_FILE,
                                          maps.size(), header.getTECMapsNumer());
            }
            TECMap previous = null;
            for (TECMap current : maps) {
                if (previous != null) {
                    tecMap.addValidBetween(new TECMapPair(previous, current,
                                                          header.getEarthRadius(), header.getHIon(), header.isMappingFunction()),
                                           previous.date, current.date);
                }
                previous = current;
            }

            names = names.isEmpty() ? name : (names + ", " + name);

        }

        /** Extract a string from a line.
         * @param line to parse
         * @param start start index of the string
         * @param length length of the string
         * @return parsed string
         */
        private String parseString(final String line, final int start, final int length) {
            return line.substring(start, FastMath.min(line.length(), start + length)).trim();
        }

        /** Extract an integer from a line.
         * @param line to parse
         * @param start start index of the integer
         * @param length length of the integer
         * @return parsed integer
         */
        private int parseInt(final String line, final int start, final int length) {
            return Integer.parseInt(parseString(line, start, length));
        }

        /** Extract a double from a line.
         * @param line to parse
         * @param start start index of the real
         * @param length length of the real
         * @return parsed real
         */
        private double parseDouble(final String line, final int start, final int length) {
            return Double.parseDouble(parseString(line, start, length));
        }

        /** Extract a date from a parsed line.
         * @param line to parse
         * @return an absolute date
         */
        private AbsoluteDate parseDate(final String line) {
            return new AbsoluteDate(parseInt(line, 0, 6),
                                    parseInt(line, 6, 6),
                                    parseInt(line, 12, 6),
                                    parseInt(line, 18, 6),
                                    parseInt(line, 24, 6),
                                    parseDouble(line, 30, 13),
                                    utc);
        }

        /** Build the coordinate array from a parsed line.
         * @param line to parse
         * @return an array of coordinates in radians
         */
        private double[] parseCoordinate(final String line) {
            final double a = parseDouble(line, 2, 6);
            final double b = parseDouble(line, 8, 6);
            final double c = parseDouble(line, 14, 6);
            final double[] coordinate = new double[((int) FastMath.abs((a - b) / c)) + 1];
            int i = 0;
            for (double cor = FastMath.min(a, b); cor <= FastMath.max(a, b); cor += FastMath.abs(c)) {
                coordinate[i] = FastMath.toRadians(cor);
                i++;
            }
            return coordinate;
        }

        /** Interpolate the TEC in latitude and longitude.
         * @param exponent exponent defining the unit of the values listed in the data blocks
         * @param values TEC values
         * @param latitudes array containing the different latitudes in radians
         * @param longitudes array containing the different latitudes in radians
         * @return the interpolating TEC functiopn in TECUnits
         */
        private BilinearInterpolatingFunction interpolateTEC(final ArrayList<Double> values, final double exponent,
                                                             final double[] latitudes, final double[] longitudes) {
            // Array dimensions
            final int dimLat = latitudes.length;
            final int dimLon = longitudes.length;

            // Build the array of TEC data
            final double factor = FastMath.pow(10.0, exponent);
            final double[][] fvalTEC = new double[dimLat][dimLon];
            int index = dimLon * dimLat;
            for (int x = 0; x < dimLat; x++) {
                for (int y = dimLon - 1; y >= 0; y--) {
                    index = index - 1;
                    fvalTEC[x][y] = values.get(index) * factor;
                }
            }

            // Build Bilinear Interpolation function
            return new BilinearInterpolatingFunction(latitudes, longitudes, fvalTEC);

        }

    }

    /**
     * Container for IONEX data.
     * <p>
     * The TEC contained in the map is previously interpolated
     * according to the latitude and the longitude given by the user.
     * </p>
     */
    private static class TECMap {

        /** Date of the TEC Map. */
        private final AbsoluteDate date;

        /** Interpolated TEC [TECUnits]. */
        private final BilinearInterpolatingFunction tec;

        /**
         * Constructor.
         * @param date date of the TEC map
         * @param tec interpolated tec
         */
        TECMap(final AbsoluteDate date, final BilinearInterpolatingFunction tec) {
            this.date = date;
            this.tec  = tec;
        }

    }

    /** Container for a consecutive pair of TEC maps.
     * @since 12.0
     */
    private static class TECMapPair {

        /** First snapshot. */
        private final TECMap first;

        /** Second snapshot. */
        private final TECMap second;

        /** Mean earth radius [m]. */
        private final double r0;

        /** Height of the ionospheric single layer [m]. */
        private final double h;

        /** Flag for mapping function computation. */
        private final boolean mapping;

        /** Simple constructor.
         * @param first first snapshot
         * @param second second snapshot
         * @param r0 mean Earth radius
         * @param h height of the ionospheric layer
         * @param mapping flag for mapping computation
         */
        TECMapPair(final TECMap first, final TECMap second,
                   final double r0, final double h, final boolean mapping) {
            this.first   = first;
            this.second  = second;
            this.r0      = r0;
            this.h       = h;
            this.mapping = mapping;
        }

    }

    /**
     * Interpolation model for TEC maps.
     * @author Luc Maisonobe
     * @since 13.1.1
     */
    public enum TimeInterpolator {

        /** Apply directly nearest (in time) TEC map.
         * <p>
         *   This corresponds to equation 1 in IONEX standard.
         * </p>
         */
        NEAREST_MAP {

            /** {@inheritDoc} */
            @Override
            double interpolateTEC(final TECMap first, final TECMap second,
                                  final AbsoluteDate date, final GeodeticPoint ipp) {

                // select the nearest map
                final double dt1      = FastMath.abs(date.durationFrom(first.date));
                final double dt2      = FastMath.abs(date.durationFrom(second.date));
                final TECMap selected = dt1 <= dt2 ? first : second;

                // apply the selected map
                return selected.tec.value(ipp.getLatitude(), ipp.getLongitude());

            }

            /** {@inheritDoc} */
            @Override
            <T extends CalculusFieldElement<T>> T interpolateTEC(final TECMap first, final TECMap second,
                                                                 final FieldAbsoluteDate<T> date,
                                                                 final FieldGeodeticPoint<T> ipp) {

                // select the nearest map
                final T dt1      = FastMath.abs(date.durationFrom(first.date));
                final T dt2      = FastMath.abs(date.durationFrom(second.date));
                final TECMap selected = dt1.getReal() <= dt2.getReal() ? first : second;

                // apply the selected map
                return selected.tec.value(ipp.getLatitude(), ipp.getLongitude());

            }

        },

        /** Use linear interpolation between consecutive TEC maps.
         * <p>
         *   This corresponds to equation 2 in IONEX standard.
         * </p>
         */
        SIMPLE_LINEAR {

            /** {@inheritDoc} */
            @Override
            double interpolateTEC(final TECMap first, final TECMap second,
                                  final AbsoluteDate date, final GeodeticPoint ipp) {

                // Get the TEC values at the two closest dates
                final double dt1  = date.durationFrom(first.date);
                final double tec1 = first.tec.value(ipp.getLatitude(), ipp.getLongitude());
                final double dt2  = date.durationFrom(second.date);
                final double tec2 = second.tec.value(ipp.getLatitude(), ipp.getLongitude());

                // Perform temporal interpolation
                return (dt1 * tec2 - dt2 * tec1) / (dt1 - dt2);

            }

            /** {@inheritDoc} */
            @Override
            <T extends CalculusFieldElement<T>> T interpolateTEC(final TECMap first, final TECMap second,
                                                                 final FieldAbsoluteDate<T> date,
                                                                 final FieldGeodeticPoint<T> ipp) {

                // Get the TEC values at the two closest dates
                final T dt1  = date.durationFrom(first.date);
                final T tec1 = first.tec.value(ipp.getLatitude(), ipp.getLongitude());
                final T dt2  = date.durationFrom(second.date);
                final T tec2 = second.tec.value(ipp.getLatitude(), ipp.getLongitude());

                // Perform temporal interpolation
                return dt1.multiply(tec2).subtract(dt2.multiply(tec1)).divide(dt1.subtract(dt2));

            }

        },

        /** Use linear interpolation between consecutive rotated maps (compensating for Earth rotation).
         * <p>
         *   This corresponds to equation 3 in IONEX standard and is the recommended interpolation method.
         * </p>
         */
        ROTATED_LINEAR {

            /** {@inheritDoc} */
            @Override
           double interpolateTEC(final TECMap first, final TECMap second,
                                  final AbsoluteDate date, final GeodeticPoint ipp) {

                // Get the TEC values at the two closest dates
                final double dt1  = date.durationFrom(first.date);
                final double dl1  = dt1 * Constants.WGS84_EARTH_ANGULAR_VELOCITY;
                final double tec1 = first.tec.value(ipp.getLatitude(),
                                                    MathUtils.normalizeAngle(dl1 + ipp.getLongitude(), 0.0));

                final double dt2  = date.durationFrom(second.date);
                final double dl2  = dt2 * Constants.WGS84_EARTH_ANGULAR_VELOCITY;
                final double tec2 = second.tec.value(ipp.getLatitude(),
                                                     MathUtils.normalizeAngle(dl2 + ipp.getLongitude(), 0.0));

                // Perform temporal interpolation
                return (dt1 * tec2 - dt2 * tec1) / (dt1 - dt2);

            }

            /** {@inheritDoc} */
            @Override
            <T extends CalculusFieldElement<T>> T interpolateTEC(final TECMap first, final TECMap second,
                                                                 final FieldAbsoluteDate<T> date,
                                                                 final FieldGeodeticPoint<T> ipp) {

                final T zero = date.getField().getZero();

                // Get the TEC values at the two closest dates
                final T dt1  = date.durationFrom(first.date);
                final T dl1  = dt1.multiply(Constants.WGS84_EARTH_ANGULAR_VELOCITY);
                final T tec1 = first.tec.value(ipp.getLatitude(),
                                               MathUtils.normalizeAngle(dl1.add(ipp.getLongitude()), zero));

                final T dt2  = date.durationFrom(second.date);
                final T dl2  = dt2.multiply(Constants.WGS84_EARTH_ANGULAR_VELOCITY);
                final T tec2 = second.tec.value(ipp.getLatitude(),
                                                MathUtils.normalizeAngle(dl2.add(ipp.getLongitude()), zero));

                // Perform temporal interpolation
                return dt1.multiply(tec2).subtract(dt2.multiply(tec1)).divide(dt1.subtract(dt2));

            }

        };

        /** Interpolate between two TEC maps.
         * @param first first map
         * @param second second map
         * @param date date
         * @param ipp Ionospheric Pierce Point
         * @return interpolated TEC
         */
        abstract double interpolateTEC(TECMap first, TECMap second,
                                       AbsoluteDate date, GeodeticPoint ipp);

        /** Interpolate between two TEC maps.
         * @param <T> type of the field elements
         * @param first first map
         * @param second second map
         * @param date date
         * @param ipp Ionospheric Pierce Point
         * @return interpolated TEC
         */
        abstract <T extends CalculusFieldElement<T>> T interpolateTEC(TECMap first, TECMap second,
                                                                      FieldAbsoluteDate<T> date,
                                                                      FieldGeodeticPoint<T> ipp);

    }

    /** Container for IONEX header. */
    private static class IONEXHeader {

        /** Number of maps contained in the IONEX file. */
        private final int nbOfMaps;

        /** Mean earth radius [m]. */
        private final double baseRadius;

        /** Height of the ionospheric single layer [m]. */
        private final double hIon;

        /** Flag for mapping function adopted for TEC determination. */
        private final boolean isMappingFunction;

        /**
         * Constructor.
         * @param nbOfMaps number of TEC maps contained in the file
         * @param baseRadius mean earth radius in meters
         * @param hIon height of the ionospheric single layer in meters
         * @param mappingFunction flag for mapping function adopted for TEC determination
         */
        IONEXHeader(final int nbOfMaps,
                    final double baseRadius, final double hIon,
                    final boolean mappingFunction) {
            this.nbOfMaps          = nbOfMaps;
            this.baseRadius        = baseRadius;
            this.hIon              = hIon;
            this.isMappingFunction = mappingFunction;
        }

        /**
         * Get the number of TEC maps contained in the file.
         * @return the number of TEC maps
         */
        public int getTECMapsNumer() {
            return nbOfMaps;
        }

        /**
         * Get the mean earth radius in meters.
         * @return the mean earth radius
         */
        public double getEarthRadius() {
            return baseRadius;
        }

        /**
         * Get the height of the ionospheric single layer in meters.
         * @return the height of the ionospheric single layer
         */
        public double getHIon() {
            return hIon;
        }

        /**
         * Get the mapping function flag.
         * @return false if mapping function computation is needed
         */
        public boolean isMappingFunction() {
            return isMappingFunction;
        }

    }

}
