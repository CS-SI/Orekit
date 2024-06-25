/* Copyright 2002-2024 CS GROUP
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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.*;

import java.util.ArrayList;
import java.util.List;


class NadirPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF
    private Frame itrf;

    @Test
    void testGetTargetPV() {
        // GIVEN
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., itrf);
        final NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        final PVCoordinatesProvider provider = (date1, frame1) -> {
            final double duration = date1.durationFrom(AbsoluteDate.FIFTIES_EPOCH);
            final Vector3D position = new Vector3D(duration * duration / 2, 0., 0.);
            final Vector3D velocity = new Vector3D(duration, 0., 0.);
            final Vector3D acceleration = new Vector3D(1, 0, 0);
            return new TimeStampedPVCoordinates(date1, position, velocity, acceleration);
        };
        // WHEN
        final TimeStampedPVCoordinates actualPV = nadirAttitudeLaw.getTargetPV(provider, date, frame);
        // THEN
        final PVCoordinatesProvider providerWithoutAcceleration = (date12, frame12) -> {
            final TimeStampedPVCoordinates originalPV = provider.getPVCoordinates(date12, frame12);
            return new TimeStampedPVCoordinates(date12, originalPV.getPosition(), originalPV.getVelocity());
        };
        final TimeStampedPVCoordinates pv = nadirAttitudeLaw.getTargetPV(providerWithoutAcceleration, date, frame);
        Assertions.assertEquals(pv.getDate(), actualPV.getDate());
        final PVCoordinates relativePV = new PVCoordinates(pv, actualPV);
        Assertions.assertEquals(0., relativePV.getPosition().getNorm(), 2e-9);
        Assertions.assertEquals(0., relativePV.getVelocity().getNorm(), 1e-7);
    }

    @Test
    void testGetTargetPVViaInterpolationField() {
        // GIVEN
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., itrf);
        final NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        final PVCoordinatesProvider provider = (dateIn, frameIn) -> new TimeStampedPVCoordinates(dateIn,
                new Vector3D(dateIn.durationFrom(AbsoluteDate.FIFTIES_EPOCH), 0., 0.), Vector3D.PLUS_I);
        final ComplexField field = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> fieldDate = new FieldAbsoluteDate<>(field, date);
        final FieldPVCoordinatesProvider<Complex> fieldProvider = (dateIn, frameIn) -> new TimeStampedFieldPVCoordinates<>(dateIn,
                new FieldVector3D<>(dateIn.durationFrom(AbsoluteDate.FIFTIES_EPOCH), field.getZero(), field.getZero()),
                FieldVector3D.getPlusI(field), FieldVector3D.getZero(field));
        // WHEN
        final TimeStampedFieldPVCoordinates<Complex> actualPV = nadirAttitudeLaw.getTargetPV(fieldProvider, fieldDate, frame);
        // THEN
        final TimeStampedPVCoordinates pv = nadirAttitudeLaw.getTargetPV(provider, date, frame);
        Assertions.assertEquals(pv.getDate(), actualPV.getDate().toAbsoluteDate());
        final PVCoordinates relativePV = new PVCoordinates(pv, actualPV.toPVCoordinates());
        final Vector3D positionDifference = relativePV.getPosition();
        Assertions.assertEquals(0., positionDifference.getNorm(), 1e-9);
        final Vector3D velocityDifference = relativePV.getVelocity();
        Assertions.assertEquals(0., velocityDifference.getNorm(), 1e-6);
    }

    @Test
    void testGetTargetPosition() {
        // GIVEN
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., itrf);
        final NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);
        final CircularOrbit circ =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                        FastMath.toRadians(5.300), PositionAngleType.MEAN,
                        FramesFactory.getEME2000(), date, mu);
        // WHEN
        final Vector3D actualPosition = nadirAttitudeLaw.getTargetPosition(circ, circ.getDate(), circ.getFrame());
        // THEN
        final Vector3D expectedPosition = nadirAttitudeLaw.getTargetPV(circ, circ.getDate(), circ.getFrame()).getPosition();
        Assertions.assertEquals(0., expectedPosition.subtract(actualPosition).getNorm(), 1e-9);
    }

    @Test
    void testGetTargetPositionField() {
        // GIVEN
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., itrf);
        final NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);
        final CircularOrbit circ =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                        FastMath.toRadians(5.300), PositionAngleType.MEAN,
                        FramesFactory.getEME2000(), date, mu);
        final FieldCircularOrbit<Complex> fieldOrbit = new FieldCircularOrbit<>(ComplexField.getInstance(), circ);
        // WHEN
        final FieldVector3D<Complex> actualPosition = nadirAttitudeLaw.getTargetPosition(fieldOrbit, fieldOrbit.getDate(), fieldOrbit.getFrame());
        // THEN
        final FieldVector3D<Complex> expectedPosition = nadirAttitudeLaw.getTargetPV(fieldOrbit, fieldOrbit.getDate(), fieldOrbit.getFrame()).getPosition();
        Assertions.assertEquals(0., expectedPosition.subtract(actualPosition).getNorm().getReal(), 1e-9);
    }

    /** Test in the case of a spheric earth : nadir pointing shall be
     * the same as earth center pointing
     */
    @Test
    void testSphericEarth() {

        // Spheric earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., itrf);

        // Create nadir pointing attitude provider
        NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);

        // Create earth center pointing attitude provider
        BodyCenterPointing earthCenterAttitudeLaw =
                new BodyCenterPointing(FramesFactory.getEME2000(), earthShape);

        // Create satellite position as circular parameters
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                   FastMath.toRadians(5.300), PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Get nadir attitude
        Rotation rotNadir = nadirAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get earth center attitude
        Rotation rotCenter = earthCenterAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // For a spheric earth, earth center pointing attitude and nadir pointing attitude
        // shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.
        Rotation rotCompo = rotCenter.composeInverse(rotNadir, RotationConvention.VECTOR_OPERATOR);
        double angle = rotCompo.getAngle();
        Assertions.assertEquals(angle, 0.0, Utils.epsilonAngle);

    }

    /** Test in the case of an elliptic earth : nadir pointing shall be :
     *   - the same as earth center pointing in case of equatorial or polar position
     *   - different from earth center pointing in any other case
     */
    @Test
    void testNonSphericEarth() {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Create nadir pointing attitude provider
        NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);

        // Create earth center pointing attitude provider
        BodyCenterPointing earthCenterAttitudeLaw =
                new BodyCenterPointing(FramesFactory.getEME2000(), earthShape);

        //  Satellite on equatorial position
        // **********************************
        KeplerianOrbit kep =
            new KeplerianOrbit(7178000.0, 1.e-8, FastMath.toRadians(50.), 0., 0.,
                                    0., PositionAngleType.TRUE, FramesFactory.getEME2000(), date, mu);

        // Get nadir attitude
        Rotation rotNadir = nadirAttitudeLaw.getAttitude(kep, date, kep.getFrame()).getRotation();

        checkField(Binary64Field.getInstance(), nadirAttitudeLaw, kep, kep.getDate(), kep.getFrame());

        // Get earth center attitude
        Rotation rotCenter = earthCenterAttitudeLaw.getAttitude(kep, date, kep.getFrame()).getRotation();

        // For a satellite at equatorial position, earth center pointing attitude and nadir pointing
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.
        Rotation rotCompo = rotCenter.composeInverse(rotNadir, RotationConvention.VECTOR_OPERATOR);
        double angle = rotCompo.getAngle();
        Assertions.assertEquals(0.0, angle, 5.e-6);

        //  Satellite on polar position
        // *****************************
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., FastMath.toRadians(90.), 0.,
                                   FastMath.toRadians(90.), PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

       // Get nadir attitude
        rotNadir = nadirAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get earth center attitude
        rotCenter = earthCenterAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // For a satellite at polar position, earth center pointing attitude and nadir pointing
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.
        rotCompo = rotCenter.composeInverse(rotNadir, RotationConvention.VECTOR_OPERATOR);
        angle = rotCompo.getAngle();
        Assertions.assertEquals(angle, 0.0, 5.e-6);

        //  Satellite on any position
        // ***************************
        circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., FastMath.toRadians(50.), 0.,
                                   FastMath.toRadians(90.), PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        // Get nadir attitude
        rotNadir = nadirAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get earth center attitude
        rotCenter = earthCenterAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // For a satellite at any position, earth center pointing attitude and nadir pointing
        // and nadir pointing attitude shall not be the same, i.e the composition of inverse earth
        // pointing rotation with nadir pointing rotation shall be different from identity.
        rotCompo = rotCenter.composeInverse(rotNadir, RotationConvention.VECTOR_OPERATOR);
        angle = rotCompo.getAngle();
        Assertions.assertEquals(angle, FastMath.toRadians(0.16797386586252272), Utils.epsilonAngle);
    }

    /** Vertical test : check that Z satellite axis is collinear to local vertical axis,
        which direction is : (cos(lon)*cos(lat), sin(lon)*cos(lat), sin(lat)),
        where lon et lat stand for observed point coordinates
        (i.e satellite ones, since they are the same by construction,
        but that's what is to test.
     */
    @Test
    void testVertical() {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Create earth center pointing attitude provider
        NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);

        //  Satellite on any position
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., FastMath.toRadians(50.), 0.,
                                   FastMath.toRadians(90.), PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        //  Vertical test
        // ***************
        // Get observed ground point position/velocity
        TimeStampedPVCoordinates pvTargetItrf = nadirAttitudeLaw.getTargetPV(circ, date, itrf);

        // Convert to geodetic coordinates
        GeodeticPoint geoTarget = earthShape.transform(pvTargetItrf.getPosition(), itrf, date);

        // Compute local vertical axis
        double xVert = FastMath.cos(geoTarget.getLongitude())*FastMath.cos(geoTarget.getLatitude());
        double yVert = FastMath.sin(geoTarget.getLongitude())*FastMath.cos(geoTarget.getLatitude());
        double zVert = FastMath.sin(geoTarget.getLatitude());
        Vector3D targetVertical = new Vector3D(xVert, yVert, zVert);

        // Get attitude rotation state
        Rotation rotSatEME2000 = nadirAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get satellite Z axis in EME2000 frame
        Vector3D zSatEME2000 = rotSatEME2000.applyInverseTo(Vector3D.PLUS_K);
        Vector3D zSatItrf = FramesFactory.getEME2000().getStaticTransformTo(itrf, date).transformVector(zSatEME2000);

        // Check that satellite Z axis is collinear to local vertical axis
        double angle= Vector3D.angle(zSatItrf, targetVertical);
        Assertions.assertEquals(0.0, FastMath.sin(angle), Utils.epsilonTest);

    }

    /** Test the derivatives of the sliding target
     */
    @Test
    void testSlidingDerivatives() {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Create earth center pointing attitude provider
        NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);

        //  Satellite on any position
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., FastMath.toRadians(50.), 0.,
                                   FastMath.toRadians(90.), PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        List<TimeStampedPVCoordinates> sample = new ArrayList<>();
        for (double dt = -0.1; dt < 0.1; dt += 0.05) {
            Orbit o = circ.shiftedBy(dt);
            sample.add(nadirAttitudeLaw.getTargetPV(o, o.getDate(), o.getFrame()));
        }

        // create interpolator
        final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                new TimeStampedPVCoordinatesHermiteInterpolator(sample.size(), CartesianDerivativesFilter.USE_P);

        TimeStampedPVCoordinates reference = interpolator.interpolate(circ.getDate(), sample);

        TimeStampedPVCoordinates target =
                nadirAttitudeLaw.getTargetPV(circ, circ.getDate(), circ.getFrame());

        Assertions.assertEquals(0.0,
                            Vector3D.distance(reference.getPosition(),     target.getPosition()),
                            1.0e-15 * reference.getPosition().getNorm());
        Assertions.assertEquals(0.0,
                            Vector3D.distance(reference.getVelocity(),     target.getVelocity()),
                            3.0e-11 * reference.getVelocity().getNorm());
        Assertions.assertEquals(0.0,
                            Vector3D.distance(reference.getAcceleration(), target.getAcceleration()),
                            1.3e-5 * reference.getAcceleration().getNorm());

    }

    @Test
    void testSpin() {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Create earth center pointing attitude provider
        NadirPointing law = new NadirPointing(FramesFactory.getEME2000(), earthShape);

        //  Satellite on any position
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngleType.MEAN,
                              FramesFactory.getEME2000(), date, mu);

        Propagator propagator = new KeplerianPropagator(orbit, law, mu, 2500.0);

        double h = 0.1;
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState s0     = propagator.propagate(date);
        SpacecraftState sPlus  = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        Assertions.assertEquals(0.0, errorAngleMinus, 5.3e-9 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assertions.assertEquals(0.0, errorAnglePlus, 8.1e-9 * evolutionAnglePlus);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Rotation rM = sMinus.getAttitude().getRotation();
        Rotation rP = sPlus.getAttitude().getRotation();
        Vector3D reference = AngularCoordinates.estimateRate(rM, rP, 2 * h);
        Assertions.assertTrue(Rotation.distance(rM, rP) > 2.0e-4);
        Assertions.assertEquals(0.0, spin0.subtract(reference).getNorm(), 2.0e-6);

    }

    private <T extends CalculusFieldElement<T>> void checkField(final Field<T> field, final GroundPointing provider,
                                                            final Orbit orbit, final AbsoluteDate date,
                                                            final Frame frame)
        {

        final Attitude attitudeD = provider.getAttitude(orbit, date, frame);
        final FieldOrbit<T> orbitF = new FieldSpacecraftState<>(field, new SpacecraftState(orbit)).getOrbit();
        final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, date);
        final FieldAttitude<T> attitudeF = provider.getAttitude(orbitF, dateF, frame);
        Assertions.assertEquals(0.0, Rotation.distance(attitudeD.getRotation(), attitudeF.getRotation().toRotation()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(attitudeD.getSpin(), attitudeF.getSpin().toVector3D()), 2.0e-14);
        Assertions.assertEquals(0.0, Vector3D.distance(attitudeD.getRotationAcceleration(), attitudeF.getRotationAcceleration().toVector3D()), 4.0e-12);

        final TimeStampedPVCoordinates         pvD = provider.getTargetPV(orbit, date, frame);
        final TimeStampedFieldPVCoordinates<T> pvF = provider.getTargetPV(orbitF, dateF, frame);
        Assertions.assertEquals(0.0, Vector3D.distance(pvD.getPosition(),     pvF.getPosition().toVector3D()),     1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(pvD.getVelocity(),     pvF.getVelocity().toVector3D()),     2.0e-8);
        Assertions.assertEquals(0.0, Vector3D.distance(pvD.getAcceleration(), pvF.getAcceleration().toVector3D()), 3.0e-5);

    }

    @BeforeEach
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            // Computation date
            date = new AbsoluteDate(new DateComponents(2008, 4, 7),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Body mu
            mu = 3.9860047e14;

            // Reference frame = ITRF
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        } catch (OrekitException oe) {
            Assertions.fail(oe.getMessage());
        }

    }

    @AfterEach
    public void tearDown() {
        date = null;
        itrf = null;
    }

}

