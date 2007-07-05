package fr.cs.aerospace.orekit.propagation;

import org.apache.commons.math.ode.DerivativeException;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.time.AbsoluteDate;



/** This interface represents an interpolator over the last step during a propagation.
 * This interface is designed to provide tools for the user to manage and react during
 * propagation. The {@link StepHandler} can use these objects to retrieve the state at 
 * intermediate times between the previous and the current grid points (this feature is 
 * often called dense output).
 */
public interface StepInterpolator {

  
  /** Get the previous grid point date
   * @return Get the previous grid point date
   */
  public AbsoluteDate getPreviousDate();

  /** Get the current grid point date.
   * @return current grid point date;
   */
  public AbsoluteDate getCurrentDate();
  
  /** Get the date of the interpolated point. {@link #setInterpolatedDate(AbsoluteDate)}
   *  has not been called, it returns the current grid point date.
   * @return interpolation point date;
   */
  public AbsoluteDate getInterpolatedDate();

  /** Set the date of the interpolated point.
   *  Setting the date outside of the current step is now allowed (it was not 
   *  allowed up to version 5.4 of Mantissa), but should be used with care since 
   *  the accuracy of the interpolator will probably be very poor far from this step.
   *  This allowance has been added to simplify implementation of search algorithms 
   *  near the step endpoints.
   * @param date date of the interpolated point 
   * @throws DerivativeException if this call induces an automatic step
   *  finalization that throws one
   */
  public void setInterpolatedDate(AbsoluteDate date)
                           throws DerivativeException;

  /** Get the state of the interpolated point.
   * @return state at date {@link #getInterpolatedDate()}
   * @throws OrekitException 
   */
  public SpacecraftState getInterpolatedState() throws OrekitException;

    
  /** Check if the natural integration direction is forward.
   * This method provides the integration direction as specified by the 
   * integrator itself, it avoid some nasty problems in degenerated cases
   * like null steps due to cancellation at step initialization, step control 
   * or switching function triggering.
   * 
   * @return true if the integration variable (date) increases during integration
   */
  public boolean isForward();
  
}
