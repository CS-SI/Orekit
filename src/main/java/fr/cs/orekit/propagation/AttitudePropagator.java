package fr.cs.orekit.propagation;

import fr.cs.orekit.attitudes.AttitudeLaw;


/** Every class implementing this interface realises a contract, which is
 * to propagate an attitude thanks to an {@link AttitudeKinematicsProvider}, provided
 * by the user with the method : {@link #setAkProvider(AttitudeKinematicsProvider)}.
 *
 * All propagated {@link SpacecraftState SpacecraftStates} will be updated to the correct
 * {@link AttitudeKinematics}, and so will all intermediate values so that the
 * {@link fr.cs.orekit.propagation.numerical.forces.ForceModel forcemodels} which needs
 * the attitude can use this information.
 *
 * @see AttitudeKinematicsProvider
 * @see AttitudeKinematics
 *
 * @author F. Maussion
 */
public interface AttitudePropagator {

    /** Sets the attitude law used by the propagator.
     * <p> If this method is never called before extrapolation, the attitude is
     * set to default : {@link fr.cs.orekit.attitudes.DefaultAttitude} <p>
     * @param attitudeLaw the attitude law to use
     */
    public void setAttitudeLaw(AttitudeLaw attitudeLaw);

}
