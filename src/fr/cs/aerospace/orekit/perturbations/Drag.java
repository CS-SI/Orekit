package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.*;
import fr.cs.aerospace.orekit.perturbations.*;

import org.spaceroots.mantissa.geometry.Vector3D;
import java.lang.Float;

/**
 * This class represents the atmospheric drag applied to the vehicle.
  
 * @version $Id$
 * @author E. Delente
 */


public class Drag implements ForceModel {
    
    /** Drag force contribution to the acceleration */
    private double[] fdrag;
    
    /** Atmosphere */
    private Atmosphere atmosphere;
    
    
   /** Default constructor.
    * Create a new instance with arbitrary default elements.
    */
    public Drag() {
        fdrag = new double[3];
        atmosphere = new Atmosphere();
    }

    
   /** Constructor from the atmospheric parameters
    * @param rho0 density at the reference altitude
    * @param h0 altitude of reference (m)
    * @param hscale scale factor 
    */
    public Drag(double rho0, double h0, double hscale) {
        fdrag = new double[3];
        atmosphere = new Atmosphere(rho0, h0, hscale);
    }

    
   /** Get the atmospheric model.
   * @return atmosphere atmosphere
   */
    public Atmosphere getAtmosphere() {
        return atmosphere;
    }
    
    
   /** Compute the contribution of the drag to the perturbing acceleration.
    * @param t current date
    * @param position current position(m)
    * @param velocity current velocity (m/s)
    * @param Attitude current attitude
    * @param adder object where the contribution should be added
    */    
    public void addContribution(RDate t, Vector3D position, Vector3D velocity, 
                                Attitude Attitude, OrbitDerivativesAdder adder) throws OrekitException {
                                    
    // Creation of a simple vehicle
    SimpleVehicle vehicle = new SimpleVehicle(1500.0, 3.0, 2.0, 0.2, 0.3);
    
    // Calculation of rho
    double x = position.getX();
    double y = position.getY();
    double z = position.getZ();
    double h = position.getNorm() - Constants.CentralBodyradius;
    double rho = 0.0;
    rho = getAtmosphere().getRho(h);    

    double halfRhoVSCx = 0.5 * rho * velocity.getNorm() * vehicle.getSurface() * 
              vehicle.getDragCoef();
    if (vehicle.getMass()< Constants.Epsilon) 
        {throw new OrekitException("Vehicle's mass is equal to 0");}
    
    fdrag[0] = - halfRhoVSCx * velocity.getX() / vehicle.getMass();
    fdrag[1] = - halfRhoVSCx * velocity.getY() / vehicle.getMass();
    fdrag[2] = - halfRhoVSCx * velocity.getZ() / vehicle.getMass();

    // Additition of calculated accelration to adder
    adder.addXYZAcceleration(fdrag[0], fdrag[1], fdrag[2]);
    }
    
    
    /** Get the switching functions.
    */
    public SWF[] getSwitchingFunctions() {
        return null;
    }
    
}
