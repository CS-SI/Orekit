package fr.cs.aerospace.orekit;

/**
 * This class contains all the useful constants.
  
 * @version $Id$
 * @author E. Delente
 */


public interface Constants {
    
    /** Gravitational constant.
     */
    double G = 6672.0 * 10E-14;

    /** Earth's angular velocity  (s^-1).
     */
    double w = 7292115*10E-11;
    
    /** Solar constant (N/m^2).
     */
    double C0 = 4.56E-6;
    
    /** Astronomical unit (m).
     */
    double AU = 149.598E9;
    
    /** Central body radius (m).
     */
    double CentralBodyradius = 6378140.0;
    
    /** Central body mu.
     */
    double CentralBodymu = 3.9860047E14;
    
    /** Moon radius (m).
     */
    double Moonradius =  1737400.0;
    
    /** Moon mu.
     */
    double Moonmu = 4.9027989E12;
    
    /** Sun radius (m).
     */
    double Sunradius = 6.96E8;
    
    /** Sun's mu.
     */
    double Sunmu = 1.32712440E20;
    
    /** Zero.
     */
    double Epsilon = 1.0E-12;
    
}
