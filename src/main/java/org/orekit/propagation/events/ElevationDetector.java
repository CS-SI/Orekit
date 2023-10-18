/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.events;

import org.hipparchus.ode.events.Action;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnDecreasing;
import org.orekit.utils.ElevationMask;
import org.orekit.utils.TrackingCoordinates;


/**
 * Finder for satellite raising/setting events that allows for the
 * setting of azimuth and/or elevation bounds or a ground azimuth/elevation
 * mask input. Each calculation be configured to use atmospheric refraction
 * as well.
 * <p>The default implementation behavior is to {@link Action#CONTINUE continue}
 * propagation at raising and to {@link Action#STOP stop} propagation
 * at setting. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @author Hank Grabowski
 * @since 6.1
 */
public class ElevationDetector extends AbstractDetector<ElevationDetector> {

    /** Elevation mask used for calculations, if defined. */
    private final ElevationMask elevationMask;

    /** Minimum elevation value used if mask is not defined. */
    private final double minElevation;

    /** Atmospheric Model used for calculations, if defined. */
    private final AtmosphericRefractionModel refractionModel;

    /** Topocentric frame in which elevation should be evaluated. */
    private final TopocentricFrame topo;

    /**
     * Creates an instance of Elevation detector based on passed in topocentric frame
     * and the minimum elevation angle.
     * <p>
     * uses default values for maximal checking interval ({@link #DEFAULT_MAXCHECK})
     * and convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * @param topo reference to a topocentric model
     * @see #withConstantElevation(double)
     * @see #withElevationMask(ElevationMask)
     * @see #withRefraction(AtmosphericRefractionModel)
     */
    public ElevationDetector(final TopocentricFrame topo) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, topo);
    }

    /**
     * Creates an instance of Elevation detector based on passed in topocentric frame
     * and overrides of default maximal checking interval and convergence threshold values.
     * @param maxCheck maximum checking interval (s)
     * @param threshold maximum divergence threshold (s)
     * @param topo reference to a topocentric model
     * @see #withConstantElevation(double)
     * @see #withElevationMask(ElevationMask)
     * @see #withRefraction(AtmosphericRefractionModel)
     */
    public ElevationDetector(final double maxCheck, final double threshold,
                             final TopocentricFrame topo) {
        this(s -> maxCheck, threshold, DEFAULT_MAX_ITER,
             new StopOnDecreasing(),
             0.0, null, null, topo);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param minElevation minimum elevation in radians (rad)
     * @param mask reference to elevation mask
     * @param refractionModel reference to refraction model
     * @param topo reference to a topocentric model
     */
    protected ElevationDetector(final AdaptableInterval maxCheck, final double threshold,
                                final int maxIter, final EventHandler handler,
                                final double minElevation, final ElevationMask mask,
                                final AtmosphericRefractionModel refractionModel,
                                final TopocentricFrame topo) {
        super(maxCheck, threshold, maxIter, handler);
        this.minElevation    = minElevation;
        this.elevationMask   = mask;
        this.refractionModel = refractionModel;
        this.topo            = topo;
    }

    /** {@inheritDoc} */
    @Override
    protected ElevationDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                       final int newMaxIter, final EventHandler newHandler) {
        return new ElevationDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                     minElevation, elevationMask, refractionModel, topo);
    }

    /**
     * Returns the currently configured elevation mask.
     * @return elevation mask
     * (null if instance has been configured with {@link #withConstantElevation(double)}
     * @see #withElevationMask(ElevationMask)
     */
    public ElevationMask getElevationMask() {
        return this.elevationMask;
    }

    /**
     * Returns the currently configured minimum valid elevation value.
     * @return minimum elevation value
     * ({@code Double.NaN} if instance has been configured with {@link #withElevationMask(ElevationMask)}
     * @see #withConstantElevation(double)
     */
    public double getMinElevation() {
        return this.minElevation;
    }

    /**
     * Returns the currently configured refraction model.
     * @return refraction model
     * @see #withRefraction(AtmosphericRefractionModel)
     */
    public AtmosphericRefractionModel getRefractionModel() {
        return this.refractionModel;
    }

    /**
     * Returns the currently configured topocentric frame definitions.
     * @return topocentric frame definition
     */
    public TopocentricFrame getTopocentricFrame() {
        return this.topo;
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current elevation
     * (and azimuth if necessary) and the reference mask or minimum value.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    @Override
    public double g(final SpacecraftState s) {

        final TrackingCoordinates tc = topo.getTrackingCoordinates(s.getPosition(), s.getFrame(), s.getDate());

        final double calculatedElevation;
        if (refractionModel != null) {
            calculatedElevation = tc.getElevation() + refractionModel.getRefraction(tc.getElevation());
        } else {
            calculatedElevation = tc.getElevation();
        }

        if (elevationMask != null) {
            return calculatedElevation - elevationMask.getElevation(tc.getAzimuth());
        } else {
            return calculatedElevation - minElevation;
        }

    }

    /**
     * Setup the minimum elevation for detection.
     * <p>
     * This will override an elevation mask if it has been configured as such previously.
     * </p>
     * @param newMinElevation minimum elevation for visibility in radians (rad)
     * @return a new detector with updated configuration (the instance is not changed)
     * @see #getMinElevation()
     * @since 6.1
     */
    public ElevationDetector withConstantElevation(final double newMinElevation) {
        return new ElevationDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
                                     newMinElevation, null, refractionModel, topo);
    }

    /**
     * Setup the elevation mask for detection using the passed in mask object.
     * @param newElevationMask elevation mask to use for the computation
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     * @see #getElevationMask()
     */
    public ElevationDetector withElevationMask(final ElevationMask newElevationMask) {
        return new ElevationDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
                                     Double.NaN, newElevationMask, refractionModel, topo);
    }

    /**
     * Setup the elevation detector to use an atmospheric refraction model in its
     * calculations.
     * <p>
     * To disable the refraction when copying an existing elevation
     * detector, call this method with a null argument.
     * </p>
     * @param newRefractionModel refraction model to use for the computation
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     * @see #getRefractionModel()
     */
    public ElevationDetector withRefraction(final AtmosphericRefractionModel newRefractionModel) {
        return new ElevationDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
                                     minElevation, elevationMask, newRefractionModel, topo);
    }

}
