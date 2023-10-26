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
package org.orekit.models.earth.ionosphere;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/**
 * An estimated ionospheric model. The ionospheric delay is computed according to the formula:
 * <p>
 *           40.3
 *    δ =  --------  *  STEC      with, STEC = VTEC * F(elevation)
 *            f²
 * </p>
 * With:
 * <ul>
 * <li>f: The frequency of the signal in Hz.</li>
 * <li>STEC: The Slant Total Electron Content in TECUnits.</li>
 * <li>VTEC: The Vertical Total Electron Content in TECUnits.</li>
 * <li>F(elevation): A mapping function which depends on satellite elevation.</li>
 * </ul>
 * The VTEC is estimated as a {@link ParameterDriver}
 *
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class EstimatedIonosphericModel implements IonosphericModel {

    /** Name of the parameter of this model: the Vertical Total Electron Content. */
    public static final String VERTICAL_TOTAL_ELECTRON_CONTENT = "vertical total electron content";

    /** Ionospheric delay factor. */
    private static final double FACTOR = 40.3e16;

    /** Ionospheric mapping Function model. */
    private final transient IonosphericMappingFunction model;

    /** Driver for the Vertical Total Electron Content.*/
    private final transient ParameterDriver vtec;


    /**
     * Build a new instance.
     * @param model ionospheric mapping function
     * @param vtecValue value of the Vertical Total Electron Content in TECUnits
     */
    public EstimatedIonosphericModel(final IonosphericMappingFunction model, final double vtecValue) {
        this.model = model;
        this.vtec  = new ParameterDriver(EstimatedIonosphericModel.VERTICAL_TOTAL_ELECTRON_CONTENT,
                                         vtecValue, FastMath.scalb(1.0, 3), 0.0, 1000.0);
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final SpacecraftState state, final TopocentricFrame baseFrame,
                            final double frequency, final double[] parameters) {
        // Elevation in radians
        final Vector3D position  = state.getPosition(baseFrame);
        final double   elevation = position.getDelta();

        // Only consider measures above the horizon
        if (elevation > 0.0) {
            // Delay
            return pathDelay(elevation, frequency, parameters);
        }

        return 0.0;
    }

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite.
     * <p>
     * The path delay is computed for any elevation angle.
     * </p>
     * @param elevation elevation of the satellite in radians
     * @param frequency frequency of the signal in Hz
     * @param parameters ionospheric model parameters
     * @return the path delay due to the ionosphere in m
     */
    public double pathDelay(final double elevation, final double frequency, final double[] parameters) {
        // Square of the frequency
        final double freq2 = frequency * frequency;
        // Mapping factor
        final double fz = model.mappingFactor(elevation);
        // "Slant" Total Electron Content
        final double stec = parameters[0] * fz;
        // Delay computation
        final double alpha  = FACTOR / freq2;
        return alpha * stec;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final FieldSpacecraftState<T> state, final TopocentricFrame baseFrame,
                                                       final double frequency, final T[] parameters) {
        // Elevation and azimuth in radians
        final FieldVector3D<T> position = state.getPosition(baseFrame);
        final T elevation = position.getDelta();

        if (elevation.getReal() > 0.0) {
            // Delay
            return pathDelay(elevation, frequency, parameters);
        }

        return elevation.getField().getZero();
    }

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite.
     * <p>
     * The path delay is computed for any elevation angle.
     * </p>
     * @param <T> type of the elements
     * @param elevation elevation of the satellite in radians
     * @param frequency frequency of the signal in Hz
     * @param parameters ionospheric model parameters at state date
     * @return the path delay due to the ionosphere in m
     */
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation, final double frequency, final T[] parameters) {
        // Square of the frequency
        final double freq2 = frequency * frequency;
        // Mapping factor
        final T fz = model.mappingFactor(elevation);
        // "Slant" Total Electron Content
        final T stec = parameters[0].multiply(fz);
        // Delay computation
        final double alpha  = FACTOR / freq2;
        return stec.multiply(alpha);
    }

    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(vtec);
    }

}
