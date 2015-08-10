package org.orekit.orbits;

import java.util.Collection;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/** This class holds cartesian orbital parameters, with the particularity that no
 * central body is defined ({@code mu} = 0).

 * <p>
 * The parameters used internally are the cartesian coordinates:
 *   <ul>
 *     <li>x</li>
 *     <li>y</li>
 *     <li>z</li>
 *     <li>xDot</li>
 *     <li>yDot</li>
 *     <li>zDot</li>
 *   </ul>
 * contained in {@link PVCoordinates}.
 * </p>

 * <p>
 * Note that, due to the absence of central body, no non-cartesian related computation
 * is available. The underlying instance of the {@link EquinoctialOrbit} class is never
 * initialized.
 * </p>
 * <p>
 * The instance <code>CartesianOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see    Orbit
 * @see    KeplerianOrbit
 * @see    CircularOrbit
 * @see    EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Guillaume Obrecht
 */
public class CartesianOrbitWithoutCentralBody extends CartesianOrbit {

	/** TODO Serializable UID*/
	
//    /** Underlying equinoctial orbit to which high-level methods are delegated. */
//    private transient EquinoctialOrbit equinoctial;
	

    
    /** Constructor from Cartesian parameters.
    *
    * <p> The acceleration provided in {@code pvCoordinates} is accessible using
    * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. 
//    * All other methods
//    * use {@code mu} and the position to compute the acceleration, including
//    * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
    *
    * @param pvaCoordinates the position, velocity and acceleration of the satellite.
    * @param frame the frame in which the {@link PVCoordinates} are defined
    * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
    * @exception IllegalArgumentException if frame is not a {@link
    * Frame#isPseudoInertial pseudo-inertial frame}
    */
   public CartesianOrbitWithoutCentralBody(final TimeStampedPVCoordinates pvaCoordinates,
                         final Frame frame)
       throws IllegalArgumentException {
       super(pvaCoordinates, frame, 0);
   }

   /** Constructor from Cartesian parameters.
    *
    * <p> The acceleration provided in {@code pvCoordinates} is accessible using
    * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. 
//    * All other methods
//    * use {@code mu} and the position to compute the acceleration, including
//    * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
    *
    * @param pvaCoordinates the position and velocity of the satellite.
    * @param frame the frame in which the {@link PVCoordinates} are defined
    * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
    * @param date date of the orbital parameters
    * @exception IllegalArgumentException if frame is not a {@link
    * Frame#isPseudoInertial pseudo-inertial frame}
    */
   public CartesianOrbitWithoutCentralBody(final PVCoordinates pvaCoordinates, final Frame frame,
                         final AbsoluteDate date)
       throws IllegalArgumentException {
       this(new TimeStampedPVCoordinates(date, pvaCoordinates), frame);
   }

//   /** Constructor from any kind of orbital parameters.
//    * @param op orbital parameters to copy
//    * TODO: Check!!!
//    */
//   public CartesianOrbitWithoutCentralBody(final Orbit op) {
//       super(op.getPVCoordinates(), op.getFrame(), op.getMu());
//       System.out.print("Constructor3\n");
//       if (op instanceof EquinoctialOrbit) {
//           equinoctial = (EquinoctialOrbit) op;
//       } else if (op instanceof CartesianOrbit) {
//           equinoctial = ((CartesianOrbit) op).equinoctial;
//       } else {
//           equinoctial = null;
//       }
//   }


/** {@inheritDoc} */
public OrbitType getType() {
	return OrbitType.CARTESIAN;
}

/** {@inheritDoc} 
 *  As the notion of semi-major axis does not exist for orbit with no central body, this method
 *  throws an exception.
 *  */
public double getA() throws IllegalArgumentException {
	throw OrekitException.createIllegalArgumentException(
			OrekitMessages.METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY );
}

/** {@inheritDoc} 
 *  As the notion of eccentricity does not exist for orbit with no central body, this method
 *  throws an exception.
 *  */
public double getE() throws IllegalArgumentException {
	throw OrekitException.createIllegalArgumentException(
			OrekitMessages.METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY );
}

///** {@inheritDoc} */
//public double getI() {
//    return Vector3D.angle(Vector3D.PLUS_K, getPVCoordinates().getMomentum());
//}

/** {@inheritDoc} 
 * As orbits without central body have no underlying equinoctial orbit, this method
 *  throws an exception.
 *  */
public double getEquinoctialEx() throws IllegalArgumentException {
	throw OrekitException.createIllegalArgumentException(
			OrekitMessages.METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY );
}

/** {@inheritDoc} 
 * As orbits without central body have no underlying equinoctial orbit, , this method
 *  throws an exception.
 *  */
public double getEquinoctialEy() throws IllegalArgumentException {
	throw OrekitException.createIllegalArgumentException(
			OrekitMessages.METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY );
}

/** {@inheritDoc} 
 * As orbits without central body have no underlying equinoctial orbit, , this method
 *  throws an exception.
 *  */
public double getHx() throws IllegalArgumentException {
	throw OrekitException.createIllegalArgumentException(
			OrekitMessages.METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY );
}


/** {@inheritDoc} 
 * As orbits without central body have no underlying equinoctial orbit, , this method
 *  throws an exception.
 *  */
public double getHy() throws IllegalArgumentException {
	throw OrekitException.createIllegalArgumentException(
			OrekitMessages.METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY );
}

/** {@inheritDoc} 
 * As orbits without central body have no underlying equinoctial orbit, , this method
 *  throws an exception.
 *  */
public double getLv() throws IllegalArgumentException {
	throw OrekitException.createIllegalArgumentException(
			OrekitMessages.METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY );
}

/** {@inheritDoc} 
 * As orbits without central body have no underlying equinoctial orbit, , this method
 *  throws an exception.
 *  */
public double getLE() throws IllegalArgumentException {
	throw OrekitException.createIllegalArgumentException(
			OrekitMessages.METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY );
}

/** {@inheritDoc} 
 * As orbits without central body have no underlying equinoctial orbit, , this method
 *  throws an exception.
 *  */
public double getLM() throws IllegalArgumentException {
	throw OrekitException.createIllegalArgumentException(
			OrekitMessages.METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY );
}

///** {@inheritDoc} */
//protected TimeStampedPVCoordinates initPVCoordinates() {
////	System.out.print("initPVCoordinates\n");
//	// nothing to do here, as the canonical elements are already the cartesian ones
//    return getPVCoordinates();
//}

/** {@inheritDoc}  */
public CartesianOrbitWithoutCentralBody shiftedBy(double dt) {	
	final PVCoordinates shiftedPV = getPVCoordinates().shiftedBy(dt);	
	return new CartesianOrbitWithoutCentralBody(shiftedPV, getFrame(), getDate().shiftedBy(dt));
}

// TODO: Check that interpolate from CartesianOrbit still works!!!

///** {@inheritDoc}
// * TODO: Create function!
// */
//public CartesianOrbitWithoutCentralBody interpolate(AbsoluteDate date, Collection<Orbit> sample)
//		throws OrekitException {
////	System.out.print("interpolate\n");
//	// TODO Auto-generated method stub
//	return null;
//}

//@Override
//public void getJacobianWrtCartesian(final PositionAngle type, final double[][] jacobian) {
////	System.out.print("getJacobianWrtCartesian\n");
//    // this is the fastest way to set the 6x6 identity matrix
//    for (int i = 0; i < 6; i++) {
//        for (int j = 0; j < 6; j++) {
//            jacobian[i][j] = 0;
//        }
//        jacobian[i][i] = 1;
//    }
//}

//@Override
//protected double[][] computeJacobianMeanWrtCartesian() {
//    // not used
//    return null;
//}
//@Override
//protected double[][] computeJacobianEccentricWrtCartesian() {
//    // not used
//    return null;
//}
//
//@Override
//protected double[][] computeJacobianTrueWrtCartesian() {
//    // not used
//    return null;
//}


/** {@inheritDoc} 
 * Note: No contribution is added to the acceleration due to the absence of central body.
 * This method here simply adds the position derivative as being the velocity.
 */
public void addKeplerContribution(final PositionAngle type, final double gm,
                                  final double[] pDot) {
    final PVCoordinates pv = getPVCoordinates();

    // position derivative is velocity  
    final Vector3D velocity = pv.getVelocity();
    pDot[0] += velocity.getX();
    pDot[1] += velocity.getY();
    pDot[2] += velocity.getZ();

    // no velocity derivative for mu = 0
}

///**  Returns a string representation of this Orbit object.
// * @return a string representation of this object
// */
//public String toString() {
//    return "cartesian parameters: " + getPVCoordinates().toString();
//}

/** TODO writeReplace */

/** TODO DTO class */
}
