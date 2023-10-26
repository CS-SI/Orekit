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
package org.orekit.models.earth.ionosphere;

import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

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

    /** The 4 coefficients of a cubic equation representing the amplitude of the vertical delay. Units are sec/semi-circle^(i-1) for the i-th coefficient, i=1, 2, 3, 4. */
    private final double[] alpha;

    /** The 4 coefficients of a cubic equation representing the period of the model. Units are sec/semi-circle^(i-1) for the i-th coefficient, i=1, 2, 3, 4. */
    private final double[] beta;

    /** GPS time scale. */
    private final TimeScale gps;

    /** Create a new Klobuchar ionospheric delay model, when a single frequency system is used.
     * This model accounts for at least 50 percent of RMS error due to ionospheric propagation effect (ICD-GPS-200)
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param alpha coefficients of a cubic equation representing the amplitude of the vertical delay.
     * @param beta coefficients of a cubic equation representing the period of the model.
     * @see #KlobucharIonoModel(double[], double[], TimeScale)
     */
    @DefaultDataContext
    public KlobucharIonoModel(final double[] alpha, final double[] beta) {
        this(alpha, beta, DataContext.getDefault().getTimeScales().getGPS());
    }

    /**
     * Create a new Klobuchar ionospheric delay model, when a single frequency system is
     * used. This model accounts for at least 50 percent of RMS error due to ionospheric
     * propagation effect (ICD-GPS-200)
     *
     * @param alpha coefficients of a cubic equation representing the amplitude of the
     *              vertical delay.
     * @param beta  coefficients of a cubic equation representing the period of the
     *              model.
     * @param gps   GPS time scale.
     * @since 10.1
     */
    public KlobucharIonoModel(final double[] alpha,
                              final double[] beta,
                              final TimeScale gps) {
        this.alpha = alpha.clone();
        this.beta  = beta.clone();
        this.gps = gps;
    }

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite.
     * <p>
     * The path delay is computed for any elevation angle.
     * </p>
     * @param date current date
     * @param geo geodetic point of receiver/station
     * @param elevation elevation of the satellite in radians
     * @param azimuth azimuth of the satellite in radians
     * @param frequency frequency of the signal in Hz
     * @param parameters ionospheric model parameters
     * @return the path delay due to the ionosphere in m
     */
    public double pathDelay(final AbsoluteDate date, final GeodeticPoint geo,
                            final double elevation, final double azimuth, final double frequency,
                            final double[] parameters) {

        // Sine and cosine of the azimuth
        final SinCos sc = FastMath.sinCos(azimuth);

        // degrees to semicircles
        final double rad2semi = 1. / FastMath.PI;
        final double semi2rad = FastMath.PI;

        // Earth Centered angle
        final double psi = 0.0137 / (elevation / FastMath.PI + 0.11) - 0.022;

        // Subionospheric latitude: the latitude of the IPP (Ionospheric Pierce Point)
        // in [-0.416, 0.416], semicircle
        final double latIono = FastMath.min(
                                      FastMath.max(geo.getLatitude() * rad2semi + psi * sc.cos(), -0.416),
                                      0.416);

        // Subionospheric longitude: the longitude of the IPP
        // in semicircle
        final double lonIono = geo.getLongitude() * rad2semi + (psi * sc.sin() / FastMath.cos(latIono * semi2rad));

        // Geomagnetic latitude, semicircle
        final double latGeom = latIono + 0.064 * FastMath.cos((lonIono - 1.617) * semi2rad);

        // day of week and tow (sec)
        // Note: Sunday=0, Monday=1, Tuesday=2, Wednesday=3, Thursday=4, Friday=5, Saturday=6
        final DateTimeComponents dtc = date.getComponents(gps);
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
        final double ratio = FastMath.pow(1575.42e6 / frequency, 2);
        return ratio * Constants.SPEED_OF_LIGHT * ionoTimeDelayL1;
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final SpacecraftState state, final TopocentricFrame baseFrame,
                            final double frequency, final double[] parameters) {

        // Elevation in radians
        final Vector3D position  = state.getPosition(baseFrame);
        final double   elevation = position.getDelta();

        // Only consider measures above the horizon
        if (elevation > 0.0) {
            // Date
            final AbsoluteDate date = state.getDate();
            // Geodetic point
            final GeodeticPoint geo = baseFrame.getPoint();
            // Azimuth angle in radians
            double azimuth = FastMath.atan2(position.getX(), position.getY());
            if (azimuth < 0.) {
                azimuth += MathUtils.TWO_PI;
            }
            // Delay
            return pathDelay(date, geo, elevation, azimuth, frequency, parameters);
        }

        return 0.0;
    }

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite.
     * <p>
     * The path delay is computed for any elevation angle.
     * </p>
     * @param <T> type of the elements
     * @param date current date
     * @param geo geodetic point of receiver/station
     * @param elevation elevation of the satellite in radians
     * @param azimuth azimuth of the satellite in radians
     * @param frequency frequency of the signal in Hz
     * @param parameters ionospheric model parameters
     * @return the path delay due to the ionosphere in m
     */
    public <T extends CalculusFieldElement<T>> T pathDelay(final FieldAbsoluteDate<T> date, final FieldGeodeticPoint<T> geo,
                                                       final T elevation, final T azimuth, final double frequency,
                                                       final T[] parameters) {

        // Sine and cosine of the azimuth
        final FieldSinCos<T> sc = FastMath.sinCos(azimuth);

        // Field
        final Field<T> field = date.getField();
        final T zero = field.getZero();
        final T one  = field.getOne();

        // degrees to semicircles
        final T pi       = one.getPi();
        final T rad2semi = pi.reciprocal();

        // Earth Centered angle
        final T psi = elevation.divide(pi).add(0.11).divide(0.0137).reciprocal().subtract(0.022);

        // Subionospheric latitude: the latitude of the IPP (Ionospheric Pierce Point)
        // in [-0.416, 0.416], semicircle
        final T latIono = FastMath.min(
                                      FastMath.max(geo.getLatitude().multiply(rad2semi).add(psi.multiply(sc.cos())), zero.subtract(0.416)),
                                      zero.add(0.416));

        // Subionospheric longitude: the longitude of the IPP
        // in semicircle
        final T lonIono = geo.getLongitude().multiply(rad2semi).add(psi.multiply(sc.sin()).divide(FastMath.cos(latIono.multiply(pi))));

        // Geomagnetic latitude, semicircle
        final T latGeom = latIono.add(FastMath.cos(lonIono.subtract(1.617).multiply(pi)).multiply(0.064));

        // day of week and tow (sec)
        // Note: Sunday=0, Monday=1, Tuesday=2, Wednesday=3, Thursday=4, Friday=5, Saturday=6
        final DateTimeComponents dtc = date.getComponents(gps);
        final int dofweek = dtc.getDate().getDayOfWeek();
        final double secday = dtc.getTime().getSecondsInLocalDay();
        final double tow = dofweek * 86400. + secday;

        final T t = lonIono.multiply(43200.).add(tow);
        final T tsec = t.subtract(FastMath.floor(t.divide(86400.)).multiply(86400.)); // Seconds of day

        // Slant factor, semicircle
        final T slantFactor = FastMath.pow(elevation.divide(pi).negate().add(0.53), 3).multiply(16.0).add(one);

        // Period of model, seconds
        final T period = FastMath.max(zero.add(72000.), latGeom.multiply(latGeom.multiply(latGeom.multiply(beta[3]).add(beta[2])).add(beta[1])).add(beta[0]));

        // Phase of the model, radians
        // (Max at 14.00 = 50400 sec local time)
        final T x = tsec.subtract(50400.0).multiply(pi.multiply(2.0)).divide(period);

        // Amplitude of the model, seconds
        final T amplitude = FastMath.max(zero, latGeom.multiply(latGeom.multiply(latGeom.multiply(alpha[3]).add(alpha[2])).add(alpha[1])).add(alpha[0]));

        // Ionospheric correction (L1)
        T ionoTimeDelayL1 = slantFactor.multiply(5. * 1e-9);
        if (FastMath.abs(x.getReal()) < 1.570) {
            ionoTimeDelayL1 = ionoTimeDelayL1.add(slantFactor.multiply(amplitude.multiply(one.subtract(FastMath.pow(x, 2).multiply(0.5)).add(FastMath.pow(x, 4).divide(24.0)))));
        }

        // Ionospheric delay for the L1 frequency, in meters, with slant correction.
        final double ratio = FastMath.pow(1575.42e6 / frequency, 2);
        return ionoTimeDelayL1.multiply(Constants.SPEED_OF_LIGHT).multiply(ratio);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final FieldSpacecraftState<T> state, final TopocentricFrame baseFrame,
                                                       final double frequency, final T[] parameters) {

        // Elevation and azimuth in radians
        final FieldVector3D<T> position = state.getPosition(baseFrame);
        final T elevation = position.getDelta();

        if (elevation.getReal() > 0.0) {
            // Date
            final FieldAbsoluteDate<T> date = state.getDate();
            // Geodetic point
            final FieldGeodeticPoint<T> geo = baseFrame.getPoint(date.getField());
            // Azimuth angle in radians
            T azimuth = FastMath.atan2(position.getX(), position.getY());
            if (azimuth.getReal() < 0.) {
                azimuth = azimuth.add(MathUtils.TWO_PI);
            }
            // Delay
            return pathDelay(date, geo, elevation, azimuth, frequency, parameters);
        }

        return elevation.getField().getZero();
    }

    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }
}
