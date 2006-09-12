package fr.cs.aerospace.orekit.frames;

import java.util.ArrayList;

import fr.cs.aerospace.orekit.time.AbsoluteDate;

/**This class provides support for propagating date changes
 * accross related date-dependant frames that should share the same date.
 * This support is transparent for users who can change the shared date in 
 * the <code>FrameSynchronizer</code>, knowing that all other frames in the group
 * will be updated too.
 * 
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @see SynchronizedFrame
 */
public class FrameSynchronizer {
	
	/** Build a new and empty date-sharing group. 
	 * @param date the initial date
	 */
	public FrameSynchronizer(AbsoluteDate date) {
		array = new ArrayList();
		currentDate = date;		
	}
	
	/** Adds a new frame in the group. This method is 
	 * called by the just created SynchronizedFrame instance, and 
	 * updates immediatly the frame. 
	 * @param the frame to add
	 */
	protected void addFrame(SynchronizedFrame frame) {
		array.add(frame);
		frame.updateFrame(currentDate);
	}
	
	/** Changes the current date of the synchronizer and updates 
	 * all the frames in the group.
	 * @param date the new date
	 */
	public void setDate(AbsoluteDate date) {
		this.currentDate = date;
		for (int i = 0; i < array.size(); i++) {
			((SynchronizedFrame) array.get(i)).updateFrame(date);
		}
	}
	
	/** Get the date.
	 * @return the current date.
	 */
	public AbsoluteDate getDate() {
		return currentDate;
	}
	
	/** List of frames handled by this synchronizer. */
	private ArrayList array;

	private AbsoluteDate currentDate;

}
