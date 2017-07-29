package org.orekit.frames;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.RealFieldUnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.FieldBracketingNthOrderBrentSolver;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinates;

/**
 * @author junan
 *
 */
public class L2TransformProvider implements TransformProvider {

	/** Serializable UID. */
	private static final long serialVersionUID = 20170725L;

	private final Frame frame1;
	private final Frame outputFrame;
	private final CelestialBody body1;
	private final CelestialBody body2;

	/**
	 * Simple constructor.
	 * @throws OrekitException 
	 */
	public L2TransformProvider(Frame frame, CelestialBody body1, CelestialBody body2) 
			throws OrekitException {
		this.outputFrame = frame;
		this.frame1 = body1.getInertiallyOrientedFrame();
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
	
	public PVCoordinates getL2(AbsoluteDate date) 
			throws OrekitException {
		
		L2Equation equation = new 
				L2Equation(body1.getPVCoordinates(date, frame1),
				body2.getPVCoordinates(date, frame1));
		
		// FieldBracketingNthOrderBrentSolver parameters
		DSFactory dSFactory = new DSFactory(1,2);
		final DerivativeStructure relativeAccuracy = dSFactory.build(1e-3, 1e-3, 1e-3);
        final DerivativeStructure absoluteAccuracy = dSFactory.build(1, 1, 1);
        final DerivativeStructure functionValueAccuracy = dSFactory.build(1, 1, 1);
        final int maximalOrder = 2;
		FieldBracketingNthOrderBrentSolver<DerivativeStructure> solver =
				new FieldBracketingNthOrderBrentSolver<DerivativeStructure>(relativeAccuracy, 
						absoluteAccuracy, functionValueAccuracy, maximalOrder);
		
		//Solver.solve() parameters
		final int maxEval = 1000;
        final DerivativeStructure min = dSFactory.build(-1e10, -1e10, -1e10);
        final DerivativeStructure max = dSFactory.build(1e16, 1e16, 1e16);
        DerivativeStructure dsR = solver.solve(maxEval, equation, min, max, AllowedSolution.ANY_SIDE);
        System.out.println("RESULT:"+dsR.getValue());
        
		// L2 point is built. Result is always given in body1 inertially oriented frame so a transform is needed.
        FieldVector3D<DerivativeStructure> dsRv = new FieldVector3D<DerivativeStructure>(dsR, dSFactory.build(0,0,0), dSFactory.build(0,0,0));
		PVCoordinates l2inFrame1 = new PVCoordinates(dsRv);
		PVCoordinates l2 = frame1.getTransformTo(outputFrame, date).transformPVCoordinates(l2inFrame1);
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
			
			double q = body2.getGM()/body1.getGM();
			DerivativeStructure R = dsP2.subtract(dsP1).getNorm();
			
//			DerivativeStructure lhs1 = r.pow(-2);
//			DerivativeStructure lhs2 = (r.subtract(R)).pow(-2).multiply(q);		
//			DerivativeStructure lhs = lhs1.add(lhs2);
//			
//			DerivativeStructure rhs1 = R.pow(-2);
//			DerivativeStructure rhs2 = R.pow(-3).multiply(r.subtract(R)).multiply(1+q);
//			DerivativeStructure rhs = rhs1.add(rhs2);
//			
//			return lhs.subtract(rhs);
//			
//			return (r.pow(-2).add((r.subtract(R)).pow(-2).multiply(q))).subtract((R.pow(-2).add(R.pow(-3).multiply(r.subtract(R)).multiply(1+q))));
			
//			System.out.println("R value:"+R.getValue());
//			System.out.println("R^2 value:"+R.pow(-2).getValue());
			
			return r.subtract(R.multiply(Math.cbrt(q/3)+1));
		}
	}
	
	
}


