package org.orekit.frames;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.RealFieldUnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.FieldBracketingNthOrderBrentSolver;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinates;

public class L2TransformProvider implements TransformProvider {

	/** Serializable UID. */
	private static final long serialVersionUID = 20170725L;

	private final Frame frame;
	private final CelestialBody body1;
	private final CelestialBody body2;

	/**
	 * Simple constructor.
	 */
	public L2TransformProvider(Frame frame, CelestialBody body1, CelestialBody body2) {
		this.frame = frame;
		this.body1 = body1;
		this.body2 = body2;
	}

	@Override
	public Transform getTransform(AbsoluteDate date) 
			throws OrekitException {
		return new Transform(date, getL2(date));
	}
	
	@Override
	public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(FieldAbsoluteDate<T> date)
			throws OrekitException {
		// TODO Auto-generated method stub
		return null;
	}
	
	private PVCoordinates getL2(AbsoluteDate date) 
			throws OrekitException {
		L2Equation equation = new 
				L2Equation(body1.getPVCoordinates(date, frame),
				body2.getPVCoordinates(date, frame));
		// FieldBracketingNthOrderBrentSolver parameters
		DSFactory dSFactory = new DSFactory(1,2);
		final DerivativeStructure relativeAccuracy = dSFactory.build(0.1,0.1,0.1);
        final DerivativeStructure absoluteAccuracy = dSFactory.build(1e4,1e4, 1e4);
        final DerivativeStructure functionValueAccuracy = dSFactory.build(1e4,1e4, 1e4);
        final int maximalOrder = 2;
		FieldBracketingNthOrderBrentSolver<DerivativeStructure> solver =
				new FieldBracketingNthOrderBrentSolver<DerivativeStructure>(relativeAccuracy, 
						absoluteAccuracy, functionValueAccuracy, maximalOrder);
		//Solver.solve() parameters
		final int maxEval = 10;
        final DerivativeStructure min = dSFactory.build(-1e15, -1e15, -1e15);
        final DerivativeStructure max = dSFactory.build(1e5, 1e5, 1e5);
		DerivativeStructure dsR = solver.solve(maxEval, equation, min, max, AllowedSolution.ANY_SIDE);
		
		// use dsR to build the L2 point
		PVCoordinates l2 = new PVCoordinates(new FieldVector3D<DerivativeStructure>(dsR, new Vector3D(0,0)));
		return l2;
	}
	
	private class L2Equation implements
	RealFieldUnivariateFunction<DerivativeStructure> {

		private final FieldVector3D<DerivativeStructure> dsP1;
		private final FieldVector3D<DerivativeStructure> dsP2;
		
		L2Equation(PVCoordinates pv1, PVCoordinates pv2) 
				throws OrekitException {
			this.dsP1 = pv1.toDerivativeStructureVector(2);
			this.dsP2 = pv2.toDerivativeStructureVector(2);
		}
		
		public DerivativeStructure value(DerivativeStructure r) {
			
			double m1 = body1.getGM();
			double m2 = body2.getGM();
			DerivativeStructure R = dsP2.subtract(dsP1).getNorm();
			
			DerivativeStructure lhs1 = r.add(R).pow(-2).multiply(m1);
			DerivativeStructure lhs2 = r.pow(-2).multiply(m2);		
			DerivativeStructure lhs = lhs1.add(lhs2);
			
			DerivativeStructure rhs1 = R.pow(-2).multiply(m1);
			DerivativeStructure rhs2 = R.pow(-3).multiply(r).multiply(m1+m2);
			DerivativeStructure rhs = rhs1.add(rhs2);
			
			return lhs.subtract(rhs);
		}
	}
	
	
}


