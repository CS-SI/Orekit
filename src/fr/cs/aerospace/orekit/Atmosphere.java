package fr.cs.aerospace.orekit;

/** This interface represents the vehicles features.
 *
 *
 * @version $Id$
 * @author E. Delente
 */

public class Atmosphere {
    
    private double rho0;
    
    private double h0;
    
    private double hscale;
    
    /** Simple Constructor.
     * Create an instance with a default value
     */    
    public Atmosphere() {
        rho0 = 0.0;
        h0 = 0.0;
        hscale = 0.0;
    }
    
    /** Create an exponential atmosphere.
     * @param rho0 Density at the altitude h0
     * @param h0 Altitude of reference (m)
     * @param hscale Scale factor
     */
    public Atmosphere(double rho0, double h0, double hscale) {
        this.rho0 = rho0;
        this.h0 = h0;
        this.hscale = hscale;
    }

    /** Get the density rho0 at the reference altitude h0.
     * @return rho0 density of reference (m)
     */
    public double getRho0(){
        return rho0;
    }

    /** Get the reference altitude h0.
     * @return h0 altitude of reference (m)
     */
    public double getH0(){
        return h0;
    }
    
    /** Get the scale factor hscale.
     * @return hscale the scale factor (m)
     */
    public double getHscale(){
        return hscale;
    }

   /** Get the density at the given altitude.
   * @param altitude altitude
   * @return altitude altitude
   */
    public double getRho(double altitude) throws OrekitException {
        if (hscale < Constants.Epsilon) {throw new OrekitException("hscale is equal to 0");}
        return rho0 * Math.exp( - (altitude - h0) / hscale);
    }
}
