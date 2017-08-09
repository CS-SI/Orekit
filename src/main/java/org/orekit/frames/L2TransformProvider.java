package org.orekit.frames;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.RealFieldUnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.FieldBracketingNthOrderBrentSolver;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinates;

/**L2 Transform provider for a frame on the L2 Lagrange point of two celestial bodies.
 * 
 * @author Luc Maisonabe
 * @author Julio Hernanz
 */

public class L2TransformProvider implements TransformProvider {

  /** Serializable UID.*/
  private static final long serialVersionUID = 20170725L;

  private final Frame frame;
  private final CelestialBody primaryBody;
  private final CelestialBody secondaryBody;
  
  /** Simple constructor.
  * @param body1 Primary body.
  * @param body2 Secondary body.
  * @throws OrekitException in .getInertiallyOrientedFrame() if frame cannot be retrieved
  */
  public L2TransformProvider(CelestialBody body1, CelestialBody body2) 
      throws OrekitException {
    this.primaryBody = body1;
    this.secondaryBody = body2;
    this.frame = primaryBody.getInertiallyOrientedFrame();
  }

  @Override
  public Transform getTransform(AbsoluteDate date) 
      throws OrekitException {
    return new Transform(date, getL2(date));
  }


  @Override
  public <T extends RealFieldElement<T>> FieldTransform<T>
      getTransform(FieldAbsoluteDate<T> date)
        throws OrekitException {
    // TODO Auto-generated method stub
    return null;
  }
  
  /** Method to get the {@link PVCoordinates} of the L2 point.
  * @param date current date
  * @return PVCoordinates of the L2 point given in frame: primaryBody.getInertiallyOrientedFrame()
  * @throws OrekitException if some frame specific error occurs at .getTransformTo()
  */
  public PVCoordinates getL2(AbsoluteDate date) 
      throws OrekitException {

    final PVCoordinates pv21 = secondaryBody.getPVCoordinates(date, frame);
    final FieldVector3D<DerivativeStructure> delta = pv21.toDerivativeStructureVector(2);
    final double q = secondaryBody.getGM() / primaryBody.getGM(); // Mass ratio
      
    L2Equation equation = new L2Equation(delta.getNorm(),q);

    // FieldBracketingNthOrderBrentSolver parameters
    DSFactory dsFactory = delta.getX().getFactory();
    final DerivativeStructure relativeAccuracy = dsFactory.constant(1e-14);
    final DerivativeStructure absoluteAccuracy = dsFactory.constant(1e-3); // i.e. 1mm 
    final DerivativeStructure functionValueAccuracy = dsFactory.constant(0);
    final int maximalOrder = 2;
    FieldBracketingNthOrderBrentSolver<DerivativeStructure> solver =
                    new FieldBracketingNthOrderBrentSolver<DerivativeStructure>(relativeAccuracy, 
                                    absoluteAccuracy, functionValueAccuracy, maximalOrder);
    final int maxEval = 1000;

    // Approximate position of L2 point, valid when m2 << m1
    final DerivativeStructure bigR = delta.getNorm();
    DerivativeStructure baseR      = bigR.multiply(FastMath.cbrt(q / 3) + 1);
    
    // We build the startValue of the solver method with an approximation
    final double deviationFromApprox = 0.1;
    final DerivativeStructure min = baseR.multiply(1 - deviationFromApprox);
    final DerivativeStructure max = baseR.multiply(1 + deviationFromApprox);
    DerivativeStructure dsR = solver.solve(maxEval, equation, min, max, AllowedSolution.ANY_SIDE);
       
    // L2 point is built
    return new PVCoordinates(new FieldVector3D<DerivativeStructure>(dsR, 
                   delta.normalize()));
  }

  private class L2Equation implements
      RealFieldUnivariateFunction<DerivativeStructure> {

    private final DerivativeStructure delta;
    private final double massRatio;

    L2Equation(DerivativeStructure delta, double q) 
                      throws OrekitException {
      this.delta = delta;
      this.massRatio = q;
    }

    public DerivativeStructure value(DerivativeStructure r) {

      // Left hand side
      DerivativeStructure lhs1 = r.multiply(r).reciprocal();
      DerivativeStructure rminusDelta = r.subtract(delta);
      DerivativeStructure lhs2 = (rminusDelta).multiply(rminusDelta).reciprocal()
                      .multiply(massRatio);
      DerivativeStructure lhs = lhs1.add(lhs2);

      // Right hand side
      DerivativeStructure rhs1 = delta.multiply(delta).reciprocal();
      DerivativeStructure rhs2 = rhs1.divide(delta).multiply(rminusDelta).multiply(1 + massRatio);
      DerivativeStructure rhs = rhs1.add(rhs2);

      // lhs-rhs = 0
      return lhs.subtract(rhs);
    }
  }


}
