/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.orbits;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;

/** Builder for orbits of satellites forming a Walker constellation.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class WalkerConstellation {

    /** Total number of satellites. */
    private final int t;

    /** Number of orbital planes. */
    private final int p;

    /** Phasing parameter. */
    private final int f;

    /** Simple constructor.
     * @param t total number of satellites
     * @param p number of orbital planes
     * @param f phasing parameter
     */
    public WalkerConstellation(final int t, final int p, final int f) {
        this.t = t;
        this.p = p;
        this.f = f;
        if (t % p != 0) {
            throw new OrekitException(OrekitMessages.WALKER_INCONSISTENT_PLANES, p, t);
        }
    }

    /** Get the total number of satellites.
     * @return total number of satellites
     */
    public int getT() {
        return t;
    }

    /** Get the number of orbital planes.
     * @return number of orbital planes
     */
    public int getP() {
        return p;
    }

    /** Get the phasing parameter.
     * @return phasing parameter
     */
    public int getF() {
        return f;
    }

    /** Create the regular slots.
     * <p>
     * This method builds the {@link #getT() T} regular satellite, with
     * integer {@link WalkerConstellationSlot#getSatellite() satellite indices}. If
     * additional in-orbit spare satellites must be created, the {@link
     * #buildSlot(WalkerConstellationSlot, int, double) buildSlot} method must be called
     * explicitly.
     * </p>
     * <p>
     * The various orbits are built from the {@code referenceOrbit} using plane
     * rotations and {@link Orbit#shiftedBy(double) shifts}. This implies that
     * if orbit does not include non-Keplerian derivatives, a
     * simple Keplerian motion is assumed, which is the intended use case.
     * </p>
     * @param <O> type of the orbits
     * @param referenceOrbit orbit of the reference satellite, in
     * {@link WalkerConstellationSlot#getPlane() plane} 0 and
     * at {@link WalkerConstellationSlot#getSatellite()} satellite index} 0
     * @return built orbits as a list of list, organized by planes
     * @see #buildReferenceSlot(Orbit)
     * @see #buildSlot(WalkerConstellationSlot, int, double)
     */
    public <O extends Orbit> List<List<WalkerConstellationSlot<O>>> buildRegularSlots(final O referenceOrbit) {

        // build the reference slot
        final WalkerConstellationSlot<O> referenceSlot = buildReferenceSlot(referenceOrbit);

        final List<List<WalkerConstellationSlot<O>>> all = new ArrayList<>(p);
        for (int plane = 0; plane < p; ++plane) {

            // prepare list for one plane
            final List<WalkerConstellationSlot<O>> planeSlots = new ArrayList<>(t / p);

            // build all slots belonging to this plane
            for (int satellite = 0; satellite < t / p; ++satellite) {
                planeSlots.add(plane == 0 && satellite == 0 ?
                               referenceSlot :
                               buildSlot(referenceSlot, plane, satellite));
            }

            // finished plane
            all.add(planeSlots);

        }

        // return the complete constellation
        return all;

    }

    /** Create the reference slot, which is satellite 0 in plane 0.
     * @param <O> type of the orbits
     * @param referenceOrbit orbit of the reference satellite, in
     * {@link WalkerConstellationSlot#getPlane() plane} 0 and
     * at {@link WalkerConstellationSlot#getSatellite()} satellite index} 0
     * @return build reference slot
     * @see #buildRegularSlots(Orbit)
     * @see #buildSlot(WalkerConstellationSlot, int, double)
     */
    public <O extends Orbit> WalkerConstellationSlot<O>buildReferenceSlot(final O referenceOrbit) {
        return new WalkerConstellationSlot<>(this, 0, 0, referenceOrbit);
    }

    /** Create one offset slot from an already existing slot.
     * @param <O> type of the orbits
     * @param existingSlot existing slot (may be the {@link #buildReferenceSlot(Orbit) reference slot} or not)
     * @param plane plane index of the new slot (may be non-integer for in-orbit spare satellites)
     * @param satellite new slot satellite index in plane (may be non-integer if needed)
     * @return built slot
     * @see #buildRegularSlots(Orbit)
     * @see #buildReferenceSlot(Orbit)
     */
    public <O extends Orbit> WalkerConstellationSlot<O> buildSlot(final WalkerConstellationSlot<O> existingSlot,
                                                                  final int plane, final double satellite) {

        // offsets from existing slot
        final O      refOrbit = existingSlot.getOrbit();
        final int    dp       = plane - existingSlot.getPlane();
        final double ds       = satellite - existingSlot.getSatellite();

        // in plane shift
        final double deltaT = (dp * f + ds * p) * refOrbit.getKeplerianPeriod() / t;
        final Orbit shifted = refOrbit.shiftedBy(deltaT);

        // plane rotation
        final Rotation      r       = new Rotation(Vector3D.PLUS_K,
                                                   MathUtils.TWO_PI * dp / p,
                                                   RotationConvention.VECTOR_OPERATOR);
        final PVCoordinates pv      = shifted.getPVCoordinates();
        final PVCoordinates rotated = new PVCoordinates(r.applyTo(pv.getPosition()),
                                                        r.applyTo(pv.getVelocity()));

        // build orbit
        final CartesianOrbit c = new CartesianOrbit(rotated, refOrbit.getFrame(),
                                                    refOrbit.getDate(), refOrbit.getMu());
        @SuppressWarnings("unchecked")
        final O orbit = (O) refOrbit.getType().convertType(c);

        // build slot
        return new WalkerConstellationSlot<>(this, plane, satellite, orbit);

    }

}
