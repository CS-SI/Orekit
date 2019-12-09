/* Copyright 2002-2019 CS Systèmes d'Information
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
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Position;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** Builder for {@link Position} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class PositionBuilder extends AbstractMeasurementBuilder<Position> {

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public PositionBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                           final double sigma, final double baseWeight,
                           final ObservableSatellite satellite) {
        super(noiseSource, sigma, baseWeight, satellite);
    }

    /** {@inheritDoc} */
    @Override
    public Position build(final SpacecraftState[] states) {

        final ObservableSatellite satellite = getSatellites()[0];
        final double sigma                  = getTheoreticalStandardDeviation()[0];
        final double baseWeight             = getBaseWeight()[0];
        final SpacecraftState[] relevant    = new SpacecraftState[] { states[satellite.getPropagatorIndex()] };

        // create a dummy measurement
        final Position dummy = new Position(relevant[0].getDate(), Vector3D.NaN, sigma, baseWeight, satellite);
        for (final EstimationModifier<Position> modifier : getModifiers()) {
            dummy.addModifier(modifier);
        }

        // set a reference date for parameters missing one
        for (final ParameterDriver driver : dummy.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                final AbsoluteDate start = getStart();
                final AbsoluteDate end   = getEnd();
                driver.setReferenceDate(start.durationFrom(end) <= 0 ? start : end);
            }
        }

        // estimate the perfect value of the measurement
        final double[] position = dummy.estimate(0, 0, relevant).getEstimatedValue();

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            position[0] += noise[0];
            position[1] += noise[1];
            position[2] += noise[2];
        }

        // generate measurement
        final Position measurement = new Position(relevant[0].getDate(), new Vector3D(position),
                                                  sigma, baseWeight, satellite);
        for (final EstimationModifier<Position> modifier : getModifiers()) {
            measurement.addModifier(modifier);
        }
        return measurement;

    }

}
