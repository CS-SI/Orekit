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
package org.orekit.utils;

import java.io.Serializable;

import org.apache.commons.math.geometry.Vector3D;

/** Simple container for Position/Velocity pairs.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple linear model. It is <em>not</em> intended as a replacement for
 * proper orbit propagation (it is not even Keplerian!) but should be sufficient
 * for either small time shifts or coarse accuracy.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class PVCoordinates implements Serializable {

    /** Fixed position/velocity at origin (both p and v are zero vectors). */
    public static final PVCoordinates ZERO = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);

    /** Serializable UID. */
    private static final long serialVersionUID = 4157449919684833834L;

    /** The position. */
    private final Vector3D position;

    /** The velocity. */
    private final Vector3D velocity;

    /** Simple constructor.
     * <p> Sets the Coordinates to default : (0 0 0) (0 0 0).</p>
     */
    public PVCoordinates() {
        position = Vector3D.ZERO;
        velocity = Vector3D.ZERO;
    }

    /** Builds a PVCoordinates pair.
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public PVCoordinates(final Vector3D position, final Vector3D velocity) {
        this.position = position;
        this.velocity = velocity;
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public PVCoordinates(final double a, final PVCoordinates pv) {
        position = new Vector3D(a, pv.position);
        velocity = new Vector3D(a, pv.velocity);
    }

    /** Subtractive constructor
     * <p>Build a relative PVCoordinates from a start and an end position.</p>
     * <p>The PVCoordinates built will be end - start.</p>
     * @param start Starting PVCoordinates
     * @param end ending PVCoordinates
     */
    public PVCoordinates(final PVCoordinates start, final PVCoordinates end) {
        this.position = end.position.subtract(start.position);
        this.velocity = end.velocity.subtract(start.velocity);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public PVCoordinates(final double a1, final PVCoordinates pv1,
                         final double a2, final PVCoordinates pv2) {
        position = new Vector3D(a1, pv1.position, a2, pv2.position);
        velocity = new Vector3D(a1, pv1.velocity, a2, pv2.velocity);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public PVCoordinates(final double a1, final PVCoordinates pv1,
                         final double a2, final PVCoordinates pv2,
                         final double a3, final PVCoordinates pv3) {
        position = new Vector3D(a1, pv1.position, a2, pv2.position, a3, pv3.position);
        velocity = new Vector3D(a1, pv1.velocity, a2, pv2.velocity, a3, pv3.velocity);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     * @param a4 fourth scale factor
     * @param pv4 fourth base (unscaled) PVCoordinates
     */
    public PVCoordinates(final double a1, final PVCoordinates pv1,
                         final double a2, final PVCoordinates pv2,
                         final double a3, final PVCoordinates pv3,
                         final double a4, final PVCoordinates pv4) {
        position = new Vector3D(a1, pv1.position, a2, pv2.position, a3, pv3.position, a4, pv4.position);
        velocity = new Vector3D(a1, pv1.velocity, a2, pv2.velocity, a3, pv3.velocity, a4, pv4.velocity);
    }

    /** Estimate velocity between two positions.
     * <p>Estimation is based on a simple fixed velocity translation
     * during the time interval between the two positions.</p>
     * @param start start position
     * @param end end position
     * @param dt time elapsed between the dates of the two positions
     * @return velocity allowing to go from start to end positions
     */
    public static Vector3D estimateVelocity(final Vector3D start, final Vector3D end, final double dt) {
        final double scale = 1.0 / dt;
        return new Vector3D(scale, end, -scale, start);
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple linear model. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     * @see org.orekit.time.AbsoluteDate#shiftedBy(double)
     * @see org.orekit.attitudes.Attitude#shiftedBy(double)
     * @see org.orekit.orbits.Orbit#shiftedBy(double)
     * @see org.orekit.propagation.SpacecraftState#shiftedBy(double)
     */
    public PVCoordinates shiftedBy(final double dt) {
        return new PVCoordinates(new Vector3D(1, position, dt, velocity), velocity);
    }

    /** Gets the position.
     * @return the position vector (m).
     */
    public Vector3D getPosition() {
        return position;
    }

    /** Gets the velocity.
     * @return the velocity vector (m/s).
     */
    public Vector3D getVelocity() {
        return velocity;
    }

    /** Gets the momentum.
     * <p>This vector is the p &otimes; v where p is position, v is velocity
     * and &otimes; is cross product. To get the real physical angular momentum
     * you need to multiply this vector by the mass.</p>
     * <p>The returned vector is recomputed each time this method is called, it
     * is not cached.</p>
     * @return a new instance of the momentum vector (m<sup>2</sup>/s).
     */
    public Vector3D getMomentum() {
        return Vector3D.crossProduct(position, velocity);
    }

    /** Return a string representation of this position/velocity pair.
     * @return string representation of this position/velocity pair
     */
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append("P(").
                                  append(position.getX()).append(comma).
                                  append(position.getY()).append(comma).
                                  append(position.getZ()).append("), V(").
                                  append(velocity.getX()).append(comma).
                                  append(velocity.getY()).append(comma).
                                  append(velocity.getZ()).append(")}").toString();
    }

}
