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
package org.orekit.estimation.measurements.generation;

import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.PV;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** Builder for {@link PV} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class PVBuilder extends AbstractMeasurementBuilder<PV> {

    /** Satellite related to this builder.
     * @since 12.0
     */
    private final ObservableSatellite satellite;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param sigmaPosition theoretical standard deviation on position components
     * @param sigmaVelocity theoretical standard deviation on velocity components
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public PVBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                     final double sigmaPosition, final double sigmaVelocity,
                     final double baseWeight, final ObservableSatellite satellite) {
        super(noiseSource,
              new double[] {
                  sigmaPosition, sigmaVelocity
              }, new double[] {
                  baseWeight
              }, satellite);
        this.satellite = satellite;
    }

    /** {@inheritDoc} */
    @Override
    public PV build(final AbsoluteDate date, final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {

        final double[] sigma                = getTheoreticalStandardDeviation();
        final double baseWeight             = getBaseWeight()[0];
        final SpacecraftState[] relevant    = new SpacecraftState[] { interpolators.get(satellite).getInterpolatedState(date) };

        // create a dummy measurement
        final PV dummy = new PV(relevant[0].getDate(), Vector3D.NaN, Vector3D.NaN,
                                sigma[0], sigma[1], baseWeight, satellite);
        for (final EstimationModifier<PV> modifier : getModifiers()) {
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
        final double[] pv = dummy.estimateWithoutDerivatives(0, 0, relevant).getEstimatedValue();

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            pv[0] += noise[0];
            pv[1] += noise[1];
            pv[2] += noise[2];
            pv[3] += noise[3];
            pv[4] += noise[4];
            pv[5] += noise[5];
        }

        // generate measurement
        final PV measurement = new PV(relevant[0].getDate(),
                                      new Vector3D(pv[0], pv[1], pv[2]), new Vector3D(pv[3], pv[4], pv[5]),
                                      sigma[0], sigma[1], baseWeight, satellite);
        for (final EstimationModifier<PV> modifier : getModifiers()) {
            measurement.addModifier(modifier);
        }
        return measurement;

    }

}
