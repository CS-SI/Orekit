/* Copyright 2023-2024 Alberto Ferrero
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Alberto Ferrero licenses this file to You under the Apache License, Version 2.0
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
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnIncreasing;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;


/** Detector for geographic latitude crossing.
 * <p>This detector identifies when a spacecraft crosses a fixed
 * latitude range with respect to a central body.</p>
 * @author Alberto Ferrero
 * @since 12.0
 * @param <T> type of the field elements
 */
public class FieldLatitudeRangeCrossingDetector <T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldLatitudeRangeCrossingDetector<T>, T> {

    /**
     * Body on which the latitude is defined.
     */
    private final OneAxisEllipsoid body;

    /**
     * Fixed latitude to be crossed, lower boundary in radians.
     */
    private final double fromLatitude;

    /**
     * Fixed latitude to be crossed, upper boundary in radians.
     */
    private final double toLatitude;

    /**
     * Sign, to get reversed inclusion latitude range (lower > upper).
     */
    private final double sign;

    /**
     * Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAX_CHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param field        the type of numbers to use.
     * @param body         body on which the latitude is defined
     * @param fromLatitude latitude to be crossed, lower range boundary
     * @param toLatitude   latitude to be crossed, upper range boundary
     */
    public FieldLatitudeRangeCrossingDetector(final Field<T> field,
                                              final OneAxisEllipsoid body,
                                              final double fromLatitude,
                                              final double toLatitude) {
        this(new FieldEventDetectionSettings<>(field, EventDetectionSettings.getDefaultEventDetectionSettings()),
            new FieldStopOnIncreasing<>(),
            body,
            fromLatitude,
            toLatitude);
    }

    /**
     * Build a detector.
     *
     * @param maxCheck     maximal checking interval (s)
     * @param threshold    convergence threshold (s)
     * @param body         body on which the latitude is defined
     * @param fromLatitude latitude to be crossed, lower range boundary
     * @param toLatitude   latitude to be crossed, upper range boundary
     */
    public FieldLatitudeRangeCrossingDetector(final T maxCheck, final T threshold,
                                              final OneAxisEllipsoid body, final double fromLatitude, final double toLatitude) {
        this(new FieldEventDetectionSettings<>(FieldAdaptableInterval.of(maxCheck.getReal()), threshold, DEFAULT_MAX_ITER),
                new FieldStopOnIncreasing<>(), body, fromLatitude, toLatitude);
    }

    /**
     * Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     *
     * @param detectionSettings event detection settings
     * @param handler      event handler to call at event occurrences
     * @param body         body on which the latitude is defined
     * @param fromLatitude latitude to be crossed, lower range boundary
     * @param toLatitude   latitude to be crossed, upper range boundary
     * @since 13.0
     */
    protected FieldLatitudeRangeCrossingDetector(final FieldEventDetectionSettings<T> detectionSettings,
                                                 final FieldEventHandler<T> handler,
                                                 final OneAxisEllipsoid body,
                                                 final double fromLatitude,
                                                 final double toLatitude) {
        super(detectionSettings, handler);
        this.body = body;
        this.fromLatitude = fromLatitude;
        this.toLatitude = toLatitude;
        this.sign = FastMath.signum(toLatitude - fromLatitude);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FieldLatitudeRangeCrossingDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                                           final FieldEventHandler<T> newHandler) {
        return new FieldLatitudeRangeCrossingDetector<>(detectionSettings, newHandler,
            body, fromLatitude, toLatitude);
    }

    /**
     * Get the body on which the geographic zone is defined.
     *
     * @return body on which the geographic zone is defined
     */
    public OneAxisEllipsoid getBody() {
        return body;
    }

    /**
     * Get the fixed latitude range to be crossed (radians), lower boundary.
     *
     * @return fixed lower boundary latitude range to be crossed (radians)
     */
    public double getFromLatitude() {
        return fromLatitude;
    }

    /**
     * Get the fixed latitude range to be crossed (radians), upper boundary.
     *
     * @return fixed lower boundary latitude range to be crossed (radians)
     */
    public double getToLatitude() {
        return toLatitude;
    }

    /**
     * Compute the value of the detection function.
     * <p>
     * The value is positive if the spacecraft latitude is inside the latitude range.
     * It is positive if the spacecraft is northward to lower boundary range and southward to upper boundary range,
     * with respect to the fixed latitude range.
     * </p>
     *
     * @param s the current state information: date, kinematics, attitude
     * @return positive if spacecraft inside the range
     */
    public T g(final FieldSpacecraftState<T> s) {

        // convert state to geodetic coordinates
        final FieldGeodeticPoint<T> gp = body.transform(s.getPVCoordinates().getPosition(),
            s.getFrame(), s.getDate());

        // point latitude
        final T latitude = gp.getLatitude();

        // inside or outside latitude range
        return latitude.subtract(fromLatitude).multiply(latitude.negate().add(toLatitude)).multiply(sign);

    }

}
