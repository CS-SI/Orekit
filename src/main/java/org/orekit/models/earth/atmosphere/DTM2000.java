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
package org.orekit.models.earth.atmosphere;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.PVCoordinatesProvider;

/** This atmosphere model is the realization of the DTM-2000 model.
 * <p>
 * It is described in the paper: <br>
 *
 * <b>The DTM-2000 empirical thermosphere model with new data assimilation
 *  and constraints at lower boundary: accuracy and properties</b><br>
 *
 * <i>S. Bruinsma, G. Thuillier and F. Barlier</i> <br>
 *
 * Journal of Atmospheric and Solar-Terrestrial Physics 65 (2003) 1053–1070<br>
 *
 *</p>
 *<p>
 * This model provides dense output for altitudes beyond 120 km.
 *</p>
 *
 * <p>
 * The model needs geographical and time information to compute general values,
 * but also needs space weather data : mean and instantaneous solar flux and
 * geomagnetic indices.
 * </p>
 * <p>
 * Mean solar flux is (for the moment) represented by the F10.7 indices. Instantaneous
 * flux can be set to the mean value if the data is not available. Geomagnetic
 * activity is represented by the Kp indice, which goes from 1 (very low activity) to
 * 9 (high activity).
 *
 * <p>
 * All these data can be found on the <a href="https://www.noaa.gov/">
 * NOAA (National Oceanic and Atmospheric Administration) website.</a>
 * </p>
 *
 *
 * @author R. Biancale, S. Bruinsma: original fortran routine
 * @author Fabien Maussion (java translation)
 */
public class DTM2000 implements Atmosphere {

    /** Serializable UID. */
    private static final long serialVersionUID = 20170705L;

    // Constants :

    /** Number of parameters. */
    private static final int NLATM = 96;

    /** Thermal diffusion coefficient. */
    private static final double[] ALEFA = {
        0, -0.40, -0.38, 0, 0, 0, 0
    };

    /** Atomic mass  H, He, O, N2, O2, N. */
    private static final double[] MA = {
        0, 1, 4, 16, 28, 32, 14
    };

    /** Atomic mass  H, He, O, N2, O2, N. */
    private static final double[] VMA = {
        0, 1.6606e-24, 6.6423e-24, 26.569e-24, 46.4958e-24, 53.1381e-24, 23.2479e-24
    };

    /** Polar Earth radius. */
    private static final double RE = 6356.77;

    /** Reference altitude. */
    private static final double ZLB0 = 120.0;

    /** Cosine of the latitude of the magnetic pole (79N, 71W). */
    private static final double CPMG = .19081;

    /** Sine of the latitude of the magnetic pole (79N, 71W). */
    private static final double SPMG = .98163;

    /** Longitude (in radians) of the magnetic pole (79N, 71W). */
    private static final double XLMG = -1.2392;

    /** Gravity acceleration at 120 km altitude. */
    private static final double GSURF = 980.665;

    /** Universal gas constant. */
    private static final double RGAS = 831.4;

    /** 2 * π / 365. */
    private static final double ROT = 0.017214206;

    /** 2 * rot. */
    private static final double ROT2 = 0.034428412;

    /** Resources text file. */
    private static final String DTM2000 = "/assets/org/orekit/dtm_2000.txt";

    // CHECKSTYLE: stop JavadocVariable check

    /** Elements coefficients. */
    private static double[] tt   = null;
    private static double[] h    = null;
    private static double[] he   = null;
    private static double[] o    = null;
    private static double[] az2  = null;
    private static double[] o2   = null;
    private static double[] az   = null;
    private static double[] t0   = null;
    private static double[] tp   = null;

    /** Sun position. */
    private PVCoordinatesProvider sun;

    /** External data container. */
    private DTM2000InputParameters inputParams;

    /** Earth body shape. */
    private BodyShape earth;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Simple constructor for independent computation.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param parameters the solar and magnetic activity data
     * @param sun the sun position
     * @param earth the earth body shape
     * @see #DTM2000(DTM2000InputParameters, PVCoordinatesProvider, BodyShape, TimeScale)
     */
    @DefaultDataContext
    public DTM2000(final DTM2000InputParameters parameters,
                   final PVCoordinatesProvider sun, final BodyShape earth) {
        this(parameters, sun, earth, DataContext.getDefault().getTimeScales().getUTC());
    }

    /** Simple constructor for independent computation.
     * @param parameters the solar and magnetic activity data
     * @param sun the sun position
     * @param earth the earth body shape
     * @param utc UTC time scale.
     * @since 10.1
     */
    public DTM2000(final DTM2000InputParameters parameters,
                   final PVCoordinatesProvider sun,
                   final BodyShape earth,
                   final TimeScale utc) {

        synchronized (DTM2000.class) {
            // lazy reading of model coefficients
            if (tt == null) {
                readcoefficients();
            }
        }

        this.earth = earth;
        this.sun = sun;
        this.inputParams = parameters;

        this.utc = utc;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return earth.getBodyFrame();
    }

    /** Get the local density with initial entries.
     * @param day day of year
     * @param alti altitude in meters
     * @param lon local longitude (rad)
     * @param lat local latitude (rad)
     * @param hl local solar time in rad (O hr = 0 rad)
     * @param f instantaneous solar flux (F10.7)
     * @param fbar mean solar flux (F10.7)
     * @param akp3 3 hrs geomagnetic activity index (1-9)
     * @param akp24 Mean of last 24 hrs geomagnetic activity index (1-9)
     * @return the local density (kg/m³)
     */
    public double getDensity(final int day,
                             final double alti, final double lon, final double lat,
                             final double hl, final double f, final double fbar,
                             final double akp3, final double akp24) {
        final double threshold = 120000;
        if (alti < threshold) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD,
                                      alti, threshold);
        }
        final Computation result = new Computation(day, alti / 1000, lon, lat, hl,
                                                   new double[] {
                                                       0, f, 0
                                                   }, new double[] {
                                                       0, fbar, 0
                                                   }, new double[] {
                                                       0, akp3, 0, akp24, 0
                                                   });
        return result.ro * 1000;
    }

    /** Get the local density with initial entries.
     * @param day day of year
     * @param alti altitude in meters
     * @param lon local longitude (rad)
     * @param lat local latitude (rad)
     * @param hl local solar time in rad (O hr = 0 rad)
     * @param f instantaneous solar flux (F10.7)
     * @param fbar mean solar flux (F10.7)
     * @param akp3 3 hrs geomagnetic activity index (1-9)
     * @param akp24 Mean of last 24 hrs geomagnetic activity index (1-9)
     * @param <T> type of the field elements
     * @return the local density (kg/m³)
          * @since 9.0
     */
    public <T extends CalculusFieldElement<T>> T getDensity(final int day,
                                                        final T alti, final T lon, final T lat,
                                                        final T hl, final double f, final double fbar,
                                                        final double akp3, final double akp24) {
        final double threshold = 120000;
        if (alti.getReal() < threshold) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD,
                                      alti, threshold);
        }
        final FieldComputation<T> result = new FieldComputation<>(day, alti.divide(1000), lon, lat, hl,
                                                                  new double[] {
                                                                      0, f, 0
                                                                  }, new double[] {
                                                                      0, fbar, 0
                                                                  }, new double[] {
                                                                      0, akp3, 0, akp24, 0
                                                                  });
        return result.ro.multiply(1000);
    }

    /** Store the DTM model elements coefficients in internal arrays.
     */
    private static void readcoefficients() {

        final int size = NLATM + 1;
        tt   = new double[size];
        h    = new double[size];
        he   = new double[size];
        o    = new double[size];
        az2  = new double[size];
        o2   = new double[size];
        az   = new double[size];
        t0   = new double[size];
        tp   = new double[size];

        try (InputStream in = checkNull(DTM2000.class.getResourceAsStream(DTM2000));
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            r.readLine();
            r.readLine();
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                final int num = Integer.parseInt(line.substring(0, 4).replace(' ', '0'));
                line = line.substring(4);
                tt[num] = Double.parseDouble(line.substring(0, 13).replace(' ', '0'));
                line = line.substring(13 + 9);
                h[num] = Double.parseDouble(line.substring(0, 13).replace(' ', '0'));
                line = line.substring(13 + 9);
                he[num] = Double.parseDouble(line.substring(0, 13).replace(' ', '0'));
                line = line.substring(13 + 9);
                o[num] = Double.parseDouble(line.substring(0, 13).replace(' ', '0'));
                line = line.substring(13 + 9);
                az2[num] = Double.parseDouble(line.substring(0, 13).replace(' ', '0'));
                line = line.substring(13 + 9);
                o2[num] = Double.parseDouble(line.substring(0, 13).replace(' ', '0'));
                line = line.substring(13 + 9);
                az[num] = Double.parseDouble(line.substring(0, 13).replace(' ', '0'));
                line = line.substring(13 + 9);
                t0[num] = Double.parseDouble(line.substring(0, 13).replace(' ', '0'));
                line = line.substring(13 + 9);
                tp[num] = Double.parseDouble(line.substring(0, 13).replace(' ', '0'));
            }
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Get the local density.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return local density (kg/m³)
     */
    public double getDensity(final AbsoluteDate date, final Vector3D position,
                             final Frame frame) {

        // check if data are available :
        if (date.compareTo(inputParams.getMaxDate()) > 0 ||
            date.compareTo(inputParams.getMinDate()) < 0) {
            throw new OrekitException(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE,
                                      date, inputParams.getMinDate(), inputParams.getMaxDate());
        }

        // compute day number in current year
        final int day = date.getComponents(utc).getDate().getDayOfYear();
        //position in ECEF so we only have to do the transform once
        final Frame ecef = earth.getBodyFrame();
        final Vector3D pEcef = frame.getStaticTransformTo(ecef, date)
                .transformPosition(position);
        // compute geodetic position
        final GeodeticPoint inBody = earth.transform(pEcef, ecef, date);
        final double alti = inBody.getAltitude();
        final double lon = inBody.getLongitude();
        final double lat = inBody.getLatitude();

        // compute local solar time
        final Vector3D sunPos = sun.getPosition(date, ecef);
        final double hl = FastMath.PI + FastMath.atan2(
                sunPos.getX() * pEcef.getY() - sunPos.getY() * pEcef.getX(),
                sunPos.getX() * pEcef.getX() + sunPos.getY() * pEcef.getY());

        // get current solar activity data and compute
        return getDensity(day, alti, lon, lat, hl, inputParams.getInstantFlux(date),
                          inputParams.getMeanFlux(date), inputParams.getThreeHourlyKP(date),
                          inputParams.get24HoursKp(date));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T
        getDensity(final FieldAbsoluteDate<T> date, final FieldVector3D<T> position,
                   final Frame frame) {
        // check if data are available :
        final AbsoluteDate dateD = date.toAbsoluteDate();
        if (dateD.compareTo(inputParams.getMaxDate()) > 0 ||
            dateD.compareTo(inputParams.getMinDate()) < 0) {
            throw new OrekitException(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE,
                                      dateD, inputParams.getMinDate(), inputParams.getMaxDate());
        }

        // compute day number in current year
        final int day = date.getComponents(utc).getDate().getDayOfYear();
        // position in ECEF so we only have to do the transform once
        final Frame ecef = earth.getBodyFrame();
        final FieldVector3D<T> pEcef = frame.getStaticTransformTo(ecef, date).transformPosition(position);
        // compute geodetic position
        final FieldGeodeticPoint<T> inBody = earth.transform(pEcef, ecef, date);
        final T alti = inBody.getAltitude();
        final T lon = inBody.getLongitude();
        final T lat = inBody.getLatitude();

        // compute local solar time
        final Vector3D sunPos = sun.getPosition(dateD, ecef);
        final T y  = pEcef.getY().multiply(sunPos.getX()).subtract(pEcef.getX().multiply(sunPos.getY()));
        final T x  = pEcef.getX().multiply(sunPos.getX()).add(pEcef.getY().multiply(sunPos.getY()));
        final T hl = y.atan2(x).add(y.getPi());

        // get current solar activity data and compute
        return getDensity(day, alti, lon, lat, hl, inputParams.getInstantFlux(dateD),
                          inputParams.getMeanFlux(dateD), inputParams.getThreeHourlyKP(dateD),
                          inputParams.get24HoursKp(dateD));
    }

    /**
     * Helper method to check for null resources. Throws an exception if {@code
     * stream} is null.
     *
     * @param stream loaded from the class resources.
     * @return {@code stream}.
     */
    private static InputStream checkNull(final InputStream stream) {
        if (stream == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_RESOURCE, DTM2000);
        }
        return stream;
    }

    /** Local holder for intermediate results ensuring the model is reentrant. */
    private static class Computation {

        /** Number of days in current year. */
        private final int day;

        /** Instant solar flux. f[1] = instantaneous flux; f[2] = 0. (not used). */
        private final double[] f;

        /** Mean solar flux. fbar[1] = mean flux; fbar[2] = 0. (not used). */
        private final double[] fbar;

        /** Kp coefficients.
         * <ul>
         *   <li>akp[1] = 3-hourly kp</li>
         *   <li>akp[2] = 0 (not used)</li>
         *   <li>akp[3] = mean kp of last 24 hours</li>
         *   <li>akp[4] = 0 (not used)</li>
         * </ul>
         */
        private final double[] akp;

        /** Cosine of the longitude. */
        private final double clfl;

        /** Sine of the longitude. */
        private final double slfl;

        /** Total density (g/cm3). */
        private final double ro;

        // CHECKSTYLE: stop JavadocVariable check

        /** Legendre coefficients. */
        private final double p10;
        private final double p20;
        private final double p30;
        private final double p40;
        private final double p50;
        private final double p60;
        private final double p11;
        private final double p21;
        private final double p31;
        private final double p41;
        private final double p51;
        private final double p22;
        private final double p32;
        private final double p42;
        private final double p52;
        private final double p62;
        private final double p33;
        private final double p10mg;
        private final double p20mg;
        private final double p40mg;

        /** Local time intermediate values. */
        private final double hl0;
        private final double ch;
        private final double sh;
        private final double c2h;
        private final double s2h;
        private final double c3h;
        private final double s3h;

        /** Simple constructor.
         * @param day day of year
         * @param altiKM altitude <em>in kilometers</em>
         * @param lon local longitude (rad)
         * @param lat local latitude (rad)
         * @param hl local solar time in rad (O hr = 0 rad)
         * @param f instantaneous solar flux (F10.7)
         * @param fbar mean solar flux (F10.7)
         * @param akp geomagnetic activity index
         */
        Computation(final int day,
                    final double altiKM, final double lon, final double lat,
                    final double hl, final double[] f, final double[] fbar,
                    final double[] akp) {

            this.day  = day;
            this.f    = f;
            this.fbar = fbar;
            this.akp  = akp;

            // Sine and cosine of local latitude and longitude
            final SinCos scLat = FastMath.sinCos(lat);
            final SinCos scLon = FastMath.sinCos(lon);

            // compute Legendre polynomials wrt geographic pole
            final double c = scLat.sin();
            final double c2 = c * c;
            final double c4 = c2 * c2;
            final double s = scLat.cos();
            final double s2 = s * s;
            p10 = c;
            p20 = 1.5 * c2 - 0.5;
            p30 = c * (2.5 * c2 - 1.5);
            p40 = 4.375 * c4 - 3.75 * c2 + 0.375;
            p50 = c * (7.875 * c4 - 8.75 * c2 + 1.875);
            p60 = (5.5 * c * p50 - 2.5 * p40) / 3.0;
            p11 = s;
            p21 = 3.0 * c * s;
            p31 = s * (7.5 * c2 - 1.5);
            p41 = c * s * (17.5 * c2 - 7.5);
            p51 = s * (39.375 * c4 - 26.25 * c2 + 1.875);
            p22 = 3.0 * s2;
            p32 = 15.0 * c * s2;
            p42 = s2 * (52.5 * c2 - 7.5);
            p52 = 3.0 * c * p42 - 2.0 * p32;
            p62 = 2.75 * c * p52 - 1.75 * p42;
            p33 = 15.0 * s * s2;

            // compute Legendre polynomials wrt magnetic pole (79N, 71W)
            final double clmlmg = FastMath.cos(lon - XLMG);
            final double cmg  = s * CPMG * clmlmg + c * SPMG;
            final double cmg2 = cmg * cmg;
            final double cmg4 = cmg2 * cmg2;
            p10mg = cmg;
            p20mg = 1.5 * cmg2 - 0.5;
            p40mg = 4.375 * cmg4 - 3.75 * cmg2 + 0.375;

            clfl = scLon.cos();
            slfl = scLon.sin();

            // local time
            hl0 = hl;
            final SinCos scHlo = FastMath.sinCos(hl0);
            ch  = scHlo.cos();
            sh  = scHlo.sin();
            c2h = ch * ch - sh * sh;
            s2h = 2.0 * ch * sh;
            c3h = c2h * ch - s2h * sh;
            s3h = s2h * ch + c2h * sh;

            final double zlb = ZLB0; // + dzlb ??

            final double[] dtt  = new double[tt.length];
            final double[] dh   = new double[tt.length];
            final double[] dhe  = new double[tt.length];
            final double[] dox  = new double[tt.length];
            final double[] daz2 = new double[tt.length];
            final double[] do2  = new double[tt.length];
            final double[] daz  = new double[tt.length];
            final double[] dt0  = new double[tt.length];
            final double[] dtp  = new double[tt.length];

            Arrays.fill(dtt,  Double.NaN);
            Arrays.fill(dh,   Double.NaN);
            Arrays.fill(dhe,  Double.NaN);
            Arrays.fill(dox,  Double.NaN);
            Arrays.fill(daz2, Double.NaN);
            Arrays.fill(do2,  Double.NaN);
            Arrays.fill(daz,  Double.NaN);
            Arrays.fill(dt0,  Double.NaN);
            Arrays.fill(dtp,  Double.NaN);

            //  compute function g(l) / tinf, t120, tp120
            int kleq = 1;
            final double gdelt = gFunction(tt, dtt, 1, kleq);
            dtt[1] = 1.0 + gdelt;
            final double tinf   = tt[1] * dtt[1];

            kleq = 0; // equinox

            if (day < 59 || day > 284) {
                kleq = -1; // north winter
            }
            if (day > 99 && day < 244) {
                kleq = 1; // north summer
            }

            final double gdelt0 =  gFunction(t0, dt0, 0, kleq);
            dt0[1] = (t0[1] + gdelt0) / t0[1];
            final double t120 = t0[1] + gdelt0;
            final double gdeltp = gFunction(tp, dtp, 0, kleq);
            dtp[1] = (tp[1] + gdeltp) / tp[1];
            final double tp120 = tp[1] + gdeltp;

            // compute n(z) concentrations: H, He, O, N2, O2, N
            final double sigma   = tp120 / (tinf - t120);
            final double dzeta   = (RE + zlb) / (RE + altiKM);
            final double zeta    = (altiKM - zlb) * dzeta;
            final double sigzeta = sigma * zeta;
            final double expsz   = FastMath.exp(-sigzeta);
            final double tz = tinf - (tinf - t120) * expsz;

            final double[] dbase = new double[7];

            kleq = 1;

            final double gdelh = gFunction(h, dh, 0, kleq);
            dh[1] = FastMath.exp(gdelh);
            dbase[1] = h[1] * dh[1];

            final double gdelhe = gFunction(he, dhe, 0, kleq);
            dhe[1] = FastMath.exp(gdelhe);
            dbase[2] = he[1] * dhe[1];

            final double gdelo = gFunction(o, dox, 1, kleq);
            dox[1] = FastMath.exp(gdelo);
            dbase[3] = o[1] * dox[1];

            final double gdelaz2 = gFunction(az2, daz2, 1, kleq);
            daz2[1] = FastMath.exp(gdelaz2);
            dbase[4] = az2[1] * daz2[1];

            final double gdelo2 = gFunction(o2, do2, 1, kleq);
            do2[1] = FastMath.exp(gdelo2);
            dbase[5] = o2[1] * do2[1];

            final double gdelaz = gFunction(az, daz, 1, kleq);
            daz[1] = FastMath.exp(gdelaz);
            dbase[6] = az[1] * daz[1];

            final double zlbre  = 1.0 + zlb / RE;
            final double glb    = (GSURF / (zlbre * zlbre)) / (sigma * RGAS * tinf);
            final double t120tz = t120 / tz;

            // Partial densities in (g/cm3).
            // d(1) = hydrogen
            // d(2) = helium
            // d(3) = atomic oxygen
            // d(4) = molecular nitrogen
            // d(5) = molecular oxygen
            // d(6) = atomic nitrogen
            double tmpro = 0;
            for (int i = 1; i <= 6; i++) {
                final double gamma = MA[i] * glb;
                final double upapg = 1.0 + ALEFA[i] + gamma;
                final double fzI = FastMath.pow(t120tz, upapg) * FastMath.exp(-sigzeta * gamma);
                // concentrations of H, He, O, N2, O2, N (particles/cm³)
                final double ccI = dbase[i] * fzI;
                // contribution of densities of H, He, O, N2, O2, N (g/cm³)
                tmpro += ccI * VMA[i];
            }
            this.ro = tmpro;

        }

        /** Computation of function G.
         * @param a vector of coefficients for computation
         * @param da vector of partial derivatives
         * @param ff0 coefficient flag (1 for Ox, Az, He, T°; 0 for H and tp120)
         * @param kle_eq season indicator flag (summer, winter, equinox)
         * @return value of G
         */
        private double gFunction(final double[] a, final double[] da,
                                 final int ff0, final int kle_eq) {

            final double[] fmfb   = new double[3];
            final double[] fbm150 = new double[3];

            // latitude terms
            da[2]  = p20;
            da[3]  = p40;
            da[74] = p10;
            double a74 = a[74];
            double a77 = a[77];
            double a78 = a[78];
            if (kle_eq == -1) {
                // winter
                a74 = -a74;
                a77 = -a77;
                a78 = -a78;
            }
            if (kle_eq == 0 ) {
                // equinox
                a74 = semestrialCorrection(a74);
                a77 = semestrialCorrection(a77);
                a78 = semestrialCorrection(a78);
            }
            da[77] = p30;
            da[78] = p50;
            da[79] = p60;

            // flux terms
            fmfb[1]   = f[1] - fbar[1];
            fmfb[2]   = f[2] - fbar[2];
            fbm150[1] = fbar[1] - 150.0;
            fbm150[2] = fbar[2];
            da[4]     = fmfb[1];
            da[6]     = fbm150[1];
            da[4]     = da[4] + a[70] * fmfb[2];
            da[6]     = da[6] + a[71] * fbm150[2];
            da[70]    = fmfb[2] * (a[4] + 2.0 * a[5] * da[4] + a[82] * p10 +
                                   a[83] * p20 + a[84] * p30);
            da[71]    = fbm150[2] * (a[6] + 2.0 * a[69] * da[6] + a[85] * p10 +
                                     a[86] * p20 + a[87] * p30);
            da[5]     = da[4] * da[4];
            da[69]    = da[6] * da[6];
            da[82]    = da[4] * p10;
            da[83]    = da[4] * p20;
            da[84]    = da[4] * p30;
            da[85]    = da[6] * p20;
            da[86]    = da[6] * p30;
            da[87]    = da[6] * p40;

            // Kp terms
            final int ikp  = 62;
            final int ikpm = 67;
            final double c2fi = 1.0 - p10mg * p10mg;
            final double dkp  = akp[1] + (a[ikp] + c2fi * a[ikp + 1]) * akp[2];
            double dakp = a[7] + a[8] * p20mg + a[68] * p40mg +
                          2.0 * dkp * (a[60] + a[61] * p20mg +
                                       a[75] * 2.0 * dkp * dkp);
            da[ikp] = dakp * akp[2];
            da[ikp + 1] = da[ikp] * c2fi;
            final double dkpm  = akp[3] + a[ikpm] * akp[4];
            final double dakpm = a[64] + a[65] * p20mg + a[72] * p40mg +
                                 2.0 * dkpm * (a[66] + a[73] * p20mg +
                                               a[76] * 2.0 * dkpm * dkpm);
            da[ikpm] = dakpm * akp[4];
            da[7]    = dkp;
            da[8]    = p20mg * dkp;
            da[68]   = p40mg * dkp;
            da[60]   = dkp * dkp;
            da[61]   = p20mg * da[60];
            da[75]   = da[60] * da[60];
            da[64]   = dkpm;
            da[65]   = p20mg * dkpm;
            da[72]   = p40mg * dkpm;
            da[66]   = dkpm * dkpm;
            da[73]   = p20mg * da[66];
            da[76]   = da[66] * da[66];

            // non-periodic g(l) function
            double f0 = a[4]  * da[4]  + a[5]  * da[5]  + a[6]  * da[6]  +
                        a[69] * da[69] + a[82] * da[82] + a[83] * da[83] +
                        a[84] * da[84] + a[85] * da[85] + a[86] * da[86] +
                        a[87] * da[87];
            final double f1f = 1.0 + f0 * ff0;

            f0 = f0 + a[2] * da[2] + a[3] * da[3] + a74 * da[74] +
                 a77 * da[77] + a[7] * da[7] + a[8] * da[8] +
                 a[60] * da[60] + a[61] * da[61] + a[68] * da[68] +
                 a[64] * da[64] + a[65] * da[65] + a[66] * da[66] +
                 a[72] * da[72] + a[73] * da[73] + a[75] * da[75] +
                 a[76] * da[76] + a78   * da[78] + a[79] * da[79];
//          termes annuels symetriques en latitude
            da[9]  = FastMath.cos(ROT * (day - a[11]));
            da[10] = p20 * da[9];
//          termes semi-annuels symetriques en latitude
            da[12] = FastMath.cos(ROT2 * (day - a[14]));
            da[13] = p20 * da[12];
//          termes annuels non symetriques en latitude
            final double coste = FastMath.cos(ROT * (day - a[18]));
            da[15] = p10 * coste;
            da[16] = p30 * coste;
            da[17] = p50 * coste;
//          terme  semi-annuel  non symetrique  en latitude
            final double cos2te = FastMath.cos(ROT2 * (day - a[20]));
            da[19] = p10 * cos2te;
            da[39] = p30 * cos2te;
            da[59] = p50 * cos2te;
//          termes diurnes [et couples annuel]
            da[21] = p11 * ch;
            da[22] = p31 * ch;
            da[23] = p51 * ch;
            da[24] = da[21] * coste;
            da[25] = p21 * ch * coste;
            da[26] = p11 * sh;
            da[27] = p31 * sh;
            da[28] = p51 * sh;
            da[29] = da[26] * coste;
            da[30] = p21 * sh * coste;
//          termes semi-diurnes [et couples annuel]
            da[31] = p22 * c2h;
            da[37] = p42 * c2h;
            da[32] = p32 * c2h * coste;
            da[33] = p22 * s2h;
            da[38] = p42 * s2h;
            da[34] = p32 * s2h * coste;
            da[88] = p32 * c2h;
            da[89] = p32 * s2h;
            da[90] = p52 * c2h;
            da[91] = p52 * s2h;
            double a88 = a[88];
            double a89 = a[89];
            double a90 = a[90];
            double a91 = a[91];
            if (kle_eq == -1) {            //hiver
                a88 = -a88;
                a89 = -a89;
                a90 = -a90;
                a91 = -a91;
            }
            if (kle_eq == 0) {             //equinox
                a88 = semestrialCorrection(a88);
                a89 = semestrialCorrection(a89);
                a90 = semestrialCorrection(a90);
                a91 = semestrialCorrection(a91);
            }
            da[92] = p62 * c2h;
            da[93] = p62 * s2h;
//          termes ter-diurnes
            da[35] = p33 * c3h;
            da[36] = p33 * s3h;
//          fonction g[l] periodique
            double fp = a[9]  * da[9]  + a[10] * da[10] + a[12] * da[12] + a[13] * da[13] +
                        a[15] * da[15] + a[16] * da[16] + a[17] * da[17] + a[19] * da[19] +
                        a[21] * da[21] + a[22] * da[22] + a[23] * da[23] + a[24] * da[24] +
                        a[25] * da[25] + a[26] * da[26] + a[27] * da[27] + a[28] * da[28] +
                        a[29] * da[29] + a[30] * da[30] + a[31] * da[31] + a[32] * da[32] +
                        a[33] * da[33] + a[34] * da[34] + a[35] * da[35] + a[36] * da[36] +
                        a[37] * da[37] + a[38] * da[38] + a[39] * da[39] + a[59] * da[59] +
                        a88   * da[88] + a89   * da[89] + a90   * da[90] + a91   * da[91] +
                        a[92] * da[92] + a[93] * da[93];
//          termes d'activite magnetique
            da[40] = p10 * coste * dkp;
            da[41] = p30 * coste * dkp;
            da[42] = p50 * coste * dkp;
            da[43] = p11 * ch * dkp;
            da[44] = p31 * ch * dkp;
            da[45] = p51 * ch * dkp;
            da[46] = p11 * sh * dkp;
            da[47] = p31 * sh * dkp;
            da[48] = p51 * sh * dkp;

//          fonction g[l] periodique supplementaire
            fp += a[40] * da[40] + a[41] * da[41] + a[42] * da[42] + a[43] * da[43] +
                  a[44] * da[44] + a[45] * da[45] + a[46] * da[46] + a[47] * da[47] +
                  a[48] * da[48];

            dakp = (a[40] * p10 + a[41] * p30 + a[42] * p50) * coste +
                   (a[43] * p11 + a[44] * p31 + a[45] * p51) * ch +
                   (a[46] * p11 + a[47] * p31 + a[48] * p51) * sh;
            da[ikp] += dakp * akp[2];
            da[ikp + 1] = da[ikp] + dakp * c2fi * akp[2];
//          termes de longitude
            da[49] = p11 * clfl;
            da[50] = p21 * clfl;
            da[51] = p31 * clfl;
            da[52] = p41 * clfl;
            da[53] = p51 * clfl;
            da[54] = p11 * slfl;
            da[55] = p21 * slfl;
            da[56] = p31 * slfl;
            da[57] = p41 * slfl;
            da[58] = p51 * slfl;

//          fonction g[l] periodique supplementaire
            fp += a[49] * da[49] + a[50] * da[50] + a[51] * da[51] + a[52] * da[52] +
                  a[53] * da[53] + a[54] * da[54] + a[55] * da[55] + a[56] * da[56] +
                  a[57] * da[57] + a[58] * da[58];

//          fonction g(l) totale (couplage avec le flux)
            return f0 + fp * f1f;

        }


        /** Apply a correction coefficient to the given parameter.
         * @param param the parameter to correct
         * @return the corrected parameter
         */
        private double semestrialCorrection(final double param) {
            final int debeq_pr = 59;
            final int debeq_au = 244;
            final double result;
            if (day >= 100) {
                final double xmult  = (day - debeq_au) / 40.0;
                result = param - 2.0 * param * xmult;
            } else {
                final double xmult  = (day - debeq_pr) / 40.0;
                result = 2.0 * param * xmult - param;
            }
            return result;
        }


    }

    /** Local holder for intermediate results ensuring the model is reentrant.
     * @param <T> type of the field elements
     */
    private static class FieldComputation<T extends CalculusFieldElement<T>> {

        /** Number of days in current year. */
        private int day;

        /** Instant solar flux. f[1] = instantaneous flux; f[2] = 0. (not used). */
        private double[] f = new double[3];

        /** Mean solar flux. fbar[1] = mean flux; fbar[2] = 0. (not used). */
        private double[] fbar = new double[3];

        /** Kp coefficients.
         * <ul>
         *   <li>akp[1] = 3-hourly kp</li>
         *   <li>akp[2] = 0 (not used)</li>
         *   <li>akp[3] = mean kp of last 24 hours</li>
         *   <li>akp[4] = 0 (not used)</li>
         * </ul>
         */
        private double[] akp = new double[5];

        /** Cosine of the longitude. */
        private final T clfl;

        /** Sine of the longitude. */
        private final T slfl;

        /** Total density (g/cm3). */
        private final T ro;

        // CHECKSTYLE: stop JavadocVariable check

        /** Legendre coefficients. */
        private final T p10;
        private final T p20;
        private final T p30;
        private final T p40;
        private final T p50;
        private final T p60;
        private final T p11;
        private final T p21;
        private final T p31;
        private final T p41;
        private final T p51;
        private final T p22;
        private final T p32;
        private final T p42;
        private final T p52;
        private final T p62;
        private final T p33;
        private final T p10mg;
        private final T p20mg;
        private final T p40mg;

        /** Local time intermediate values. */
        private final T hl0;
        private final T ch;
        private final T sh;
        private final T c2h;
        private final T s2h;
        private final T c3h;
        private final T s3h;

        /** Simple constructor.
         * @param day day of year
         * @param altiKM altitude <em>in kilometers</em>
         * @param lon local longitude (rad)
         * @param lat local latitude (rad)
         * @param hl local solar time in rad (O hr = 0 rad)
         * @param f instantaneous solar flux (F10.7)
         * @param far mean solar flux (F10.7)
         * @param akp geomagnetic activity index
         */
        FieldComputation(final int day,
                         final T altiKM, final T lon, final T lat,
                         final T hl, final double[] f, final double[] far,
                         final double[] akp) {

            this.day  = day;
            this.f    = f;
            this.fbar = far;
            this.akp  = akp;

            // Sine and cosine of local latitude and longitude
            final FieldSinCos<T> scLat = FastMath.sinCos(lat);
            final FieldSinCos<T> scLon = FastMath.sinCos(lon);

            // compute Legendre polynomials wrt geographic pole
            final T c = scLat.sin();
            final T c2 = c.multiply(c);
            final T c4 = c2.multiply(c2);
            final T s = scLat.cos();
            final T s2 = s.multiply(s);
            p10 = c;
            p20 = c2.multiply(1.5).subtract(0.5);
            p30 = c.multiply(c2.multiply(2.5).subtract(1.5));
            p40 = c4.multiply(4.375).subtract(c2.multiply(3.75)).add(0.375);
            p50 = c.multiply(c4.multiply(7.875).subtract(c2.multiply(8.75)).add(1.875));
            p60 = (c.multiply(5.5).multiply(p50).subtract(p40.multiply(2.5))).divide(3.0);
            p11 = s;
            p21 = c.multiply(3.0).multiply(s);
            p31 = s.multiply(c2.multiply(7.5).subtract(1.5));
            p41 = c.multiply(s).multiply(c2.multiply(17.5).subtract(7.5));
            p51 = s.multiply(c4.multiply(39.375).subtract(c2.multiply(26.25)).add(1.875));
            p22 = s2.multiply(3.0);
            p32 = c.multiply(15.0).multiply(s2);
            p42 = s2.multiply(c2.multiply(52.5).subtract(7.5));
            p52 = c.multiply(3.0).multiply(p42).subtract(p32.multiply(2.0));
            p62 = c.multiply(2.75).multiply(p52).subtract(p42.multiply(1.75));
            p33 = s.multiply(15.0).multiply(s2);

            // compute Legendre polynomials wrt magnetic pole (79N, 71W)
            final T clmlmg = lon.subtract(XLMG).cos();
            final T cmg  = s.multiply(CPMG).multiply(clmlmg).add(c.multiply(SPMG));
            final T cmg2 = cmg.multiply(cmg);
            final T cmg4 = cmg2.multiply(cmg2);
            p10mg = cmg;
            p20mg = cmg2.multiply(1.5).subtract(0.5);
            p40mg = cmg4.multiply(4.375).subtract(cmg2.multiply(3.75)).add(0.375);

            clfl = scLon.cos();
            slfl = scLon.sin();

            // local time
            hl0 = hl;
            final FieldSinCos<T> scHlo = FastMath.sinCos(hl0);
            ch  = scHlo.cos();
            sh  = scHlo.sin();
            c2h = ch.multiply(ch).subtract(sh.multiply(sh));
            s2h = ch.multiply(sh).multiply(2);
            c3h = c2h.multiply(ch).subtract(s2h.multiply(sh));
            s3h = s2h.multiply(ch).add(c2h.multiply(sh));

            final double zlb = ZLB0; // + dzlb ??

            final T[] dtt  = MathArrays.buildArray(altiKM.getField(), tt.length);
            final T[] dh   = MathArrays.buildArray(altiKM.getField(), tt.length);
            final T[] dhe  = MathArrays.buildArray(altiKM.getField(), tt.length);
            final T[] dox  = MathArrays.buildArray(altiKM.getField(), tt.length);
            final T[] daz2 = MathArrays.buildArray(altiKM.getField(), tt.length);
            final T[] do2  = MathArrays.buildArray(altiKM.getField(), tt.length);
            final T[] daz  = MathArrays.buildArray(altiKM.getField(), tt.length);
            final T[] dt0  = MathArrays.buildArray(altiKM.getField(), tt.length);
            final T[] dtp  = MathArrays.buildArray(altiKM.getField(), tt.length);

            //  compute function g(l) / tinf, t120, tp120
            int kleq = 1;
            final T gdelt = gFunction(tt, dtt, 1, kleq);
            dtt[1] = gdelt.add(1);
            final T tinf   = dtt[1].multiply(tt[1]);

            kleq = 0; // equinox

            if (day < 59 || day > 284) {
                kleq = -1; // north winter
            }
            if (day > 99 && day < 244) {
                kleq = 1; // north summer
            }

            final T gdelt0 =  gFunction(t0, dt0, 0, kleq);
            dt0[1] = gdelt0.add(t0[1]).divide(t0[1]);
            final T t120 = gdelt0.add(t0[1]);
            final T gdeltp = gFunction(tp, dtp, 0, kleq);
            dtp[1] = gdeltp.add(tp[1]).divide(tp[1]);
            final T tp120 = gdeltp.add(tp[1]);

            // compute n(z) concentrations: H, He, O, N2, O2, N
            final T sigma   = tp120.divide(tinf.subtract(t120));
            final T dzeta   = altiKM.add(RE).reciprocal().multiply(zlb + RE);
            final T zeta    = altiKM.subtract(zlb).multiply(dzeta);
            final T sigzeta = sigma.multiply(zeta);
            final T expsz   = sigzeta.negate().exp();
            final T tz = tinf.subtract(tinf.subtract(t120).multiply(expsz));

            final T[] dbase = MathArrays.buildArray(altiKM.getField(), 7);

            kleq = 1;

            final T gdelh = gFunction(h, dh, 0, kleq);
            dh[1] = gdelh.exp();
            dbase[1] = dh[1].multiply(h[1]);

            final T gdelhe = gFunction(he, dhe, 0, kleq);
            dhe[1] = gdelhe.exp();
            dbase[2] = dhe[1].multiply(he[1]);

            final T gdelo = gFunction(o, dox, 1, kleq);
            dox[1] = gdelo.exp();
            dbase[3] = dox[1].multiply(o[1]);

            final T gdelaz2 = gFunction(az2, daz2, 1, kleq);
            daz2[1] = gdelaz2.exp();
            dbase[4] = daz2[1].multiply(az2[1]);

            final T gdelo2 = gFunction(o2, do2, 1, kleq);
            do2[1] = gdelo2.exp();
            dbase[5] = do2[1].multiply(o2[1]);

            final T gdelaz = gFunction(az, daz, 1, kleq);
            daz[1] = gdelaz.exp();
            dbase[6] = daz[1].multiply(az[1]);

            final double zlbre  = 1.0 + zlb / RE;
            final T glb    = sigma.multiply(RGAS).multiply(tinf).reciprocal().multiply(GSURF / (zlbre * zlbre));
            final T t120tz = t120.divide(tz);

            // Partial densities in (g/cm3).
            // d(1) = hydrogen
            // d(2) = helium
            // d(3) = atomic oxygen
            // d(4) = molecular nitrogen
            // d(5) = molecular oxygen
            // d(6) = atomic nitrogen
            T tmpro = altiKM.getField().getZero();
            for (int i = 1; i <= 6; i++) {
                final T gamma = glb.multiply(MA[i]);
                final T upapg = gamma.add(1.0 + ALEFA[i]);
                final T fzI = t120tz.pow(upapg).multiply(sigzeta.negate().multiply(gamma).exp());
                // concentrations of H, He, O, N2, O2, N (particles/cm³)
                final T ccI = dbase[i].multiply(fzI);
                // contribution of densities of H, He, O, N2, O2, N (g/cm³)
                tmpro = tmpro.add(ccI.multiply(VMA[i]));
            }
            this.ro = tmpro;

        }

        /** Computation of function G.
         * @param a vector of coefficients for computation
         * @param da vector of partial derivatives
         * @param ff0 coefficient flag (1 for Ox, Az, He, T°; 0 for H and tp120)
         * @param kle_eq season indicator flag (summer, winter, equinox)
         * @return value of G
         */
        private T gFunction(final double[] a, final T[] da,
                            final int ff0, final int kle_eq) {

            final T zero = da[0].getField().getZero();
            final double[] fmfb   = new double[3];
            final double[] fbm150 = new double[3];

            // latitude terms
            da[2]  = p20;
            da[3]  = p40;
            da[74] = p10;
            double a74 = a[74];
            double a77 = a[77];
            double a78 = a[78];
            if (kle_eq == -1) {
                // winter
                a74 = -a74;
                a77 = -a77;
                a78 = -a78;
            }
            if (kle_eq == 0 ) {
                // equinox
                a74 = semestrialCorrection(a74);
                a77 = semestrialCorrection(a77);
                a78 = semestrialCorrection(a78);
            }
            da[77] = p30;
            da[78] = p50;
            da[79] = p60;

            // flux terms
            fmfb[1]   = f[1] - fbar[1];
            fmfb[2]   = f[2] - fbar[2];
            fbm150[1] = fbar[1] - 150.0;
            fbm150[2] = fbar[2];
            da[4]     = zero.add(fmfb[1]);
            da[6]     = zero.add(fbm150[1]);
            da[4]     = da[4].add(a[70] * fmfb[2]);
            da[6]     = da[6].add(a[71] * fbm150[2]);
            da[70]    = da[4].multiply(a[ 5]).multiply(2).
                            add(p10.multiply(a[82])).
                            add(p20.multiply(a[83])).
                            add(p30.multiply(a[84])).
                            add(a[4]).
                        multiply(fmfb[2]);
            da[71]    = da[6].multiply(a[69]).multiply(2).
                            add(p10.multiply(a[85])).
                            add(p20.multiply(a[86])).
                            add(p30.multiply(a[87])).
                            add(a[6]).
                        multiply(fbm150[2]);
            da[5]     = da[4].multiply(da[4]);
            da[69]    = da[6].multiply(da[6]);
            da[82]    = da[4].multiply(p10);
            da[83]    = da[4].multiply(p20);
            da[84]    = da[4].multiply(p30);
            da[85]    = da[6].multiply(p20);
            da[86]    = da[6].multiply(p30);
            da[87]    = da[6].multiply(p40);

            // Kp terms
            final int ikp  = 62;
            final int ikpm = 67;
            final T c2fi = p10mg.multiply(p10mg).negate().add(1);
            final T dkp  = c2fi.multiply(a[ikp + 1]).add(a[ikp]).multiply(akp[2]).add(akp[1]);
            T dakp = p20mg.multiply(a[8]).add(p40mg.multiply(a[68])).
                     add(p20mg.multiply(a[61]).add(dkp.multiply(dkp).multiply(2 * a[75]).add(a[60])).multiply(dkp.multiply(2))).
                     add(a[7]);
            da[ikp]     = dakp.multiply(akp[2]);
            da[ikp + 1] = da[ikp].multiply(c2fi);
            final double dkpm  = akp[3] + a[ikpm] * akp[4];
            final T dakpm = p20mg.multiply(a[65]).add(p40mg.multiply(a[72])).
                            add(p20mg.multiply(a[73]).add(a[66] + a[76] * 2.0 * dkpm * dkpm).multiply( 2.0 * dkpm)).
                            add(a[64]);
            da[ikpm] = dakpm.multiply(akp[4]);
            da[7]    = dkp;
            da[8]    = p20mg.multiply(dkp);
            da[68]   = p40mg.multiply(dkp);
            da[60]   = dkp.multiply(dkp);
            da[61]   = p20mg.multiply(da[60]);
            da[75]   = da[60].multiply(da[60]);
            da[64]   = zero.add(dkpm);
            da[65]   = p20mg.multiply(dkpm);
            da[72]   = p40mg.multiply(dkpm);
            da[66]   = zero.add(dkpm * dkpm);
            da[73]   = p20mg.multiply(da[66]);
            da[76]   = da[66].multiply(da[66]);

            // non-periodic g(l) function
            T f0 = da[4].multiply(a[4]).
                   add(da[5].multiply(a[5])).
                   add(da[6].multiply(a[6])).
                   add(da[69].multiply(a[69])).
                   add(da[82].multiply(a[82])).
                   add(da[83].multiply(a[83])).
                   add(da[84].multiply(a[84])).
                   add(da[85].multiply(a[85])).
                   add(da[86].multiply(a[86])).
                   add(da[87].multiply(a[87]));
            final T f1f = f0.multiply(ff0).add(1);

            f0 = f0.
                 add(da[2].multiply(a[2])).
                 add(da[3].multiply(a[3])).
                 add(da[7].multiply(a[7])).
                 add(da[8].multiply(a[8])).
                 add(da[60].multiply(a[60])).
                 add(da[61].multiply(a[61])).
                 add(da[68].multiply(a[68])).
                 add(da[64].multiply(a[64])).
                 add(da[65].multiply(a[65])).
                 add(da[66].multiply(a[66])).
                 add(da[72].multiply(a[72])).
                 add(da[73].multiply(a[73])).
                 add(da[74].multiply(a74)).
                 add(da[75].multiply(a[75])).
                 add(da[76].multiply(a[76])).
                 add(da[77].multiply(a77)).
                 add(da[78].multiply(a78)).
                 add(da[79].multiply(a[79]));
//          termes annuels symetriques en latitude
            da[9]  = zero.add(FastMath.cos(ROT * (day - a[11])));
            da[10] = p20.multiply(da[9]);
//          termes semi-annuels symetriques en latitude
            da[12] = zero.add(FastMath.cos(ROT2 * (day - a[14])));
            da[13] = p20.multiply(da[12]);
//          termes annuels non symetriques en latitude
            final double coste = FastMath.cos(ROT * (day - a[18]));
            da[15] = p10.multiply(coste);
            da[16] = p30.multiply(coste);
            da[17] = p50.multiply(coste);
//          terme  semi-annuel  non symetrique  en latitude
            final double cos2te = FastMath.cos(ROT2 * (day - a[20]));
            da[19] = p10.multiply(cos2te);
            da[39] = p30.multiply(cos2te);
            da[59] = p50.multiply(cos2te);
//          termes diurnes [et couples annuel]
            da[21] = p11.multiply(ch);
            da[22] = p31.multiply(ch);
            da[23] = p51.multiply(ch);
            da[24] = da[21].multiply(coste);
            da[25] = p21.multiply(ch).multiply(coste);
            da[26] = p11.multiply(sh);
            da[27] = p31.multiply(sh);
            da[28] = p51.multiply(sh);
            da[29] = da[26].multiply(coste);
            da[30] = p21.multiply(sh).multiply(coste);
//          termes semi-diurnes [et couples annuel]
            da[31] = p22.multiply(c2h);
            da[37] = p42.multiply(c2h);
            da[32] = p32.multiply(c2h).multiply(coste);
            da[33] = p22.multiply(s2h);
            da[38] = p42.multiply(s2h);
            da[34] = p32.multiply(s2h).multiply(coste);
            da[88] = p32.multiply(c2h);
            da[89] = p32.multiply(s2h);
            da[90] = p52.multiply(c2h);
            da[91] = p52.multiply(s2h);
            double a88 = a[88];
            double a89 = a[89];
            double a90 = a[90];
            double a91 = a[91];
            if (kle_eq == -1) {            //hiver
                a88 = -a88;
                a89 = -a89;
                a90 = -a90;
                a91 = -a91;
            }
            if (kle_eq == 0) {             //equinox
                a88 = semestrialCorrection(a88);
                a89 = semestrialCorrection(a89);
                a90 = semestrialCorrection(a90);
                a91 = semestrialCorrection(a91);
            }
            da[92] = p62.multiply(c2h);
            da[93] = p62.multiply(s2h);
//          termes ter-diurnes
            da[35] = p33.multiply(c3h);
            da[36] = p33.multiply(s3h);
//          fonction g[l] periodique
            T fp =     da[ 9].multiply(a[ 9]) .add(da[10].multiply(a[10])).add(da[12].multiply(a[12])).add(da[13].multiply(a[13])).
                   add(da[15].multiply(a[15])).add(da[16].multiply(a[16])).add(da[17].multiply(a[17])).add(da[19].multiply(a[19])).
                   add(da[21].multiply(a[21])).add(da[22].multiply(a[22])).add(da[23].multiply(a[23])).add(da[24].multiply(a[24])).
                   add(da[25].multiply(a[25])).add(da[26].multiply(a[26])).add(da[27].multiply(a[27])).add(da[28].multiply(a[28])).
                   add(da[29].multiply(a[29])).add(da[30].multiply(a[30])).add(da[31].multiply(a[31])).add(da[32].multiply(a[32])).
                   add(da[33].multiply(a[33])).add(da[34].multiply(a[34])).add(da[35].multiply(a[35])).add(da[36].multiply(a[36])).
                   add(da[37].multiply(a[37])).add(da[38].multiply(a[38])).add(da[39].multiply(a[39])).add(da[59].multiply(a[59])).
                   add(da[88].multiply(a88))  .add(da[89].multiply(a89)  ).add(da[90].multiply(a90)  ).add(da[91].multiply(a91)  ).
                   add(da[92].multiply(a[92])).add(da[93].multiply(a[93]));
//          termes d'activite magnetique
            da[40] = p10.multiply(coste).multiply(dkp);
            da[41] = p30.multiply(coste).multiply(dkp);
            da[42] = p50.multiply(coste).multiply(dkp);
            da[43] = p11.multiply(ch).multiply(dkp);
            da[44] = p31.multiply(ch).multiply(dkp);
            da[45] = p51.multiply(ch).multiply(dkp);
            da[46] = p11.multiply(sh).multiply(dkp);
            da[47] = p31.multiply(sh).multiply(dkp);
            da[48] = p51.multiply(sh).multiply(dkp);

//          fonction g[l] periodique supplementaire
            fp = fp.
                  add(da[40].multiply(a[40])).
                  add(da[41].multiply(a[41])).
                  add(da[42].multiply(a[42])).
                  add(da[43].multiply(a[43])).
                  add(da[44].multiply(a[44])).
                  add(da[45].multiply(a[45])).
                  add(da[46].multiply(a[46])).
                  add(da[47].multiply(a[47])).
                  add(da[48].multiply(a[48]));

            dakp =     p10.multiply(a[40]).add(p30.multiply(a[41])).add(p50.multiply(a[42])).multiply(coste).
                   add(p11.multiply(a[40]).add(p31.multiply(a[44])).add(p51.multiply(a[45])).multiply(ch)).
                   add(p11.multiply(a[46]).add(p31.multiply(a[47])).add(p51.multiply(a[48])).multiply(sh));
            da[ikp] = da[ikp].add(dakp.multiply(akp[2]));
            da[ikp + 1] = da[ikp].add(dakp.multiply(c2fi).multiply(akp[2]));
//          termes de longitude
            da[49] = p11.multiply(clfl);
            da[50] = p21.multiply(clfl);
            da[51] = p31.multiply(clfl);
            da[52] = p41.multiply(clfl);
            da[53] = p51.multiply(clfl);
            da[54] = p11.multiply(slfl);
            da[55] = p21.multiply(slfl);
            da[56] = p31.multiply(slfl);
            da[57] = p41.multiply(slfl);
            da[58] = p51.multiply(slfl);

//          fonction g[l] periodique supplementaire
            fp = fp.
                 add(da[49].multiply(a[49])).
                 add(da[50].multiply(a[50])).
                 add(da[51].multiply(a[51])).
                 add(da[52].multiply(a[52])).
                 add(da[53].multiply(a[53])).
                 add(da[54].multiply(a[54])).
                 add(da[55].multiply(a[55])).
                 add(da[56].multiply(a[56])).
                 add(da[57].multiply(a[57])).
                 add(da[58].multiply(a[58]));

//          fonction g(l) totale (couplage avec le flux)
            return f0.add(fp.multiply(f1f));

        }


        /** Apply a correction coefficient to the given parameter.
         * @param param the parameter to correct
         * @return the corrected parameter
         */
        private double semestrialCorrection(final double param) {
            final int debeq_pr = 59;
            final int debeq_au = 244;
            final double result;
            if (day >= 100) {
                final double xmult  = (day - debeq_au) / 40.0;
                result = param - 2.0 * param * xmult;
            } else {
                final double xmult  = (day - debeq_pr) / 40.0;
                result = 2.0 * param * xmult - param;
            }
            return result;
        }

    }

}
