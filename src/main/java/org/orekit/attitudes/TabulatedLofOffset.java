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
package org.orekit.attitudes;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;

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


    /** Serializable UID. */
    private static final long serialVersionUID = 20151211L;

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
     * @exception OrekitException if inertialFrame is not a pseudo-inertial frame
     */
    public TabulatedLofOffset(final Frame inertialFrame, final LOFType type,
                              final List<TimeStampedAngularCoordinates> table,
                              final int n, final AngularDerivativesFilter filter)
        throws OrekitException {
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
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // get attitudes sample on which interpolation will be performed
        final List<TimeStampedAngularCoordinates> sample = table.getNeighbors(date);

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

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     * @exception NotSerializableException if the state mapper cannot be serialized (typically for DSST propagator)
     */
    private Object writeReplace() throws NotSerializableException {
        return new DataTransferObject(inertialFrame, type, table.getAll(), table.getNeighborsSize(), filter);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20151211L;

        /** Inertial frame with respect to which orbit should be computed. */
        private final Frame inertialFrame;

        /** Type of Local Orbital Frame. */
        private LOFType type;

        /** Cached attitude table. */
        private final List<TimeStampedAngularCoordinates> list;

        /** Number of attitude to use for interpolation. */
        private final int n;

        /** Filter for derivatives from the sample to use in interpolation. */
        private final AngularDerivativesFilter filter;

        /** Simple constructor.
         * @param inertialFrame inertial frame with respect to which orbit should be computed
         * @param type type of Local Orbital Frame
         * @param list tabulated attitudes
         * @param n number of attitude to use for interpolation
         * @param filter filter for derivatives from the sample to use in interpolation
         */
        DataTransferObject(final Frame inertialFrame, final LOFType type,
                           final List<TimeStampedAngularCoordinates> list,
                           final int n, final AngularDerivativesFilter filter) {
            this.inertialFrame = inertialFrame;
            this.type          = type;
            this.list          = list;
            this.n             = n;
            this.filter        = filter;
        }

        /** Replace the deserialized data transfer object with a {@link TabulatedLofOffset}.
         * @return replacement {@link TabulatedLofOffset}
         */
        private Object readResolve() {
            try {
                return new TabulatedLofOffset(inertialFrame, type, list, n, filter);
            } catch (OrekitException oe) {
                // this should never happen
                throw new OrekitInternalError(oe);
            }
        }

    }

}
