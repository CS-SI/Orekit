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

import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ElevationMask;


/** Scheduling predicate taking care of visibility from a ground location.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class VisibleFromGround implements SchedulingPredicate {

    /** Ground location from which measurement is performed. */
    private final TopocentricFrame groundLocation;

    /** Elevation mask used for calculations, if defined. */
    private final ElevationMask elevationMask;

    /** Minimum elevation value used if mask is not defined. */
    private final double minElevation;

    /** Atmospheric Model used for calculations, if defined. */
    private final AtmosphericRefractionModel refractionModel;

    /** Index of the propagator related to this predicate. */
    private final int propagatorIndex;

    /** Simple constructor.
     * @param groundLocation ground location from which measurement is performed
     * @param minElevation minimum elevation in radians (rad)
     * @param refractionModel reference to refraction model (null if refraction should be ignored)
     * @param propagatorIndex index of the propagator related to this predicate
     */
    public VisibleFromGround(final TopocentricFrame groundLocation,
                            final double minElevation, final AtmosphericRefractionModel refractionModel,
                            final int propagatorIndex) {
        this.groundLocation  = groundLocation;
        this.elevationMask   = null;
        this.minElevation    = minElevation;
        this.refractionModel = refractionModel;
        this.propagatorIndex = propagatorIndex;
    }

    /** Simple constructor.
     * @param groundLocation ground location from which measurement is performed
     * @param mask reference to elevation mask
     * @param refractionModel reference to refraction model (null if refraction should be ignored)
     * @param propagatorIndex index of the propagator related to this predicate
     */
    public VisibleFromGround(final TopocentricFrame groundLocation,
                            final ElevationMask mask, final AtmosphericRefractionModel refractionModel,
                            final int propagatorIndex) {
        this.groundLocation  = groundLocation;
        this.elevationMask   = mask;
        this.minElevation    = Double.NaN;
        this.refractionModel = refractionModel;
        this.propagatorIndex = propagatorIndex;
    }

    /** {@inheritDoc} */
    @Override
    public boolean feasibleMeasurement(final SpacecraftState... states) {
        final SpacecraftState s = states[propagatorIndex];
        final double trueElevation = groundLocation.getElevation(s.getPVCoordinates().getPosition(),
                                                                 s.getFrame(), s.getDate());

        final double calculatedElevation;
        if (refractionModel != null) {
            calculatedElevation = trueElevation + refractionModel.getRefraction(trueElevation);
        } else {
            calculatedElevation = trueElevation;
        }

        if (elevationMask != null) {
            final double azimuth = groundLocation.getAzimuth(s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate());
            return calculatedElevation >= elevationMask.getElevation(azimuth);
        } else {
            return calculatedElevation >= minElevation;
        }
    }

}
