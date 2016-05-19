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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.Ellipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class handles body center pointing attitude provider.

 * <p>
 * This class represents the attitude provider where the satellite z axis is
 * pointing to the body frame center.</p>
 * <p>
 * The object <code>BodyCenterPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class BodyCenterPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150529L;

    /** Body ellipsoid.  */
    private final Ellipsoid ellipsoid;

    /** Creates new instance.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param shape Body shape
     * @exception OrekitException if the frame specified is not a pseudo-inertial frame
     * @since 7.1
     */
    public BodyCenterPointing(final Frame inertialFrame, final Ellipsoid shape)
        throws OrekitException {
        super(inertialFrame, shape.getFrame());
        this.ellipsoid = shape;
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                   final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // spacecraft coordinates in body frame
        final TimeStampedPVCoordinates scInBodyFrame = pvProv.getPVCoordinates(date, getBodyFrame());

        // central projection to ground (NOT the classical nadir point)
        final double u     = scInBodyFrame.getPosition().getX() / ellipsoid.getA();
        final double v     = scInBodyFrame.getPosition().getY() / ellipsoid.getB();
        final double w     = scInBodyFrame.getPosition().getZ() / ellipsoid.getC();
        final double d2    = u * u + v * v + w * w;
        final double d     = FastMath.sqrt(d2);
        final double ratio = 1.0 / d;
        final Vector3D projectedP = new Vector3D(ratio, scInBodyFrame.getPosition());

        // velocity
        final double uDot     = scInBodyFrame.getVelocity().getX() / ellipsoid.getA();
        final double vDot     = scInBodyFrame.getVelocity().getY() / ellipsoid.getB();
        final double wDot     = scInBodyFrame.getVelocity().getZ() / ellipsoid.getC();
        final double dDot     = MathArrays.linearCombination(u, uDot, v, vDot, w, wDot) / d;
        final double ratioDot = -dDot / d2;
        final Vector3D projectedV = new Vector3D(ratio,    scInBodyFrame.getVelocity(),
                                                 ratioDot, scInBodyFrame.getPosition());

        // acceleration
        final double uDotDot      = scInBodyFrame.getAcceleration().getX() / ellipsoid.getA();
        final double vDotDot      = scInBodyFrame.getAcceleration().getY() / ellipsoid.getB();
        final double wDotDot      = scInBodyFrame.getAcceleration().getZ() / ellipsoid.getC();
        final double dDotDot      = (MathArrays.linearCombination(u, uDotDot, v, vDotDot, w, wDotDot) +
                                     uDot * uDot + vDot * vDot + wDot * wDot - dDot * dDot) / d;
        final double ratioDotDot  = (2 * dDot * dDot - d * dDotDot) / (d * d2);
        final Vector3D projectedA = new Vector3D(ratio,        scInBodyFrame.getAcceleration(),
                                                 2 * ratioDot, scInBodyFrame.getVelocity(),
                                                 ratioDotDot,  scInBodyFrame.getPosition());

        final TimeStampedPVCoordinates projected =
                new TimeStampedPVCoordinates(date, projectedP, projectedV, projectedA);
        return getBodyFrame().getTransformTo(frame, date).transformPVCoordinates(projected);

    }

}
