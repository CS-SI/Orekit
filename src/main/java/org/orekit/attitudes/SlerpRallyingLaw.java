/* Copyright 2010-2011 Centre National d'Études Spatiales
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;

/** Class representing a simple rallying attitude law between two bounding laws.
 * <p>
 * Rallying is done using Spherical Linear Interpolation (SLERP)
 * </p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 * @since 5.1
 */
public class SlerpRallyingLaw implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = 2909376386948681570L;

    /** Evolution spin axis. */
    private final Vector3D evolAxis;

    /** Evolution spin rate. */
    private final double evolRate;

    /** Evolution spin. */
    private final Vector3D evolSpin;

    /** Start attitude. */
    private final Attitude attitude1;

    /** Creates a new instance from a start attitude and an end attitude.
     * @param start start attitude
     * @param end end attitude
     * @exception OrekitException if the start and end attitudes are not computed
     * from the same reference frame
     */
    public SlerpRallyingLaw(final Attitude start, final Attitude end) throws OrekitException {

        if (start.getReferenceFrame() != end.getReferenceFrame()) {
            throw OrekitException.createIllegalArgumentException(
                  OrekitMessages.FRAMES_MISMATCH,
                  start.getReferenceFrame().getName(), end.getReferenceFrame().getName());
        }

        attitude1 = start;

        // Get evolution rotation
        final Rotation evolution = start.getRotation().applyTo(end.getRotation().revert());

        // Get evolution axis and angle
        evolAxis =   evolution.getAxis();
        evolRate = -(evolution.getAngle ()) / (end.getDate().durationFrom(start.getDate()));
        evolSpin = new Vector3D(evolRate, evolAxis);

    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final AbsoluteDate date) throws OrekitException {

        // Compute intermediary rotation at required date
        final Rotation intermEvolRot = new Rotation(evolAxis, evolRate * date.durationFrom(attitude1.getDate()));

        return new Attitude(date, attitude1.getReferenceFrame(),
                            intermEvolRot.applyTo(attitude1.getRotation()), evolSpin);
    }

}
