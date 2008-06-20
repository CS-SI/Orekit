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
package org.orekit.utils;

import java.io.Serializable;

import org.apache.commons.math.geometry.Vector3D;

/** Simple container for Position/Velocity pairs.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class PVCoordinates implements Serializable {

    /** Fixed position/velocity at origin (both p and v are zero vectors). */
    public static final PVCoordinates ZERO = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);

    /** Serializable UID. */
    private static final long serialVersionUID = 8581359579182947710L;

    /** The position. */
    private final Vector3D position;

    /** The velocity. */
    private final Vector3D velocity;

    /** Simple constructor.
     * <p> Sets the Coordinates to default : (0 0 0) (0 0 0).
     */
    public PVCoordinates() {
        position = Vector3D.ZERO;
        velocity = Vector3D.ZERO;
    }

    /** Builds a PVCoordinates couple.
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public PVCoordinates(final Vector3D position, final Vector3D velocity) {
        this.position = position;
        this.velocity = velocity;
    }

    /** Multiplicative constructor
     * Build a PVCoordinates from another one and a scale factor.
     * The PVCoordinates built will be a * pv
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public PVCoordinates(final double a, final PVCoordinates pv) {
        this.position = new Vector3D(a, pv.position);
        this.velocity = new Vector3D(a, pv.velocity);
    }

    /** Linear constructor
     * Build a PVCoordinates from two other ones and corresponding scale factors.
     * The PVCoordinates built will be a1 * u1 + a2 * u2
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public PVCoordinates(final double a1, final PVCoordinates pv1,
                         final double a2, final PVCoordinates pv2) {
        this.position = new Vector3D(a1, pv1.position, a2, pv2.position);
        this.velocity = new Vector3D(a1, pv1.velocity, a2, pv2.velocity);
    }

    /** Linear constructor
     * Build a PVCoordinates from three other ones and corresponding scale factors.
     * The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3
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
        this.position = new Vector3D(a1, pv1.position, a2, pv2.position, a3, pv3.position);
        this.velocity = new Vector3D(a1, pv1.velocity, a2, pv2.velocity, a3, pv3.velocity);
    }

    /** Linear constructor
     * Build a PVCoordinates from four other ones and corresponding scale factors.
     * The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4
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
        this.position = new Vector3D(a1, pv1.position, a2, pv2.position,
                          a3, pv3.position, a4, pv4.position);
        this.velocity = new Vector3D(a1, pv1.velocity, a2, pv2.velocity,
                          a3, pv3.velocity, a4, pv4.velocity);
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
