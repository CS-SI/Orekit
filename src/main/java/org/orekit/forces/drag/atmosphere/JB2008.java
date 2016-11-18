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

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;

/** This is the realization of the Jacchia-Bowman 2008 atmospheric model.
 * <p>
 * It is described in the paper:<br>
 * <a href="http://sol.spacenvironment.net/~JB2008/pubs/AIAA_2008-6438_JB2008_Model.pdf">A
 * New Empirical Thermospheric Density Model JB2008 Using New Solar Indices</a><br>
 * <i>Bruce R. Bowman & al.</i><br>
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
 * </p>
 * <p>
 * This model provides dense output for all altitudes and positions. Output data are :
 * <ul>
 * <li>Exospheric Temperature above Input Position (deg K)</li>
 * <li>Temperature at Input Position (deg K)</li>
 * <li>Total Mass-Density at Input Position (kg/m³)</li>
 * </ul>
 * </p>
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

    /** Temperatures.
     *  <ul>
     *  <li>temp[0]: Exospheric Temperature above Input Position (deg K)</li>
     *  <li>temp[1]: Temperature at Input Position (deg K)</li>
     *  </ul>
     */
    private double[] temp = new double[2];

    /** Total Mass-Density at Input Position (kg/m³). */
    private double rho;

    /** Sun position. */
    private PVCoordinatesProvider sun;

    /** External data container. */
    private JB2008InputParameters inputParams;

    /** Earth body shape. */
    private BodyShape earth;

    /** Constructor with space environment information for internal computation.
     * @param parameters the solar and magnetic activity data
     * @param sun the sun position
     * @param earth the earth body shape
     */
    public JB2008(final JB2008InputParameters parameters,
                  final PVCoordinatesProvider sun, final BodyShape earth) {
        this.earth = earth;
        this.sun = sun;
        this.inputParams = parameters;
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
     * @param y10 Solar X-Ray & Lya index scaled to F10<br>
     *        (Tabular time 5.0 days earlier)
     * @param y10B Solar X-Ray & Lya 81-day ave. centered index<br>
     *        (Tabular time 5.0 days earlier)
     * @param dstdtc Temperature change computed from Dst index
     * @return total mass-Density at input position (kg/m³)
     * @exception OrekitException if altitude is below 90 km
     */
    public double getDensity(final double dateMJD, final double sunRA, final double sunDecli,
                             final double satLon, final double satLat, final double satAlt,
                             final double f10, final double f10B, final double s10,
                             final double s10B, final double xm10, final double xm10B,
                             final double y10, final double y10B, final double dstdtc)
        throws OrekitException {

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

        rho = 3.46e-6 * mb2 * tloc1 / FastMath.exp(sub2 / RSTAR) / (mb1 * tloc2);

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
        if ((altKm >= 1000.0) && (altKm < 1500.0)) {
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
        if ((satAlt >= 120) && (satAlt <= 200)) {
            final double dtc200 = poly2CDTC(fs, st, cs);
            final double dtc200dz = poly1CDTC(fs, st, cs);
            final double cc = 3.0 * dtc200 - dtc200dz;
            final double dd = dtc200 - cc;
            final double zp = (satAlt - 120.0) / 80.0;
            dTc = zp * zp * (cc + dd * zp);
        } else if ((satAlt > 200.0) && (satAlt <= 240.0)) {
            final double h = (satAlt - 200.0) / 50.0;
            dTc = poly1CDTC(fs, st, cs) * h + poly2CDTC(fs, st, cs);
        } else if ((satAlt > 240.0) && (satAlt <= 300.0)) {
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
        } else if ((satAlt > 300.0) && (satAlt <= 600.0)) {
            final double h = satAlt / 100.0;
            dTc = poly1BDTC(fs, st, cs, h * poly2BDTC(st));
        } else if ((satAlt > 600.0) && (satAlt <= 800.0)) {
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

    /** Calculates second polynomial with BDTC array.
     * @param st local solar time in [0, 1[
     * @return the value of the polynomial
     */
    private static double poly2BDTC(final double st) {
        return BDTC[12] + st * (BDTC[13] + st * (BDTC[14] + st * (BDTC[15] + st * (BDTC[16] + st * BDTC[17]))));
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

    /** Evaluates the gravity at the altitude - Equation (8).
     * @param z altitude (km)
     * @return the gravity (m/s2)
     */
    private static double gravity(final double z) {
        final double tmp = 1.0 + z / EARTH_RADIUS;
        return Constants.G0_STANDARD_GRAVITY / (tmp * tmp);
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
        final double taux2 = tau + tau;
        final double sin1P = FastMath.sin(tau);
        final double cos1P = FastMath.cos(tau);
        final double sin2P = FastMath.sin(taux2);
        final double cos2P = FastMath.cos(taux2);
        final double gtz = GTM[0] + GTM[1] * sin1P + GTM[2] * cos1P + GTM[3] * sin2P + GTM[4] * cos2P +
                           fsmb * (GTM[5] + GTM[6] * sin1P + GTM[7] * cos1P + GTM[8] * sin2P + GTM[9] * cos2P);

        return FastMath.max(1.0e-6, fzz) * gtz;

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

    // OUTPUT:

    /** Get the exospheric temperature above input position.
     * {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double)}
     * <b> must </b> must be called before calling this function.
     * @return the exospheric temperature (deg K)
     */
    public double getExosphericTemp() {
        return temp[0];
    }

    /** Get the temperature at input position.
     * {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double)}
     * <b> must </b> must be called before calling this function.
     * @return the local temperature (deg K)
     */
    public double getLocalTemp() {
        return temp[1];
    }

    /** Get the local density.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return local density (kg/m³)
     * @exception OrekitException if date is out of range of solar activity
     */
    public double getDensity(final AbsoluteDate date, final Vector3D position,
                             final Frame frame)
        throws OrekitException {
        // check if data are available :
        if (date.compareTo(inputParams.getMaxDate()) > 0 ||
            date.compareTo(inputParams.getMinDate()) < 0) {
            throw new OrekitException(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE,
                                      date, inputParams.getMinDate(), inputParams.getMaxDate());
        }

        // compute MJD date
        final double dateMJD = date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / Constants.JULIAN_DAY;

        // compute geodetic position
        final GeodeticPoint inBody = earth.transform(position, frame, date);

        // compute sun position
        final Frame ecef = earth.getBodyFrame();
        final Vector3D sunPos = sun.getPVCoordinates(date, ecef).getPosition();
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

    @Override
    public <T extends RealFieldElement<T>> T
        getDensity(final FieldAbsoluteDate<T> date, final FieldVector3D<T> position,
                   final Frame frame)
            throws OrekitException {
        // TODO: field implementation
        throw new UnsupportedOperationException();
    }

}
