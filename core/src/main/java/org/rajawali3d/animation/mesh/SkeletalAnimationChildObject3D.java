/**
 * Copyright 2013 Dennis Ippel
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.rajawali3d.animation.mesh;

import android.opengl.GLES20;
import c.org.rajawali3d.gl.buffers.BufferInfo;
import org.rajawali3d.Object3D;
import org.rajawali3d.animation.mesh.SkeletalAnimationObject3D.SkeletalAnimationException;
import org.rajawali3d.cameras.Camera;
import org.rajawali3d.geometry.NonInterleavedGeometry;
import org.rajawali3d.materials.plugins.SkeletalAnimationMaterialPlugin;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.util.RajLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/*
 * Making Skeleton a top level object, because it is shared between all the meshes and needs to be animated only once
 * per rendering cycle
 */
public class SkeletalAnimationChildObject3D extends AAnimationObject3D {
    public static final int MAX_WEIGHTS_PER_VERTEX = 8;

    public  SkeletalAnimationObject3D mSkeleton;
    private SkeletalAnimationSequence mSequence;
    public  float[]                   boneWeights1, boneIndexes1, boneWeights2, boneIndexes2;

    private BufferInfo mboneWeights1BufferInfo = new BufferInfo();
    private BufferInfo mboneIndexes1BufferInfo = new BufferInfo();
    private BufferInfo mboneWeights2BufferInfo = new BufferInfo();
    private BufferInfo mboneIndexes2BufferInfo = new BufferInfo();

    /**
     * FloatBuffer containing first 4 bone weights
     */
    protected FloatBuffer mboneWeights1;

    /**
     * FloatBuffer containing first 4 bone indexes
     */
    protected FloatBuffer mboneIndexes1;

    /**
     * FloatBuffer containing bone weights if the maxWeightCount > 4
     */
    protected FloatBuffer mboneWeights2;

    /**
     * FloatBuffer containing bone indexes if the maxWeightCount > 4 Current implementation supports up to 8 joint
     * weights per vertex
     */
    protected FloatBuffer                     mboneIndexes2;
    // private SkeletonMeshData mMesh;
    protected int                             mMaxBoneWeightsPerVertex;
    private   int                             mNumVertices;
    private   BoneVertex[]                    mVertices;
    private   BoneWeight[]                    mWeights;
    private   SkeletalAnimationMaterialPlugin mMaterialPlugin;
    private boolean mInverseZScale = false;

    public SkeletalAnimationChildObject3D() {
        super();
        mSkeleton = null;
    }

    @Override
    public void calculateModelMatrix(final Matrix4 parentMatrix) {
        super.calculateModelMatrix(parentMatrix);

        if (mInverseZScale) {
            mMMatrix.scale(1, 1, -1);
        }
    }

    public void setShaderParams(Camera camera) {
        super.setShaderParams(camera);

        if (mMaterialPlugin == null) {
            mMaterialPlugin = (SkeletalAnimationMaterialPlugin) mMaterial
                    .getPlugin(SkeletalAnimationMaterialPlugin.class);
        }
        mMaterialPlugin.setBone1Indices(mboneIndexes1BufferInfo.glHandle);
        mMaterialPlugin.setBone1Weights(mboneWeights1BufferInfo.glHandle);
        if (mMaxBoneWeightsPerVertex > 4) {
            mMaterialPlugin.setBone2Indices(mboneIndexes2BufferInfo.glHandle);
            mMaterialPlugin.setBone2Weights(mboneWeights2BufferInfo.glHandle);
        }
        mMaterialPlugin.setBoneMatrix(mSkeleton.uBoneMatrix);
    }

    public void setSkeleton(Object3D skeleton) {
        if (skeleton instanceof SkeletalAnimationObject3D) {
            mSkeleton = (SkeletalAnimationObject3D) skeleton;
        } else {
            throw new RuntimeException(
                    "Skeleton must be of type AnimationSkeleton!");
        }
    }

    public int getNumJoints() {
        return (mSkeleton == null || mSkeleton.getJoints() == null ? 0 : mSkeleton.getJoints().length);
    }

    public void setSkeletonMeshData(BoneVertex[] vertices, BoneWeight[] weights) {
        setSkeletonMeshData(vertices.length, vertices, weights.length, weights);
    }

    public void setSkeletonMeshData(int numVertices, BoneVertex[] vertices, int numWeights, BoneWeight[] weights) {
        mNumVertices = numVertices;
        mVertices = vertices;
        mWeights = weights;

        prepareBoneWeightsAndIndices();
        mboneIndexes1 = alocateBuffer(mboneIndexes1, boneIndexes1);
        mboneWeights1 = alocateBuffer(mboneWeights1, boneWeights1);
        mboneIndexes1BufferInfo.buffer = mboneIndexes1;
        mboneIndexes1BufferInfo.bufferType = BufferInfo.FLOAT_BUFFER;
        mboneIndexes1BufferInfo.target = GLES20.GL_ARRAY_BUFFER;
        mboneWeights1BufferInfo.buffer = mboneWeights1;
        mboneWeights1BufferInfo.bufferType = BufferInfo.FLOAT_BUFFER;
        mboneWeights1BufferInfo.target = GLES20.GL_ARRAY_BUFFER;
        mGeometry.addBuffer(mboneIndexes1BufferInfo);
        mGeometry.addBuffer(mboneWeights1BufferInfo);
        if (mMaxBoneWeightsPerVertex > 4) {
            mboneIndexes2 = alocateBuffer(mboneIndexes2, boneIndexes2);
            mboneWeights2 = alocateBuffer(mboneWeights2, boneWeights2);
            mboneIndexes2BufferInfo.buffer = mboneIndexes2;
            mboneIndexes2BufferInfo.bufferType = BufferInfo.FLOAT_BUFFER;
            mboneIndexes2BufferInfo.target = GLES20.GL_ARRAY_BUFFER;
            mboneWeights2BufferInfo.buffer = mboneWeights2;
            mboneWeights2BufferInfo.bufferType = BufferInfo.FLOAT_BUFFER;
            mboneWeights2BufferInfo.target = GLES20.GL_ARRAY_BUFFER;
            mGeometry.addBuffer(mboneIndexes2BufferInfo);
            mGeometry.addBuffer(mboneWeights2BufferInfo);
        }
    }

    private FloatBuffer alocateBuffer(FloatBuffer buffer, float[] data) {
        if (buffer == null) {
            buffer = ByteBuffer
                    .allocateDirect(data.length * NonInterleavedGeometry.FLOAT_SIZE_BYTES * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();

            buffer.put(data);
            buffer.position(0);
        } else {
            buffer.put(data);
        }
        return buffer;
    }

    public void play() {
        if (mSequence == null) {
            RajLog.e("[BoneAnimationObject3D.play()] Cannot play animation. No sequence was set.");
            return;
        }
        super.play();
        for (Object3D child : mChildren) {
            if (child instanceof AAnimationObject3D) {
                ((AAnimationObject3D) child).play();
            }
        }
    }

    public void setAnimationSequence(SkeletalAnimationSequence sequence) {
        mSequence = sequence;
        if (sequence != null && sequence.getFrames() != null) {
            mNumFrames = sequence.getFrames().length;

            for (Object3D child : mChildren) {
                if (child instanceof SkeletalAnimationChildObject3D) {
                    ((SkeletalAnimationChildObject3D) child).setAnimationSequence(sequence);
                }
            }
        }
    }

    /*
     * Creates boneWeights and boneIndexes arrays
     */
    public void prepareBoneWeightsAndIndices() {
        int weightStep = 4;
        boneWeights1 = new float[mNumVertices * 4];
        boneIndexes1 = new float[mNumVertices * 4];
        boneWeights2 = new float[mNumVertices * 4];
        boneIndexes2 = new float[mNumVertices * 4];
        for (int i = 0; i < mNumVertices; i++) {
            BoneVertex vert = mVertices[i];
            for (int j = 0; j < vert.numWeights; ++j) {
                BoneWeight weight = mWeights[vert.weightIndex + j];

                if (j < 4) {
                    boneWeights1[weightStep * i + j] = weight.weightValue;
                    boneIndexes1[weightStep * i + j] = weight.jointIndex;
                } else {
                    int jj = j % 4;
                    boneWeights2[weightStep * i + jj] = weight.weightValue;
                    boneIndexes2[weightStep * i + jj] = weight.jointIndex;
                }
            }
        }
    }

    public void setMaxBoneWeightsPerVertex(int maxBoneWeightsPerVertex) throws SkeletalAnimationException {
        mMaxBoneWeightsPerVertex = maxBoneWeightsPerVertex;
        if (mMaxBoneWeightsPerVertex > MAX_WEIGHTS_PER_VERTEX) {
            throw new SkeletalAnimationObject3D.SkeletalAnimationException("A maximum of " + MAX_WEIGHTS_PER_VERTEX
                                                                           + " weights per vertex is allowed. Your "
                                                                           + "model uses more then "
                                                                           + MAX_WEIGHTS_PER_VERTEX + ".");
        }
    }

    public int getMaxBoneWeightsPerVertex() {
        return mMaxBoneWeightsPerVertex;
    }

    public static class BoneVertex {

        public Vector2 textureCoordinate = new Vector2();
        public Vector3 normal            = new Vector3();
        public int weightIndex;
        public int numWeights;
    }

    public static class BoneWeight {

        public int   jointIndex;
        public float weightValue;
        public Vector3 position = new Vector3();
    }

    public void setInverseZScale(boolean value) {
        mInverseZScale = value;
    }

    public void setData(float[] vertices, float[] normals, float[] textureCoords, float[] colors, int[] indices,
                        boolean createVBOs) {
        setData
                (
                        vertices, GLES20.GL_STREAM_DRAW,
                        normals, GLES20.GL_STREAM_DRAW,
                        textureCoords, GLES20.GL_STATIC_DRAW,
                        colors, GLES20.GL_STATIC_DRAW,
                        indices, GLES20.GL_STATIC_DRAW,
                        createVBOs
                );

    }

    // The cloneChildren argument isn't used here; it's included to prevent
    // calls to clone(boolean, boolean) from falling through to Object3D
    public SkeletalAnimationChildObject3D clone(boolean copyMaterial, boolean cloneChildren) {
        SkeletalAnimationChildObject3D clone = new SkeletalAnimationChildObject3D();
        clone.setRotation(getOrientation());
        clone.setPosition(getPosition());
        clone.setScale(getScale());
        clone.getGeometry().copyFrom(mGeometry);
        clone.isContainer(mIsContainerOnly);
        clone.setMaterial(mMaterial);
        clone.mElementsBufferType = GLES20.GL_UNSIGNED_INT;
        clone.mTransparent = this.mTransparent;
        clone.mEnableBlending = this.mEnableBlending;
        clone.mBlendFuncSFactor = this.mBlendFuncSFactor;
        clone.mBlendFuncDFactor = this.mBlendFuncDFactor;
        clone.mEnableDepthTest = this.mEnableDepthTest;
        clone.mEnableDepthMask = this.mEnableDepthMask;


        clone.setAnimationSequence(mSequence);
        clone.setSkeleton(mSkeleton);
        try {
            clone.setMaxBoneWeightsPerVertex(mMaxBoneWeightsPerVertex);
        } catch (SkeletalAnimationException e) {
            e.printStackTrace();
        }
        clone.setSkeletonMeshData(mNumVertices, mVertices, 0, mWeights);
        clone.setInverseZScale(mInverseZScale);
        return clone;
    }
}
