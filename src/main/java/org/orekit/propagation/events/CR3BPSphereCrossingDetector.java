/* Copyright 2002-2019 CS Systèmes d'Information
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

package org.orekit.propagation.events;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnDecreasing;

/** Detector for YZ Planes crossing.
 * @author Vincent Mouraux
 */
public class CR3BPSphereCrossingDetector
    extends
    AbstractDetector<CR3BPSphereCrossingDetector> {

    /** Radius of the primary body. */
    private final double primaryR;

    /** Radius of the secondary body. */
    private final double secondaryR;

    /** CR3BP System. */
    private final CR3BPSystem syst;

    /**
     * Simple Constructor.
     * @param primaryR Radius of the primary body sphere (m)
     * @param secondaryR Radius of the secondary body sphere (m)
     * @param syst CR3BP System considered
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public CR3BPSphereCrossingDetector(final double primaryR, final double secondaryR, final CR3BPSystem syst, final double maxCheck, final double threshold) {
        this(primaryR, secondaryR, syst, maxCheck, threshold, DEFAULT_MAX_ITER,
             new StopOnDecreasing<CR3BPSphereCrossingDetector>());
    }

    /**
     * Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder API
     * with the various {@code withXxx()} methods to set up the instance in a
     * readable manner without using a huge amount of parameters.
     * </p>
     * @param primaryR Radius of the primary body sphere (m)
     * @param secondaryR Radius of the secondary body sphere (m)
     * @param syst CR3BP System considered
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     */
    private CR3BPSphereCrossingDetector(final double primaryR, final double secondaryR, final CR3BPSystem syst, final double maxCheck, final double threshold,
                             final int maxIter,
                             final EventHandler<? super CR3BPSphereCrossingDetector> handler) {
        super(maxCheck, threshold, maxIter, handler);
        this.primaryR = primaryR;
        this.secondaryR = secondaryR;
        this.syst = syst;
    }

    /** {@inheritDoc} */
    @Override
    protected CR3BPSphereCrossingDetector
        create(final double newMaxCheck, final double newThreshold,
               final int newMaxIter,
               final EventHandler<? super CR3BPSphereCrossingDetector> newHandler) {
        return new CR3BPSphereCrossingDetector(primaryR, secondaryR, syst, newMaxCheck, newThreshold, newMaxIter,
                                    newHandler);
    }

    /** Compute the value of the detection function.
     * @param s the current state information: date, kinematics, attitude
     * @return Product of the differences between the spacecraft and the center of the primaries
     */
    public double g(final SpacecraftState s) {
        final double mu = syst.getMassRatio();
        final double dDim = syst.getDdim();

        final double x = s.getPVCoordinates().getPosition().getX();
        final double y = s.getPVCoordinates().getPosition().getY();
        final double z = s.getPVCoordinates().getPosition().getZ();

        final double r1 = FastMath.sqrt((x + mu) * (x + mu) + y * y + z * z) * dDim;
        final double r2 = FastMath.sqrt((x - 1 + mu) * (x - 1 + mu) + y * y + z * z) * dDim;
        return (r1 - primaryR) * (r2 - secondaryR);

    }
}
