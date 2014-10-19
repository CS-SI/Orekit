/* Copyright 2002-2014 CS Systèmes d'Information
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

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Common parts shared by several orbital events finders.
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public abstract class AbstractDetector implements EventDetector {

    /** Default maximum checking interval (s). */
    public static final double DEFAULT_MAXCHECK = 600;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = 1.e-6;

    /** Default cmaximum number of iterations in the event time search. */
    public static final int DEFAULT_MAX_ITER = 100;

    /** Serializable UID. */
    private static final long serialVersionUID = 20131202l;

    /** Max check interval. */
    private final double maxCheck;

    /** Convergence threshold. */
    private final double threshold;

    /** Maximum number of iterations in the event time search. */
    private final int maxIter;

    /** Build a new instance.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @deprecated as of 6.1, replaced with {@link #AbstractDetector(double, double, int)}
     */
    @Deprecated
    protected AbstractDetector(final double maxCheck, final double threshold) {
        this(maxCheck, threshold, DEFAULT_MAX_ITER);
    }

    /** Build a new instance.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     */
    protected AbstractDetector(final double maxCheck, final double threshold, final int maxIter) {
        this.maxCheck  = maxCheck;
        this.threshold = threshold;
        this.maxIter   = maxIter;
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // do nothing by default
    }

    /** {@inheritDoc}
     * @deprecated as of 6.1 replaced by {@link
     * org.orekit.propagation.events.handlers.EventHandler#eventOccurred(SpacecraftState,
     * EventDetector, boolean)}
     */
    @Deprecated
    public abstract Action eventOccurred(SpacecraftState s, boolean increasing)
        throws OrekitException;

    /** {@inheritDoc} */
    public abstract double g(SpacecraftState s) throws OrekitException;

    /** {@inheritDoc} */
    public double getMaxCheckInterval() {
        return maxCheck;
    }

    /** {@inheritDoc} */
    public int getMaxIterationCount() {
        return maxIter;
    }

    /** {@inheritDoc} */
    public double getThreshold() {
        return threshold;
    }

    /** {@inheritDoc}
     * @deprecated as of 6.1 replaced by {@link
     * org.orekit.propagation.events.handlers.EventHandler#resetState(EventDetector, SpacecraftState)}
     */
    @Deprecated
    public SpacecraftState resetState(final SpacecraftState oldState)
        throws OrekitException {
        return oldState;
    }

}
