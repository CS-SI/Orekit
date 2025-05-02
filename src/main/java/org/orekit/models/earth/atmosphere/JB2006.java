/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.KinematicTransform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.PVCoordinates;

/**
 * This is the realization of the Jacchia-Bowman 2006 atmospheric model.
 * <p>
 * It is described in the paper: <br>
 *
 * <a href="http://sol.spacenvironment.net/~JB2006/pubs/JB2006_AIAA-6166_model.pdf">A
 * New Empirical Thermospheric Density Model JB2006 Using New Solar Indices</a><br>
 *
 * <i>Bruce R. Bowman, W. Kent Tobiska and Frank A. Marcos</i> <br>
 * <p>
 * AIAA 2006-6166<br>
 * </p>
 * <p>
 * Two computation methods are proposed to the user:
 * <ul>
 * <li> one OREKIT independent and compliant with initial FORTRAN routine entry values:
 *        {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, double, double)}. </li>
 * <li> one compliant with OREKIT Atmosphere interface, necessary to the
 *        {@link org.orekit.forces.drag.DragForce
 *        drag force model} computation.</li>
 * </ul>
 *
 * <p>
 * This model provides dense output for all altitudes and positions. Output data are :
 * <ul>
 * <li>Exospheric Temperature above Input Position (deg K)</li>
 * <li>Temperature at Input Position (deg K)</li>
 * <li>Total Mass-Density at Input Position (kg/m³)</li>
 * </ul>
 *
 * <p>
 * The model needs geographical and time information to compute general values,
 * but also needs space weather data : mean and daily solar flux, retrieved threw
 * different indices, and planetary geomagnetic indices. <br>
 * More information on these indices can be found on the  <a
 * href="http://sol.spacenvironment.net/~JB2006/JB2006_index.html">
 * official JB2006 website.</a>
 * </p>
 *
 * @author Bruce R Bowman (HQ AFSPC, Space Analysis Division), Feb 2006: FORTRAN routine
 * @author Fabien Maussion (java translation)
 * @author Bryan Cazabonne (Orekit 13 update and field translation)
 * @since 13.1
 */
public class JB2006 extends AbstractSunInfluencedAtmosphere {

    /** Minimum altitude (m) for JB2006 use. */
    private static final double ALT_MIN = 90000.;

    /** Earth radius (km). */
    private static final double EARTH_RADIUS = 6356.766;

    /** The alpha are the thermal diffusion coefficients in equation (6). */
    private static final double[] ALPHA = {0, 0, 0, 0, -0.38};

    /** Natural logarithm of 10.0. */
    private static final double AL10 = 2.3025851;

    /** Molecular weights in order: N2, O2, O, Ar, He and H. */
    private static final double[] AMW = {28.0134, 31.9988, 15.9994, 39.9480, 4.0026, 1.00797};

    /** Avogadro's number in mks units (molecules/kmol). */
    private static final double AVOGAD = 6.02257e26;

    /** The FRAC are the assumed sea-level volume fractions in order: N2, O2, Ar, and He. */
    private static final double[] FRAC = {0.78110, 0.20955, 9.3400e-3, 1.2890e-5};

    /** Universal gas-constant in mks units (joules/K/kmol). */
    private static final double RSTAR = 8314.32;

    /** Value used to establish height step sizes in the regime 90km to 105km. */
    private static final double R1 = 0.010;

    /** Value used to establish height step sizes in the regime 105km to 500km. */
    private static final double R2 = 0.025;

    /** Value used to establish height step sizes in the regime above 500km. */
    private static final double R3 = 0.075;

    /** Weights for the Newton-Cotes five-points quadrature formula. */
    private static final double[] WT = {0.311111111111111, 1.422222222222222, 0.533333333333333, 1.422222222222222, 0.311111111111111};

    /** Coefficients for high altitude density correction. */
    private static final double[] CHT = {0.22, -0.20e-02, 0.115e-02, -0.211e-05};

    /** FZ global model values (1978-2004 fit). */
    private static final double[] FZM = { 0.111613e+00, -0.159000e-02, 0.126190e-01, -0.100064e-01, -0.237509e-04, 0.260759e-04};

    /** GT global model values (1978-2004 fit). */
    private static final double[] GTM = {-0.833646e+00,
                                         -0.265450e+00,
                                         0.467603e+00,
                                         -0.299906e+00,
                                         -0.105451e+00,
                                         -0.165537e-01,
                                         -0.380037e-01,
                                         -0.150991e-01,
                                         -0.541280e-01,
                                         0.119554e-01,
                                         0.437544e-02,
                                         -0.369016e-02,
                                         0.206763e-02,
                                         -0.142888e-02,
                                         -0.867124e-05,
                                         0.189032e-04,
                                         0.156988e-03,
                                         0.491286e-03,
                                         -0.391484e-04,
                                         -0.126854e-04,
                                         0.134078e-04,
                                         -0.614176e-05,
                                         0.343423e-05};

    /** XAMBAR relative data. */
    private static final double[] CXAMB = {28.15204, -8.5586e-2, +1.2840e-4, -1.0056e-5, -1.0210e-5, +1.5044e-6, +9.9826e-8};

    /** DTSUB relative data. */
    private static final double[] BDT_SUB = {-0.457512297e+01,
                                             -0.512114909e+01,
                                             -0.693003609e+02,
                                             0.203716701e+03,
                                             0.703316291e+03,
                                             -0.194349234e+04,
                                             0.110651308e+04,
                                             -0.174378996e+03,
                                             0.188594601e+04,
                                             -0.709371517e+04,
                                             0.922454523e+04,
                                             -0.384508073e+04,
                                             -0.645841789e+01,
                                             0.409703319e+02,
                                             -0.482006560e+03,
                                             0.181870931e+04,
                                             -0.237389204e+04,
                                             0.996703815e+03,
                                             0.361416936e+02};

    /** DTSUB relative data.  */
    private static final double[] CDT_SUB = {-0.155986211e+02,
                                             -0.512114909e+01,
                                             -0.693003609e+02,
                                             0.203716701e+03,
                                             0.703316291e+03,
                                             -0.194349234e+04,
                                             0.110651308e+04,
                                             -0.220835117e+03,
                                             0.143256989e+04,
                                             -0.318481844e+04,
                                             0.328981513e+04,
                                             -0.135332119e+04,
                                             0.199956489e+02,
                                             -0.127093998e+02,
                                             0.212825156e+02,
                                             -0.275555432e+01,
                                             0.110234982e+02,
                                             0.148881951e+03,
                                             -0.751640284e+03,
                                             0.637876542e+03,
                                             0.127093998e+02,
                                             -0.212825156e+02,
                                             0.275555432e+01};

    /** External data container. */
    private final JB2006InputParameters inputParams;

    /** Earth body shape. */
    private final BodyShape earth;

    /** UTC time scale. */
    private final TimeScale utc;

    /**
     * Constructor with space environment information for internal computation.
     *
     * @param parameters the solar and magnetic activity data
     * @param sun        the sun position
     * @param earth      the earth body shape
     */
    @DefaultDataContext
    public JB2006(final JB2006InputParameters parameters, final ExtendedPositionProvider sun, final BodyShape earth) {
        this(parameters, sun, earth, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor with space environment information for internal computation.
     *
     * @param parameters the solar and magnetic activity data
     * @param sun        the sun position
     * @param earth      the earth body shape
     * @param utc        UTC time scale. Used to computed the day fraction.
     */
    @DefaultDataContext
    public JB2006(final JB2006InputParameters parameters, final ExtendedPositionProvider sun,
                  final BodyShape earth, final TimeScale utc) {
        super(sun);
        this.earth = earth;
        this.inputParams = parameters;
        this.utc = utc;
    }

    /**
     * Get the local density with initial entries.
     *
     * @param dateMJD  date and time, in modified julian days and fraction
     * @param sunRA    Right Ascension of Sun (radians)
     * @param sunDecli Declination of Sun (radians)
     * @param satLon   Right Ascension of position (radians)
     * @param satLat   Geocentric latitude of position (radians)
     * @param satAlt   Height of position (m)
     * @param f10      10.7-cm Solar flux (1e<sup>-22</sup>*Watt/(m²*Hertz)).
     *                 Tabular time 1.0 day earlier
     * @param f10B     10.7-cm Solar Flux, averaged 81-day centered on the input time
     * @param ap       Geomagnetic planetary 3-hour index A<sub>p</sub>
     *                 for a tabular time 6.7 hours earlier
     * @param s10      EUV index (26-34 nm) scaled to F10. Tabular time 1 day earlier.
     * @param s10B     UV 81-day averaged centered index
     * @param xm10     MG2 index scaled to F10
     * @param xm10B    MG2 81-day ave. centered index. Tabular time 5.0 days earlier.
     * @return total mass-Density at input position (kg/m³)
     */
    public double getDensity(final double dateMJD, final double sunRA, final double sunDecli,
                             final double satLon, final double satLat, final double satAlt,
                             final double f10, final double f10B, final double ap, final double s10,
                             final double s10B, final double xm10, final double xm10B) {

        if (satAlt < ALT_MIN) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, satAlt, ALT_MIN);
        }
        final double scaledSatAlt = satAlt / 1000.0;

        // Equation (14)
        final double tsubc = 379 + 3.353 * f10B + 0.358 * (f10 - f10B) + 2.094 * (s10 - s10B) + 0.343 * (xm10 - xm10B);

        // Equation (15)
        final double eta = 0.5 * FastMath.abs(satLat - sunDecli);
        final double theta = 0.5 * FastMath.abs(satLat + sunDecli);

        // Equation (16)
        final double h = satLon - sunRA;
        final double tau = h - 0.64577182 + 0.10471976 * FastMath.sin(h + 0.75049158);
        double solTimeHour = FastMath.toDegrees(h + FastMath.PI) / 15.0;
        if (solTimeHour >= 24) {
            solTimeHour = solTimeHour - 24.;
        }
        if (solTimeHour < 0) {
            solTimeHour = solTimeHour + 24.;
        }

        // Equation (17)
        final double cosEta = FastMath.pow(FastMath.cos(eta), 2.5);
        final double sinTeta = FastMath.pow(FastMath.sin(theta), 2.5);
        final double cosTau = FastMath.abs(FastMath.cos(0.5 * tau));
        final double df = sinTeta + (cosEta - sinTeta) * cosTau * cosTau * cosTau;
        final double tsubl = tsubc * (1. + 0.31 * df);

        // Equation (18)
        final double expAp = FastMath.exp(-0.08 * ap);
        final double dtg = ap + 100. * (1. - expAp);

        // Compute correction to dTc for local solar time and lat correction
        final double dtclst = dTc(f10, solTimeHour, satLat, scaledSatAlt);

        // Compute the local exospheric temperature.
        final double tInf = tsubl + dtg + dtclst;

        // Equation (9)
        final double tsubx = 444.3807 + 0.02385 * tInf - 392.8292 * FastMath.exp(-0.0021357 * tInf);

        // Equation (11)
        final double gsubx = 0.054285714 * (tsubx - 183.);

        // The TC array will be an argument in the call to
        // XLOCAL, which evaluates Equation (10) or Equation (13)
        final double[] tc = new double[4];
        tc[0] = tsubx;
        tc[1] = gsubx;
        //   A AND GSUBX/A OF Equation (13)
        tc[2] = (tInf - tsubx) / MathUtils.SEMI_PI;
        tc[3] = gsubx / tc[2];

        // Equation (5)
        final double z1    = 90.;
        final double z2    = FastMath.min(scaledSatAlt, 105.0);
        double al          = FastMath.log(z2 / z1);
        int n              = (int) FastMath.floor(al / R1) + 1;
        double zr          = FastMath.exp(al / n);
        final double mb1   = mBar(z1);
        final double tloc1 = localTemp(z1, tc);
        double zend        = z1;
        double sub2        = 0.;
        double ain         = mb1 * gravity(z1) / tloc1;
        double mb2         = 0;
        double tloc2       = 0;
        double z           = 0;
        double gravl       = 0;

        for (int i = 0; i < n; ++i) {
            z = zend;
            zend = zr * z;
            final double dz = 0.25 * (zend - z);
            double sum1 = WT[0] * ain;
            for (int j = 1; j < 5; ++j) {
                z += dz;
                mb2   = mBar(z);
                tloc2 = localTemp(z, tc);
                gravl = gravity(z);
                ain   = mb2 * gravl / tloc2;
                sum1 += WT[j] * ain;
            }
            sub2 += dz * sum1;
        }
        final double fact1 = 1000.0 / RSTAR;
        double rho = 3.46e-6 * mb2 * tloc1 * FastMath.exp(-fact1 * sub2) / (mb1 * tloc2);

        // Equation (2)
        final double anm = AVOGAD * rho;
        final double an = anm / mb2;

        // Equation (3)
        double fact2 = anm / 28.960;
        final double[] aln = new double[6];
        aln[0] = FastMath.log(FRAC[0] * fact2);
        aln[3] = FastMath.log(FRAC[2] * fact2);
        aln[4] = FastMath.log(FRAC[3] * fact2);
        // Equation (4)
        aln[1] = FastMath.log(fact2 * (1. + FRAC[1]) - an);
        aln[2] = FastMath.log(2. * (an - fact2));

        if (scaledSatAlt <= 105.0) {
            // Put in negligible hydrogen for use in DO-LOOP 13
            aln[5] = aln[4] - 25.0;
        }
        else {
            // Equation (6)
            al = FastMath.log(FastMath.min(scaledSatAlt, 500.0) / z);
            n = (int) FastMath.floor(al / R2) + 1;
            zr = FastMath.exp(al / n);
            sub2 = 0.;
            ain = gravl / tloc2;

            double tloc3 = 0;
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr * z;
                final double dz = 0.25 * (zend - z);
                double SUM1 = WT[0] * ain;
                for (int j = 1; j < 5; ++j) {
                    z += dz;
                    tloc3 = localTemp(z, tc);
                    gravl = gravity(z);
                    ain = gravl / tloc3;
                    SUM1 += WT[j] * ain;
                }
                sub2 = sub2 + dz * SUM1;
            }

            al = FastMath.log(FastMath.max(scaledSatAlt, 500.0) / z);
            final double r = (scaledSatAlt > 500.0) ? R3 : R2;
            n = (int) FastMath.floor(al / r) + 1;
            zr = FastMath.exp(al / n);
            double sum3 = 0.;
            double tloc4 = 0;
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr * z;
                final double dz = 0.25 * (zend - z);
                double sum1 = WT[0] * ain;
                for (int j = 1; j < 5; ++j) {
                    z += dz;
                    tloc4 = localTemp(z, tc);
                    gravl = gravity(z);
                    ain = gravl / tloc4;
                    sum1 = sum1 + WT[j] * ain;
                }
                sum3 = sum3 + dz * sum1;
            }
            final double altr;
            final double hSign;
            if (scaledSatAlt <= 500.) {
                altr = FastMath.log(tloc3 / tloc2);
                fact2 = fact1 * sub2;
                hSign = 1.0;
            }
            else {
                altr = FastMath.log(tloc4 / tloc2);
                fact2 = fact1 * (sub2 + sum3);
                hSign = -1.0;
            }
            for (int i = 0; i < 5; ++i) {
                aln[i] = aln[i] - (1.0 + ALPHA[i]) * altr - fact2 * AMW[i];
            }

            // Equation (7) - Note that in CIRA72, AL10T5 = DLOG10(T500)
            final double al10t5 = FastMath.log10(tInf);
            final double alnh5 = (5.5 * al10t5 - 39.40) * al10t5 + 73.13;
            aln[5] = AL10 * (alnh5 + 6.) + hSign * (FastMath.log(tloc4 / tloc3) + fact1 * sum3 * AMW[5]);
        }

        // Equation (24)  - J70 Seasonal-Latitudinal Variation
        final double capPhi = ((dateMJD - 36204.0) / 365.2422) % 1;
        final int signum = (satLat >= 0) ? 1 : -1;
        final double sinLat = FastMath.sin(satLat);
        final double hm90  = scaledSatAlt - 90.;
        final double dlrsl = 0.02 * hm90 * FastMath.exp(-0.045 * hm90) * signum * FastMath.sin(MathUtils.TWO_PI * capPhi + 1.72) * sinLat * sinLat;

        // Equation (23) - Computes the semiannual variation
        double dlrsa = 0;
        if (z < 2000.0) {
            // Use new semiannual model DELTA LOG RHO
            dlrsa = semian(dayOfYear(dateMJD), scaledSatAlt, f10B);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = AL10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final double dlr = AL10 * (dlrsl + dlrsa);
        for (int i = 0; i < 6; ++i) {
            aln[i] += dlr;
        }

        // Compute mass-density and mean-molecular-weight and
        // convert number density logs from natural to common.
        double sumnm = 0.0;
        for (int i = 0; i < 6; ++i) {
            sumnm += FastMath.exp(aln[i]) * AMW[i];
        }

        rho = sumnm / AVOGAD;

        // Compute the high altitude exospheric density correction factor
        double fex = 1.0;
        if (scaledSatAlt >= 1000.0 && scaledSatAlt < 1500.0) {
            final double zeta = (scaledSatAlt - 1000.) * 0.002;
            final double f15c = CHT[0] + CHT[1] * f10B + CHT[2] * 1500.0 + CHT[3] * f10B * 1500.0;
            final double f15cZeta = (CHT[2] + CHT[3] * f10B) * 500.0;
            final double fex2 = 3.0 * f15c - f15cZeta - 3.0;
            final double fex3 = f15cZeta - 2.0 * f15c + 2.0;
            fex += zeta * zeta * (fex2 + fex3 * zeta);
        }
        if (scaledSatAlt >= 1500.0) {
            fex = CHT[0] + CHT[1] * f10B + CHT[2] * scaledSatAlt + CHT[3] * f10B * scaledSatAlt;
        }

        // Apply the exospheric density correction factor.
        rho *= fex;

        return rho;
    }

    /**
     * Get the local density with initial entries.
     *
     * @param dateMJD  date and time, in modified julian days and fraction
     * @param sunRA    Right Ascension of Sun (radians)
     * @param sunDecli Declination of Sun (radians)
     * @param satLon   Right Ascension of position (radians)
     * @param satLat   Geocentric latitude of position (radians)
     * @param satAlt   Height of position (m)
     * @param f10      10.7-cm Solar flux (1e<sup>-22</sup>*Watt/(m²*Hertz)).
     *                 Tabular time 1.0 day earlier
     * @param f10B     10.7-cm Solar Flux, averaged 81-day centered on the input time
     * @param ap       Geomagnetic planetary 3-hour index A<sub>p</sub>
     *                 for a tabular time 6.7 hours earlier
     * @param s10      EUV index (26-34 nm) scaled to F10. Tabular time 1 day earlier.
     * @param s10B     UV 81-day averaged centered index
     * @param xm10     MG2 index scaled to F10
     * @param xm10B    MG2 81-day ave. centered index. Tabular time 5.0 days earlier.
     * @param <T>      type of the elements
     * @return total mass-Density at input position (kg/m³)
     */
    public <T extends CalculusFieldElement<T>> T getDensity(final T dateMJD, final T sunRA, final T sunDecli,
                                                            final T satLon, final T satLat, final T satAlt,
                                                            final double f10, final double f10B,
                                                            final double ap,
                                                            final double s10, final double s10B,
                                                            final double xm10, final double xm10B) {

        if (satAlt.getReal() < ALT_MIN) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, satAlt, ALT_MIN);
        }

        final Field<T> field  = satAlt.getField();
        final T scaledSatAlt = satAlt.divide(1000.0);

        // Equation (14)
        final double tsubc = 379 + 3.353 * f10B + 0.358 * (f10 - f10B) + 2.094 * (s10 - s10B) + 0.343 * (xm10 - xm10B);

        // Equation (15)
        final T eta   = FastMath.abs(satLat.subtract(sunDecli)).multiply(0.5);
        final T theta = FastMath.abs(satLat.add(sunDecli)).multiply(0.5);

        // Equation (16)
        final T h = satLon.subtract(sunRA);
        final T tau = h.subtract(0.64577182).add(h.add(0.75049158).sin().multiply(0.10471976));
        T solarTime = FastMath.toDegrees(h.add(FastMath.PI)).divide(15.0);
        while (solarTime.getReal() >= 24) {
            solarTime = solarTime.subtract(24);
        }
        while (solarTime.getReal() < 0) {
            solarTime = solarTime.add(24);
        }

        // Equation (17)
        final T cos     = eta.cos();
        final T cosEta  = cos.square().multiply(cos.sqrt());
        final T sin     = theta.sin();
        final T sinTeta = sin.square().multiply(sin.sqrt());
        final T cosTau  = tau.multiply(0.5).cos().abs();
        final T df      = sinTeta.add(cosEta.subtract(sinTeta).multiply(cosTau).multiply(cosTau).multiply(cosTau));
        final T tsubl   = df.multiply(0.31).add(1).multiply(tsubc);

        // Equation (18)
        final double expAp = FastMath.exp(-0.08 * ap);
        final double dtg   = ap + 100. * (1. - expAp);

        // Compute correction to dTc for local solar time and lat correction
        final T dtclst = dTc(f10, solarTime, satLat, scaledSatAlt);

        // Compute the local exospheric temperature.
        final T tinf = tsubl.add(dtg).add(dtclst);

        // Equation (9)
        final T tsubx = tinf.multiply(0.02385).add(444.3807).subtract(tinf.multiply(-0.0021357).exp().multiply(392.8292));

        // Equation (11)
        final T gsubx = tsubx.subtract(183.).multiply(0.054285714);

        // The TC array will be an argument in the call to
        // XLOCAL, which evaluates Equation (10) or Equation (13)
        final T[] tc = MathArrays.buildArray(field, 4);
        tc[0] = tsubx;
        tc[1] = gsubx;
        //   A AND GSUBX/A OF Equation (13)
        tc[2] = tinf.subtract(tsubx).divide(MathUtils.SEMI_PI);
        tc[3] = gsubx.divide(tc[2]);

        // Equation (5)
        final T z1    = field.getZero().newInstance(90.);
        final T z2    = min(105.0, scaledSatAlt);
        T al          = z2.divide(z1).log();
        int n         = 1 + (int) FastMath.floor(al.getReal() / R1);
        T zr          = al.divide(n).exp();
        final T mb1   = mBar(z1);
        final T tloc1 = localTemp(z1, tc);
        T zend        = z1;
        T sub2        = field.getZero();
        T ain         = mb1.multiply(gravity(z1)).divide(tloc1);
        T mb2         = field.getZero();
        T tloc2       = field.getZero();
        T z           = field.getZero();
        T gravl       = field.getZero();

        for (int i = 0; i < n; ++i) {
            z = zend;
            zend = zr.multiply(z);
            final T dz = zend.subtract(z).multiply(0.25);
            T sum1 = ain.multiply(WT[0]);
            for (int j = 1; j < 5; ++j) {
                z = z.add(dz);
                mb2   = mBar(z);
                tloc2 = localTemp(z, tc);
                gravl = gravity(z);
                ain   = mb2.multiply(gravl).divide(tloc2);
                sum1  = sum1.add(ain.multiply(WT[j]));
            }
            sub2 = sub2.add(dz.multiply(sum1));
        }
        final double fact1 = 1000.0 / RSTAR;
        T rho = mb2.multiply(3.46e-6).multiply(tloc1).multiply(sub2.multiply(-fact1).exp()).divide(mb1.multiply(tloc2));

        // Equation (2)
        final T anm = rho.multiply(AVOGAD);
        final T an  = anm.divide(mb2);

        // Equation (3)
        T fact2  = anm.divide(28.960);
        final T[] aln = MathArrays.buildArray(field, 6);
        aln[0] = fact2.multiply(FRAC[0]).log();
        aln[3] = fact2.multiply(FRAC[2]).log();
        aln[4] = fact2.multiply(FRAC[3]).log();

        // Equation (4)
        aln[1] = fact2.multiply(1. + FRAC[1]).subtract(an).log();
        aln[2] = an.subtract(fact2).multiply(2).log();

        if (scaledSatAlt.getReal() <= 105.0) {
            // Put in negligible hydrogen for use in DO-LOOP 13
            aln[5] = aln[4].subtract(25.0);
        }
        else {
            // Equation (6)
            al   = min(500.0, scaledSatAlt).divide(z).log();
            n    = 1 + (int) FastMath.floor(al.getReal() / R2);
            zr   = al.divide(n).exp();
            sub2 = field.getZero();
            ain  = gravl.divide(tloc2);

            T tloc3 = field.getZero();
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr.multiply(z);
                final T dz = zend.subtract(z).multiply(0.25);
                T sum1 = ain.multiply(WT[0]);
                for (int j = 1; j < 5; ++j) {
                    z     = z.add(dz);
                    tloc3 = localTemp(z, tc);
                    gravl = gravity(z);
                    ain   = gravl.divide(tloc3);
                    sum1  = sum1.add(ain.multiply(WT[j]));
                }
                sub2 = sub2.add(dz.multiply(sum1));
            }

            al = max(500.0, scaledSatAlt).divide(z).log();
            final double r = (scaledSatAlt.getReal() > 500.0) ? R3 : R2;
            n = 1 + (int) FastMath.floor(al.getReal() / r);
            zr = al.divide(n).exp();
            T sum3 = field.getZero();
            T tloc4 = field.getZero();
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr.multiply(z);
                final T dz = zend.subtract(z).multiply(0.25);
                T sum1 = ain.multiply(WT[0]);
                for (int j = 1; j < 5; ++j) {
                    z = z.add(dz);
                    tloc4 = localTemp(z, tc);
                    gravl = gravity(z);
                    ain   = gravl.divide(tloc4);
                    sum1  = sum1.add(ain.multiply(WT[j]));
                }
                sum3 = sum3.add(dz.multiply(sum1));
            }
            final T altr;
            final double hSign;
            if (scaledSatAlt.getReal() <= 500.) {
                altr = tloc3.divide(tloc2).log();
                fact2 = sub2.multiply(fact1);
                hSign = 1.0;
            }
            else {
                altr = tloc4.divide(tloc2).log();
                fact2 = sub2.add(sum3).multiply(fact1);
                hSign = -1.0;
            }
            for (int i = 0; i < 5; ++i) {
                aln[i] = aln[i].subtract(altr.multiply(1.0 + ALPHA[i])).subtract(fact2.multiply(AMW[i]));
            }

            // Equation (7) - Note that in CIRA72, AL10T5 = DLOG10(T500)
            final T al10t5 = tinf.log10();
            final T alnh5 = al10t5.multiply(5.5).subtract(39.40).multiply(al10t5).add(73.13);
            aln[5] = alnh5.add(6.).multiply(AL10).add(tloc4.divide(tloc3).log().add(sum3.multiply(fact1).multiply(AMW[5])).multiply(hSign));
        }

        // Equation (24)  - J70 Seasonal-Latitudinal Variation
        T capPhi = dateMJD.subtract(36204.0).divide(365.2422);
        capPhi = capPhi.subtract(FastMath.floor(capPhi.getReal()));
        final int signum = (satLat.getReal() >= 0.) ? 1 : -1;
        final T sinLat = satLat.sin();
        final T hm90  = scaledSatAlt.subtract(90.);
        final T dlrsl = hm90.multiply(0.02).multiply(hm90.multiply(-0.045).exp()).
                        multiply(capPhi.multiply(MathUtils.TWO_PI).add(1.72).sin()).
                        multiply(signum).multiply(sinLat).multiply(sinLat);

        // Equation (23) - Computes the semiannual variation
        T dlrsa = field.getZero();
        if (z.getReal() < 2000.0) {
            // Use new semiannual model DELTA LOG RHO
            dlrsa = semian(dayOfYear(dateMJD), scaledSatAlt, f10B);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = AL10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final T dlr = dlrsl.add(dlrsa).multiply(AL10);
        for (int i = 0; i < 6; ++i) {
            aln[i] = aln[i].add(dlr);
        }

        // Compute mass-density and mean-molecular-weight and
        // convert number density logs from natural to common.
        T sumnm = field.getZero();
        for (int i = 0; i < 6; ++i) {
            sumnm = sumnm.add(aln[i].exp().multiply(AMW[i]));
        }
        rho = sumnm.divide(AVOGAD);

        // Compute the high altitude exospheric density correction factor
        T fex = field.getOne();
        if (scaledSatAlt.getReal() >= 1000.0 && scaledSatAlt.getReal() < 1500.0) {
            final T zeta = scaledSatAlt.subtract(1000.).multiply(0.002);
            final double f15c = CHT[0] + CHT[1] * f10B + CHT[2] * 1500.0 + CHT[3] * f10B * 1500.0;
            final double f15cZeta = (CHT[2] + CHT[3] * f10B) * 500.0;
            final double fex2 = 3.0 * f15c - f15cZeta - 3.0;
            final double fex3 = f15cZeta - 2.0 * f15c + 2.0;
            fex = fex.add(zeta.multiply(zeta).multiply(zeta.multiply(fex3).add(fex2)));
        }
        if (scaledSatAlt.getReal() >= 1500.0) {
            fex = scaledSatAlt.multiply(CHT[3] * f10B).add(scaledSatAlt.multiply(CHT[2])).add(CHT[0] + CHT[1] * f10B);
        }

        // Apply the exospheric density correction factor.
        rho = rho.multiply(fex);

        return rho;
    }

    /** {@inheritDoc}*/
    @Override
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame) {
        if (date.compareTo(inputParams.getMaxDate()) > 0 || date.compareTo(inputParams.getMinDate()) < 0) {
            throw new OrekitException(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE, date, inputParams.getMinDate(), inputParams.getMaxDate());
        }

        // compute modified julian days date
        final DateTimeComponents dt = date.getComponents(utc);
        final double dateMJD = dt.getDate().getMJD() +
                               dt.getTime().getSecondsInLocalDay() / Constants.JULIAN_DAY;

        // compute geodetic position
        final GeodeticPoint inBody = earth.transform(position, frame, date);

        // compute sun position
        final Frame ecef = earth.getBodyFrame();
        final Vector3D sunPos = getSunPosition(date, ecef);
        final GeodeticPoint sunInBody = earth.transform(sunPos, ecef, date);
        return getDensity(dateMJD, sunInBody.getLongitude(), sunInBody.getLatitude(), inBody.getLongitude(), inBody.getLatitude(), inBody.getAltitude(), inputParams.getF10(date),
                          inputParams.getF10B(date), inputParams.getAp(date), inputParams.getS10(date), inputParams.getS10B(date), inputParams.getXM10(date),
                          inputParams.getXM10B(date));
    }

    /** {@inheritDoc}*/
    @Override
    public <T extends CalculusFieldElement<T>> T getDensity(final FieldAbsoluteDate<T> date, final FieldVector3D<T> position, final Frame frame) {
        final AbsoluteDate dateD = date.toAbsoluteDate();
        if (dateD.compareTo(inputParams.getMaxDate()) > 0 ||
            dateD.compareTo(inputParams.getMinDate()) < 0) {
            throw new OrekitException(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE,
                                      dateD, inputParams.getMinDate(), inputParams.getMaxDate());
        }

        // compute MJD date
        final DateTimeComponents components = date.getComponents(utc);
        final T dateMJD = date.durationFrom(new FieldAbsoluteDate<>(date.getField(), components, utc))
                              .add(components.getTime().getSecondsInLocalDay())
                              .divide(Constants.JULIAN_DAY)
                              .add(components.getDate().getMJD());

        // compute geodetic position (km and °)
        final FieldGeodeticPoint<T> inBody = earth.transform(position, frame, date);

        // compute sun position
        final Frame ecef = earth.getBodyFrame();
        final FieldVector3D<T> sunPos = getSunPosition(date, ecef);
        final FieldGeodeticPoint<T> sunInBody = earth.transform(sunPos, ecef, date);
        return getDensity(dateMJD,
                          sunInBody.getLongitude(), sunInBody.getLatitude(),
                          inBody.getLongitude(), inBody.getLatitude(), inBody.getAltitude(),
                          inputParams.getF10(dateD), inputParams.getF10B(dateD),
                          inputParams.getAp(dateD), inputParams.getS10(dateD),
                          inputParams.getS10B(dateD), inputParams.getXM10(dateD),
                          inputParams.getXM10B(dateD));
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)  {
        final KinematicTransform bodyToFrame = earth.getBodyFrame().getKinematicTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, Vector3D.ZERO);
        final PVCoordinates pvFrame = bodyToFrame.transformOnlyPV(pvBody);
        return pvFrame.getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return earth.getBodyFrame();
    }

    /**
     * Compute daily temperature correction for Jacchia-Bowman model.
     *
     * @param f10         solar flux index
     * @param solTimeHour local solar time (hours 0-23.999)
     * @param satLat      sat lat (radians)
     * @param satAlt      height (km)
     * @return dTc correction
     */
    private static double dTc(final double f10, final double solTimeHour, final double satLat, final double satAlt) {

        double dTc = 0;
        final double tx = solTimeHour / 24.0;
        final double tx2 = tx * tx;
        final double tx3 = tx2 * tx;
        final double tx4 = tx3 * tx;
        final double tx5 = tx4 * tx;
        final double ycs = FastMath.cos(satLat);
        final double f = (f10 - 100.0) / 100.0;
        double h;
        double sum;

        // Calculates dTc
        if (satAlt >= 120 && satAlt <= 200) {
            final double dtc2000 = CDT_SUB[16] +
                                   CDT_SUB[17] * tx * ycs +
                                   CDT_SUB[18] * tx2 * ycs +
                                   CDT_SUB[19] * tx3 * ycs +
                                   CDT_SUB[20] * f * ycs +
                                   CDT_SUB[21] * tx * f * ycs +
                                   CDT_SUB[22] * tx2 * f * ycs;
            sum = CDT_SUB[0] +
                  BDT_SUB[1] * f +
                  CDT_SUB[2] * tx * f +
                  CDT_SUB[3] * tx2 * f +
                  CDT_SUB[4] * tx3 * f +
                  CDT_SUB[5] * tx4 * f +
                  CDT_SUB[6] * tx5 * f +
                  CDT_SUB[7] * tx * ycs +
                  CDT_SUB[8] * tx2 * ycs +
                  CDT_SUB[9] * tx3 * ycs +
                  CDT_SUB[10] * tx4 * ycs +
                  CDT_SUB[11] * tx5 * ycs +
                  CDT_SUB[12] * ycs +
                  CDT_SUB[13] * f * ycs +
                  CDT_SUB[14] * tx * f * ycs +
                  CDT_SUB[15] * tx2 * f * ycs;
            final double dtc200Dz = sum;
            final double cc = 3.0 * dtc2000 - dtc200Dz;
            final double dd = dtc2000 - cc;
            final double zp = (satAlt - 120.0) / 80.0;
            dTc = cc * zp * zp + dd * zp * zp * zp;
        }
        if (satAlt > 200.0 && satAlt <= 240.0) {
            h = (satAlt - 200.0) / 50.0;
            sum = CDT_SUB[0] * h +
                  BDT_SUB[1] * f * h +
                  CDT_SUB[2] * tx * f * h +
                  CDT_SUB[3] * tx2 * f * h +
                  CDT_SUB[4] * tx3 * f * h +
                  CDT_SUB[5] * tx4 * f * h +
                  CDT_SUB[6] * tx5 * f * h +
                  CDT_SUB[7] * tx * ycs * h +
                  CDT_SUB[8] * tx2 * ycs * h +
                  CDT_SUB[9] * tx3 * ycs * h +
                  CDT_SUB[10] * tx4 * ycs * h +
                  CDT_SUB[11] * tx5 * ycs * h +
                  CDT_SUB[12] * ycs * h +
                  CDT_SUB[13] * f * ycs * h +
                  CDT_SUB[14] * tx * f * ycs * h +
                  CDT_SUB[15] * tx2 * f * ycs * h +
                  CDT_SUB[16] +
                  CDT_SUB[17] * tx * ycs +
                  CDT_SUB[18] * tx2 * ycs +
                  CDT_SUB[19] * tx3 * ycs +
                  CDT_SUB[20] * f * ycs +
                  CDT_SUB[21] * tx * f * ycs +
                  CDT_SUB[22] * tx2 * f * ycs;
            dTc = sum;
        }
        if (satAlt > 240.0 && satAlt <= 300.0) {
            h = 40.0 / 50.0;
            sum = CDT_SUB[0] * h +
                  BDT_SUB[1] * f * h +
                  CDT_SUB[2] * tx * f * h +
                  CDT_SUB[3] * tx2 * f * h +
                  CDT_SUB[4] * tx3 * f * h +
                  CDT_SUB[5] * tx4 * f * h +
                  CDT_SUB[6] * tx5 * f * h +
                  CDT_SUB[7] * tx * ycs * h +
                  CDT_SUB[8] * tx2 * ycs * h +
                  CDT_SUB[9] * tx3 * ycs * h +
                  CDT_SUB[10] * tx4 * ycs * h +
                  CDT_SUB[11] * tx5 * ycs * h +
                  CDT_SUB[12] * ycs * h +
                  CDT_SUB[13] * f * ycs * h +
                  CDT_SUB[14] * tx * f * ycs * h +
                  CDT_SUB[15] * tx2 * f * ycs * h +
                  CDT_SUB[16] +
                  CDT_SUB[17] * tx * ycs +
                  CDT_SUB[18] * tx2 * ycs +
                  CDT_SUB[19] * tx3 * ycs +
                  CDT_SUB[20] * f * ycs +
                  CDT_SUB[21] * tx * f * ycs +
                  CDT_SUB[22] * tx2 * f * ycs;
            final double aa = sum;
            final double bb = CDT_SUB[0] +
                              BDT_SUB[1] * f +
                              CDT_SUB[2] * tx * f +
                              CDT_SUB[3] * tx2 * f +
                              CDT_SUB[4] * tx3 * f +
                              CDT_SUB[5] * tx4 * f +
                              CDT_SUB[6] * tx5 * f +
                              CDT_SUB[7] * tx * ycs +
                              CDT_SUB[8] * tx2 * ycs +
                              CDT_SUB[9] * tx3 * ycs +
                              CDT_SUB[10] * tx4 * ycs +
                              CDT_SUB[11] * tx5 * ycs +
                              CDT_SUB[12] * ycs +
                              CDT_SUB[13] * f * ycs +
                              CDT_SUB[14] * tx * f * ycs +
                              CDT_SUB[15] * tx2 * f * ycs;
            h = 300.0 / 100.0;
            sum = BDT_SUB[0] +
                  BDT_SUB[1] * f +
                  BDT_SUB[2] * tx * f +
                  BDT_SUB[3] * tx2 * f +
                  BDT_SUB[4] * tx3 * f +
                  BDT_SUB[5] * tx4 * f +
                  BDT_SUB[6] * tx5 * f +
                  BDT_SUB[7] * tx * ycs +
                  BDT_SUB[8] * tx2 * ycs +
                  BDT_SUB[9] * tx3 * ycs +
                  BDT_SUB[10] * tx4 * ycs +
                  BDT_SUB[11] * tx5 * ycs +
                  BDT_SUB[12] * h * ycs +
                  BDT_SUB[13] * tx * h * ycs +
                  BDT_SUB[14] * tx2 * h * ycs +
                  BDT_SUB[15] * tx3 * h * ycs +
                  BDT_SUB[16] * tx4 * h * ycs +
                  BDT_SUB[17] * tx5 * h * ycs +
                  BDT_SUB[18] * ycs;
            final double dtc300 = sum;
            sum = BDT_SUB[12] * ycs + BDT_SUB[13] * tx * ycs + BDT_SUB[14] * tx2 * ycs + BDT_SUB[15] * tx3 * ycs + BDT_SUB[16] * tx4 * ycs + BDT_SUB[17] * tx5 * ycs;
            final double dtc300Dz = sum;
            final double cc = 3.0 * dtc300 - dtc300Dz - 3.0 * aa - 2.0 * bb;
            final double dd = dtc300 - aa - bb - cc;
            final double zp = (satAlt - 240.0) / 60.0;
            dTc = aa + bb * zp + cc * zp * zp + dd * zp * zp * zp;
        }
        if (satAlt > 300.0 && satAlt <= 600.0) {
            h = satAlt / 100.0;
            sum = BDT_SUB[0] +
                  BDT_SUB[1] * f +
                  BDT_SUB[2] * tx * f +
                  BDT_SUB[3] * tx2 * f +
                  BDT_SUB[4] * tx3 * f +
                  BDT_SUB[5] * tx4 * f +
                  BDT_SUB[6] * tx5 * f +
                  BDT_SUB[7] * tx * ycs +
                  BDT_SUB[8] * tx2 * ycs +
                  BDT_SUB[9] * tx3 * ycs +
                  BDT_SUB[10] * tx4 * ycs +
                  BDT_SUB[11] * tx5 * ycs +
                  BDT_SUB[12] * h * ycs +
                  BDT_SUB[13] * tx * h * ycs +
                  BDT_SUB[14] * tx2 * h * ycs +
                  BDT_SUB[15] * tx3 * h * ycs +
                  BDT_SUB[16] * tx4 * h * ycs +
                  BDT_SUB[17] * tx5 * h * ycs +
                  BDT_SUB[18] * ycs;
            dTc = sum;
        }
        if (satAlt > 600.0 && satAlt <= 800.0) {
            final double zp = (satAlt - 600.0) / 100.0;
            final double hp = 600.0 / 100.0;
            final double aa = BDT_SUB[0] +
                              BDT_SUB[1] * f +
                              BDT_SUB[2] * tx * f +
                              BDT_SUB[3] * tx2 * f +
                              BDT_SUB[4] * tx3 * f +
                              BDT_SUB[5] * tx4 * f +
                              BDT_SUB[6] * tx5 * f +
                              BDT_SUB[7] * tx * ycs +
                              BDT_SUB[8] * tx2 * ycs +
                              BDT_SUB[9] * tx3 * ycs +
                              BDT_SUB[10] * tx4 * ycs +
                              BDT_SUB[11] * tx5 * ycs +
                              BDT_SUB[12] * hp * ycs +
                              BDT_SUB[13] * tx * hp * ycs +
                              BDT_SUB[14] * tx2 * hp * ycs +
                              BDT_SUB[15] * tx3 * hp * ycs +
                              BDT_SUB[16] * tx4 * hp * ycs +
                              BDT_SUB[17] * tx5 * hp * ycs +
                              BDT_SUB[18] * ycs;
            final double bb = BDT_SUB[12] * ycs + BDT_SUB[13] * tx * ycs + BDT_SUB[14] * tx2 * ycs + BDT_SUB[15] * tx3 * ycs + BDT_SUB[16] * tx4 * ycs + BDT_SUB[17] * tx5 * ycs;
            final double cc = -(3.0 * aa + 4.0 * bb) / 4.0;
            final double dd = (aa + bb) / 4.0;
            dTc = aa + bb * zp + cc * zp * zp + dd * zp * zp * zp;
        }
        return dTc;
    }

    /**
     * Compute daily temperature correction for Jacchia-Bowman model.
     *
     * @param f10         solar flux index
     * @param solTimeHour local solar time (hours 0-23.999)
     * @param satLat      sat lat (radians)
     * @param satAlt      height (km)
     * @param <T>         type of the field elements
     * @return dTc correction
     */
    private static <T extends CalculusFieldElement<T>> T dTc(final double f10, final T solTimeHour,
                                                             final T satLat, final T satAlt) {

        final T zero = solTimeHour.getField().getZero();
        T dTc = zero;
        final T tx = solTimeHour.divide(24.0);
        final T tx2 = tx.multiply(tx);
        final T tx3 = tx2.multiply(tx);
        final T tx4 = tx3.multiply(tx);
        final T tx5 = tx4.multiply(tx);
        final T ycs = satLat.cos();
        final double f = (f10 - 100.0) / 100.0;
        T h;
        T sum;

        // Calculates dTc
        if (satAlt.getReal() >= 120 && satAlt.getReal() <= 200) {
            final T dtc200 = tx.multiply(ycs).multiply(CDT_SUB[17]).
                             add(tx2.multiply(ycs.multiply(CDT_SUB[18])).
                             add(tx3.multiply(ycs).multiply(CDT_SUB[19])).
                             add(ycs.multiply(f).multiply(CDT_SUB[20])).
                             add(tx.multiply(f).multiply(ycs).multiply(CDT_SUB[21])).
                             add(tx2.multiply(f).multiply(ycs).multiply(CDT_SUB[22])).
                             add(CDT_SUB[16]));
            sum = tx.multiply(f).multiply(CDT_SUB[2]).
                  add(tx2.multiply(f).multiply(CDT_SUB[3])).
                  add(tx3.multiply(f).multiply(CDT_SUB[4])).
                  add(tx4.multiply(f).multiply(CDT_SUB[5])).
                  add(tx5.multiply(f).multiply(CDT_SUB[6])).
                  add(tx.multiply(ycs).multiply(CDT_SUB[7])).
                  add(tx2.multiply(ycs).multiply(CDT_SUB[8])).
                  add(tx3.multiply(ycs).multiply(CDT_SUB[9])).
                  add(tx4.multiply(ycs).multiply(CDT_SUB[10])).
                  add(tx5.multiply(ycs).multiply(CDT_SUB[11])).
                  add(ycs.multiply(CDT_SUB[12])).
                  add(ycs.multiply(f).multiply(CDT_SUB[13])).
                  add(tx.multiply(f).multiply(ycs).multiply(CDT_SUB[14])).
                  add(tx2.multiply(f).multiply(ycs).multiply(CDT_SUB[15])).
                  add(CDT_SUB[0] + BDT_SUB[1] * f);
            final T dtc2000z = sum;
            final T cc = dtc200.multiply(3.0).subtract(dtc2000z);
            final T dd = dtc200.subtract(cc);
            final T zp = satAlt.subtract(120.0).divide(80.0);
            dTc = cc.multiply(zp).multiply(zp).add(dd.multiply(zp).multiply(zp).multiply(zp));
        }
        if (satAlt.getReal() > 200.0 && satAlt.getReal() <= 240.0) {
            h = satAlt.subtract(200.0).divide(50.0);
            sum = h.multiply(CDT_SUB[0]).
                  add(h.multiply(BDT_SUB[1] * f)).
                  add(h.multiply(tx).multiply(CDT_SUB[2] * f)).
                  add(h.multiply(tx2).multiply(CDT_SUB[3] * f)).
                  add(h.multiply(tx3).multiply(CDT_SUB[4] * f)).
                  add(h.multiply(tx4).multiply(CDT_SUB[5] * f)).
                  add(h.multiply(tx5).multiply(CDT_SUB[6] * f)).
                  add(h.multiply(tx).multiply(ycs).multiply(CDT_SUB[7])).
                  add(h.multiply(tx2).multiply(ycs).multiply(CDT_SUB[8])).
                  add(h.multiply(tx3).multiply(ycs).multiply(CDT_SUB[9])).
                  add(h.multiply(tx4).multiply(ycs).multiply(CDT_SUB[10])).
                  add(h.multiply(tx5).multiply(ycs).multiply(CDT_SUB[11])).
                  add(h.multiply(ycs).multiply(CDT_SUB[12])).
                  add(h.multiply(ycs).multiply(CDT_SUB[13] * f)).
                  add(h.multiply(tx).multiply(ycs).multiply(CDT_SUB[14] * f)).
                  add(h.multiply(tx2).multiply(ycs).multiply(CDT_SUB[15] * f)).
                  add(CDT_SUB[16]).
                  add(ycs.multiply(tx).multiply(CDT_SUB[17])).
                  add(ycs.multiply(tx2).multiply(CDT_SUB[18])).
                  add(ycs.multiply(tx3).multiply(CDT_SUB[19])).
                  add(ycs.multiply(CDT_SUB[20] * f)).
                  add(ycs.multiply(tx).multiply(CDT_SUB[21] * f)).
                  add(ycs.multiply(tx2).multiply(CDT_SUB[22] * f));
            dTc = sum;
        }
        if (satAlt.getReal() > 240.0 && satAlt.getReal() <= 300.0) {
            h = zero.add(40.0 / 50.0);
            sum = h.multiply(CDT_SUB[0]).
                  add(h.multiply(BDT_SUB[1] * f)).
                  add(h.multiply(tx).multiply(CDT_SUB[2] * f)).
                  add(h.multiply(tx2).multiply(CDT_SUB[3] * f)).
                  add(h.multiply(tx3).multiply(CDT_SUB[4] * f)).
                  add(h.multiply(tx4).multiply(CDT_SUB[5] * f)).
                  add(h.multiply(tx5).multiply(CDT_SUB[6] * f)).
                  add(h.multiply(tx).multiply(ycs).multiply( CDT_SUB[7])).
                  add(h.multiply(tx2).multiply(ycs).multiply(CDT_SUB[8])).
                  add(h.multiply(tx3).multiply(ycs).multiply(CDT_SUB[9])).
                  add(h.multiply(tx4).multiply(ycs).multiply(CDT_SUB[10])).
                  add(h.multiply(tx5).multiply(ycs).multiply(CDT_SUB[11])).
                  add(h.multiply(ycs).multiply(CDT_SUB[12])).
                  add(h.multiply(ycs).multiply(CDT_SUB[13] * f)).
                  add(h.multiply(tx).multiply(ycs).multiply(CDT_SUB[14] * f)).
                  add(h.multiply(tx2).multiply(ycs).multiply(CDT_SUB[15] * f)).
                  add(CDT_SUB[16]).
                  add(ycs.multiply(tx).multiply(CDT_SUB[17])).
                  add(ycs.multiply(tx2).multiply(CDT_SUB[18])).
                  add(ycs.multiply(tx3).multiply(CDT_SUB[19])).
                  add(ycs.multiply(CDT_SUB[20] * f)).
                  add(ycs.multiply(tx).multiply(CDT_SUB[21] * f)).
                  add(ycs.multiply(tx2).multiply(CDT_SUB[22] * f));
            final T aa = sum;
            final T bb = tx.multiply(f * CDT_SUB[2]).
                         add(tx2.multiply(f * CDT_SUB[3])).
                         add(tx3.multiply(f * CDT_SUB[4])).
                         add(tx4.multiply(f * CDT_SUB[5])).
                         add(tx5.multiply(f * CDT_SUB[6])).
                         add(tx.multiply(ycs).multiply(CDT_SUB[7])).
                         add(tx2.multiply(ycs).multiply(CDT_SUB[8])).
                         add(tx3.multiply(ycs).multiply(CDT_SUB[9])).
                         add(tx4.multiply(ycs).multiply(CDT_SUB[10])).
                         add(tx5.multiply(ycs).multiply(CDT_SUB[11])).
                         add(ycs.multiply(CDT_SUB[12])).
                         add(ycs.multiply(CDT_SUB[13] * f)).
                         add(ycs.multiply(tx).multiply(CDT_SUB[14] *  f)).
                         add(ycs.multiply(tx2).multiply(CDT_SUB[15] *  f)).
                         add(CDT_SUB[0] + BDT_SUB[1] * f);
            h = zero.add(300.0 / 100.0);
            sum = tx.multiply(f).multiply(BDT_SUB[2]).
                  add(tx2.multiply(f).multiply(BDT_SUB[3])).
                  add(tx3.multiply(f).multiply(BDT_SUB[4])).
                  add(tx4.multiply(f).multiply(BDT_SUB[5])).
                  add(tx5.multiply(f).multiply(BDT_SUB[6])).
                  add(tx.multiply(ycs).multiply(BDT_SUB[7])).
                  add(tx2.multiply(ycs).multiply(BDT_SUB[8])).
                  add(tx3.multiply(ycs).multiply(BDT_SUB[9])).
                  add(tx4.multiply(ycs).multiply(BDT_SUB[10])).
                  add(tx5.multiply(ycs).multiply(BDT_SUB[11])).
                  add(ycs.multiply(h).multiply(BDT_SUB[12])).
                  add(ycs.multiply(tx).multiply(h).multiply(BDT_SUB[13])).
                  add(ycs.multiply(tx2).multiply(h).multiply(BDT_SUB[14])).
                  add(ycs.multiply(tx3).multiply(h).multiply(BDT_SUB[15])).
                  add(ycs.multiply(tx4).multiply(h).multiply(BDT_SUB[16])).
                  add(ycs.multiply(tx5).multiply(h).multiply(BDT_SUB[17])).
                  add(ycs.multiply(BDT_SUB[18])).
                  add(BDT_SUB[0] + BDT_SUB[1] * f);
            final T dtc300 = sum;
            sum = ycs.multiply(BDT_SUB[12]).
                  add(ycs.multiply(tx).multiply(BDT_SUB[13])).
                  add(ycs.multiply(tx2).multiply(BDT_SUB[14])).
                  add(ycs.multiply(tx3).multiply(BDT_SUB[15])).
                  add(ycs.multiply(tx4).multiply(BDT_SUB[16])).
                  add(ycs.multiply(tx5).multiply(BDT_SUB[17]));
            final T dtc3000z = sum;
            final T cc = dtc300.multiply(3.0).subtract(dtc3000z).subtract(aa.multiply(3.0)).subtract(bb.multiply(2.0));
            final T dd = dtc300.subtract(aa).subtract(bb).subtract(cc);
            final T zp = satAlt.subtract(240.0).divide(60.0);
            dTc = aa.add(bb.multiply(zp)).add(cc.multiply(zp).multiply(zp)).add(dd.multiply(zp).multiply(zp).multiply(zp));
        }
        if (satAlt.getReal() > 300.0 && satAlt.getReal() <= 600.0) {
            h = satAlt.divide(100.0);
            sum = tx.multiply(f).multiply(BDT_SUB[2]).
                  add(tx2.multiply(f).multiply(BDT_SUB[3])).
                  add(tx3.multiply(f).multiply(BDT_SUB[4])).
                  add(tx4.multiply(f).multiply(BDT_SUB[5])).
                  add(tx5.multiply(f).multiply(BDT_SUB[6])).
                  add(tx.multiply(ycs).multiply(BDT_SUB[7])).
                  add(tx2.multiply(ycs).multiply(BDT_SUB[8])).
                  add(tx3.multiply(ycs).multiply(BDT_SUB[9])).
                  add(tx4.multiply(ycs).multiply(BDT_SUB[10])).
                  add(tx5.multiply(ycs).multiply(BDT_SUB[11])).
                  add(ycs.multiply(h).multiply(BDT_SUB[12])).
                  add(ycs.multiply(tx).multiply(h).multiply(BDT_SUB[13])).
                  add(ycs.multiply(tx2).multiply(h).multiply(BDT_SUB[14])).
                  add(ycs.multiply(tx3).multiply(h).multiply(BDT_SUB[15])).
                  add(ycs.multiply(tx4).multiply(h).multiply(BDT_SUB[16])).
                  add(ycs.multiply(tx5).multiply(h).multiply(BDT_SUB[17])).
                  add(ycs.multiply(BDT_SUB[18])).
                  add(BDT_SUB[0] + BDT_SUB[1] * f);
            dTc = sum;
        }
        if (satAlt.getReal() > 600.0 && satAlt.getReal() <= 800.0) {
            final T zp = satAlt.subtract(600.0).divide(100.0);
            final double hp = 600.0 / 100.0;
            final T aa = tx.multiply(f).multiply(BDT_SUB[2]).
                         add(tx2.multiply(f).multiply(BDT_SUB[3])).
                         add(tx3.multiply(f).multiply(BDT_SUB[4])).
                         add(tx4.multiply(f).multiply(BDT_SUB[5])).
                         add(tx5.multiply(f).multiply(BDT_SUB[6])).
                         add(tx.multiply(ycs).multiply(BDT_SUB[7])).
                         add(tx2.multiply(ycs).multiply(BDT_SUB[8])).
                         add(tx3.multiply(ycs).multiply(BDT_SUB[9])).
                         add(tx4.multiply(ycs).multiply(BDT_SUB[10])).
                         add(tx5.multiply(ycs).multiply(BDT_SUB[11])).
                         add(ycs.multiply(hp).multiply(BDT_SUB[12])).
                         add(ycs.multiply(tx).multiply(hp).multiply(BDT_SUB[13])).
                         add(ycs.multiply(tx2).multiply(hp).multiply(BDT_SUB[14])).
                         add(ycs.multiply(tx3).multiply(hp).multiply(BDT_SUB[15])).
                         add(ycs.multiply(tx4).multiply(hp).multiply(BDT_SUB[16])).
                         add(ycs.multiply(tx5).multiply(hp).multiply(BDT_SUB[17])).
                         add(ycs.multiply(BDT_SUB[18])).
                         add(BDT_SUB[0] + BDT_SUB[1] * f);
            final T bb = ycs.multiply(BDT_SUB[12]).
                         add(tx.multiply(ycs).multiply(BDT_SUB[13])).
                         add(tx2.multiply(ycs).multiply(BDT_SUB[14])).
                         add(tx3.multiply(ycs).multiply(BDT_SUB[15])).
                         add(tx4.multiply(ycs).multiply(BDT_SUB[16])).
                         add(tx5.multiply(ycs).multiply(BDT_SUB[17]));
            final T cc = aa.multiply(3.0).add(bb.multiply(4.0)).negate().divide(4.0);
            final T dd = aa.add(bb).divide(4.0);
            dTc = aa.add(bb.multiply(zp)).add(cc.multiply(zp).multiply(zp)).add(dd.multiply(zp).multiply(zp).multiply(zp));
        }

        return dTc;
    }

    /** Evaluates mean molecualr mass - Equation (1).
     * @param z altitude (km)
     * @return mean molecular mass
     */
    private static double mBar(final double z) {
        final double dz = z - 100.;
        double amb = CXAMB[6];
        for (int i = 5; i >= 0; --i) {
            amb = dz * amb + CXAMB[i];
        }
        return amb;
    }

    /** Evaluates mean molecualr mass - Equation (1).
     * @param z altitude (km)
     * @return mean molecular mass
     * @param <T> type of the field elements
     */
    private static <T extends CalculusFieldElement<T>>  T mBar(final T z) {
        final T dz = z.subtract(100.);
        T amb = z.getField().getZero().newInstance(CXAMB[6]);
        for (int i = 5; i >= 0; --i) {
            amb = dz.multiply(amb).add(CXAMB[i]);
        }
        return amb;
    }

    /** Evaluates the local temperature, Eq. (10) or (13) depending on altitude.
     * @param z  altitude
     * @param tc tc array ???
     * @return equation (10) value
     */
    private static double localTemp(final double z, final double[] tc) {
        final double dz = z - 125;
        if (dz <= 0) {
            return ((-9.8204695e-6 * dz - 7.3039742e-4) * dz * dz + 1.0) * dz * tc[1] + tc[0];
        }
        else {
            return tc[0] + tc[2] * FastMath.atan(tc[3] * dz * (1 + 4.5e-6 * FastMath.pow(dz, 2.5)));
        }
    }

    /** Evaluates the local temperature, Eq. (10) or (13) depending on altitude.
     * @param z altitude
     * @param tc tc array
     * @return temperature profile
     * @param <T> type of the field elements
     */
    private static <T extends CalculusFieldElement<T>>  T localTemp(final T z, final T[] tc) {
        final T dz = z.subtract(125.);
        if (dz.getReal() <= 0.) {
            return dz.multiply(-9.8204695e-6).subtract(7.3039742e-4).multiply(dz).multiply(dz).add(1.0).multiply(dz).multiply(tc[1]).add(tc[0]);
        } else {
            return dz.multiply(dz).multiply(dz.sqrt()).multiply(4.5e-6).add(1).multiply(dz).multiply(tc[3]).atan().multiply(tc[2]).add(tc[0]);
        }
    }

    /**
     * Evaluates the gravity at the altitude - Equation (8).
     * @param z altitude (km)
     * @return the gravity field (m/s2)
     */
    private static double gravity(final double z) {
        final double temp = 1.0 + z / EARTH_RADIUS;
        return Constants.G0_STANDARD_GRAVITY / (temp * temp);
    }

    /** Evaluates the gravity at the altitude - Equation (8).
     * @param z altitude (km)
     * @return the gravity (m/s2)
     * @param <T> type of the field elements
     */
    private static <T extends CalculusFieldElement<T>> T gravity(final T z) {
        final T tmp = z.divide(EARTH_RADIUS).add(1);
        return tmp.multiply(tmp).reciprocal().multiply(Constants.G0_STANDARD_GRAVITY);
    }

    /** Compute semi-annual variation (delta log(rho)).
     * @param day    day of year
     * @param height height (km)
     * @param f10Bar average 81-day centered f10
     * @return semi-annual variation
     */
    private static double semian(final double day, final double height, final double f10Bar) {

        final double f10Bar2 = f10Bar * f10Bar;
        final double htz = height / 1000.0;

        // SEMIANNUAL AMPLITUDE
        final double fzz = FZM[0] + FZM[1] * f10Bar + FZM[2] * f10Bar * htz + FZM[3] * f10Bar * htz * htz + FZM[4] * f10Bar * f10Bar * htz + FZM[5] * f10Bar * f10Bar * htz * htz;

        // SEMIANNUAL PHASE FUNCTION
        final double tau = MathUtils.TWO_PI * (day - 1.0) / 365;
        final double sin1P = FastMath.sin(tau);
        final double cos1P = FastMath.cos(tau);
        final double sin2P = FastMath.sin(2.0 * tau);
        final double cos2P = FastMath.cos(2.0 * tau);
        final double sin3P = FastMath.sin(3.0 * tau);
        final double cos3P = FastMath.cos(3.0 * tau);
        final double sin4P = FastMath.sin(4.0 * tau);
        final double cos4P = FastMath.cos(4.0 * tau);
        final double gtz = GTM[0] +
                           GTM[1] * sin1P +
                           GTM[2] * cos1P +
                           GTM[3] * sin2P +
                           GTM[4] * cos2P +
                           GTM[5] * sin3P +
                           GTM[6] * cos3P +
                           GTM[7] * sin4P +
                           GTM[8] * cos4P +
                           GTM[9] * f10Bar +
                           GTM[10] * f10Bar * sin1P +
                           GTM[11] * f10Bar * cos1P +
                           GTM[12] * f10Bar * sin2P +
                           GTM[13] * f10Bar * cos2P +
                           GTM[14] * f10Bar * sin3P +
                           GTM[15] * f10Bar * cos3P +
                           GTM[16] * f10Bar * sin4P +
                           GTM[17] * f10Bar * cos4P +
                           GTM[18] * f10Bar2 +
                           GTM[19] * f10Bar2 * sin1P +
                           GTM[20] * f10Bar2 * cos1P +
                           GTM[21] * f10Bar2 * sin2P +
                           GTM[22] * f10Bar2 * cos2P;

        return FastMath.max(1.0e-6, fzz) * gtz;
    }

    /** Compute semi-annual variation (delta log(rho)).
     * @param doy    day of year
     * @param height height (km)
     * @param f10Bar average 81-day centered f10
     * @param <T> type of the field elements
     * @return semi-annual variation
     */
    private static <T extends CalculusFieldElement<T>> T semian(final T doy, final T height, final double f10Bar) {

        final double f10Bar2 = f10Bar * f10Bar;
        final T htz = height.divide(1000.0);

        // SEMIANNUAL AMPLITUDE
        final T fzz = htz.multiply(FZM[2] * f10Bar).add(htz.square().multiply(FZM[3] * f10Bar)).add(htz.multiply(FZM[4] * f10Bar * f10Bar)).add(htz.square().multiply(FZM[5] * f10Bar * f10Bar)).add(FZM[0] + FZM[1] * f10Bar);

        // SEMIANNUAL PHASE FUNCTION
        final T tau   = doy.subtract(1).divide(365).multiply(MathUtils.TWO_PI);
        final FieldSinCos<T> sc1P = FastMath.sinCos(tau);
        final FieldSinCos<T> sc2P = FastMath.sinCos(tau.multiply(2.0));
        final FieldSinCos<T> sc3P = FastMath.sinCos(tau.multiply(3.0));
        final FieldSinCos<T> sc4P = FastMath.sinCos(tau.multiply(4.0));
        final T gtz = sc1P.sin().multiply(GTM[1]).add(
                      sc1P.cos().multiply(GTM[2])).add(
                      sc2P.sin().multiply(GTM[3])).add(
                      sc2P.cos().multiply(GTM[4])).add(
                      sc3P.sin().multiply(GTM[5])).add(
                      sc3P.cos().multiply(GTM[6])).add(
                      sc4P.sin().multiply(GTM[7])).add(
                      sc4P.cos().multiply(GTM[8])).add(
                      GTM[9] * f10Bar).add(
                      sc1P.sin().multiply(f10Bar).multiply(GTM[10])).add(
                      sc1P.cos().multiply(f10Bar).multiply(GTM[11])).add(
                      sc2P.sin().multiply(f10Bar).multiply(GTM[12])).add(
                      sc2P.cos().multiply(f10Bar).multiply(GTM[13])).add(
                      sc3P.sin().multiply(f10Bar).multiply(GTM[14])).add(
                      sc3P.cos().multiply(f10Bar).multiply(GTM[15])).add(
                      sc4P.sin().multiply(f10Bar).multiply(GTM[16])).add(
                      sc4P.cos().multiply(f10Bar).multiply(GTM[17])).add(
                      GTM[18] * f10Bar2).add(
                      sc1P.sin().multiply(f10Bar2).multiply(GTM[19])).add(
                      sc1P.cos().multiply(f10Bar2).multiply(GTM[20])).add(
                      sc2P.sin().multiply(f10Bar2).multiply(GTM[21])).add(
                      sc2P.cos().multiply(f10Bar2).multiply(GTM[22])).add(GTM[0]);

        return fzz.getReal() > 1.0e-6 ? gtz.multiply(fzz) : gtz.multiply(1.0e-6);
    }

    /** Compute day of year.
     * @param dateMJD Modified Julian date
     * @return the number days in year
     */
    private static double dayOfYear(final double dateMJD) {
        final double d1950 = dateMJD - 33281.0;

        int iyday = (int) d1950;
        final double frac = d1950 - iyday;
        iyday = iyday + 364;

        int itemp = iyday / 1461;

        iyday = iyday - itemp * 1461;
        itemp = iyday / 365;
        if (itemp >= 3) {
            itemp = 3;
        }
        iyday = iyday - 365 * itemp + 1;
        return iyday + frac;
    }

    /** Compute day of year.
     * @param dateMJD Modified Julian date
     * @param <T> type of the field elements
     * @return the number days in year
     */
    private static <T extends CalculusFieldElement<T>> T dayOfYear(final T dateMJD) {
        final T d1950 = dateMJD.subtract(33281.0);

        int iyday = (int) d1950.getReal();
        final T frac = d1950.subtract(iyday);
        iyday = iyday + 364;

        int itemp = iyday / 1461;

        iyday = iyday - itemp * 1461;
        itemp = iyday / 365;
        if (itemp >= 3) {
            itemp = 3;
        }
        iyday = iyday - 365 * itemp + 1;
        return frac.add(iyday);
    }

    /** Compute min of two values, one double and one field element.
     * @param d double value
     * @param f field element
     * @param <T> type of the field elements
     * @return min value
     */
    private <T extends CalculusFieldElement<T>> T min(final double d, final T f) {
        return (f.getReal() > d) ? f.getField().getZero().newInstance(d) : f;
    }

    /** Compute max of two values, one double and one field element.
     * @param d double value
     * @param f field element
     * @param <T> type of the field elements
     * @return max value
     */
    private <T extends CalculusFieldElement<T>> T max(final double d, final T f) {
        return (f.getReal() <= d) ? f.getField().getZero().newInstance(d) : f;
    }

}
