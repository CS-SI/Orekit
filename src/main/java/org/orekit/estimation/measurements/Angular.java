/* Copyright 2002-2016 CS Systèmes d'Information
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

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.GroundStation.OffsetDerivatives;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;

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
              station.getZenithOffsetDriver());
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

        // take propagation time into account
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)
        final Vector3D        stationP     = station.getOffsetFrame().getPVCoordinates(getDate(), state.getFrame()).getPosition();
        final double          tauD         = station.signalTimeOfFlight(state.getPVCoordinates(), stationP, getDate());
        final double          delta        = getDate().durationFrom(state.getDate());
        final double          dt           = delta - tauD;
        final SpacecraftState transitState = state.shiftedBy(dt);

        // transformation from inertial frame to station parent frame
        final Frame     bodyFrame = station.getOffsetFrame().getParentShape().getBodyFrame();
        final Transform iner2Body = state.getFrame().getTransformTo(bodyFrame, getDate());

        // station topocentric frame (east-north-zenith) in station parent frame expressed as DerivativeStructures
        final OffsetDerivatives od = station.getOffsetDerivatives(6, 3, 4, 5);
        final FieldVector3D<DerivativeStructure> east   = od.getEast();
        final FieldVector3D<DerivativeStructure> north  = od.getNorth();
        final FieldVector3D<DerivativeStructure> zenith = od.getZenith();

        // station origin in station parent frame
        final FieldVector3D<DerivativeStructure> qP = od.getOrigin();

        // satellite vector expressed in station parent frame
        final Vector3D transitp = iner2Body.transformPosition(transitState.getPVCoordinates().getPosition());

        // satellite vector expressed in station parent frame expressed as DerivativeStructures
        final FieldVector3D<DerivativeStructure> pP = new FieldVector3D<DerivativeStructure>(new DerivativeStructure(6, 1, 0, transitp.getX()),
                                                                                             new DerivativeStructure(6, 1, 1, transitp.getY()),
                                                                                             new DerivativeStructure(6, 1, 2, transitp.getZ()));
        // station-satellite vector expressed in station parent frame
        final FieldVector3D<DerivativeStructure> staSat = pP.subtract(qP);

        final DerivativeStructure baseAzimuth = DerivativeStructure.atan2(staSat.dotProduct(east), staSat.dotProduct(north));
        final double              twoPiWrap   = MathUtils.normalizeAngle(baseAzimuth.getReal(), getObservedValue()[0]) -
                                                baseAzimuth.getReal();
        final DerivativeStructure azimuth     = baseAzimuth.add(twoPiWrap);
        final DerivativeStructure elevation   = staSat.dotProduct(zenith).divide(staSat.getNorm()).asin();

        // prepare the estimation
        final EstimatedMeasurement<Angular> estimated =
                        new EstimatedMeasurement<Angular>(this, iteration, evaluation, transitState);

        // azimuth - elevation values
        estimated.setEstimatedValue(azimuth.getValue(), elevation.getValue());

        // partial derivatives of azimuth with respect to state
        final AngularCoordinates ac = iner2Body.getInverse().getAngular();

        // partial derivatives of azimuth with respect to state expressed in station parent frame
        final Vector3D tto  = new Vector3D(azimuth.getPartialDerivative(1, 0, 0, 0, 0, 0),
                                           azimuth.getPartialDerivative(0, 1, 0, 0, 0, 0),
                                           azimuth.getPartialDerivative(0, 0, 1, 0, 0, 0));

        // partial derivatives of azimuth with respect to state expressed in satellite inertial frame
        final Vector3D dAzOndPtmp = ac.getRotation().applyTo(tto);
        final double[] dAzOndP = new double[] {
                                               dAzOndPtmp.getX(),
                                               dAzOndPtmp.getY(),
                                               dAzOndPtmp.getZ(),
                                               dAzOndPtmp.getX() * dt,
                                               dAzOndPtmp.getY() * dt,
                                               dAzOndPtmp.getZ() * dt
        };

        // partial derivatives of Elevation with respect to state expressed in station parent frame
        final Vector3D ttu  = new Vector3D(elevation.getPartialDerivative(1, 0, 0, 0, 0, 0),
                                           elevation.getPartialDerivative(0, 1, 0, 0, 0, 0),
                                           elevation.getPartialDerivative(0, 0, 1, 0, 0, 0));

        // partial derivatives of elevation with respect to state expressed in satellite inertial frame
        final Vector3D dElOndPtmp = ac.getRotation().applyTo(ttu);
        final double[] dElOndP = new double[] {
                                                dElOndPtmp.getX(),
                                                dElOndPtmp.getY(),
                                                dElOndPtmp.getZ(),
                                                dElOndPtmp.getX() * dt,
                                                dElOndPtmp.getY() * dt,
                                                dElOndPtmp.getZ() * dt
        };

        estimated.setStateDerivatives(dAzOndP, dElOndP);

        if (station.getEastOffsetDriver().isSelected()  ||
            station.getNorthOffsetDriver().isSelected() ||
            station.getZenithOffsetDriver().isSelected()) {

            // partial derivatives with respect to parameters
            // Be aware: east; north and zenith are expressed in station parent frame but the derivatives are expressed
            // with respect to reference station topocentric frame

            if (station.getEastOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(station.getEastOffsetDriver(),
                                                   azimuth.getPartialDerivative(0, 0, 0, 1, 0, 0),
                                                   elevation.getPartialDerivative(0, 0, 0, 1, 0, 0));
            }
            if (station.getNorthOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(station.getNorthOffsetDriver(),
                                                   azimuth.getPartialDerivative(0, 0, 0, 0, 1, 0),
                                                   elevation.getPartialDerivative(0, 0, 0, 0, 1, 0));
            }
            if (station.getZenithOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(station.getZenithOffsetDriver(),
                                                   azimuth.getPartialDerivative(0, 0, 0, 0, 0, 1),
                                                   elevation.getPartialDerivative(0, 0, 0, 0, 0, 1));
            }

        }

        return estimated;
    }

}
