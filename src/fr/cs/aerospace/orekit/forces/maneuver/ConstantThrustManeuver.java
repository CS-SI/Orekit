/**
 * 
 */
package fr.cs.aerospace.orekit.forces.maneuver;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.ForceModel;
import fr.cs.aerospace.orekit.forces.SWF;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.models.spacecraft.ManeuverSpacecraft;
import fr.cs.aerospace.orekit.propagation.TimeDerivativesEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;


/** This class implements a simple maneuver with constant thrust and direction.
 * 
 * @author F. Maussion
 */
public class ConstantThrustManeuver implements ForceModel {

  /** Simple constructor.
   * @param startDate the instant of ignition
   * @param duration the duration of the thrust (s)
   * @param spacecraft the engine caracteristics
   * @param direction direction of the acceleration in (T,N,W) frame.
   */
  public ConstantThrustManeuver(AbsoluteDate startDate, double duration,
                        ManeuverSpacecraft spacecraft, Vector3D direction) {
    
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
      
    this.spacecraft = spacecraft;
    this.direction = new Vector3D(direction);
    this.direction.normalizeSelf();
    isOff = true;
    
  }
  
    /** Compute the contribution of maneuver to the global acceleration.
    * @param date current date
    * @param pvCoordinates
    * @param adder object where the contribution should be added
    */  
  public void addContribution(AbsoluteDate t, PVCoordinates pvCoordinates,
                              TimeDerivativesEquations adder)
      throws OrekitException {
    
    if(isOff) {
      // do nothing
    }
    else {      
      double acc; 
//      System.out.println(adder.getMass());
//      System.out.println(startDate.minus(t));
      acc = -spacecraft.getOutFlow()*spacecraft.getg0()
                                *spacecraft.getIsp()/adder.getMass();      
      Vector3D acceleration = new Vector3D(acc, direction);
      
      adder.addTNWAcceleration(acceleration.getX(),
                            acceleration.getY(), acceleration.getZ());
      adder.addDotMass(spacecraft.getOutFlow());
    }
  }

  /** Gets the swithching functions related to start and stop passes.
   * @return start /stop switching functions
   */
  public SWF[] getSwitchingFunctions() {
    return new SWF[] { new StartSwitch(), new EndSwitch() };
  }

  /** This class defines the begining of the acceleration switching function.
   * It triggers at the ignition.
   */
  private class StartSwitch implements SWF {

    public void eventOccurred(AbsoluteDate t, PVCoordinates pvCoordinates, Frame frame) {
      isOff = false;
    }

    /** The G-function is the difference between the start date and the currentdate. 
     */
    public double g(AbsoluteDate date, PVCoordinates pvCoordinates, Frame frame)
        throws OrekitException {      
      return startDate.minus(date);
      
    }

    public double getMaxCheckInterval() {
      // we dont wan't to miss the fire
      return duration;
    }

    public double getThreshold() {
      // convergence threshold in seconds
      return 0.1;
    }

  }

  /** This class defines the end of the acceleration switching function.
   * It triggers at the end of the maneuver.
   */
  private class EndSwitch implements SWF {

    public void eventOccurred(AbsoluteDate t, PVCoordinates pvCoordinates, Frame frame) {
      isOff = true;
    }

    /** The G-function is the difference between the end date and the currentdate. 
     */
    public double g(AbsoluteDate date, PVCoordinates pvCoordinates, Frame frame)
        throws OrekitException {      
      return endDate.minus(date);
    }

    public double getMaxCheckInterval() {
      return duration;
    }

    public double getThreshold() {
      // convergence threshold in seconds
      return 0.1;
    }

  }
  
  /** is the engine off? */
  private boolean isOff;
  
  /** start of the maneuver */
  private AbsoluteDate startDate;
  
  /** end of the maneuver */
  private AbsoluteDate endDate;
  
  /** duration (s) */
  private double duration;

  /** The engine caracteristics */
  private ManeuverSpacecraft spacecraft;
  
  /** Direction of the acceleration in (T,N,W) frame */
  private Vector3D direction;
}
