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
package org.orekit.forces.maneuvers;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/** This class is used to both manage the attitude of the satellite and the direction of thrust.
 * @author Mikael Fillastre
 * @author Andrea Fiorentino
 */
public class ThrustDirectionProvider implements AttitudeProvider {

	/** types, see builders for details */
    private enum ThrustDirectionProviderType {
        SATELLITE_ATTITUDE, 
        CUSTOM_ATTITUDE, 
        DIRECTION_IN_LOF, 
        DIRECTION_IN_FRAME
    }

    private final ThrustDirectionProviderType type;
    /** external attitude provider, for CUSTOM_ATTITUDE type. Set to null otherwise. */
    private final AttitudeProvider attitudeProvider;
    /** direction provider, for DIRECTION_IN_LOF and DIRECTION_IN_FRAME types. Set to null otherwise. */
    private final VariableThrustDirectionVector variableDirectionInFrame;
    /** thruster axis in satellite frame */
    private final Vector3D fixedDirection;
    /** reference frame for thrust direction, for DIRECTION_IN_FRAME type. Set to null otherwise. */
    private final Frame thrustDirectionFrame;
    /** Local Orbital Frame type, for DIRECTION_IN_LOF type. Set to null otherwise. */
    private final LOFType thrustDirectionLofType;


    private static void checkParameterNotNull(Object parameter, String name,
            ThrustDirectionProviderType type) {
        if (parameter == null) {
            throw new OrekitException(OrekitMessages.PARAMETER_NOT_SET,
            		name, "ThrustDirectionProvider-" + type.toString());
        }
    }

    private ThrustDirectionProvider (final ThrustDirectionProviderType type,
            final AttitudeProvider attitudeProvider,
            final VariableThrustDirectionVector variableDirectionInFrame, Vector3D fixedDirection,
            final Frame frame, final LOFType thrustDirectionLofType) {
        this.type = type;
        this.attitudeProvider = attitudeProvider;
        this.variableDirectionInFrame = variableDirectionInFrame;
        this.thrustDirectionFrame = frame;
        this.thrustDirectionLofType = thrustDirectionLofType;
        this.fixedDirection = fixedDirection;
    }

    /**
     * Build a ThrustDirectionProvider from a fixed direction in the satellite frame
     * the satellite attitude won't be managed by this object
     * @param direction constant direction in the satellite frame
     * @return a new instance
     */
    public static ThrustDirectionProvider buildFromFixedDirectionInSatelliteFrame(
            final Vector3D direction) {
        ThrustDirectionProvider obj = new ThrustDirectionProvider(
                ThrustDirectionProviderType.SATELLITE_ATTITUDE, null, null, direction, null, null);
        checkParameterNotNull(direction, "fixedDirection", obj.type);
        return obj;
    }

    /**
     * Build a ThrustDirectionProvider where the attitude is provided by an external object
     * the direction of thrust will be constant
     * @param attitudeProvider the object that provide the satellite attitude
     * @param direction thruster axis in satellite frame
     * @return a new instance
     */
    public static ThrustDirectionProvider buildFromCustomAttitude(
            final AttitudeProvider attitudeProvider, final Vector3D direction) {
        ThrustDirectionProvider obj = new ThrustDirectionProvider(
                ThrustDirectionProviderType.CUSTOM_ATTITUDE, attitudeProvider, null, direction,
                null, null);
        checkParameterNotNull(attitudeProvider, "attitudeProvider", obj.type);
        checkParameterNotNull(direction, "direction", obj.type);
        return obj;
    }

    /**
     * Build a ThrustDirectionProvider by a variable direction in a custom frame
     * @param thrustDirectionFrame reference frame for thrust direction
     * @param variableDirectionInFrame the object providing the thrust direction
     * @param thrusterAxisInSatelliteFrame thruster axis in satellite frame
     * @return a new instance
     */
    public static ThrustDirectionProvider buildFromDirectionInFrame(
            final Frame thrustDirectionFrame,
            final VariableThrustDirectionVector variableDirectionInFrame,
            final Vector3D thrusterAxisInSatelliteFrame) {
        ThrustDirectionProvider obj = new ThrustDirectionProvider(
                ThrustDirectionProviderType.DIRECTION_IN_FRAME, null, variableDirectionInFrame,
                thrusterAxisInSatelliteFrame, thrustDirectionFrame, null);
        checkParameterNotNull(variableDirectionInFrame, "variableDirectionInFrame", obj.type);
        checkParameterNotNull(thrustDirectionFrame, "thrustDirectionFrame", obj.type);
        return obj;
    }

	/**
     * Build a ThrustDirectionProvider by a variable direction in a Local Orbital Frame
	 * @param thrustDirectionLofType Local Orbital Frame type
	 * @param variableDirectionInFrame the object providing the thrust direction
	 * @param thrusterAxisInSatelliteFrame thruster axis in satellite frame
	 * @return a new instance
	 */
    public static ThrustDirectionProvider buildFromDirectionInLOF(
            final LOFType thrustDirectionLofType,
            final VariableThrustDirectionVector variableDirectionInFrame,
            final Vector3D thrusterAxisInSatelliteFrame) {
        ThrustDirectionProvider obj = new ThrustDirectionProvider(
                ThrustDirectionProviderType.DIRECTION_IN_LOF, null, variableDirectionInFrame,
                thrusterAxisInSatelliteFrame, null, thrustDirectionLofType);
        checkParameterNotNull(variableDirectionInFrame, "variableDirectionInFrame", obj.type);
        checkParameterNotNull(thrustDirectionLofType, "thrustDirectionLofType", obj.type);
        return obj;
    }

    public Vector3D getFixedDirection() {
        return fixedDirection;
    }

    @Override
    public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
        switch (type) {
            case CUSTOM_ATTITUDE:
                return attitudeProvider.getAttitude(pvProv, date, frame);
            case DIRECTION_IN_FRAME:
            case DIRECTION_IN_LOF:
                return getAttitudeFromFrame(pvProv, date, frame);
            default:
                throw new OrekitException(OrekitMessages.INVALID_TYPE_FOR_FUNCTION, "ThrustDirectionProvider.getAttitude", "type", type.toString());
        }
    }

    @Override
    public <T extends RealFieldElement<T>> FieldAttitude<T> getAttitude(
            FieldPVCoordinatesProvider<T> pvProv, FieldAbsoluteDate<T> date, Frame frame) {
        throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED, "ThrustDirectionProvider with RealFieldElement");
    }


    public Attitude getAttitudeFromFrame(PVCoordinatesProvider pvProv, AbsoluteDate date,
            Frame frame) {

        Rotation inertial2htrusterFrame;
        if (type.equals(ThrustDirectionProviderType.DIRECTION_IN_FRAME)) {
            inertial2htrusterFrame = thrustDirectionFrame.getTransformTo(frame, date).getRotation();
        } else { // LOF
            inertial2htrusterFrame = thrustDirectionLofType
                    .rotationFromInertial(pvProv.getPVCoordinates(date, frame));
        }

        Vector3D thrustDirection = variableDirectionInFrame.computeThrustDirection(pvProv, date,
                frame);
        Vector3D thrustDirectionInertial = inertial2htrusterFrame.applyInverseTo(thrustDirection);

        Rotation attitude = new Rotation(getFixedDirection(), thrustDirectionInertial);
        Attitude att = new Attitude(date, frame, attitude.revert(), Vector3D.ZERO, Vector3D.ZERO);

        return att;
    }

    public AttitudeProvider getManeuverAttitudeProvider() {
        AttitudeProvider attitudeProvider = null;
        if (type != ThrustDirectionProviderType.SATELLITE_ATTITUDE) {
            attitudeProvider = this;
        } // else default behaviour
        return attitudeProvider;
    }

}
