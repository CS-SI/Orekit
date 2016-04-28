package org.orekit.estimation.iod;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;

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
	public void testGibbs() throws OrekitException {
	    final Context context = EstimationTestUtils.eccentricContext();
        
        final double mu = context.initialOrbit.getMu();
		final Frame frame = context.initialOrbit.getFrame();
		
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);
        
        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        
        final List<Measurement<?>> measurements =
                EstimationTestUtils.createMeasurements(propagator,
                                                       new PVMeasurementCreator(),
                                                       0.0, 1.0, 60.0);
        
		final Vector3D position1 = new Vector3D(
				measurements.get(0).getObservedValue()[0], 
				measurements.get(0).getObservedValue()[1],
				measurements.get(0).getObservedValue()[2]);
		final PV pv1 = new PV(measurements.get(0).getDate(), position1, Vector3D.ZERO, 0., 0., 1.);

		final Vector3D position2 = new Vector3D(
				measurements.get(1).getObservedValue()[0], 
				measurements.get(1).getObservedValue()[1],
				measurements.get(1).getObservedValue()[2]);
		final PV pv2 = new PV(measurements.get(1).getDate(), position2, Vector3D.ZERO, 0., 0., 1.);
		
		final Vector3D position3 = new Vector3D(
				measurements.get(2).getObservedValue()[0], 
				measurements.get(2).getObservedValue()[1],
				measurements.get(2).getObservedValue()[2]);
		final PV pv3 = new PV(measurements.get(2).getDate(), position3, Vector3D.ZERO, 0., 0., 1.);
		
        // instantiate the IOD method		
		final IodGibbs gibbs = new IodGibbs(mu);
		final KeplerianOrbit orbit = gibbs.estimate(frame, pv1, pv2, pv3);
				
		Assert.assertEquals(orbit.getA(), context.initialOrbit.getA(), 1.0e-9 * context.initialOrbit.getA());
		Assert.assertEquals(orbit.getE(), context.initialOrbit.getE(), 1.0e-9 * context.initialOrbit.getE());
		Assert.assertEquals(orbit.getI(), context.initialOrbit.getI(), 1.0e-9 * context.initialOrbit.getI());
	}
	
}
