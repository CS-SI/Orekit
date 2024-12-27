/* Copyright 2002-2024 Thales Alenia Space
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
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;

/** Galileo-specific version of NeQuick engine.
 * @since 13.0
 * @author Luc Maisonobe
 */
public class NeQuickGalileo extends NeQuickModel {

    /** Starting number of points for integration. */
    private static final int N_START = 8;

    /** Broadcast ionization engine coefficients. */
    private final double[] alpha;

    /**
     * Build a new instance.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param alpha effective ionisation level coefficients
     * @see #NeQuickGalileo(double[], TimeScale)
     */
    @DefaultDataContext
    public NeQuickGalileo(final double[] alpha) {
        this(alpha, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Build a new instance of the Galileo version of the NeQuick-2 model.
     * <p>
     * The Galileo version uses a loose modip grid and 3 broadcast parameters to compute
     * effective ionization level.
     * </p>
     * @param alpha broadcast effective ionisation level coefficients
     * @param utc UTC time scale.
     * @since 10.1
     */
    public NeQuickGalileo(final double[] alpha, final TimeScale utc) {
        super(utc);
        this.alpha = alpha.clone();
    }

    /**
     * Compute effective ionization level.
     *
     * @param modip modified dip latitude at receiver location
     * @return effective ionization level (Az in Nequick Galileo, R12 in original Nequick ITU)
     */
    private double effectiveIonizationLevel(final double modip) {
        // Particular condition (Eq. 17)
        if (alpha[0] == 0.0 && alpha[1] == 0.0 && alpha[2] == 0.0) {
            return 63.7;
        } else {
            // Az = a0 + modip * a1 + modip² * a2 (Eq. 18)
            return FastMath.min(FastMath.max(alpha[0] + modip * (alpha[1] + modip * alpha[2]), 0.0), 400.0);
        }
    }

    /**
     * Compute effective ionization level.
     *
     * @param <T>   type of the field elements
     * @param modip modified dip latitude at receiver location
     * @return effective ionization level (Az in Nequick Galileo, R12 in original Nequick ITU)
     */
    private <T extends CalculusFieldElement<T>> T effectiveIonizationLevel(final T modip) {
        // Particular condition (Eq. 17)
        if (alpha[0] == 0.0 && alpha[1] == 0.0 && alpha[2] == 0.0) {
            return modip.newInstance(63.7);
        } else {
            // Az = a0 + modip * a1 + modip² * a2 (Eq. 18)
            return FastMath.min(FastMath.max(modip.multiply(alpha[2]).add(alpha[1]).multiply(modip).add(alpha[0]),
                                             0.0),
                                400.0);
            }
    }

    /** {@inheritDoc} */
    @Override
    double stec(final DateTimeComponents dateTime, final Ray ray) {

        // Tolerance for the integration accuracy. Defined inside the reference document, section 2.5.8.1.
        final double h1 = ray.getRecH();
        final double tolerance;
        if (h1 < 1000000.0) {
            tolerance = 0.001;
        } else {
            tolerance = 0.01;
        }

        // Integration
        int n = N_START;
        final Segment seg1 = new Segment(n, ray, ray.getS1(), ray.getS2());
        double gn1 = stecIntegration(dateTime, seg1);
        n *= 2;
        final Segment seg2 = new Segment(n, ray, ray.getS1(), ray.getS2());
        double gn2 = stecIntegration(dateTime, seg2);

        int count = 1;
        while (FastMath.abs(gn2 - gn1) > tolerance * FastMath.abs(gn1) && count < 20) {
                gn1 = gn2;
                n *= 2;
                final Segment seg = new Segment(n, ray, ray.getS1(), ray.getS2());
                gn2 = stecIntegration(dateTime, seg);
                count += 1;
        }

        // If count > 20 the integration did not converge
        if (count == 20) {
                throw new OrekitException(OrekitMessages.STEC_INTEGRATION_DID_NOT_CONVERGE);
        }

        // Eq. 202
        return (gn2 + ((gn2 - gn1) / 15.0)) * 1.0e-16;

    }

    /** {@inheritDoc} */
    @Override
    <T extends CalculusFieldElement<T>> T stec(final DateTimeComponents dateTime, final FieldRay<T> ray) {

        // Tolerance for the integration accuracy. Defined inside the reference document, section 2.5.8.1.
        final T h1 = ray.getRecH();
        final double tolerance;
        if (h1.getReal() < 1000000.0) {
            tolerance = 0.001;
        } else {
            tolerance = 0.01;
        }

        // Integration
        int n = N_START;
        final FieldSegment<T> seg1 = new FieldSegment<>(n, ray, ray.getS1(), ray.getS2());
        T gn1 = stecIntegration(dateTime, seg1);
        n *= 2;
        final FieldSegment<T> seg2 = new FieldSegment<>(n, ray, ray.getS1(), ray.getS2());
        T gn2 = stecIntegration(dateTime, seg2);

        int count = 1;
        while (FastMath.abs(gn2.subtract(gn1)).getReal() > FastMath.abs(gn1).multiply(tolerance)
            .getReal() && count < 20) {
                gn1 = gn2;
                n *= 2;
                final FieldSegment<T> seg = new FieldSegment<>(n, ray, ray.getS1(), ray.getS2());
                gn2 = stecIntegration(dateTime, seg);
                count += 1;
        }

        // If count > 20 the integration did not converge
        if (count == 20) {
                throw new OrekitException(OrekitMessages.STEC_INTEGRATION_DID_NOT_CONVERGE);
        }

        // Eq. 202
        return gn2.add(gn2.subtract(gn1).divide(15.0)).multiply(1.0e-16);

    }

    /**
     * This method performs the STEC integration.
     *
     * @param dateTime current date and time components
     * @param seg      coordinates along the integration path
     * @return result of the integration
     */
    private double stecIntegration(final DateTimeComponents dateTime, final Segment seg) {

        // Compute electron density
        double density = 0.0;
        for (int i = 0; i < seg.getNbPoints(); i++) {
            final GeodeticPoint gp = seg.getPoint(i);
            final double modip = GalileoHolder.INSTANCE.computeMODIP(gp.getLatitude(), gp.getLongitude());
            final double az = effectiveIonizationLevel(modip);
            density +=
                electronDensity(dateTime, modip, az, gp.getLatitude(), gp.getLongitude(), gp.getAltitude());
        }

        return 0.5 * seg.getInterval() * density;
    }

    /**
     * This method performs the STEC integration.
     *
     * @param <T>      type of the elements
     * @param dateTime current date and time components
     * @param seg      coordinates along the integration path
     * @return result of the integration
     */
    private <T extends CalculusFieldElement<T>> T stecIntegration(final DateTimeComponents dateTime,
                                                              final FieldSegment<T> seg) {
        // Compute electron density
        T density = seg.getInterval().getField().getZero();
        for (int i = 0; i < seg.getNbPoints(); i++) {
            final FieldGeodeticPoint<T> gp = seg.getPoint(i);
            final T modip = GalileoHolder.INSTANCE.computeMODIP(gp.getLatitude(), gp.getLongitude());
            final T az = effectiveIonizationLevel(modip);
            density =
                density.add(electronDensity(dateTime, modip, az, gp.getLatitude(), gp.getLongitude(),
                                            gp.getAltitude()));
        }

        return seg.getInterval().multiply(density).multiply(0.5);
    }

    /** {@inheritDoc} */
    @Override
    double clipH(final double hInKm) {
        // If h < 100km we use h = 100km as recommended in the reference document
        // for equations 111 to 113
        return FastMath.max(hInKm, 100.0);
    }

    /** {@inheritDoc} */
    @Override
    <T extends CalculusFieldElement<T>> T clipH(final T hInKm) {
        // If h < 100km we use h = 100km as recommended in the reference document
        // for equations 111 to 113
        return FastMath.max(hInKm, 100);
    }

    /** {@inheritDoc} */
    @Override
    boolean applyChapmanParameters(final double hInKm) {
        return hInKm < 100.0;
    }

    /** Holder for the Galileo-specific modip singleton.
     * <p>
     * We use the initialization on demand holder idiom to store the singleton,
     * as it is both thread-safe, efficient (no synchronization) and works with
     * all versions of java.
     * </p>
     */
    private static class GalileoHolder {

        /** Unique instance. */
        private static final ModipGrid INSTANCE =
            new ModipGrid(NeQuickVersion.NEQUICK_2_GALILEO.getnbCellsLon(),
                          NeQuickVersion.NEQUICK_2_GALILEO.getnbCellsLat(),
                          NeQuickVersion.NEQUICK_2_GALILEO.getSource(),
                          NeQuickVersion.NEQUICK_2_GALILEO.isWrappingAlreadyIncluded());

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private GalileoHolder() {
        }

    }

}
