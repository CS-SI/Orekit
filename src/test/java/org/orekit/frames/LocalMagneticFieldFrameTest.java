/* Copyright 2002-2022 CS GROUP
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
package org.orekit.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.models.earth.GeoMagneticField;
import org.orekit.models.earth.GeoMagneticFieldFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class LocalMagneticFieldFrameTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:earth");
    }

    @Test
    void testDefaultConstructorAndName() {
        // GIVEN
        final Frame            inertialFrameMock    = Mockito.mock(Frame.class);
        final Frame            bodyFrameMock        = Mockito.mock(Frame.class);
        final GeoMagneticField geoMagneticFieldMock = Mockito.mock(GeoMagneticField.class);

        final LocalMagneticFieldFrame localMagneticFieldFrame =
                new LocalMagneticFieldFrame(inertialFrameMock, geoMagneticFieldMock, bodyFrameMock);

        // WHEN
        final String computedName = localMagneticFieldFrame.getName();

        // THEN
        // Assert name
        Assertions.assertEquals("LOCAL_MAGNETIC_FIELD_FRAME", computedName);

        // Assert getters
        Assertions.assertEquals(inertialFrameMock, localMagneticFieldFrame.getInertialFrame());
        Assertions.assertEquals(geoMagneticFieldMock, localMagneticFieldFrame.getMagneticField());
    }


    @Test
    void testFieldRotation() {
        // GIVEN
        // Create local magnetic field frame instance
        final Frame            inertialFrameMock    = Mockito.mock(Frame.class);
        final Frame            bodyFrameMock        = Mockito.mock(Frame.class);
        final GeoMagneticField geoMagneticFieldMock = Mockito.mock(GeoMagneticField.class);

        final LocalMagneticFieldFrame localMagneticFieldFrame =
                new LocalMagneticFieldFrame(inertialFrameMock, geoMagneticFieldMock, bodyFrameMock);

        // Create a spy to stub non field rotationFromInertial method
        final LocalMagneticFieldFrame spy = Mockito.spy(localMagneticFieldFrame);

        // Create returned non field rotation from non field rotationFromInertial method
        final double   q0       = 0;
        final double   q1       = 0.707;
        final double   q2       = 0;
        final double   q3       = 0.707;
        final Rotation rotation = new Rotation(q0, q1, q2, q3, true);
        Mockito.doReturn(rotation).when(spy).rotationFromInertial(Mockito.any(AbsoluteDate.class),
                                                                  Mockito.any(PVCoordinates.class));

        // Create fielded date
        final AbsoluteDate date = Mockito.mock(AbsoluteDate.class);
        @SuppressWarnings("unchecked")
        final FieldAbsoluteDate<Binary64> fieldDate = Mockito.mock(FieldAbsoluteDate.class);
        Mockito.when(fieldDate.toAbsoluteDate()).thenReturn(date);

        // Create fielded pv
        final PVCoordinates pv = Mockito.mock(PVCoordinates.class);
        @SuppressWarnings("unchecked")
        final FieldPVCoordinates<Binary64> fieldPV = Mockito.mock(FieldPVCoordinates.class);
        Mockito.when(fieldPV.toPVCoordinates()).thenReturn(pv);

        // Create Binary64 field instance
        final Field<Binary64> binary64Field = Binary64Field.getInstance();

        // WHEN
        final FieldRotation<Binary64> computedRotation = spy.rotationFromInertial(binary64Field, fieldDate, fieldPV);

        // THEN
        Assertions.assertEquals(rotation.getQ0(), computedRotation.getQ0().getReal());
        Assertions.assertEquals(rotation.getQ1(), computedRotation.getQ1().getReal());
        Assertions.assertEquals(rotation.getQ2(), computedRotation.getQ2().getReal());
        Assertions.assertEquals(rotation.getQ3(), computedRotation.getQ3().getReal());
    }

    @Test
    void testAligningJDirection() {
        // GIVEN
        // Create mock pv
        final Vector3D positionMock      = Mockito.mock(Vector3D.class);
        final Vector3D velocityMock      = Mockito.mock(Vector3D.class);
        final Vector3D momentumMock      = Mockito.mock(Vector3D.class);
        final Vector3D minusPositionMock = Mockito.mock(Vector3D.class);
        final Vector3D minusVelocityMock = Mockito.mock(Vector3D.class);
        final Vector3D minusMomentumMock = Mockito.mock(Vector3D.class);

        Mockito.when(positionMock.negate()).thenReturn(minusPositionMock);
        Mockito.when(velocityMock.negate()).thenReturn(minusVelocityMock);
        Mockito.when(momentumMock.negate()).thenReturn(minusMomentumMock);

        Mockito.when(Vector3D.crossProduct(positionMock, velocityMock)).thenReturn(momentumMock);

        final PVCoordinates pv = new PVCoordinates(positionMock, velocityMock);

        // Extract enum values
        final LocalMagneticFieldFrame.LOFBuilderVector plusPos = LocalMagneticFieldFrame.LOFBuilderVector.PLUS_POSITION;
        final LocalMagneticFieldFrame.LOFBuilderVector plusVel = LocalMagneticFieldFrame.LOFBuilderVector.PLUS_VELOCITY;
        final LocalMagneticFieldFrame.LOFBuilderVector plusMom = LocalMagneticFieldFrame.LOFBuilderVector.PLUS_MOMENTUM;
        final LocalMagneticFieldFrame.LOFBuilderVector minusPos =
                LocalMagneticFieldFrame.LOFBuilderVector.MINUS_POSITION;
        final LocalMagneticFieldFrame.LOFBuilderVector minusVel =
                LocalMagneticFieldFrame.LOFBuilderVector.MINUS_VELOCITY;
        final LocalMagneticFieldFrame.LOFBuilderVector minusMom =
                LocalMagneticFieldFrame.LOFBuilderVector.MINUS_MOMENTUM;

        // WHEN
        final Vector3D computedPlusPos  = plusPos.getVector(pv);
        final Vector3D computedPlusVel  = plusVel.getVector(pv);
        final Vector3D computedPlusMom  = plusMom.getVector(pv);
        final Vector3D computedMinusPos = minusPos.getVector(pv);
        final Vector3D computedMinusVel = minusVel.getVector(pv);
        final Vector3D computedMinusMom = minusMom.getVector(pv);

        // THEN
        Assertions.assertEquals(positionMock, computedPlusPos);
        Assertions.assertEquals(velocityMock, computedPlusVel);
        Assertions.assertEquals(momentumMock, computedPlusMom);
        Assertions.assertEquals(minusPositionMock, computedMinusPos);
        Assertions.assertEquals(minusVelocityMock, computedMinusVel);
        Assertions.assertEquals(minusMomentumMock, computedMinusMom);
    }

    @Test
    public void testBotPointing() {

        final Map<String, Pair<Rotation, Double[]>> rotationAnglesMap = new HashMap<>();
        rotationAnglesMap.put("+x", new Pair<>(new Rotation(1.0, 0.0, 0.0, 0.0, false),
                                               new Double[] { FastMath.toRadians(0.0), FastMath.toRadians(0.0) }));
        rotationAnglesMap.put("+y", new Pair<>(new Rotation(0.7071068, 0.0, 0.0, 0.7071068, false),
                                               new Double[] { FastMath.toRadians(90.0), FastMath.toRadians(0.0) }));
        rotationAnglesMap.put("+z", new Pair<>(new Rotation(Vector3D.PLUS_K, Vector3D.PLUS_I),
                                               new Double[] { FastMath.toRadians(90.0), FastMath.toRadians(90.0) }));
        rotationAnglesMap.put("-x", new Pair<>(new Rotation(0.0, 0.0, 1.0, 0.0, false),
                                               new Double[] { FastMath.toRadians(180.0), FastMath.toRadians(0.0) }));
        rotationAnglesMap.put("-y", new Pair<>(new Rotation(Vector3D.MINUS_J, Vector3D.PLUS_I),
                                               new Double[] { FastMath.toRadians(90.0), FastMath.toRadians(180.0) }));
        rotationAnglesMap.put("-z", new Pair<>(new Rotation(Vector3D.MINUS_K, Vector3D.PLUS_I),
                                               new Double[] { FastMath.toRadians(90.0), FastMath.toRadians(-90.0) }));

        // UTC time zone
        final ZoneId utcTime = ZoneId.of("UTC");

        // ITRF frame
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, false);

        // get GeoMagnetic model
        final LocalDateTime    utcDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(1532785820000L), utcTime);
        final GeoMagneticField model   = GeoMagneticFieldFactory.getIGRF(utcDate.getYear());

        final Frame frame = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2018, 7, 28),
                                                   new TimeComponents(13, 50, 20.00),
                                                   TimeScalesFactory.getTAI());

        for (Map.Entry<String, Pair<Rotation, Double[]>> entry : rotationAnglesMap.entrySet()) {
            String   axis             = entry.getKey();
            Rotation rotate           = entry.getValue().getFirst();
            Double[] expectedThetaPhi = entry.getValue().getSecond();

            // test Bdot pointing, with body axis oriented to magnetic north
            final double[] rotationAngles = rotate.getAngles(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR);
            final LOF lof =
                    new LocalMagneticFieldFrame(frame, model, LocalMagneticFieldFrame.LOFBuilderVector.PLUS_MOMENTUM,
                                                itrf);

            final AttitudeProvider bdotPointing = new LofOffset(frame, lof, RotationOrder.XYZ,
                                                                rotationAngles[0], rotationAngles[1], rotationAngles[2]);

            // get orbit, attitude and magnetic field at equator
            KeplerianOrbit orbit = new KeplerianOrbit(6866719.5237, 0.003, FastMath.toRadians(89.7),
                                                      0, 0, 0, PositionAngleType.TRUE, frame, date, 3.986004415e14);
            Attitude        equatorialAttitude        = bdotPointing.getAttitude(orbit, date, frame);
            SpacecraftState spacecraftStateEquatorial = new SpacecraftState(orbit, equatorialAttitude);

            // move from the equator to poles with 1/4th of the orbit
            double h = orbit.getKeplerianPeriod() / 4.0;

            // get orbit, attitude and magnetic field at south pole
            Orbit           southPoleOrbit           = orbit.shiftedBy(h);
            AbsoluteDate    southPoleDate            = southPoleOrbit.getDate();
            Attitude        southPoleAttitude        = bdotPointing.getAttitude(southPoleOrbit, southPoleDate, frame);
            SpacecraftState spacecraftStateSouthPole = new SpacecraftState(southPoleOrbit, southPoleAttitude);

            // get orbit, attitude and magnetic field at north pole
            Orbit           northPoleOrbit           = orbit.shiftedBy(-h);
            AbsoluteDate    northPoleDate            = northPoleOrbit.getDate();
            Attitude        northPoleAttitude        = bdotPointing.getAttitude(northPoleOrbit, northPoleDate, frame);
            SpacecraftState spacecraftStateNorthPole = new SpacecraftState(northPoleOrbit, northPoleAttitude);

            // check attitude aligned with magnetic field, projecting magnetic field vector to satellite body frame
            // x = cos(theta)
            // y = sin(theta) cos(phi)
            // z = sin(theta) sin(phi)
            checkMagneticFieldAngles(axis, expectedThetaPhi, itrf, model, spacecraftStateEquatorial);
            checkMagneticFieldAngles(axis, expectedThetaPhi, itrf, model, spacecraftStateSouthPole);
            checkMagneticFieldAngles(axis, expectedThetaPhi, itrf, model, spacecraftStateNorthPole);
        }
    }

    private void checkMagneticFieldAngles(final String axis, final Double[] expectedThetaPhi, final Frame itrf,
                                          final GeoMagneticField model, SpacecraftState s) {
        // calculate magnetic field intensity
        Vector3D posItrf = s.getPosition(itrf);
        double   lat     = posItrf.getDelta();
        double   lng     = posItrf.getAlpha();
        double   alt     = posItrf.getNorm() - Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        Vector3D magneticFieldVector = model.calculateField(lat, lng, alt).getFieldVector();
        Vector3D magneticFieldVectorInertial = itrf.getTransformTo(s.getFrame(), s.getDate())
                                                   .transformVector(magneticFieldVector);

        // calculate projection of magnetic field vector into spacecraft body frame
        double[] magneticThetaPhi = getProjectionAngles(s, magneticFieldVectorInertial);

        double toll = FastMath.toRadians(1e-3);
        assertEquals(expectedThetaPhi[0], magneticThetaPhi[0], toll,
                     axis + " Theta angle, lat:" + FastMath.toDegrees(lat) + ", lng:" + FastMath.toDegrees(lng));
        if (FastMath.sin(expectedThetaPhi[0]) > 1e-5) {
            assertEquals(expectedThetaPhi[1], magneticThetaPhi[1], toll,
                         axis + " Phi angle, lat:" + FastMath.toDegrees(lat) + ", lng:" + FastMath.toDegrees(lng));
        }
    }

    /**
     * Get projected angles (theta, phi) of arbitrary vector on satellite body frame, considering attitude.
     *
     * @param s spacecraft state
     * @param vector vector to project, on arbitrary frame
     *
     * @return projected angles theta, phi [rad]
     */
    private double[] getProjectionAngles(final SpacecraftState s, final Vector3D vector) {
        //Get J2000 vector in Spacecraft body reference frame (consider the attitude)
        Transform toSpacecraftBody          = s.toTransform();
        Vector3D  vectorInCurrentStateFrame = toSpacecraftBody.transformVector(vector);
        double elevation = FastMath.abs(vectorInCurrentStateFrame.getDelta()) < 1e-5 ?
                0.0 : vectorInCurrentStateFrame.getDelta();
        double azimuth = FastMath.abs(vectorInCurrentStateFrame.getAlpha()) < 1e-5 ?
                0.0 : vectorInCurrentStateFrame.getAlpha();
        double theta = Math.acos(Math.cos(elevation) * Math.cos(azimuth));
        double phi   = Math.atan2(Math.tan(elevation), Math.sin(azimuth));
        return new double[] { theta, phi };
    }

}

