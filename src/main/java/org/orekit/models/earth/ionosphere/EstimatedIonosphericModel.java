/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.RealFieldElement;
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

    /** Serializable UID. */
    private static final long serialVersionUID = 20200304L;

    /** Mean Earth radius [km]. */
    private static final double RE = 6371.0;

    /** Meters to kilometers factor. */
    private static final double M_TO_KM = 0.001;

    /** Ionospheric delay factor. */
    private static final double FACTOR = 40.3e16;

    /** Driver for the Vertical Total Electron Content.*/
    private final ParameterDriver vtec;

    /** Ionospheric single layer in kilometers.*/
    private final double hIon;

    /**
     * Build a new instance.
     * @param vtecValue value of the Vertical Total Electron Content in TECUnits
     * @param height height of the ionospheric single layer in meters
     */
    public EstimatedIonosphericModel(final double vtecValue, final double height) {
        this.vtec = new ParameterDriver(EstimatedIonosphericModel.VERTICAL_TOTAL_ELECTRON_CONTENT,
                                        vtecValue, FastMath.scalb(1.0, 3), 0.0, 1000.0);
        // Convert meters to kilometers
        this.hIon = height * M_TO_KM;
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final SpacecraftState state, final TopocentricFrame baseFrame,
                            final double frequency, final double[] parameters) {
        // Elevation in radians
        final Vector3D position  = state.getPVCoordinates(baseFrame).getPosition();
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
        final double fz = mappingFunction(elevation);
        // "Slant" Total Electron Content
        final double stec = parameters[0] * fz;
        // Delay computation
        final double alpha  = FACTOR / freq2;
        return alpha * stec;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T pathDelay(final FieldSpacecraftState<T> state, final TopocentricFrame baseFrame,
                                                       final double frequency, final T[] parameters) {
        // Elevation and azimuth in radians
        final FieldVector3D<T> position = state.getPVCoordinates(baseFrame).getPosition();
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
     * @param parameters ionospheric model parameters
     * @return the path delay due to the ionosphere in m
     */
    public <T extends RealFieldElement<T>> T pathDelay(final T elevation, final double frequency, final T[] parameters) {
        // Square of the frequency
        final double freq2 = frequency * frequency;
        // Mapping factor
        final T fz = mappingFunction(elevation);
        // "Slant" Total Electron Content
        final T stec = parameters[0].multiply(fz);
        // Delay computation
        final double alpha  = FACTOR / freq2;
        return stec.multiply(alpha);
    }

    /**
     * Computes the ionospheric mapping function.
     * @param elevation the elevation of the satellite in radians
     * @return the mapping function
     */
    public double mappingFunction(final double elevation) {
        // Calculate the zenith angle from the elevation
        final double z = FastMath.abs(0.5 * FastMath.PI - elevation);
        // Distance ratio
        final double ratio = RE / (RE + hIon);
        // Mapping function
        final double coef = FastMath.sin(z) * ratio;
        return 1.0 / FastMath.sqrt(1.0 - coef * coef);
    }

    /**
     * Computes the ionospheric mapping function.
     * @param <T> type of the elements
     * @param elevation the elevation of the satellite in radians
     * @return the mapping function
     */
    public <T extends RealFieldElement<T>> T mappingFunction(final T elevation) {
        // Calculate the zenith angle from the elevation
        final T z = FastMath.abs(elevation.negate().add(0.5 * FastMath.PI));
        // Distance ratio
        final double ratio = RE / (RE + hIon);
        // Mapping function
        final T coef = FastMath.sin(z).multiply(ratio);
        return FastMath.sqrt(coef.multiply(coef).negate().add(1.0)).reciprocal();
    }

    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(vtec);
    }
}
