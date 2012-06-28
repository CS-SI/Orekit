/* Copyright 2002-2012 CS Systèmes d'Information
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

package org.orekit.frames;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

/** Transform provider using thread-safe interpolation on transforms sample.
 * <p>
 * The interpolation is a polynomial Hermite interpolation, which
 * can either use or ignore the derivatives provided by the raw
 * provider. This means that simple raw providers that do not compute
 * derivatives can be used, the derivatives will be added appropriately
 * by the interpolation process.
 * </p>
 * @see TimeStampedCache
 * @author Luc Maisonobe
 */
public class InterpolatingTransformProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -1750070230136582364L;

    /** Provider for raw (non-interpolated) transforms. */
    private final TransformProvider rawProvider;

    /** Flag for use of sample transforms velocities. */
    private final boolean useVelocities;

    /** Flag for use sample points rotation rates. */
    private final boolean useRotationRates;

    /** Earliest supported date. */
    private final AbsoluteDate earliest;

    /** Latest supported date. */
    private final AbsoluteDate latest;

    /** Grid points time step. */
    private final double step;

    /** Cache for sample points. */
    private final TimeStampedCache<Transform> cache;

    /** Simple constructor.
     * @param rawProvider provider for raw (non-interpolated) transforms
     * @param useVelocities if true, use sample transforms velocities,
     * otherwise ignore them and use only positions
     * @param useRotationRates if true, use sample points rotation rates,
     * otherwise ignore them and use only rotations
     * @param earliest earliest supported date
     * @param latest latest supported date
     * @param gridPoints number of interpolation grid points
     * @param step grid points time step
     * @param maxSlots maximum number of independent cached time slots
     * in the {@link TimeStampedCache time-stamped cache}
     * @param maxSpan maximum duration span in seconds of one slot
     * in the {@link TimeStampedCache time-stamped cache}
     */
    public InterpolatingTransformProvider(final TransformProvider rawProvider,
                                          final boolean useVelocities, final boolean useRotationRates,
                                          final AbsoluteDate earliest, final AbsoluteDate latest,
                                          final int gridPoints, final double step,
                                          final int maxSlots, final double maxSpan) {
        this.rawProvider      = rawProvider;
        this.useVelocities    = useVelocities;
        this.useRotationRates = useRotationRates;
        this.earliest         = earliest;
        this.latest           = latest;
        this.step             = step;
        this.cache            = new TimeStampedCache<Transform>(maxSlots, maxSpan, Transform.class,
                                                                new Generator(), gridPoints);
    }

    /** Get the underlying provider for raw (non-interpolated) transforms.
     * @return provider for raw (non-interpolated) transforms
     */
    public TransformProvider getRawProvider() {
        return rawProvider;
    }

    /** {@inheritDoc} */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {
        try {

            // retrieve a sample from the thread-safe cache
            final Transform[] sample = cache.getNeighbors(date);

            // interpolate to specified date
            return Transform.interpolate(date, useVelocities, useRotationRates, Arrays.asList(sample));

        } catch (OrekitExceptionWrapper oew) {
            // something went wrong while generating the sample,
            // we just forward the exception up
            throw oew.getException();
        }
    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the data needed for generation,
     * but does <em>not</em> serializes the cache itself (in fact the cache is
     * not serializable).
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(rawProvider, useVelocities, useRotationRates,
                                      earliest, latest, cache.getNeighborsSize(),
                                      step, cache.getMaxSlots(), cache.getMaxSpan());
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 8127819004703645170L;

        /** Provider for raw (non-interpolated) transforms. */
        private final TransformProvider rawProvider;

        /** Flag for use of sample transforms velocities. */
        private final boolean useVelocities;

        /** Flag for use sample points rotation rates. */
        private final boolean useRotationRates;

        /** Earliest supported date. */
        private final AbsoluteDate earliest;

        /** Latest supported date. */
        private final AbsoluteDate latest;

        /** Number of grid points. */
        private final int gridPoints;

        /** Grid points time step. */
        private final double step;

        /** Maximum number of independent cached time slots. */
        private final int maxSlots;

        /** Maximum duration span in seconds of one slot. */
        private final double maxSpan;

        /** Simple constructor.
         * @param rawProvider provider for raw (non-interpolated) transforms
         * @param useVelocities if true, use sample transforms velocities,
         * otherwise ignore them and use only positions
         * @param useRotationRates if true, use sample points rotation rates,
         * otherwise ignore them and use only rotations
         * @param earliest earliest supported date
         * @param latest latest supported date
         * @param gridPoints number of interpolation grid points
         * @param step grid points time step
         * @param maxSlots maximum number of independent cached time slots
         * in the {@link TimeStampedCache time-stamped cache}
         * @param maxSpan maximum duration span in seconds of one slot
         * in the {@link TimeStampedCache time-stamped cache}
         */
        public DataTransferObject(final TransformProvider rawProvider,
                                  final boolean useVelocities, final boolean useRotationRates,
                                  final AbsoluteDate earliest, final AbsoluteDate latest,
                                  final int gridPoints, final double step,
                                  final int maxSlots, final double maxSpan) {
            this.rawProvider      = rawProvider;
            this.useVelocities    = useVelocities;
            this.useRotationRates = useRotationRates;
            this.earliest         = earliest;
            this.latest           = latest;
            this.gridPoints       = gridPoints;
            this.step             = step;
            this.maxSlots         = maxSlots;
            this.maxSpan          = maxSpan;
        }

        /** Replace the deserialized data transfer object with a {@link InterpolatingTransformProvider}.
         * @return replacement {@link InterpolatingTransformProvider}
         */
        private Object readResolve() {
            // build a new provider, with an empty cache
            return new InterpolatingTransformProvider(rawProvider, useVelocities, useRotationRates,
                                                      earliest, latest,
                                                      gridPoints, step, maxSlots, maxSpan);
        }

    }

    /** Local generator for thread-safe cache. */
    private class Generator implements TimeStampedGenerator<Transform> {

        /** {@inheritDoc} */
        public AbsoluteDate getEarliest() {
            return earliest;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getLatest() {
            return latest;
        }

        /** {@inheritDoc} */
        public List<Transform> generate(final Transform existing, final AbsoluteDate date) {

            try {
                List<Transform> generated = new ArrayList<Transform>();

                if (existing == null) {

                    // no prior existing transforms, just generate a first set
                    for (int i = 0; i < cache.getNeighborsSize(); ++i) {
                        generated.add(rawProvider.getTransform(date.shiftedBy(i * step)));
                    }

                } else {

                    // some transforms have already been generated
                    // add the missing ones up to specified date

                    AbsoluteDate t = existing.getDate();
                    if (date.compareTo(t) > 0) {
                        // forward generation
                        do {
                            t = t.shiftedBy(step);
                            generated.add(generated.size(), rawProvider.getTransform(t));
                        } while (t.compareTo(date) <= 0);
                    } else {
                        // backward generation
                        do {
                            t = t.shiftedBy(-step);
                            generated.add(0, rawProvider.getTransform(t));
                        } while (t.compareTo(date) >= 0);         
                    }
                }

                // return the generated transforms
                return generated;
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }

        }

    }

}
