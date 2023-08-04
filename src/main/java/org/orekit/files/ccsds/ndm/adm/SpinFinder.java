/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.adm;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.attitudes.Attitude;
import org.orekit.utils.TimeStampedAngularCoordinates;

/** Utility to extract spin data.
 * <p>
 * In CCSDS ADM, spin axis is forced to Z. It is not the instantaneous rotation
 * rate as it also moves.
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
class SpinFinder {

    /** Spin axis. */
    private final Vector3D spin;

    /** Right ascension of spin axis (rad). */
    private final double spinAlpha;

    /** declination of spin axis (rad). */
    private final double spinDelta;

    /** Spin angle. */
    private final double spinAngle;

    /** Build from attitude rotation.
     * @param attitude angular coordinates, using {@link Attitude Attitude} convention
     * (i.e. from inertial frame to spacecraft frame)
     */
    SpinFinder(final TimeStampedAngularCoordinates attitude) {
        // spin axis is forced to Z (but it is not the instantaneous rotation rate as it also moves)
        spin      = attitude.getRotation().applyInverseTo(Vector3D.PLUS_K);
        spinAlpha = spin.getAlpha();
        spinDelta = spin.getDelta();
        final Rotation alignSpin   = new Rotation(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                                  MathUtils.SEMI_PI + spinAlpha,
                                                  MathUtils.SEMI_PI - spinDelta,
                                                  0.0);
        final Rotation phasing     = attitude.getRotation().applyTo(alignSpin.revert());
        spinAngle = FastMath.copySign(phasing.getAngle(),
                                      phasing.getAxis(RotationConvention.FRAME_TRANSFORM).getZ());

    }

    /** Get the spin axis in inertial frame.
     * @return spin axis in inertial frame
     */
    public Vector3D getSpin() {
        return spin;
    }

    /** Get the declination of spin axis.
     * @return declination of spin axis (rad)
     */
    public double getSpinDelta() {
        return spinDelta;
    }

    /** Get the right ascension of spin axis.
     * @return right ascension of spin axis (rad)
     */
    public double getSpinAlpha() {
        return spinAlpha;
    }

    /** Get the spin angle.
     * @return spin angle (rad)
     */
    public double getSpinAngle() {
        return spinAngle;
    }

}
