/* Copyright 2022-2025 Luc Maisonobe
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
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.events.FieldEventSlopeFilter;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnIncreasing;

/** Detector for elevation extremum with respect to a ground point.
 * <p>This detector identifies when a spacecraft reaches its
 * extremum elevation with respect to a ground point.</p>
 * <p>
 * As in most cases only the elevation maximum is needed and the
 * minimum is often irrelevant, this detector is often wrapped into
 * an {@link FieldEventSlopeFilter event slope filter} configured with
 * {@link FilterType#TRIGGER_ONLY_DECREASING_EVENTS} (i.e. when the
 * elevation derivative decreases from positive values to negative values,
 * which correspond to a maximum). Setting up this filter saves some computation
 * time as the elevation minimum occurrences are not even looked at. It is
 * however still often necessary to do an additional filtering
 * </p>
 * @param <T> type of the field element
 * @author Luc Maisonobe
 * @since 12.0
 */
public class FieldElevationExtremumDetector<T extends CalculusFieldElement<T>>
    extends FieldAbstractTopocentricDetector<FieldElevationExtremumDetector<T>, T> {

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAX_CHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param field field to which elements belong
     * @param topo topocentric frame centered on ground point
     */
    public FieldElevationExtremumDetector(final Field<T> field, final TopocentricFrame topo) {
        this(field.getZero().newInstance(DEFAULT_MAX_CHECK),
             field.getZero().newInstance(DEFAULT_THRESHOLD),
             topo);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param topo topocentric frame centered on ground point
     */
    public FieldElevationExtremumDetector(final T maxCheck, final T threshold,
                                          final TopocentricFrame topo) {
        this(new FieldEventDetectionSettings<>(maxCheck.getReal(), threshold, DEFAULT_MAX_ITER), new FieldStopOnIncreasing<>(),
             topo);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param topo topocentric frame centered on ground point
     */
    protected FieldElevationExtremumDetector(final FieldEventDetectionSettings<T> detectionSettings,
                                             final FieldEventHandler<T> handler,
                                             final TopocentricFrame topo) {
        super(detectionSettings, handler, topo);
    }

    /** {@inheritDoc} */
    @Override
    protected FieldElevationExtremumDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                                       final FieldEventHandler<T> newHandler) {
        return new FieldElevationExtremumDetector<>(detectionSettings, newHandler, getTopocentricFrame());
    }

    /** Get the elevation value.
     * @param s the current state information: date, kinematics, attitude
     * @return spacecraft elevation
     */
    public T getElevation(final FieldSpacecraftState<T> s) {
        return getTopocentricFrame().getElevation(s.getPosition(), s.getFrame(), s.getDate());
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the spacecraft elevation first time derivative.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return spacecraft elevation first time derivative
     */
    public T g(final FieldSpacecraftState<T> s) {

        // get position, velocity acceleration of spacecraft in topocentric frame
        final FieldStaticTransform<FieldUnivariateDerivative1<T>> inertToTopo = s.getFrame().getStaticTransformTo(getTopocentricFrame(),
                s.getDate().toFUD1Field());
        final FieldVector3D<FieldUnivariateDerivative1<T>> positionInert = s.getPVCoordinates().toUnivariateDerivative1Vector();
        final FieldVector3D<FieldUnivariateDerivative1<T>> posTopo = inertToTopo.transformPosition(positionInert);

        // compute elevation and its first time derivative
        final FieldUnivariateDerivative1<T> elevation = posTopo.getDelta();

        // return elevation first time derivative
        return elevation.getFirstDerivative();

    }

}
