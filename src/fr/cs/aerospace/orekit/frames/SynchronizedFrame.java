package fr.cs.aerospace.orekit.frames;

import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** Base class for date-dependant frames.
 * <p> A <code>SynchronizedFrame</code> must be a member of a date-sharing group, 
 * and kept in stock in a {@link FrameSynchronizer FrameSynchronizer}.</p>
 * 
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @see ITRF2000Frame
 * @see FrameSynchronizer
 */
public abstract class SynchronizedFrame extends Frame {

  /** Build a date dependant frame in a synchronized group, and set up
   * the frame current date to the date of the group.
   * @param frame parent
   * @param fSynch the frame synchronizer which keeps in stock all
   * the frames bounded to the same group
   * @param name the instance name
   * @see #getDate()
   */
  protected SynchronizedFrame(Frame frame, FrameSynchronizer fSynch, String name) {
    super(frame, null, name); 
    this.currentDate = fSynch.getDate();
    this.frameSynch = fSynch;
    fSynch.addFrame(this);
  }

  /** Update the frame to the given date. This method can be called
   * by the instance's FrameSynchronizer only. 
   * @param date new value of the shared date
   */
  protected abstract void updateFrame(AbsoluteDate date);
  
  /** Get the current date of the instance
   * @return the current date
   */
  public AbsoluteDate getDate(){
	  return currentDate;
  }
  
  /** Get the synchronizer of the date sharing group 
   * @return the instance's <code>FrameSynchronizer</code>
   */
  public FrameSynchronizer getFrameSynchronizer(){
	  return frameSynch;
  }
  
  private AbsoluteDate currentDate;
  
  private FrameSynchronizer frameSynch;
}
