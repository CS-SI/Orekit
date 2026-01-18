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
package org.orekit.estimation.measurements.model;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.StaticTransform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Perfect measurement model for topocentric azimuth and elevation model.
 * The sensor is the signal receiver and is assumed to be ground-fixed.
 * @since 14.0
 * @author Romain Serra
 */
public class TopocentricAzElModel extends AbstractAngularMeasurementModel {

    /** Body shape where sensor is fixed. */
    private final BodyShape bodyShape;

    /** Inertial frame needed for intermediate computations. */
    private final Frame inertialFrame;

    /**
     * Constructor with default inertial frame.
     * @param bodyShape Earth-fixed frame
     * @param signalTravelTimeModel time delay computer
     */
    @DefaultDataContext
    public TopocentricAzElModel(final BodyShape bodyShape,
                                final SignalTravelTimeModel signalTravelTimeModel) {
        this(FramesFactory.getGCRF(), bodyShape, signalTravelTimeModel);
    }

    /**
     * Constructor.
     * @param inertialFrame inertial frame needed for intermediate computations
     * @param bodyShape Earth-fixed frame
     * @param signalTravelTimeModel time delay computer
     */
    public TopocentricAzElModel(final Frame inertialFrame, final BodyShape bodyShape,
                                final SignalTravelTimeModel signalTravelTimeModel) {
        super(signalTravelTimeModel);
        if (!inertialFrame.isPseudoInertial()) {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, inertialFrame.getName());
        }
        this.inertialFrame = inertialFrame;
        this.bodyShape = bodyShape;
    }

    /**
     * Compute theoretical measurement.
     * @param receiver receiver geodetic coordinates
     * @param receptionDate signal reception date
     * @param emitter signal emitter coordinates provider
     * @return azimuth-elevation (radians)
     */
    public double[] value(final GeodeticPoint receiver, final AbsoluteDate receptionDate,
                          final PVCoordinatesProvider emitter) {
        return value(receiver, receptionDate, emitter, receptionDate);
    }

    /**
     * Compute theoretical measurement with guess for emission date.
     * @param receiver receiver geodetic coordinates
     * @param receptionDate signal reception date
     * @param emitter signal emitter coordinates provider
     * @param approxEmissionDate guess for the emission date (shall be adjusted by signal travel time computer)
     * @return azimuth-elevation (radians)
     */
    public double[] value(final GeodeticPoint receiver, final AbsoluteDate receptionDate,
                          final PVCoordinatesProvider emitter, final AbsoluteDate approxEmissionDate) {
        // Compute line-of-sight
        final Frame bodyFixedFrame = bodyShape.getBodyFrame();
        final Vector3D bodyFixedReceiverPosition = bodyShape.transform(receiver);
        final StaticTransform toInertialFrameAtReception = bodyFixedFrame.getStaticTransformTo(inertialFrame, receptionDate);
        final Vector3D receiverPosition = toInertialFrameAtReception.transformPosition(bodyFixedReceiverPosition);
        final Vector3D apparentLineOfSight = getEmitterToReceiverVector(inertialFrame, receiverPosition, receptionDate,
                emitter, approxEmissionDate).normalize();

        // Compute azimuth and elevation
        final Vector3D east = toInertialFrameAtReception.transformVector(receiver.getEast());
        final Vector3D north = toInertialFrameAtReception.transformVector(receiver.getNorth());
        final Vector3D zenith = toInertialFrameAtReception.transformVector(receiver.getZenith());
        final double azimuth = FastMath.atan2(apparentLineOfSight.dotProduct(east), apparentLineOfSight.dotProduct(north));
        final double elevation = FastMath.asin(apparentLineOfSight.dotProduct(zenith) / apparentLineOfSight.getNorm2());
        return new double[] { azimuth, elevation };
    }

    /**
     * Compute theoretical measurement in Taylor Differential Algebra.
     * @param receiver receiver geodetic coordinates
     * @param receptionDate signal reception date
     * @param emitter signal emitter coordinates provider
     * @return azimuth-elevation (radians)
     */
    public Gradient[] value(final FieldGeodeticPoint<Gradient> receiver,
                            final FieldAbsoluteDate<Gradient> receptionDate,
                            final FieldPVCoordinatesProvider<Gradient> emitter) {
        return value(receiver, receptionDate, emitter, receptionDate);
    }

    /**
     * Compute theoretical measurement in Taylor Differential Algebra with guess for emission date.
     * @param receiver receiver geodetic coordinates
     * @param receptionDate signal reception date
     * @param emitter signal emitter coordinates provider
     * @param approxEmissionDate guess for the emission date (shall be adjusted by signal travel time computer)
     * @return azimuth-elevation (radians)
     */
    public Gradient[] value(final FieldGeodeticPoint<Gradient> receiver,
                            final FieldAbsoluteDate<Gradient> receptionDate,
                            final FieldPVCoordinatesProvider<Gradient> emitter,
                            final FieldAbsoluteDate<Gradient> approxEmissionDate) {
        // Compute line-of-sight
        final Frame bodyFixedFrame = bodyShape.getBodyFrame();
        final FieldVector3D<Gradient> bodyFixedReceiverPosition = bodyShape.transform(receiver);
        final FieldStaticTransform<Gradient> toInertialFrameAtReception = bodyFixedFrame.getStaticTransformTo(inertialFrame,
                receptionDate);
        final FieldVector3D<Gradient> receiverPosition = toInertialFrameAtReception.transformPosition(bodyFixedReceiverPosition);
        final FieldVector3D<Gradient> apparentLineOfSight = getEmitterToReceiverVector(inertialFrame, receiverPosition,
                receptionDate, emitter, approxEmissionDate).normalize();

        // Compute azimuth and elevation
        final FieldVector3D<Gradient> east = toInertialFrameAtReception.transformVector(receiver.getEast());
        final FieldVector3D<Gradient> north = toInertialFrameAtReception.transformVector(receiver.getNorth());
        final FieldVector3D<Gradient> zenith = toInertialFrameAtReception.transformVector(receiver.getZenith());
        final Gradient azimuth = FastMath.atan2(apparentLineOfSight.dotProduct(east),
                apparentLineOfSight.dotProduct(north));
        final Gradient elevation = FastMath.asin(apparentLineOfSight.dotProduct(zenith)
                .divide(apparentLineOfSight.getNorm2()));
        return new Gradient[] { azimuth, elevation };
    }
}
