package fr.cs.aerospace.orekit;

/** This class contains useful constants.
  
 * @version $Id$
 * @author  E. Delente
 */


public interface Constants {
    
  /** Gravitational constant. */
  double G = 6672.0 * 10E-14;
  
  /** Earth's angular velocity  (rad/s). */
  double w = 7292115*10E-11;

  /** Central body radius (m). */
  double CentralBodyradius = 6378140.0;
  
  /** Central body mu. */
  double CentralBodymu = 3.9860047E14;
  
  /** Zero. */
  double Epsilon = 1.0E-12;
  
}
