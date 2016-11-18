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
package org.orekit.forces.drag.atmosphere;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
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
 * <p>
 * Two computation methods are proposed to the user:
 * <ul>
 *   <li>one OREKIT independent and compliant with initial FORTRAN
 *       routine entry values:
 *        {@link #getDensity(int, double, double, double, double, double, double, double, double)}.</li>
 *   <li> one compliant with OREKIT Atmosphere interface, necessary to the
 *        {@link org.orekit.forces.drag.DragForce drag force model}
 *        computation.</li>
 * </ul>
 *
 *<p>
 * This model provides dense output for altitudes beyond 120 km. Computed data are:
 * <ul>
 *   <li>Temperature at altitude z (K)</li>
 *   <li>Exospheric temperature above input position (K)</li>
 *   <li>Vertical gradient of T a 120 km</li>
 *   <li>Total density (kg/m³)</li>
 *   <li>Mean atomic mass</li>
 *   <li>Partial densities in (kg/m³) : hydrogen, helium, atomic oxygen,
 *   molecular nitrogen, molecular oxygen, atomic nitrogen</li>
 * </ul>
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
 * All these data can be found on the <a href="http://sec.noaa.gov/Data/index.html">
 * NOAA (National Oceanic and Atmospheric Administration) website.</a>
 * </p>
 *
 *
 * @author R. Biancale, S. Bruinsma: original fortran routine
 * @author Fabien Maussion (java translation)
 */
public class DTM2000 implements Atmosphere {

    /** Identifier for hydrogen. */
    public static final int HYDROGEN = 1;

    /** Identifier for helium. */
    public static final int HELIUM = 2;

    /** Identifier for atomic oxygen. */
    public static final int ATOMIC_OXYGEN = 3;

    /** Identifier for molecular nitrogen. */
    public static final int MOLECULAR_NITROGEN = 4;

    /** Identifier for molecular oxygen. */
    public static final int MOLECULAR_OXYGEN = 5;

    /** Identifier for atomic nitrogen. */
    public static final int ATOMIC_NITROGEN = 6;

    /** Serializable UID. */
    private static final long serialVersionUID = -8901940398967553588L;

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

    /** Partial derivatives. */
    private double[] dtt;
    private double[] dh;
    private double[] dhe;
    private double[] dox;
    private double[] daz2;
    private double[] do2;
    private double[] daz;
    private double[] dt0;
    private double[] dtp;

    // CHECKSTYLE: resume JavadocVariable check

    /** Number of days in current year. */
    private int cachedDay;

    /** Instant solar flux. f[1] = instantaneous flux; f[2] = 0. (not used). */
    private double[] cachedF = new double[3];

    /** Mean solar flux. fbar[1] = mean flux; fbar[2] = 0. (not used). */
    private double[] cachedFbar = new double[3];

    /** Kp coefficients.
     * <ul>
     *   <li>akp[1] = 3-hourly kp</li>
     *   <li>akp[2] = 0 (not used)</li>
     *   <li>akp[3] = mean kp of last 24 hours</li>
     *   <li>akp[4] = 0 (not used)</li>
     * </ul>
     */
    private double[] akp = new double[5];

    /** Geodetic altitude in km (minimum altitude: 120 km). */
    private double cachedAlti;

    /** Local solar time (rad). */
    private double cachedHl;

    /** Geodetic Latitude (rad). */
    private double alat;

    /** Geodetic longitude (rad). */
    private double xlon;

    /** Temperature at altitude z (K). */
    private double tz;

    /** Exospheric temperature. */
    private double tinf;

    /** Vertical gradient of T a 120 km. */
    private double tp120;

    /** Total density (g/cm3). */
    private double ro;

    /** Mean atomic mass. */
    private double wmm;

    /** Partial densities in (g/cm3).
     * d(1) = hydrogen
     * d(2) = helium
     * d(3) = atomic oxygen
     * d(4) = molecular nitrogen
     * d(5) = molecular oxygen
     * d(6) = atomic nitrogen
     */
    private double[] d = new double[7];

    // CHECKSTYLE: stop JavadocVariable check

    /** Legendre coefficients. */
    private double p10;
    private double p20;
    private double p30;
    private double p40;
    private double p50;
    private double p60;
    private double p11;
    private double p21;
    private double p31;
    private double p41;
    private double p51;
    private double p22;
    private double p32;
    private double p42;
    private double p52;
    private double p62;
    private double p33;
    private double p10mg;
    private double p20mg;
    private double p40mg;

    /** Local time intermediate values. */
    private double hl0;
    private double ch;
    private double sh;
    private double c2h;
    private double s2h;
    private double c3h;
    private double s3h;

    // CHECKSTYLE: resume JavadocVariable check

    /** Sun position. */
    private PVCoordinatesProvider sun;

    /** External data container. */
    private DTM2000InputParameters inputParams;

    /** Earth body shape. */
    private BodyShape earth;

    /** Simple constructor for independent computation.
     * @param parameters the solar and magnetic activity data
     * @param sun the sun position
     * @param earth the earth body shape
     * @exception OrekitException if some resource file reading error occurs
     */
    public DTM2000(final DTM2000InputParameters parameters,
                   final PVCoordinatesProvider sun, final BodyShape earth)
        throws OrekitException {

        synchronized (DTM2000.class) {
            // lazy reading of model coefficients
            if (tt == null) {
                readcoefficients();
            }
        }

        this.earth = earth;
        this.sun = sun;
        this.inputParams = parameters;

        dtt  = new double[tt.length];
        dh   = new double[tt.length];
        dhe  = new double[tt.length];
        dox  = new double[tt.length];
        daz2 = new double[tt.length];
        do2  = new double[tt.length];
        daz  = new double[tt.length];
        dt0  = new double[tt.length];
        dtp  = new double[tt.length];

        Arrays.fill(dtt,  Double.NaN);
        Arrays.fill(dh,   Double.NaN);
        Arrays.fill(dhe,  Double.NaN);
        Arrays.fill(dox,  Double.NaN);
        Arrays.fill(daz2, Double.NaN);
        Arrays.fill(do2,  Double.NaN);
        Arrays.fill(daz,  Double.NaN);
        Arrays.fill(dt0,  Double.NaN);
        Arrays.fill(dtp,  Double.NaN);

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
     * @exception OrekitException if altitude is outside of supported range
     */
    public synchronized double getDensity(final int day,
                                          final double alti, final double lon, final double lat,
                                          final double hl, final double f, final double fbar,
                                          final double akp3, final double akp24)
        throws OrekitException {
        final double threshold = 120000;
        if (alti < threshold) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD,
                                      alti, threshold);
        }
        this.cachedDay  = day;
        this.cachedAlti = alti / 1000;
        xlon = lon;
        alat = lat;
        this.cachedHl = hl;
        this.cachedF[1] = f;
        this.cachedFbar[1] = fbar;
        akp[1] = akp3;
        akp[3] = akp24;
        computation();
        return ro * 1000;
    }


    /** Computes output vales once the inputs are set.
     */
    private synchronized void computation() {
        ro = 0.0;

        final double zlb = ZLB0; // + dzlb ??

        // compute Legendre polynomials wrt geographic pole
        final double c = FastMath.sin(alat);
        final double c2 = c * c;
        final double c4 = c2 * c2;
        final double s = FastMath.cos(alat);
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
        final double clmlmg = FastMath.cos(xlon - XLMG);
        final double cmg  = s * CPMG * clmlmg + c * SPMG;
        final double cmg2 = cmg * cmg;
        final double cmg4 = cmg2 * cmg2;
        p10mg = cmg;
        p20mg = 1.5 * cmg2 - 0.5;
        p40mg = 4.375 * cmg4 - 3.75 * cmg2 + 0.375;

        // local time
        hl0 = cachedHl;
        ch  = FastMath.cos(hl0);
        sh  = FastMath.sin(hl0);
        c2h = ch * ch - sh * sh;
        s2h = 2.0 * ch * sh;
        c3h = c2h * ch - s2h * sh;
        s3h = s2h * ch + c2h * sh;

        //  compute function g(l) / tinf, t120, tp120
        int kleq = 1;
        final double gdelt = gFunction(tt, dtt, 1, kleq);
        dtt[1] = 1.0 + gdelt;
        tinf   = tt[1] * dtt[1];

        kleq = 0; // equinox

        if ((cachedDay < 59) || (cachedDay > 284)) {
            kleq = -1; // north winter
        }
        if ((cachedDay > 99) && (cachedDay < 244)) {
            kleq = 1; // north summer
        }

        final double gdelt0 =  gFunction(t0, dt0, 0, kleq);
        dt0[1] = (t0[1] + gdelt0) / t0[1];
        final double t120 = t0[1] + gdelt0;
        final double gdeltp = gFunction(tp, dtp, 0, kleq);
        dtp[1] = (tp[1] + gdeltp) / tp[1];
        tp120 = tp[1] + gdeltp;

        // compute n(z) concentrations: H, He, O, N2, O2, N
        final double sigma   = tp120 / (tinf - t120);
        final double dzeta   = (RE + zlb) / (RE + cachedAlti);
        final double zeta    = (cachedAlti - zlb) * dzeta;
        final double sigzeta = sigma * zeta;
        final double expsz   = FastMath.exp(-sigzeta);
        tz = tinf - (tinf - t120) * expsz;

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

        final double[] cc = new double[7];
        final double[] fz = new double[7];

        for (int i = 1; i <= 6; i++) {
            final double gamma = MA[i] * glb;
            final double upapg = 1.0 + ALEFA[i] + gamma;
            fz[i] = FastMath.pow(t120tz, upapg) * FastMath.exp(-sigzeta * gamma);
            // concentrations of H, He, O, N2, O2, N (particles/cm³)
            cc[i] = dbase[i] * fz[i];
            // densities of H, He, O, N2, O2, N (g/cm³)
            d[i]  = cc[i] * VMA[i];
            // total density
            ro += d[i];
        }

        // mean atomic mass
        wmm = ro / (VMA[1] * (cc[1] + cc[2] + cc[3] + cc[4] + cc[5] + cc[6]));

    }

    /** Computation of function G.
     * @param a vector of coefficients for computation
     * @param da vector of partial derivatives
     * @param ff0 coefficient flag (1 for Ox, Az, He, T°; 0 for H and tp120)
     * @param kle_eq season indicator flag (summer, winter, equinox)
     * @return value of G
     */
    private synchronized double gFunction(final double[] a, final double[] da,
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
        fmfb[1]   = cachedF[1] - cachedFbar[1];
        fmfb[2]   = cachedF[2] - cachedFbar[2];
        fbm150[1] = cachedFbar[1] - 150.0;
        fbm150[2] = cachedFbar[2];
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
//      termes annuels symetriques en latitude
        da[9]  = FastMath.cos(ROT * (cachedDay - a[11]));
        da[10] = p20 * da[9];
//      termes semi-annuels symetriques en latitude
        da[12] = FastMath.cos(ROT2 * (cachedDay - a[14]));
        da[13] = p20 * da[12];
//      termes annuels non symetriques en latitude
        final double coste = FastMath.cos(ROT * (cachedDay - a[18]));
        da[15] = p10 * coste;
        da[16] = p30 * coste;
        da[17] = p50 * coste;
//      terme  semi-annuel  non symetrique  en latitude
        final double cos2te = FastMath.cos(ROT2 * (cachedDay - a[20]));
        da[19] = p10 * cos2te;
        da[39] = p30 * cos2te;
        da[59] = p50 * cos2te;
//      termes diurnes [et couples annuel]
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
//      termes semi-diurnes [et couples annuel]
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
//      termes ter-diurnes
        da[35] = p33 * c3h;
        da[36] = p33 * s3h;
//      fonction g[l] periodique
        double fp = a[9]  * da[9]  + a[10] * da[10] + a[12] * da[12] + a[13] * da[13] +
                    a[15] * da[15] + a[16] * da[16] + a[17] * da[17] + a[19] * da[19] +
                    a[21] * da[21] + a[22] * da[22] + a[23] * da[23] + a[24] * da[24] +
                    a[25] * da[25] + a[26] * da[26] + a[27] * da[27] + a[28] * da[28] +
                    a[29] * da[29] + a[30] * da[30] + a[31] * da[31] + a[32] * da[32] +
                    a[33] * da[33] + a[34] * da[34] + a[35] * da[35] + a[36] * da[36] +
                    a[37] * da[37] + a[38] * da[38] + a[39] * da[39] + a[59] * da[59] +
                    a88   * da[88] + a89   * da[89] + a90   * da[90] + a91   * da[91] +
                    a[92] * da[92] + a[93] * da[93];
//      termes d'activite magnetique
        da[40] = p10 * coste * dkp;
        da[41] = p30 * coste * dkp;
        da[42] = p50 * coste * dkp;
        da[43] = p11 * ch * dkp;
        da[44] = p31 * ch * dkp;
        da[45] = p51 * ch * dkp;
        da[46] = p11 * sh * dkp;
        da[47] = p31 * sh * dkp;
        da[48] = p51 * sh * dkp;

//      fonction g[l] periodique supplementaire
        fp += a[40] * da[40] + a[41] * da[41] + a[42] * da[42] + a[43] * da[43] +
              a[44] * da[44] + a[45] * da[45] + a[46] * da[46] + a[47] * da[47] +
              a[48] * da[48];

        dakp = (a[40] * p10 + a[41] * p30 + a[42] * p50) * coste +
               (a[43] * p11 + a[44] * p31 + a[45] * p51) * ch +
               (a[46] * p11 + a[47] * p31 + a[48] * p51) * sh;
        da[ikp] += dakp * akp[2];
        da[ikp + 1] = da[ikp] + dakp * c2fi * akp[2];
//      termes de longitude
        final double clfl = FastMath.cos(xlon);
        da[49] = p11 * clfl;
        da[50] = p21 * clfl;
        da[51] = p31 * clfl;
        da[52] = p41 * clfl;
        da[53] = p51 * clfl;
        final double slfl = FastMath.sin(xlon);
        da[54] = p11 * slfl;
        da[55] = p21 * slfl;
        da[56] = p31 * slfl;
        da[57] = p41 * slfl;
        da[58] = p51 * slfl;

//      fonction g[l] periodique supplementaire
        fp += a[49] * da[49] + a[50] * da[50] + a[51] * da[51] + a[52] * da[52] +
              a[53] * da[53] + a[54] * da[54] + a[55] * da[55] + a[56] * da[56] +
              a[57] * da[57] + a[58] * da[58];

//      fonction g(l) totale (couplage avec le flux)
        return f0 + fp * f1f;

    }


    /** Apply a correction coefficient to the given parameter.
     * @param param the parameter to correct
     * @return the corrected parameter
     */
    private synchronized double semestrialCorrection(final double param) {
        final int debeq_pr = 59;
        final int debeq_au = 244;
        final double result;
        if (cachedDay >= 100) {
            final double xmult  = (cachedDay - debeq_au) / 40.0;
            result = param - 2.0 * param * xmult;
        } else {
            final double xmult  = (cachedDay - debeq_pr) / 40.0;
            result = 2.0 * param * xmult - param;
        }
        return result;
    }


    /** Store the DTM model elements coefficients in internal arrays.
     * @exception OrekitException if some resource file reading error occurs
     */
    private static void readcoefficients() throws OrekitException {

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

        final InputStream in = DTM2000.class.getResourceAsStream(DTM2000);
        if (in == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_RESOURCE, DTM2000);
        }

        BufferedReader r = null;
        try {

            r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
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
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ioe) {
                    throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
                }
            }
        }
    }



    /** Get the current exospheric temperature above input position.
     * {@link #getDensity(int, double, double, double, double, double, double, double, double) getDensity}
     * method <b>must</b> be called before calling this function.
     * @return the exospheric temperature (K)
     */
    public double getTinf() {
        return tinf;
    }

    /** Get the local temperature.
     * {@link #getDensity(int, double, double, double, double, double, double, double, double) getDensity}
     * method <b>must</b> be called before calling this function.
     * @return the temperature at altitude z (K)
     */
    public double getT() {
        return tz;
    }

    /** Get the local mean atomic mass.
     * {@link #getDensity(int, double, double, double, double, double, double, double, double) getDensity}
     * method <b>must</b> be called before calling this function.
     * @return the local mean atomic mass
     */
    public double getMam() {
        return wmm;
    }

    /** Get the local partial density of the selected element.
     * {@link #getDensity(int, double, double, double, double, double, double, double, double) getDensity}
     * method <b>must</b> be called before calling this function.
     * @param identifier one of the six elements : {@link #HYDROGEN}, {@link #HELIUM},
     * {@link #ATOMIC_OXYGEN}, {@link #MOLECULAR_NITROGEN},  {@link #MOLECULAR_OXYGEN}, {@link #ATOMIC_NITROGEN}
     * @return the local partial density (kg/m³)
     */
    public double getPartialDensities(final int identifier) {
        if (identifier < 1 || identifier > 6) {
            throw new IllegalArgumentException("element identifier is not correct");
        }
        return d[identifier] * 1000;
    }

    /** Get the local density.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return local density (kg/m³)
     * @exception OrekitException if date is out of range of solar activity model
     * or if some frame conversion cannot be performed
     */
    public double getDensity(final AbsoluteDate date, final Vector3D position,
                             final Frame frame)
        throws OrekitException {

        // check if data are available :
        if ((date.compareTo(inputParams.getMaxDate()) > 0) ||
            (date.compareTo(inputParams.getMinDate()) < 0)) {
            throw new OrekitException(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE,
                                      date, inputParams.getMinDate(), inputParams.getMaxDate());
        }

        // compute day number in current year
        final Calendar cal = new GregorianCalendar();
        cal.setTime(date.toDate(TimeScalesFactory.getUTC()));
        final int day = cal.get(Calendar.DAY_OF_YEAR);
        //position in ECEF so we only have to do the transform once
        final Frame ecef = earth.getBodyFrame();
        final Vector3D pEcef = frame.getTransformTo(ecef, date)
                .transformPosition(position);
        // compute geodetic position
        final GeodeticPoint inBody = earth.transform(pEcef, ecef, date);
        final double alti = inBody.getAltitude();
        final double lon = inBody.getLongitude();
        final double lat = inBody.getLatitude();

        // compute local solar time
        final Vector3D sunPos = sun.getPVCoordinates(date, ecef).getPosition();
        final double hl = FastMath.PI + FastMath.atan2(
                sunPos.getX() * pEcef.getY() - sunPos.getY() * pEcef.getX(),
                sunPos.getX() * pEcef.getX() + sunPos.getY() * pEcef.getY());

        // get current solar activity data and compute
        return getDensity(day, alti, lon, lat, hl, inputParams.getInstantFlux(date),
                          inputParams.getMeanFlux(date), inputParams.getThreeHourlyKP(date),
                          inputParams.get24HoursKp(date));

    }

    @Override
    public <T extends RealFieldElement<T>> T
        getDensity(final FieldAbsoluteDate<T> date, final FieldVector3D<T> position,
                   final Frame frame)
            throws OrekitException {
        // TODO: field implementation
        throw new UnsupportedOperationException();
    }

}
