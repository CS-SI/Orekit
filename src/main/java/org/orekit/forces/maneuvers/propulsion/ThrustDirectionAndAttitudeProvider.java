/* Copyright 2020 Exotrail
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Exotrail licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOF;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * This class is used in to both manage the attitude of the satellite and the
 * direction of thrust.
 *<p>
 * It is used in ConfigurableLowThrustManeuver to set the spacecraft attitude
 * according to the expected thrust direction.
 *<p>
 * The direction can be variable or fixed, defined in the spaceraft frame, a
 * Local Orbital Frame or a user frame.
 *<p>
 * It is also possible to use an external attitude provider.
 *
 * @author Mikael Fillastre
 * @author Andrea Fiorentino
 * @since 10.2
 */
public class ThrustDirectionAndAttitudeProvider implements AttitudeProvider {

    /** Field name for error message. */
    private static final String FIELD_NAME_VARIABLE_DIRECTION = "variableDirectionInFrame";

    /** Field name for error message. */
    private static final String FIELD_NAME_DIRECTION_FRAME = "thrustDirectionFrame";

    /** Field name for error message. */
    private static final String FIELD_NAME_LOF_TYPE = "thrustDirectionLof";

    /** Types, see builders for details. */
    private enum ThrustDirectionAndAttitudeProviderType {
        /** SATELLITE_ATTITUDE. */
        SATELLITE_ATTITUDE,
        /** CUSTOM_ATTITUDE. */
        CUSTOM_ATTITUDE,
        /** DIRECTION_IN_LOF. */
        DIRECTION_IN_LOF,
        /** DIRECTION_IN_FRAME. */
        DIRECTION_IN_FRAME
    }

    /** Type. */
    private final ThrustDirectionAndAttitudeProviderType type;

    /**
     * External attitude provider, for CUSTOM_ATTITUDE type. Set to null otherwise.
     */
    private final AttitudeProvider attitudeProvider;

    /**
     * Direction provider, for DIRECTION_IN_LOF and DIRECTION_IN_FRAME types. Set to
     * null otherwise.
     */
    private final ThrustDirectionProvider variableDirectionInFrame;

    /** Thruster axis in satellite frame. */
    private final Vector3D thrusterAxisInSatelliteFrame;

    /**
     * Reference frame for thrust direction, for DIRECTION_IN_FRAME type. Set to
     * null otherwise.
     */
    private final Frame thrustDirectionFrame;

    /**
     * Local Orbital Frame, for DIRECTION_IN_LOF local orbital frame. Set to null otherwise.
     */
    private final LOF thrustDirectionLof;

    /**
     * Internal constructor.
     * @param type                         Type
     * @param attitudeProvider             External attitude provider, for
     *                                     CUSTOM_ATTITUDE type. Set to null
     *                                     otherwise
     * @param variableDirectionInFrame     Direction provider, for DIRECTION_IN_LOF
     *                                     and DIRECTION_IN_FRAME types. Set to null
     *                                     otherwise.
     * @param thrusterAxisInSatelliteFrame Thruster axis in satellite frame
     * @param frame                        Reference frame for thrust direction
     * @param thrustDirectionLof           Local Orbital Frame, for DIRECTION_IN_LOF local orbital frame
     *                                     (set to null otherwise)
     */
    private ThrustDirectionAndAttitudeProvider(final ThrustDirectionAndAttitudeProviderType type,
            final AttitudeProvider attitudeProvider, final ThrustDirectionProvider variableDirectionInFrame,
            final Vector3D thrusterAxisInSatelliteFrame, final Frame frame, final LOF thrustDirectionLof) {
        this.type = type;
        this.attitudeProvider = attitudeProvider;
        this.variableDirectionInFrame = variableDirectionInFrame;
        this.thrustDirectionFrame = frame;
        this.thrustDirectionLof = thrustDirectionLof;
        this.thrusterAxisInSatelliteFrame = thrusterAxisInSatelliteFrame;
    }

    /**
     * Throw an error if a mandatory parameter is not set.
     * @param parameter value
     * @param name      name of the parameter (for user message)
     * @param type      type to add details to user
     */
    private static void checkParameterNotNull(final Object parameter, final String name,
            final ThrustDirectionAndAttitudeProviderType type) {
        if (parameter == null) {
            throw new OrekitException(OrekitMessages.PARAMETER_NOT_SET, name,
                    "ThrustDirectionAndAttitudeProvider-" + type.toString());
        }
    }

    /**
     * Build a ThrustDirectionAndAttitudeProvider from a fixed direction in the
     * satellite frame. The satellite attitude won't be managed by this object
     *
     * @param direction constant direction in the satellite frame
     * @return a new instance
     */
    public static ThrustDirectionAndAttitudeProvider buildFromFixedDirectionInSatelliteFrame(final Vector3D direction) {
        final ThrustDirectionAndAttitudeProvider obj = new ThrustDirectionAndAttitudeProvider(
                ThrustDirectionAndAttitudeProviderType.SATELLITE_ATTITUDE, null, null, direction, null, null);
        checkParameterNotNull(direction, "thrusterAxisInSatelliteFrame", obj.type);
        return obj;
    }

    /**
     * Build a ThrustDirectionAndAttitudeProvider where the attitude is provided by
     * an external. Object the direction of thrust will be constant
     *
     * @param attitudeProvider the object that provide the satellite attitude
     * @param direction        thruster axis in satellite frame
     * @return a new instance
     */
    public static ThrustDirectionAndAttitudeProvider buildFromCustomAttitude(final AttitudeProvider attitudeProvider,
            final Vector3D direction) {
        final ThrustDirectionAndAttitudeProvider obj = new ThrustDirectionAndAttitudeProvider(
                ThrustDirectionAndAttitudeProviderType.CUSTOM_ATTITUDE, attitudeProvider, null, direction, null, null);
        checkParameterNotNull(attitudeProvider, "attitudeProvider", obj.type);
        checkParameterNotNull(direction, "direction", obj.type);
        return obj;
    }

    /**
     * Build a ThrustDirectionAndAttitudeProvider by a variable direction in a
     * custom frame.
     *
     * @param thrustDirectionFrame         reference frame for thrust direction
     * @param variableDirectionInFrame     the object providing the thrust direction
     * @param thrusterAxisInSatelliteFrame thruster axis in satellite frame
     * @return a new instance
     */
    public static ThrustDirectionAndAttitudeProvider buildFromDirectionInFrame(final Frame thrustDirectionFrame,
            final ThrustDirectionProvider variableDirectionInFrame, final Vector3D thrusterAxisInSatelliteFrame) {
        final ThrustDirectionAndAttitudeProvider obj = new ThrustDirectionAndAttitudeProvider(
                ThrustDirectionAndAttitudeProviderType.DIRECTION_IN_FRAME, null, variableDirectionInFrame,
                thrusterAxisInSatelliteFrame, thrustDirectionFrame, null);
        checkParameterNotNull(variableDirectionInFrame, FIELD_NAME_VARIABLE_DIRECTION, obj.type);
        checkParameterNotNull(thrustDirectionFrame, FIELD_NAME_DIRECTION_FRAME, obj.type);
        return obj;
    }

    /**
     * Build a ThrustDirectionAndAttitudeProvider by a variable direction in a Local
     * Orbital Frame.
     *
     * @param thrustDirectionLof           local orbital frame
     * @param variableDirectionInFrame     the object providing the thrust direction
     * @param thrusterAxisInSatelliteFrame thruster axis in satellite frame
     * @return a new instance
     */
    public static ThrustDirectionAndAttitudeProvider buildFromDirectionInLOF(final LOF thrustDirectionLof,
            final ThrustDirectionProvider variableDirectionInFrame, final Vector3D thrusterAxisInSatelliteFrame) {
        final ThrustDirectionAndAttitudeProvider obj = new ThrustDirectionAndAttitudeProvider(
                ThrustDirectionAndAttitudeProviderType.DIRECTION_IN_LOF, null, variableDirectionInFrame,
                thrusterAxisInSatelliteFrame, null, thrustDirectionLof);
        checkParameterNotNull(variableDirectionInFrame, FIELD_NAME_VARIABLE_DIRECTION, obj.type);
        checkParameterNotNull(thrustDirectionLof, FIELD_NAME_LOF_TYPE, obj.type);
        return obj;
    }

    /**
     * Thruster axis in satellite frame.
     * @return field
     */
    public Vector3D getThrusterAxisInSatelliteFrame() {
        return thrusterAxisInSatelliteFrame;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
        switch (type) {
            case CUSTOM_ATTITUDE:
                return attitudeProvider.getAttitude(pvProv, date, frame);
            case DIRECTION_IN_FRAME:
            case DIRECTION_IN_LOF:
                return getAttitudeFromFrame(pvProv, date, frame);
            default:
                throw new OrekitException(OrekitMessages.INVALID_TYPE_FOR_FUNCTION,
                        "ThrustDirectionAndAttitudeProvider.getAttitude", "type", type.toString());
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
            final FieldAbsoluteDate<T> date, final Frame frame) {
        throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED,
                "ThrustDirectionAndAttitudeProvider with CalculusFieldElement");
    }

    /**
     * Compute the attitude for DIRECTION_IN_FRAME or DIRECTION_IN_LOF types.
     * @param pvProv local position-velocity provider around current date
     * @param date   current date
     * @param frame  reference frame from which attitude is computed
     * @return attitude attitude on the specified date and position-velocity state
     */
    protected Attitude getAttitudeFromFrame(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
            final Frame frame) {

        final Rotation inertial2ThrusterFrame;
        if (type.equals(ThrustDirectionAndAttitudeProviderType.DIRECTION_IN_FRAME)) {
            inertial2ThrusterFrame = frame.getStaticTransformTo(thrustDirectionFrame, date).getRotation();
        } else { // LOF
            inertial2ThrusterFrame = thrustDirectionLof.rotationFromInertial(date, pvProv.getPVCoordinates(date, frame));
        }

        final Vector3D thrustDirection = variableDirectionInFrame.computeThrustDirection(pvProv, date, frame);
        final Vector3D thrustDirectionInertial = inertial2ThrusterFrame.applyInverseTo(thrustDirection);

        final Rotation attitude = new Rotation(getThrusterAxisInSatelliteFrame(), thrustDirectionInertial);
        final Attitude att = new Attitude(date, frame, attitude.revert(), Vector3D.ZERO, Vector3D.ZERO);

        return att;
    }

    /**
     * Attitude provider to use.
     * @return null in mode SATELLITE_ATTITUDE
     */
    public AttitudeProvider getManeuverAttitudeProvider() {
        AttitudeProvider attitudeProviderToReturn = null;
        if (type != ThrustDirectionAndAttitudeProviderType.SATELLITE_ATTITUDE) {
            attitudeProviderToReturn = this;
        } // else default behavior
        return attitudeProviderToReturn;
    }

}
