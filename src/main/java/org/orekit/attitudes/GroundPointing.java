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

    /** Serializable UID. */
    private static final long serialVersionUID = -1459257023765594793L;

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

    /** Compute the target ground point at given date in given frame.
     * @param orbit orbit state
     * @param frame frame in which observed ground point should be provided
     * @return observed ground point position/velocity in specified frame
     * @throws OrekitException if some specific error occurs,
     * such as no target reached
     */
    public abstract PVCoordinates getObservedGroundPoint(final Orbit orbit, final Frame frame)
        throws OrekitException;

    /** Compute the system state at given date in given frame.
     * <p>User should check that position/velocity and frame are consistent.</p>
     * @return satellite attitude state at date
     * @throws OrekitException if some specific error occurs
     */
    public Attitude getState(Orbit orbit)
        throws OrekitException {

        final PVCoordinates pv = orbit.getPVCoordinates();
        final Frame frame = orbit.getFrame();

        // Construction of the satellite-target position/velocity vector
        final PVCoordinates pointing = new PVCoordinates(pv, getObservedGroundPoint(orbit, frame));
        final Vector3D pos = pointing.getPosition();
        final Vector3D vel = pointing.getVelocity();

//        double h = 0.1;
//        PVCoordinates pM1h = new PVCoordinates(pv.shiftedBy(-h), getObservedGroundPoint(orbit.shiftedBy(-h), frame));
//        PVCoordinates pP1h = new PVCoordinates(pv.shiftedBy( h), getObservedGroundPoint(orbit.shiftedBy( h), frame));

        // New orekit exception if null position.
        if (pos.equals(Vector3D.ZERO)) {
            throw new OrekitException("satellite collided with target");
        }

        // Attitude rotation in given frame :
        // line of sight -> z satellite axis,
        // satellite velocity -> x satellite axis.
        final Rotation rot = new Rotation(pos, pv.getVelocity(), Vector3D.PLUS_K, Vector3D.PLUS_I);
//        final Rotation rotM1h = new Rotation(pM1h.getPosition(), pv.shiftedBy(    -h).getVelocity(), Vector3D.PLUS_K, Vector3D.PLUS_I);
//        final Rotation rotP1h = new Rotation(pP1h.getPosition(), pv.shiftedBy(     h).getVelocity(), Vector3D.PLUS_K, Vector3D.PLUS_I);
//        Vector3D es1 = Attitude.estimateSpin(rotM1h, rotP1h, 2 * h);

        // Attitude spin
        final Vector3D spin = new Vector3D(1 / pos.getNormSq(), Vector3D.crossProduct(pos, vel));
//        System.out.println(date + "   " +
//                           spin.getX() + " " + spin.getY() + " " + spin.getZ() + " (" + spin.getNorm() + ")   " +
//                           es1.getX() + " " + es1.getY() + " " + es1.getZ() + " (" + es1.getNorm() + ")");

        return new Attitude(frame, rot, rot.applyTo(spin));
    }

}
