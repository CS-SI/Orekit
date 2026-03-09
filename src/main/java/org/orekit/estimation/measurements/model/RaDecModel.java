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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Perfect measurement model for right ascension and declination. It is assumed that the signal reception date is known.
 * It is passive in the sense that the sensor did not generate the signal in the first place, it is only collecting it.
 * @since 14.0
 * @author Romain Serra
 */
public class RaDecModel extends AbstractAngularMeasurementModel {

    /** Reference frame defining axis used with right ascension and declination. */
    private final Frame referenceFrame;

    /**
     * Constructor.
     * @param referenceFrame reference frame for RA-Dec (must be inertial)
     * @param signalTravelTimeModel time delay computer
     */
    public RaDecModel(final Frame referenceFrame, final SignalTravelTimeModel signalTravelTimeModel) {
        super(signalTravelTimeModel);
        if (!referenceFrame.isPseudoInertial()) {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, referenceFrame.getName());
        }
        this.referenceFrame = referenceFrame;
    }

    /**
     * Compute theoretical measurement.
     * @param frame frame where receiver position is given
     * @param receiverPosition receiver position in input frame at reception time
     * @param receptionDate signal reception date
     * @param emitter signal emitter coordinates provider
     * @return RA-Dec (radians)
     */
    public double[] value(final Frame frame, final Vector3D receiverPosition, final AbsoluteDate receptionDate,
                          final PVCoordinatesProvider emitter) {
        return value(frame, receiverPosition, receptionDate, emitter, receptionDate);
    }

    /**
     * Compute theoretical measurement with guess for the emission date.
     * @param frame frame where receiver position is given
     * @param receiverPosition receiver position in input frame at reception time
     * @param receptionDate signal reception date
     * @param emitter signal emitter coordinates provider
     * @param approxEmissionDate guess for the emission date (shall be adjusted by signal travel time computer)
     * @return RA-Dec (radians)
     */
    public double[] value(final Frame frame, final Vector3D receiverPosition, final AbsoluteDate receptionDate,
                          final PVCoordinatesProvider emitter, final AbsoluteDate approxEmissionDate) {
        // Compute line-of-sight
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate, receiverPosition,
                frame);
        final Vector3D apparentLineOfSightInInputFrame = getEmitterToReceiverVector(receptionCondition, emitter,
                approxEmissionDate).normalize();
        final StaticTransform toInertialFrameAtReception = frame.getStaticTransformTo(referenceFrame, receptionDate);
        final Vector3D apparentLineOfSight = toInertialFrameAtReception.transformVector(apparentLineOfSightInInputFrame);

        // Compute right ascension and declination
        final double rightAscension = apparentLineOfSight.getAlpha();
        final double declination = apparentLineOfSight.getDelta();
        return new double[] { rightAscension, declination };
    }

    /**
     * Compute theoretical measurement with FIeld.
     * @param <T> field type
     * @param frame frame where receiver position is given
     * @param receiverPosition receiver position in input frame at reception time
     * @param receptionDate signal reception date
     * @param emitter signal emitter coordinates provider
     * @return RA-Dec (radians)
     */
    public <T extends CalculusFieldElement<T>> T[] value(final Frame frame, final FieldVector3D<T> receiverPosition,
                                                         final FieldAbsoluteDate<T> receptionDate,
                                                         final FieldPVCoordinatesProvider<T> emitter) {
        return value(frame, receiverPosition, receptionDate, emitter, receptionDate);
    }

    /**
     * Compute theoretical measurement with FIeld with guess for emission date.
     * @param <T> field type
     * @param frame frame where receiver position is given
     * @param receiverPosition receiver position in input frame at reception time
     * @param receptionDate signal reception date
     * @param emitter signal emitter coordinates provider
     * @param approxEmissionDate guess for the emission date (shall be adjusted by signal travel time computer)
     * @return RA-Dec (radians)
     */
    public <T extends CalculusFieldElement<T>> T[] value(final Frame frame, final FieldVector3D<T> receiverPosition,
                                                         final FieldAbsoluteDate<T> receptionDate,
                                                         final FieldPVCoordinatesProvider<T> emitter,
                                                         final FieldAbsoluteDate<T> approxEmissionDate) {
        // Compute line-of-sight
        final FieldVector3D<T> apparentLineOfSightInInputFrame = getEmitterToReceiverVector(frame, receiverPosition,
                receptionDate, emitter, approxEmissionDate).normalize();
        final FieldStaticTransform<T> toInertialFrameAtReception = frame.getStaticTransformTo(referenceFrame, receptionDate);
        final FieldVector3D<T> apparentLineOfSight = toInertialFrameAtReception.transformVector(apparentLineOfSightInInputFrame);

        // Compute right ascension and declination
        final T rightAscension = apparentLineOfSight.getAlpha();
        final T declination = apparentLineOfSight.getDelta();
        final T[] output = MathArrays.buildArray(receiverPosition.getX().getField(), 2);
        output[0] = rightAscension;
        output[1] = declination;
        return output;
    }
}
