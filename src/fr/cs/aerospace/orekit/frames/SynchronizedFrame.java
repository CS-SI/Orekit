package fr.cs.aerospace.orekit.frames;

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
   * @see #getDate()
   */
  protected SynchronizedFrame(Frame parent, FrameSynchronizer synchronizer,
                              String name) {
    super(parent, null, name); 
    this.synchronizer = synchronizer;
    synchronizer.addFrame(this);
  }

  /** Update the frame to the given date.
   * <p>This method can be called by the instance's {@link FrameSynchronizer
   * FrameSynchronizer} only.</p>
   * @param date new value of the shared date
   */
  protected abstract void updateFrame(AbsoluteDate date);
  
  /** Get the synchronizer of the date sharing group 
   * @return the instance's <code>FrameSynchronizer</code>
   */
  public FrameSynchronizer getFrameSynchronizer(){
	  return synchronizer;
  }
  
  private FrameSynchronizer synchronizer;

}
