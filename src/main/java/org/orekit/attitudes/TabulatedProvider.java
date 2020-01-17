/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.RealFieldElement;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;


/**
 * This class handles an attitude provider interpolating from a predefined table.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @see TabulatedLofOffset
 * @since 6.1
 */
public class TabulatedProvider implements AttitudeProvider {

    /** Reference frame for tabulated attitudes. */
    private final Frame referenceFrame;

    /** Cached attitude table. */
    private final transient ImmutableTimeStampedCache<TimeStampedAngularCoordinates> table;

    /** Filter for derivatives from the sample to use in interpolation. */
    private final AngularDerivativesFilter filter;

    /** Creates new instance.
     * @param referenceFrame reference frame for tabulated attitudes
     * @param table tabulated attitudes
     * @param n number of attitude to use for interpolation
     * @param filter filter for derivatives from the sample to use in interpolation
     */
    public TabulatedProvider(final Frame referenceFrame, final List<TimeStampedAngularCoordinates> table,
                             final int n, final AngularDerivativesFilter filter) {
        this.referenceFrame  = referenceFrame;
        this.table           = new ImmutableTimeStampedCache<TimeStampedAngularCoordinates>(n, table);
        this.filter          = filter;
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {

        // get attitudes sample on which interpolation will be performed
        final List<TimeStampedAngularCoordinates> sample = table.getNeighbors(date).collect(Collectors.toList());

        // interpolate
        final TimeStampedAngularCoordinates interpolated =
                TimeStampedAngularCoordinates.interpolate(date, filter, sample);

        // build the attitude
        return new Attitude(referenceFrame, interpolated);

    }

    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                        final FieldAbsoluteDate<T> date,
                                                                        final Frame frame) {

        // get attitudes sample on which interpolation will be performed
        final List<TimeStampedFieldAngularCoordinates<T>> sample =
                        table.
                        getNeighbors(date.toAbsoluteDate()).
                        map(ac -> new TimeStampedFieldAngularCoordinates<>(date.getField(), ac)).
                        collect(Collectors.toList());

        // interpolate
        final TimeStampedFieldAngularCoordinates<T> interpolated =
                TimeStampedFieldAngularCoordinates.interpolate(date, filter, sample);

        // build the attitude
        return new FieldAttitude<>(referenceFrame, interpolated);

    }

}
