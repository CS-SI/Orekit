package org.orekit.propagation.analytical.gnss;

import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

/**
 * 
 * This class aims at propagating a SBAS orbit from {@link SBASOrbitalElements}.
 *
 * @see "Tyler Reid, Todd Walker, Per Enge, L1/L5 SBAS MOPS Ephemeris Message to
 *      Support Multiple Orbit Classes, ION ITM, 2013"
 *
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */
public class FieldSBASPropagator<T extends RealFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {
	/** The SBAS orbital elements used. */
	private final FieldSBASOrbitalElements<T> sbasOrbit;

	/** The spacecraft mass (kg). */
	private final T mass;

	/** The Earth gravity coefficient used for SBAS propagation. */
	private final T mu;

	/** The ECI frame used for SBAS propagation. */
	private final Frame eci;

	/** The ECEF frame used for SBAS propagation. */
	private final Frame ecef;
	
	/** Initial state. */
    private FieldSpacecraftState<T> initialState;

	/**
	 * Default constructor.
	 * 
	 * @param field
	 * @param sbasOrbit
	 */
	@DefaultDataContext
	public FieldSBASPropagator(final Field<T> field, final FieldSBASOrbitalElements<T> sbasOrbit) {
		this(field, sbasOrbit, DataContext.getDefault().getFrames());
	}

	/**
	 * Constructor.
	 * 
	 * @param field
	 * @param sbasOrbit
	 * @param frames
	 */
	public FieldSBASPropagator(final Field<T> field, final FieldSBASOrbitalElements<T> sbasOrbit, final Frames frames) {
		this(field, sbasOrbit, Propagator.getDefaultLaw(frames), DEFAULT_MASS, frames.getEME2000(),
				frames.getITRF(IERSConventions.IERS_2010, true));
	}

	/**
	 * Constructor.
	 * 
	 * @param field
	 * @param sbasOrbit
	 * @param attitudeProvider
	 * @param mass
	 * @param eci
	 * @param ecef
	 */
	public FieldSBASPropagator(final Field<T> field, final FieldSBASOrbitalElements<T> sbasOrbit,
			final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef /*, final FieldSpacecraftState<T> initialState*/) {
		super(field, attitudeProvider);
		// Stores the SBAS orbital elements
		this.sbasOrbit = sbasOrbit;
		// Sets the start date as the date of the orbital elements
		setStartDate(sbasOrbit.getDate());
		// Sets the mu
		final T zero = field.getZero();
		this.mu = zero.add(FieldSBASOrbitalElements.SBAS_MU);
		// Sets the mass
		this.mass = zero.add(mass);
		// Sets the Earth Centered Inertial frame
		this.eci = eci;
		// Sets the Earth Centered Earth Fixed frame
		this.ecef = ecef;
		//this.initialState = initialState;
	}

	/**
	 * Get the underlying SBAS orbital elements.
	 *
	 * @return the underlying SBAS orbital elements
	 */
	public FieldSBASOrbitalElements<T> getFieldSBASOrbitalElements() {
		return sbasOrbit;
	}

	/**
	 * Gets the PVCoordinates of the GNSS SV in {@link #getECEF() ECEF frame}.
	 *
	 * <p>
	 * The algorithm uses automatic differentiation to compute velocity and
	 * acceleration.
	 * </p>
	 *
	 * @param date the computation date
	 * @return the GNSS SV PVCoordinates in {@link #getECEF() ECEF frame}
	 */
	public FieldPVCoordinates<T> propagateInEcef(final FieldAbsoluteDate<T> date) {
		// Duration from SBAS ephemeris Reference date
		final T zero = date.getField().getZero();
		final FieldUnivariateDerivative2<T> dt = new FieldUnivariateDerivative2<T>(zero.add(getDT(date)), zero.add(1.0),
				zero.add(1.0));
		// Satellite coordinates
		final FieldUnivariateDerivative2<T> x = dt
				.multiply(dt.multiply(sbasOrbit.getXDotDot().multiply(0.5)).add(sbasOrbit.getXDot()))
				.add(sbasOrbit.getX());
		final FieldUnivariateDerivative2<T> y = dt
				.multiply(dt.multiply(sbasOrbit.getYDotDot().multiply(0.5)).add(sbasOrbit.getYDot()))
				.add(sbasOrbit.getY());
		final FieldUnivariateDerivative2<T> z = dt
				.multiply(dt.multiply(sbasOrbit.getZDotDot().multiply(0.5)).add(sbasOrbit.getZDot()))
				.add(sbasOrbit.getZ());
		// Returns the Earth-fixed coordinates
		final FieldVector3D<FieldUnivariateDerivative2<T>> positionwithDerivatives = new FieldVector3D<>(x, y, z);
		return new FieldPVCoordinates<T>(
				new FieldVector3D<T>(positionwithDerivatives.getX().getValue(),
						positionwithDerivatives.getY().getValue(), positionwithDerivatives.getZ().getValue()),
				new FieldVector3D<T>(positionwithDerivatives.getX().getFirstDerivative(),
						positionwithDerivatives.getY().getFirstDerivative(),
						positionwithDerivatives.getZ().getFirstDerivative()),
				new FieldVector3D<T>(positionwithDerivatives.getX().getSecondDerivative(),
						positionwithDerivatives.getY().getSecondDerivative(),
						positionwithDerivatives.getZ().getSecondDerivative()));
	}

	/** {@inheritDoc} */
	protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date) {
		// Gets the PVCoordinates in ECEF frame
		final FieldPVCoordinates<T> pvaInECEF = propagateInEcef(date);
		// Transforms the PVCoordinates to ECI frame
		final FieldPVCoordinates<T> pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
		// Returns the Cartesian orbit
		final T zero = date.getField().getZero();
		return new FieldCartesianOrbit<T>(pvaInECI, eci, date, zero.add(mu));
	}

	/**
	 * Get the Earth gravity coefficient used for SBAS propagation.
	 * 
	 * @return the Earth gravity coefficient.
	 */
	public T getMU() {
		return mu;
	}

	/**
	 * Gets the Earth Centered Inertial frame used to propagate the orbit.
	 *
	 * @return the ECI frame
	 */
	public Frame getECI() {
		return eci;
	}

	/**
	 * Gets the Earth Centered Earth Fixed frame used to propagate GNSS orbits.
	 *
	 * @return the ECEF frame
	 */
	public Frame getECEF() {
		return ecef;
	}

	/**
	 * Get the underlying SBAS orbital elements.
	 *
	 * @return the underlying SBAS orbital elements
	 */
	public FieldSBASOrbitalElements<T> getSBASOrbitalElements() {
		return sbasOrbit;
	}

	/** {@inheritDoc} */
	public Frame getFrame() {
		return eci;
	}

	/** {@inheritDoc} */
	public void resetInitialState(final SpacecraftState state) {
		throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
	}

	/** {@inheritDoc} */
	protected T getMass(final FieldAbsoluteDate<T> date) {
		return mass;
	}

	/** {@inheritDoc} */
	protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
		throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
	}

	/**
	 * Get the duration from SBAS Reference epoch.
	 * 
	 * @param date the considered date
	 * @return the duration from SBAS orbit Reference epoch (s)
	 */
	private T getDT(final FieldAbsoluteDate<T> date) {
		// Time from ephemeris reference epoch
		return date.durationFrom(sbasOrbit.getDate());
	}

	@Override
	protected void resetIntermediateState(FieldSpacecraftState<T> state, boolean forward) {
		throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);

	}

	@Override
	protected FieldOrbit<T> propagateOrbit(FieldAbsoluteDate<T> date, T[] parameters) {
		// Gets the PVCoordinates in ECEF frame
        final FieldPVCoordinates<T> pvaInECEF = propagateInEcef(date);
        // Transforms the PVCoordinates to ECI frame
        final FieldPVCoordinates<T> pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
        // Returns the Cartesian orbit
        return new FieldCartesianOrbit<T>(pvaInECI, eci, date, mu);
	}

	@Override
	protected List<ParameterDriver> getParametersDrivers() {
		
		return Collections.emptyList();
	}

}
