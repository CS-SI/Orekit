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
package org.orekit.models.earth;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/**
 * Klobuchar ionospheric delay model.
 * Klobuchar ionospheric delay model is designed as a GNSS correction model.
 * The parameters for the model are provided by the GPS satellites in their broadcast
 * messsage.
 * This model is based on the assumption the electron content is concentrated
 * in 350 km layer.
 *
 * The delay refers to L1 (1575.42 MHz).
 * If the delay is sought for L2 (1227.60 MHz), multiply the result by 1.65 (Klobuchar, 1996).
 * More generally, since ionospheric delay is inversely proportional to the square of the signal
 * frequency f, to adapt this model to other GNSS frequencies f, multiply by (L1 / f)^2.
 *
 * References:
 *     ICD-GPS-200, Rev. C, (1997), pp. 125-128
 *     Klobuchar, J.A., Ionospheric time-delay algorithm for single-frequency GPS users,
 *         IEEE Transactions on Aerospace and Electronic Systems, Vol. 23, No. 3, May 1987
 *     Klobuchar, J.A., "Ionospheric Effects on GPS", Global Positioning System: Theory and
 *         Applications, 1996, pp.513-514, Parkinson, Spilker.
 *
 * @author Joris Olympio
 * @since 7.1
 *
 */
public class KlobucharIonoModel implements IonosphericModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 7277525837842061107L;

    /** The 4 coefficients of a cubic equation representing the amplitude of the vertical delay. Units are sec/semi-circle^(i-1) for the i-th coefficient, i=1,2,3,4. */
    private final double[] alpha;
    /** The 4 coefficients of a cubic equation representing the period of the model. Units are sec/semi-circle^(i-1) for the i-th coefficient, i=1,2,3,4. */
    private final double[] beta;

    /** ratio of signal frequency with L1 frequency. */
    private final double ratio;

    /** Create a new Klobuchar ionospheric delay model, when single L1 frequency system is used.
     * This model accounts for at least 50 percent of RMS error due to ionospheric propagation effect (ICD-GPS-200)
     *
     * @param alpha coefficients of a cubic equation representing the amplitude of the vertical delay.
     * @param beta coefficients of a cubic equation representing the period of the model.
     */
    public KlobucharIonoModel(final double[] alpha, final double[] beta) {
        this.alpha = alpha.clone();
        this.beta  = beta.clone();
        this.ratio = 1.;
    }

    /** Create a new Klobuchar ionospheric delay model, when a single frequency system is used.
     * This model accounts for at least 50 percent of RMS error due to ionospheric propagation effect (ICD-GPS-200)
     *
     * @param alpha coefficients of a cubic equation representing the amplitude of the vertical delay.
     * @param beta coefficients of a cubic equation representing the period of the model.
     * @param frequency frequency of the radiowave signal in MHz
     */
    public KlobucharIonoModel(final double[] alpha, final double[] beta, final double frequency) {
        this.alpha = alpha.clone();
        this.beta  = beta.clone();
        this.ratio = FastMath.pow(1575.42 / frequency, 2);
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final AbsoluteDate date, final GeodeticPoint geo,
                                     final double elevation, final double azimuth) {
        // degees to semisircles
        final double rad2semi = 1. / FastMath.PI;
        final double semi2rad = FastMath.PI;

        // Earth Centered angle
        final double psi = 0.0137 / (elevation / FastMath.PI + 0.11) - 0.022;

        // Subionospheric latitude: the latitude of the IPP (Ionospheric Pierce Point)
        // in [-0.416, 0.416], semicircle
        final double latIono = FastMath.min(
                                      FastMath.max(geo.getLatitude() * rad2semi + psi * FastMath.cos(azimuth), -0.416),
                                      0.416);

        // Subionospheric longitude: the longitude of the IPP
        // in semicircle
        final double lonIono = geo.getLongitude() * rad2semi + (psi * FastMath.sin(azimuth) / FastMath.cos(latIono * semi2rad));

        // Geomagnetic latitude, semicircle
        final double latGeom = latIono + 0.064 * FastMath.cos((lonIono - 1.617) * semi2rad);

        // day of week and tow (sec)
        // Note: Sunday=0, Monday=1, Tuesday=2, Wednesday=3, Thursday=4, Friday=5, Saturday=6
        final DateTimeComponents dtc = date.getComponents(TimeScalesFactory.getGPS());
        final int dofweek = dtc.getDate().getDayOfWeek();
        final double secday = dtc.getTime().getSecondsInLocalDay();
        final double tow = dofweek * 86400. + secday;

        final double t = 43200. * lonIono + tow;
        final double tsec = t - FastMath.floor(t / 86400.) * 86400; // Seconds of day

        // Slant factor, semicircle
        final double slantFactor = 1.0 + 16.0 * FastMath.pow(0.53 - elevation / FastMath.PI, 3);

        // Period of model, seconds
        final double period = FastMath.max(72000., beta[0] + (beta[1]  + (beta[2] + beta[3] * latGeom) * latGeom) * latGeom);

        // Phase of the model, radians
        // (Max at 14.00 = 50400 sec local time)
        final double x = 2.0 * FastMath.PI * (tsec - 50400.0) / period;

        // Amplitude of the model, seconds
        final double amplitude = FastMath.max(0, alpha[0] + (alpha[1]  + (alpha[2] + alpha[3] * latGeom) * latGeom) * latGeom);

        // Ionospheric correction (L1)
        double ionoTimeDelayL1 = slantFactor * (5. * 1e-9);
        if (FastMath.abs(x) < 1.570) {
            ionoTimeDelayL1 += slantFactor * (amplitude * (1.0 - FastMath.pow(x, 2) / 2.0 + FastMath.pow(x, 4) / 24.0));
        }

        // Ionospheric delay for the L1 frequency, in meters, with slant correction.
        return ratio * Constants.SPEED_OF_LIGHT * ionoTimeDelayL1;
    }
}
