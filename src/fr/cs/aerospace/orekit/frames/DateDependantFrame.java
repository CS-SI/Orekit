package fr.cs.aerospace.orekit.frames;

import java.util.ArrayList;
import java.util.Iterator;

import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** Base class for date-dependant frames.
 * <p>This class provides support for propagating date changes
 * accross related date-dependant frames that should share the same date.
 * This support is transparent for users who can change the shared date on any
 * single frame in a sharing group, knowing that all other frames in the group
 * will be updated too.</p>
 * @author Luc Maisonobe
 * @see ITRF2000Frame
 */
public abstract class DateDependantFrame extends Frame {

  /** Build the first frame of a date sharing group.
   * @param frame parent (non date-dependant) frame
   * @param date shared date (a <em>new</em> private instance
   * will be built from this date value to ensure proper sharing,
   * further changes in the parameter instance will <em>not</em>
   * automatically update the frames)
   * @see #getDate()
   */
  protected DateDependantFrame(Frame frame, AbsoluteDate date) {
    super(frame, null);
    shared = new SharedDate(date);
    shared.add(this);
  }

  /** Build a frame in an existing date sharing group.
   * @param frame parent (date-dependant) frame
   */
  protected DateDependantFrame(DateDependantFrame frame) {
    super(frame, null);
    shared = frame.shared;
    shared.add(this);
  }

  /** Get the current date shared by all frames in the group.
   * <p>The object returned is an instance of an internal class that
   * ensures proper updates of all frames sharing this date when the
   * instance is changed thanks to a specialized implementation of
   * the {@link AbsoluteDate#dateChanged() dateChanged} method.</p>
   * @return current shared date
   */
  public AbsoluteDate getDate() {
    return shared;
  }

  /** Set the current date shared by all frames in the group.
   * @param date new shared date (the <em>existing</em> private
   * instance will be reset from this date value to ensure proper
   * sharing, further changes in the parameter instance will
   * <em>not</em> automatically update the frames)
   * @see #getDate()
   */
  public void setDate(AbsoluteDate date) {
    shared.reset(date);
  }

  /** Update the frame to the given (shared) date.
   * @param date new value of the shared date
   */
  protected abstract void updateFrame(AbsoluteDate date);

  /** Specialized shared date class. */
  private static class SharedDate extends AbsoluteDate {

    /** Build a shared date without any sharing frame.
     * @param date shared date
     */
    public SharedDate(AbsoluteDate date) {
      super(date);
      this.frames = new ArrayList();
    }

    /** Add a frame to the group.
     * @param frame frame to add
     */
    public void add(DateDependantFrame frame) {
      frames.add(frame);
      frame.updateFrame(this);
    }

    /** Update all frames sharing the date as it has changed.
     */
    public void dateChanged() {
      for (Iterator iterator = frames.iterator(); iterator.hasNext();) {
        ((DateDependantFrame) iterator.next()).updateFrame(this);
      }
    }

    /** Frames belonging to the group. */
    private ArrayList frames;

  }

  /** Shared date. */
  private SharedDate shared;

}
