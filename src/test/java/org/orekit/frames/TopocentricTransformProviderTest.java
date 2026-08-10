package org.orekit.frames;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TopocentricTransformProviderTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testFieldGetStaticTransform() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(false));
        final GeodeticPoint point = new GeodeticPoint(1., 2., 3.);
        final TopocentricTransformProvider provider = new TopocentricTransformProvider(point, bodyShape);
        final FieldAbsoluteDate<Binary64> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        // WHEN
        final FieldStaticTransform<Binary64> fieldStaticTransform = provider.getStaticTransform(fieldAbsoluteDate);
        // THEN
        final StaticTransform staticTransform = provider.getTransform(fieldAbsoluteDate.toAbsoluteDate());
        assertEquals(staticTransform.getTranslation(), fieldStaticTransform.getTranslation().toVector3D());
        assertEquals(0., Rotation.distance(staticTransform.getRotation(), fieldStaticTransform.getRotation().toRotation()));
    }

    @Test
    void testFieldGetKinematicTransform() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(false));
        final GeodeticPoint point = new GeodeticPoint(1., 2., 3.);
        final TopocentricTransformProvider provider = new TopocentricTransformProvider(point, bodyShape);
        final FieldAbsoluteDate<Binary64> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        // WHEN
        final FieldKinematicTransform<Binary64> fieldKinematicTransform = provider.getKinematicTransform(fieldAbsoluteDate);
        // THEN
        final KinematicTransform kinematicTransform = provider.getTransform(fieldAbsoluteDate.toAbsoluteDate());
        assertEquals(kinematicTransform.getTranslation(), fieldKinematicTransform.getTranslation().toVector3D());
        assertEquals(0., Rotation.distance(kinematicTransform.getRotation(), fieldKinematicTransform.getRotation().toRotation()));
        assertEquals(kinematicTransform.getRotationRate(), fieldKinematicTransform.getRotationRate().toVector3D());
        assertEquals(kinematicTransform.getVelocity(), fieldKinematicTransform.getVelocity().toVector3D());
    }

    @Test
    void testFieldGetTransform() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(false));
        final GeodeticPoint point = new GeodeticPoint(1., 2., 3.);
        final TopocentricTransformProvider provider = new TopocentricTransformProvider(point, bodyShape);
        final FieldAbsoluteDate<Binary64> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        // WHEN
        final FieldTransform<Binary64> fieldTransform = provider.getTransform(fieldAbsoluteDate);
        // THEN
        final Transform transform = provider.getTransform(fieldAbsoluteDate.toAbsoluteDate());
        assertEquals(transform.getTranslation(), fieldTransform.getTranslation().toVector3D());
        assertEquals(0., Rotation.distance(transform.getRotation(), fieldTransform.getRotation().toRotation()));
        assertEquals(transform.getRotationRate(), fieldTransform.getRotationRate().toVector3D());
        assertEquals(transform.getVelocity(), fieldTransform.getVelocity().toVector3D());
        assertEquals(transform.getRotationAcceleration(), fieldTransform.getRotationAcceleration().toVector3D());
    }

    @Test
    void testGetStaticTransform() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(false));
        final GeodeticPoint point = new GeodeticPoint(1., 2., 3.);
        final TopocentricTransformProvider provider = new TopocentricTransformProvider(point, bodyShape);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final StaticTransform staticTransform = provider.getStaticTransform(date);
        // THEN
        final Transform transform = provider.getTransform(date);
        assertEquals(staticTransform.getDate(), transform.getDate());
        assertEquals(staticTransform.getTranslation(), transform.getTranslation());
        assertEquals(0., Rotation.distance(staticTransform.getRotation(), transform.getRotation()));
    }

    @Test
    void testGetKinematicTransform() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(false));
        final GeodeticPoint point = new GeodeticPoint(4., 5., 6.);
        final TopocentricTransformProvider provider = new TopocentricTransformProvider(point, bodyShape);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final KinematicTransform kinematicTransform = provider.getKinematicTransform(date);
        // THEN
        final Transform transform = provider.getTransform(date);
        assertEquals(kinematicTransform.getDate(), transform.getDate());
        assertEquals(kinematicTransform.getTranslation(), transform.getTranslation());
        assertEquals(0., Rotation.distance(kinematicTransform.getRotation(), transform.getRotation()));
        assertEquals(kinematicTransform.getVelocity(), transform.getVelocity());
        assertEquals(kinematicTransform.getRotationRate(), transform.getRotationRate());
    }

    @Test
    void testGetTransform() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(false));
        final GeodeticPoint point = new GeodeticPoint(4., 5., 6.);
        final TopocentricTransformProvider provider = new TopocentricTransformProvider(point, bodyShape);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final Transform transform = provider.getTransform(date);
        // THEN
        assertEquals(date, transform.getDate());
        assertEquals(bodyShape.transform(point).negate(), transform.getTranslation());
        final Rotation expectedRotation = new Rotation(point.getEast(), point.getZenith(), Vector3D.PLUS_I, Vector3D.PLUS_K);
        assertEquals(0., Rotation.distance(expectedRotation, transform.getRotation()));
        assertEquals(Vector3D.ZERO, transform.getVelocity());
        assertEquals(Vector3D.ZERO, transform.getRotationRate());
    }
}
