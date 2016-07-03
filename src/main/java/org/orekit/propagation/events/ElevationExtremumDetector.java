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

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Detector for elevation extremum with respect to a ground point.
 * <p>This detector identifies when a spacecraft reaches its
 * extremum elevation with respect to a ground point.</p>
 * <p>
 * As in most cases only the elevation maximum is needed and the
 * minimum is often irrelevant, this detector is often wrapped into
 * an {@link EventSlopeFilter event slope filter} configured with
 * {@link FilterType#TRIGGER_ONLY_DECREASING_EVENTS} (i.e. when the
 * elevation derivative decreases from positive values to negative values,
 * which correspond to a maximum). Setting up this filter saves some computation
 * time as the elevation minimum occurrences are not even looked at. It is
 * however still often necessary to do an additional filtering
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class ElevationExtremumDetector extends AbstractDetector<ElevationExtremumDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150909L;

    /** Topocentric frame in which elevation should be evaluated. */
    private final TopocentricFrame topo;

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param topo topocentric frame centered on ground point
     */
    public ElevationExtremumDetector(final TopocentricFrame topo) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, topo);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param topo topocentric frame centered on ground point
     */
    public ElevationExtremumDetector(final double maxCheck, final double threshold,
                                     final TopocentricFrame topo) {
        this(maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnIncreasing<ElevationExtremumDetector>(),
             topo);
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
     * @param topo topocentric frame centered on ground point
     */
    private ElevationExtremumDetector(final double maxCheck, final double threshold,
                                      final int maxIter, final EventHandler<? super ElevationExtremumDetector> handler,
                                      final TopocentricFrame topo) {
        super(maxCheck, threshold, maxIter, handler);
        this.topo = topo;
    }

    /** {@inheritDoc} */
    @Override
    protected ElevationExtremumDetector create(final double newMaxCheck, final double newThreshold,
                                              final int newMaxIter,
                                              final EventHandler<? super ElevationExtremumDetector> newHandler) {
        return new ElevationExtremumDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, topo);
    }

    /**
     * Returns the topocentric frame centered on ground point.
     * @return topocentric frame centered on ground point
     */
    public TopocentricFrame getTopocentricFrame() {
        return this.topo;
    }

    /** Get the elevation value.
     * @param s the current state information: date, kinematics, attitude
     * @return spacecraft elevation
     * @exception OrekitException if some specific error occurs
     */
    public double getElevation(final SpacecraftState s) throws OrekitException {
        return topo.getElevation(s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate());
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the spacecraft elevation first time derivative.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return spacecraft elevation first time derivative
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {

        // get position, velocity acceleration of spacecraft in topocentric frame
        final Transform inertToTopo = s.getFrame().getTransformTo(topo, s.getDate());
        final TimeStampedPVCoordinates pvTopo = inertToTopo.transformPVCoordinates(s.getPVCoordinates());

        // convert the coordinates to DerivativeStructure based vector
        // instead of having vector position, then vector velocity then vector acceleration
        // we get one vector and each coordinate is a DerivativeStructure containing
        // value, first time derivative (we don't need second time derivative here)
        final FieldVector3D<DerivativeStructure> pvDS = pvTopo.toDerivativeStructureVector(1);

        // compute elevation and its first time derivative
        final DerivativeStructure elevation = pvDS.getZ().divide(pvDS.getNorm()).asin();

        // return elevation first time derivative
        return elevation.getPartialDerivative(1);

    }

}
