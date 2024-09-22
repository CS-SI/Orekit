/* Copyright 2002-2024 CS GROUP
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnIncreasing;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Finder for extremum approach events.
 * <p>
 * This class finds extremum approach events (i.e. closest or farthest approach).
 * </p>
 * <p>
 * The default implementation behavior is to {@link Action#CONTINUE continue} propagation at farthest approach and to
 * {@link Action#STOP stop} propagation at closest approach. This can be changed by calling
 * {@link FieldAbstractDetector#withHandler(FieldEventHandler)} after construction (go to the end of the documentation to see
 * an example).
 * </p>
 * <p>
 * As this detector needs two objects (moving relative to each other), it embeds one
 * {@link FieldPVCoordinatesProvider fielded coordinates provider} for the secondary object and is registered as an event
 * detector in the propagator of the primary object. The secondary object
 * {@link FieldPVCoordinatesProvider fielded coordinates provider} will therefore be driven by this detector (and hence by
 * the propagator in which this detector is registered). Note that you can also create this detector using a standard
 * {@link PVCoordinatesProvider coordinates provider}
 * </p>
 * <p><b>
 * In order to avoid infinite recursion, care must be taken to have the secondary object provider being <em>completely
 * independent</em> from anything else. In particular, if the provider is a propagator, it should <em>not</em> be run
 * together in a {@link PropagatorsParallelizer propagators parallelizer} with the propagator this detector is registered in.
 * It is fine however to configure two separate propagators PsA and PsB with similar settings for the secondary object and
 * one propagator Pm for the primary object and then use Psa in this detector registered within Pm while Pm and Psb are run
 * in the context of a {@link PropagatorsParallelizer propagators parallelizer}.
 * </b></p>
 * <p>
 * For efficiency reason during the event search loop, it is recommended to have the secondary provider be an analytical
 * propagator or an ephemeris. A numerical propagator as a secondary propagator works but is expected to be computationally
 * costly.
 * </p>
 * <p>
 * Also, it is possible to detect solely one type of event using an {@link EventSlopeFilter event slope filter}. For example
 * in order to only detect closest approach, one should type the following :
 * </p>
 * <pre>{@code
 * FieldExtremumApproachDetector<Type> extremumApproachDetector = new FieldExtremumApproachDetector<>(field, secondaryPVProvider);
 * FieldEventDetector<Type> closeApproachDetector = new FieldEventSlopeFilter<>(extremumApproachDetector, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);
 *  }
 * </pre>
 *
 * @author Vincent Cucchietti
 * @see org.orekit.propagation.FieldPropagator#addEventDetector(FieldEventDetector)
 * @see FieldEventSlopeFilter
 * @see FilterType
 * @since 11.3
 */
public class FieldExtremumApproachDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldExtremumApproachDetector<T>, T> {

    /**
     * PVCoordinates provider of the other object with which we want to find out the extremum approach.
     */
    private final FieldPVCoordinatesProvider<T> secondaryPVProvider;

    /**
     * Constructor with default values.
     * <p>
     * By default, the implemented behavior is to {@link Action#CONTINUE continue} propagation at farthest approach and to
     * {@link Action#STOP stop} propagation at closest approach.
     * <p>
     * <b>BEWARE : This constructor will "fieldify" given secondary PV coordinates provider.</b>
     *
     * @param field field the type of number to use
     * @param secondaryPVProvider PVCoordinates provider of the other object with which we want to find out the extremum
     * approach.
     */
    public FieldExtremumApproachDetector(final Field<T> field, final PVCoordinatesProvider secondaryPVProvider) {
        this(field, (FieldPVCoordinatesProvider<T>) (date, frame) -> {
            final TimeStampedPVCoordinates timeStampedPV =
                    secondaryPVProvider.getPVCoordinates(date.toAbsoluteDate(), frame);
            return new TimeStampedFieldPVCoordinates<>(field, timeStampedPV);
        });
    }

    /**
     * Constructor with default values.
     * <p>
     * By default, the implemented behavior is to {@link Action#CONTINUE continue} propagation at farthest approach and to
     * {@link Action#STOP stop} propagation at closest approach.
     * </p>
     *
     * @param field field the type of number to use
     * @param secondaryPVProvider PVCoordinates provider of the other object with which we want to find out the extremum
     * approach.
     */
    public FieldExtremumApproachDetector(final Field<T> field, final FieldPVCoordinatesProvider<T> secondaryPVProvider) {
        this(field.getZero().newInstance(DEFAULT_MAXCHECK), field.getZero().newInstance(DEFAULT_THRESHOLD), DEFAULT_MAX_ITER,
             new FieldStopOnIncreasing<>(), secondaryPVProvider);
    }

    /**
     * Constructor.
     * <p>
     * This constructor is to be used if the user wants to change the default behavior of the detector.
     * </p>
     *
     * @param maxCheck Maximum checking interval.
     * @param threshold Convergence threshold (s).
     * @param maxIter Maximum number of iterations in the event time search.
     * @param handler Event handler to call at event occurrences.
     * @param secondaryPVProvider PVCoordinates provider of the other object with which we want to find out the extremum
     * approach.
     *
     * @see FieldEventHandler
     */
    protected FieldExtremumApproachDetector(final T maxCheck, final T threshold, final int maxIter,
                                            final FieldEventHandler<T> handler,
                                            final FieldPVCoordinatesProvider<T> secondaryPVProvider) {
        this(FieldAdaptableInterval.of(maxCheck.getReal()), threshold, maxIter, handler, secondaryPVProvider);
    }

    /**
     * Constructor.
     * <p>
     * This constructor is to be used if the user wants to change the default behavior of the detector.
     * </p>
     *
     * @param maxCheck Maximum checking interval.
     * @param threshold Convergence threshold (s).
     * @param maxIter Maximum number of iterations in the event time search.
     * @param handler Event handler to call at event occurrences.
     * @param secondaryPVProvider PVCoordinates provider of the other object with which we want to find out the extremum
     * approach.
     *
     * @see EventHandler
     */
    protected FieldExtremumApproachDetector(final FieldAdaptableInterval<T> maxCheck, final T threshold, final int maxIter,
                                            final FieldEventHandler<T> handler,
                                            final FieldPVCoordinatesProvider<T> secondaryPVProvider) {
        super(new FieldEventDetectionSettings<>(maxCheck, threshold, maxIter), handler);
        this.secondaryPVProvider = secondaryPVProvider;
    }

    /**
     * Compute the relative PV between primary and secondary objects.
     *
     * @param s Spacecraft state.
     *
     * @return Relative position between primary (=s) and secondaryPVProvider.
     *
     * @deprecated The output type of this method shall be modified in the future to improve code efficiency (though it will
     * still give access to the relative position and velocity)
     */
    @Deprecated
    public FieldPVCoordinates<T> computeDeltaPV(final FieldSpacecraftState<T> s) {
        final FieldVector3D<T> primaryPos = s.getPosition();
        final FieldVector3D<T> primaryVel = s.getPVCoordinates().getVelocity();

        final FieldPVCoordinates<T> secondaryPV  = secondaryPVProvider.getPVCoordinates(s.getDate(), s.getFrame());
        final FieldVector3D<T>      secondaryPos = secondaryPV.getPosition();
        final FieldVector3D<T>      secondaryVel = secondaryPV.getVelocity();

        final FieldVector3D<T> relativePos = secondaryPos.subtract(primaryPos);
        final FieldVector3D<T> relativeVel = secondaryVel.subtract(primaryVel);

        return new FieldPVCoordinates<>(relativePos, relativeVel);
    }

    /**
     * Get the secondary position-velocity provider stored in this instance.
     *
     * @return the secondary position-velocity provider stored in this instance
     */
    public FieldPVCoordinatesProvider<T> getSecondaryPVProvider() {
        return secondaryPVProvider;
    }

    /**
     * The {@code g} is positive when the primary object is getting further away from the secondary object and is negative
     * when it is getting closer to it.
     *
     * @param s the current state information: date, kinematics, attitude
     *
     * @return value of the switching function
     */
    @Override
    public T g(final FieldSpacecraftState<T> s) {
        final FieldPVCoordinates<T> deltaPV = computeDeltaPV(s);
        return FieldVector3D.dotProduct(deltaPV.getPosition(), deltaPV.getVelocity());
    }

    /** {@inheritDoc} */
    @Override
    protected FieldExtremumApproachDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold,
                                                      final int newMaxIter, final FieldEventHandler<T> newHandler) {
        return new FieldExtremumApproachDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler, secondaryPVProvider);
    }
}
