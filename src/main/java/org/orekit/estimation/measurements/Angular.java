/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Class modeling an Azimuth-Elevation measurement from a ground station.
 * The motion of the spacecraft during the signal flight time is taken into
 * account. The date of the measurement corresponds to the reception on
 * ground of the reflected signal.
 *
 * @author Thierry Ceolin
 * @since 8.0
 */
public class Angular extends AbstractMeasurement<Angular> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param angular observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @exception OrekitException if a {@link org.orekit.utils.ParameterDriver}
     * name conflict occurs
     */
    public Angular(final GroundStation station, final AbsoluteDate date,
                   final double[] angular, final double[] sigma, final double[] baseWeight)
        throws OrekitException {
        super(date, angular, sigma, baseWeight,
              station.getEastOffsetDriver(),
              station.getNorthOffsetDriver(),
              station.getZenithOffsetDriver(),
              station.getPrimeMeridianOffsetDriver(),
              station.getPrimeMeridianDriftDriver(),
              station.getPolarOffsetXDriver(),
              station.getPolarDriftXDriver(),
              station.getPolarOffsetYDriver(),
              station.getPolarDriftYDriver());
        this.station = station;
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Angular> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                  final SpacecraftState state)
        throws OrekitException {

        // get the number of parameters used for derivation
        int nbParams = 3;
        final int primeMeridianOffsetIndex;
        if (station.getPrimeMeridianOffsetDriver().isSelected()) {
            primeMeridianOffsetIndex = nbParams++;
        } else {
            primeMeridianOffsetIndex = -1;
        }
        final int primeMeridianDriftIndex;
        if (station.getPrimeMeridianDriftDriver().isSelected()) {
            primeMeridianDriftIndex = nbParams++;
        } else {
            primeMeridianDriftIndex = -1;
        }
        final int polarOffsetXIndex;
        if (station.getPolarOffsetXDriver().isSelected()) {
            polarOffsetXIndex = nbParams++;
        } else {
            polarOffsetXIndex = -1;
        }
        final int polarDriftXIndex;
        if (station.getPolarDriftXDriver().isSelected()) {
            polarDriftXIndex = nbParams++;
        } else {
            polarDriftXIndex = -1;
        }
        final int polarOffsetYIndex;
        if (station.getPolarOffsetYDriver().isSelected()) {
            polarOffsetYIndex = nbParams++;
        } else {
            polarOffsetYIndex = -1;
        }
        final int polarDriftYIndex;
        if (station.getPolarDriftYDriver().isSelected()) {
            polarDriftYIndex = nbParams++;
        } else {
            polarDriftYIndex = -1;
        }
        final int eastOffsetIndex;
        if (station.getEastOffsetDriver().isSelected()) {
            eastOffsetIndex = nbParams++;
        } else {
            eastOffsetIndex = -1;
        }
        final int northOffsetIndex;
        if (station.getNorthOffsetDriver().isSelected()) {
            northOffsetIndex = nbParams++;
        } else {
            northOffsetIndex = -1;
        }
        final int zenithOffsetIndex;
        if (station.getZenithOffsetDriver().isSelected()) {
            zenithOffsetIndex = nbParams++;
        } else {
            zenithOffsetIndex = -1;
        }
        final DSFactory                          factory = new DSFactory(nbParams, 1);
        final Field<DerivativeStructure>         field   = factory.getDerivativeField();
        final FieldVector3D<DerivativeStructure> zero    = FieldVector3D.getZero(field);

        // take propagation time into account
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)
        final Vector3D        stationP     = station.getOffsetToInertial(state.getFrame(), getDate()).transformPosition(Vector3D.ZERO);
        final double          tauD         = station.signalTimeOfFlight(state.getPVCoordinates(), stationP, getDate());
        final double          delta        = getDate().durationFrom(state.getDate());
        final double          dt           = delta - tauD;
        final SpacecraftState transitState = state.shiftedBy(dt);

        // transform between station and inertial frame, expressed as a derivative structure
        // The components of station's position in offset frame are the 3 last derivative parameters
        final AbsoluteDate downlinkDate = getDate();
        final FieldAbsoluteDate<DerivativeStructure> downlinkDateDS =
                        new FieldAbsoluteDate<>(field, downlinkDate);
        final FieldTransform<DerivativeStructure> offsetToInertialDownlink =
                        station.getOffsetToInertial(state.getFrame(), downlinkDateDS, factory,
                                                    primeMeridianOffsetIndex, primeMeridianDriftIndex,
                                                    polarOffsetXIndex, polarDriftXIndex,
                                                    polarOffsetYIndex, polarDriftYIndex,
                                                    eastOffsetIndex, northOffsetIndex, zenithOffsetIndex);

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationDownlink =
                        offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDateDS,
                                                                                                            zero, zero, zero));
        // station topocentric frame (east-north-zenith) in inertial frame expressed as DerivativeStructures
        final FieldVector3D<DerivativeStructure> east   = offsetToInertialDownlink.transformVector(FieldVector3D.getPlusI(field));
        final FieldVector3D<DerivativeStructure> north  = offsetToInertialDownlink.transformVector(FieldVector3D.getPlusJ(field));
        final FieldVector3D<DerivativeStructure> zenith = offsetToInertialDownlink.transformVector(FieldVector3D.getPlusK(field));

        // station origin in inertial frame
        final FieldVector3D<DerivativeStructure> qP = stationDownlink.getPosition();

        // satellite vector expressed in inertial frame
        final Vector3D transitp = transitState.getPVCoordinates().getPosition();

        // satellite vector expressed in inertial frame expressed as DerivativeStructures
        final FieldVector3D<DerivativeStructure> pP = new FieldVector3D<>(factory.variable(0, transitp.getX()),
                                                                          factory.variable(1, transitp.getY()),
                                                                          factory.variable(2, transitp.getZ()));
        // station-satellite vector expressed in inertial frame
        final FieldVector3D<DerivativeStructure> staSat = pP.subtract(qP);

        final DerivativeStructure baseAzimuth = DerivativeStructure.atan2(staSat.dotProduct(east), staSat.dotProduct(north));
        final double              twoPiWrap   = MathUtils.normalizeAngle(baseAzimuth.getReal(), getObservedValue()[0]) -
                                                baseAzimuth.getReal();
        final DerivativeStructure azimuth     = baseAzimuth.add(twoPiWrap);
        final DerivativeStructure elevation   = staSat.dotProduct(zenith).divide(staSat.getNorm()).asin();

        // prepare the estimation
        final EstimatedMeasurement<Angular> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation, transitState);

        // azimuth - elevation values
        estimated.setEstimatedValue(azimuth.getValue(), elevation.getValue());

        // partial derivatives of azimuth with respect to state
        final double[] azDerivatives = azimuth.getAllDerivatives();
        final double[] dAzOndP = new double[] {
            azDerivatives[1],      azDerivatives[2],      azDerivatives[3],
            azDerivatives[1] * dt, azDerivatives[2] * dt, azDerivatives[3] * dt
        };

        // partial derivatives of Elevation with respect to state
        final double[] elDerivatives = elevation.getAllDerivatives();
        final double[] dElOndP = new double[] {
            elDerivatives[1],      elDerivatives[2],      elDerivatives[3],
            elDerivatives[1] * dt, elDerivatives[2] * dt, elDerivatives[3] * dt
        };
        estimated.setStateDerivatives(dAzOndP, dElOndP);

        // set partial derivatives with respect to parameters
        setDerivatives(estimated, station.getPrimeMeridianOffsetDriver(), primeMeridianOffsetIndex, azDerivatives, elDerivatives);
        setDerivatives(estimated, station.getPrimeMeridianDriftDriver(),  primeMeridianDriftIndex,  azDerivatives, elDerivatives);
        setDerivatives(estimated, station.getPolarOffsetXDriver(),        polarOffsetXIndex,        azDerivatives, elDerivatives);
        setDerivatives(estimated, station.getPolarDriftXDriver(),         polarDriftXIndex,         azDerivatives, elDerivatives);
        setDerivatives(estimated, station.getPolarOffsetYDriver(),        polarOffsetYIndex,        azDerivatives, elDerivatives);
        setDerivatives(estimated, station.getPolarDriftYDriver(),         polarDriftYIndex,         azDerivatives, elDerivatives);
        setDerivatives(estimated, station.getEastOffsetDriver(),          eastOffsetIndex,          azDerivatives, elDerivatives);
        setDerivatives(estimated, station.getNorthOffsetDriver(),         northOffsetIndex,         azDerivatives, elDerivatives);
        setDerivatives(estimated, station.getZenithOffsetDriver(),        zenithOffsetIndex,        azDerivatives, elDerivatives);

        return estimated;

    }

    /** Set derivatives with resptect to parameters.
     * @param estimated estimated measurement
     * @param driver parameter driver
     * @param index index of the parameter in the set of
     * free parameters in derivatives computations (negative if not used)
     * @param azDerivatives azimuth derivatives (beware element at index 0 is the value, not a derivative)
     * @param elDerivatives elevation derivatives (beware element at index 0 is the value, not a derivative)
     */
    private void setDerivatives(final EstimatedMeasurement<Angular> estimated,
                                final ParameterDriver driver, final int index,
                                final double[] azDerivatives, final double[] elDerivatives) {
        if (index >= 0) {
            estimated.setParameterDerivatives(driver, azDerivatives[index + 1], elDerivatives[index + 1]);
        }
    }

}
