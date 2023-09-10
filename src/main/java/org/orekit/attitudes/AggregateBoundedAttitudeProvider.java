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
package org.orekit.attitudes;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * A {@link BoundedAttitudeProvider} that covers a larger time span from several constituent
 * attitude providers that cover shorter time spans.
 *
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class AggregateBoundedAttitudeProvider implements BoundedAttitudeProvider {

    /** Constituent attitude provider. */
    private final NavigableMap<AbsoluteDate, BoundedAttitudeProvider> providers;

    /**
     * Constructor.
     * @param providers attitude providers that provide the backing data for this instance.
     *                  There must be at least one attitude provider in the collection.
     *                  If there are gaps between the {@link BoundedAttitudeProvider#getMaxDate()}
     *                  of one attitude provider and the {@link BoundedAttitudeProvider#getMinDate()}
     *                  of the next attitude provider an exception may be thrown by any method of
     *                  this class at any time. If there are overlaps between the the {@link
     *                  BoundedAttitudeProvider#getMaxDate()} of one attitude provider and the {@link
     *                  BoundedAttitudeProvider#getMinDate()} of the next attitude provider then the
     *                  attitude provider with the latest {@link BoundedAttitudeProvider#getMinDate()}
     *                  is used.
     */
    public AggregateBoundedAttitudeProvider(final Collection<? extends BoundedAttitudeProvider> providers) {

        // Check if the collection is empty
        if (providers.isEmpty()) {
            throw new OrekitException(OrekitMessages.NOT_ENOUGH_ATTITUDE_PROVIDERS);
        }

        // Initialize map
        this.providers = new TreeMap<>();

        // Loop on providers
        for (final BoundedAttitudeProvider provider : providers) {
            // Fill collection
            this.providers.put(provider.getMinDate(), provider);
        }

    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) {

        // Get the attitude provider for the given date
        final BoundedAttitudeProvider provider = getAttitudeProvider(date);

        // Build attitude
        return provider.getAttitude(pvProv, date, frame);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                        final FieldAbsoluteDate<T> date, final Frame frame) {

        // Get the attitude provider for the given date
        final BoundedAttitudeProvider provider = getAttitudeProvider(date.toAbsoluteDate());

        // Build attitude
        return provider.getAttitude(pvProv, date, frame);

    }

    /** {@inheritDoc} */
    @Override
    public Rotation getAttitudeRotation(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
        return getAttitudeProvider(date).getAttitudeRotation(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                    final FieldAbsoluteDate<T> date,
                                                                                    final Frame frame) {
        return getAttitudeProvider(date.toAbsoluteDate()).getAttitudeRotation(pvProv, date, frame);
    }

    @Override
    public AbsoluteDate getMinDate() {
        return providers.firstEntry().getValue().getMinDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMaxDate() {
        return providers.lastEntry().getValue().getMaxDate();
    }

    /**
     * Get the attitude provider to use for the given date.
     * @param date of query
     * @return attitude provider to use on date.
     */
    private BoundedAttitudeProvider getAttitudeProvider(final AbsoluteDate date) {
        final Entry<AbsoluteDate, BoundedAttitudeProvider> attitudeEntry = providers.floorEntry(date);
        if (attitudeEntry != null) {
            return attitudeEntry.getValue();
        } else {
            // Let the first attitude provider throw the exception
            return providers.firstEntry().getValue();
        }
    }

}
