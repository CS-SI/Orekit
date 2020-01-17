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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/**
 * This class handles an attitude provider interpolating from a predefined table
 * containing offsets from a Local Orbital Frame.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @see LofOffset
 * @see TabulatedProvider
 * @author Luc Maisonobe
 * @since 7.1
 */
public class TabulatedLofOffset implements AttitudeProvider {

    /** Inertial frame with respect to which orbit should be computed. */
    private final Frame inertialFrame;

    /** Type of Local Orbital Frame. */
    private LOFType type;

    /** Cached attitude table. */
    private final transient ImmutableTimeStampedCache<TimeStampedAngularCoordinates> table;

    /** Filter for derivatives from the sample to use in interpolation. */
    private final AngularDerivativesFilter filter;

    /** Creates new instance.
     * @param inertialFrame inertial frame with respect to which orbit should be computed
     * @param type type of Local Orbital Frame
     * @param table tabulated attitudes
     * @param n number of attitude to use for interpolation
     * @param filter filter for derivatives from the sample to use in interpolation
     */
    public TabulatedLofOffset(final Frame inertialFrame, final LOFType type,
                              final List<TimeStampedAngularCoordinates> table,
                              final int n, final AngularDerivativesFilter filter) {
        if (!inertialFrame.isPseudoInertial()) {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME,
                                      inertialFrame.getName());
        }
        this.inertialFrame = inertialFrame;
        this.type          = type;
        this.table         = new ImmutableTimeStampedCache<TimeStampedAngularCoordinates>(n, table);
        this.filter        = filter;
    }

    /** Get an unmodifiable view of the tabulated attitudes.
     * @return unmodifiable view of the tabulated attitudes
     */
    public List<TimeStampedAngularCoordinates> getTable() {
        return table.getAll();
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {

        // get attitudes sample on which interpolation will be performed
        final List<TimeStampedAngularCoordinates> sample = table.getNeighbors(date).collect(Collectors.toList());

        // interpolate
        final TimeStampedAngularCoordinates interpolated =
                TimeStampedAngularCoordinates.interpolate(date, filter, sample);

        // construction of the local orbital frame, using PV from inertial frame
        final PVCoordinates pv = pvProv.getPVCoordinates(date, inertialFrame);
        final Transform inertialToLof = type.transformFromInertial(date, pv);

        // take into account the specified start frame (which may not be an inertial one)
        final Transform frameToInertial = frame.getTransformTo(inertialFrame, date);
        final Transform frameToLof      = new Transform(date, frameToInertial, inertialToLof);

        // compose with interpolated rotation
        return new Attitude(date, frame,
                            interpolated.addOffset(frameToLof.getAngular()));
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

        // construction of the local orbital frame, using PV from inertial frame
        final FieldPVCoordinates<T> pv = pvProv.getPVCoordinates(date, inertialFrame);
        final FieldTransform<T> inertialToLof = type.transformFromInertial(date, pv);

        // take into account the specified start frame (which may not be an inertial one)
        final FieldTransform<T> frameToInertial = frame.getTransformTo(inertialFrame, date);
        final FieldTransform<T> frameToLof      = new FieldTransform<>(date, frameToInertial, inertialToLof);

        // compose with interpolated rotation
        return new FieldAttitude<>(date, frame,
                                   interpolated.addOffset(frameToLof.getAngular()));
    }

}
