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
public class IodLambertTest {

    @Test
    public void testLambert() throws OrekitException {
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

}
