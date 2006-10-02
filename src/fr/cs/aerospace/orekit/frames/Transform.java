package fr.cs.aerospace.orekit.frames;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.utils.PVCoordinates;
import fr.cs.aerospace.orekit.utils.Vector;

/** Transformation class in three dimensional space.
 * <p>This class represents the transformation engine between {@link Frame frames}.
 * It is used both to define the relationship between each frame and its
 * parent frame and to gather all individual transforms into one
 * operation when converting between frames far away from each other.</p>
 *  @author Luc Maisonobe
 */
public class Transform {

  /** Build a transform from its primitive operations.
   * @param translation first primitive operation to apply
   * @param rotation second primitive operation to apply
   */
  private Transform(Vector3D translation, Rotation rotation, Vector3D velocity, Vector3D instantAxis, double w ) {
    this.translation    = translation;
    this.rotation       = rotation;
    this.velocity       = velocity;
    this.normalizedAxis = instantAxis;
    if(this.normalizedAxis.getNorm() != 0) {
    	this.normalizedAxis.normalizeSelf();
    }
    this.w              = w;
    this.rotAxis        = new Vector3D( w , this.normalizedAxis);
  }

  /** Build an identity transform.
   */
  public Transform() {
    this(new Vector3D(), new Rotation(), new Vector3D(), new Vector3D(), 0);
  }

  /** Build a translation transform.
   * @param translation translation to apply (i.e. coordinates of
   * the transformed origin, or coordinates of the origin of the
   * new frame in the old frame)
   */
  public Transform(Vector3D translation) {
    this(translation, new Rotation(), new Vector3D(), new Vector3D(), 0);
  }

  /** Build a rotation transform.
   * @param rotation rotation to apply
   */
  public Transform(Rotation rotation) {
    this(new Vector3D(), rotation, new Vector3D(), new Vector3D(), 0);
  }

  public Transform(Vector3D translation, Vector3D velocity) {
	  this(translation, new Rotation(), velocity, new Vector3D(), 0);
  }
  
  public Transform(Rotation rotation, Vector3D instantAxis, double w) {
	  this(new Vector3D(), rotation, new Vector3D(), instantAxis, w);
  }
  
  /** Build a transform by combining two existing ones.
   * @param first first transform applied
   * @param second second transform applied
   */
  public Transform(Transform first, Transform second) {
	
	  this(
	    // new translation
	  Vector3D.add(first.translation,  first.rotation.applyInverseTo(second.translation)),
		// new rotation	  
      second.rotation.applyTo(first.rotation), 
        // new velocity
      //FIXME the next line works when you combine a translation before a rotation only
       second.transformPVCoordinates(new PVCoordinates( Vector3D.negate(second.translation) , first.getVelocity() )).getVelocity() ,
      //Vector3D.add(first.getVelocity(), first.transformVector(second.velocity)) , 
      // new axis
      Vector3D.add(second.transformVector(first.rotAxis) , second.rotAxis),  
      // new w  
      (Vector3D.add(second.transformVector(first.rotAxis) , second.rotAxis)).getNorm() 
	  );
	
  }
  
  /** Get the inverse transform of the instance.
   * @return inverse transform of the instance
   */
  public Transform getInverse() {
	  //FIXME doesn't work when you inverse a rotation plus a translation, but works for simple operations
    return new Transform(rotation.applyTo(Vector3D.negate(translation)),
                         rotation.revert(), Vector3D.negate(velocity), Vector3D.negate(normalizedAxis), w);
  }

  /** Transform a position vector (including translation effects).
   * @param position vector to transform
   */
  public Vector3D transformPosition(Vector3D position) {
    return rotation.applyTo(Vector3D.add(translation, position));
  }

  /** Transform a vector (ignoring translation effects).
   * @param vector vector to transform
   */
  public Vector3D transformVector(Vector3D vector) {
    return rotation.applyTo(vector);
  } 
  
  public PVCoordinates transformPVCoordinates(PVCoordinates pvCoordinates) {
	  
	  Vector3D newPosition = transformPosition(pvCoordinates.getPosition());
	
	  Vector3D rotationEffects = Vector3D.add( transformVector(pvCoordinates.getVelocity()) ,
			  Vector3D.crossProduct(rotAxis, Vector3D.negate(transformPosition(pvCoordinates.getPosition()) ) ) ) ;
	  
	  Vector3D newVelocity = Vector3D.add(rotationEffects , this.velocity) ;
	  
	  return  new PVCoordinates( newPosition , newVelocity); 
  }

  /** Get the underlying elementary translation.
   * <p>A transform can be uniquely represented as an elementary
   * translation followed by an elementary rotation. This method
   * returns this unique elementary translation.</p>
   * @return underlying elementary translation
   * @see #getRotation()
   */
  public Vector3D getTranslation() {
    return translation;
  }

  /** Get the underlying elementary rotation.
   * <p>A transform can be uniquely represented as an elementary
   * translation followed by an elementary rotation. This method
   * returns this unique elementary rotation.</p>
   * @return underlying elementary rotation
   * @see #getTranslation()
   */
  public Rotation getRotation() {
    return rotation;
  }
  

  public Vector3D getNormalizedAxis() {
  	return normalizedAxis;
  }


  public Vector3D getRotAxis() {
  	return rotAxis;
  }


  public Vector3D getVelocity() {
  	return velocity;
  }


  public double getW() {
  	return w;
  }

  /** Global translation. */
  private Vector3D translation;

  /** Global rotation. */
  private Rotation rotation;

  private Vector3D velocity;
  
  private Vector3D normalizedAxis;
  
  private Vector3D rotAxis;
  
  private double w;


}
