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
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.BodyShape;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.ExtendedPositionProvider;

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
public class JB2006 extends AbstractJacchiaBowmanModel {

    /** FZ global model values (1978-2004 fit). */
    private static final double[] FZM = { 0.111613e+00, -0.159000e-02, 0.126190e-01, -0.100064e-01, -0.237509e-04, 0.260759e-04};

    /** GT global model values (1978-2004 fit). */
    private static final double[] GTM = {-0.833646e+00, -0.265450e+00, 0.467603e+00, -0.299906e+00, -0.105451e+00,
                                         -0.165537e-01, -0.380037e-01, -0.150991e-01, -0.541280e-01,  0.119554e-01,
                                         0.437544e-02, -0.369016e-02, 0.206763e-02, -0.142888e-02, -0.867124e-05,
                                         0.189032e-04, 0.156988e-03, 0.491286e-03, -0.391484e-04, -0.126854e-04,
                                         0.134078e-04, -0.614176e-05, 0.343423e-05};

    /** External data container. */
    private final JB2006InputParameters inputParams;

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
    public JB2006(final JB2006InputParameters parameters, final ExtendedPositionProvider sun,
                  final BodyShape earth, final TimeScale utc) {
        super(sun, utc, earth, parameters.getMinDate(), parameters.getMaxDate());
        this.inputParams = parameters;
    }

    /** {@inheritDoc} */
    @Override
    protected double computeTInf(final AbsoluteDate date, final double tsubl, final double dtclst) {
        return tsubl + getDtg(date) + dtclst;
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T computeTInf(final AbsoluteDate date, final T tsubl, final T dtclst) {
        return tsubl.add(getDtg(date)).add(dtclst);
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
        return 379 + 3.353 * f10B + 0.358 * (f10 - f10B) + 2.094 * (s10 - s10B) + 0.343 * (xm10 - xm10B);
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
    protected double semian(final AbsoluteDate date, final double day, final double height) {

        final double f10Bar = inputParams.getF10B(date);
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
    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T semian(final AbsoluteDate date, final T doy, final T height) {

        final double f10Bar = getF10B(date);
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

    /** Computes the temperature computed by Equation (18).
     * @param date computation epoch
     * @return the temperature given by Equation (18)
     */
    private double getDtg(final AbsoluteDate date) {
        // Equation (18)
        final double ap = inputParams.getAp(date);
        final double expAp = FastMath.exp(-0.08 * ap);
        return ap + 100. * (1. - expAp);
    }
}
