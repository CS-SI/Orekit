package fr.cs.aerospace.orekit.forces.maneuvers;

import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.propagation.TimeDerivativesEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;


/** This interface repesents a variable acceleration direction.
 * @see ConstantThrustManeuver
 * @author F. Maussion
 */
public abstract class ThrustForceDirection {
  
  /** Identifier for TNW frame. */
  public static final int TNW = 0;
  
  /** Identifier for QSW frame. */
  public static final int QSW = 1;
  
  /** Identifier for inertial frame. */
  public static final int INERTIAL = 2;
  
  
  /** Simple constructor.
   * @param frameType the frame in which is defined the direction,
   *  must be one of {@link #TNW}, {@link #QSW} or  {@link #INERTIAL}
   */
  protected ThrustForceDirection(int frameType) {
    if (frameType<0||frameType>2) {
      this.frameType = INERTIAL;
    }
    else {
      this.frameType = frameType;
    }
  }
  
  /** Get the frame type ({@link #TNW}, {@link #QSW} or {@link #INERTIAL})
   * @return the frame type
   */
  public int getType() {
    return frameType;
  } 
  
  /** Get the acceleration direction at a specific time and location.
   * @param date the current date
   * @param pvCoordinates the coordinates
   * @param frame the frame in which are defined the coordinates
   * @return the acceleration direction in selected frame
   * @throws OrekitException if some specifice error occurs
   */
  public abstract Vector3D getDirection(AbsoluteDate date, 
                                         PVCoordinates pvCoordinates,
                                           TimeDerivativesEquations adder)
                                             throws OrekitException;
  
  /** The frame type : ({@link #TNW}, {@link #QSW} or {@link #INERTIAL}) */
  private int frameType;
  
}
