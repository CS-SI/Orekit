/* Copyright 2002-2024 Joseph Reed
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Joseph Reed licenses this file to You under the Apache License, Version 2.0
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
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Finder for beta angle crossing events.
 * <p>Locate events when the beta angle (the angle between the orbit plane and the celestial body)
 * crosses a threshold. The {@link #g(FieldSpacecraftState)} function is negative when the beta angle
 * is above the threshold and positive when the beta angle is below the threshold.</p>
 * <p>The inertial frame provided must have it's origin centered at the satellite's orbit plane. The
 * beta angle is computed as the angle between the celestial body's position in this frame with the
 * satellite's orbital momentum vector.</p>
 * <p>The default implementation behavior is to {@link Action#STOP stop}
 * propagation at the first event date occurrence. This can be changed by calling
 * {@link #withHandler(FieldEventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @param <T> The field type
 * @author Joe Reed
 * @since 12.1
 */
public class FieldBetaAngleDetector<T extends CalculusFieldElement<T>> extends FieldAbstractDetector<FieldBetaAngleDetector<T>, T> {
    /** Beta angle crossing threshold. */
    private final T betaAngleThreshold;
    /** Coordinate provider for the celestial body. */
    private final FieldPVCoordinatesProvider<T> celestialBodyProvider;
    /** Inertial frame in which beta angle is calculated. */
    private final Frame inertialFrame;

    /**Solar beta angle constructor.
     * <p>This method uses the default data context, assigns the sun as the celestial
     * body and uses GCRF as the inertial frame.</p>
     * @param betaAngleThreshold beta angle threshold (radians)
     */
    @DefaultDataContext
    public FieldBetaAngleDetector(final T betaAngleThreshold) {
        this(betaAngleThreshold.getField(), betaAngleThreshold,
             CelestialBodyFactory.getSun().toFieldPVCoordinatesProvider(betaAngleThreshold.getField()),
             FramesFactory.getGCRF());
    }

    /** Class constructor.
     * @param field the field instance
     * @param betaAngleThreshold beta angle threshold (radians)
     * @param celestialBodyProvider coordinate provider for the celestial provider
     * @param inertialFrame inertial frame in which to compute the beta angle
     */
    public FieldBetaAngleDetector(final Field<T> field, final T betaAngleThreshold,
            final FieldPVCoordinatesProvider<T> celestialBodyProvider,
            final Frame inertialFrame) {
        this(new FieldEventDetectionSettings<>(field, EventDetectionSettings.getDefaultEventDetectionSettings()),
                new FieldStopOnEvent<>(), betaAngleThreshold, celestialBodyProvider, inertialFrame);
    }

    /** Protected constructor with full parameters.
     * <p>This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.</p>
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param betaAngleThreshold beta angle threshold (radians)
     * @param celestialBodyProvider coordinate provider for the celestial provider
     * @param inertialFrame inertial frame in which to compute the beta angle
     * @since 13.0
     */
    protected FieldBetaAngleDetector(final FieldEventDetectionSettings<T> detectionSettings, final FieldEventHandler<T> handler,
                             final T betaAngleThreshold, final FieldPVCoordinatesProvider<T> celestialBodyProvider,
                             final Frame inertialFrame) {
        super(detectionSettings, handler);
        this.betaAngleThreshold = betaAngleThreshold;
        this.celestialBodyProvider = celestialBodyProvider;
        this.inertialFrame = inertialFrame;
    }

    /** Coordinate provider for the celestial body.
     * @return celestial body's coordinate provider
     */
    public FieldPVCoordinatesProvider<T> getCelestialBodyProvider() {
        return this.celestialBodyProvider;
    }

    /** The inertial frame in which beta angle is computed.
     * @return the inertial frame
     */
    public Frame getInertialFrame() {
        return this.inertialFrame;
    }

    /** The beta angle threshold (radians).
     * @return the beta angle threshold (radians)
     */
    public T getBetaAngleThreshold() {
        return this.betaAngleThreshold;
    }

    /** Create a new instance with the provided coordinate provider.
     * <p>This method does not change the current instance.</p>
     * @param newProvider the new coordinate provider
     * @return the new detector instance
     */
    public FieldBetaAngleDetector<T> withCelestialProvider(final FieldPVCoordinatesProvider<T> newProvider) {
        return new FieldBetaAngleDetector<>(getDetectionSettings(),
                getHandler(), getBetaAngleThreshold(), newProvider, getInertialFrame());
    }

    /** Create a new instance with the provided beta angle threshold.
     * <p>This method does not change the current instance.</p>
     * @param newBetaAngleThreshold the beta angle threshold
     * @return the new detector instance
     */
    public FieldBetaAngleDetector<T> withBetaThreshold(final T newBetaAngleThreshold) {
        return new FieldBetaAngleDetector<>(getDetectionSettings(),
                getHandler(), newBetaAngleThreshold, getCelestialBodyProvider(), getInertialFrame());
    }

    /** Create a new instance with the provided inertial frame.
     * <p>This method does not change the current instance.</p>
     * @param newFrame the inertial frame
     * @return the new detector instance
     */
    public FieldBetaAngleDetector<T> withInertialFrame(final Frame newFrame) {
        return new FieldBetaAngleDetector<>(getDetectionSettings(),
                getHandler(), getBetaAngleThreshold(), getCelestialBodyProvider(), newFrame);
    }

    /** {@inheritDoc} */
    @Override
    public T g(final FieldSpacecraftState<T> s) {
        final T beta = calculateBetaAngle(s, celestialBodyProvider, inertialFrame);
        return betaAngleThreshold.subtract(beta);
    }

    /**Calculate the beta angle between the orbit plane and the celestial body.
     * <p>This method computes the beta angle using the frame from the spacecraft state.</p>
     * @param state spacecraft state
     * @param celestialBodyProvider celestial body coordinate provider
     * @param <T> The field type
     * @return the beta angle (radians)
     */
    public static <T extends CalculusFieldElement<T>> T calculateBetaAngle(final FieldSpacecraftState<T> state,
            final FieldPVCoordinatesProvider<T> celestialBodyProvider) {
        return calculateBetaAngle(state, celestialBodyProvider, state.getFrame());
    }

    /**Calculate the beta angle between the orbit plane and the celestial body.
     * @param state spacecraft state
     * @param celestialBodyProvider celestial body coordinate provider
     * @param frame inertial frame in which beta angle will be computed
     * @param <T> The field type
     * @return the beta angle (radians)
     */
    public static <T extends CalculusFieldElement<T>> T calculateBetaAngle(final FieldSpacecraftState<T> state,
            final FieldPVCoordinatesProvider<T> celestialBodyProvider, final Frame frame) {
        final FieldVector3D<T> celestialP = celestialBodyProvider.getPosition(state.getDate(), frame);
        final TimeStampedFieldPVCoordinates<T> pv = state.getPVCoordinates(frame);
        return FieldVector3D.angle(celestialP, pv.getMomentum()).negate().add(MathUtils.SEMI_PI);
    }

    /** {@inheritDoc} */
    @Override
    protected FieldBetaAngleDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                               final FieldEventHandler<T> newHandler) {
        return new FieldBetaAngleDetector<>(detectionSettings, newHandler,
                getBetaAngleThreshold(), getCelestialBodyProvider(), getInertialFrame());
    }
}
