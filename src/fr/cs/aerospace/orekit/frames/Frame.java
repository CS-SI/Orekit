package fr.cs.aerospace.orekit.frames;

import java.util.LinkedList;

import fr.cs.aerospace.orekit.errors.Translator;

/** Tridimensional references frames class.
 * <p>This class is the base class for all frames in OREKIT. The frames are
 * linked together in a tree with the J2000 frame as the root of the tree.
 * Each frame is defined by translation/rotation transforms from a reference
 * frame which is its parent frame in the tree structure.</p>
 * <p>The translation/rotation transforms may be constant or varying. For
 * simple fixed transforms, using this base class is sufficient. For varying
 * transforms (time-dependant or telemetry-based for example), it may be
 * useful to define specific subclasses.</p>
 * @author Guylaine Prat
 * @author Luc Maisonobe
 */
public class Frame {

  /** Get the uniq J2000 frame.
   * @return the uniq instance of the J2000 frame
   */
  public static Frame getJ2000() {
    if (j2000 == null) {
      j2000 = new Frame();
    }
    return j2000;
  }

  /** Private constructor for the J2000 root frame.
   */
  private Frame() {
    parent    = null;
    transform = new Transform();
  }

  /** Build a frame from its transform with respect to its parent.
   * @param parent parent frame (must be non-null)
   * @param transform transform from parent frame to instance
   * @exception IllegalArgumentException if the parent frame is null
   */
  public Frame(Frame parent, Transform transform)
   throws IllegalArgumentException {

    if (parent == null) {
      String message = Translator.getInstance().translate("null parent frame");
      throw new IllegalArgumentException(message);
    }

    this.parent    = parent;
    this.transform = transform;

  }

  /** Update the transform from the parent frame to the instance.
   * @param transform new transform from parent frame to instance
   */
  public void updateTransform(Transform transform) {
    this.transform = transform;
  }

  /** Get the transform from the instance to another frame.
   * @param destination destination frame to which we want to transform vectors
   * @return transform from the instance to the destination frame
   */
  public Transform getTransformTo(Frame destination) {

    // common ancestor to both frames in the frames tree
    Frame common = findCommon(this, destination);

    // transform from instance up to common
    Transform instanceToCommon = new Transform();
    for (Frame frame = this; frame != common; frame = frame.parent) {
      instanceToCommon =
        new Transform(instanceToCommon, frame.transform);
    }

    // transform from destination up to common
    Transform destinationToCommon = new Transform();
    for (Frame frame = destination; frame != common; frame = frame.parent) {
      destinationToCommon =
        new Transform(destinationToCommon, frame.transform);
    }

    // transform from instance, to common, to destination
    return new Transform(instanceToCommon, destinationToCommon.getInverse());

  }

  /** Find the deepest common ancestor of two frames in the frames tree.
   * @param from origin frame
   * @param to destination frame
   * @return an ancestor frame of both <code>from</code> and <code>to</code>
   */
  private static Frame findCommon(Frame from, Frame to) {

    // definitions of the path up to the head tree for each frame
    LinkedList pathFrom = from.pathToRoot();
    LinkedList pathTo   = to.pathToRoot();

    if (pathFrom.isEmpty()) { // handle root case
      // in this case the common frame is root
      return from;
    }
    if (pathTo.isEmpty()) { // handle root case
      // in this case the common frame is root
      return to;
    }

    // at this stage pathFrom contains almost one frame
    Frame lastFrom = (Frame) pathFrom.removeLast();
    Frame common   = lastFrom; // common must be one of the instance of Frame already defined

    // at the beginning of the loop pathTo contains almost one frame
    for (Frame lastTo = (Frame) pathTo.removeLast();
         (lastTo == lastFrom) && (lastTo != null) && (lastFrom != null);
         // in order to deal with the end of the list which throwed an exception
         lastTo = (Frame) (pathTo.isEmpty() ? null : pathTo.removeLast())) {

      common = lastFrom;

      // in order to deal with the end of the list which throwed an exception
      lastFrom = (Frame) (pathFrom.isEmpty() ? null : pathFrom.removeLast());
    }

    return common;

  }

  /** Get the path from instance frame to the root frame.
   * @return path from instance to root, excluding instance itself
   * (empty if instance is root)
   */
  private LinkedList pathToRoot() {
    LinkedList path = new LinkedList();
    for (Frame frame = parent; frame != null; frame = frame.parent) {
      path.add(frame);
    }
    return path;
  }

  /**  parent frame (only J2000 doesn't have a parent). */
  private Frame parent;

  /** Transform from parent frame to instance. */
  private Transform transform;

  /** J2000 root frame. */
  private static Frame j2000 = null;

}
