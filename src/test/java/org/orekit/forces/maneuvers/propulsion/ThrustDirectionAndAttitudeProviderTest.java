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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

public class ThrustDirectionAndAttitudeProviderTest {
    /** */
    private static final double EPSILON_ROTATION = 1E-15;
    /** */
    private FactoryManagedFrame frame;
    /** */
    private AbsoluteDate date;
    /** */
    private PVCoordinatesProvider pvProv;
    /** */
    private final Vector3D thrusterAxisInSatelliteFrame = Vector3D.PLUS_J;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        frame = FramesFactory.getCIRF(IERSConventions.IERS_2010, true);
        date = new AbsoluteDate(2020, 01, 01, TimeScalesFactory.getUTC());
        final double sma = Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 700e3;
        final double ecc = 0.01;
        final double inc = FastMath.toRadians(60);
        final double pa = FastMath.toRadians(0);
        final double raan = 0.;
        final double anomaly = FastMath.toRadians(0);

        final KeplerianOrbit initialOrbit = new KeplerianOrbit(sma, ecc, inc, pa, raan, anomaly, PositionAngleType.TRUE,
                FramesFactory.getCIRF(IERSConventions.IERS_2010, true), date, Constants.EGM96_EARTH_MU);
        pvProv = new KeplerianPropagator(initialOrbit);
    }

    private AttitudeProvider buildVelocityAttitudeProvider() {
        return new LofOffset(frame, LOFType.TNW);
    }

    @Test
    public void fixedDirectionCanNotProvideTheAttitude() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final ThrustDirectionAndAttitudeProvider provider = ThrustDirectionAndAttitudeProvider
                    .buildFromFixedDirectionInSatelliteFrame(Vector3D.PLUS_I);
            Assertions.assertNull(provider.getManeuverAttitudeProvider());
            provider.getAttitude(pvProv, date, frame); // raise an error
        });
    }

    @Test
    public void missingParameterTest() {
        Assertions.assertThrows(OrekitException.class, () -> {
            ThrustDirectionAndAttitudeProvider.buildFromDirectionInFrame(frame, null, thrusterAxisInSatelliteFrame);
        });
    }

    @Test
    public void attitudeFromDirectionInFrame() {

        final Vector3D fixedThrustDirection = new Vector3D(1, 2, 3).normalize();
        final ConstantThrustDirectionProvider thrustDirectionInertial = new ConstantThrustDirectionProvider(
                fixedThrustDirection);

        final ThrustDirectionAndAttitudeProvider provider = ThrustDirectionAndAttitudeProvider
                .buildFromDirectionInFrame(frame, thrustDirectionInertial, thrusterAxisInSatelliteFrame);

        Assertions.assertNotNull(provider.getManeuverAttitudeProvider());

        // attitude from Frame: inertial => sat
        final Attitude inertialToSat = provider.getAttitude(pvProv, date, frame);

        final Vector3D thrustDirectionRecomputed = inertialToSat.getRotation().revert()
                .applyTo(thrusterAxisInSatelliteFrame);
        Assertions.assertEquals(0, fixedThrustDirection.subtract(thrustDirectionRecomputed).getNorm(), EPSILON_ROTATION);

    }

    @Test
    public void attitudeFromDirectionInLOF() {
        final Vector3D fixedThrustDirection = Vector3D.PLUS_I;
        final ConstantThrustDirectionProvider thrustDirectionTNW = new ConstantThrustDirectionProvider(Vector3D.PLUS_I);

        final ThrustDirectionAndAttitudeProvider provider = ThrustDirectionAndAttitudeProvider
                .buildFromDirectionInLOF(LOFType.TNW, thrustDirectionTNW, thrusterAxisInSatelliteFrame);

        Assertions.assertNotNull(provider.getManeuverAttitudeProvider());

        // attitude from Frame: inertial => sat
        final Attitude inertialToSat = provider.getAttitude(pvProv, date, frame);

        // recompute rotation from satellite frame to TNW
        final Rotation inertialToLof = LOFType.TNW.transformFromInertial(date, pvProv.getPVCoordinates(date, frame))
                .getRotation();
        final Rotation satToLof = inertialToLof.compose(inertialToSat.getRotation().revert(),
                RotationConvention.VECTOR_OPERATOR);

        final Vector3D thrustDirectionRecomputed = satToLof.applyTo(thrusterAxisInSatelliteFrame);
        Assertions.assertEquals(0., fixedThrustDirection.subtract(thrustDirectionRecomputed).getNorm(), EPSILON_ROTATION);
    }

    @Test
    public void attitudeFromCustomProvider() {
        final ThrustDirectionAndAttitudeProvider provider = ThrustDirectionAndAttitudeProvider
                .buildFromCustomAttitude(buildVelocityAttitudeProvider(), thrusterAxisInSatelliteFrame);
        Assertions.assertNotNull(provider.getManeuverAttitudeProvider());
        Assertions.assertEquals(
                buildVelocityAttitudeProvider().getAttitude(pvProv, date, frame).getRotation()
                        .applyTo(thrusterAxisInSatelliteFrame),
                provider.getAttitude(pvProv, date, frame).getRotation().applyTo(thrusterAxisInSatelliteFrame));
    }

    @Test
    public void getAttitudeFieldError() {
        Assertions.assertThrows(OrekitException.class, () -> {

            final ThrustDirectionAndAttitudeProvider provider = ThrustDirectionAndAttitudeProvider
                    .buildFromCustomAttitude(buildVelocityAttitudeProvider(), Vector3D.PLUS_I);
            Assertions.assertNotNull(provider.getManeuverAttitudeProvider());
            provider.getAttitude(null, new FieldAbsoluteDate<>(date, new Binary64(0)), frame); // raise an error

        });
 }
}
