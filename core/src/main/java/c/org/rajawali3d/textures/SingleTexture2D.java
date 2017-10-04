/**
 * Copyright 2013 Dennis Ippel
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package c.org.rajawali3d.textures;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;

import net.jcip.annotations.ThreadSafe;

import org.rajawali3d.util.RajLog;

import c.org.rajawali3d.annotations.RenderThread;
import c.org.rajawali3d.textures.annotation.PixelFormat;
import c.org.rajawali3d.textures.annotation.Type.TextureType;

/**
 * This class is used to specify common functions of a single 2D texture. Subclasses are expected to be thread safe.
 *
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 * @author dennis.ippel
 */
@SuppressWarnings("WeakerAccess")
@ThreadSafe
public abstract class SingleTexture2D extends BaseTexture {

    /**
     * The texture data.
     */
    @Nullable
    private volatile TextureDataReference textureData;

    /**
     * Basic no-args constructor used by some subclasses. No initialization is performed.
     */
    protected SingleTexture2D() throws TextureException {
        super();
        setTextureTarget(GLES20.GL_TEXTURE_2D);
    }

    /**
     * Constructs a new {@link SingleTexture2D} with the specified name and type.
     *
     * @param type {@link TextureType} The texture usage type.
     * @param name {@link String} The texture name.
     */
    public SingleTexture2D(@TextureType int type, @NonNull String name) {
        super(type, name);
        setTextureTarget(GLES20.GL_TEXTURE_2D);
    }

    /**
     * Constructs a new {@link SingleTexture2D} with data provided by the Android resource id. The texture name is
     * set by
     * querying Android for the resource name.
     *
     * @param context    {@link Context} The application context.
     * @param type       {@link TextureType} The texture usage type.
     * @param resourceId {@code int} The Android resource id to load from.
     *
     * @throws TextureException if there is an error reading the resource.
     */
    public SingleTexture2D(@NonNull Context context, @TextureType int type, @DrawableRes int resourceId)
        throws TextureException {
        this(type, context.getResources().getResourceName(resourceId));
        setTextureDataFromResourceId(context, resourceId);
    }

    /**
     * Constructs a new {@link SingleTexture2D} with the provided data.
     *
     * @param type {@link TextureType} The texture usage type.
     * @param name {@link String} The texture name.
     * @param data {@link TextureDataReference} The texture data.
     */
    public SingleTexture2D(@TextureType int type, @NonNull String name, @NonNull TextureDataReference data) {
        this(type, name);
        setTextureData(data);
    }

    /**
     * Constructs a new {@link SingleTexture2D} with data and settings from the provided {@link SingleTexture2D}.
     *
     * @param other The other {@link SingleTexture2D}.
     *
     * @throws TextureException Thrown if an error occurs during any part of the texture copy process.
     */
    public SingleTexture2D(@NonNull SingleTexture2D other) throws TextureException {
        super(other);
        setFrom(other);
    }

    /**
     * Copies all properties and data from another {@link SingleTexture2D}.
     *
     * @param other The other {@link SingleTexture2D}.
     *
     * @throws TextureException Thrown if an error occurs during any part of the texture copy process.
     */
    public void setFrom(@NonNull SingleTexture2D other) throws TextureException {
        final TextureDataReference data = other.getTextureData();
        if (data != null) {
            super.setFrom(other);
            setTextureData(data);
        } else {
            throw new TextureException("Texture data was null!");
        }
    }

    /**
     * Sets the data used by this {@link SingleTexture2D} from an Android resource id. This will create a new
     * {@link TextureDataReference} and set it as the active data. Do not use this method if you wish to use the
     * texture as a Luminance texture as it will assume a {@link GLES20#GL_RGB} or {@link GLES20#GL_RGBA}
     * {@link PixelFormat}.
     *
     * @param context    {@link Context} The Android application context.
     * @param resourceId {@code int} The Android resource id to load from.
     *
     * @return The new {@link TextureDataReference} which was created.
     * @throws TextureException if there is an error reading the resource.
     */
    @NonNull
    public TextureDataReference setTextureDataFromResourceId(@NonNull Context context,
                                                             @RawRes @DrawableRes int resourceId)
        throws TextureException {
        // Prevent the bitmap from being scaled as it is decoded
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        // Decode the bitmap
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        final TextureDataReference reference = new TextureDataReference(bitmap, null,
            bitmap.getConfig().equals(Config.RGB_565) ? GLES20.GL_RGB : GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE, bitmap.getWidth(), bitmap.getHeight());
        setTextureData(reference);
        return reference;
    }

    /**
     * Sets the data used by this {@link SingleTexture2D} from the provided {@link TextureDataReference}. The data
     * reference will have its reference count incremented and any existing data reference will be released.
     *
     * @param data The new {@link TextureDataReference} to use.
     */
    public void setTextureData(@Nullable TextureDataReference data) {
        // Save a stack reference to the old data
        final TextureDataReference oldData = this.textureData;

        // Save and increment reference count of new data
        if (data != null) {
            data.holdReference();
        }
        textureData = data;

        // Release any existing reference
        if (oldData != null) {
            oldData.recycle();
        }
    }

    /**
     * Retrieves the current {@link TextureDataReference} used by this {@link SingleTexture2D}.
     *
     * @return The current {@link TextureDataReference}.
     */
    @Nullable
    public TextureDataReference getTextureData() {
        return textureData;
    }

    @RenderThread
    @Override
    void add() throws TextureException {
        // Check if there is valid data
        final TextureDataReference textureData = this.textureData;

        if (textureData == null || textureData.isDestroyed()
            || (textureData.hasBuffer() && textureData.getByteBuffer().limit() == 0 && !textureData.hasBitmap())) {
            throw new TextureException("Texture could not be added because there is no valid data set.");
        }

        // Set the dimensions
        setWidth(textureData.getWidth());
        setHeight(textureData.getHeight());

        // Generate a texture id
        final int textureId = generateTextureId();

        if (textureId > 0) {
            // If a valid id was generated...
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            // Handle minification filtering
            applyMinificationFilter();

            // Handle magnification filtering
            applyMagnificationFilter();

            // Handle anisotropy if needed.
            applyAnisotropy();

            // Handle s coordinate wrapping
            applySWrapping();

            // Handle t coordinate wrapping
            applyTWrapping();

            // Push the texture data
            if (textureData.hasBuffer()) {
                if (getWidth() == 0 || getHeight() == 0) {
                    throw new TextureException(
                        "Could not create ByteBuffer texture. One or more of the following properties haven't "
                            + "been set: width or height format");
                }
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, getTexelFormat(), getWidth(), getHeight(), 0,
                    textureData.getPixelFormat(), textureData.getDataType(),
                    textureData.getByteBuffer());
            } else {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, getTexelFormat(), textureData.getBitmap(), 0);
            }

            // Generate mipmaps if enabled
            if (isMipmaped()) {
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            }

            // Store the texture id
            setTextureId(textureId);
        } else {
            throw new TextureException("Failed to generate a new texture id.");
        }

        if (willRecycle()) {
            setTextureData(null);
        }

        // Rebind the null texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @RenderThread
    @Override
    void remove() throws TextureException {
        final TextureDataReference textureData = this.textureData;
        final int id = getTextureId();
        if (id > 0) {
            // Call delete with GL only if necessary
            GLES20.glDeleteTextures(1, new int[]{getTextureId()}, 0);
            if (textureData != null) {
                // When removing a texture, release a reference count for its data if we have saved it.
                textureData.recycle();
            }
        }

        //TODO: Notify materials that were using this texture
    }

    @RenderThread
    @Override
    void replace() throws TextureException {
        final TextureDataReference textureData = this.textureData;
        if (textureData == null || textureData.isDestroyed() || (textureData.hasBuffer()
            && textureData.getByteBuffer().limit() == 0 && !textureData.hasBitmap())) {
            final String error = "Texture could not be replaced because there is no Bitmap or ByteBuffer set.";
            RajLog.e(error);
            throw new TextureException(error);
        }

        if (textureData.getWidth() != getWidth() || textureData.getHeight() != getHeight()) {
            throw new TextureException(
                "Texture could not be updated because the texture size is different from the original.");
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTextureId());

        if (textureData.hasBuffer()) {
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, getWidth(), getHeight(), textureData.getPixelFormat(),
                GLES20.GL_UNSIGNED_BYTE, textureData.getByteBuffer());
        } else {
            int bitmapFormat = textureData.getBitmap().getConfig() == Config.ARGB_8888 ? GLES20.GL_RGBA : GLES20.GL_RGB;

            if (bitmapFormat != getTexelFormat()) {
                throw new TextureException(
                    "Texture could not be updated because the texel format is different from the original");
            }

            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, textureData.getBitmap(), getTexelFormat(),
                GLES20.GL_UNSIGNED_BYTE);
        }

        if (isMipmaped()) {
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @RenderThread
    @Override
    void reset() throws TextureException {
        setTextureData(null);
    }

    //TODO: Update method
}
