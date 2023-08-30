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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;


/** Detector for ground location being at night.
 * <p>
 * This detector is mainly useful for scheduling optical measurements
 * (either passive telescope observation of satellites against the stars background
 *  or active satellite laser ranging).
 * </p>
 * <p>
 * The {@code g} function of this detector is positive when ground is at night
 * (i.e. Sun is below dawn/dusk elevation angle).
 * </p>
 * @author Luc Maisonobe
 * @since 9.3
 */
public class GroundAtNightDetector extends AbstractDetector<GroundAtNightDetector> {

    /** Sun elevation at civil dawn/dusk (6° below horizon). */
    public static final double CIVIL_DAWN_DUSK_ELEVATION = FastMath.toRadians(-6.0);

    /** Sun elevation at nautical dawn/dusk (12° below horizon). */
    public static final double NAUTICAL_DAWN_DUSK_ELEVATION = FastMath.toRadians(-12.0);

    /** Sun elevation at astronomical dawn/dusk (18° below horizon). */
    public static final double ASTRONOMICAL_DAWN_DUSK_ELEVATION = FastMath.toRadians(-18.0);

    /** Ground location to check. */
    private final TopocentricFrame groundLocation;

    /** Provider for Sun position. */
    private final PVCoordinatesProvider sun;

    /** Sun elevation below which we consider night is dark enough. */
    private final double dawnDuskElevation;

    /** Atmospheric Model used for calculations, if defined. */
    private final AtmosphericRefractionModel refractionModel;

    /** Simple constructor.
     * <p>
     * Beware that {@link org.orekit.models.earth.EarthStandardAtmosphereRefraction Earth
     * standard refraction model} does apply only for elevations above -2°. It is therefore
     * not suitable for used with {@link #CIVIL_DAWN_DUSK_ELEVATION} (-6°), {@link
     * #NAUTICAL_DAWN_DUSK_ELEVATION} (-12°) or {@link #ASTRONOMICAL_DAWN_DUSK_ELEVATION} (-18°).
     * The {@link org.orekit.models.earth.EarthITU453AtmosphereRefraction ITU 453 refraction model}
     * which can compute refraction at large negative elevations should be preferred.
     * </p>
     * @param groundLocation ground location to check
     * @param sun provider for Sun position
     * @param dawnDuskElevation Sun elevation below which we consider night is dark enough (rad)
     * (typically {@link #ASTRONOMICAL_DAWN_DUSK_ELEVATION})
     * @param refractionModel reference to refraction model (null if refraction should be ignored)
     */
    public GroundAtNightDetector(final TopocentricFrame groundLocation, final PVCoordinatesProvider sun,
                                 final double dawnDuskElevation,
                                 final AtmosphericRefractionModel refractionModel) {
        this(groundLocation, sun, dawnDuskElevation, refractionModel,
             s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
             new ContinueOnEvent());
    }

    /** Private constructor.
     * @param groundLocation ground location from which measurement is performed
     * @param sun provider for Sun position
     * @param dawnDuskElevation Sun elevation below which we consider night is dark enough (rad)
     * (typically {@link #ASTRONOMICAL_DAWN_DUSK_ELEVATION})
     * @param refractionModel reference to refraction model (null if refraction should be ignored),
     * @param maxCheck  maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter   maximum number of iterations in the event time search
     * @param handler   event handler to call at event occurrences
     */
    protected GroundAtNightDetector(final TopocentricFrame groundLocation, final PVCoordinatesProvider sun,
                                    final double dawnDuskElevation,
                                    final AtmosphericRefractionModel refractionModel,
                                    final AdaptableInterval maxCheck,
                                    final double threshold,
                                    final int maxIter,
                                    final EventHandler handler) {
        super(maxCheck, threshold, maxIter, handler);
        this.groundLocation    = groundLocation;
        this.sun               = sun;
        this.dawnDuskElevation = dawnDuskElevation;
        this.refractionModel   = refractionModel;
    }

    /** {@inheritDoc} */
    @Override
    protected GroundAtNightDetector create(final AdaptableInterval newMaxCheck,
                                           final double newThreshold,
                                           final int newMaxIter,
                                           final EventHandler newHandler) {
        return new GroundAtNightDetector(groundLocation, sun, dawnDuskElevation, refractionModel,
                                         newMaxCheck, newThreshold, newMaxIter, newHandler);
    }

    /** {@inheritDoc}
     * <p>
     * The {@code g} function of this detector is positive when ground is at night
     * (i.e. Sun is below dawn/dusk elevation angle).
     * </p>
     * <p>
     * This function only depends on date, not on the actual position of the spacecraft.
     * </p>
     */
    @Override
    public double g(final SpacecraftState state) {

        final AbsoluteDate  date     = state.getDate();
        final Frame         frame    = state.getFrame();
        final Vector3D      position = sun.getPosition(date, frame);
        final double trueElevation   = groundLocation.getElevation(position, frame, date);

        final double calculatedElevation;
        if (refractionModel != null) {
            calculatedElevation = trueElevation + refractionModel.getRefraction(trueElevation);
        } else {
            calculatedElevation = trueElevation;
        }

        return dawnDuskElevation - calculatedElevation;

    }

}
