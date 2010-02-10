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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.utils.PVCoordinates;


/** This class leverages common parts for compensation modes around ground pointing attitudes.
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public abstract class GroundPointingWrapper extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = 262999520075931766L;

    /** Underlying ground pointing attitude law.  */
    private final GroundPointing groundPointingLaw;

    /** Creates a new instance.
     * @param groundPointingLaw ground pointing attitude law without compensation
     */
    public GroundPointingWrapper(final GroundPointing groundPointingLaw) {
        super(groundPointingLaw.getBodyFrame());
        this.groundPointingLaw = groundPointingLaw;
    }

    /** Get the attitude rotation.
     * @return attitude satellite rotation from reference frame.
     */
    public GroundPointing getGroundPointingLaw() {
        return groundPointingLaw;
    }

    /** {@inheritDoc} */
    protected Vector3D getTargetPoint(final Orbit orbit, final Frame frame)
        throws OrekitException {
        return groundPointingLaw.getTargetPoint(orbit, frame);
    }

    /** {@inheritDoc} */
    @Override
    protected PVCoordinates getTargetPV(final Orbit orbit, final Frame frame)
        throws OrekitException {
        return groundPointingLaw.getTargetPV(orbit, frame);
    }

    /** Compute the base system state at given date, without compensation.
     * @param orbit orbit state for which attitude is requested
     * @return satellite base attitude state, i.e without compensation.
     * @throws OrekitException if some specific error occurs
     */
    public Attitude getBaseState(final Orbit orbit)
        throws OrekitException {
        return groundPointingLaw.getAttitude(orbit);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final Orbit orbit)
        throws OrekitException {

        // Get attitude from base attitude law
        final Attitude base = getBaseState(orbit);

        // Get compensation
        final Rotation compensation = getCompensation(orbit, base);

        // Compute compensation rotation rate
        final double h = 0.1;
        final Rotation compensationM1H  = getCompensation(orbit.shiftedBy(-h), base.shiftedBy(-h));
        final Rotation compensationP1H  = getCompensation(orbit.shiftedBy( h), base.shiftedBy( h));
        final Vector3D compensationRate = Attitude.estimateSpin(compensationM1H, compensationP1H, 2 * h);

        // Combination of base attitude, compensation and compensation rate
        return new Attitude(orbit.getDate(), orbit.getFrame(),
                            compensation.applyTo(base.getRotation()),
                            compensationRate.add(compensation.applyTo(base.getSpin())));

    }

    /** Compute the compensation rotation at given date.
     * @param orbit orbit state for which compensation is requested
     * @param base base satellite attitude in given frame.
     * @return compensation rotation at date, i.e rotation between non compensated
     * attitude state and compensated state.
     * @throws OrekitException if some specific error occurs
     */
    public abstract Rotation getCompensation(final Orbit orbit, final Attitude base)
        throws OrekitException;

}
