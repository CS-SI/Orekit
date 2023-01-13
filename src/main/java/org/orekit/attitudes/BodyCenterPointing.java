/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.Ellipsoid;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
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

    /** Body ellipsoid.  */
    private final Ellipsoid ellipsoid;

    /** Creates new instance.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param shape Body shape
     * @since 7.1
     */
    public BodyCenterPointing(final Frame inertialFrame, final Ellipsoid shape) {
        super(inertialFrame, shape.getFrame());
        this.ellipsoid = shape;
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                final AbsoluteDate date, final Frame frame) {

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

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getTargetPV(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                        final FieldAbsoluteDate<T> date, final Frame frame) {

        // spacecraft coordinates in body frame
        final TimeStampedFieldPVCoordinates<T> scInBodyFrame = pvProv.getPVCoordinates(date, getBodyFrame());

        // central projection to ground (NOT the classical nadir point)
        final T u     = scInBodyFrame.getPosition().getX().divide(ellipsoid.getA());
        final T v     = scInBodyFrame.getPosition().getY().divide(ellipsoid.getB());
        final T w     = scInBodyFrame.getPosition().getZ().divide(ellipsoid.getC());
        final T d2    = u.pow(2).add(v.pow(2)).add(w.pow(2));
        final T d     = d2.sqrt();
        final T ratio = d.reciprocal();
        final FieldVector3D<T> projectedP = new FieldVector3D<>(ratio, scInBodyFrame.getPosition());

        // velocity
        final T uDot     = scInBodyFrame.getVelocity().getX().divide(ellipsoid.getA());
        final T vDot     = scInBodyFrame.getVelocity().getY().divide(ellipsoid.getB());
        final T wDot     = scInBodyFrame.getVelocity().getZ().divide(ellipsoid.getC());
        //we aren't using the linearCombination in the library
        final T dDot     = (u.multiply(uDot).add(v.multiply(vDot)).add(w.multiply(wDot))).divide(d);
        final T ratioDot = dDot.multiply(-1).divide(d2);
        final FieldVector3D<T> projectedV = new FieldVector3D<>(ratio,    scInBodyFrame.getVelocity(),
                                                                ratioDot, scInBodyFrame.getPosition());

        // acceleration
        final T uDotDot      = scInBodyFrame.getAcceleration().getX().divide(ellipsoid.getA());
        final T vDotDot      = scInBodyFrame.getAcceleration().getY().divide(ellipsoid.getB());
        final T wDotDot      = scInBodyFrame.getAcceleration().getZ().divide(ellipsoid.getC());
        final T dDotDot      = u.multiply(uDotDot).add(v.multiply(vDotDot)).add(w.multiply( wDotDot)
                                         .add(uDot.pow(2).add(vDot.pow(2)).add(wDot.pow(2)).subtract(dDot.pow(2))))
                                         .divide(d);
        final T ratioDotDot  = (dDot.pow(2).multiply(2).subtract(d.multiply(dDotDot))).divide(d.multiply(d2));
        final FieldVector3D<T> projectedA = new FieldVector3D<>(ratio,                scInBodyFrame.getAcceleration(),
                                                                ratioDot.multiply(2), scInBodyFrame.getVelocity(),
                                                                ratioDotDot,          scInBodyFrame.getPosition());
        final TimeStampedFieldPVCoordinates<T> projected =
                new TimeStampedFieldPVCoordinates<>(date, projectedP, projectedV, projectedA);
        return getBodyFrame().getTransformTo(frame, date.toAbsoluteDate()).transformPVCoordinates(projected);

    }

}
