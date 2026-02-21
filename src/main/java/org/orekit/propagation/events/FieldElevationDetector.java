/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.functions.AbstractElevationCrossingFunction;
import org.orekit.propagation.events.functions.ElevationValueCrossingFunction;
import org.orekit.propagation.events.functions.MaskedElevationEventFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnDecreasing;
import org.orekit.utils.ElevationMask;


/**
 * Finder for satellite raising/setting events that allows for the
 * setting of azimuth and/or elevation bounds or a ground azimuth/elevation
 * mask input. Each calculation be configured to use atmospheric refraction
 * as well.
 * <p>The default implementation behavior is to {@link Action#CONTINUE continue}
 * propagation at raising and to {@link Action#STOP stop} propagation
 * at setting. This can be changed by calling
 * {@link #withHandler(FieldEventHandler)} after construction.</p>
 * @author Hank Grabowski
 * @param <T> type of the field elements
 */
public class FieldElevationDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractTopocentricDetector<FieldElevationDetector<T>, T> {

    /**
     * Creates an instance of Elevation detector based on passed in topocentric frame
     * and the minimum elevation angle.
     * <p>
     * uses default values for maximal checking interval ({@link #DEFAULT_MAX_CHECK})
     * and convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * @param field type of the elements
     * @param topo reference to a topocentric model
     * @see #withConstantElevation(double)
     * @see #withElevationMask(ElevationMask)
     * @see #withRefraction(AtmosphericRefractionModel)
     */
    public FieldElevationDetector(final Field<T> field, final TopocentricFrame topo) {
        this(new FieldEventDetectionSettings<>(field, EventDetectionSettings.getDefaultEventDetectionSettings()),
             new FieldStopOnDecreasing<>(), new ElevationValueCrossingFunction(null, topo, 0.));
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
    public FieldElevationDetector(final T maxCheck, final T threshold, final TopocentricFrame topo) {
        this(new FieldEventDetectionSettings<>(maxCheck.getReal(), threshold, DEFAULT_MAX_ITER),
             new FieldStopOnDecreasing<>(), new ElevationValueCrossingFunction(null, topo, 0.));
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param detectionSettings detection settings
     * @param handler event handler to call at event occurrences
     * @param elevationCrossingFunction elevation crossing function
     * @since 14.0
     */
    public FieldElevationDetector(final FieldEventDetectionSettings<T> detectionSettings, final FieldEventHandler<T> handler,
                                  final AbstractElevationCrossingFunction elevationCrossingFunction) {
        super(elevationCrossingFunction, detectionSettings, handler, elevationCrossingFunction.getTopocentricFrame());
    }

    /** {@inheritDoc} */
    @Override
    protected FieldElevationDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                               final FieldEventHandler<T> newHandler) {
        return new FieldElevationDetector<>(detectionSettings, newHandler, (AbstractElevationCrossingFunction) getEventFunction());
    }

    /**
     * Returns the currently configured elevation mask.
     * @return elevation mask
     * (null if instance has been configured with {@link #withConstantElevation(double)}
     * @see #withElevationMask(ElevationMask)
     */
    public ElevationMask getElevationMask() {
        if (getEventFunction() instanceof MaskedElevationEventFunction) {
            return ((MaskedElevationEventFunction) getEventFunction()).getElevationMask();
        } else {
            return null;
        }
    }

    /**
     * Returns the currently configured minimum valid elevation value.
     * @return minimum elevation value
     * ({@code Double.NaN} if instance has been configured with {@link #withElevationMask(ElevationMask)}
     * @see #withConstantElevation(double)
     */
    public double getMinElevation() {
        if (getEventFunction() instanceof ElevationValueCrossingFunction) {
            return ((ElevationValueCrossingFunction) getEventFunction()).getCriticalElevation();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Returns the currently configured refraction model.
     * @return refraction model
     * @see #withRefraction(AtmosphericRefractionModel)
     */
    public AtmosphericRefractionModel getRefractionModel() {
        return ((AbstractElevationCrossingFunction) getEventFunction()).getRefractionModel();
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current elevation
     * (and azimuth if necessary) and the reference mask or minimum value.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    @Override
    public T g(final FieldSpacecraftState<T> s) {
        return getEventFunction().value(s);
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
    public FieldElevationDetector<T> withConstantElevation(final double newMinElevation) {
        return new FieldElevationDetector<>(getDetectionSettings(), getHandler(),
                new ElevationValueCrossingFunction(getRefractionModel(), getTopocentricFrame(), newMinElevation));
    }

    /**
     * Setup the elevation mask for detection using the passed in mask object.
     * @param newElevationMask elevation mask to use for the computation
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     * @see #getElevationMask()
     */
    public FieldElevationDetector<T> withElevationMask(final ElevationMask newElevationMask) {
        return new FieldElevationDetector<>(getDetectionSettings(), getHandler(),
                new MaskedElevationEventFunction(getRefractionModel(), getTopocentricFrame(), newElevationMask));
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
    public FieldElevationDetector<T> withRefraction(final AtmosphericRefractionModel newRefractionModel) {
        if (getEventFunction() instanceof ElevationValueCrossingFunction) {
            return new FieldElevationDetector<>(getDetectionSettings(), getHandler(),
                    new ElevationValueCrossingFunction(newRefractionModel, getTopocentricFrame(), getMinElevation()));
        } else {
            return new FieldElevationDetector<>(getDetectionSettings(), getHandler(),
                    new MaskedElevationEventFunction(newRefractionModel, getTopocentricFrame(), getElevationMask()));
        }
    }

    @Override
    public ElevationDetector toEventDetector(final EventHandler eventHandler) {
        return new ElevationDetector(getDetectionSettings().toEventDetectionSettings(), eventHandler,
                (AbstractElevationCrossingFunction) getEventFunction());
    }
}
