/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/**
 * This class handles a simple attitude law at constant rate around a fixed axis.
 * <p>This attitude law is a simple linear extrapolation from an initial
 * orientation, a rotation axis and a rotation rate. All this elements can be
 * specified as a simple {@link Attitude reference attitude}.</p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class FixedRate implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = 2809335511030866147L;

    /** Reference attitude.  */
    private final Attitude referenceAttitude;

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Creates a new instance.
     * @param referenceAttitude attitude at reference date
     * @param referenceDate reference date
     */
    public FixedRate(final Attitude referenceAttitude, final AbsoluteDate referenceDate) {
        this.referenceAttitude = referenceAttitude;
        this.referenceDate     = referenceDate;
    }

    /** {@inheritDoc} */
    public Attitude getState(final AbsoluteDate date,
                             final PVCoordinates pv, final Frame frame) {
        final Vector3D spin = referenceAttitude.getSpin();
        final double angle = date.durationFrom(referenceDate) * spin.getNorm();
        Rotation evolution;
        try {
            evolution = new Rotation(spin, angle);
        } catch (ArithmeticException ae) {
            // the spin is null
            evolution = Rotation.IDENTITY;
        }
        final Rotation r = evolution.applyTo(referenceAttitude.getRotation());
        return new Attitude(referenceAttitude.getReferenceFrame(), r, spin);
    }

    /** Get the reference attitude.
     * @return reference attitude
     */
    public Attitude getReferenceAttitude() {
        return referenceAttitude;
    }

    /** Get the reference date.
     * @return reference date
     */
    public AbsoluteDate getReferenceDate() {
        return referenceDate;
    }

}
