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
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.BodyShape;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
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
public class JB2008 extends AbstractJacchiaBowmanModel {

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
        super(sun, utc, earth,
              parameters == null ? AbsoluteDate.PAST_INFINITY : parameters.getMinDate(),
              parameters == null ? AbsoluteDate.FUTURE_INFINITY : parameters.getMaxDate());
        this.inputParams = parameters;
    }

    /** Get the local density with initial entries.
     * <p>
     * The method creates a new instance of {@link JB2008} model and set the input
     * solar activity data equal to the provided one. These data are then
     * available form {@link AbsoluteDate#PAST_INFINITY} to {@link AbsoluteDate#FUTURE_INFINITY}.
     * </p>
     * @param dateMJD date and time, in modified julian days and fraction
     * @param sunRA Right Ascension of Sun (radians).
     * @param sunDecli Declination of Sun (radians).
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
        final LocalProvider provider = new LocalProvider(f10, f10B, s10, s10B, xm10, xm10B, y10, y10B, dstdtc);
        final JB2008 modelWithLocalData = new JB2008(provider, getSun(), getEarth(), getUtc());
        final double mjd = FastMath.floor(dateMJD);
        final AbsoluteDate computationEpoch = AbsoluteDate.createMJDDate((int) mjd, (dateMJD - mjd) * Constants.JULIAN_DAY, getUtc());
        return modelWithLocalData.computeDensity(computationEpoch, sunRA, sunDecli, satLon, satLat, satAlt);
    }

    /** Get the local density with initial entries.
     * <p>
     * The method creates a new instance of {@link JB2008} model and set the input
     * solar activity data equal to the provided one. These data are then
     * available form {@link AbsoluteDate#PAST_INFINITY} to {@link AbsoluteDate#FUTURE_INFINITY}.
     * </p>
     * @param dateMJD date and time, in modified julian days and fraction
     * @param sunRA Right Ascension of Sun (radians).
     * @param sunDecli Declination of Sun (radians).
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
     * @param <T> type of the elements
     * @return total mass-Density at input position (kg/m³)
     */
    public <T extends CalculusFieldElement<T>> T getDensity(final T dateMJD, final T sunRA, final T sunDecli,
                                                            final T satLon, final T satLat, final T satAlt,
                                                            final double f10, final double f10B, final double s10,
                                                            final double s10B, final double xm10, final double xm10B,
                                                            final double y10, final double y10B, final double dstdtc) {
        final LocalProvider provider = new LocalProvider(f10, f10B, s10, s10B, xm10, xm10B, y10, y10B, dstdtc);
        final JB2008 modelWithLocalData = new JB2008(provider, getSun(), getEarth(), getUtc());
        final T mjd = FastMath.floor(dateMJD);
        final FieldAbsoluteDate<T> computationEpoch = FieldAbsoluteDate.createMJDDate((int) mjd.getReal(), dateMJD.subtract(mjd).multiply(Constants.JULIAN_DAY), getUtc());
        return modelWithLocalData.computeDensity(computationEpoch, sunRA, sunDecli, satLon, satLat, satAlt);
    }

        /** {@inheritDoc} */
    @Override
    protected double computeTInf(final AbsoluteDate date, final double tsubl, final double dtclst) {
        return tsubl + inputParams.getDSTDTC(date) + dtclst;
    }

    /** {@inheritDoc} */
    @Override
    protected double computeTc(final AbsoluteDate date) {
        final double f10   = inputParams.getF10(date);
        final double f10B  = inputParams.getF10B(date);
        final double s10   = inputParams.getS10(date);
        final double s10B  = inputParams.getS10B(date);
        final double xm10  = inputParams.getXM10(date);
        final double xm10B = inputParams.getXM10B(date);
        final double y10   = inputParams.getY10(date);
        final double y10B  = inputParams.getY10B(date);
        final double fn    = FastMath.min(1.0, FastMath.pow(f10B / 240., 0.25));
        final double fsb   = f10B * fn + s10B * (1. - fn);
        return 392.4 + 3.227 * fsb + 0.298 * (f10 - f10B) + 2.259 * (s10 - s10B) + 0.312 * (xm10 - xm10B) + 0.178 * (y10 - y10B);
    }

    /** {@inheritDoc} */
    @Override
    protected double getF10(final AbsoluteDate date) {
        return inputParams.getF10(date);
    }

    /** {@inheritDoc} */
    @Override
    protected double getF10B(final AbsoluteDate date) {
        return inputParams.getF10B(date);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T computeTInf(final AbsoluteDate date, final T tsubl, final T dtclst) {
        return tsubl.add(inputParams.getDSTDTC(date)).add(dtclst);
    }

    /** {@inheritDoc} */
    @Override
    protected double semian(final AbsoluteDate date, final double day, final double altKm) {

        final double f10B = inputParams.getF10B(date);
        final double s10B = inputParams.getS10B(date);
        final double xm10B = inputParams.getXM10B(date);

        final double htz = altKm / 1000.0;

        // COMPUTE NEW 81-DAY CENTERED SOLAR INDEX FOR FZ
        double fsmb = f10B - 0.70 * s10B - 0.04 * xm10B;

        // SEMIANNUAL AMPLITUDE
        final double fzz = FZM[0] + fsmb * (FZM[1] + htz * (FZM[2] + FZM[3] * htz + FZM[4] * fsmb));

        // COMPUTE DAILY 81-DAY CENTERED SOLAR INDEX FOR GT
        fsmb  = f10B - 0.75 * s10B - 0.37 * xm10B;

        // SEMIANNUAL PHASE FUNCTION
        final double tau   = MathUtils.TWO_PI * (day - 1.0) / 365;
        final SinCos sc1P = FastMath.sinCos(tau);
        final SinCos sc2P = SinCos.sum(sc1P, sc1P);
        final double gtz = GTM[0] + GTM[1] * sc1P.sin() + GTM[2] * sc1P.cos() + GTM[3] * sc2P.sin() + GTM[4] * sc2P.cos() +
                   fsmb * (GTM[5] + GTM[6] * sc1P.sin() + GTM[7] * sc1P.cos() + GTM[8] * sc2P.sin() + GTM[9] * sc2P.cos());

        return FastMath.max(1.0e-6, fzz) * gtz;

    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>>  T semian(final AbsoluteDate date, final T day, final T altKm) {

        final double f10B = inputParams.getF10B(date);
        final double s10B = inputParams.getS10B(date);
        final double xm10B = inputParams.getXM10B(date);

        final T htz = altKm.divide(1000.0);

        // COMPUTE NEW 81-DAY CENTERED SOLAR INDEX FOR FZ
        double fsmb = f10B - 0.70 * s10B - 0.04 * xm10B;

        // SEMIANNUAL AMPLITUDE
        final T fzz = htz.multiply(FZM[3]).add(FZM[2] + FZM[4] * fsmb).multiply(htz).add(FZM[1]).multiply(fsmb).add(FZM[0]);

        // COMPUTE DAILY 81-DAY CENTERED SOLAR INDEX FOR GT
        fsmb  = f10B - 0.75 * s10B - 0.37 * xm10B;

        // SEMIANNUAL PHASE FUNCTION
        final T tau   = day.subtract(1).divide(365).multiply(MathUtils.TWO_PI);
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

    /** Local provider for solar activity data. */
    private static class LocalProvider implements JB2008InputParameters {

        /** 10.7-cm Solar flux. */
        private double f10;

        /** 10.7-cm Solar Flux, averaged 81-day centered on the input time. */
        private double f10B;

        /** EUV index (26-34 nm) scaled to F10. */
        private double s10;

        /** UV 81-day averaged centered index. */
        private double s10B;

        /** MG2 index scaled to F10. */
        private double xm10;

        /** MG2 81-day ave. centered index. */
        private double xm10B;

        /** Solar X-Ray &amp; Lya index scaled to F10. */
        private double y10;

        /** Solar X-Ray &amp; Lya 81-day ave. centered index. */
        private double y10B;

        /** Temperature change computed from Dst index. */
        private double dstdtc;

        /** Constructor.
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
         */
        LocalProvider(final double f10, final double f10B, final double s10,
                      final double s10B, final double xm10, final double xm10B,
                      final double y10, final double y10B, final double dstdtc) {
            this.f10    = f10;
            this.f10B   = f10B;
            this.s10    = s10;
            this.s10B   = s10B;
            this.xm10   = xm10;
            this.xm10B  = xm10B;
            this.y10    = y10;
            this.y10B   = y10B;
            this.dstdtc = dstdtc;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getMinDate() {
            return AbsoluteDate.PAST_INFINITY;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getMaxDate() {
            return AbsoluteDate.FUTURE_INFINITY;
        }

        /** {@inheritDoc} */
        @Override
        public double getF10(final AbsoluteDate date) {
            return f10;
        }

        /** {@inheritDoc} */
        @Override
        public double getF10B(final AbsoluteDate date) {
            return f10B;
        }

        /** {@inheritDoc} */
        @Override
        public double getS10(final AbsoluteDate date) {
            return s10;
        }

        /** {@inheritDoc} */
        @Override
        public double getS10B(final AbsoluteDate date) {
            return s10B;
        }

        /** {@inheritDoc} */
        @Override
        public double getXM10(final AbsoluteDate date) {
            return xm10;
        }

        /** {@inheritDoc} */
        @Override
        public double getXM10B(final AbsoluteDate date) {
            return xm10B;
        }

        /** {@inheritDoc} */
        @Override
        public double getY10(final AbsoluteDate date) {
            return y10;
        }

        /** {@inheritDoc} */
        @Override
        public double getY10B(final AbsoluteDate date) {
            return y10B;
        }

        /** {@inheritDoc} */
        @Override
        public double getDSTDTC(final AbsoluteDate date) {
            return dstdtc;
        }
    }
}
