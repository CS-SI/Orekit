/* Copyright 2002-2011 CS Communication & Systèmes
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
package org.orekit.frames;

import java.io.Serializable;

import org.apache.commons.math.geometry.euclidean.threed.Rotation;
import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.MathUtils;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;


/** Transformation class for geodetic systems.
 *
 * <p>The Helmert transformation is mainly used to convert between various
 * realizations of geodetic frames, for example in the ITRF family.</p>
 *
 * <p>The original Helmert transformation is a 14 parameters transform that
 * includes translation, velocity, rotation, rotation rate and scale factor.
 * The scale factor is useful for coordinates near Earth surface, but it
 * cannot be extended to outer space as it would correspond to a non-unitary
 * transform. Therefore, the scale factor is <em>not</em> used here.<p>
 *
 * <p>Instances of this class are guaranteed to be immutable.</p>
 *
 * @author Luc Maisonobe
 * @since 5.1
 */
public class HelmertTransformation implements Serializable {

    /** serializable UID. */
    private static final long serialVersionUID = -6092152895769611904L;

    /** Global translation. */
    private final Vector3D translation;

    /** First time derivative of the translation. */
    private final Vector3D velocity;

    /** Global rotation vector (applying rotation is done by computing cross product). */
    private final Vector3D rotationVector;

    /** First time derivative of the rotation (norm representing angular rate). */
    private final Vector3D rotationRate;

    /** Reference epoch of the transform. */
    private final AbsoluteDate epoch;

    /** Build a transform from its primitive operations.
     * @param epoch reference epoch of the transform
     * @param t1 translation parameter along X axis (BEWARE, this is in mm)
     * @param t2 translation parameter along Y axis (BEWARE, this is in mm)
     * @param t3 translation parameter along Z axis (BEWARE, this is in mm)
     * @param r1 rotation parameter around X axis (BEWARE, this is in mas)
     * @param r2 rotation parameter around Y axis (BEWARE, this is in mas)
     * @param r3 rotation parameter around Z axis (BEWARE, this is in mas)
     * @param t1Dot rate of translation parameter along X axis (BEWARE, this is in mm/y)
     * @param t2Dot rate of translation parameter along Y axis (BEWARE, this is in mm/y)
     * @param t3Dot rate of translation parameter along Z axis (BEWARE, this is in mm/y)
     * @param r1Dot rate of rotation parameter around X axis (BEWARE, this is in mas/y)
     * @param r2Dot rate of rotation parameter around Y axis (BEWARE, this is in mas/y)
     * @param r3Dot rate of rotation parameter around Z axis (BEWARE, this is in mas/y)
     */
    public HelmertTransformation(final AbsoluteDate epoch,
                                 final double t1, final double t2, final double t3,
                                 final double r1, final double r2, final double r3,
                                 final double t1Dot, final double t2Dot, final double t3Dot,
                                 final double r1Dot, final double r2Dot, final double r3Dot) {

        // conversion parameters to SI units
        final double mmToM    = 1.0e-3;
        final double masToRad = 1.0e-3 * Constants.ARC_SECONDS_TO_RADIANS;

        this.epoch          = epoch;
        this.translation    = new Vector3D(t1 * mmToM,
                                           t2 * mmToM,
                                           t3 * mmToM);
        this.velocity       = new Vector3D(t1Dot * mmToM / Constants.JULIAN_YEAR,
                                           t2Dot * mmToM / Constants.JULIAN_YEAR,
                                           t3Dot * mmToM / Constants.JULIAN_YEAR);
        this.rotationVector = new Vector3D(r1 * masToRad,
                                           r2 * masToRad,
                                           r3 * masToRad);
        this.rotationRate   = new Vector3D(r1Dot * masToRad / Constants.JULIAN_YEAR,
                                           r2Dot * masToRad / Constants.JULIAN_YEAR,
                                           r3Dot * masToRad / Constants.JULIAN_YEAR);

    }

    /** Get the reference epoch of the transform.
     * @return reference epoch of the transform
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Compute the transform at some date.
     * @param date date at which the transform is desired
     * @return computed transform at specified date
     */
    public Transform getTransform(final AbsoluteDate date) {

        // compute parameters evolution since reference epoch
        final double dt = date.durationFrom(epoch);
        final Vector3D dP = new Vector3D(1, translation, dt, velocity);
        final Vector3D dR = new Vector3D(1, rotationVector, dt, rotationRate);

        // build tranlation part
        final Transform translationTransform = new Transform(dP, velocity);

        // build rotation part
        final double angle = dR.getNorm();
        final Transform rotationTransform =
            new Transform((angle < MathUtils.SAFE_MIN) ? Rotation.IDENTITY : new Rotation(dR, angle),
                          rotationRate);

        // combine both parts
        return new Transform(translationTransform, rotationTransform);

    }

}
