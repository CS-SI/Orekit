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
package org.orekit.frames;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import org.hipparchus.CalculusFieldElement;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;


/** Provider for a specific version of International Terrestrial Reference Frame.
 * <p>
 * This class ensure the ITRF it defines is a specific version, regardless of
 * the version of the underlying {@link EOPEntry Earth Orientation Parameters}.
 * </p>
 * @author Luc Maisonobe
 * @since 9.2
 */
class VersionedITRFProvider implements EOPBasedTransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20180403L;

    /** ITRF version this provider should generate. */
    private final ITRFVersion version;

    /** Raw ITRF provider. */
    private final ITRFProvider rawProvider;

    /** Converter between different ITRF versions. */
    private final AtomicReference<ITRFVersion.Converter> converter;

    /** TT time scale. */
    private final TimeScale tt;

    /** Simple constructor.
     * @param version ITRF version this provider should generate
     * @param rawProvider raw ITRF provider
     * @param tt TT time scale.
     */
    VersionedITRFProvider(final ITRFVersion version,
                          final ITRFProvider rawProvider,
                          final TimeScale tt) {
        this.version     = version;
        this.rawProvider = rawProvider;
        this.converter   = new AtomicReference<ITRFVersion.Converter>();
        this.tt = tt;
    }

    /** Get the ITRF version.
     * @return ITRF version
     */
    public ITRFVersion getITRFVersion() {
        return version;
    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return rawProvider.getEOPHistory();
    }

    /** {@inheritDoc} */
    @Override
    public VersionedITRFProvider getNonInterpolatingProvider() {
        return new VersionedITRFProvider(version, rawProvider.getNonInterpolatingProvider(), tt);
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {

        // get the transform from the current EOP
        final Transform rawTransform = rawProvider.getTransform(date);

        // add the conversion layer
        final ITRFVersion.Converter converterForDate = getConverter(date);
        if (converterForDate == null) {
            return rawTransform;
        } else {
            return new Transform(date, rawTransform, converterForDate.getTransform(date));
        }

    }

    /** {@inheritDoc} */
    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {

        // get the transform from the current EOP
        final StaticTransform rawTransform = rawProvider.getStaticTransform(date);

        // add the conversion layer
        final ITRFVersion.Converter converterForDate = getConverter(date);
        if (converterForDate == null) {
            return rawTransform;
        } else {
            return StaticTransform.compose(
                    date,
                    rawTransform,
                    converterForDate.getStaticTransform(date));
        }

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        // get the transform from the current EOP
        final FieldTransform<T> rawTransform = rawProvider.getTransform(date);

        // add the conversion layer
        final ITRFVersion.Converter converterForDate = getConverter(date.toAbsoluteDate());
        if (converterForDate == null) {
            return rawTransform;
        } else {
            return new FieldTransform<>(date, rawTransform, converterForDate.getTransform(date));
        }

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {

        // get the transform from the current EOP
        final FieldStaticTransform<T> rawTransform = rawProvider.getStaticTransform(date);

        // add the conversion layer
        final ITRFVersion.Converter converterForDate = getConverter(date.toAbsoluteDate());
        if (converterForDate == null) {
            return rawTransform;
        } else {
            return FieldStaticTransform.compose(
                    date,
                    rawTransform,
                    converterForDate.getStaticTransform(date));
        }

    }

    /** Get a converter for the date.
     * @param date date to check
     * @return converter that should be applied for this date, or null
     * if no converter is needed
     */
    private ITRFVersion.Converter getConverter(final AbsoluteDate date) {

        // check if the current EOP already provides the version we want
        final ITRFVersion rawVersion = getEOPHistory().getITRFVersion(date);
        if (rawVersion == version) {
            // we already have what we need
            return null;
        }

        final ITRFVersion.Converter existing = converter.get();
        if (existing != null && existing.getOrigin() == rawVersion) {
            // the current converter can handle this date
            return existing;
        }

        // we need to create a new converter from raw version to desired version
        final ITRFVersion.Converter newConverter =
                ITRFVersion.getConverter(rawVersion, version, tt);
        converter.compareAndSet(null, newConverter);
        return newConverter;

    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    @DefaultDataContext
    private Object writeReplace() {
        return new DataTransferObject(version, rawProvider);
    }

    /** Internal class used only for serialization. */
    @DefaultDataContext
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20180403L;

        /** ITRF version this provider should generate. */
        private final ITRFVersion version;

        /** Raw ITRF provider. */
        private final ITRFProvider rawProvider;

        /** Simple constructor.
         * @param version ITRF version this provider should generate
         * @param rawProvider raw ITRF provider
         */
        DataTransferObject(final ITRFVersion version, final ITRFProvider rawProvider) {
            this.version     = version;
            this.rawProvider = rawProvider;
        }

        /** Replace the deserialized data transfer object with a {@link VersionedITRFProvider}.
         * @return replacement {@link VersionedITRFProvider}
         */
        private Object readResolve() {
            return new VersionedITRFProvider(version, rawProvider,
                    DataContext.getDefault().getTimeScales().getTT());
        }

    }

}
