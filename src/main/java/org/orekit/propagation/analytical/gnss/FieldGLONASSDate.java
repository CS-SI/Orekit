package org.orekit.propagation.analytical.gnss;

import java.io.Serializable;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;

public class FieldGLONASSDate<T extends RealFieldElement<T>> implements Serializable, FieldTimeStamped<T> {

	/** Serializable UID. */
	private static final long serialVersionUID = 20190131L;

	/** Constant for date computation. */
	private static final int C1 = 44195;

	/** Constant for date computation. */
	private static final int C2 = 45290;

	/** The number of the current day in a four year interval N<sub>a</sub>. */
	private final int na;

	/** The number of the current four year interval N<sub>4</sub>. */
	private final int n4;

	/** Number of seconds since N<sub>a</sub>. */
	private final T secInNa;

	/** Current Julian date JD0. */
	private T jd0;

	/** Greenwich Mean Sidereal Time (rad). */
	private T gmst;

	/** Corresponding date. */
	private final transient FieldAbsoluteDate<T> date;
	
	private final Field<T> field;
	

	/**
	 * Build an instance corresponding to a GLONASS date.
	 *
	 * <p>
	 * This method uses the {@link DataContext#getDefault() default data context}.
	 *
	 * @param na      the number of the current day in a four year interval
	 * @param n4      the number of the current four year interval
	 * @param secInNa the number of seconds since na start
	 * @see #GLONASSDate(int, int, T, TimeScale)
	 */
	@DefaultDataContext
	public FieldGLONASSDate(final Field<T> field, final int na, final int n4, final T secInNa) {
		this(field, na, n4, secInNa, DataContext.getDefault().getTimeScales().getGLONASS());
	}

	/**
	 * Build an instance corresponding to a GLONASS date.
	 *
	 * @param na      the number of the current day in a four year interval
	 * @param n4      the number of the current four year interval
	 * @param secInNa the number of seconds since na start
	 * @param glonass time scale.
	 * @since 10.1
	 */
	public FieldGLONASSDate(final Field<T> field, final int na, final int n4, final T secInNa, final TimeScale glonass) {
		this.field = field;
		this.na = na;
		this.n4 = n4;
		final T zero = field.getZero();
		this.secInNa = zero.add(secInNa);
		// Compute JD0
		final int ratio = FastMath.round((float) (na - 3) / (25 + C1 + C2));
		this.jd0 = zero.add(1461 * (n4 - 1) + na + 2450082.5 - ratio);
		// GMST
		this.gmst = computeGMST();
		this.date = computeDate(field, glonass);
	}

	/**
	 * Build an instance from an absolute date.
	 *
	 * <p>
	 * This method uses the {@link DataContext#getDefault() default data context}.
	 *
	 * @param date absolute date to consider
	 * @see #GLONASSDate(AbsoluteDate, TimeScale)
	 */
	@DefaultDataContext
	public FieldGLONASSDate(final Field<T> field, final FieldAbsoluteDate<T> date) {
		this(field, date, DataContext.getDefault().getTimeScales().getGLONASS());
	}

	/**
	 * Build an instance from an absolute date.
	 *
	 * @param date    absolute date to consider
	 * @param glonass time scale.
	 * @since 10.1
	 */
	public FieldGLONASSDate(final Field<T> field, final FieldAbsoluteDate<T> date, final TimeScale glonass) {
		this.field = field;
		final DateTimeComponents dateTime = date.getComponents(glonass);
		// N4
		final int year = dateTime.getDate().getYear();
		this.n4 = ((int) (year - 1996) / 4) + 1;
		// Na
		final int start = 1996 + 4 * (n4 - 1);
		
		final T zero = field.getZero();
		final T duration = zero.add(date.durationFrom(new FieldAbsoluteDate<T>(field, start, 1, 1, glonass))
				.getReal());

		this.na = (int) (duration.getReal() / 86400) + 1;
		this.secInNa = zero.add(dateTime.getTime().getSecondsInLocalDay());
		// Compute JD0
		final int ratio = FastMath.round((float) (na - 3) / (25 + C1 + C2));
		this.jd0 = zero.add(1461 * (n4 - 1) + na + 2450082.5 - ratio);
		// GMST
		this.gmst = computeGMST();
		this.date = date;
	}

	@Override
	public FieldAbsoluteDate<T> getDate() {
		return date;
	}

	/**
	 * Get the number of seconds since N<sub>a</sub> start.
	 * 
	 * @return number of seconds since N<sub>a</sub> start
	 */
	public T getSecInDay() {
		return secInNa;
	}

	/**
	 * Get the number of the current day in a four year interval.
	 * 
	 * @return the number of the current day in a four year interval
	 */
	public int getDayNumber() {
		return na;
	}

	/**
	 * Get the number of the current four year interval.
	 * 
	 * @return the number of the current four year interval
	 */
	public int getIntervalNumber() {
		return n4;
	}

	/**
	 * Get the current Julian date JD0.
	 * 
	 * @return the current date JD0
	 */
	public T getJD0() {
		return jd0;
	}

	/**
	 * Get the Greenwich Mean Sidereal Time.
	 * 
	 * @return the Greenwich Mean Sidereal Time (rad)
	 */
	public T getGMST() {
		return gmst;
	}

	/**
	 * Compute the Greenwich Mean Sidereal Time using the current Julian date JD0.
	 * 
	 * @return the Greenwich Mean Sidereal Time (rad)
	 */
	private T computeGMST() {
		final T zero = field.getZero();
		final T ref = zero.add(2451545.0);
		// Earth's rotation angle in radians
		final T era = zero.add(2. * GLONASSOrbitalElements.GLONASS_PI
				* (0.7790572732640)).add((jd0.subtract(ref)).multiply(1.00273781191135448));
		// Time from Epoch 2000 (1st January, 00:00 UTC) till current Epoch in Julian
		// centuries
		final T time = (jd0.subtract(ref)).divide(Constants.JULIAN_CENTURY);
		// Time to the power n
		final T time2 = time.multiply(time);
		final T time3 = time2.multiply(time);
		final T time4 = time2.multiply(time2);
		final T time5 = time2.multiply(time3);
		// GMST computation
		final T gTime = era.add(7.03270726e-8).add(time.multiply(2.23603658710194e-2)).add(time2.add(6.7465784654e-6))
				.subtract(time3.multiply(2.1332e-12)).subtract(time4.multiply(1.452308e-10)).subtract(time5.multiply(1.784e-13));
		return gTime;
	}

	/**
	 * Compute the GLONASS date.
	 * 
	 * @return the date
	 * @param glonass time scale.
	 */
	private FieldAbsoluteDate<T> computeDate(final Field<T> field, final TimeScale glonass) {
		// Compute the number of Julian day for the current date
		final T jdn = jd0.add(0.5);
		// Coefficients
		final int a = (int) (jdn.getReal() + 32044);
		final int b = (4 * a + 3) / 146097;
		final int c = a - (146097 * b) / 4;
		final int d = (4 * c + 3) / 1461;
		final int e = c - (1461 * d) / 4;
		final int m = (5 * e + 2) / 153;
		// Year, month and day
		final int day = e - (153 * m + 2) / 5 + 1;
		final int month = m + 3 - 12 * (m / 10);
		final int year = 100 * b + d - 4800 + m / 10;

		return new FieldAbsoluteDate<T>(field, new DateComponents(year, month, day),
				new TimeComponents(secInNa.getReal()), glonass);
	}

	/**
	 * Replace the instance with a data transfer object for serialization.
	 * 
	 * @return data transfer object that will be serialized
	 */
	@DefaultDataContext
	private Object writeReplace() {
		return new DataTransferObject(field, na, n4, secInNa);
	}

	/** Internal class used only for serialization. */
	@DefaultDataContext
	private class DataTransferObject implements Serializable {

		/** Serializable UID. */
		private static final long serialVersionUID = 20190131L;

		/** The number of the current day in a four year interval N<sub>a</sub>. */
		private final int na;

		/** The number of the current four year interval N<sub>4</sub>. */
		private final int n4;

		/** Number of seconds since N<sub>a</sub>. */
		private final T secInNa;
		
		private final Field<T> field;

		/**
		 * Simple constructor.
		 * 
		 * @param na      the number of the current day in a four year interval
		 * @param n4      the number of the current four year interval
		 * @param secInNa the number of seconds since na start
		 */
		DataTransferObject(final Field<T> field, final int na, final int n4, final T secInNa) {
			this.field = field;
			this.na = na;
			this.n4 = n4;
			this.secInNa = secInNa;
		}

		/**
		 * Replace the deserialized data transfer object with a {@link GPSDate}.
		 * 
		 * @return replacement {@link GPSDate}
		 */
		private Object readResolve() {
			return new FieldGLONASSDate<>(field, na, n4, secInNa);
		}

	}

}
