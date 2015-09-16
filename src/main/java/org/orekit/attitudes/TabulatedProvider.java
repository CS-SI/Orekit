/* Copyright 2002-2015 CS Systèmes d'Information
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
import java.util.ArrayList;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;


/**
 * This class handles an attitude provider interpolating from a predefined table.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @since 6.1
 */
public class TabulatedProvider implements AttitudeProvider {


    /** Serializable UID. */
    private static final long serialVersionUID = 20140723L;

    /** Reference frame for tabulated attitudes. */
    private final Frame referenceFrame;

    /** Cached attitude table. */
    private final transient ImmutableTimeStampedCache<TimeStampedAngularCoordinates> table;

    /** Filter for derivatives from the sample to use in interpolation. */
    private final AngularDerivativesFilter filter;

    /** Creates new instance.
     * @param table tabulated attitudes
     * @param n number of attitude to use for interpolation
     * @param useRotationRate if true, rotation rate from the tables are used in
     * the interpolation, otherwise rates present in the table are ignored
     * and rate is reconstructed from the rotation angles only
     * @deprecated as of 7.0, replaced with {@link #TabulatedProvider(Frame, List, int, AngularDerivativesFilter)}
     */
    @Deprecated
    public TabulatedProvider(final List<Attitude> table, final int n, final boolean useRotationRate) {
        this(table.get(0).getReferenceFrame(),
             toTimeStampedAngularCoordinates(table),
             n, useRotationRate ? AngularDerivativesFilter.USE_RR : AngularDerivativesFilter.USE_R);
    }

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
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // get attitudes sample on which interpolation will be performed
        final List<TimeStampedAngularCoordinates> sample = table.getNeighbors(date);

        // interpolate
        final TimeStampedAngularCoordinates interpolated =
                TimeStampedAngularCoordinates.interpolate(date, filter, sample);

        // build the attitude
        return new Attitude(referenceFrame, interpolated);

    }

    /** Convert an attitude list into a time-stamped angular coordinates list.
     * @param attitudes attitudes list
     * @return converted list
     */
    private static List<TimeStampedAngularCoordinates> toTimeStampedAngularCoordinates(final List<Attitude> attitudes) {
        final List<TimeStampedAngularCoordinates> converted =
                new ArrayList<TimeStampedAngularCoordinates>(attitudes.size());
        for (final Attitude attitude : attitudes) {
            converted.add(attitude.getOrientation());
        }
        return converted;
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     * @exception NotSerializableException if the state mapper cannot be serialized (typically for DSST propagator)
     */
    private Object writeReplace() throws NotSerializableException {
        return new DataTransferObject(referenceFrame, table.getAll(), table.getNeighborsSize(), filter);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140723L;

        /** Reference frame for tabulated attitudes. */
        private final Frame referenceFrame;

        /** Cached attitude table. */
        private final List<TimeStampedAngularCoordinates> list;

        /** Number of attitude to use for interpolation. */
        private final int n;

        /** Filter for derivatives from the sample to use in interpolation. */
        private final AngularDerivativesFilter filter;

        /** Simple constructor.
         * @param referenceFrame reference frame for tabulated attitudes
         * @param list tabulated attitudes
         * @param n number of attitude to use for interpolation
         * @param filter filter for derivatives from the sample to use in interpolation
         */
        DataTransferObject(final Frame referenceFrame, final List<TimeStampedAngularCoordinates> list,
                                  final int n, final AngularDerivativesFilter filter) {
            this.referenceFrame  = referenceFrame;
            this.list            = list;
            this.n               = n;
            this.filter          = filter;
        }

        /** Replace the deserialized data transfer object with a {@link TabulatedProvider}.
         * @return replacement {@link TabulatedProvider}
         */
        private Object readResolve() {
            return new TabulatedProvider(referenceFrame, list, n, filter);
        }

    }

}
