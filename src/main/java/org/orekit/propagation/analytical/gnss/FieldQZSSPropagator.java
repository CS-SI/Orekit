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
 * This class aims at propagating a QZSS orbit from {@link FieldQZSSOrbitalElements}.
 *
 * @see <a href="http://qzss.go.jp/en/technical/download/pdf/ps-is-qzss/is-qzss-pnt-003.pdf?t=1549268771755">
 *       QZSS Interface Specification</a>
 *
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */
public class FieldQZSSPropagator<T extends RealFieldElement<T>> extends FieldAbstractGNSSPropagator<T> {
	
	// Constants
    /** WGS 84 value of the earth's rotation rate in rad/s. */
    private static final double QZSS_AV = 7.2921151467e-5;

    /** Duration of the QZSS cycle in seconds. */
    private static final double QZSS_CYCLE_DURATION = FieldQZSSOrbitalElements.QZSS_WEEK_IN_SECONDS *
                                                      FieldQZSSOrbitalElements.QZSS_WEEK_NB;

    // Fields
    /** The QZSS orbital elements used. */
    private final FieldQZSSOrbitalElements<T> qzssOrbit;
    
    /**
	 * Default constructor.
	 * 
	 * @param field
	 * @param qzssOrbit
	 */
	@DefaultDataContext
	public FieldQZSSPropagator(final Field<T> field, final FieldQZSSOrbitalElements<T> qzssOrbit) {
		this(field, qzssOrbit, DataContext.getDefault().getFrames());
	}

	/**
	 * Constructor.
	 * 
	 * @param field
	 * @param qzssOrbit
	 * @param frames
	 */
	public FieldQZSSPropagator(final Field<T> field, final FieldQZSSOrbitalElements<T> qzssOrbit,
			final Frames frames) {
		this(field, qzssOrbit, Propagator.getDefaultLaw(frames), DEFAULT_MASS, frames.getEME2000(),
				frames.getITRF(IERSConventions.IERS_2010, true));
	}

	/**
	 * Constructor.
	 * 
	 * @param field
	 * @param qzssOrbit
	 * @param attitudeProvider
	 * @param mass
	 * @param eci
	 * @param ecef
	 */
	public FieldQZSSPropagator(final Field<T> field, final FieldQZSSOrbitalElements<T> qzssOrbit,
			final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef) {
		super(field, qzssOrbit, attitudeProvider, eci, ecef, mass, QZSS_AV, QZSS_CYCLE_DURATION,
				FieldQZSSOrbitalElements.QZSS_MU);
		// Stores the QZSS orbital elements
		this.qzssOrbit = qzssOrbit;
	}

	/**
	 * Get the underlying QZSS orbital elements.
	 *
	 * @return the underlying QZSS orbital elements
	 */
	public FieldQZSSOrbitalElements<T> getFieldQZSSOrbitalElements() {
		return qzssOrbit;
	}
}
