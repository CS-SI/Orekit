package org.orekit.estimation.iod;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/**
 *
 * Source: http://ccar.colorado.edu/asen5050/projects/projects_2012/kemble/gibbs_derivation.htm
 *
 * @author Joris Olympio
 * @since 7.1
 *
 */
public class IodLambertTest {

    @Test
    public void testLambert() throws OrekitException {
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final double mu = context.initialOrbit.getMu();
        final Frame frame = context.initialOrbit.getFrame();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final List<ObservedMeasurement<?>> measurements =
                EstimationTestUtils.createMeasurements(propagator,
                                                       new PVMeasurementCreator(),
                                                       0.0, 1.0, 60.0);

        // measurement data 1
        final int idMeasure1 = 0;
        final AbsoluteDate date1 = measurements.get(idMeasure1).getDate();
        /*final Vector3D stapos1 = context.stations.get(0)  // FIXME we need to access the station of the measurement
                                    .getBaseFrame()
                                    .getPVCoordinates(date1, frame)
                                    .getPosition();*/
        final Vector3D position1 = new Vector3D(
                                                measurements.get(idMeasure1).getObservedValue()[0],
                                                measurements.get(idMeasure1).getObservedValue()[1],
                                                measurements.get(idMeasure1).getObservedValue()[2]);

        // measurement data 2
        final int idMeasure2 = 10;
        final AbsoluteDate date2 = measurements.get(idMeasure2).getDate();
        /*final Vector3D stapos2 = context.stations.get(0)  // FIXME we need to access the station of the measurement
                        .getBaseFrame()
                        .getPVCoordinates(date2, frame)
                        .getPosition();*/
        final Vector3D position2 = new Vector3D(
                                                measurements.get(idMeasure2).getObservedValue()[0],
                                                measurements.get(idMeasure2).getObservedValue()[1],
                                                measurements.get(idMeasure2).getObservedValue()[2]);

        final int nRev = 0;

        // instantiate the IOD method
        final IodLambert iod = new IodLambert(mu);

        final KeplerianOrbit orbit = iod.estimate(frame,
                                            true,
                                            nRev,
                                            /*stapos1.add*/(position1), date1,
                                            /*stapos2.add*/(position2), date2);

        Assert.assertEquals(orbit.getA(), context.initialOrbit.getA(), 1.0e-9 * context.initialOrbit.getA());
        Assert.assertEquals(orbit.getE(), context.initialOrbit.getE(), 1.0e-9 * context.initialOrbit.getE());
        Assert.assertEquals(orbit.getI(), context.initialOrbit.getI(), 1.0e-9 * context.initialOrbit.getI());
    }

    @Test
    public void testMultiRevolutions() throws OrekitException {

        Utils.setDataRoot("regular-data");
        TLE aussatB1 = new TLE("1 22087U 92054A   17084.21270512 -.00000243 +00000-0 +00000-0 0  9999",
                               "2 22087 008.5687 046.5717 0005960 022.3650 173.1619 00.99207999101265");
        final Propagator propagator = TLEPropagator.selectExtrapolator(aussatB1);
        final Frame teme = FramesFactory.getTEME();

        final AbsoluteDate t1 = new AbsoluteDate("2017-03-25T23:48:31.282", TimeScalesFactory.getUTC());
        final Vector3D p1 = propagator.propagate(t1).getPVCoordinates(teme).getPosition();
        final AbsoluteDate t2 = t1.shiftedBy(40000);
        final Vector3D p2 = propagator.propagate(t2).getPVCoordinates(teme).getPosition();
        final AbsoluteDate t3 = t1.shiftedBy(115200.0);
        final Vector3D p3 = propagator.propagate(t3).getPVCoordinates(teme).getPosition();

        IodLambert lambert = new IodLambert(Constants.WGS84_EARTH_MU);
        KeplerianOrbit k0 = lambert.estimate(teme, true, 0, p1, t1, p2, t2);
        Assert.assertEquals(6.08e-4, k0.getE(), 1.0e-6);
        Assert.assertEquals(8.55, FastMath.toDegrees(k0.getI()), 0.01);
        Assert.assertEquals(0.0, Vector3D.distance(p1, k0.getPVCoordinates(t1, teme).getPosition()), 2.0e-8);
        Assert.assertEquals(0.0, Vector3D.distance(p2, k0.getPVCoordinates(t2, teme).getPosition()), 2.0e-7);

        KeplerianOrbit k1 = lambert.estimate(teme, false, 1, p1, t1, p3, t3);
        Assert.assertEquals(5.97e-4, k1.getE(), 1.0e-6);
        Assert.assertEquals(8.55, FastMath.toDegrees(k1.getI()), 0.01);
        Assert.assertEquals(0.0, Vector3D.distance(p1, k1.getPVCoordinates(t1, teme).getPosition()), 7.0e-9);
        Assert.assertEquals(0.0, Vector3D.distance(p3, k1.getPVCoordinates(t3, teme).getPosition()), 3.0e-7);

    }

}
