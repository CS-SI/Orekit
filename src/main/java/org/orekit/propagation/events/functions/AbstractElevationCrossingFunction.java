/* Copyright 2022-2026 Romain Serra
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
package org.orekit.propagation.events.functions;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;


/**
 * Abstract class for elevation crossing event function.
 * It is negative when under the necessary elevation.
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractElevationCrossingFunction extends AbstractTopocentricEventFunction {

    /** Atmospheric Model used for calculations, if defined. */
    private final AtmosphericRefractionModel refractionModel;

    /** Constructor.
     * @param refractionModel reference to refraction model (can be null in which case no correction is applied)
     * @param topo reference to a topocentric model
     */
    protected AbstractElevationCrossingFunction(final AtmosphericRefractionModel refractionModel,
                                                final TopocentricFrame topo) {
        super(topo);
        this.refractionModel = refractionModel;
    }

    /**
     * Returns the currently configured refraction model.
     * @return refraction model
     */
    public AtmosphericRefractionModel getRefractionModel() {
        return this.refractionModel;
    }

    /**
     * Compute elevation.
     * @param topocentricPosition position in topocentric frame
     * @param date date
     * @return elevation
     */
    protected double getElevation(final Vector3D topocentricPosition, final AbsoluteDate date) {
        return getTopocentricFrame().getElevation(topocentricPosition, getTopocentricFrame(), date);
    }

    /**
     * Compute elevation.
     * @param topocentricPosition position in topocentric frame
     * @param date date
     * @param <T> field type
     * @return elevation
     */
    protected <T extends CalculusFieldElement<T>> T getElevation(final FieldVector3D<T> topocentricPosition,
                                                                 final FieldAbsoluteDate<T> date) {
        return getTopocentricFrame().getElevation(topocentricPosition, getTopocentricFrame(), date);
    }

    /** Apply refraction correction if applicable.
     * @param elevation value before correction
     * @return apparent elevation due to refraction
     */
    protected double applyRefraction(final double elevation) {
        if (refractionModel != null) {
            return elevation + refractionModel.getRefraction(elevation);
        } else {
            return elevation;
        }
    }

    /** Apply refraction correction if applicable (Field version).
     * @param elevation value before correction
     * @param <T> field type
     * @return apparent elevation due to refraction
     */
    protected <T extends CalculusFieldElement<T>> T applyRefraction(final T elevation) {
        if (refractionModel != null) {
            return elevation.add(refractionModel.getRefraction(elevation.getReal()));
        } else {
            return elevation;
        }
    }
}
