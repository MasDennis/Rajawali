package rajawali.animation;

import rajawali.math.Number3D;
import rajawali.math.Number3D.Axis;

/**
 * Animation that orbits {@link ATransformable3D} object around a point. 
 * @author Andrew Jo
 *
 */
public class EllipticalOrbitAnimation3D extends Animation3D {

	// Pre-calculated for performance optimization
	protected final double PI_DIV_180 = 0.01745329251994329576923690768489;

	/**
	 * Defines the direction of the orbit around the parent body.
	 */
	public enum OrbitDirection {
		CLOCKWISE,
		COUNTERCLOCKWISE
	}
	
	protected Number3D mFocalPoint;
	protected Number3D mPeriapsis;
	protected Number3D mNormal;
	protected double mEccentricity;
	protected OrbitDirection mDirection;
	
	/**
	 * Defines an elliptical orbit around a point.
	 * @param focalPoint Point which the {@link ATransformable3D} orbits around.
	 * @param periapsis Point which the object passes closest to the focal point.
	 * @param normal Normal to the orbital plane. This defines the orbital inclination.
	 * @param eccentricity Eccentricity of the orbit. Zero value results in a circular orbit.
	 * @param direction Direction of the orbit.
	 */
	public EllipticalOrbitAnimation3D(Number3D focalPoint, Number3D periapsis, Number3D normal, double eccentricity,
			OrbitDirection direction) {
		super();
		mFocalPoint = focalPoint;
		mPeriapsis = periapsis;
		mNormal = normal.clone();
		mEccentricity = eccentricity;
		mDirection = direction;
	}
	
	
	/**
	 * Defines an elliptical orbit around a point with no orbital inclination.
	 * @param focalPoint Point which the {@link ATransformable3D} orbits around.
	 * @param periapsis Point which the object passes closest to the focal point.
	 * @param eccentricity Eccentricity of the orbit. Zero value results in a circular orbit.
	 * @param direction Direction of the orbit.
	 */
	public EllipticalOrbitAnimation3D(Number3D focalPoint, Number3D periapsis, double eccentricity,
			OrbitDirection direction) {
		this(focalPoint, periapsis, Number3D.getAxisVector(Axis.Y), eccentricity, direction);
	}
	
	@Override
	protected void applyTransformation() {
		// Everything here is stored in double precision because single precision floating point causes a major
		// overflow. Number3D still stores internally in single precision so all the calculation will be done in here
		// until Number3D is completely overhauled. Theory behind math in this method can be looked up easily on
		// Wikipedia.
		
		// Angle in radians (interpolated time from 0 to 1 results in radian angle 0 to 2PI)
		double angle = (mDirection == OrbitDirection.CLOCKWISE ? -1 : 1) * 360f * mInterpolatedTime * PI_DIV_180;
		
		// Calculate the distances of periapsis and apoapsis to the focal point.
		double periapsisRadius = mPeriapsis.distanceTo(mFocalPoint);
		double apoapsisRadius = periapsisRadius * (1 + mEccentricity) / (1 - mEccentricity);

		// Get the apoapsis point which will be needed to calculate the center point of the ellipse.
		// NOTE: Discard least significant digits after 8th decimal places to lower the computational error epsilon.
		double uAx = (Math.round(mFocalPoint.x * 1e8) - Math.round(mPeriapsis.x * 1e8)) / 1e8;
		double uAy = (Math.round(mFocalPoint.y * 1e8) - Math.round(mPeriapsis.y * 1e8)) / 1e8;
		double uAz = (Math.round(mFocalPoint.z * 1e8) - Math.round(mPeriapsis.z * 1e8)) / 1e8;
		double mod = Math.sqrt(uAx * uAx + uAy * uAy + uAz * uAz);
		if (mod != 0 && mod != 1) {
			mod = 1 / mod;
			uAx *= mod;
			uAy *= mod;
			uAz *= mod;
		}
		double apoapsisDir_x = Math.round(uAx * apoapsisRadius * 1e8) / 1e8;
		double apoapsisDir_y = Math.round(uAy * apoapsisRadius * 1e8) / 1e8;
		double apoapsisDir_z = Math.round(uAz * apoapsisRadius * 1e8) / 1e8;
		double apoapsisPos_x = Math.round((apoapsisDir_x + mFocalPoint.x) * 1e8) / 1e8;
		double apoapsisPos_y = Math.round((apoapsisDir_y + mFocalPoint.y) * 1e8) / 1e8;
		double apoapsisPos_z = Math.round((apoapsisDir_z + mFocalPoint.z) * 1e8) / 1e8;

		// Midpoint between apoapsis and periapsis is the center of the ellipse.
		double center_x = Math.round(((mPeriapsis.x + apoapsisPos_x) / 2) * 1e8) / 1e8;
		double center_y = Math.round(((mPeriapsis.y + apoapsisPos_y) / 2) * 1e8) / 1e8;
		double center_z = Math.round(((mPeriapsis.z + apoapsisPos_z) / 2) * 1e8) / 1e8;

		// Calculate semiminor axis length.
		double b = Math.sqrt(periapsisRadius * apoapsisRadius);

		// Direction vector to periapsis from the center point and ascending node from the center point
		double semimajorAxis_x = Math.round((mPeriapsis.x - center_x) * 1e8) / 1e8;
		double semimajorAxis_y = Math.round((mPeriapsis.y - center_y) * 1e8) / 1e8;
		double semimajorAxis_z = Math.round((mPeriapsis.z - center_z) * 1e8) / 1e8;
		double unitSemiMajorAxis_x = semimajorAxis_x;
		double unitSemiMajorAxis_y = semimajorAxis_y;
		double unitSemiMajorAxis_z = semimajorAxis_z;
		mod = Math.sqrt(semimajorAxis_x * semimajorAxis_x + semimajorAxis_y * semimajorAxis_y + semimajorAxis_z
				* semimajorAxis_z);
		if (mod != 0 && mod != 1) {
			mod = 1 / mod;
			unitSemiMajorAxis_x *= mod;
			unitSemiMajorAxis_y *= mod;
			unitSemiMajorAxis_z *= mod;
		}

		// Translate normal vector to the center point.
		Number3D unitNormal = mNormal.clone();
		unitNormal.normalize();
		double uNx = Math.round(unitNormal.x * 1e8) / 1e8;
		double uNy = Math.round(unitNormal.y * 1e8) / 1e8;
		double uNz = Math.round(unitNormal.z * 1e8) / 1e8;
		double normalCenter_x = center_x + uNx;
		double normalCenter_y = center_y + uNy;
		double normalCenter_z = center_z + uNz;
		mod = Math.sqrt(normalCenter_x * normalCenter_x + normalCenter_y * normalCenter_y + normalCenter_z
				* normalCenter_z);
		if (mod != 0 && mod != 1) {
			mod = 1 / mod;
			normalCenter_x *= mod;
			normalCenter_y *= mod;
			normalCenter_z *= mod;
		}

		// We can calculate the semiminor axis from unit vector of cross product of semimajor axis and the normal.
		Number3D unitSemiminorAxis = Number3D.cross(new Number3D(unitSemiMajorAxis_x, unitSemiMajorAxis_y,
				unitSemiMajorAxis_z), new Number3D(normalCenter_x, normalCenter_y, normalCenter_z));
		Number3D semiminorAxis = Number3D.multiply(unitSemiminorAxis, (float) b);

		// Parametric equation for ellipse in 3D space.
		double x = center_x + (Math.cos(angle) * semimajorAxis_x) + (Math.sin(angle) * semiminorAxis.x);
		double y = center_y + (Math.cos(angle) * semimajorAxis_y) + (Math.sin(angle) * semiminorAxis.y);
		double z = center_z + (Math.cos(angle) * semimajorAxis_z) + (Math.sin(angle) * semiminorAxis.z);
		mTransformable3D.setPosition((float) x, (float) y, (float) z);
	}
}
