package org.orekit.estimation.iod;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

/**
 *
 * Source: http://ccar.colorado.edu/asen5050/projects/projects_2012/kemble/gibbs_derivation.htm
 *
 * @author Joris Olympio
 * @since 7.1
 *
 */
public class IodGibbsTest {

    @Test
    public void testGibbs1() throws OrekitException {
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

        final Vector3D position1 = new Vector3D(measurements.get(0).getObservedValue()[0],
                                                measurements.get(0).getObservedValue()[1],
                                                measurements.get(0).getObservedValue()[2]);
        final PV pv1 = new PV(measurements.get(0).getDate(), position1, Vector3D.ZERO, 0., 0., 1.);

        final Vector3D position2 = new Vector3D(measurements.get(1).getObservedValue()[0],
                                                measurements.get(1).getObservedValue()[1],
                                                measurements.get(1).getObservedValue()[2]);
        final PV pv2 = new PV(measurements.get(1).getDate(), position2, Vector3D.ZERO, 0., 0., 1.);

        final Vector3D position3 = new Vector3D(measurements.get(2).getObservedValue()[0],
                                                measurements.get(2).getObservedValue()[1],
                                                measurements.get(2).getObservedValue()[2]);
        final PV pv3 = new PV(measurements.get(2).getDate(), position3, Vector3D.ZERO, 0., 0., 1.);

        // instantiate the IOD method		
        final IodGibbs gibbs = new IodGibbs(mu);
        final KeplerianOrbit orbit = gibbs.estimate(frame, pv1, pv2, pv3);

        Assert.assertEquals(context.initialOrbit.getA(), orbit.getA(), 1.0e-9 * context.initialOrbit.getA());
        Assert.assertEquals(context.initialOrbit.getE(), orbit.getE(),  1.0e-9 * context.initialOrbit.getE());
        Assert.assertEquals(context.initialOrbit.getI(),  orbit.getI(), 1.0e-9 * context.initialOrbit.getI());
    }

    @Test
    public void testGibbs2() throws OrekitException {

        // test extracted from "Fundamentals of astrodynamics & applications", D. Vallado, 3rd ed, chap Initial Orbit Determination, Exple 7-3, p457

        //extraction of the context.
        final Context context = EstimationTestUtils.eccentricContext();
        final double mu = context.initialOrbit.getMu();

        //initialisation
        final IodGibbs gibbs = new IodGibbs(mu);

        // Observation  vector (EME2000)
        final Vector3D posR1= new Vector3D(0.0, 0.0, 6378137.0);
        final Vector3D posR2= new Vector3D(0.0, -4464696.0, -5102509.0);
        final Vector3D posR3= new Vector3D(0.0, 5740323.0, 3189068);

        //epoch corresponding to the observation vector
        AbsoluteDate dateRef = new AbsoluteDate(2000,01, 01, 0,0,0, TimeScalesFactory.getUTC());
        AbsoluteDate date2 = dateRef.shiftedBy(76.48);
        AbsoluteDate date3 = dateRef.shiftedBy(153.04);

        // Reference result (cf. Vallado)
        final Vector3D velR2 = new Vector3D(0.0, 5531.148, -5191.806);

        //Gibbs IOD
        final KeplerianOrbit orbit = gibbs.estimate(FramesFactory.getEME2000(),
                                                    posR1, dateRef, posR2, date2, posR3, date3);

        //test
        Assert.assertEquals(0.0, orbit.getPVCoordinates().getVelocity().getNorm() - velR2.getNorm(), 1e-3);
    }

    @Test
    public void testGibbs3() throws OrekitException {

        // test extracted from "Fundamentals of astrodynamics & applications", D. Vallado, 3rd ed, chap Initial Orbit Determination, Exple 7-4, p463
        // Remark: the test value in Vallado is performed with an Herrick-Gibbs methods but results are very close with Gibbs method.

        //extraction of context
        final Context context = EstimationTestUtils.eccentricContext();
        final double mu = context.initialOrbit.getMu();

        //Initialisation
        final IodGibbs gibbs = new IodGibbs(mu);	

        // Observations vector (EME2000)
        final Vector3D posR1 = new Vector3D(3419855.64, 6019826.02, 2784600.22);
        final Vector3D posR2 = new Vector3D(2935911.95, 6326183.24, 2660595.84);
        final Vector3D posR3 = new Vector3D(2434952.02, 6597386.74, 2521523.11);

        //epoch corresponding to the observation vector
        AbsoluteDate dateRef = new AbsoluteDate(2000,01, 01, 0,0,0, TimeScalesFactory.getUTC());
        AbsoluteDate date2 = dateRef.shiftedBy(76.48);
        AbsoluteDate date3 = dateRef.shiftedBy(153.04);

        // Reference result
        final Vector3D velR2 = new Vector3D(-6441.632, 3777.625, -1720.582);

        //Gibbs IOD
        final KeplerianOrbit orbit = gibbs.estimate(FramesFactory.getEME2000(),
                                                    posR1, dateRef, posR2, date2, posR3, date3);

        //test for the norm of the velocity
        Assert.assertEquals(0.0, orbit.getPVCoordinates().getVelocity().getNorm() - velR2.getNorm(),  1e-3);

    }

}
