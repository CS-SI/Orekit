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

import java.util.stream.IntStream;
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
import org.orekit.utils.ExtendedPositionProvider;

/** This is the realization of the Jacchia-Bowman 2008 atmospheric model.
 * <p>
 * It is described in the paper:<br>
 * <a href="https://www.researchgate.net/publication/228621668_A_New_Empirical_Thermospheric_Density_Model_JB2008_Using_New_Solar_and_Geomagnetic_Indices">A
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
public class JB2008 extends AbstractSunInfluencedAtmosphere {

    /** FZ global model values (1997-2006 fit).  */
    private static final double[] FZM = {
        0.2689e+00, -0.1176e-01, 0.2782e-01, -0.2782e-01, 0.3470e-03
    };

    /** GT global model values (1997-2006 fit). */
    private static final double[] GTM = {
        -0.3633e+00, 0.8506e-01,  0.2401e+00, -0.1897e+00, -0.2554e+00,
        -0.1790e-01, 0.5650e-03, -0.6407e-03, -0.3418e-02, -0.1252e-02
    };

    /** External data container. */
    private final JB2008InputParameters inputParams;

    /** Earth body shape. */
    private final BodyShape earth;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Constructor with space environment information for internal computation.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param parameters the solar and magnetic activity data
     * @param sun the sun position
     * @param earth the earth body shape
     * @see #JB2008(JB2008InputParameters, ExtendedPositionProvider, BodyShape, TimeScale)
     */
    @DefaultDataContext
    public JB2008(final JB2008InputParameters parameters,
                  final ExtendedPositionProvider sun, final BodyShape earth) {
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
                  final ExtendedPositionProvider sun,
                  final BodyShape earth,
                  final TimeScale utc) {
        super(sun);
        this.earth = earth;
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

        if (satAlt < JacchiaBowmanEquationsFactory.ALT_MIN) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, satAlt, JacchiaBowmanEquationsFactory.ALT_MIN);
        }

        final double altKm = satAlt / 1000.0;

        // Equation (14)
        // Temperature equation obtained using numerous satellites for the years from 1997 through 2007 when all new solar indices were available
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
        final double solarTime = JacchiaBowmanEquationsFactory.solarTimeHour(h);

        // Equation (17)
        final double tsubl = JacchiaBowmanEquationsFactory.tSubL(eta, theta, tau, tsubc);

        // Compute correction to dTc for local solar time and lat correction
        final double dtclst = JacchiaBowmanEquationsFactory.dTc(f10, solarTime, satLat, altKm);

        // Compute the local exospheric temperature.
        // Add geomagnetic storm effect from input dTc value
        final double tinf = tsubl + dstdtc + dtclst;

        // Equation (9)
        final double tsubx = JacchiaBowmanEquationsFactory.tSubX(tinf);

        // Equation (11)
        final double gsubx = JacchiaBowmanEquationsFactory.gSubX(tsubx);

        // The TC array will be an argument in the call to "localTemp"
        final double[] tc = JacchiaBowmanEquationsFactory.tSubCArray(tsubx, gsubx, tinf);

        // Equation (5)
        final double z1 = 90.;
        final double z2 = FastMath.min(altKm, 105.0);
        double al = FastMath.log(z2 / z1);
        int n = 1 + (int) FastMath.floor(al / JacchiaBowmanEquationsFactory.R1);
        double zr = FastMath.exp(al / n);
        final double mb1 = JacchiaBowmanEquationsFactory.mBar(z1);
        final double tloc1 = JacchiaBowmanEquationsFactory.localTemp(z1, tc);
        double zend  = z1;
        double sub2  = 0.;
        double ain   = mb1 * JacchiaBowmanEquationsFactory.gravity(z1) / tloc1;
        double mb2   = 0;
        double tloc2 = 0;
        double z     = 0;
        double gravl = 0;

        for (int i = 0; i < n; ++i) {
            z = zend;
            zend = zr * z;
            final double dz = 0.25 * (zend - z);
            double sum1 = JacchiaBowmanEquationsFactory.WT[0] * ain;
            for (int j = 1; j < 5; ++j) {
                z += dz;
                mb2   = JacchiaBowmanEquationsFactory.mBar(z);
                tloc2 = JacchiaBowmanEquationsFactory.localTemp(z, tc);
                gravl = JacchiaBowmanEquationsFactory.gravity(z);
                ain   = mb2 * gravl / tloc2;
                sum1 += JacchiaBowmanEquationsFactory.WT[j] * ain;
            }
            sub2 += dz * sum1;
        }

        double rho = 3.46e-6 * mb2 * tloc1 / FastMath.exp(sub2 / JacchiaBowmanEquationsFactory.RSTAR) / (mb1 * tloc2);

        // Equation (2)
        final double anm = JacchiaBowmanEquationsFactory.AVOGAD * rho;
        final double an  = anm / mb2;

        // Equation (3)
        double fact2  = anm / 28.960;
        final double[] aln = new double[6];
        aln[0] = FastMath.log(JacchiaBowmanEquationsFactory.FRAC[0] * fact2);
        aln[3] = FastMath.log(JacchiaBowmanEquationsFactory.FRAC[2] * fact2);
        aln[4] = FastMath.log(JacchiaBowmanEquationsFactory.FRAC[3] * fact2);
        // Equation (4)
        aln[1] = FastMath.log(fact2 * (1. + JacchiaBowmanEquationsFactory.FRAC[1]) - an);
        aln[2] = FastMath.log(2. * (an - fact2));

        if (altKm <= 105.0) {
            // Put in negligible hydrogen for use in DO-LOOP 13
            aln[5] = aln[4] - 25.0;
        } else {
            // Equation (6)
            al   = FastMath.log(FastMath.min(altKm, 500.0) / z);
            n    = 1 + (int) FastMath.floor(al / JacchiaBowmanEquationsFactory.R2);
            zr   = FastMath.exp(al / n);
            sub2 = 0.;
            ain  = gravl / tloc2;

            double tloc3 = 0;
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr * z;
                final double dz = 0.25 * (zend - z);
                double sum1 = JacchiaBowmanEquationsFactory.WT[0] * ain;
                for (int j = 1; j < 5; ++j) {
                    z += dz;
                    tloc3 = JacchiaBowmanEquationsFactory.localTemp(z, tc);
                    gravl = JacchiaBowmanEquationsFactory.gravity(z);
                    ain   = gravl / tloc3;
                    sum1 += JacchiaBowmanEquationsFactory.WT[j] * ain;
                }
                sub2 += dz * sum1;
            }

            al = FastMath.log(FastMath.max(altKm, 500.0) / z);
            final double r = (altKm > 500.0) ? JacchiaBowmanEquationsFactory.R3 : JacchiaBowmanEquationsFactory.R2;
            n = 1 + (int) FastMath.floor(al / r);
            zr = FastMath.exp(al / n);
            double sum3 = 0.;
            double tloc4 = 0;
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr * z;
                final double dz = 0.25 * (zend - z);
                double sum1 = JacchiaBowmanEquationsFactory.WT[0] * ain;
                for (int j = 1; j < 5; ++j) {
                    z += dz;
                    tloc4 = JacchiaBowmanEquationsFactory.localTemp(z, tc);
                    gravl = JacchiaBowmanEquationsFactory.gravity(z);
                    ain   = gravl / tloc4;
                    sum1 += JacchiaBowmanEquationsFactory.WT[j] * ain;
                }
                sum3 += dz * sum1;
            }
            final double altr;
            final double hSign;
            if (altKm <= 500.) {
                altr = FastMath.log(tloc3 / tloc2);
                fact2 = sub2 / JacchiaBowmanEquationsFactory.RSTAR;
                hSign = 1.0;
            } else {
                altr = FastMath.log(tloc4 / tloc2);
                fact2 = (sub2 + sum3) / JacchiaBowmanEquationsFactory.RSTAR;
                hSign = -1.0;
            }
            for (int i = 0; i < 5; ++i) {
                aln[i] = aln[i] - (1.0 + JacchiaBowmanEquationsFactory.ALPHA[i]) * altr - fact2 * JacchiaBowmanEquationsFactory.AMW[i];
            }

            // Equation (7)
            final double al10t5 = FastMath.log10(tinf);
            final double alnh5 = (5.5 * al10t5 - 39.40) * al10t5 + 73.13;
            aln[5] = JacchiaBowmanEquationsFactory.LOG10 * (alnh5 + 6.) + hSign * (FastMath.log(tloc4 / tloc3) + sum3 * JacchiaBowmanEquationsFactory.AMW[5] / JacchiaBowmanEquationsFactory.RSTAR);
        }

        // Equation (24) - J70 Seasonal-Latitudinal Variation
        final double dlrsl = JacchiaBowmanEquationsFactory.dlrsl(altKm, dateMJD, satLat);

        // Equation (23) - Computes the semiannual variation
        double dlrsa = 0;
        if (z < 2000.0) {
            // Use new semiannual model dLog(rho)
            dlrsa = semian08(JacchiaBowmanEquationsFactory.dayOfYear(dateMJD), altKm, f10B, s10B, xm10B);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = LOG10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final double dlr = JacchiaBowmanEquationsFactory.LOG10 * (dlrsl + dlrsa);
        for (int i = 0; i < 6; ++i) {
            aln[i] += dlr;
        }

        // Compute mass-density and mean-molecular-weight and
        // convert number density logs from natural to common.
        final double sumnm = IntStream.range(0, 6).mapToDouble(i -> FastMath.exp(aln[i]) * JacchiaBowmanEquationsFactory.AMW[i]).sum();
        rho = sumnm / JacchiaBowmanEquationsFactory.AVOGAD;

        // Compute the high altitude exospheric density correction factor
        final double fex = JacchiaBowmanEquationsFactory.densityCorrectionFactor(altKm, f10B);

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

        if (satAlt.getReal() < JacchiaBowmanEquationsFactory.ALT_MIN) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD,
                                      satAlt.getReal(), JacchiaBowmanEquationsFactory.ALT_MIN);
        }

        final Field<T> field  = satAlt.getField();
        final T pi    = field.getOne().getPi();
        final T altKm = satAlt.divide(1000.0);

        // Equation (14) (Temperature equation)
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
        final T solarTime = JacchiaBowmanEquationsFactory.solarTimeHour(h);

        // Equation (17)
        final T tsubl = JacchiaBowmanEquationsFactory.tSubL(eta, theta, tau, tsubc);

        // Compute correction to dTc for local solar time and lat correction
        final T dtclst = JacchiaBowmanEquationsFactory.dTc(f10, solarTime, satLat, altKm);

        // Compute the local exospheric temperature.
        // Add geomagnetic storm effect from input dTc value
        final T tinf = tsubl.add(dstdtc).add(dtclst);

        // Equation (9)
        final T tsubx = JacchiaBowmanEquationsFactory.tSubX(tinf);

        // Equation (11)
        final T gsubx = JacchiaBowmanEquationsFactory.gSubX(tsubx);

        // The TC array will be an argument in the call to "localTemp"
        final T[] tc = JacchiaBowmanEquationsFactory.tSubCArray(tsubx, gsubx, tinf, field);

        // Equation (5)
        final T z1 = field.getZero().newInstance(90.);
        final T z2 = FastMath.min(altKm, 105.0);
        T al = z2.divide(z1).log();
        int n = 1 + (int) FastMath.floor(al.getReal() / JacchiaBowmanEquationsFactory.R1);
        T zr = al.divide(n).exp();
        final T mb1 = JacchiaBowmanEquationsFactory.mBar(z1);
        final T tloc1 = JacchiaBowmanEquationsFactory.localTemp(z1, tc);
        T zend  = z1;
        T sub2  = field.getZero();
        T ain   = mb1.multiply(JacchiaBowmanEquationsFactory.gravity(z1)).divide(tloc1);
        T mb2   = field.getZero();
        T tloc2 = field.getZero();
        T z     = field.getZero();
        T gravl = field.getZero();
        for (int i = 0; i < n; ++i) {
            z = zend;
            zend = zr.multiply(z);
            final T dz = zend.subtract(z).multiply(0.25);
            T sum1 = ain.multiply(JacchiaBowmanEquationsFactory.WT[0]);
            for (int j = 1; j < 5; ++j) {
                z = z.add(dz);
                mb2   = JacchiaBowmanEquationsFactory.mBar(z);
                tloc2 = JacchiaBowmanEquationsFactory.localTemp(z, tc);
                gravl = JacchiaBowmanEquationsFactory.gravity(z);
                ain   = mb2.multiply(gravl).divide(tloc2);
                sum1  = sum1.add(ain.multiply(JacchiaBowmanEquationsFactory.WT[j]));
            }
            sub2 = sub2.add(dz.multiply(sum1));
        }
        T rho = mb2.multiply(3.46e-6).multiply(tloc1).divide(sub2.divide(JacchiaBowmanEquationsFactory.RSTAR).exp().multiply(mb1.multiply(tloc2)));

        // Equation (2)
        final T anm = rho.multiply(JacchiaBowmanEquationsFactory.AVOGAD);
        final T an  = anm.divide(mb2);

        // Equation (3)
        T fact2  = anm.divide(28.960);
        final T[] aln = MathArrays.buildArray(field, 6);
        aln[0] = fact2.multiply(JacchiaBowmanEquationsFactory.FRAC[0]).log();
        aln[3] = fact2.multiply(JacchiaBowmanEquationsFactory.FRAC[2]).log();
        aln[4] = fact2.multiply(JacchiaBowmanEquationsFactory.FRAC[3]).log();
        // Equation (4)
        aln[1] = fact2.multiply(1. + JacchiaBowmanEquationsFactory.FRAC[1]).subtract(an).log();
        aln[2] = an.subtract(fact2).multiply(2).log();

        if (altKm.getReal() <= 105.0) {
            // Put in negligible hydrogen for use in DO-LOOP 13
            aln[5] = aln[4].subtract(25.0);
        } else {
            // Equation (6)
            al   = FastMath.min(altKm, 500.0).divide(z).log();
            n    = 1 + (int) FastMath.floor(al.getReal() / JacchiaBowmanEquationsFactory.R2);
            zr   = al.divide(n).exp();
            sub2 = field.getZero();
            ain  = gravl.divide(tloc2);

            T tloc3 = field.getZero();
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr.multiply(z);
                final T dz = zend.subtract(z).multiply(0.25);
                T sum1 = ain.multiply(JacchiaBowmanEquationsFactory.WT[0]);
                for (int j = 1; j < 5; ++j) {
                    z = z.add(dz);
                    tloc3 = JacchiaBowmanEquationsFactory.localTemp(z, tc);
                    gravl = JacchiaBowmanEquationsFactory.gravity(z);
                    ain   = gravl.divide(tloc3);
                    sum1  = sum1.add(ain.multiply(JacchiaBowmanEquationsFactory.WT[j]));
                }
                sub2 = sub2.add(dz.multiply(sum1));
            }

            al = FastMath.max(altKm, 500.0).divide(z).log();
            final double r = (altKm.getReal() > 500.0) ? JacchiaBowmanEquationsFactory.R3 : JacchiaBowmanEquationsFactory.R2;
            n = 1 + (int) FastMath.floor(al.getReal() / r);
            zr = al.divide(n).exp();
            T sum3 = field.getZero();
            T tloc4 = field.getZero();
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr.multiply(z);
                final T dz = zend.subtract(z).multiply(0.25);
                T sum1 = ain.multiply(JacchiaBowmanEquationsFactory.WT[0]);
                for (int j = 1; j < 5; ++j) {
                    z = z.add(dz);
                    tloc4 = JacchiaBowmanEquationsFactory.localTemp(z, tc);
                    gravl = JacchiaBowmanEquationsFactory.gravity(z);
                    ain   = gravl.divide(tloc4);
                    sum1  = sum1.add(ain.multiply(JacchiaBowmanEquationsFactory.WT[j]));
                }
                sum3 = sum3.add(dz.multiply(sum1));
            }
            final T altr;
            final double hSign;
            if (altKm.getReal() <= 500.) {
                altr = tloc3.divide(tloc2).log();
                fact2 = sub2.divide(JacchiaBowmanEquationsFactory.RSTAR);
                hSign = 1.0;
            } else {
                altr = tloc4.divide(tloc2).log();
                fact2 = sub2.add(sum3).divide(JacchiaBowmanEquationsFactory.RSTAR);
                hSign = -1.0;
            }
            for (int i = 0; i < 5; ++i) {
                aln[i] = aln[i].subtract(altr.multiply(1.0 + JacchiaBowmanEquationsFactory.ALPHA[i])).subtract(fact2.multiply(JacchiaBowmanEquationsFactory.AMW[i]));
            }

            // Equation (7)
            final T al10t5 = tinf.log10();
            final T alnh5 = al10t5.multiply(5.5).subtract(39.40).multiply(al10t5).add(73.13);
            aln[5] = alnh5.add(6.).multiply(JacchiaBowmanEquationsFactory.LOG10).
                     add(tloc4.divide(tloc3).log().add(sum3.multiply(JacchiaBowmanEquationsFactory.AMW[5] / JacchiaBowmanEquationsFactory.RSTAR)).multiply(hSign));
        }

        // Equation (24) - J70 Seasonal-Latitudinal Variation
        final T dlrsl = JacchiaBowmanEquationsFactory.dlrsl(altKm, dateMJD, satLat);

        // Equation (23) - Computes the semiannual variation
        T dlrsa = field.getZero();
        if (z.getReal() < 2000.0) {
            // Use new semiannual model dLog(rho)
            dlrsa = semian08(JacchiaBowmanEquationsFactory.dayOfYear(dateMJD), altKm, f10B, s10B, xm10B);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = LOG10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final T dlr = dlrsl.add(dlrsa).multiply(JacchiaBowmanEquationsFactory.LOG10);
        for (int i = 0; i < 6; ++i) {
            aln[i] = aln[i].add(dlr);
        }

        // Compute mass-density and mean-molecular-weight and
        // convert number density logs from natural to common.
        T sumnm = field.getZero();
        for (int i = 0; i < 6; ++i) {
            sumnm = sumnm.add(aln[i].exp().multiply(JacchiaBowmanEquationsFactory.AMW[i]));
        }
        rho = sumnm.divide(JacchiaBowmanEquationsFactory.AVOGAD);

        // Compute the high altitude exospheric density correction factor
        final T fex = JacchiaBowmanEquationsFactory.densityCorrectionFactor(altKm, f10B, field);

        // Apply the exospheric density correction factor.
        rho = rho.multiply(fex);

        return rho;
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
        final Vector3D sunPos = getSunPosition(date, ecef);
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
        final FieldVector3D<T> sunPos = getSunPosition(date, ecef);
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
