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

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;


/**
 * This class handles a simple attitude provider at constant rate around a fixed axis.
 * <p>This attitude provider is a simple linear extrapolation from an initial
 * orientation, a rotation axis and a rotation rate. All this elements can be
 * specified as a simple {@link Attitude reference attitude}.</p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class FixedRate implements AttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 6874119218379303688L;

    /** Reference attitude.  */
    private final Attitude referenceAttitude;

    /** Creates a new instance.
     * @param referenceAttitude attitude at reference date
     */
    public FixedRate(final Attitude referenceAttitude) {
        this.referenceAttitude = referenceAttitude;
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        final double timeShift = date.durationFrom(referenceAttitude.getDate());
        final Attitude shifted = referenceAttitude.shiftedBy(timeShift);
        return shifted.withReferenceFrame(frame);
    }

    /** Get the reference attitude.
     * @return reference attitude
     */
    public Attitude getReferenceAttitude() {
        return referenceAttitude;
    }

}
