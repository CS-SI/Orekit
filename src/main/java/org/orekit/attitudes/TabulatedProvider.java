/* Copyright 2002-2013 CS Systèmes d'Information
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.PVCoordinatesProvider;


/**
 * This class handles an attitude provider interpolating from a predefined table.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @since 6.1
 */
public class TabulatedProvider implements AttitudeProvider {


    /** Serializable UID. */
    private static final long serialVersionUID = 20131128L;

    /** Cached attitude table. */
    private final ImmutableTimeStampedCache<Attitude> table;

    /** Indivator for rate use. */
    private final boolean useRotationRate;

    /** Creates new instance.
     * @param table tabulated attitudes
     * @param n number of attitude to use for interpolation
     * @param useRotationRate if true, rotation rate from the tables are used in
     * the interpolation, ortherwise rates present in the table are ignored
     * and rate is reconstructed from the rotation angles only
     */
    public TabulatedProvider(final List<Attitude> table, final int n, final boolean useRotationRate) {
        this.table           = new ImmutableTimeStampedCache<Attitude>(n, table);
        this.useRotationRate = useRotationRate;
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // get attitudes sample on which interpolation will be performed
        final List<Attitude> sample = table.getNeighbors(date);

        // interpolate
        final List<Pair<AbsoluteDate, AngularCoordinates>> datedAC =
                new ArrayList<Pair<AbsoluteDate, AngularCoordinates>>(sample.size());
        for (final Attitude attitude : sample) {
            datedAC.add(new Pair<AbsoluteDate, AngularCoordinates>(attitude.getDate(), attitude.getOrientation()));
        }
        final AngularCoordinates interpolated = AngularCoordinates.interpolate(date, useRotationRate, datedAC);

        // build the attitude
        return new Attitude(date, sample.get(0).getReferenceFrame(), interpolated);

    }

}
