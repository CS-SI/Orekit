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

package org.orekit.frames;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

/** Transform provider using thread-safe interpolation on transforms sample.
 * <p>
 * The interpolation is a polynomial Hermite interpolation, which
 * can either use or ignore the derivatives provided by the raw
 * provider. This means that simple raw providers that do not compute
 * derivatives can be used, the derivatives will be added appropriately
 * by the interpolation process.
 * </p>
 * @see GenericTimeStampedCache
 * @see ShiftingTransformProvider
 * @author Luc Maisonobe
 */
public class InterpolatingTransformProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140723L;

    /** Provider for raw (non-interpolated) transforms. */
    private final TransformProvider rawProvider;

    /** Filter for Cartesian derivatives to use in interpolation. */
    private final CartesianDerivativesFilter cFilter;

    /** Filter for angular derivatives to use in interpolation. */
    private final AngularDerivativesFilter aFilter;

    /** Earliest supported date. */
    private final AbsoluteDate earliest;

    /** Latest supported date. */
    private final AbsoluteDate latest;

    /** Grid points time step. */
    private final double step;

    /** Cache for sample points. */
    private final transient GenericTimeStampedCache<Transform> cache;

    /** Simple constructor.
     * @param rawProvider provider for raw (non-interpolated) transforms
     * @param cFilter filter for derivatives from the sample to use in interpolation
     * @param aFilter filter for derivatives from the sample to use in interpolation
     * @param earliest earliest supported date
     * @param latest latest supported date
     * @param gridPoints number of interpolation grid points
     * @param step grid points time step
     * @param maxSlots maximum number of independent cached time slots
     * in the {@link GenericTimeStampedCache time-stamped cache}
     * @param maxSpan maximum duration span in seconds of one slot
     * in the {@link GenericTimeStampedCache time-stamped cache}
     * @param newSlotInterval time interval above which a new slot is created
     * in the {@link GenericTimeStampedCache time-stamped cache}
     */
    public InterpolatingTransformProvider(final TransformProvider rawProvider,
                                          final CartesianDerivativesFilter cFilter,
                                          final AngularDerivativesFilter aFilter,
                                          final AbsoluteDate earliest, final AbsoluteDate latest,
                                          final int gridPoints, final double step,
                                          final int maxSlots, final double maxSpan, final double newSlotInterval) {
        this.rawProvider = rawProvider;
        this.cFilter     = cFilter;
        this.aFilter     = aFilter;
        this.earliest    = earliest;
        this.latest      = latest;
        this.step        = step;
        this.cache       = new GenericTimeStampedCache<Transform>(gridPoints, maxSlots, maxSpan, newSlotInterval,
                                                                  new Generator(), Transform.class);
    }

    /** Get the underlying provider for raw (non-interpolated) transforms.
     * @return provider for raw (non-interpolated) transforms
     */
    public TransformProvider getRawProvider() {
        return rawProvider;
    }

    /** Get the number of interpolation grid points.
     * @return number of interpolation grid points
     */
    public int getGridPoints() {
        return cache.getNeighborsSize();
    }

    /** Get the grid points time step.
     * @return grid points time step
     */
    public double getStep() {
        return step;
    }

    /** {@inheritDoc} */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {
        try {

            // retrieve a sample from the thread-safe cache
            final List<Transform> sample = cache.getNeighbors(date);

            // interpolate to specified date
            return Transform.interpolate(date, cFilter, aFilter, sample);

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
        return new DTO(rawProvider, cFilter.getMaxOrder(), aFilter.getMaxOrder(),
                       earliest, latest, cache.getNeighborsSize(), step,
                       cache.getMaxSlots(), cache.getMaxSpan(), cache.getNewSlotQuantumGap());
    }

    /** Internal class used only for serialization. */
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140723L;

        /** Provider for raw (non-interpolated) transforms. */
        private final TransformProvider rawProvider;

        /** Cartesian derivatives to use in interpolation. */
        private final int cDerivatives;

        /** Angular derivatives to use in interpolation. */
        private final int aDerivatives;

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

        /** Time interval above which a new slot is created. */
        private final double newSlotInterval;

        /** Simple constructor.
         * @param rawProvider provider for raw (non-interpolated) transforms
         * @param cDerivatives derivation order for Cartesian coordinates
         * @param aDerivatives derivation order for angular coordinates
         * @param earliest earliest supported date
         * @param latest latest supported date
         * @param gridPoints number of interpolation grid points
         * @param step grid points time step
         * @param maxSlots maximum number of independent cached time slots
         * in the {@link GenericTimeStampedCache time-stamped cache}
         * @param maxSpan maximum duration span in seconds of one slot
         * in the {@link GenericTimeStampedCache time-stamped cache}
         * @param newSlotInterval time interval above which a new slot is created
         * in the {@link GenericTimeStampedCache time-stamped cache}
         */
        private DTO(final TransformProvider rawProvider, final int cDerivatives, final int aDerivatives,
                    final AbsoluteDate earliest, final AbsoluteDate latest,
                    final int gridPoints, final double step,
                    final int maxSlots, final double maxSpan, final double newSlotInterval) {
            this.rawProvider      = rawProvider;
            this.cDerivatives     = cDerivatives;
            this.aDerivatives     = aDerivatives;
            this.earliest         = earliest;
            this.latest           = latest;
            this.gridPoints       = gridPoints;
            this.step             = step;
            this.maxSlots         = maxSlots;
            this.maxSpan          = maxSpan;
            this.newSlotInterval  = newSlotInterval;
        }

        /** Replace the deserialized data transfer object with a {@link InterpolatingTransformProvider}.
         * @return replacement {@link InterpolatingTransformProvider}
         */
        private Object readResolve() {
            // build a new provider, with an empty cache
            return new InterpolatingTransformProvider(rawProvider,
                                                      CartesianDerivativesFilter.getFilter(cDerivatives),
                                                      AngularDerivativesFilter.getFilter(aDerivatives),
                                                      earliest, latest, gridPoints, step,
                                                      maxSlots, maxSpan, newSlotInterval);
        }

    }

    /** Local generator for thread-safe cache. */
    private class Generator implements TimeStampedGenerator<Transform> {

        /** {@inheritDoc} */
        public List<Transform> generate(final Transform existing, final AbsoluteDate date) {

            try {
                final List<Transform> generated = new ArrayList<Transform>();

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
