package fr.cs.orekit.forces.maneuvers;

import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.Translator;
import fr.cs.orekit.forces.ForceModel;
import fr.cs.orekit.forces.SWF;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.TimeDerivativesEquations;
import fr.cs.orekit.time.AbsoluteDate;


/** This class implements a simple maneuver with constant thrust.
 * 
 * @author F. Maussion
 */
public class ConstantThrustManeuver implements ForceModel {

  /** Identifier for TNW frame. */
  public static final int TNW = 0;

  /** Identifier for QSW frame. */
  public static final int QSW = 1;

  /** Identifier for inertial frame. */
  public static final int INERTIAL = 2;

  /** Simple constructor for a constant direction and constant thrust.
   * @param startDate the instant of ignition
   * @param duration the duration of the thrust (s)
   * @param force the thrust force (N)
   * @param isp the Isp (s)
   * @param direction the acceleration direction in choosed frame.
   * @param frameType the frame in which is defined the direction,
   *  must be one of {@link #TNW}, {@link #QSW} or  {@link #INERTIAL}
   */
  public ConstantThrustManeuver(AbsoluteDate startDate, double duration,
                                double force, double isp, Vector3D direction, int frameType) {

    if (duration>=0) {
      this.startDate = startDate;
      this.endDate = new AbsoluteDate(startDate , duration);
      this.duration = duration;
    }
    else {
      this.endDate = startDate;
      this.startDate = new AbsoluteDate(startDate , duration);
      this.duration = - duration;
    }
    this.force = force;
    this.flowRate = -force/(g0*isp);
    this.direction = direction.normalize();
    this.frameType = frameType;
    firing = false;     
  }

  /** Constructor for a variable direction and constant thrust.
   * @param startDate the instant of ignition
   * @param duration the duration of the thrust (s)
   * @param force the thrust force (N)
   * @param isp the Isp (s)
   * @param direction the variable acceleration direction.
   */
  public ConstantThrustManeuver(AbsoluteDate startDate, double duration,
                                double force, double isp, ThrustForceDirection direction) {

    this(startDate, duration, force, isp, null, direction.getType());
    this.variableDir = direction;   
  }

  /** Compute the contribution of maneuver to the global acceleration.
   * @param s the current state information : date, cinematics, attitude
   * @param adder object where the contribution should be added
   * @param mu central gravitation coefficient
   * @throws OrekitException if some specific error occurs
   */  
  public void addContribution(SpacecraftState s, TimeDerivativesEquations adder, double mu)
  throws OrekitException {
    if(firing) {      
      if (variableDir!=null) {
        direction = variableDir.getDirection(s).normalize();
      }

      double acc = force/s.getMass();        
      Vector3D acceleration = new Vector3D(acc, direction);

      switch (frameType) {
      case TNW :
        adder.addTNWAcceleration(acceleration.getX(),
                                 acceleration.getY(), acceleration.getZ());
        break;
      case QSW :
        adder.addQSWAcceleration(acceleration.getX(),
                                 acceleration.getY(), acceleration.getZ());
        break;
      case INERTIAL :
        adder.addXYZAcceleration(acceleration.getX(),
                                 acceleration.getY(), acceleration.getZ());
        break;
      default :
        throw new IllegalArgumentException(Translator.getInstance().translate(
        "Choosen frame type is not correct"));
      }            
      adder.addMassDerivative(flowRate);
    }

  }

  /** Gets the swithching functions related to start and stop passes.
   * @return start / stop switching functions
   */
  public SWF[] getSwitchingFunctions() {
    return new SWF[] { new StartSwitch(), new EndSwitch() };
  }

  /** This class defines the begining of the acceleration switching function.
   * It triggers at the ignition.
   */
  private class StartSwitch implements SWF {

    public void eventOccurred(SpacecraftState s, double mu) {
      firing = true;
    }

    /** The G-function is the difference between the start date and the current date. 
     * @param s the current state information : date, cinematics, attitude
     * @param mu central gravitation coefficient
     */
    public double g(SpacecraftState s, double mu)
    throws OrekitException {
      return startDate.minus(s.getDate());

    }

    public double getMaxCheckInterval() {
      return duration;
    }

    public double getThreshold() {
      // convergence threshold in seconds
      return 1.0e-4;
    }

    private static final long serialVersionUID = -3929595470907786248L;

  }

  /** This class defines the end of the acceleration switching function.
   * It triggers at the end of the maneuver.
   */
  private class EndSwitch implements SWF {

    public void eventOccurred(SpacecraftState s, double mu) {
      firing = false;
    }

    /** The G-function is the difference between the end date and the currentdate. 
     * @param s the current state information : date, cinematics, attitude
     * @param mu central gravitation coefficient
     */
    public double g(SpacecraftState s, double mu)
    throws OrekitException {   
      return endDate.minus(s.getDate());
    }

    public double getMaxCheckInterval() {
      return duration;
    }

    public double getThreshold() {
      // convergence threshold in seconds
      return 1.0e-4;
    }

    private static final long serialVersionUID = 2437223790185987916L;

  }

  /** state of the engine */
  private boolean firing;

  /** Frame type */
  private int frameType;

  /** start of the maneuver */
  private AbsoluteDate startDate;

  /** end of the maneuver */
  private AbsoluteDate endDate;

  /** duration (s) */
  private double duration;

  /** The engine caracteristics */
  private double force;
  private double flowRate;

  /** Direction of the acceleration in selected frame */
  private Vector3D direction;

  private ThrustForceDirection variableDir;

  /** Reference gravity acceleration constant (m/s²) */
  private static final double g0 = 9.80665;

}
