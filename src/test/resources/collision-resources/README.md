# conjunction
Conjunction data for collision avoidance design. The data is contained in data.dat file (printed Matlab table) and organized as follows

ID: event ID, it coincides with the line number 

R [km]: radius of the collision disk (sum of objects radii)

p_j2k_x [km], p_j2k_y [km], p_j2k_z [km], p_j2k_vx [km/s], p_j2k_vy [km/s], p_j2k_vz [km/s]: primary (p) state vector at closest approach, cartesian ECIJ200

p_c_rr  [km^2], p_c_tt  [km^2], p_c_nn  [km^2], p_c_rt  [km^2], p_c_rn  [km^2], p_c_tn  [km^2]: primary (p) positional covariance (c) elements, radial (r), transverse (t) and normal (n) at closest aproach, primary RTN reference frame

s_j2k_x [km], s_j2k_y [km], s_j2k_z [km], s_j2k_vx [km/s], s_j2k_vy [km/s], s_j2k_vz [km/s]: secondary (s) state vector at closest approach, cartesian ECIJ200

s_c_rr  [km^2], s_c_tt  [km^2], s_c_nn  [km^2], s_c_rt  [km^2], s_c_rn  [km^2], s_c_tn  [km^2], secondary (s) positional covariance (c) elements, radial (r), transverse (t) and normal (n) at closest aproach, secondary RTN reference frame

Pc: collision probability using Alfano's method Eq 5a (S. Alfano, Review of Conjunction Probability Methods for Short-term Encounters, AAS Paper 07-148, 2007)

Pc_approx: collision probability approximation using Alfriend's method Eq. 18 (K. Alfriend, M. Akella, J. Frisbee, J. Foster, D.-J. Lee, and M. Wilkins, “Probability of Collision Error Analysis,”Space Debris, vol. 1,no. 1, pp. 21–35, 1999)

Pc_max; maximum collision probability using Alfriend's method Eq. 21 (K. Alfriend, M. Akella, J. Frisbee, J. Foster, D.-J. Lee, and M. Wilkins, “Probability of Collision Error Analysis,”Space Debris, vol. 1,no. 1, pp. 21–35, 1999) 

d^* [km]: relative distance at closest approach

v^* [km/s]: relative speed at closest approach 

d_m^2 [km^]: squared Mahalanobis distance at closest approach (b-plane)

This data is derived from ESA Collision Avoidance Competition data (https://kelvins.esa.int/collision-avoidance-challenge/data/) with the help of Sebastien Henry 
