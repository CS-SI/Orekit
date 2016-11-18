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
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;

/** This is the realization of the Jacchia-Bowman 2006 atmospheric model.
 * <p>
 * It is described in the paper: <br>
 *
 * <a href="http://sol.spacenvironment.net/~JB2006/pubs/JB2006_AIAA-6166_model.pdf">A
 * New Empirical Thermospheric Density Model JB2006 Using New Solar Indices</a><br>
 *
 * <i>Bruce R. Bowman, W. Kent Tobiska and Frank A. Marcos</i> <br>
 *
 * AIAA 2006-6166<br>
 *</p>
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
 *</p>
 *
 * @author Bruce R Bowman (HQ AFSPC, Space Analysis Division), Feb 2006: FORTRAN routine
 * @author Fabien Maussion (java translation)
 */
public class JB2006 implements Atmosphere {

    /** Serializable UID. */
    private static final long serialVersionUID = -4201270765122160831L;

    /** The alpha are the thermal diffusion coefficients in equation (6). */
    private static final double[] ALPHA = {
        0, 0, 0, 0, 0, -0.38
    };

    /** Natural logarithm of 10.0. */
    private static final double AL10  = 2.3025851;

    /** Molecular weights in order: N2, O2, O, Ar, He and H. */
    private static final double[] AMW = {
        0,
        28.0134, 31.9988, 15.9994, 39.9480, 4.0026, 1.00797
    };

    /** Avogadro's number in mks units (molecules/kmol). */
    private static final double AVOGAD = 6.02257e26;

    /** Approximate value for 2 π. */
    private static final double TWOPI   = 6.2831853;

    /** Approximate value for π. */
    private static final double PI      = 3.1415927;

    /** Approximate value for π / 2. */
    private static final double PIOV2   = 1.5707963;

    /** The FRAC are the assumed sea-level volume fractions in order: N2, O2, Ar, and He. */
    private static final double[] FRAC = {
        0,
        0.78110, 0.20955, 9.3400e-3, 1.2890e-5
    };

    /** Universal gas-constant in mks units (joules/K/kmol). */
    private static final double RSTAR = 8314.32;

    /** Value used to establish height step sizes in the regime 90km to 105km. */
    private static final double R1 = 0.010;

    /** Value used to establish height step sizes in the regime 105km to 500km. */
    private static final double R2 = 0.025;

    /** Value used to establish height step sizes in the regime above 500km. */
    private static final double R3 = 0.075;

    /** Weights for the Newton-Cotes five-points quadrature formula. */
    private static final double[] WT = {
        0,
        0.311111111111111, 1.422222222222222,
        0.533333333333333, 1.422222222222222,
        0.311111111111111
    };

    /** Coefficients for high altitude density correction. */
    private static final double[] CHT = {
        0, 0.22, -0.20e-02, 0.115e-02, -0.211e-05
    };

    /** FZ global model values (1978-2004 fit).  */
    private static final double[] FZM = {
        0,
        0.111613e+00, -0.159000e-02, 0.126190e-01,
        -0.100064e-01, -0.237509e-04, 0.260759e-04
    };

    /** GT global model values (1978-2004 fit). */
    private static final double[] GTM = {
        0,
        -0.833646e+00, -0.265450e+00, 0.467603e+00, -0.299906e+00,
        -0.105451e+00, -0.165537e-01, -0.380037e-01, -0.150991e-01,
        -0.541280e-01, 0.119554e-01, 0.437544e-02, -0.369016e-02,
        0.206763e-02, -0.142888e-02, -0.867124e-05, 0.189032e-04,
        0.156988e-03,  0.491286e-03, -0.391484e-04, -0.126854e-04,
        0.134078e-04, -0.614176e-05, 0.343423e-05
    };

    /** XAMBAR relative data. */
    private static final double[] CXAMB = {
        0,
        28.15204, -8.5586e-2, +1.2840e-4, -1.0056e-5,
        -1.0210e-5, +1.5044e-6, +9.9826e-8
    };

    /** DTSUB relative data. */
    private static final double[] BDT_SUB = {
        0,
        -0.457512297e+01, -0.512114909e+01, -0.693003609e+02,
        0.203716701e+03,  0.703316291e+03, -0.194349234e+04,
        0.110651308e+04, -0.174378996e+03,  0.188594601e+04,
        -0.709371517e+04,  0.922454523e+04, -0.384508073e+04,
        -0.645841789e+01,  0.409703319e+02, -0.482006560e+03,
        0.181870931e+04, -0.237389204e+04,  0.996703815e+03,
        0.361416936e+02
    };

    /** DTSUB relative data. */
    private static final double[] CDT_SUB = {
        0,
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
     *  <li>TEMP(1): Exospheric Temperature above Input Position (deg K)</li>
     *  <li>TEMP(2): Temperature at Input Position (deg K)</li>
     *  </ul>
     */
    private double[] temp = new double[3];

    /** Total Mass-Density at Input Position (kg/m³). */
    private double rho;

    /** Sun position. */
    private PVCoordinatesProvider sun;

    /** External data container. */
    private JB2006InputParameters inputParams;

    /** Earth body shape. */
    private BodyShape earth;

    /** Constructor with space environment information for internal computation.
     * @param parameters the solar and magnetic activity data
     * @param sun the sun position
     * @param earth the earth body shape
     */
    public JB2006(final JB2006InputParameters parameters,
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
     * @param f10 10.7-cm Solar flux (1e<sup>-22</sup>*Watt/(m²*Hertz)).
     *            Tabular time 1.0 day earlier
     * @param f10B 10.7-cm Solar Flux, averaged 81-day centered on the input time
     * @param ap Geomagnetic planetary 3-hour index A<sub>p</sub>
     *            for a tabular time 6.7 hours earlier
     * @param s10 EUV index (26-34 nm) scaled to F10. Tabular time 1 day earlier.
     * @param s10B UV 81-day averaged centered index
     * @param xm10 MG2 index scaled to F10
     * @param xm10B MG2 81-day ave. centered index. Tabular time 5.0 days earlier.
     * @return total mass-Density at input position (kg/m³)
     * @exception OrekitException if altitude is below 90 km
     */
    public double getDensity(final double dateMJD, final double sunRA, final double sunDecli,
                             final double satLon, final double satLat, final double satAlt,
                             final double f10, final double f10B, final double ap,
                             final double s10, final double s10B, final double xm10, final double xm10B)
        throws OrekitException {

        if (satAlt < 90000) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD,
                                      satAlt, 90000.0);
        }
        final double scaledSatAlt = satAlt / 1000.0;

        // Equation (14)
        final double tc = 379 + 3.353 * f10B + 0.358 * (f10 - f10B) +
                          2.094 * (s10 - s10B) + 0.343 * (xm10 - xm10B);

        // Equation (15)
        final double eta =   0.5 * FastMath.abs(satLat - sunDecli);
        final double theta = 0.5 * FastMath.abs(satLat + sunDecli);

        // Equation (16)
        final double h     = satLon - sunRA;
        final double tau   = h - 0.64577182 + 0.10471976 * FastMath.sin(h + 0.75049158);
        double solTimeHour = FastMath.toDegrees(h + PI) / 15.0;
        if (solTimeHour >= 24) {
            solTimeHour = solTimeHour - 24.;
        }
        if (solTimeHour < 0) {
            solTimeHour = solTimeHour + 24.;
        }

        // Equation (17)
        final double C = FastMath.pow(FastMath.cos(eta), 2.5);
        final double S = FastMath.pow(FastMath.sin(theta), 2.5);
        final double tmp = FastMath.abs(FastMath.cos(0.5 * tau));
        final double DF = S + (C - S) * tmp * tmp * tmp;
        final double TSUBL = tc * (1. + 0.31 * DF);

        // Equation (18)
        final double EXPAP = FastMath.exp(-0.08 * ap);
        final double DTG = ap + 100. * (1. - EXPAP);

        // Compute correction to dTc for local solar time and lat correction
        final double DTCLST = dTc(f10, solTimeHour, satLat, scaledSatAlt);

        // Compute the local exospheric temperature.
        final double TINF = TSUBL + DTG + DTCLST;
        temp[1] = TINF;

        // Equation (9)
        final double TSUBX = 444.3807 + 0.02385 * TINF - 392.8292 * FastMath.exp(-0.0021357 * TINF);

        // Equation (11)
        final double GSUBX = 0.054285714 * (TSUBX - 183.);

        // The TC array will be an argument in the call to
        // XLOCAL, which evaluates Equation (10) or Equation (13)
        final double[] TC = new double[4];
        TC[0] = TSUBX;
        TC[1] = GSUBX;

        //   A AND GSUBX/A OF Equation (13)
        TC[2] = (TINF - TSUBX) / PIOV2;
        TC[3] = GSUBX / TC[2];

        // Equation (5)
        final double Z1 = 90.;
        final double Z2 = FastMath.min(scaledSatAlt, 105.0);
        double AL = FastMath.log(Z2 / Z1);
        int N = (int) FastMath.floor(AL / R1) + 1;
        double ZR = FastMath.exp(AL / N);
        final double AMBAR1 = xAmbar(Z1);
        final double TLOC1 = xLocal(Z1, TC);
        double ZEND   = Z1;
        double SUM2   = 0.;
        double AIN    = AMBAR1 * xGrav(Z1) / TLOC1;
        double AMBAR2 = 0;
        double TLOC2  = 0;
        double Z      = 0;
        double GRAVL  = 0;

        for (int i = 1; i <= N; ++i) {
            Z = ZEND;
            ZEND = ZR * Z;
            final double DZ = 0.25 * (ZEND - Z);
            double SUM1 = WT[1] * AIN;
            for (int j = 2; j <= 5; ++j) {
                Z += DZ;
                AMBAR2 = xAmbar(Z);
                TLOC2  = xLocal(Z, TC);
                GRAVL  = xGrav(Z);
                AIN    = AMBAR2 * GRAVL / TLOC2;
                SUM1  += WT[j] * AIN;
            }
            SUM2 = SUM2 + DZ * SUM1;
        }
        final double FACT1 = 1000.0 / RSTAR;
        rho = 3.46e-6 * AMBAR2 * TLOC1 * FastMath.exp(-FACT1 * SUM2) / (AMBAR1 * TLOC2);

        // Equation (2)
        final double ANM = AVOGAD * rho;
        double AN  = ANM / AMBAR2;

        // Equation (3)
        double FACT2  = ANM / 28.960;
        final double[] ALN = new double[7];
        ALN[1] = FastMath.log(FRAC[1] * FACT2);
        ALN[4] = FastMath.log(FRAC[3] * FACT2);
        ALN[5] = FastMath.log(FRAC[4] * FACT2);

        // Equation (4)
        ALN[2] = FastMath.log(FACT2 * (1. + FRAC[2]) - AN);
        ALN[3] = FastMath.log(2. * (AN - FACT2));

        if (scaledSatAlt <= 105.0) {
            temp[2] = TLOC2;
            // Put in negligible hydrogen for use in DO-LOOP 13
            ALN[6] = ALN[5] - 25.0;
        } else {
            // Equation (6)
            final double Z3 = FastMath.min(scaledSatAlt, 500.0);
            AL   = FastMath.log(Z3 / Z);
            N    = (int) FastMath.floor(AL / R2) + 1;
            ZR   = FastMath.exp(AL / N);
            SUM2 = 0.;
            AIN  = GRAVL / TLOC2;

            double TLOC3 = 0;
            for (int I = 1; I <= N; ++I) {
                Z = ZEND;
                ZEND = ZR * Z;
                final double DZ = 0.25 * (ZEND - Z);
                double SUM1 = WT[1] * AIN;
                for (int J = 2; J <= 5; ++J) {
                    Z    += DZ;
                    TLOC3 = xLocal(Z, TC);
                    GRAVL = xGrav(Z);
                    AIN   = GRAVL / TLOC3;
                    SUM1  = SUM1 + WT[J] * AIN;
                }
                SUM2 = SUM2 + DZ * SUM1;
            }

            final double Z4 = FastMath.max(scaledSatAlt, 500.0);
            AL = FastMath.log(Z4 / Z);
            double R = R2;
            if (scaledSatAlt > 500.0) {
                R = R3;
            }
            N = (int) FastMath.floor(AL / R) + 1;
            ZR = FastMath.exp(AL / N);
            double SUM3 = 0.;
            double TLOC4 = 0;
            for (int I = 1; I <= N; ++I) {
                Z = ZEND;
                ZEND = ZR * Z;
                final double DZ = 0.25 * (ZEND - Z);
                double SUM1 = WT[1] * AIN;
                for (int J = 2; J <= 5; ++J) {
                    Z    += DZ;
                    TLOC4 = xLocal(Z, TC);
                    GRAVL = xGrav(Z);
                    AIN   = GRAVL / TLOC4;
                    SUM1  = SUM1 + WT[J] * AIN;
                }
                SUM3 = SUM3 + DZ * SUM1;
            }
            final double ALTR;
            final double HSIGN;
            if (scaledSatAlt <= 500.) {
                temp[2] = TLOC3;
                ALTR = FastMath.log(TLOC3 / TLOC2);
                FACT2 = FACT1 * SUM2;
                HSIGN = 1.0;

            } else {
                temp[2] = TLOC4;
                ALTR = FastMath.log(TLOC4 / TLOC2);
                FACT2 = FACT1 * (SUM2 + SUM3);
                HSIGN = -1.0;
            }
            for (int I = 1; I <= 5; ++I) {
                ALN[I] = ALN[I] - (1.0 + ALPHA[I]) * ALTR - FACT2 * AMW[I];
            }

            // Equation (7) - Note that in CIRA72, AL10T5 = DLOG10(T500)
            final double AL10T5 = FastMath.log(TINF) / FastMath.log(10);
            final double ALNH5 = (5.5 * AL10T5 - 39.40) * AL10T5 + 73.13;
            ALN[6] = AL10 * (ALNH5 + 6.) + HSIGN * (FastMath.log(TLOC4 / TLOC3) + FACT1 * SUM3 * AMW[6]);

        }

        // Equation (24)  - J70 Seasonal-Latitudinal Variation
        final double CAPPHI = ((dateMJD - 36204.0) / 365.2422) % 1;
        final int signum = (satLat >= 0) ? 1 : -1;
        final double sinLat = FastMath.sin(satLat);
        final double DLRSL = 0.02 * (scaledSatAlt - 90.) * FastMath.exp(-0.045 * (scaledSatAlt - 90.)) *
                             signum * FastMath.sin(TWOPI * CAPPHI + 1.72) * sinLat * sinLat;

        // Equation (23) - Computes the semiannual variation
        double DLRSA = 0;
        if (Z < 2000.0) {
            final double D1950 = dateMJD - 33281.0;
            // Use new semiannual model DELTA LOG RHO
            DLRSA = semian(dayOfYear(D1950), scaledSatAlt, f10B);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = AL10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final double DLR = AL10 * (DLRSL + DLRSA);
        for (int i = 1; i <= 6; ++i) {
            ALN[i] += DLR;
        }

        // Compute mass-density and mean-molecular-weight and
        // convert number density logs from natural to common.

        double SUMNM = 0.0;

        for (int I = 1; I <= 6; ++I) {
            AN = FastMath.exp(ALN[I]);
            SUMNM += AN * AMW[I];
        }

        rho = SUMNM / AVOGAD;

        // Compute the high altitude exospheric density correction factor
        double FEX = 1.0;
        if ((scaledSatAlt >= 1000.0) && (scaledSatAlt < 1500.0)) {
            final double ZETA   = (scaledSatAlt - 1000.) * 0.002;
            final double ZETA2  =  ZETA * ZETA;
            final double ZETA3  =  ZETA * ZETA2;
            final double F15C   = CHT[1] + CHT[2] * f10B + CHT[3] * 1500.0 + CHT[4] * f10B * 1500.0;
            final double F15C_ZETA = (CHT[3] + CHT[4] * f10B) * 500.0;
            final double FEX2   = 3.0 * F15C - F15C_ZETA - 3.0;
            final double FEX3   = F15C_ZETA - 2.0 * F15C + 2.0;
            FEX    = 1.0 + FEX2 * ZETA2 + FEX3 * ZETA3;
        }
        if (scaledSatAlt >= 1500.0) {
            FEX    = CHT[1] + CHT[2] * f10B + CHT[3] * scaledSatAlt + CHT[4] * f10B * scaledSatAlt;
        }

        // Apply the exospheric density correction factor.
        rho  *= FEX;

        return rho;

    }

    /** Compute daily temperature correction for Jacchia-Bowman model.
     * @param f10 solar flux index
     * @param solTimeHour local solar time (hours 0-23.999)
     * @param satLat sat lat (radians)
     * @param satAlt height (km)
     * @return dTc correction
     */
    private static double dTc(final double f10, final double solTimeHour,
                              final double satLat, final double satAlt) {

        double dTc = 0;
        final double tx  = solTimeHour / 24.0;
        final double tx2 = tx * tx;
        final double tx3 = tx2 * tx;
        final double tx4 = tx3 * tx;
        final double tx5 = tx4 * tx;
        final double ycs = FastMath.cos(satLat);
        final double f   = (f10 - 100.0) / 100.0;
        double h;
        double sum;

        // Calculates dTc
        if ((satAlt >= 120) && (satAlt <= 200)) {
            final double DTC200 = CDT_SUB[17] + CDT_SUB[18] * tx * ycs + CDT_SUB[19] * tx2 * ycs +
                                  CDT_SUB[20] * tx3 * ycs + CDT_SUB[21] * f * ycs + CDT_SUB[22] * tx * f * ycs +
                                  CDT_SUB[23] * tx2 * f * ycs;
            sum = CDT_SUB[1] + BDT_SUB[2] * f + CDT_SUB[3] * tx * f + CDT_SUB[4] * tx2 * f +
                  CDT_SUB[5] * tx3 * f + CDT_SUB[6] * tx4 * f + CDT_SUB[7] * tx5 * f +
                  CDT_SUB[8] * tx * ycs + CDT_SUB[9] * tx2 * ycs + CDT_SUB[10] * tx3 * ycs +
                  CDT_SUB[11] * tx4 * ycs + CDT_SUB[12] * tx5 * ycs + CDT_SUB[13] * ycs +
                  CDT_SUB[14] * f * ycs + CDT_SUB[15] * tx * f * ycs  + CDT_SUB[16] * tx2 * f * ycs;
            final double DTC200DZ = sum;
            final double CC  = 3.0 * DTC200 - DTC200DZ;
            final double DD  = DTC200 - CC;
            final double ZP  = (satAlt - 120.0) / 80.0;
            dTc = CC * ZP * ZP + DD * ZP * ZP * ZP;
        }

        if ((satAlt > 200.0) && (satAlt <= 240.0)) {
            h = (satAlt - 200.0) / 50.0;
            sum = CDT_SUB[1] * h + BDT_SUB[2] * f * h + CDT_SUB[3] * tx * f * h + CDT_SUB[4] * tx2 * f * h +
                  CDT_SUB[5] * tx3 * f * h + CDT_SUB[6] * tx4 * f * h + CDT_SUB[7] * tx5 * f * h +
                  CDT_SUB[8] * tx * ycs * h + CDT_SUB[9] * tx2 * ycs * h + CDT_SUB[10] * tx3 * ycs * h +
                  CDT_SUB[11] * tx4 * ycs * h + CDT_SUB[12] * tx5 * ycs * h + CDT_SUB[13] * ycs * h +
                  CDT_SUB[14] * f * ycs * h + CDT_SUB[15] * tx * f * ycs * h  + CDT_SUB[16] * tx2 * f * ycs * h +
                  CDT_SUB[17] + CDT_SUB[18] * tx * ycs + CDT_SUB[19] * tx2 * ycs +
                  CDT_SUB[20] * tx3 * ycs + CDT_SUB[21] * f * ycs + CDT_SUB[22] * tx * f * ycs +
                  CDT_SUB[23] * tx2 * f * ycs;
            dTc = sum;
        }

        if ((satAlt > 240.0) && (satAlt <= 300.0)) {
            h = 40.0 / 50.0;
            sum = CDT_SUB[1] * h + BDT_SUB[2] * f * h + CDT_SUB[3] * tx * f * h + CDT_SUB[4] * tx2 * f * h +
                  CDT_SUB[5] * tx3 * f * h + CDT_SUB[6] * tx4 * f * h + CDT_SUB[7] * tx5 * f * h +
                  CDT_SUB[8] * tx * ycs * h + CDT_SUB[9] * tx2 * ycs * h + CDT_SUB[10] * tx3 * ycs * h +
                  CDT_SUB[11] * tx4 * ycs * h + CDT_SUB[12] * tx5 * ycs * h + CDT_SUB[13] * ycs * h +
                  CDT_SUB[14] * f * ycs * h + CDT_SUB[15] * tx * f * ycs * h  + CDT_SUB[16] * tx2 * f * ycs * h +
                  CDT_SUB[17] + CDT_SUB[18] * tx * ycs + CDT_SUB[19] * tx2 * ycs +
                  CDT_SUB[20] * tx3 * ycs + CDT_SUB[21] * f * ycs + CDT_SUB[22] * tx * f * ycs +
                  CDT_SUB[23] * tx2 * f * ycs;
            final double AA = sum;
            final double BB = CDT_SUB[1] + BDT_SUB[2] * f + CDT_SUB[3] * tx * f + CDT_SUB[4] * tx2 * f +
                        CDT_SUB[5] * tx3 * f + CDT_SUB[6] * tx4 * f + CDT_SUB[7] * tx5 * f +
                        CDT_SUB[8] * tx * ycs + CDT_SUB[9] * tx2 * ycs + CDT_SUB[10] * tx3 * ycs +
                        CDT_SUB[11] * tx4 * ycs + CDT_SUB[12] * tx5 * ycs + CDT_SUB[13] * ycs +
                        CDT_SUB[14] * f * ycs + CDT_SUB[15] * tx * f * ycs + CDT_SUB[16] * tx2 * f * ycs;
            h   = 300.0 / 100.0;
            sum = BDT_SUB[1] + BDT_SUB[2] * f  + BDT_SUB[3] * tx * f         + BDT_SUB[4] * tx2 * f +
                  BDT_SUB[5] * tx3 * f      + BDT_SUB[6] * tx4 * f      + BDT_SUB[7] * tx5 * f +
                  BDT_SUB[8] * tx * ycs       + BDT_SUB[9] * tx2 * ycs    + BDT_SUB[10] * tx3 * ycs +
                  BDT_SUB[11] * tx4 * ycs   + BDT_SUB[12] * tx5 * ycs   + BDT_SUB[13] * h * ycs +
                  BDT_SUB[14] * tx * h * ycs    + BDT_SUB[15] * tx2 * h * ycs + BDT_SUB[16] * tx3 * h * ycs +
                  BDT_SUB[17] * tx4 * h * ycs + BDT_SUB[18] * tx5 * h * ycs + BDT_SUB[19] * ycs;
            final double DTC300 = sum;
            sum = BDT_SUB[13] * ycs +
                  BDT_SUB[14] * tx * ycs + BDT_SUB[15] * tx2 * ycs + BDT_SUB[16] * tx3 * ycs +
                  BDT_SUB[17] * tx4 * ycs + BDT_SUB[18] * tx5 * ycs;
            final double DTC300DZ = sum;
            final double CC = 3.0 * DTC300 - DTC300DZ - 3.0 * AA - 2.0 * BB;
            final double  DD = DTC300 - AA - BB - CC;
            final double ZP  = (satAlt - 240.0) / 60.0;
            dTc = AA + BB * ZP + CC * ZP * ZP + DD * ZP * ZP * ZP;
        }

        if ((satAlt > 300.0) && (satAlt <= 600.0)) {
            h   = satAlt / 100.0;
            sum = BDT_SUB[1]    + BDT_SUB[2] * f  + BDT_SUB[3] * tx * f         + BDT_SUB[4] * tx2 * f +
                  BDT_SUB[5] * tx3 * f      + BDT_SUB[6] * tx4 * f      + BDT_SUB[7] * tx5 * f +
                  BDT_SUB[8] * tx * ycs       + BDT_SUB[9] * tx2 * ycs    + BDT_SUB[10] * tx3 * ycs +
                  BDT_SUB[11] * tx4 * ycs   + BDT_SUB[12] * tx5 * ycs   + BDT_SUB[13] * h * ycs +
                  BDT_SUB[14] * tx * h * ycs    + BDT_SUB[15] * tx2 * h * ycs + BDT_SUB[16] * tx3 * h * ycs +
                  BDT_SUB[17] * tx4 * h * ycs + BDT_SUB[18] * tx5 * h * ycs + BDT_SUB[19] * ycs;
            dTc = sum;
        }

        if ((satAlt > 600.0) && (satAlt <= 800.0)) {
            final double ZP = (satAlt - 600.0) / 100.0;
            final double HP = 600.0 / 100.0;
            final double AA  = BDT_SUB[1]    + BDT_SUB[2] * f  + BDT_SUB[3] * tx * f         + BDT_SUB[4] * tx2 * f +
                               BDT_SUB[5] * tx3 * f      + BDT_SUB[6] * tx4 * f      + BDT_SUB[7] * tx5 * f +
                               BDT_SUB[8] * tx * ycs       + BDT_SUB[9] * tx2 * ycs    + BDT_SUB[10] * tx3 * ycs +
                               BDT_SUB[11] * tx4 * ycs   + BDT_SUB[12] * tx5 * ycs   + BDT_SUB[13] * HP * ycs +
                               BDT_SUB[14] * tx * HP * ycs   + BDT_SUB[15] * tx2 * HP * ycs + BDT_SUB[16] * tx3 * HP * ycs +
                               BDT_SUB[17] * tx4 * HP * ycs + BDT_SUB[18] * tx5 * HP * ycs + BDT_SUB[19] * ycs;
            final double BB  = BDT_SUB[13] * ycs +
                               BDT_SUB[14] * tx * ycs    + BDT_SUB[15] * tx2 * ycs + BDT_SUB[16] * tx3 * ycs +
                               BDT_SUB[17] * tx4 * ycs + BDT_SUB[18] * tx5 * ycs;
            final double CC  = -(3.0 * AA + 4.0 * BB) / 4.0;
            final double DD  = (AA + BB) / 4.0;
            dTc = AA + BB * ZP + CC * ZP * ZP + DD * ZP * ZP * ZP;
        }

        return dTc;
    }

    /** Evaluates Equation (1).
     * @param z altitude
     * @return equation (1) value
     */
    private static double xAmbar(final double z) {
        final double dz = z - 100.;
        double amb = CXAMB[7];
        for (int i = 6; i >= 1; --i) {
            amb = dz * amb + CXAMB[i];
        }
        return amb;
    }

    /**  Evaluates Equation (10) or Equation (13), depending on Z.
     * @param z altitude
     * @param TC tc array ???
     * @return equation (10) value
     */
    private static double xLocal(final double z, final double[] TC) {
        final double dz = z - 125;
        if (dz <= 0) {
            return ((-9.8204695e-6 * dz - 7.3039742e-4) * dz * dz + 1.0) * dz * TC[1] + TC[0];
        } else {
            return TC[0] + TC[2] * FastMath.atan(TC[3] * dz * (1 + 4.5e-6 * FastMath.pow(dz, 2.5)));
        }
    }

    /** Evaluates Equation (8) of gravity field.
     * @param z altitude
     * @return the gravity field
     */
    private static double xGrav(final double z) {
        final double temp = 1.0 + z / 6356.766;
        return 9.80665 / (temp * temp);
    }

    /** Compute semi-annual variation (delta log(rho)).
     * @param day day of year
     * @param height height (km)
     * @param f10Bar average 81-day centered f10
     * @return semi-annual variation
     */
    private static double semian (final double day, final double height, final double f10Bar) {

        final double f10Bar2 = f10Bar * f10Bar;
        final double htz = height / 1000.0;

        // SEMIANNUAL AMPLITUDE
        final double fzz = FZM[1] + FZM[2] * f10Bar  + FZM[3] * f10Bar * htz +
                           FZM[4] * f10Bar * htz * htz + FZM[5] * f10Bar * f10Bar * htz +
                           FZM[6] * f10Bar * f10Bar * htz * htz;

        // SEMIANNUAL PHASE FUNCTION
        final double tau   = TWOPI * (day - 1.0) / 365;
        final double sin1P = FastMath.sin(tau);
        final double cos1P = FastMath.cos(tau);
        final double sin2P = FastMath.sin(2.0 * tau);
        final double cos2P = FastMath.cos(2.0 * tau);
        final double sin3P = FastMath.sin(3.0 * tau);
        final double cos3P = FastMath.cos(3.0 * tau);
        final double sin4P = FastMath.sin(4.0 * tau);
        final double cos4P = FastMath.cos(4.0 * tau);
        final double gtz = GTM[1] + GTM[2] * sin1P + GTM[3] * cos1P +
                           GTM[4] * sin2P + GTM[5] * cos2P +
                           GTM[6] * sin3P + GTM[7] * cos3P +
                           GTM[8] * sin4P + GTM[9] * cos4P +
                           GTM[10] * f10Bar + GTM[11] * f10Bar * sin1P + GTM[12] * f10Bar * cos1P +
                           GTM[13] * f10Bar * sin2P + GTM[14] * f10Bar * cos2P +
                           GTM[15] * f10Bar * sin3P + GTM[16] * f10Bar * cos3P +
                           GTM[17] * f10Bar * sin4P + GTM[18] * f10Bar * cos4P +
                           GTM[19] * f10Bar2  + GTM[20] * f10Bar2  * sin1P + GTM[21] * f10Bar2  * cos1P +
                           GTM[22] * f10Bar2  * sin2P + GTM[23] * f10Bar2  * cos2P;

        return FastMath.max(1.0e-6, fzz) * gtz;

    }

    /** Compute day of year.
     * @param d1950 (days since 1950)
     * @return the number days in year
     */
    private static double dayOfYear(final double d1950) {

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
     * {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, double, double)}
     * <b> must </b> must be called before calling this function.
     * @return the exospheric temperature (deg K)
     */
    public double getExosphericTemp() {
        return temp[1];
    }

    /** Get the temperature at input position.
     * {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, double, double)}
     * <b> must </b> must be called before calling this function.
     * @return the local temperature (deg K)
     */
    public double getLocalTemp() {
        return temp[2];
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

        // compute modified julian days date
        final double dateMJD = date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / Constants.JULIAN_DAY;

        // compute geodetic position
        final GeodeticPoint inBody = earth.transform(position, frame, date);

        // compute sun position
        final Frame ecef = earth.getBodyFrame();
        final GeodeticPoint sunInBody =
            earth.transform(sun.getPVCoordinates(date, ecef).getPosition(), ecef, date);
        return getDensity(dateMJD,
                          sunInBody.getLongitude(), sunInBody.getLatitude(),
                          inBody.getLongitude(), inBody.getLatitude(),
                          inBody.getAltitude(), inputParams.getF10(date),
                          inputParams.getF10B(date),
                          inputParams.getAp(date), inputParams.getS10(date),
                          inputParams.getS10B(date), inputParams.getXM10(date),
                          inputParams.getXM10B(date));
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
