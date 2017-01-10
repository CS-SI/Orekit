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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

/** Transform provider using thread-safe shifts on transforms sample.
 * <p>
 * The shifts take derivatives into account, up to user specified order.
 * </p>
 * @see GenericTimeStampedCache
 * @see InterpolatingTransformProvider
 * @since 7.1
 * @author Luc Maisonobe
 */
public class ShiftingTransformProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150601L;

    /** First level cache. */
    private final InterpolatingTransformProvider interpolatingProvider;

    /** Cache for sample points. */
    private final transient GenericTimeStampedCache<Transform> cache;

    /** Field caches for sample points. */
    // we use Object as the value of fieldCaches because despite numerous attempts,
    // we could not find a way to use GenericTimeStampedCache<FieldTransform<? extends RealFieldElement<?>>
    // without the compiler complaining
    private final transient Map<Field<? extends RealFieldElement<?>>, Object> fieldCaches;

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
    public ShiftingTransformProvider(final TransformProvider rawProvider,
                                     final CartesianDerivativesFilter cFilter,
                                     final AngularDerivativesFilter aFilter,
                                     final AbsoluteDate earliest, final AbsoluteDate latest,
                                     final int gridPoints, final double step,
                                     final int maxSlots, final double maxSpan, final double newSlotInterval) {
        this(new InterpolatingTransformProvider(rawProvider, cFilter, aFilter,
                                                earliest, latest, gridPoints, step,
                                                maxSlots, maxSpan, newSlotInterval),
             maxSlots, maxSpan, newSlotInterval);
    }

    /** Simple constructor.
     * @param interpolatingProvider first level cache provider
     * @param maxSlots maximum number of independent cached time slots
     * in the {@link GenericTimeStampedCache time-stamped cache}
     * @param maxSpan maximum duration span in seconds of one slot
     * in the {@link GenericTimeStampedCache time-stamped cache}
     * @param newSlotInterval time interval above which a new slot is created
     * in the {@link GenericTimeStampedCache time-stamped cache}
     */
    private ShiftingTransformProvider(final InterpolatingTransformProvider interpolatingProvider,
                                     final int maxSlots, final double maxSpan, final double newSlotInterval) {
        this.interpolatingProvider = interpolatingProvider;
        this.cache = new GenericTimeStampedCache<Transform>(2, maxSlots, maxSpan, newSlotInterval,
                                                            new Generator());
        this.fieldCaches = new HashMap<>();
    }

    /** Get the underlying provider for raw (non-interpolated) transforms.
     * @return provider for raw (non-interpolated) transforms
     */
    public TransformProvider getRawProvider() {
        return interpolatingProvider.getRawProvider();
    }

    /** Get the number of interpolation grid points.
     * @return number of interpolation grid points
     */
    public int getGridPoints() {
        return interpolatingProvider.getGridPoints();
    }

    /** Get the grid points time step.
     * @return grid points time step
     */
    public double getStep() {
        return interpolatingProvider.getStep();
    }

    /** {@inheritDoc} */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {
        try {

            // retrieve a sample from the thread-safe cache
            final Transform closest = cache.getNeighbors(date).reduce((t0, t1) ->
                FastMath.abs(date.durationFrom(t0.getDate())) < FastMath.abs(date.durationFrom(t1.getDate())) ? t0 : t1
            ).get();
            return closest.shiftedBy(date.durationFrom(closest.getDate()));

        } catch (OrekitExceptionWrapper oew) {
            // something went wrong while generating the sample,
            // we just forward the exception up
            throw oew.getException();
        }
    }

    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date)
        throws OrekitException {
        try {

            @SuppressWarnings("unchecked")
            GenericTimeStampedCache<FieldTransform<T>> fieldCache =
                (GenericTimeStampedCache<FieldTransform<T>>) fieldCaches.get(date.getField());
            if (fieldCache == null) {
                fieldCache =
                    new GenericTimeStampedCache<FieldTransform<T>>(cache.getNeighborsSize(),
                                                                   cache.getMaxSlots(),
                                                                   cache.getMaxSpan(),
                                                                   cache.getNewSlotQuantumGap(),
                                                                   new FieldGenerator<>(date));
                fieldCaches.put(date.getField(), fieldCache);
            }

            // retrieve a sample from the thread-safe cache
            final FieldTransform<T> closest = fieldCache.getNeighbors(date.toAbsoluteDate()).reduce((t0, t1) ->
                date.durationFrom(t0.getDate()).abs().getReal() < date.durationFrom(t1.getDate()).abs().getReal() ?
                t0 : t1
            ).get();
            return closest.shiftedBy(date.durationFrom(closest.getDate()));

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
        return new DTO(interpolatingProvider,
                       cache.getMaxSlots(), cache.getMaxSpan(), cache.getNewSlotQuantumGap());
    }

    /** Internal class used only for serialization. */
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20150601L;

        /** Provider for raw (non-interpolated) transforms. */
        private final InterpolatingTransformProvider interpolatingProvider;

        /** Maximum number of independent cached time slots. */
        private final int maxSlots;

        /** Maximum duration span in seconds of one slot. */
        private final double maxSpan;

        /** Time interval above which a new slot is created. */
        private final double newSlotInterval;

        /** Simple constructor.
         * @param interpolatingProvider first level cache provider
         * @param maxSlots maximum number of independent cached time slots
         * in the {@link GenericTimeStampedCache time-stamped cache}
         * @param maxSpan maximum duration span in seconds of one slot
         * in the {@link GenericTimeStampedCache time-stamped cache}
         * @param newSlotInterval time interval above which a new slot is created
         * in the {@link GenericTimeStampedCache time-stamped cache}
         */
        private DTO(final InterpolatingTransformProvider interpolatingProvider,
                    final int maxSlots, final double maxSpan, final double newSlotInterval) {
            this.interpolatingProvider = interpolatingProvider;
            this.maxSlots              = maxSlots;
            this.maxSpan               = maxSpan;
            this.newSlotInterval       = newSlotInterval;
        }

        /** Replace the deserialized data transfer object with a {@link ShiftingTransformProvider}.
         * @return replacement {@link ShiftingTransformProvider}
         */
        private Object readResolve() {
            // build a new provider, with an empty cache
            return new ShiftingTransformProvider(interpolatingProvider,
                                                 maxSlots, maxSpan, newSlotInterval);
        }

    }

    /** Local generator for thread-safe cache. */
    private class Generator implements TimeStampedGenerator<Transform> {

        /** {@inheritDoc} */
        public List<Transform> generate(final Transform existing, final AbsoluteDate date) {

            try {
                final List<Transform> generated = new ArrayList<>();

                if (existing == null) {

                    // no prior existing transforms, just generate a first set
                    for (int i = 0; i < cache.getNeighborsSize(); ++i) {
                        generated.add(interpolatingProvider.getTransform(date.shiftedBy(i * interpolatingProvider.getStep())));
                    }

                } else {

                    // some transforms have already been generated
                    // add the missing ones up to specified date

                    AbsoluteDate t = existing.getDate();
                    if (date.compareTo(t) > 0) {
                        // forward generation
                        do {
                            t = t.shiftedBy(interpolatingProvider.getStep());
                            generated.add(generated.size(), interpolatingProvider.getTransform(t));
                        } while (t.compareTo(date) <= 0);
                    } else {
                        // backward generation
                        do {
                            t = t.shiftedBy(-interpolatingProvider.getStep());
                            generated.add(0, interpolatingProvider.getTransform(t));
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

    /** Local generator for thread-safe cache.
     * @param <T> type of the field elements
     */
    private class FieldGenerator<T extends RealFieldElement<T>> implements TimeStampedGenerator<FieldTransform<T>> {

        /** Reference date. */
        private final FieldAbsoluteDate<T> refDate;

        /** Reference date as an absolute date. */
        private final AbsoluteDate aRefDate;

        /** Simple constructor.
         * @param refDate reference date
         */
        FieldGenerator(final FieldAbsoluteDate<T> refDate) {
            this.refDate  = refDate;
            this.aRefDate = refDate.toAbsoluteDate();
        }

        /** {@inheritDoc} */
        public List<FieldTransform<T>> generate(final FieldTransform<T> existing, final AbsoluteDate date) {

            try {

                final List<FieldTransform<T>> generated = new ArrayList<>();
                final FieldAbsoluteDate<T> fDate = refDate.shiftedBy(date.durationFrom(aRefDate));

                if (existing == null) {

                    // no prior existing transforms, just generate a first set
                    for (int i = 0; i < cache.getNeighborsSize(); ++i) {
                        generated.add(interpolatingProvider.getTransform(fDate.shiftedBy(i * interpolatingProvider.getStep())));
                    }

                } else {

                    // some transforms have already been generated
                    // add the missing ones up to specified date

                    AbsoluteDate at = existing.getDate();
                    FieldAbsoluteDate<T> ft = existing.getFieldDate();
                    if (date.compareTo(at) > 0) {
                        // forward generation
                        do {
                            ft = ft.shiftedBy(interpolatingProvider.getStep());
                            final FieldTransform<T> ftr = interpolatingProvider.getTransform(ft);
                            generated.add(generated.size(), ftr);
                            at = ftr.getDate();
                        } while (at.compareTo(date) <= 0);
                    } else {
                        // backward generation
                        do {
                            ft = ft.shiftedBy(-interpolatingProvider.getStep());
                            final FieldTransform<T> ftr = interpolatingProvider.getTransform(ft);
                            generated.add(0, ftr);
                            at = ftr.getDate();
                        } while (at.compareTo(date) >= 0);
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
