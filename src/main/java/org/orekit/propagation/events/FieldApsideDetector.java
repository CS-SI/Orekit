/* Copyright 2002-2016 CS Systèmes d'Information
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

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnIncreasing;
import org.orekit.utils.FieldPVCoordinates;

/** Finder for apside crossing events.
 * <p>This class finds apside crossing events (i.e. apogee or perigee crossing).</p>
 * <p>The default implementation behavior is to {@link
 * org.orekit.propagation.events.handlers.FieldEventHandler.Action#CONTINUE continue}
 * propagation at apogee crossing and to {@link
 * org.orekit.propagation.events.handlers.FieldEventHandler.Action#STOP stop} propagation
 * at perigee crossing. This can be changed by calling
 * {@link #withHandler(FieldEventHandler)} after construction.</p>
 * <p>Beware that apside detection will fail for almost circular orbits. If
 * for example an apside detector is used to trigger an {@link
 * org.orekit.forces.maneuvers.ImpulseManeuver ImpulseManeuver} and the maneuver
 * change the orbit shape to circular, then the detector may completely fail just
 * after the maneuver has been performed!</p>
 * @see org.orekit.propagation.FieldPropagator#addEventDetector(FieldEventDetector)
 * @author Luc Maisonobe
 */
public class FieldApsideDetector<T extends RealFieldElement<T>> extends FieldAbstractDetector<FieldApsideDetector<T>, T> {

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the
     * max check interval to period/3 and to set the convergence
     * threshold according to orbit size</p>
     * @param orbit initial orbit
     */
    public FieldApsideDetector(final FieldOrbit<T> orbit) {
        this(orbit.getKeplerianPeriod().multiply(1.0e-13), orbit);
    }

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the
     * max check interval to period/3</p>
     * @param threshold convergence threshold (s)
     * @param orbit initial orbit
     */
    public FieldApsideDetector(final T threshold, final FieldOrbit<T> orbit) {
        super(orbit.getKeplerianPeriod().divide(3), threshold,
              DEFAULT_MAX_ITER, new FieldStopOnIncreasing<FieldApsideDetector<T>, T>());
    }

    /** Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     */
    private FieldApsideDetector(final T maxCheck, final T threshold,
                                final int maxIter, final FieldEventHandler<? super FieldApsideDetector<T>, T> handler) {
        super(maxCheck, threshold, maxIter, handler);
    }

    /** {@inheritDoc} */
    @Override
    protected FieldApsideDetector<T> create(final T newMaxCheck, final T newThreshold,
                                            final int newMaxIter,
                                            final FieldEventHandler<? super FieldApsideDetector<T>, T> newHandler) {
        return new FieldApsideDetector<T>(newMaxCheck, newThreshold, newMaxIter, newHandler);
    }

    /** Compute the value of the switching function.
     * This function computes the dot product of the 2 vectors : position.velocity.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public T g(final FieldSpacecraftState<T> s) throws OrekitException {
        final FieldPVCoordinates<T> pv = s.getPVCoordinates();
        return FieldVector3D.dotProduct(pv.getPosition(), pv.getVelocity());
    }

}
