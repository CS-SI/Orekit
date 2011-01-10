/* Copyright 2010 Centre National d'Études Spatiales
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
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

/** Class representing a pointing law with two references.
 * <p>
 * The first reference is used as an perfect pointing and the second reference serves
 * for phasing around the axis defined by the first reference.
 * </p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 * @since 5.1
 */
public class TwoDirectionsPointingLaw implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = 3534621602462551992L;

    /** Reference frame in which targets vectors are given. */
    private final Frame referenceFrame;

    /** Spacecraft position-velocity provider. */
    private final PVCoordinatesProvider pvProvider;

    /** Pointing target direction, in inertial frame. */
    private final Vector3D pointingTarget;

    /** Flag to define if pointing target is a direction or a fixed point. */
    private final boolean pointingParallax;

    /** Pointing direction, in spacecraft frame. */
    private final Vector3D pointingAxis;

    /** Pointing target direction, in inertial frame. */
    private final Vector3D phasingTarget;

    /** Flag to define if phasing target is a direction or a fixed point. */
    private final boolean phasingParallax;

    /** Phasing target reference direction, in inertial frame. */
    private final Vector3D phasingAxis1;

    /** Third direction to complete direct frame (pointingTarget, phasingAxis1, phasingAxis2). */
    private final Vector3D phasingAxis2;

    /** Phasing angle. */
    private final  double phasingAngle;


    /** Build a new instance.
     * <p>The first direction is fixed by pointing target and spacecraft pointing axis,
     * which is placed exactly in pointing target direction. To complete the attitude computation,
     * the third degree of freedom is fixed by phasing target and its position with regard to
     * meridian plane defined by (pointingAxis, phasing axis 1).
     * The phasing angle is the angle between phasing axis 1 and the projection of phasing target
     * direction on (phasingAxis1, phasingAxis2) plane. </p>
     * <p>Pointing target and phasing target can both either be a direction vector, independent from frame
     * (like an angular momentum for example), to which parallax correction should not be applied,
     * or a fixed point vector, to which parallax correction should be applied.</p>
     * @param referenceFrame reference frame in which pointing target and phasing target are given
     * @param pvProvider spacecraft position-velocity provider
     * @param pointingTarget pointing target
     * @param pointingParallax boolean to define if pointing target is a direction or a fixed point
     * @param pointingAxis pointing axis, in spacecraft frame
     * @param phasingTarget target phasing target
     * @param phasingParallax boolean to define if phasing target is a direction or a fixed point
     * @param phasingAxis1 axis normal to pointing axis, in spacecraft frame, from which phasing angle is measured
     * @param phasingAngle phase angle, measured from meridian plane (pointingAxis, phasingAxis), in the plane normal to pointingTarget
     */
    protected TwoDirectionsPointingLaw(final Frame referenceFrame, final PVCoordinatesProvider pvProvider,
                                       final Vector3D pointingTarget, final boolean pointingParallax,
                                       final Vector3D pointingAxis,
                                       final Vector3D phasingTarget, final boolean phasingParallax,
                                       final Vector3D phasingAxis1, final double phasingAngle) {

        this.referenceFrame = referenceFrame;
        this.pvProvider = pvProvider;
        this.pointingTarget = pointingTarget;
        this.pointingParallax = pointingParallax;
        this.pointingAxis = pointingAxis;
        this.phasingTarget = phasingTarget;
        this.phasingAxis1 = phasingAxis1;
        this.phasingAxis2 = Vector3D.crossProduct(pointingAxis, phasingAxis1);
        this.phasingParallax = phasingParallax;
        this.phasingAngle = phasingAngle;
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final AbsoluteDate date) throws OrekitException {

        Vector3D pointingTargetDirection = pointingTarget;
        Vector3D phasingTargetDirection = phasingTarget;

        // If parallax correction are needed
        if (pointingParallax) {
            pointingTargetDirection = pointingTargetDirection.subtract(pvProvider.getPVCoordinates(date, referenceFrame).getPosition());
        }
        if (phasingParallax) {
            phasingTargetDirection = phasingTargetDirection.subtract(pvProvider.getPVCoordinates(date, referenceFrame).getPosition());
        }

        // Phasing target is in meridian plane at defined angle (target phase) from phasing axis
        final Vector3D phasingDirSat = new Vector3D(Math.cos(phasingAngle), phasingAxis1, Math.sin(phasingAngle), phasingAxis2);

        // Construction of the attitude rotation :
        // 1/ Pointing axis in target direction from satellite :
        // 2/ Phasing target is in meridian plane at defined angle (target phase) from off axis 1
        final Rotation attRot = new Rotation(pointingTargetDirection, phasingTargetDirection,
                                             pointingAxis, phasingDirSat);

        // Spin is forced to null for the moment, implementation of spin computation will be added later
        final Vector3D attSpin = new Vector3D(0., 0., 0.);

        return new Attitude(date, referenceFrame, attRot, attSpin);

    }

}
