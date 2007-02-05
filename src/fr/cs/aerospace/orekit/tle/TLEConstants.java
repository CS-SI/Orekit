package fr.cs.aerospace.orekit.tle;


/** Constants necessary to TLE propagation.
 * 
 * This constants are used in the WGS-72 model, compliant with NORAD implementations.
 * 
 * @author F. Maussion
 */
final class TLEConstants {
  
  public static final double oneThird = 1.0/3.0;
  public static final double twoThirds = 2.0/3.0;
    
  /** Earth radius in km */
  public static final double er = 6378.135;
 
  /** Equatorial radius redimensionned (1) */
  public static final double ae = 1.0;
  
  public static final double minutesPerDay = 1440.0;
  
  /** Potential ceofficients  */
  public static final double mu = 3.986008e+14;
  public static final double xke = 0.0743669161331734132;
  public static final double xj3 = -2.53881e-6;
  public static final double xj2 = 1.082616e-3;
  public static final double xj4 = -1.65597e-6;
  public static final double ck2 = (0.5 * xj2 * ae * ae);
  public static final double ck4 = (-.375 * xj4 * ae * ae * ae * ae);
  public static final double s = (ae * (1. + 78. / er)); 
  public static final double qoms2t = 1.880279159015270643865e-9;  
  public static final double a3ovk2 = -xj3/ck2*ae*ae*ae;
    
}
