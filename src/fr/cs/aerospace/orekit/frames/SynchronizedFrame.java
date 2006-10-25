package fr.cs.aerospace.orekit.frames;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** Base class for date-dependant frames.
 * <p> A <code>SynchronizedFrame</code> is a {@link Frame frame} that
 * shares its date with other instances thanks to a {@link FrameSynchronizer
 * frame synchronizer}. This synchronization is important when combining
 * transforms between frames.</p>
 * 
 * @author Fabien Maussion
 * @see ITRF2000Frame
 * @see FrameSynchronizer
 */
public abstract class SynchronizedFrame extends Frame {

  /** Build a date dependant frame in a synchronized group, and set up
   * the frame current date to the date of the group.
   * <p>The transform between the built instance and its parent is <em>not</em>
   * specified as an argument. It will be set up by the synchronizer which
   * will update the frame to the current date as soon as it is built.</p>
   * @param parent parent frame
   * @param synchronizer the frame synchronizer which handles all
   * the frames bound to the same group
   * @param name the instance name
   * @exception OrekitException if some frame specific error occurs
   */
  protected SynchronizedFrame(Frame parent, FrameSynchronizer synchronizer,
                              String name)
    throws OrekitException {
    super(parent, null, name); 
    this.synchronizer = synchronizer;
    synchronizer.addFrame(this);
  }

  /** Get the transform from the instance to another frame at a specific date.
   * <p> It is important to specify the date so the method will call its 
   * {@link FrameSynchronizer} to update all the frames of the instance's 
   * date-sharing group.   
   * </p>
   * @param destination destination frame to which we want to transform vectors
   * @param date the date when has to be calculated the transform. 
   * @return transform from the instance to the destination frame
   */
  public Transform getTransformTo(Frame destination, AbsoluteDate date) throws OrekitException {
    synchronizer.setDate(date);
    return this.getTransformTo(destination);
  }
  
  /** Update the frame to the given date.
   * <p>This method can be called by the instance's {@link FrameSynchronizer
   * FrameSynchronizer} only.</p>
   * @param date new value of the shared date
   * @exception OrekitException if some frame specific error occurs
   */
  protected abstract void updateFrame(AbsoluteDate date)
    throws OrekitException;

  private FrameSynchronizer synchronizer;

}
