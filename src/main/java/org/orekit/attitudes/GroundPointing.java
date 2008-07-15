/* Copyright 2002-2008 CS Communication & Systèmes
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
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;



/**
 * This class handles ground pointing attitude laws.
 *
 * <p>This class is a basic model for different kind of ground pointing
 * attitude laws, such as : body center pointing, nadir pointing,
 * target pointing, etc...
 * </p>
 * <p>
 * The object <code>GroundPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     AttitudeLaw
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public abstract class GroundPointing implements AttitudeLaw {


    /** Body frame. */
    private final Frame bodyFrame;

    /** Default constructor.
     * Build a new instance with arbitrary default elements.
     * @param bodyFrame the frame that rotates with the body
     */
    protected GroundPointing(final Frame bodyFrame) {
        this.bodyFrame = bodyFrame;
    }

    /** Get the body frame.
     * @return body frame
     */
    public Frame getBodyFrame() {
        return bodyFrame;
    }

    /** Get target point in body frame.
     * @param date date when the target point shall be computed
     * @param pv position/velocity of the point
     * @param frame frame in which the point shall be computed
     * @throws OrekitException if some specific error occurs,
     * such as no target reached
     * @return target in body frame
     */
    protected abstract PVCoordinates getTargetInBodyFrame(AbsoluteDate date,
                                                          PVCoordinates pv,
                                                          Frame frame)
        throws OrekitException;

    /** Compute the target ground point at given date in given frame.
     * @param date date when the point shall be computed
     * @param pv position-velocity of the point
     * @param frame frame in which the point shall be computed
     * @throws OrekitException if some specific error occurs
     * @return observed ground point position/velocity in given frame
     *
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p>
     */
    public PVCoordinates getObservedGroundPoint(final AbsoluteDate date,
                                                final PVCoordinates pv,
                                                final Frame frame)
        throws OrekitException {

        // Get target in body frame
        final PVCoordinates targetInBodyFrame = getTargetInBodyFrame(date, pv, frame);

        // Transform to given frame
        final Transform t = bodyFrame.getTransformTo(frame, date);

        // Target in given frame.
        return t.transformPVCoordinates(targetInBodyFrame);
    }

    /** Compute the system state at given date in given frame.
     * @param date date when system state shall be computed
     * @param pv satellite position/velocity in given frame
     * @param frame the frame in which pv is defined
     * @return satellite attitude state at date
     * @throws OrekitException if some specific error occurs
     *
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p>
     */
    public Attitude getState(final AbsoluteDate date, final PVCoordinates pv, final Frame frame)
        throws OrekitException {

        // Construction of the satellite-target position/velocity vector
        final PVCoordinates pointing =  new PVCoordinates(1, getObservedGroundPoint(date, pv, frame), -1, pv);
        final Vector3D pos = pointing.getPosition();
        final Vector3D vel = pointing.getVelocity();

        // New orekit exception if null position.
        if (pos.equals(Vector3D.ZERO)) {
            throw new OrekitException("satellite smashed on its target", new Object[0]);
        }

        // Attitude rotation in given frame :
        // line of sight -> z satellite axis,
        // satellite velocity -> x satellite axis.
        final Rotation rot = new Rotation(pos, pv.getVelocity(), Vector3D.PLUS_K, Vector3D.PLUS_I);

        // Attitude spin
        final Vector3D spin = new Vector3D(1 / Vector3D.dotProduct(pos, pos),
                                           Vector3D.crossProduct(pos, vel));

        return new Attitude(frame, rot, rot.applyTo(spin));
    }

}
