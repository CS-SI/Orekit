package fr.cs.aerospace.orekit.forces.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.bodies.ThirdBody;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.ForceModel;
import fr.cs.aerospace.orekit.forces.SWF;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.models.bodies.Moon;
import fr.cs.aerospace.orekit.models.bodies.Sun;
import fr.cs.aerospace.orekit.propagation.TimeDerivativesEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** Third body attraction force model.
 *  
 * @author F. Maussion
 */
public class ThirdBodyAttraction implements ForceModel {

    /** Simple constructor.
     * @param body the third body to consider 
     * (ex: {@link Sun} or {@link Moon})
     */
	public ThirdBodyAttraction(ThirdBody body) {
		this.body = body;
	}
	
    /** Compute the contribution of the body attraction to the perturbing
    * acceleration.
    * @param t current date
     * @param pvCoordinates
     * @param adder object where the contribution should be added
    */  
	public void addContribution(AbsoluteDate t, PVCoordinates pvCoordinates,
			Frame frame, double mass, AttitudeKinematics ak, TimeDerivativesEquations adder) throws OrekitException {
		
		Vector3D otherBody = new Vector3D(body.getPosition(t, frame));
		
		Vector3D centralBody = new Vector3D(-1.0 , pvCoordinates.getPosition());
		centralBody = Vector3D.add(centralBody,otherBody);
		
		centralBody = Vector3D.multiply(1.0/Math.pow(centralBody.getNorm(), 3),centralBody);
		otherBody = Vector3D.multiply(1.0/Math.pow(otherBody.getNorm(), 3),otherBody);
		
		Vector3D gamma = Vector3D.subtract(centralBody, otherBody);
		gamma = Vector3D.multiply(body.getMu(), gamma);
		
		adder.addXYZAcceleration(gamma.getX(), gamma.getY(), gamma.getZ());
	}

   /** Ther are no SwitchingFunctions for this model.
    * @return null
    */
	public SWF[] getSwitchingFunctions() {
		return new SWF[0];
	}
    
    /** The body to consider */
	private ThirdBody body;
	
}
