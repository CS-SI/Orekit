/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.generation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;


/** Scheduling predicate taking care of ground location being at night.
 * <p>
 * This predicate is mainly useful for optical measurements
 * (either passive telescope observation of satellites against the stars background
 *  or active satellite laser ranging).
 * </p>
 * @author Luc Maisonobe
 * @since 9.3
 */
public class GroundAtNight implements SchedulingPredicate {

    /** Sun elevation at civil dawn/dusk (6° below horizon). */
    public static final double CIVIL_DAWN_DUSK_ELEVATION = FastMath.toRadians(-6.0);

    /** Sun elevation at nautical dawn/dusk (12° below horizon). */
    public static final double NAUTICAL_DAWN_DUSK_ELEVATION = FastMath.toRadians(-12.0);

    /** Sun elevation at astronomical dawn/dusk (18° below horizon). */
    public static final double ASTRONOMICAL_DAWN_DUSK_ELEVATION = FastMath.toRadians(-18.0);

    /** Ground location from which measurement is performed. */
    private final TopocentricFrame groundLocation;

    /** Provider for Sun position. */
    private final PVCoordinatesProvider sun;

    /** Sun elevation below which we consider night is dark enough. */
    private final double dawnDuskElevation;

    /** Atmospheric Model used for calculations, if defined. */
    private final AtmosphericRefractionModel refractionModel;

    /** Simple constructor.
     * @param groundLocation ground location from which measurement is performed
     * @param sun provider for Sun position
     * @param dawnDuskElevation Sun elevation below which we consider night is dark enough (rad)
     * (typically {@link #ASTRONOMICAL_DAWN_DUSK_ELEVATION})
     * @param refractionModel reference to refraction model (null if refraction should be ignored)
     */
    public GroundAtNight(final TopocentricFrame groundLocation, final PVCoordinatesProvider sun,
                         final double dawnDuskElevation,
                         final AtmosphericRefractionModel refractionModel) {
        this.groundLocation    = groundLocation;
        this.sun               = sun;
        this.dawnDuskElevation = dawnDuskElevation;
        this.refractionModel   = refractionModel;
    }

    /** {@inheritDoc} */
    @Override
    public boolean feasibleMeasurement(final SpacecraftState... states) {

        final AbsoluteDate  date     = states[0].getDate();
        final Frame         frame    = states[0].getFrame();
        final Vector3D      position = sun.getPVCoordinates(date, frame).getPosition();
        final double trueElevation   = groundLocation.getElevation(position, frame, date);

        final double calculatedElevation;
        if (refractionModel != null) {
            calculatedElevation = trueElevation + refractionModel.getRefraction(trueElevation);
        } else {
            calculatedElevation = trueElevation;
        }

        return calculatedElevation <= dawnDuskElevation;

    }

}
