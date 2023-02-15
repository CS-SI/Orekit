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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.CalculusFieldElement;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;

/** Enumerate for ITRF versions.
 * @see EOPEntry
 * @see HelmertTransformation
 * @author Luc Maisonobe
 * @since 9.2
 */
public enum ITRFVersion {

    /** Constant for ITRF 2020. */
    ITRF_2020(2020),

    /** Constant for ITRF 2014. */
    ITRF_2014(2014),

    /** Constant for ITRF 2008. */
    ITRF_2008(2008),

    /** Constant for ITRF 2005. */
    ITRF_2005(2005),

    /** Constant for ITRF 2000. */
    ITRF_2000(2000),

    /** Constant for ITRF 1997. */
    ITRF_1997(1997),

    /** Constant for ITRF 1996. */
    ITRF_1996(1996),

    /** Constant for ITRF 1994. */
    ITRF_1994(1994),

    /** Constant for ITRF 1993. */
    ITRF_1993(1993),

    /** Constant for ITRF 1992. */
    ITRF_1992(1992),

    /** Constant for ITRF 1991. */
    ITRF_1991(1991),

    /** Constant for ITRF 1990. */
    ITRF_1990(1990),

    /** Constant for ITRF 89. */
    ITRF_1989(1989),

    /** Constant for ITRF 88. */
    ITRF_1988(1988);

    /** Regular expression for ITRF names, using several variations. */
    private static final Pattern PATTERN = Pattern.compile("[Ii][Tt][Rr][Ff][-_ ]?([0-9]{2,4})");

    /** Reference year of the frame version. */
    private final int year;

    /** Name. */
    private final String name;

    /** Simple constructor.
     * @param year reference year of the frame version
     */
    ITRFVersion(final int year) {
        this.year = year;
        this.name = "ITRF-" + year;
    }

    /** Get the reference year of the frame version.
     * @return reference year of the frame version
     */
    public int getYear() {
        return year;
    }

    /** Get the name the frame version.
     * @return name of the frame version
     */
    public String getName() {
        return name;
    }

    /** Find an ITRF version from its reference year.
     * @param year reference year of the frame version
     * @return ITRF version for specified year
     */
    public static ITRFVersion getITRFVersion(final int year) {

        final int fixedYear = (year > 87 && year < 100) ? (year + 1900) : year;

        // loop over all predefined frames versions
        for (final ITRFVersion version : values()) {
            if (version.getYear() == fixedYear) {
                return version;
            }
        }

        // we don't have the required frame
        throw new OrekitException(OrekitMessages.NO_SUCH_ITRF_FRAME, year);

    }

    /** Find an ITRF version from its name.
     * @param name name of the frame version (case is ignored)
     * @return ITRF version
     */
    public static ITRFVersion getITRFVersion(final String name) {

        // extract year from name
        final Matcher matcher = PATTERN.matcher(name);
        if (matcher.matches()) {
            try {
                return getITRFVersion(Integer.parseInt(matcher.group(1)));
            } catch (OrekitException oe) {
                throw new OrekitException(OrekitMessages.NO_SUCH_ITRF_FRAME, name);
            }
        }

        // we don't have the required frame
        throw new OrekitException(OrekitMessages.NO_SUCH_ITRF_FRAME, name);

    }

    /** Get last supported ITRF version.
     * @return last supported ITRF version
     * @since 11.2
     */
    public static ITRFVersion getLast() {
        ITRFVersion last = ITRFVersion.ITRF_1988;
        for (final ITRFVersion iv : ITRFVersion.values()) {
            if (iv.getYear() > last.getYear()) {
                last = iv;
            }
        }
        return last;
    }

    /** Find a converter between specified ITRF frames.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param origin origin ITRF
     * @param destination destination ITRF
     * @return transform from {@code origin} to {@code destination}
     * @see #getConverter(ITRFVersion, ITRFVersion, TimeScale)
     */
    @DefaultDataContext
    public static Converter getConverter(final ITRFVersion origin, final ITRFVersion destination) {
        return getConverter(origin, destination,
                DataContext.getDefault().getTimeScales().getTT());
    }

    /** Find a converter between specified ITRF frames.
     * @param origin origin ITRF
     * @param destination destination ITRF
     * @param tt TT time scale.
     * @return transform from {@code origin} to {@code destination}
     * @since 10.1
     */
    public static Converter getConverter(final ITRFVersion origin,
                                         final ITRFVersion destination,
                                         final TimeScale tt) {

        TransformProvider provider = null;

        // special case for no transform
        if (origin == destination) {
            provider = TransformProviderUtils.IDENTITY_PROVIDER;
        }

        if (provider == null) {
            // try to find a direct provider
            provider = getDirectTransformProvider(origin, destination, tt);
        }

        if (provider == null) {
            // no direct provider found, use last supported ITRF as a pivot frame
            final ITRFVersion last = getLast();
            provider = TransformProviderUtils.getCombinedProvider(getDirectTransformProvider(origin, last, tt),
                                                                  getDirectTransformProvider(last, destination, tt));
        }

        // build the converter, to keep the origin and destination information
        return new Converter(origin, destination, provider);

    }

    /** Find a direct transform provider between specified ITRF frames.
     * @param origin origin ITRF
     * @param destination destination ITRF
     * @param tt TT time scale.
     * @return transform from {@code origin} to {@code destination}, or null if no direct transform is found
     */
    private static TransformProvider getDirectTransformProvider(
            final ITRFVersion origin,
            final ITRFVersion destination,
            final TimeScale tt) {

        // loop over all predefined transforms
        for (final HelmertTransformation.Predefined predefined : HelmertTransformation.Predefined.values()) {
            if (predefined.getOrigin() == origin && predefined.getDestination() == destination) {
                // we have an Helmert transformation in the specified direction
                return predefined.getTransformation(tt);
            } else if (predefined.getOrigin() == destination && predefined.getDestination() == origin) {
                // we have an Helmert transformation in the opposite direction
                return TransformProviderUtils.getReversedProvider(predefined.getTransformation(tt));
            }
        }

        // we don't have the required transform
        return null;

    }

    /** Specialized transform provider between ITRF frames. */
    public static class Converter implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 20180330L;

        /** Origin ITRF. */
        private final ITRFVersion origin;

        /** Destination ITRF. */
        private final ITRFVersion destination;

        /** Underlying provider. */
        private final TransformProvider provider;

        /** Simple constructor.
         * @param origin origin ITRF
         * @param destination destination ITRF
         * @param provider underlying provider
         */
        Converter(final ITRFVersion origin, final ITRFVersion destination, final TransformProvider provider) {
            this.origin      = origin;
            this.destination = destination;
            this.provider    = provider;
        }

        /** Get the origin ITRF.
         * @return origin ITRF
         */
        public ITRFVersion getOrigin() {
            return origin;
        }

        /** Get the destination ITRF.
         * @return destination ITRF
         */
        public ITRFVersion getDestination() {
            return destination;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) {
            return provider.getTransform(date);
        }

        @Override
        public StaticTransform getStaticTransform(final AbsoluteDate date) {
            return provider.getStaticTransform(date);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
            return provider.getTransform(date);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
            return provider.getStaticTransform(date);
        }

    }

}
