package org.orekit.estimation.iod;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;

/**
 *
 * Source: http://ccar.colorado.edu/asen5050/projects/projects_2012/kemble/gibbs_derivation.htm
 *
 * @author Joris Olympio
 * @since 7.1
 *
 */
public class IodGoodingTest {

    @Test
    public void testGooding() throws OrekitException
    {
        final Context context = EstimationTestUtils.eccentricContext();

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
        final Vector3D stapos1 = new Vector3D(0,0,0);/*context.stations.get(0)  // FIXME we need to access the station of the measurement
                                    .getBaseFrame()
                                    .getPVCoordinates(date1, frame)
                                    .getPosition();*/
        final Vector3D position1 = new Vector3D(
                                                measurements.get(idMeasure1).getObservedValue()[0],
                                                measurements.get(idMeasure1).getObservedValue()[1],
                                                measurements.get(idMeasure1).getObservedValue()[2]);
        final double r1 = position1.getNorm();
        final Vector3D lineOfSight1 = position1.normalize();

        // measurement data 2
        final int idMeasure2 = 20;
        final AbsoluteDate date2 = measurements.get(idMeasure2).getDate();
        final Vector3D stapos2 = new Vector3D(0,0,0);/*context.stations.get(0)  // FIXME we need to access the station of the measurement
                        .getBaseFrame()
                        .getPVCoordinates(date2, frame)
                        .getPosition();*/
        final Vector3D position2 = new Vector3D(
                                                measurements.get(idMeasure2).getObservedValue()[0],
                                                measurements.get(idMeasure2).getObservedValue()[1],
                                                measurements.get(idMeasure2).getObservedValue()[2]);
        final Vector3D lineOfSight2 = position2.normalize();

        // measurement data 3
        final int idMeasure3 = 40;
        final AbsoluteDate date3 = measurements.get(idMeasure3).getDate();
        final Vector3D stapos3 = new Vector3D(0,0,0);/*context.stations.get(0)  // FIXME we need to access the station of the measurement
                        .getBaseFrame()
                        .getPVCoordinates(date3, frame)
                        .getPosition();*/
        final Vector3D position3 = new Vector3D(
                                                measurements.get(idMeasure3).getObservedValue()[0],
                                                measurements.get(idMeasure3).getObservedValue()[1],
                                                measurements.get(idMeasure3).getObservedValue()[2]);
        final double r3 = position3.getNorm();
        final Vector3D lineOfSight3 = position3.normalize();

        // instantiate the IOD method
        final IodGooding iod = new IodGooding(frame, mu);

        // the problem is very sensitive, and unless one can provide the exact
        // initial range estimate, the estimate may be far off the truth...
        final KeplerianOrbit orbit = iod.estimate(stapos1, stapos2, stapos3,
                                                  lineOfSight1, date1,
                                                  lineOfSight2, date2,
                                                  lineOfSight3, date3,
                                                  r1 * 1.0, r3 * 1.0);
        Assert.assertEquals(orbit.getA(), context.initialOrbit.getA(), 1.0e-6 * context.initialOrbit.getA());
        Assert.assertEquals(orbit.getE(), context.initialOrbit.getE(), 1.0e-6 * context.initialOrbit.getE());
        Assert.assertEquals(orbit.getI(), context.initialOrbit.getI(), 1.0e-6 * context.initialOrbit.getI());

        Assert.assertEquals(13127847.99808, iod.getRange1(), 1.0e-3);
        Assert.assertEquals(13375711.51931, iod.getRange2(), 1.0e-3);
        Assert.assertEquals(13950296.64852, iod.getRange3(), 1.0e-3);

    }
}
