package fr.cs.aerospace.orekit.attitudes;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.ForceModel;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.propagation.AttitudePropagator;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** This interface calculates the attitude of a space system.
 * 
 * <p> It returns the Attitude representation at given time and position.
 *  This interface will be used as a parameter to represent a state during 
 *  numerical propagation, as some {@link ForceModel force models} need it.<p>
 *  
 *  <p> Orekit will propose several implemented attitude interfaces, providing 
 *  specific and perfectly automatised attitudes, as they do not consider for 
 *  the moment the perturbing couples, the captors and spacecraft dynamic.</p> 
 *  
 * @see AttitudePropagator#setAkProvider(AttitudeKinematicsProvider)
 * 
 * @author F. Maussion
 */
public interface AttitudeKinematicsProvider {

  /** Get the attitude representation in the selected frame.
   * @param date the current date
   * @param pv the coordinates in the inertial frame
   * @param frame the inertial frame in which are defined the coordinates
   * @return the attitude representation of the spacecraft
   * @throws OrekitException if some specific error occurs.
   */
  public AttitudeKinematics getAttitudeKinematics(AbsoluteDate date,
                                                  PVCoordinates pv, Frame frame)
      throws OrekitException;

}
