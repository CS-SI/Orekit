package fr.cs.aerospace.orekit.forces.maneuvers;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.propagation.SpacecraftState;


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
   * @param currentState current state information : date, cinematics, attitude
   * @return the acceleration direction in selected frame
   * @throws OrekitException if some specific error occurs
   */
  public abstract Vector3D getDirection(SpacecraftState currentState)
                                             throws OrekitException;
  
  /** The frame type : ({@link #TNW}, {@link #QSW} or {@link #INERTIAL}) */
  private int frameType;
  
}
