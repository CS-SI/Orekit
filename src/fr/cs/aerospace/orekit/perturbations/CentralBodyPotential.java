package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.Attitude;
import fr.cs.aerospace.orekit.OrbitDerivativesAdder;
import fr.cs.aerospace.orekit.OrekitException;

import org.spaceroots.mantissa.geometry.Vector3D;
/**
 * This class represents the gravitational field of a celestial body.
 
 * <p>The gravitational field of a central body is split in two parts.
 * The first one is the central attraction which is a single coefficient.
 * The second one is the perturbing acceleration which is expressed using
 * spherical harmonics.</p>
 
 * @version $Id$
 * @author L. Maisonobe
 * @author E. Delente
 */

public abstract class CentralBodyPotential implements ForceModel {
    
    /*** Name of the model. */
    protected String name;
    
    /** Central body attraction coefficient. */
    protected double mu;
    
    /** Equatorial radius of the Central Body. */
    protected double equatorialRadius;
    
    /** Central Body potential field vector. */    
    protected Vector3D acceleration;
    
    /** First normalized potential zonal coefficients array. */    
    protected double[] J;
    
    /** First normalized potential tesseral coefficients array. */    
    protected double[][] C;
    
    /** Second normalized potential tesseral coefficients array. */    
    protected double[][] S;
    
    /** Definition of degree, order and maximum potential size. */
    protected int ndeg;
    protected int nord;
    protected int maxpot = 100;
        
    /** Initialization of the acceleration. */
    protected double[] fpot;
    
    
   /** Create a new instance of CentralBodyPotential.
    * Build a spherical potential without perturbing acceleration
    * @param name name of the model
    * @param mu central body attraction coefficient
    */
    public CentralBodyPotential(String name, double mu) {
        this.name = name;
        this.mu   = mu;
        equatorialRadius = Double.NaN;
        acceleration = new Vector3D();
        J = null;
        C = null;
        S = null;
        ndeg = 0;
        nord = 0;
        maxpot = 100;
        fpot = new double[3];
    }
    
    
    /** Create a new instance of CentralBodyPotential.
     * @param name name of the model
     * @param mu central body attraction coefficient
     * @param equatorialRadius equatorial radius used for spherical harmonics
     * modeling
     * @param c normalized coefficients array (cosine part)
     * @param s normalized coefficients array (sine part)
     * @param degree degree of potential
     */
    public CentralBodyPotential(String name, double mu,
                                double equatorialRadius, double[] J,
                                double[][] C, double[][] S) {
        this.name = name;
        this.mu   = mu;
        this.equatorialRadius = equatorialRadius;
        acceleration = new Vector3D();
        this.J = J;
        this.C = C;
        this.S = S;
        ndeg = J.length-1;
        nord = S.length-1;
        maxpot = 100;
        fpot = new double[3];
    }
    
    
    /** Initialize the parameters of an instance of CentralBodyPotential.
     */
    public abstract void ResetPotentialModel();
    
    
    /** Compute the contribution of the central body potential to the
    * perturbing acceleration, using the Drozyner algorithm.
    * The central part of the acceleration (mu/r^2 term) is not computed here,
    * only the <em>perturbing</em> acceleration is considered, not the main
    * part.
    * @param t current date
    * @param position current position (m)
    * @param velocity current velocity (m/s)
    * @param Attitude current Attitude
    * @param adder object where the contribution should be added
    */
    public abstract void addContribution(RDate t, Vector3D position, Vector3D velocity, Attitude Attitude, OrbitDerivativesAdder adder) throws OrekitException;
    
    
    /** Get the switching functions. */
    public SWF[] getSwitchingFunctions() {
        return null;
    }
    
}

