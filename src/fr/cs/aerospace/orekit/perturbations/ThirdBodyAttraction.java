package fr.cs.aerospace.orekit.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.bodies.ThirdBody;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.propagation.EquinoctialGaussEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

public class ThirdBodyAttraction implements ForceModel {

	public ThirdBodyAttraction(ThirdBody body) {
		this.body = body;
	}
	
	public void addContribution(AbsoluteDate t, PVCoordinates pvCoordinates,
			EquinoctialGaussEquations adder) throws OrekitException {
		
		Vector3D otherBody = new Vector3D(body.getPosition(t, adder.getFrame()));
		
		Vector3D centralBody = new Vector3D(-1.0 , pvCoordinates.getPosition());
		centralBody = Vector3D.add(centralBody,otherBody);
		
		centralBody = Vector3D.multiply(1/Math.pow(centralBody.getNorm(), 3),centralBody);
		otherBody = Vector3D.multiply(1/Math.pow(otherBody.getNorm(), 3),otherBody);
		
		Vector3D gamma = Vector3D.subtract(centralBody, otherBody);
		gamma = Vector3D.multiply(body.getMu(), gamma);
		
		adder.addXYZAcceleration(gamma.getX(), gamma.getY(), gamma.getZ());
	}

	public SWF[] getSwitchingFunctions() {
		return null;
	}

	private ThirdBody body;
	
}
