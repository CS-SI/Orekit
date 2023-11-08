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

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
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
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;

/** This is the realization of the Jacchia-Bowman 2008 atmospheric model.
 * <p>
 * It is described in the paper:<br>
 * <a href="http://sol.spacenvironment.net/~JB2008/pubs/AIAA_2008-6438_JB2008_Model.pdf">A
 * New Empirical Thermospheric Density Model JB2008 Using New Solar Indices</a><br>
 * <i>Bruce R. Bowman &amp; al.</i><br>
 * AIAA 2008-6438<br>
 * </p>
 * <p>
 * Two computation methods are proposed to the user:
 * <ul>
 * <li>one OREKIT independent and compliant with initial FORTRAN routine entry values:
 *     {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double)}. </li>
 * <li>one compliant with OREKIT Atmosphere interface, necessary to the
 *     {@link org.orekit.forces.drag.DragForce drag force model} computation.</li>
 * </ul>
 * <p>
 * This model provides dense output for all altitudes and positions. Output data are :
 * <ul>
 * <li>Exospheric Temperature above Input Position (deg K)</li>
 * <li>Temperature at Input Position (deg K)</li>
 * <li>Total Mass-Density at Input Position (kg/m³)</li>
 * </ul>
 * <p>
 * The model needs geographical and time information to compute general values,
 * but also needs space weather data : mean and daily solar flux, retrieved through
 * different indices, and planetary geomagnetic indices.<br>
 * More information on these indices can be found on the <a
 * href="http://sol.spacenvironment.net/~JB2008/indices.html">
 * official JB2008 website.</a>
 * </p>
 *
 * @author Bruce R Bowman (HQ AFSPC, Space Analysis Division), 2008: FORTRAN routine
 * @author Pascal Parraud (java translation)
 */
public class JB2008 implements Atmosphere {

    /** Serializable UID. */
    private static final long serialVersionUID = -4201270765122160831L;

    /** Minimum altitude (m) for JB2008 use. */
    private static final double ALT_MIN = 90000.;

    /** Earth radius (km). */
    private static final double EARTH_RADIUS = 6356.766;

    /** Natural logarithm of 10.0. */
    private static final double LOG10  = FastMath.log(10.);

    /** Avogadro's number in mks units (molecules/kmol). */
    private static final double AVOGAD = 6.02257e26;

    /** Universal gas-constant in mks units (joules/K/kmol). */
    private static final double RSTAR  = 8.31432;

    /** The alpha are the thermal diffusion coefficients in Equation (6). */
    private static final double[] ALPHA = {
        0, 0, 0, 0, -0.38
    };

    /** Molecular weights in order: N2, O2, O, Ar, He and H. */
    private static final double[] AMW = {
        28.0134, 31.9988, 15.9994, 39.9480, 4.0026, 1.00797
    };

    /** The FRAC are the assumed sea-level volume fractions in order: N2, O2, Ar, and He. */
    private static final double[] FRAC = {
        0.78110, 0.20955, 9.3400e-3, 1.2890e-5
    };

    /** Value used to establish height step sizes in the regime 90km to 105km. */
    private static final double R1 = 0.010;

    /** Value used to establish height step sizes in the regime 105km to 500km. */
    private static final double R2 = 0.025;

    /** Value used to establish height step sizes in the regime above 500km. */
    private static final double R3 = 0.075;

    /** Weights for the Newton-Cotes five-points quadrature formula. */
    private static final double[] WT = {
        0.311111111111111, 1.422222222222222, 0.533333333333333, 1.422222222222222, 0.311111111111111
    };

    /** Coefficients for high altitude density correction. */
    private static final double[] CHT = {
        0.22, -0.20e-02, 0.115e-02, -0.211e-05
    };

    /** FZ global model values (1997-2006 fit).  */
    private static final double[] FZM = {
        0.2689e+00, -0.1176e-01, 0.2782e-01, -0.2782e-01, 0.3470e-03
    };

    /** GT global model values (1997-2006 fit). */
    private static final double[] GTM = {
        -0.3633e+00, 0.8506e-01,  0.2401e+00, -0.1897e+00, -0.2554e+00,
        -0.1790e-01, 0.5650e-03, -0.6407e-03, -0.3418e-02, -0.1252e-02
    };

    /** Mbar polynomial coeffcients. */
    private static final double[] CMB = {
        28.15204, -8.5586e-2, +1.2840e-4, -1.0056e-5, -1.0210e-5, +1.5044e-6, +9.9826e-8
    };

    /** DTC relative data. */
    private static final double[] BDTC = {
        -0.457512297e+01, -0.512114909e+01, -0.693003609e+02,
        0.203716701e+03,  0.703316291e+03, -0.194349234e+04,
        0.110651308e+04, -0.174378996e+03,  0.188594601e+04,
        -0.709371517e+04,  0.922454523e+04, -0.384508073e+04,
        -0.645841789e+01,  0.409703319e+02, -0.482006560e+03,
        0.181870931e+04, -0.237389204e+04,  0.996703815e+03,
        0.361416936e+02
    };

    /** DTC relative data. */
    private static final double[] CDTC = {
        -0.155986211e+02, -0.512114909e+01, -0.693003609e+02,
        0.203716701e+03,  0.703316291e+03, -0.194349234e+04,
        0.110651308e+04, -0.220835117e+03,  0.143256989e+04,
        -0.318481844e+04,  0.328981513e+04, -0.135332119e+04,
        0.199956489e+02, -0.127093998e+02,  0.212825156e+02,
        -0.275555432e+01,  0.110234982e+02,  0.148881951e+03,
        -0.751640284e+03,  0.637876542e+03,  0.127093998e+02,
        -0.212825156e+02,  0.275555432e+01
    };

    /** Sun position. */
    private PVCoordinatesProvider sun;

    /** External data container. */
    private JB2008InputParameters inputParams;

    /** Earth body shape. */
    private BodyShape earth;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Constructor with space environment information for internal computation.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param parameters the solar and magnetic activity data
     * @param sun the sun position
     * @param earth the earth body shape
     * @see #JB2008(JB2008InputParameters, PVCoordinatesProvider, BodyShape, TimeScale)
     */
    @DefaultDataContext
    public JB2008(final JB2008InputParameters parameters,
                  final PVCoordinatesProvider sun, final BodyShape earth) {
        this(parameters, sun, earth, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor with space environment information for internal computation.
     *
     * @param parameters the solar and magnetic activity data
     * @param sun        the sun position
     * @param earth      the earth body shape
     * @param utc        UTC time scale. Used to computed the day fraction.
     * @since 10.1
     */
    public JB2008(final JB2008InputParameters parameters,
                  final PVCoordinatesProvider sun,
                  final BodyShape earth,
                  final TimeScale utc) {
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
     * @param dateMJD date and time, in modified julian days and fraction
     * @param sunRA Right Ascension of Sun (radians)
     * @param sunDecli Declination of Sun (radians)
     * @param satLon Right Ascension of position (radians)
     * @param satLat Geocentric latitude of position (radians)
     * @param satAlt Height of position (m)
     * @param f10 10.7-cm Solar flux (1e<sup>-22</sup>*Watt/(m²*Hertz))<br>
     *        (Tabular time 1.0 day earlier)
     * @param f10B 10.7-cm Solar Flux, averaged 81-day centered on the input time<br>
     *        (Tabular time 1.0 day earlier)
     * @param s10 EUV index (26-34 nm) scaled to F10<br>
     *        (Tabular time 1 day earlier)
     * @param s10B UV 81-day averaged centered index
     *        (Tabular time 1 day earlier)
     * @param xm10 MG2 index scaled to F10<br>
     *        (Tabular time 2.0 days earlier)
     * @param xm10B MG2 81-day ave. centered index<br>
     *        (Tabular time 2.0 days earlier)
     * @param y10 Solar X-Ray &amp; Lya index scaled to F10<br>
     *        (Tabular time 5.0 days earlier)
     * @param y10B Solar X-Ray &amp; Lya 81-day ave. centered index<br>
     *        (Tabular time 5.0 days earlier)
     * @param dstdtc Temperature change computed from Dst index
     * @return total mass-Density at input position (kg/m³)
     */
    public double getDensity(final double dateMJD, final double sunRA, final double sunDecli,
                             final double satLon, final double satLat, final double satAlt,
                             final double f10, final double f10B, final double s10,
                             final double s10B, final double xm10, final double xm10B,
                             final double y10, final double y10B, final double dstdtc) {

        if (satAlt < ALT_MIN) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, satAlt, ALT_MIN);
        }

        final double altKm = satAlt / 1000.0;

        // Equation (14)
        final double fn  = FastMath.min(1.0, FastMath.pow(f10B / 240., 0.25));
        final double fsb = f10B * fn + s10B * (1. - fn);
        final double tsubc = 392.4 + 3.227 * fsb + 0.298 * (f10 - f10B) + 2.259 * (s10 - s10B) +
                             0.312 * (xm10 - xm10B) + 0.178 * (y10 - y10B);

        // Equation (15)
        final double eta   = 0.5 * FastMath.abs(satLat - sunDecli);
        final double theta = 0.5 * FastMath.abs(satLat + sunDecli);

        // Equation (16)
        final double h   = satLon - sunRA;
        final double tau = h - 0.64577182 + 0.10471976 * FastMath.sin(h + 0.75049158);
        double solarTime = FastMath.toDegrees(h + FastMath.PI) / 15.0;
        while (solarTime >= 24) {
            solarTime -= 24.;
        }
        while (solarTime < 0) {
            solarTime += 24.;
        }

        // Equation (17)
        final double cosEta  = FastMath.pow(FastMath.cos(eta), 2.5);
        final double sinTeta = FastMath.pow(FastMath.sin(theta), 2.5);
        final double cosTau  = FastMath.abs(FastMath.cos(0.5 * tau));
        final double df = sinTeta + (cosEta - sinTeta) * cosTau * cosTau * cosTau;
        final double tsubl = tsubc * (1. + 0.31 * df);

        // Compute correction to dTc for local solar time and lat correction
        final double dtclst = dTc(f10, solarTime, satLat, altKm);

        // Compute the local exospheric temperature.
        // Add geomagnetic storm effect from input dTc value
        final double[] temp = new double[2];
        temp[0] = tsubl + dstdtc;
        final double tinf = temp[0] + dtclst;

        // Equation (9)
        final double tsubx = 444.3807 + 0.02385 * tinf - 392.8292 * FastMath.exp(-0.0021357 * tinf);

        // Equation (11)
        final double gsubx = 0.054285714 * (tsubx - 183.);

        // The TC array will be an argument in the call to localTemp,
        // which evaluates Eq. (10) or (13)
        final double[] tc = new double[4];
        tc[0] = tsubx;
        tc[1] = gsubx;
        // A of Equation (13)
        tc[2] = (tinf - tsubx) * 2. / FastMath.PI;
        tc[3] = gsubx / tc[2];

        // Equation (5)
        final double z1 = 90.;
        final double z2 = FastMath.min(altKm, 105.0);
        double al = FastMath.log(z2 / z1);
        int n = 1 + (int) FastMath.floor(al / R1);
        double zr = FastMath.exp(al / n);
        final double mb1 = mBar(z1);
        final double tloc1 = localTemp(z1, tc);
        double zend  = z1;
        double sub2  = 0.;
        double ain   = mb1 * gravity(z1) / tloc1;
        double mb2   = 0;
        double tloc2 = 0;
        double z     = 0;
        double gravl = 0;

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

        double rho = 3.46e-6 * mb2 * tloc1 / FastMath.exp(sub2 / RSTAR) / (mb1 * tloc2);

        // Equation (2)
        final double anm = AVOGAD * rho;
        final double an  = anm / mb2;

        // Equation (3)
        double fact2  = anm / 28.960;
        final double[] aln = new double[6];
        aln[0] = FastMath.log(FRAC[0] * fact2);
        aln[3] = FastMath.log(FRAC[2] * fact2);
        aln[4] = FastMath.log(FRAC[3] * fact2);
        // Equation (4)
        aln[1] = FastMath.log(fact2 * (1. + FRAC[1]) - an);
        aln[2] = FastMath.log(2. * (an - fact2));

        if (altKm <= 105.0) {
            temp[1] = tloc2;
            // Put in negligible hydrogen for use in DO-LOOP 13
            aln[5] = aln[4] - 25.0;
        } else {
            // Equation (6)
            al   = FastMath.log(FastMath.min(altKm, 500.0) / z);
            n    = 1 + (int) FastMath.floor(al / R2);
            zr   = FastMath.exp(al / n);
            sub2 = 0.;
            ain  = gravl / tloc2;

            double tloc3 = 0;
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr * z;
                final double dz = 0.25 * (zend - z);
                double sum1 = WT[0] * ain;
                for (int j = 1; j < 5; ++j) {
                    z += dz;
                    tloc3 = localTemp(z, tc);
                    gravl = gravity(z);
                    ain   = gravl / tloc3;
                    sum1 += WT[j] * ain;
                }
                sub2 += dz * sum1;
            }

            al = FastMath.log(FastMath.max(altKm, 500.0) / z);
            final double r = (altKm > 500.0) ? R3 : R2;
            n = 1 + (int) FastMath.floor(al / r);
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
                    ain   = gravl / tloc4;
                    sum1 += WT[j] * ain;
                }
                sum3 += dz * sum1;
            }
            final double altr;
            final double hSign;
            if (altKm <= 500.) {
                temp[1] = tloc3;
                altr = FastMath.log(tloc3 / tloc2);
                fact2 = sub2 / RSTAR;
                hSign = 1.0;
            } else {
                temp[1] = tloc4;
                altr = FastMath.log(tloc4 / tloc2);
                fact2 = (sub2 + sum3) / RSTAR;
                hSign = -1.0;
            }
            for (int i = 0; i < 5; ++i) {
                aln[i] = aln[i] - (1.0 + ALPHA[i]) * altr - fact2 * AMW[i];
            }

            // Equation (7)
            final double al10t5 = FastMath.log10(tinf);
            final double alnh5 = (5.5 * al10t5 - 39.40) * al10t5 + 73.13;
            aln[5] = LOG10 * (alnh5 + 6.) + hSign * (FastMath.log(tloc4 / tloc3) + sum3 * AMW[5] / RSTAR);
        }

        // Equation (24) - J70 Seasonal-Latitudinal Variation
        final double capPhi = ((dateMJD - 36204.0) / 365.2422) % 1;
        final int signum = (satLat >= 0.) ? 1 : -1;
        final double sinLat = FastMath.sin(satLat);
        final double hm90  = altKm - 90.;
        final double dlrsl = 0.02 * hm90 * FastMath.exp(-0.045 * hm90) *
                             signum * FastMath.sin(MathUtils.TWO_PI * capPhi + 1.72) * sinLat * sinLat;

        // Equation (23) - Computes the semiannual variation
        double dlrsa = 0;
        if (z < 2000.0) {
            // Use new semiannual model dLog(rho)
            dlrsa = semian08(dayOfYear(dateMJD), altKm, f10B, s10B, xm10B);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = LOG10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final double dlr = LOG10 * (dlrsl + dlrsa);
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
        if (altKm >= 1000.0 && altKm < 1500.0) {
            final double zeta = (altKm - 1000.) * 0.002;
            final double f15c = CHT[0] + CHT[1] * f10B + (CHT[2] + CHT[3] * f10B) * 1500.0;
            final double f15cZeta = (CHT[2] + CHT[3] * f10B) * 500.0;
            final double fex2 = 3.0 * f15c - f15cZeta - 3.0;
            final double fex3 = f15cZeta - 2.0 * f15c + 2.0;
            fex += zeta * zeta * (fex2 + fex3 * zeta);
        } else if (altKm >= 1500.0) {
            fex = CHT[0] + CHT[1] * f10B + CHT[2] * altKm + CHT[3] * f10B * altKm;
        }

        // Apply the exospheric density correction factor.
        rho *= fex;

        return rho;
    }

    /** Get the local density with initial entries.
     * @param dateMJD date and time, in modified julian days and fraction
     * @param sunRA Right Ascension of Sun (radians)
     * @param sunDecli Declination of Sun (radians)
     * @param satLon Right Ascension of position (radians)
     * @param satLat Geocentric latitude of position (radians)
     * @param satAlt Height of position (m)
     * @param f10 10.7-cm Solar flux (1e<sup>-22</sup>*Watt/(m²*Hertz))<br>
     *        (Tabular time 1.0 day earlier)
     * @param f10B 10.7-cm Solar Flux, averaged 81-day centered on the input time<br>
     *        (Tabular time 1.0 day earlier)
     * @param s10 EUV index (26-34 nm) scaled to F10<br>
     *        (Tabular time 1 day earlier)
     * @param s10B UV 81-day averaged centered index
     *        (Tabular time 1 day earlier)
     * @param xm10 MG2 index scaled to F10<br>
     *        (Tabular time 2.0 days earlier)
     * @param xm10B MG2 81-day ave. centered index<br>
     *        (Tabular time 2.0 days earlier)
     * @param y10 Solar X-Ray &amp; Lya index scaled to F10<br>
     *        (Tabular time 5.0 days earlier)
     * @param y10B Solar X-Ray &amp; Lya 81-day ave. centered index<br>
     *        (Tabular time 5.0 days earlier)
     * @param dstdtc Temperature change computed from Dst index
     * @param <T> type of the field elements
     * @return total mass-Density at input position (kg/m³)
     */
    public <T extends CalculusFieldElement<T>> T getDensity(final T dateMJD, final T sunRA, final T sunDecli,
                                                        final T satLon, final T satLat, final T satAlt,
                                                        final double f10, final double f10B, final double s10,
                                                        final double s10B, final double xm10, final double xm10B,
                                                        final double y10, final double y10B, final double dstdtc) {

        if (satAlt.getReal() < ALT_MIN) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD,
                                      satAlt.getReal(), ALT_MIN);
        }

        final Field<T> field  = satAlt.getField();
        final T pi    = field.getOne().getPi();
        final T altKm = satAlt.divide(1000.0);

        // Equation (14)
        final double fn  = FastMath.min(1.0, FastMath.pow(f10B / 240., 0.25));
        final double fsb = f10B * fn + s10B * (1. - fn);
        final double tsubc = 392.4 + 3.227 * fsb + 0.298 * (f10 - f10B) + 2.259 * (s10 - s10B) +
                             0.312 * (xm10 - xm10B) + 0.178 * (y10 - y10B);

        // Equation (15)
        final T eta   = satLat.subtract(sunDecli).abs().multiply(0.5);
        final T theta = satLat.add(sunDecli).abs().multiply(0.5);

        // Equation (16)
        final T h   = satLon.subtract(sunRA);
        final T tau = h.subtract(0.64577182).add(h.add(0.75049158).sin().multiply(0.10471976));
        T solarTime = FastMath.toDegrees(h.add(pi)).divide(15.0);
        while (solarTime.getReal() >= 24) {
            solarTime = solarTime.subtract(24);
        }
        while (solarTime.getReal() < 0) {
            solarTime = solarTime.add(24);
        }

        // Equation (17)
        final T cos     = eta.cos();
        final T cosEta  = cos.multiply(cos).multiply(cos.sqrt());
        final T sin     = theta.sin();
        final T sinTeta = sin.multiply(sin).multiply(sin.sqrt());
        final T cosTau  = tau.multiply(0.5).cos().abs();
        final T df      = sinTeta.add(cosEta.subtract(sinTeta).multiply(cosTau).multiply(cosTau).multiply(cosTau));
        final T tsubl   = df.multiply(0.31).add(1).multiply(tsubc);

        // Compute correction to dTc for local solar time and lat correction
        final T dtclst = dTc(f10, solarTime, satLat, altKm);

        // Compute the local exospheric temperature.
        // Add geomagnetic storm effect from input dTc value
        final T[] temp = MathArrays.buildArray(field, 2);
        temp[0] = tsubl.add(dstdtc);
        final T tinf = temp[0].add(dtclst);

        // Equation (9)
        final T tsubx = tinf.multiply(0.02385).add(444.3807).subtract(tinf.multiply(-0.0021357).exp().multiply(392.8292));

        // Equation (11)
        final T gsubx = tsubx.subtract(183.).multiply(0.054285714);

        // The TC array will be an argument in the call to localTemp,
        // which evaluates Eq. (10) or (13)
        final T[] tc = MathArrays.buildArray(field, 4);
        tc[0] = tsubx;
        tc[1] = gsubx;
        // A of Equation (13)
        tc[2] = tinf.subtract(tsubx).multiply(pi.reciprocal().multiply(2.0));
        tc[3] = gsubx.divide(tc[2]);

        // Equation (5)
        final T z1 = field.getZero().add(90.);
        final T z2 = min(105.0, altKm);
        T al = z2.divide(z1).log();
        int n = 1 + (int) FastMath.floor(al.getReal() / R1);
        T zr = al.divide(n).exp();
        final T mb1 = mBar(z1);
        final T tloc1 = localTemp(z1, tc);
        T zend  = z1;
        T sub2  = field.getZero();
        T ain   = mb1.multiply(gravity(z1)).divide(tloc1);
        T mb2   = field.getZero();
        T tloc2 = field.getZero();
        T z     = field.getZero();
        T gravl = field.getZero();
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

        T rho = mb2.multiply(3.46e-6).multiply(tloc1).divide(sub2.divide(RSTAR).exp().multiply(mb1.multiply(tloc2)));

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

        if (altKm.getReal() <= 105.0) {
            temp[1] = tloc2;
            // Put in negligible hydrogen for use in DO-LOOP 13
            aln[5] = aln[4].subtract(25.0);
        } else {
            // Equation (6)
            al   = min(500.0, altKm).divide(z).log();
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
                    z = z.add(dz);
                    tloc3 = localTemp(z, tc);
                    gravl = gravity(z);
                    ain   = gravl.divide(tloc3);
                    sum1  = sum1.add(ain.multiply(WT[j]));
                }
                sub2 = sub2.add(dz.multiply(sum1));
            }

            al = max(500.0, altKm).divide(z).log();
            final double r = (altKm.getReal() > 500.0) ? R3 : R2;
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
            if (altKm.getReal() <= 500.) {
                temp[1] = tloc3;
                altr = tloc3.divide(tloc2).log();
                fact2 = sub2.divide(RSTAR);
                hSign = 1.0;
            } else {
                temp[1] = tloc4;
                altr = tloc4.divide(tloc2).log();
                fact2 = sub2.add(sum3).divide(RSTAR);
                hSign = -1.0;
            }
            for (int i = 0; i < 5; ++i) {
                aln[i] = aln[i].subtract(altr.multiply(1.0 + ALPHA[i])).subtract(fact2.multiply(AMW[i]));
            }

            // Equation (7)
            final T al10t5 = tinf.log10();
            final T alnh5 = al10t5.multiply(5.5).subtract(39.40).multiply(al10t5).add(73.13);
            aln[5] = alnh5.add(6.).multiply(LOG10).
                     add(tloc4.divide(tloc3).log().add(sum3.multiply(AMW[5] / RSTAR)).multiply(hSign));
        }

        // Equation (24) - J70 Seasonal-Latitudinal Variation
        T capPhi = dateMJD.subtract(36204.0).divide(365.2422);
        capPhi = capPhi.subtract(FastMath.floor(capPhi.getReal()));
        final int signum = (satLat.getReal() >= 0.) ? 1 : -1;
        final T sinLat = satLat.sin();
        final T hm90  = altKm.subtract(90.);
        final T dlrsl = hm90.multiply(0.02).multiply(hm90.multiply(-0.045).exp()).
                        multiply(capPhi.multiply(MathUtils.TWO_PI).add(1.72).sin()).
                        multiply(signum).multiply(sinLat).multiply(sinLat);

        // Equation (23) - Computes the semiannual variation
        T dlrsa = field.getZero();
        if (z.getReal() < 2000.0) {
            // Use new semiannual model dLog(rho)
            dlrsa = semian08(dayOfYear(dateMJD), altKm, f10B, s10B, xm10B);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = LOG10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final T dlr = dlrsl.add(dlrsa).multiply(LOG10);
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
        if (altKm.getReal() >= 1000.0 && altKm.getReal() < 1500.0) {
            final T zeta = altKm.subtract(1000.).multiply(0.002);
            final double f15c     = CHT[0] + CHT[1] * f10B + (CHT[2] + CHT[3] * f10B) * 1500.0;
            final double f15cZeta = (CHT[2] + CHT[3] * f10B) * 500.0;
            final double fex2     = 3.0 * f15c - f15cZeta - 3.0;
            final double fex3     = f15cZeta - 2.0 * f15c + 2.0;
            fex = fex.add(zeta.multiply(zeta).multiply(zeta.multiply(fex3).add(fex2)));
        } else if (altKm.getReal() >= 1500.0) {
            fex = altKm.multiply(CHT[3] * f10B).add(altKm.multiply(CHT[2])).add(CHT[0] + CHT[1] * f10B);
        }

        // Apply the exospheric density correction factor.
        rho = rho.multiply(fex);

        return rho;
    }

    /** Compute daily temperature correction for Jacchia-Bowman model.
     * @param f10 solar flux index
     * @param solarTime local solar time (hours in [0, 24[)
     * @param satLat sat lat (radians)
     * @param satAlt height (km)
     * @return dTc correction
     */
    private static double dTc(final double f10, final double solarTime,
                              final double satLat, final double satAlt) {
        double dTc = 0.;
        final double st = solarTime / 24.0;
        final double cs = FastMath.cos(satLat);
        final double fs = (f10 - 100.0) / 100.0;

        // Calculates dTc according to height
        if (satAlt >= 120 && satAlt <= 200) {
            final double dtc200 = poly2CDTC(fs, st, cs);
            final double dtc200dz = poly1CDTC(fs, st, cs);
            final double cc = 3.0 * dtc200 - dtc200dz;
            final double dd = dtc200 - cc;
            final double zp = (satAlt - 120.0) / 80.0;
            dTc = zp * zp * (cc + dd * zp);
        } else if (satAlt > 200.0 && satAlt <= 240.0) {
            final double h = (satAlt - 200.0) / 50.0;
            dTc = poly1CDTC(fs, st, cs) * h + poly2CDTC(fs, st, cs);
        } else if (satAlt > 240.0 && satAlt <= 300.0) {
            final double h = 0.8;
            final double bb = poly1CDTC(fs, st, cs);
            final double aa = bb * h + poly2CDTC(fs, st, cs);
            final double p2BDT = poly2BDTC(st);
            final double dtc300 = poly1BDTC(fs, st, cs, 3 * p2BDT);
            final double dtc300dz = cs * p2BDT;
            final double cc = 3.0 * dtc300 - dtc300dz - 3.0 * aa - 2.0 * bb;
            final double dd = dtc300 - aa - bb - cc;
            final double zp = (satAlt - 240.0) / 60.0;
            dTc = aa + zp * (bb + zp * (cc + zp * dd));
        } else if (satAlt > 300.0 && satAlt <= 600.0) {
            final double h = satAlt / 100.0;
            dTc = poly1BDTC(fs, st, cs, h * poly2BDTC(st));
        } else if (satAlt > 600.0 && satAlt <= 800.0) {
            final double poly2 = poly2BDTC(st);
            final double aa = poly1BDTC(fs, st, cs, 6 * poly2);
            final double bb = cs * poly2;
            final double cc = -(3.0 * aa + 4.0 * bb) / 4.0;
            final double dd = (aa + bb) / 4.0;
            final double zp = (satAlt - 600.0) / 100.0;
            dTc = aa + zp * (bb + zp * (cc + zp * dd));
        }

        return dTc;
    }

    /** Compute daily temperature correction for Jacchia-Bowman model.
     * @param f10 solar flux index
     * @param solarTime local solar time (hours in [0, 24[)
     * @param satLat sat lat (radians)
     * @param satAlt height (km)
     * @param <T> type of the filed elements
     * @return dTc correction
     */
    private static <T extends CalculusFieldElement<T>> T dTc(final double f10, final T solarTime,
                                                         final T satLat, final T satAlt) {
        T dTc = solarTime.getField().getZero();
        final T      st = solarTime.divide(24.0);
        final T      cs = satLat.cos();
        final double fs = (f10 - 100.0) / 100.0;

        // Calculates dTc according to height
        if (satAlt.getReal() >= 120 && satAlt.getReal() <= 200) {
            final T dtc200   = poly2CDTC(fs, st, cs);
            final T dtc200dz = poly1CDTC(fs, st, cs);
            final T cc       = dtc200.multiply(3).subtract(dtc200dz);
            final T dd       = dtc200.subtract(cc);
            final T zp       = satAlt.subtract(120.0).divide(80.0);
            dTc = zp.multiply(zp).multiply(cc.add(dd.multiply(zp)));
        } else if (satAlt.getReal() > 200.0 && satAlt.getReal() <= 240.0) {
            final T h = satAlt.subtract(200.0).divide(50.0);
            dTc = poly1CDTC(fs, st, cs).multiply(h).add(poly2CDTC(fs, st, cs));
        } else if (satAlt.getReal() > 240.0 && satAlt.getReal() <= 300.0) {
            final T h = solarTime.getField().getZero().add(0.8);
            final T bb = poly1CDTC(fs, st, cs);
            final T aa = bb.multiply(h).add(poly2CDTC(fs, st, cs));
            final T p2BDT = poly2BDTC(st);
            final T dtc300 = poly1BDTC(fs, st, cs, p2BDT.multiply(3));
            final T dtc300dz = cs.multiply(p2BDT);
            final T cc = dtc300.multiply(3).subtract(dtc300dz).subtract(aa.multiply(3)).subtract(bb.multiply(2));
            final T dd = dtc300.subtract(aa).subtract(bb).subtract(cc);
            final T zp = satAlt.subtract(240.0).divide(60.0);
            dTc = aa.add(zp.multiply(bb.add(zp.multiply(cc.add(zp.multiply(dd))))));
        } else if (satAlt.getReal() > 300.0 && satAlt.getReal() <= 600.0) {
            final T h = satAlt.divide(100.0);
            dTc = poly1BDTC(fs, st, cs, h.multiply(poly2BDTC(st)));
        } else if (satAlt.getReal() > 600.0 && satAlt.getReal() <= 800.0) {
            final T poly2 = poly2BDTC(st);
            final T aa = poly1BDTC(fs, st, cs, poly2.multiply(6));
            final T bb = cs.multiply(poly2);
            final T cc = aa.multiply(3).add(bb.multiply(4)).divide(-4.0);
            final T dd = aa.add(bb).divide(4.0);
            final T zp = satAlt.subtract(600.0).divide(100.0);
            dTc = aa.add(zp.multiply(bb.add(zp.multiply(cc.add(zp.multiply(dd))))));
        }

        return dTc;
    }

    /** Calculates first polynomial with CDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @return the value of the polynomial
     */
    private static double poly1CDTC(final double fs, final double st, final double cs) {
        return CDTC[0] +
                fs * (CDTC[1] + st * (CDTC[2] + st * (CDTC[3] + st * (CDTC[4] + st * (CDTC[5] + st * CDTC[6]))))) +
                cs * st * (CDTC[7] + st * (CDTC[8] + st * (CDTC[9] + st * (CDTC[10] + st * CDTC[11])))) +
                cs * (CDTC[12] + fs * (CDTC[13] + st * (CDTC[14] + st * CDTC[15])));
    }

    /** Calculates first polynomial with CDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @param <T> type of the field elements
     * @return the value of the polynomial
     */
    private static <T extends CalculusFieldElement<T>>  T poly1CDTC(final double fs, final T st, final T cs) {
        return    st.multiply(CDTC[6]).
              add(CDTC[5]).multiply(st).
              add(CDTC[4]).multiply(st).
              add(CDTC[3]).multiply(st).
              add(CDTC[2]).multiply(st).
              add(CDTC[1]).multiply(fs).
              add(st.multiply(CDTC[11]).
                  add(CDTC[10]).multiply(st).
                  add(CDTC[ 9]).multiply(st).
                  add(CDTC[ 8]).multiply(st).
                  add(CDTC[7]).multiply(st).multiply(cs)).
              add(st.multiply(CDTC[15]).
                  add(CDTC[14]).multiply(st).
                  add(CDTC[13]).multiply(fs).
                  add(CDTC[12]).multiply(cs)).
              add(CDTC[0]);
    }

    /** Calculates second polynomial with CDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @return the value of the polynomial
     */
    private static double poly2CDTC(final double fs, final double st, final double cs) {
        return CDTC[16] + st * cs * (CDTC[17] + st * (CDTC[18] + st * CDTC[19])) +
                          fs * cs * (CDTC[20] + st * (CDTC[21] + st * CDTC[22]));
    }

    /** Calculates second polynomial with CDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @param <T> type of the field elements
     * @return the value of the polynomial
     */
    private static <T extends CalculusFieldElement<T>>  T poly2CDTC(final double fs, final T st, final T cs) {
        return         st.multiply(CDTC[19]).
                   add(CDTC[18]).multiply(st).
                   add(CDTC[17]).multiply(cs).multiply(st).
               add(    st.multiply(CDTC[22]).
                   add(CDTC[21]).multiply(st).
                   add(CDTC[20]).multiply(cs).multiply(fs)).
               add(CDTC[16]);
    }

    /** Calculates first polynomial with BDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @param hp scaled height * poly2BDTC
     * @return the value of the polynomial
     */
    private static double poly1BDTC(final double fs, final double st, final double cs, final double hp) {
        return BDTC[0] +
                fs * (BDTC[1] + st * (BDTC[2] + st * (BDTC[3] + st * (BDTC[4] + st * (BDTC[5] + st * BDTC[6]))))) +
                cs * (st * (BDTC[7] + st * (BDTC[8] + st * (BDTC[9] + st * (BDTC[10] + st * BDTC[11])))) + hp + BDTC[18]);
    }

    /** Calculates first polynomial with BDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @param hp scaled height * poly2BDTC
     * @param <T> type of the field elements
     * @return the value of the polynomial
     */
    private static <T extends CalculusFieldElement<T>>  T poly1BDTC(final double fs, final T st, final T cs, final T hp) {
        return     st.multiply(BDTC[6]).
               add(BDTC[5]).multiply(st).
               add(BDTC[4]).multiply(st).
               add(BDTC[3]).multiply(st).
               add(BDTC[2]).multiply(st).
               add(BDTC[1]).multiply(fs).
               add(    st.multiply(BDTC[11]).
                   add(BDTC[10]).multiply(st).
                   add(BDTC[ 9]).multiply(st).
                   add(BDTC[ 8]).multiply(st).
                   add(BDTC[ 7]).multiply(st).
                   add(hp).add(BDTC[18]).multiply(cs)).
               add(BDTC[0]);
    }

    /** Calculates second polynomial with BDTC array.
     * @param st local solar time in [0, 1[
     * @return the value of the polynomial
     */
    private static double poly2BDTC(final double st) {
        return BDTC[12] + st * (BDTC[13] + st * (BDTC[14] + st * (BDTC[15] + st * (BDTC[16] + st * BDTC[17]))));
    }

    /** Calculates second polynomial with BDTC array.
     * @param st local solar time in [0, 1[
     * @param <T> type of the field elements
     * @return the value of the polynomial
     */
    private static <T extends CalculusFieldElement<T>>  T poly2BDTC(final T st) {
        return     st.multiply(BDTC[17]).
               add(BDTC[16]).multiply(st).
               add(BDTC[15]).multiply(st).
               add(BDTC[14]).multiply(st).
               add(BDTC[13]).multiply(st).
               add(BDTC[12]);
    }

    /** Evaluates mean molecualr mass - Equation (1).
     * @param z altitude (km)
     * @return mean molecular mass
     */
    private static double mBar(final double z) {
        final double dz = z - 100.;
        double amb = CMB[6];
        for (int i = 5; i >= 0; --i) {
            amb = dz * amb + CMB[i];
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
        T amb = z.getField().getZero().add(CMB[6]);
        for (int i = 5; i >= 0; --i) {
            amb = dz.multiply(amb).add(CMB[i]);
        }
        return amb;
    }

    /** Evaluates the local temperature, Eq. (10) or (13) depending on altitude.
     * @param z altitude
     * @param tc tc array
     * @return temperature profile
     */
    private static double localTemp(final double z, final double[] tc) {
        final double dz = z - 125.;
        if (dz <= 0.) {
            return ((-9.8204695e-6 * dz - 7.3039742e-4) * dz * dz + 1.0) * dz * tc[1] + tc[0];
        } else {
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

    /** Evaluates the gravity at the altitude - Equation (8).
     * @param z altitude (km)
     * @return the gravity (m/s2)
     */
    private static double gravity(final double z) {
        final double tmp = 1.0 + z / EARTH_RADIUS;
        return Constants.G0_STANDARD_GRAVITY / (tmp * tmp);
    }

    /** Evaluates the gravity at the altitude - Equation (8).
     * @param z altitude (km)
     * @return the gravity (m/s2)
     * @param <T> type of the field elements
     */
    private static <T extends CalculusFieldElement<T>>  T gravity(final T z) {
        final T tmp = z.divide(EARTH_RADIUS).add(1);
        return tmp.multiply(tmp).reciprocal().multiply(Constants.G0_STANDARD_GRAVITY);
    }

    /** Compute semi-annual variation (delta log(rho)).
     * @param doy day of year
     * @param alt height (km)
     * @param f10B average 81-day centered f10
     * @param s10B average 81-day centered s10
     * @param xm10B average 81-day centered xn10
     * @return semi-annual variation
     */
    private static double semian08(final double doy, final double alt,
                                   final double f10B, final double s10B, final double xm10B) {

        final double htz = alt / 1000.0;

        // COMPUTE NEW 81-DAY CENTERED SOLAR INDEX FOR FZ
        double fsmb = f10B - 0.70 * s10B - 0.04 * xm10B;

        // SEMIANNUAL AMPLITUDE
        final double fzz = FZM[0] + fsmb * (FZM[1] + htz * (FZM[2] + FZM[3] * htz + FZM[4] * fsmb));

        // COMPUTE DAILY 81-DAY CENTERED SOLAR INDEX FOR GT
        fsmb  = f10B - 0.75 * s10B - 0.37 * xm10B;

        // SEMIANNUAL PHASE FUNCTION
        final double tau   = MathUtils.TWO_PI * (doy - 1.0) / 365;
        final SinCos sc1P = FastMath.sinCos(tau);
        final SinCos sc2P = SinCos.sum(sc1P, sc1P);
        final double gtz = GTM[0] + GTM[1] * sc1P.sin() + GTM[2] * sc1P.cos() + GTM[3] * sc2P.sin() + GTM[4] * sc2P.cos() +
                   fsmb * (GTM[5] + GTM[6] * sc1P.sin() + GTM[7] * sc1P.cos() + GTM[8] * sc2P.sin() + GTM[9] * sc2P.cos());

        return FastMath.max(1.0e-6, fzz) * gtz;

    }

    /** Compute semi-annual variation (delta log(rho)).
     * @param doy day of year
     * @param alt height (km)
     * @param f10B average 81-day centered f10
     * @param s10B average 81-day centered s10
     * @param xm10B average 81-day centered xn10
     * @return semi-annual variation
     * @param <T> type of the field elements
     */
    private static <T extends CalculusFieldElement<T>>  T semian08(final T doy, final T alt,
                                                               final double f10B, final double s10B, final double xm10B) {

        final T htz = alt.divide(1000.0);

        // COMPUTE NEW 81-DAY CENTERED SOLAR INDEX FOR FZ
        double fsmb = f10B - 0.70 * s10B - 0.04 * xm10B;

        // SEMIANNUAL AMPLITUDE
        final T fzz = htz.multiply(FZM[3]).add(FZM[2] + FZM[4] * fsmb).multiply(htz).add(FZM[1]).multiply(fsmb).add(FZM[0]);

        // COMPUTE DAILY 81-DAY CENTERED SOLAR INDEX FOR GT
        fsmb  = f10B - 0.75 * s10B - 0.37 * xm10B;

        // SEMIANNUAL PHASE FUNCTION
        final T tau   = doy.subtract(1).divide(365).multiply(MathUtils.TWO_PI);
        final FieldSinCos<T> sc1P = FastMath.sinCos(tau);
        final FieldSinCos<T> sc2P = FieldSinCos.sum(sc1P, sc1P);
        final T gtz =           sc2P.cos().multiply(GTM[9]).
                            add(sc2P.sin().multiply(GTM[8])).
                            add(sc1P.cos().multiply(GTM[7])).
                            add(sc1P.sin().multiply(GTM[6])).
                            add(GTM[5]).multiply(fsmb).
                        add(    sc2P.cos().multiply(GTM[4]).
                            add(sc2P.sin().multiply(GTM[3])).
                            add(sc1P.cos().multiply(GTM[2])).
                            add(sc1P.sin().multiply(GTM[1])).
                            add(GTM[0]));

        return fzz.getReal() > 1.0e-6 ? gtz.multiply(fzz) : gtz.multiply(1.0e-6);

    }

    /** Compute day of year.
     * @param dateMJD Modified Julian date
     * @return the number days in year
     */
    private static double dayOfYear(final double dateMJD) {
        final double d1950 = dateMJD - 33281;

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
        final T d1950 = dateMJD.subtract(33281);

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

    // OUTPUT:

    /** Compute min of two values, one double and one field element.
     * @param d double value
     * @param f field element
     * @param <T> type of the field elements
     * @return min value
     */
    private <T extends CalculusFieldElement<T>> T min(final double d, final T f) {
        return (f.getReal() > d) ? f.getField().getZero().add(d) : f;
    }

    /** Compute max of two values, one double and one field element.
     * @param d double value
     * @param f field element
     * @param <T> type of the field elements
     * @return max value
     */
    private <T extends CalculusFieldElement<T>> T max(final double d, final T f) {
        return (f.getReal() <= d) ? f.getField().getZero().add(d) : f;
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

        // compute MJD date
        final DateTimeComponents dt = date.getComponents(utc);
        final double dateMJD = dt.getDate().getMJD() +
                dt.getTime().getSecondsInLocalDay() / Constants.JULIAN_DAY;

        // compute geodetic position
        final GeodeticPoint inBody = earth.transform(position, frame, date);

        // compute sun position
        final Frame ecef = earth.getBodyFrame();
        final Vector3D sunPos = sun.getPosition(date, ecef);
        final GeodeticPoint sunInBody = earth.transform(sunPos, ecef, date);

        return getDensity(dateMJD,
                          sunInBody.getLongitude(), sunInBody.getLatitude(),
                          inBody.getLongitude(), inBody.getLatitude(), inBody.getAltitude(),
                          inputParams.getF10(date), inputParams.getF10B(date),
                          inputParams.getS10(date), inputParams.getS10B(date),
                          inputParams.getXM10(date), inputParams.getXM10B(date),
                          inputParams.getY10(date), inputParams.getY10B(date),
                          inputParams.getDSTDTC(date));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T getDensity(final FieldAbsoluteDate<T> date,
                                                        final FieldVector3D<T> position,
                                                        final Frame frame) {

        // check if data are available :
        final AbsoluteDate dateD = date.toAbsoluteDate();
        if (dateD.compareTo(inputParams.getMaxDate()) > 0 ||
                        dateD.compareTo(inputParams.getMinDate()) < 0) {
            throw new OrekitException(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE,
                                      dateD, inputParams.getMinDate(), inputParams.getMaxDate());
        }

        // compute MJD date
        final DateTimeComponents components = date.getComponents(utc);
        final T dateMJD = date
                .durationFrom(new FieldAbsoluteDate<>(date.getField(), components, utc))
                .add(components.getTime().getSecondsInLocalDay())
                .divide(Constants.JULIAN_DAY)
                .add(components.getDate().getMJD());

        // compute geodetic position (km and °)
        final FieldGeodeticPoint<T> inBody = earth.transform(position, frame, date);

        // compute sun position
        final Frame ecef = earth.getBodyFrame();
        final FieldVector3D<T> sunPos = new FieldVector3D<>(date.getField(),
                        sun.getPosition(dateD, ecef));
        final FieldGeodeticPoint<T> sunInBody = earth.transform(sunPos, ecef, date);

        return getDensity(dateMJD,
                          sunInBody.getLongitude(), sunInBody.getLatitude(),
                          inBody.getLongitude(), inBody.getLatitude(), inBody.getAltitude(),
                          inputParams.getF10(dateD), inputParams.getF10B(dateD),
                          inputParams.getS10(dateD), inputParams.getS10B(dateD),
                          inputParams.getXM10(dateD), inputParams.getXM10B(dateD),
                          inputParams.getY10(dateD), inputParams.getY10B(dateD),
                          inputParams.getDSTDTC(dateD));

    }

}
