/* Copyright 2022-2025 Thales Alenia Space
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.ionosphere.nequick;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataSource;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.units.Unit;

/** Original model from Aeronomy and Radiopropagation Laboratory
 * of the Abdus Salam International Centre for Theoretical Physics Trieste, Italy.
 * <p>
 * None of the code from Abdus Salam International Centre for Theoretical Physics Trieste
 * has been used, the models have been reimplemented from scratch by the Orekit team.
 * </p>
 * @since 13.0
 * @author Luc Maisonobe
 */
public class NeQuickItu extends NeQuickModel {

    /** One thousand kilometer height. */
    private static final double H_1000 = 1000000.0;

    /** Two thousands kilometer height. */
    private static final double H_2000 = 2000000.0;

    /** H0 (km). */
    private static final double H0 = 90.0;

    /** HD (km). */
    private static final double HD = 5.0;

    /** Starting number of points for integration. */
    private static final int N_START = 8;

    /** Max number of points for integration. */
    private static final int N_STOP = 1024;

    /** Small convergence criterion. */
    private static final double EPS_SMALL = 1.0e-3;

    /** Medium convergence criterion. */
    private static final double EPS_MEDIUM = 1.0e-2;

    /** Solar flux. */
    private final double f107;

    /** Build a new instance.
     * @param f107 solar flux
     * @param utc UTC time scale
     */
    public NeQuickItu(final double f107, final TimeScale utc) {
        super(utc);
        this.f107 = f107;
    }

    /** Get solar flux.
     * @return solar flux
     */
    public double getF107() {
        return f107;
    }

    /** {@inheritDoc} */
    @Override
    double stec(final DateTimeComponents dateTime, final Ray ray) {
        if (ray.getSatH() <= H_2000) {
            if (ray.getRecH() >= H_1000) {
                // only one integration interval
                return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), ray.getS2());
            } else {
                // two integration intervals, below and above 1000km
                final double h1000 = NeQuickModel.RE + H_1000;
                final double s1000 = FastMath.sqrt(h1000 * h1000 - ray.getRadius() * ray.getRadius());
                return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s1000) +
                       stecIntegration(dateTime, EPS_MEDIUM, ray, s1000, ray.getS2());
            }
        } else {
            if (ray.getRecH() >= H_2000) {
                // only one integration interval
                return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), ray.getS2());
            } else {
                final double h2000 = NeQuickModel.RE + H_2000;
                final double s2000 = FastMath.sqrt(h2000 * h2000 - ray.getRadius() * ray.getRadius());
                if (ray.getRecH() >= H_1000) {
                    // two integration intervals, below and above 2000km
                    return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s2000) +
                           stecIntegration(dateTime, EPS_SMALL, ray, s2000, ray.getS2());
                } else {
                    // three integration intervals, below 1000km, between 1000km and 2000km, and above 2000km
                    final double h1000 = NeQuickModel.RE + H_1000;
                    final double s1000 = FastMath.sqrt(h1000 * h1000 - ray.getRadius() * ray.getRadius());
                    return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s1000) +
                           stecIntegration(dateTime, EPS_MEDIUM, ray, s1000, s2000) +
                           stecIntegration(dateTime, EPS_MEDIUM, ray, s2000, ray.getS2());
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    <T extends CalculusFieldElement<T>> T stec(final DateTimeComponents dateTime, final FieldRay<T> ray) {
        if (ray.getSatH().getReal() <= H_2000) {
            if (ray.getRecH().getReal() >= H_1000) {
                // only one integration interval
                return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), ray.getS2());
            } else {
                // two integration intervals, below and above 1000km
                final double h1000 = NeQuickModel.RE + H_1000;
                final T s1000 = FastMath.sqrt(ray.getRadius().multiply(ray.getRadius()).negate().add(h1000 * h1000));
                return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s1000).
                       add(stecIntegration(dateTime, EPS_MEDIUM, ray, s1000, ray.getS2()));
            }
        } else {
            if (ray.getRecH().getReal() >= H_2000) {
                // only one integration interval
                return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), ray.getS2());
            } else {
                final double h2000 = NeQuickModel.RE + H_2000;
                final T s2000 = FastMath.sqrt(ray.getRadius().multiply(ray.getRadius()).negate().add(h2000 * h2000));
                if (ray.getRecH().getReal() >= H_1000) {
                    // two integration intervals, below and above 2000km
                    return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s2000).
                           add(stecIntegration(dateTime, EPS_SMALL, ray, s2000, ray.getS2()));
                } else {
                    // three integration intervals, below 1000km, between 1000km and 2000km, and above 2000km
                    final double h1000 = NeQuickModel.RE + H_1000;
                    final T s1000 = FastMath.sqrt(ray.getRadius().multiply(ray.getRadius()).negate().add(h1000 * h1000));
                    return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s1000).
                           add(stecIntegration(dateTime, EPS_MEDIUM, ray, s1000, s2000)).
                           add(stecIntegration(dateTime, EPS_MEDIUM, ray, s2000, ray.getS2()));
                }
            }
        }
    }

    /**
     * This method performs the STEC integration.
     *
     * @param dateTime current date and time components
     * @param eps convergence criterion
     * @param ray ray-perigee parameters
     * @param s1  lower boundary of integration
     * @param s2  upper boundary for integration
     * @return result of the integration
     */
    private double stecIntegration(final DateTimeComponents dateTime, final double eps, final Ray ray, final double s1,
                                   final double s2) {

        double gInt1 = Double.NaN;
        double gInt2 = Double.NaN;

        for (int n = N_START; n <= N_STOP; n = 2 * n) {

            // integrate with n intervals (2n points)
            final Segment segment = new Segment(n, ray, s1, s2);
            double sum = 0;
            for (int i = 0; i < segment.getNbPoints(); ++i) {
                final GeodeticPoint gp = segment.getPoint(i);
                final double ed = electronDensity(dateTime, f107,
                                                  gp.getLatitude(), gp.getLongitude(), gp.getAltitude());
                sum += ed;
            }

            gInt1 = gInt2;
            gInt2 = sum * 0.5 * segment.getInterval();
            if (FastMath.abs(gInt1 - gInt2) <= FastMath.abs(gInt1 * eps)) {
                // convergence reached
                break;
            }

        }

        return Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(gInt2 + (gInt2 - gInt1) / 15.0);

    }

    /**
     * This method performs the STEC integration.
     *
     * @param <T> type of the field elements
     * @param dateTime current date and time components
     * @param eps convergence criterion
     * @param ray ray-perigee parameters
     * @param s1  lower boundary of integration
     * @param s2  upper boundary for integration
     * @return result of the integration
     */
    private <T extends CalculusFieldElement<T>> T stecIntegration(final DateTimeComponents dateTime,
                                                                  final double eps,
                                                                  final FieldRay<T> ray, final T s1, final T s2) {

        T gInt1 = s1.newInstance(Double.NaN);
        T gInt2 = s1.newInstance(Double.NaN);
        final T f107T = s1.newInstance(f107);

        for (int n = N_START; n <= N_STOP; n = 2 * n) {

            // integrate with n intervals (2n points)
            final FieldSegment<T> segment = new FieldSegment<>(n, ray, s1, s2);
            T sum = s1.getField().getZero();
            for (int i = 0; i < segment.getNbPoints(); ++i) {
                final FieldGeodeticPoint<T> gp = segment.getPoint(i);
                final T ed =  electronDensity(dateTime, f107T,
                                              gp.getLatitude(), gp.getLongitude(), gp.getAltitude());
                sum = sum.add(ed);
            }

            gInt1 = gInt2;
            gInt2 = sum.multiply(0.5).multiply(segment.getInterval());
            if (FastMath.abs(gInt1.subtract(gInt2).getReal()) <= FastMath.abs(gInt1.getReal() * eps)) {
                // convergence reached
                break;
            }

        }

        return Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(gInt2.add(gInt2.subtract(gInt1).divide(15.0)));

    }

    /** {@inheritDoc} */
    @Override
    protected double computeMODIP(final double latitude, final double longitude) {
        return ItuHolder.INSTANCE.computeMODIP(latitude, longitude);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T computeMODIP(final T latitude, final T longitude) {
        return ItuHolder.INSTANCE.computeMODIP(latitude, longitude);
    }

    /** {@inheritDoc} */
    @Override
    boolean applyChapmanParameters(final double hInKm) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    double[] computeExponentialArguments(final double h, final NeQuickParameters parameters) {
        final double   exp   = clipExp(10.0 / (1.0 + FastMath.abs(h - parameters.getHmF2())));
        final double[] arguments = new double[3];
        arguments[0] = fixLowHArg( (h - parameters.getHmF2()) / parameters.getB2Bot(), h);
        arguments[1] = fixLowHArg(((h - parameters.getHmF1()) / parameters.getBF1(h)) * exp, h);
        arguments[2] = fixLowHArg(((h - parameters.getHmE())  / parameters.getBE(h))  * exp, h);
        return arguments;
    }

    /** {@inheritDoc} */
    @Override
    <T extends CalculusFieldElement<T>> T[] computeExponentialArguments(final T h,
                                                                        final FieldNeQuickParameters<T> parameters) {
        final T   exp   = clipExp(FastMath.abs(h.subtract(parameters.getHmF2())).add(1.0).reciprocal().multiply(10.0));
        final T[] arguments = MathArrays.buildArray(h.getField(), 3);
        arguments[0] = fixLowHArg(h.subtract(parameters.getHmF2()).divide(parameters.getB2Bot()), h);
        arguments[1] = fixLowHArg(h.subtract(parameters.getHmF1()).divide(parameters.getBF1(h)).multiply(exp), h);
        arguments[2] = fixLowHArg(h.subtract(parameters.getHmE()).divide(parameters.getBE(h)).multiply(exp), h);
        return arguments;
    }

    /**
     * Fix arguments for low altitudes.
     * @param arg argument of the exponential
     * @param h height in km
     * @return fixed argument
     * @since 13.0
     */
    private double fixLowHArg(final double arg, final double h) {
        return h < H0 ? arg * (HD + H0 - h) / HD : arg;
    }

    /**
     * Fix arguments for low altitudes.
     * @param <T> type of the field elements
     * @param arg argument of the exponential
     * @param h height in km
     * @return fixed argument
     * @since 13.0
     */
    private <T extends CalculusFieldElement<T>> T fixLowHArg(final T arg, final T h) {
        return h.getReal() < H0 ? arg.multiply(h.negate().add(HD + H0)).divide(HD) : arg;
    }

    /** {@inheritDoc} */
    @Override
    double computeH0(final NeQuickParameters parameters) {
        final double b2k = -0.0538  * parameters.getFoF2() -
                            0.00664 * parameters.getHmF2() +
                            0.113   * parameters.getHmF2() / parameters.getB2Bot() +
                            0.00257 * parameters.getAzr() +
                            3.22;
        return parameters.getB2Bot() * parameters.join(b2k, 1.0, 2.0, b2k - 1.0);
    }

    /** {@inheritDoc} */
    @Override
    <T extends CalculusFieldElement<T>> T computeH0(final FieldNeQuickParameters<T> parameters) {
        final T b2k = parameters.getFoF2().multiply(-0.0538).
                      subtract(parameters.getHmF2().multiply(0.00664)).
                      add(parameters.getHmF2().multiply(0.113).divide(parameters.getB2Bot())).
                      add(parameters.getAzr().multiply(0.00257)).
                      add (3.22);
        return parameters.getB2Bot().
               multiply(parameters.join(b2k, b2k.newInstance(1.0), b2k.newInstance(2.0), b2k.subtract(1.0)));
    }

    /** Holder for the ITU modip singleton.
     * <p>
     * We use the initialization on demand holder idiom to store the singleton,
     * as it is both thread-safe, efficient (no synchronization) and works with
     * all versions of java.
     * </p>
     */
    private static class ItuHolder {

        /** Resource for modip grid. */
        private static final String MODIP_GRID = "/assets/org/orekit/nequick/modip.asc";

        /** Unique instance. */
        private static final ModipGrid INSTANCE =
            new ModipGrid(180, 180,
                          new DataSource(MODIP_GRID, () -> ItuHolder.class.getResourceAsStream(MODIP_GRID)),
                          false);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private ItuHolder() {
        }

    }

}
