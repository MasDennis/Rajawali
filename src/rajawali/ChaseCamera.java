package rajawali;

import rajawali.math.Vector3;
import rajawali.math.Quaternion;

public class ChaseCamera extends Camera {
	protected Vector3 mCameraOffset;
	protected BaseObject3D mObjectToChase;
	protected float mSlerpFactor = .1f;
	protected float[] mRotMatrix;
	protected Vector3 mTmpVec;
	protected Quaternion mTmpOrientation;
	protected Quaternion mPreviousOrientation;
	
	public ChaseCamera() {
		this(new Vector3(0, 3, 16), .1f, null);
	}
	
	public ChaseCamera(Vector3 cameraOffset, float slerpFactor) {
		this(cameraOffset, .1f, null);
	}

	public ChaseCamera(Vector3 cameraOffset, float slerpFactor, BaseObject3D objectToChase) {
		super();
		mTmpOrientation = new Quaternion();
		mPreviousOrientation = new Quaternion();
		mTmpVec = new Vector3();
		mCameraOffset = cameraOffset;
		mObjectToChase = objectToChase;
		mSlerpFactor = slerpFactor;
		mRotMatrix = new float[16];
	}
	
	private Quaternion mTmpQuatChase=new Quaternion();
	
	public float[] getViewMatrix() {
		mPosition.setAllFrom(mObjectToChase.getPosition());
		mTmpVec.setAllFrom(mCameraOffset);
		
		mTmpOrientation.slerpSelf(mPreviousOrientation, mObjectToChase.getOrientation(mTmpQuatChase), mSlerpFactor);
		mTmpOrientation.toRotationMatrix(mRotMatrix);
		mTmpVec.multiply(mRotMatrix);
		
		mPosition.add(mTmpVec);
		setLookAt(mObjectToChase.getPosition());
		
		mPreviousOrientation.setAllFrom(mTmpOrientation);
		
		return super.getViewMatrix();
	}

	public void setCameraOffset(Vector3 offset) {
		mCameraOffset.setAllFrom(offset);
	}
	
	public Vector3 getCameraOffset() {
		return mCameraOffset;
	}
	
	public void setSlerpFactor(float factor) {
		mSlerpFactor = factor;
	}
	
	public float getSlerpFactor() {
		return mSlerpFactor;
	}

	public BaseObject3D getObjectToChase() {
		return mObjectToChase;
	}

	public void setObjectToChase(BaseObject3D objectToChase) {
		this.mObjectToChase = objectToChase;
	}
	
	public BaseObject3D getChasedObject() {
		return mObjectToChase;
	}	
}