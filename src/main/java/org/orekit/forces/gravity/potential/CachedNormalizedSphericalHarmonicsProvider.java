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
package org.orekit.forces.gravity.potential;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.orekit.errors.OrekitException;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.TimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

/** Caching wrapper for {@link NormalizedSphericalHarmonicsProvider}.
 * <p>
 * This wrapper improves efficiency of {@link NormalizedSphericalHarmonicsProvider}
 * by sampling the values at a user defined rate and using interpolation
 * between samples. This is important with providers that have sub-daily
 * frequencies and are computing intensive, such as tides fields.
 * </p>
 * @see NormalizedSphericalHarmonicsProvider
 * @see org.orekit.forces.gravity.SolidTides
 * @see TimeStampedCache
 * @author Luc Maisonobe
 * @since 6.1
 */
public class CachedNormalizedSphericalHarmonicsProvider implements NormalizedSphericalHarmonicsProvider {

    /** Underlying raw provider. */
    private final NormalizedSphericalHarmonicsProvider rawProvider;

    /** Number of coefficients in C<sub>n, m</sub> and S<sub>n, m</sub> arrays (counted separately). */
    private final int size;

    /** Cache. */
    private final TimeStampedCache<TimeStampedSphericalHarmonics> cache;

    /** Simple constructor.
     * @param rawProvider underlying raw provider
     * @param step time step between sample points for interpolation
     * @param nbPoints number of points to use for interpolation, must be at least 2
     * @param maxSlots maximum number of independent cached time slots
     * @param maxSpan maximum duration span in seconds of one slot
     * (can be set to {@code Double.POSITIVE_INFINITY} if desired)
     * @param newSlotInterval time interval above which a new slot is created
     * instead of extending an existing one
     */
    public CachedNormalizedSphericalHarmonicsProvider(final NormalizedSphericalHarmonicsProvider rawProvider,
                                                      final double step, final int nbPoints,
                                                      final int maxSlots, final double maxSpan,
                                                      final double newSlotInterval) {

        this.rawProvider  = rawProvider;
        final int k       = rawProvider.getMaxDegree() + 1;
        this.size         = (k * (k + 1)) / 2;

        cache = new GenericTimeStampedCache<TimeStampedSphericalHarmonics>(nbPoints, maxSlots, maxSpan,
                                                                           newSlotInterval, new Generator(step),
                                                                           TimeStampedSphericalHarmonics.class);
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxDegree() {
        return rawProvider.getMaxDegree();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOrder() {
        return rawProvider.getMaxOrder();
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return rawProvider.getMu();
    }

    /** {@inheritDoc} */
    @Override
    public double getAe() {
        return rawProvider.getAe();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getReferenceDate() {
        return rawProvider.getReferenceDate();
    }

    /** {@inheritDoc} */
    @Override
    public double getOffset(final AbsoluteDate date) {
        return rawProvider.getOffset(date);
    }

    /** {@inheritDoc} */
    @Override
    public TideSystem getTideSystem() {
        return rawProvider.getTideSystem();
    }

    /** {@inheritDoc} */
    @Override
    public NormalizedSphericalHarmonics onDate(final AbsoluteDate date) throws
            TimeStampedCacheException {
        return TimeStampedSphericalHarmonics.interpolate(date, cache.getNeighbors(date));
    }

    /** Generator for time-stamped spherical harmonics. */
    private class Generator implements TimeStampedGenerator<TimeStampedSphericalHarmonics> {

        /** Time step between generated sets. */
        private final double step;

        /** Simple constructor.
         * @param step time step between generated sets
         */
        Generator(final double step) {
            this.step = step;
        }

        /** {@inheritDoc} */
        @Override
        public List<TimeStampedSphericalHarmonics> generate(final TimeStampedSphericalHarmonics existing,
                                                            final AbsoluteDate date)
            throws TimeStampedCacheException {
            try {

                final List<TimeStampedSphericalHarmonics> generated =
                        new ArrayList<TimeStampedSphericalHarmonics>();
                final double[] cnmsnm = new double[2 * size];

                if (existing == null) {

                    // no prior existing transforms, just generate a first set
                    for (int i = 0; i < cache.getNeighborsSize(); ++i) {
                        final AbsoluteDate t = date.shiftedBy((i - cache.getNeighborsSize() / 2) * step);
                        fillArray(rawProvider.onDate(t), cnmsnm);
                        generated.add(new TimeStampedSphericalHarmonics(t, cnmsnm));
                    }

                } else {

                    // some coefficients have already been generated
                    // add the missing ones up to specified date

                    AbsoluteDate t = existing.getDate();
                    if (date.compareTo(t) > 0) {
                        // forward generation
                        do {
                            t = t.shiftedBy(step);
                            fillArray(rawProvider.onDate(t), cnmsnm);
                            generated.add(new TimeStampedSphericalHarmonics(t, cnmsnm));
                        } while (t.compareTo(date) <= 0);
                    } else {
                        // backward generation
                        do {
                            t = t.shiftedBy(-step);
                            fillArray(rawProvider.onDate(t), cnmsnm);
                            generated.add(new TimeStampedSphericalHarmonics(t, cnmsnm));
                        } while (t.compareTo(date) >= 0);
                        // ensure forward chronological order
                        Collections.reverse(generated);
                    }

                }

                // return the generated sample
                return generated;

            } catch (OrekitException oe) {
                throw new TimeStampedCacheException(oe);
            }
        }

        /** Fill coefficients array for one entry.
         * @param raw the un-interpolated spherical harmonics
         * @param cnmsnm arrays to fill in
         * @exception OrekitException if coefficients cannot be computed at specified date
         */
        private void fillArray(final NormalizedSphericalHarmonics raw,
                               final double[] cnmsnm)
            throws OrekitException {
            int index = 0;
            for (int n = 0; n <= rawProvider.getMaxDegree(); ++n) {
                for (int m = 0; m <= n; ++m) {
                    cnmsnm[index++] = raw.getNormalizedCnm(n, m);
                }
            }
            for (int n = 0; n <= rawProvider.getMaxDegree(); ++n) {
                for (int m = 0; m <= n; ++m) {
                    cnmsnm[index++] = raw.getNormalizedSnm(n, m);
                }
            }
        }

    }

    /**
     * Internal class for time-stamped spherical harmonics. Instances are created using
     * {@link #interpolate(AbsoluteDate, Collection)}
     */
    private static class TimeStampedSphericalHarmonics
            implements TimeStamped, Serializable, NormalizedSphericalHarmonics {

        /** Serializable UID. */
        private static final long serialVersionUID = 20131029l;

        /** Current date. */
        private final AbsoluteDate date;

        /** number of C or S coefficients. */
        private final int size;

        /** Flattened array for C<sub>n,m</sub> and S<sub>n,m</sub> coefficients. */
        private final double[] cnmsnm;

        /** Simple constructor.
         * @param date current date
         * @param cnmsnm flattened array for C<sub>n,m</sub> and S<sub>n,m</sub>
         *               coefficients. It is copied.
         */
        private TimeStampedSphericalHarmonics(final AbsoluteDate date,
                                              final double[] cnmsnm) {
            this.date   = date;
            this.cnmsnm = cnmsnm.clone();
            this.size   = cnmsnm.length / 2;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /** {@inheritDoc} */
        @Override
        public double getNormalizedCnm(final int n, final int m) throws OrekitException {
            return cnmsnm[(n * (n + 1)) / 2 + m];
        }

        /** {@inheritDoc} */
        @Override
        public double getNormalizedSnm(final int n, final int m) throws OrekitException {
            return cnmsnm[(n * (n + 1)) / 2 + m + size];
        }

        /** Interpolate spherical harmonics.
         * <p>
         * The interpolated instance is created by polynomial Hermite interpolation.
         * </p>
         * @param date interpolation date
         * @param sample sample points on which interpolation should be done
         * @return a new time-stamped spherical harmonics, interpolated at specified date
         */
        public static TimeStampedSphericalHarmonics interpolate(final AbsoluteDate date,
                                                                final Collection<TimeStampedSphericalHarmonics> sample) {

            // set up an interpolator taking derivatives into account
            final HermiteInterpolator interpolator = new HermiteInterpolator();

            // add sample points
            for (final TimeStampedSphericalHarmonics tssh : sample) {
                interpolator.addSamplePoint(tssh.date.durationFrom(date), tssh.cnmsnm);
            }

            // build a new interpolated instance
            return new TimeStampedSphericalHarmonics(date, interpolator.value(0.0));

        }

    }

}
