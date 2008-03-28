package fr.cs.orekit.propagation;

import fr.cs.orekit.attitudes.AttitudeKinematics;
import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.attitudes.models.IdentityAttitude;
import fr.cs.orekit.propagation.numerical.forces.ForceModel;


/** Every class implementing this interface realises a contract, which is
 * to propagate an attitude thanks to an {@link AttitudeKinematicsProvider}, provided
 * by the user with the method : {@link #setAkProvider(AttitudeKinematicsProvider)}.
 *
 * All propagated {@link SpacecraftState SpacecraftStates} will be updated to the correct
 * {@link AttitudeKinematics}, and so will all intermediate values so that the
 * {@link ForceModel forcemodels} wich need the attitude can use this information.
 *
 * @see AttitudeKinematicsProvider
 * @see AttitudeKinematics
 *
 * @author F. Maussion
 */
public interface AttitudePropagator {

    /** Sets the attitude provider used by the propagator.
     * <p> If this method is never called before extrapolation, the attitude is
     * set to default : {@link IdentityAttitude} <p>
     * @param akProvider the attitude to propagate
     */
    public void setAkProvider(AttitudeKinematicsProvider akProvider);

}
