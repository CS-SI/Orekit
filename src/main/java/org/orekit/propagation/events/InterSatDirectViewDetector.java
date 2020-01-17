/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;


/** Detector for inter-satellites direct view (i.e. no masking by central body limb).
 * <p>
 * As this detector needs two satellites, it embeds one {@link
 * PVCoordinatesProvider coordinates provider} for the slave satellite
 * and is registered as an event detector in the propagator of the master
 * satellite. The slave satellite provider will therefore be driven by this
 * detector (and hence by the propagator in which this detector is registered).
 * </p>
 * <p>
 * In order to avoid infinite recursion, care must be taken to have the slave
 * satellite provider being <em>completely independent</em> from anything else.
 * In particular, if the provider is a propagator, it should <em>not</em> be run
 * together in a {@link PropagatorsParallelizer propagators parallelizer} with
 * the propagator this detector is registered in. It is fine however to configure
 * two separate propagators PsA and PsB with similar settings for the slave satellite
 * and one propagator Pm for the master satellite and then use Psa in this detector
 * registered within Pm while Pm and Psb are run in the context of a {@link
 * PropagatorsParallelizer propagators parallelizer}.
 * </p>
 * <p>
 * For efficiency reason during the event search loop, it is recommended to have
 * the slave provider be an analytical propagator or an ephemeris. A numerical propagator
 * as a slave propagator works but is expected to be computationally costly.
 * </p>
 * <p>
 * The {@code g} function of this detector is positive when satellites can see
 * each other directly and negative when the central body limb is in between and
 * blocks the direct view.
 * </p>
 * <p>
 * This detector only checks masking by central body limb, it does not take into
 * account satellites antenna patterns. If these patterns must be considered, then
 * this detector can be {@link BooleanDetector#andCombine(EventDetector...) and combined}
 * with  the {@link BooleanDetector#notCombine(EventDetector) logical not} of
 * {@link FieldOfViewDetector field of view detectors}.
 * </p>
 * @author Luc Maisonobe
 * @since 9.3
 */
public class InterSatDirectViewDetector extends AbstractDetector<InterSatDirectViewDetector> {

    /** Central body. */
    private final OneAxisEllipsoid body;

    /** Equatorial radius squared. */
    private final double ae2;

    /** 1 minus flatness squared. */
    private final double g2;

    /** Coordinates provider for the slave satellite. */
    private final PVCoordinatesProvider slave;

    /** simple constructor.
     *
     * @param body central body
     * @param slave provider for the slave satellite
     */
    public InterSatDirectViewDetector(final OneAxisEllipsoid body, final PVCoordinatesProvider slave) {
        this(body, slave, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
             new ContinueOnEvent<>());
    }

    /** Private constructor.
     * @param body central body
     * @param slave provider for the slave satellite
     * @param maxCheck  maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter   maximum number of iterations in the event time search
     * @param handler   event handler to call at event occurrences
     */
    private InterSatDirectViewDetector(final OneAxisEllipsoid body,
                                       final PVCoordinatesProvider slave,
                                       final double maxCheck,
                                       final double threshold,
                                       final int maxIter,
                                       final EventHandler<? super InterSatDirectViewDetector> handler) {
        super(maxCheck, threshold, maxIter, handler);
        this.body  = body;
        this.ae2   = body.getEquatorialRadius() * body.getEquatorialRadius();
        this.g2    = (1.0 - body.getFlattening()) * (1.0 - body.getFlattening());
        this.slave = slave;
    }

    /** Get the central body.
     * @return central body
     */
    public OneAxisEllipsoid getCentralBody() {
        return body;
    }

    /** Get the provider for the slave satellite.
     * @return provider for the slave satellite
     */
    public PVCoordinatesProvider getSlave() {
        return slave;
    }

    /** {@inheritDoc} */
    @Override
    protected InterSatDirectViewDetector create(final double newMaxCheck,
                                                final double newThreshold,
                                                final int newMaxIter,
                                                final EventHandler<? super InterSatDirectViewDetector> newHandler) {
        return new InterSatDirectViewDetector(body, slave, newMaxCheck, newThreshold, newMaxIter, newHandler);
    }

    /** {@inheritDoc}
     * <p>
     * The {@code g} function of this detector is positive when satellites can see
     * each other directly and negative when the central body limb is in between and
     * blocks the direct view.
     * </p>
     */
    @Override
    public double g(final SpacecraftState state) {

        // get the line between master and slave in body frame
        final AbsoluteDate date    = state.getDate();
        final Frame        frame   = body.getBodyFrame();
        final Vector3D     pMaster = state.getPVCoordinates(frame).getPosition();
        final Vector3D     pSlave  = slave.getPVCoordinates(date, frame).getPosition();

        // points along the master/slave lines are defined as
        // xk = x + k * dx, yk = y + k * dy, zk = z + k * dz
        // so k is 0 at master and 1 at slave
        final double x  = pMaster.getX();
        final double y  = pMaster.getY();
        final double z  = pMaster.getZ();
        final double dx = pSlave.getX() - x;
        final double dy = pSlave.getY() - y;
        final double dz = pSlave.getZ() - z;

        // intersection between line and central body surface
        // is a root of a 2nd degree polynomial :
        // a k^2 - 2 b k + c = 0
        final double a =   g2 * (dx * dx + dy * dy) + dz * dz;
        final double b = -(g2 * (x * dx + y * dy) + z * dz);
        final double c =   g2 * (x * x + y * y - ae2) + z * z;
        final double s = b * b - a * c;
        if (s < 0) {
            // the quadratic has no solution, the line between master and slave
            // doesn't crosses central body limb, direct view is possible
            // return a positive value, preserving continuity across zero crossing
            return -s;
        }

        // the quadratic has two solutions (degenerated to one if s = 0)
        // direct view is blocked when one of these solutions is between 0 and 1
        final double k1 = (b < 0) ? (b - FastMath.sqrt(s)) / a : c / (b + FastMath.sqrt(s));
        final double k2 = c / (a * k1);
        if (FastMath.max(k1, k2) < 0.0 || FastMath.min(k1, k2) > 1.0) {
            // the intersections are either behind master or farther away than slave
            // along the line, direct view is possible
            // return a positive value, preserving continuity across zero crossing
            return s;
        } else {
            // part of the central body is between master and slave
            // this includes unrealistic cases where master, slave or both are inside the central body ;-)
            // in all these cases, direct view is blocked
            // return a negative value, preserving continuity across zero crossing
            return -s;
        }

    }

}
