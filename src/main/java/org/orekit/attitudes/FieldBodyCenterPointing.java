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

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.bodies.Ellipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

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
public class FieldBodyCenterPointing<T extends RealFieldElement<T>> extends FieldGroundPointing<T> {

    /** Body ellipsoid.  */
    private final Ellipsoid ellipsoid;

    /** Creates new instance.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param shape Body shape
     * @exception OrekitException if the frame specified is not a pseudo-inertial frame
     * @since 7.1
     */
    public FieldBodyCenterPointing(final Frame inertialFrame, final Ellipsoid shape)
        throws OrekitException {
        super(inertialFrame, shape.getFrame());

        this.ellipsoid = shape;
    }

    /** {@inheritDoc} */
    public TimeStampedFieldPVCoordinates<T> getTargetPV(final FieldPVCoordinatesProvider<T> pvProv,
                                                   final FieldAbsoluteDate<T> date, final Frame frame)
        throws OrekitException {

        // spacecraft coordinates in body frame
        final TimeStampedFieldPVCoordinates<T> scInBodyFrame = pvProv.getPVCoordinates(date, getBodyFrame());

        // central projection to ground (NOT the classical nadir point)
        final T u     = scInBodyFrame.getPosition().getX().divide(ellipsoid.getA());
        final T v     = scInBodyFrame.getPosition().getY().divide(ellipsoid.getB());
        final T w     = scInBodyFrame.getPosition().getZ().divide(ellipsoid.getC());
        final T d2    = u.pow(2).add(v.pow(2)).add(w.pow(2));
        final T d     = d2.sqrt();
        final T ratio = d.pow(-1);
        final FieldVector3D<T> projectedP = new FieldVector3D<T>(ratio, scInBodyFrame.getPosition());

        // velocity
        final T uDot     = scInBodyFrame.getVelocity().getX().divide(ellipsoid.getA());
        final T vDot     = scInBodyFrame.getVelocity().getY().divide(ellipsoid.getB());
        final T wDot     = scInBodyFrame.getVelocity().getZ().divide(ellipsoid.getC());
        //we aren't using the linearCombination in the library
        final T dDot     = (u.multiply(uDot).add(v.multiply(vDot)).add(w.multiply(wDot))).divide(d);
        final T ratioDot = dDot.multiply(-1).divide(d2);
        final FieldVector3D<T> projectedV = new FieldVector3D<T>(ratio,    scInBodyFrame.getVelocity(),
                                                          ratioDot, scInBodyFrame.getPosition());

        // acceleration
        final T uDotDot      = scInBodyFrame.getAcceleration().getX().divide(ellipsoid.getA());
        final T vDotDot      = scInBodyFrame.getAcceleration().getY().divide(ellipsoid.getB());
        final T wDotDot      = scInBodyFrame.getAcceleration().getZ().divide(ellipsoid.getC());
        final T dDotDot      = u.multiply(uDotDot).add(v.multiply(vDotDot)).add(w.multiply( wDotDot)
                                         .add(uDot.pow(2).add(vDot.pow(2)).add(wDot.pow(2)).subtract(dDot.pow(2))))
                                         .divide(d);
        final T ratioDotDot  = (dDot.pow(2).multiply(2).subtract(d.multiply(dDotDot))).divide(d.multiply(d2));
        final FieldVector3D<T> projectedA = new FieldVector3D<T>(ratio,        scInBodyFrame.getAcceleration(),
                                                 ratioDot.multiply(2), scInBodyFrame.getVelocity(),
                                                 ratioDotDot,  scInBodyFrame.getPosition());
        final TimeStampedFieldPVCoordinates<T> projected =
                new TimeStampedFieldPVCoordinates<T>(date, projectedP, projectedV, projectedA);
        return getBodyFrame().getTransformTo(frame, date.toAbsoluteDate()).transformPVCoordinates(projected);

    }

}
