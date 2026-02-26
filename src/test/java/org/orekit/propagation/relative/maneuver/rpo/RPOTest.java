package org.orekit.propagation.relative.maneuver.rpo;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.List;

public class RPOTest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-8;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    // Assess the computation of the forced linear waypoints.
    @Test
    public void LinearWaypointsCalculatorTest() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(-1000,600,200), new Vector3D(0,0,0));
        final TimeStampedPVCoordinates finalPVT = new TimeStampedPVCoordinates(epoch.shiftedBy(1000),new Vector3D(0,0,0), new Vector3D(10,10,10));
        final List<TimeStampedPVCoordinates> waypointsCW = RPOModel.CW.computeForcedLinearWaypoints(initialPVT, finalPVT,11);
        final List<TimeStampedPVCoordinates> waypointsYA = RPOModel.YA.computeForcedLinearWaypoints(initialPVT, finalPVT,11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypointsCW.get(i).getDate().toDouble(), epoch.shiftedBy(i * 100).toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsCW.get(i).getPosition(),new Vector3D(-1000 + i * 100,  600 - i * 60,200- i * 20),NUMERICAL_TOLERANCE);
            Assertions.assertEquals(waypointsYA.get(i).getDate().toDouble(), epoch.shiftedBy(i * 100).toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsYA.get(i).getPosition(),new Vector3D(-1000 + i * 100, 600 - i * 60,200 - i * 20),NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints simplest case with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(),epoch.shiftedBy(1000. / 10 * i).toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(),new Vector3D(100 * FastMath.sin(i * 2 * FastMath.PI / 10),-100 * FastMath.cos(i * 2 * FastMath.PI / 10),0),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints retrograde case with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestRetrogradeCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypointsPrograde = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsRetrograde = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,true);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypointsPrograde.size(); i++) {
            // Assert the dates are the same.
            Assertions.assertEquals(waypointsPrograde.get(i).getDate().toDouble(), waypointsRetrograde.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints list position are reversed.
            TestUtils.validateVector3D(waypointsPrograde.get(waypointsPrograde.size()-(i+1)).getPosition(), waypointsRetrograde.get(i).getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsRetrograde.get(i).getVelocity(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with multi revolutions with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestMultiRevCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,2,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 1; i < 11; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble() + 1000, waypoints.get(10 + i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i).getPosition(), waypoints.get(10 + i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestInclinationCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsInclined = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, FastMath.PI/2,0,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsInclined.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition();
            TestUtils.validateVector3D(new Vector3D(0, refPosition.getY(), refPosition.getX()), waypointsInclined.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination and an Orientation with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestRaanCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsInclinedOriented = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, FastMath.PI/2,FastMath.PI/2,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsInclinedOriented.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition();
            TestUtils.validateVector3D(new Vector3D(-refPosition.getY(), 0, refPosition.getX()), waypointsInclinedOriented.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with a start angle with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestStartAngleCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,4,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsWithStartAngle = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,4,1,FastMath.PI/2,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size()-1; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsWithStartAngle.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i+1).getPosition(), waypointsWithStartAngle.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints simplest case with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(),epoch.shiftedBy(1000. / 10 * i).toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(),new Vector3D(-100 * FastMath.cos(i * 2 * FastMath.PI / 10),0,-100 * FastMath.sin(i * 2 * FastMath.PI / 10) ),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints retrograde case with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestRetrogradeYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypointsPrograde = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsRetrograde = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,true);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypointsPrograde.size(); i++) {
            // Assert the dates are the same.
            Assertions.assertEquals(waypointsPrograde.get(i).getDate().toDouble(), waypointsRetrograde.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints list position are reversed.
            TestUtils.validateVector3D(waypointsPrograde.get(waypointsPrograde.size()-(i+1)).getPosition(), waypointsRetrograde.get(i).getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsRetrograde.get(i).getVelocity(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with multi revolutions with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestMultiRevYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,2,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 1; i < 11; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble() + 1000, waypoints.get(10 + i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i).getPosition(), waypoints.get(10 + i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestInclinationYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsInclined = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, FastMath.PI/2,0,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsInclined.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition();
            TestUtils.validateVector3D(new Vector3D(refPosition.getX(), refPosition.getZ(), 0), waypointsInclined.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination and an Orientation with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestRaanYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsInclinedOriented = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, FastMath.PI/2,FastMath.PI/2,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsInclinedOriented.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition();
            TestUtils.validateVector3D(new Vector3D(0, refPosition.getZ(), refPosition.getX()), waypointsInclinedOriented.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with a start angle with CW rpo model.
    @Test
    public void ForcedCircularWaypointsCalculatorTestStartAngleYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,4,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsWithStartAngle = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,4,1,FastMath.PI/2,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size()-1; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsWithStartAngle.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i+1).getPosition(), waypointsWithStartAngle.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

}
