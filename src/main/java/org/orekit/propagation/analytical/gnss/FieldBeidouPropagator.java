package org.orekit.propagation.analytical.gnss;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.propagation.Propagator;
import org.orekit.utils.IERSConventions;

/**
 * This class aims at propagating a Beidou orbit from
 * {@link FieldBeidouOrbitalElements}.
 *
 * @see <a href=
 *      "http://www2.unb.ca/gge/Resources/beidou_icd_english_ver2.0.pdf">Beidou
 *      Interface Control Document</a>
 *
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */
public class FieldBeidouPropagator<T extends RealFieldElement<T>> extends FieldAbstractGNSSPropagator<T> {

	// Constants
	/** Value of the earth's rotation rate in rad/s. */
	private static final double BEIDOU_AV = 7.2921150e-5;

	/** Duration of the Beidou cycle in seconds. */
	private static final double BEIDOU_CYCLE_DURATION = FieldBeidouOrbitalElements.BEIDOU_WEEK_IN_SECONDS
			* FieldBeidouOrbitalElements.BEIDOU_WEEK_NB;

	// Fields
	/** The Beidou orbital elements used. */
	private final FieldBeidouOrbitalElements<T> bdsOrbit;

	/**
	 * Default constructor.
	 * 
	 * @param field
	 * @param bdsOrbit
	 */
	@DefaultDataContext
	public FieldBeidouPropagator(final Field<T> field, final FieldBeidouOrbitalElements<T> bdsOrbit) {
		this(field, bdsOrbit, DataContext.getDefault().getFrames());
	}

	/**
	 * Constructor.
	 * 
	 * @param field
	 * @param bdsOrbit
	 * @param frames
	 */
	public FieldBeidouPropagator(final Field<T> field, final FieldBeidouOrbitalElements<T> bdsOrbit,
			final Frames frames) {
		this(field, bdsOrbit, Propagator.getDefaultLaw(frames), DEFAULT_MASS, frames.getEME2000(),
				frames.getITRF(IERSConventions.IERS_2010, true));
	}

	/**
	 * Constructor.
	 * 
	 * @param field
	 * @param bdsOrbit
	 * @param attitudeProvider
	 * @param mass
	 * @param eci
	 * @param ecef
	 */
	public FieldBeidouPropagator(final Field<T> field, final FieldBeidouOrbitalElements<T> bdsOrbit,
			final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef) {
		super(field, bdsOrbit, attitudeProvider, eci, ecef, mass, BEIDOU_AV, BEIDOU_CYCLE_DURATION,
				FieldBeidouOrbitalElements.BEIDOU_MU);
		// Stores the Beidou orbital elements
		this.bdsOrbit = bdsOrbit;
	}

	/**
	 * Get the underlying Beidou orbital elements.
	 *
	 * @return the underlying Beidou orbital elements
	 */
	public FieldBeidouOrbitalElements<T> getFieldBeidouOrbitalElements() {
		return bdsOrbit;
	}

}
