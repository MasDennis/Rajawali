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
package c.org.rajawali3d.gl;

import c.org.rajawali3d.gl.extensions.GLExtension;
import org.rajawali3d.util.RajLog;

import android.annotation.TargetApi;
import android.opengl.EGLExt;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import java.util.HashMap;
import java.util.Map;

import c.org.rajawali3d.gl.extensions.EXTDebugMarker;
import c.org.rajawali3d.gl.extensions.OESElementIndexUINT;
import c.org.rajawali3d.gl.extensions.texture.AMDCompressedATCTexture;
import c.org.rajawali3d.gl.extensions.texture.EXTTextureCompressionDXT1;
import c.org.rajawali3d.gl.extensions.texture.EXTTextureCompressionS3TC;
import c.org.rajawali3d.gl.extensions.texture.EXTTextureFilterAnisotropic;
import c.org.rajawali3d.gl.extensions.texture.IMGTextureCompressionPVRTC;
import c.org.rajawali3d.gl.extensions.texture.KHRTextureCompressionASTC;
import c.org.rajawali3d.gl.extensions.texture.NVTextureCompressionLATC;
import c.org.rajawali3d.gl.extensions.texture.NVTextureCompressionS3TC;
import c.org.rajawali3d.gl.extensions.texture.NVTextureCompressionS3TCUpdate;
import c.org.rajawali3d.gl.extensions.texture.OESCompressedETC1RGB8;
import c.org.rajawali3d.gl.extensions.texture.OESCompressedPalettedTexture;
import c.org.rajawali3d.gl.extensions.texture.OESTexture3D;
import c.org.rajawali3d.gl.extensions.texture.OESTextureCompressionASTC;


/**
 * Lists all OpenGL specific capabilities
 *
 * @author dennis.ippel
 */
public class Capabilities {

    static {
        System.loadLibrary("glExtensions");
    }

    private static Capabilities instance = null;

    private static volatile boolean glesVersionChecked = false;

    private static int eglMajorVersion;
    private static int eglMinorVersion;
    private static int glesMajorVersion;

    private final int maxTextureSize;
    private final int maxCombinedTextureImageUnits;
    private final int maxCubeMapTextureSize;
    private final int maxFragmentUniformVectors;
    private final int maxRenderbufferSize;
    private final int maxTextureImageUnits;
    private final int maxVaryingVectors;
    private final int maxVertexAttribs;
    private final int maxVertexTextureImageUnits;
    private final int maxVertexUniformVectors;
    private final int maxViewportWidth;
    private final int maxViewportHeight;
    private final int minAliasedLineWidth;
    private final int maxAliasedLineWidth;
    private final int minAliasedPointSize;
    private final int maxAliasedPointSize;

    @NonNull
    private final String vendor;
    @NonNull
    private final String renderer;
    @NonNull
    private final String version;
    @NonNull
    private final String[] extensions;

    private final Map<String, GLExtension> loadedExtensions;

    private int[] param;

    private Capabilities() {
        RajLog.d("Fetching device capabilities.");

        param = new int[1];
        vendor = GLES20.glGetString(GLES20.GL_VENDOR);
        renderer = GLES20.glGetString(GLES20.GL_RENDERER);
        version = GLES20.glGetString(GLES20.GL_VERSION);

        loadedExtensions = new HashMap<>();

        maxCombinedTextureImageUnits = getInt(GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        maxCubeMapTextureSize = getInt(GLES20.GL_MAX_CUBE_MAP_TEXTURE_SIZE);
        maxFragmentUniformVectors = getInt(GLES20.GL_MAX_FRAGMENT_UNIFORM_VECTORS);
        maxRenderbufferSize = getInt(GLES20.GL_MAX_RENDERBUFFER_SIZE);
        maxTextureImageUnits = getInt(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS);
        maxTextureSize = getInt(GLES20.GL_MAX_TEXTURE_SIZE);
        maxVaryingVectors = getInt(GLES20.GL_MAX_VARYING_VECTORS);
        maxVertexAttribs = getInt(GLES20.GL_MAX_VERTEX_ATTRIBS);
        maxVertexTextureImageUnits = getInt(GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS);
        maxVertexUniformVectors = getInt(GLES20.GL_MAX_VERTEX_UNIFORM_VECTORS);
        maxViewportWidth = getInt(GLES20.GL_MAX_VIEWPORT_DIMS, 2, 0);
        maxViewportHeight = getInt(GLES20.GL_MAX_VIEWPORT_DIMS, 2, 1);
        minAliasedLineWidth = getInt(GLES20.GL_ALIASED_LINE_WIDTH_RANGE, 2, 0);
        maxAliasedLineWidth = getInt(GLES20.GL_ALIASED_LINE_WIDTH_RANGE, 2, 1);
        minAliasedPointSize = getInt(GLES20.GL_ALIASED_POINT_SIZE_RANGE, 2, 0);
        maxAliasedPointSize = getInt(GLES20.GL_ALIASED_POINT_SIZE_RANGE, 2, 1);

        final String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        this.extensions = extensions.split(" ");
    }

    @NonNull
    public static synchronized Capabilities getInstance() {
        if (instance == null) {
            instance = new Capabilities();
        }
        return instance;
    }

    @VisibleForTesting
    public static synchronized void clearInstance() {
        instance = null;
        glesVersionChecked = false;
    }

    @VisibleForTesting
    static void checkGLESVersion() {
        // Get an EGL context and display
        final EGL10 egl = (EGL10) EGLContext.getEGL();
        final EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        final int[] version = new int[2];
        if (!egl.eglInitialize(display, version)) {
            throw new IllegalStateException("Failed to initialize an EGL context while getting device capabilities.");
        }
        eglMajorVersion = version[0];
        eglMinorVersion = version[1];
        // RajLog.d("Device EGL Version: " + version[0] + "." + version[1]);

        // Assume GLES 2 by default
        glesMajorVersion = 2;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // The API for GLES3 might exist, we need to check
            // RajLog.d("Attempting to get an OpenGL ES 3 config.");
            checkGLESVersionIs3(egl, display);
        }
        egl.eglTerminate(display);
        // RajLog.d("Determined GLES Major version to be: " + mGLESMajorVersion);
        glesVersionChecked = true;
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    static void checkGLESVersionIs3(@NonNull EGL10 egl, EGLDisplay display) {
        // Find out how many EGLConfigs exist
        final int[] num_config = new int[1];
        egl.eglGetConfigs(display, null, 0, num_config);

        // Allocate and retrieve the EGLConfigs/handles
        int configCount = num_config[0];
        final EGLConfig[] configs = new EGLConfig[configCount];
        egl.eglGetConfigs(display, configs, configCount, num_config);

        // Check for a config that supports GLES 3 (using a manual search rather than
        // eglChooseConfig(), which may simply ignore the new ES3 bit on older versions)
        final int value[] = new int[1];
        for (EGLConfig config : configs) {
            egl.eglGetConfigAttrib(display, config, EGL10.EGL_RENDERABLE_TYPE, value);
            if ((value[0] & EGLExt.EGL_OPENGL_ES3_BIT_KHR) != 0) {
                // TODO is this secondary check for color sizes necessary, correct, and useful?
                // We have at least one GLES 3 config, can now use eglChooseConfig()
                // to see if one of them has at least 4 bits per color
                final int[] configAttribs = {
                    EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
                    EGL10.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR, EGL10.EGL_NONE
                };
                value[0] = 0;
                egl.eglChooseConfig(display, configAttribs, configs, 1, value);
                glesMajorVersion = value[0] > 0 ? 3 : 2;
                break;
            }
        }
    }

    private int getInt(int pname) {
        GLES20.glGetIntegerv(pname, param, 0);
        return param[0];
    }

    private int getInt(int pname, int length, int index) {
        int[] params = new int[length];
        GLES20.glGetIntegerv(pname, params, 0);
        return params[index];
    }

    /**
     * The EGL major version number of this device.
     *
     * @return
     */
    public static int getEGLMajorVersion() {
        if (!glesVersionChecked) {
            checkGLESVersion();
        }
        return eglMajorVersion;
    }

    /**
     * The EGL minor version number of this device.
     *
     * @return
     */
    public static int getEGLMinorVersion() {
        if (!glesVersionChecked) {
            checkGLESVersion();
        }
        return eglMinorVersion;
    }

    /**
     * The highest GL ES major version number supported by this device.
     *
     * @return
     */
    public static int getGLESMajorVersion() {
        if (!glesVersionChecked) {
            checkGLESVersion();
        }
        return glesMajorVersion;
    }

    @NonNull
    public String getVendor() {
        return vendor;
    }

    @NonNull
    public String getRenderer() {
        return renderer;
    }

    @NonNull
    public String getVersion() {
        return version;
    }

    /**
     * Fetch the list of extension strings this device supports.
     *
     * @return {@code String[]} The list of extensions.
     */
    @NonNull
    public String[] getExtensions() {
        return extensions;
    }

    /**
     * Checks if a particular extension is supported by this device.
     *
     * @param extension {@link String} Non-null string of the extension to check for. This is case sensitive.
     *
     * @throws UnsupportedCapabilityException if the extension is not supported by the device.
     */
    public void verifyExtension(@NonNull String extension) throws UnsupportedCapabilityException {
        for (String ext : extensions) {
            if (extension.equals(ext)) {
                return;
            }
        }
        throw new UnsupportedCapabilityException("Extension (" + extension + ") is not supported!");
    }

    /**
     * Requests that the engine load the specified extension and configure it with the parameters from the current
     * device. If the extension has already been loaded, the previously configured extension is returned rather than
     * re-reading from the device.
     *
     * @param extension The name of the extension to load as specified in the GL extension registry.
     * @return The {@link GLExtension} instance which was loaded and configured.
     * @throws UnsupportedCapabilityException if the requested extension is not supported by the device.
     * @throws IllegalArgumentException Thrown if the requested extension is unknown to Rajawali. This does not mean
     * it is not available, however you will have to implement {@link GLExtension} for this extension and load it
     * manually then provide it to {@link Capabilities#usingExtension(GLExtension)} if you wish for its parameters to
     * be available through this central repository.
     *
     * @see <a href="https://www.opengl.org/registry/">OpenGL Registry</a>
     */
    public GLExtension loadExtension(@NonNull String extension) throws UnsupportedCapabilityException,
        IllegalArgumentException {
        if (!loadedExtensions.containsKey(extension)) {
            GLExtension glExtension;
            switch (extension) {
                // Compressed Texture Extensions
                case AMDCompressedATCTexture.name:
                    glExtension = AMDCompressedATCTexture.load();
                    break;
                case EXTTextureCompressionDXT1.name:
                    glExtension = EXTTextureCompressionDXT1.load();
                    break;
                case EXTTextureCompressionS3TC.name:
                    glExtension = EXTTextureCompressionS3TC.load();
                    break;
                case IMGTextureCompressionPVRTC.name:
                    glExtension = IMGTextureCompressionPVRTC.load();
                    break;
                case KHRTextureCompressionASTC.name:
                    glExtension = KHRTextureCompressionASTC.load();
                    break;
                case NVTextureCompressionLATC.name:
                    glExtension = NVTextureCompressionLATC.load();
                    break;
                case NVTextureCompressionS3TC.name:
                    glExtension = NVTextureCompressionS3TC.load();
                    break;
                case OESCompressedETC1RGB8.name:
                    glExtension = OESCompressedETC1RGB8.load();
                    break;
                case OESCompressedPalettedTexture.name:
                    glExtension = OESCompressedPalettedTexture.load();
                    break;
                case OESTextureCompressionASTC.name:
                    glExtension = OESTextureCompressionASTC.load();
                    break;
                // Texture utility extensions
                case EXTTextureFilterAnisotropic.name:
                    glExtension = EXTTextureFilterAnisotropic.load();
                    break;
                case NVTextureCompressionS3TCUpdate.name:
                    glExtension = NVTextureCompressionS3TCUpdate.load();
                    break;
                case OESTexture3D.name:
                    glExtension = OESTexture3D.load();
                    break;
                // Debug extensions
                case EXTDebugMarker.name:
                    glExtension = EXTDebugMarker.load();
                    break;
                // Other extensions
                case OESElementIndexUINT.name:
                    glExtension = OESElementIndexUINT.load();
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Rajawali does not know about extension: " + extension + ". Have you tried explicitly "
                            + "providing it via Capabilities#usingExtension(GLExtension)?");
            }
            loadedExtensions.put(extension, glExtension);
        }
        return loadedExtensions.get(extension);
    }

    /**
     * Indicates that an extension is being used by providing it explicitly to the engine. This is useful for cases
     * where the engine does not formally support an extension you are using. It is expected that the extension you
     * provide has already been configured by reading the relevant parameters from the device.
     *
     * @param extension The {@link GLExtension} implementation you are using.
     */
    public void usingExtension(@NonNull GLExtension extension) {
        loadedExtensions.put(extension.getName(), extension);
    }

    /**
     * A rough estimate of the largest texture that OpenGL can handle.
     *
     * @return
     */
    public int getMaxTextureSize() {
        return maxTextureSize;
    }

    /**
     * The maximum supported texture image units that can be used to access texture maps from the vertex shader
     * and the fragment processor combined. If both the vertex shader and the fragment processing stage access
     * the same texture image unit, then that counts as using two texture image units against this limit.
     *
     * @return
     */
    public int getMaxCombinedTextureUnits() {
        return maxCombinedTextureImageUnits;
    }

    /**
     * The value gives a rough estimate of the largest cube-map texture that the GL can handle.
     * The value must be at least 1024.
     *
     * @return
     */
    public int getMaxCubeMapTextureSize() {
        return maxCubeMapTextureSize;
    }

    /**
     * The maximum number of individual 4-vectors of floating-point, integer, or boolean values that can be held
     * in uniform variable storage for a fragment shader.
     *
     * @return
     */
    public int getMaxFragmentUniformVectors() {
        return maxFragmentUniformVectors;
    }

    /**
     * Indicates the maximum supported size for renderbuffers.
     *
     * @return
     */
    public int getMaxRenderbufferSize() {
        return maxRenderbufferSize;
    }

    /**
     * The maximum supported texture image units that can be used to access texture maps from the fragment shader.
     *
     * @return
     */
    public int getMaxTextureImageUnits() {
        return maxTextureImageUnits;
    }

    /**
     * The maximum number of 4-vectors for varying variables.
     *
     * @return
     */
    public int getMaxVaryingVectors() {
        return maxVaryingVectors;
    }

    /**
     * The maximum number of 4-component generic vertex attributes accessible to a vertex shader.
     *
     * @return
     */
    public int getMaxVertexAttribs() {
        return maxVertexAttribs;
    }

    /**
     * The maximum supported texture image units that can be used to access texture maps from the vertex shader.
     *
     * @return
     */
    public int getMaxVertexTextureImageUnits() {
        return maxVertexTextureImageUnits;
    }

    /**
     * The maximum number of 4-vectors that may be held in uniform variable storage for the vertex shader.
     *
     * @return
     */
    public int getMaxVertexUniformVectors() {
        return maxVertexUniformVectors;
    }

    /**
     * The maximum supported viewport width
     *
     * @return
     */
    public int getMaxViewportWidth() {
        return maxViewportWidth;
    }

    /**
     * The maximum supported viewport height
     *
     * @return
     */
    public int getMaxViewportHeight() {
        return maxViewportHeight;
    }

    /**
     * Indicates the minimum width supported for aliased lines
     *
     * @return
     */
    public int getMinAliasedLineWidth() {
        return minAliasedLineWidth;
    }

    /**
     * Indicates the maximum width supported for aliased lines
     *
     * @return
     */
    public int getMaxAliasedLineWidth() {
        return maxAliasedLineWidth;
    }

    /**
     * Indicates the minimum size supported for aliased points
     *
     * @return
     */
    public int getMinAliasedPointSize() {
        return minAliasedPointSize;
    }

    /**
     * Indicates the maximum size supported for aliased points
     *
     * @return
     */
    public int getMaxAliasedPointSize() {
        return maxAliasedPointSize;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("-=-=-=- OpenGL ES Capabilities -=-=-=-\n");
        sb.append("Max Combined Texture Image Units   : ").append(maxCombinedTextureImageUnits).append("\n");
        sb.append("Max Cube Map Texture Size          : ").append(maxCubeMapTextureSize).append("\n");
        sb.append("Max FRAGMENT Uniform Vectors       : ").append(maxFragmentUniformVectors).append("\n");
        sb.append("Max Renderbuffer Size              : ").append(maxRenderbufferSize).append("\n");
        sb.append("Max Texture Image Units            : ").append(maxTextureImageUnits).append("\n");
        sb.append("Max Texture Size                   : ").append(maxTextureSize).append("\n");
        sb.append("Max Varying Vectors                : ").append(maxVaryingVectors).append("\n");
        sb.append("Max VERTEX Attribs                 : ").append(maxVertexAttribs).append("\n");
        sb.append("Max VERTEX Texture Image Units     : ").append(maxVertexTextureImageUnits).append("\n");
        sb.append("Max VERTEX Uniform Vectors         : ").append(maxVertexUniformVectors).append("\n");
        sb.append("Max Viewport Width                 : ").append(maxViewportWidth).append("\n");
        sb.append("Max Viewport Height                : ").append(maxViewportHeight).append("\n");
        sb.append("Min Aliased Line Width             : ").append(minAliasedLineWidth).append("\n");
        sb.append("Max Aliased Line Width             : ").append(maxAliasedLineWidth).append("\n");
        sb.append("Min Aliased Point Size             : ").append(minAliasedPointSize).append("\n");
        sb.append("Max Aliased Point Width            : ").append(maxAliasedPointSize).append("\n");
        sb.append("-=-=-=- OpenGL ES Capabilities -=-=-=-\n");
        return sb.toString();
    }

    public static class UnsupportedCapabilityException extends Exception {

        /**
         * Constructs a new {@code UnsupportedCapabilityException} that includes the current stack trace.
         */
        public UnsupportedCapabilityException() {
        }

        /**
         * Constructs a new {@code UnsupportedCapabilityException} with the current stack trace and the
         * specified detail message.
         *
         * @param detailMessage The detail message for this exception.
         */
        public UnsupportedCapabilityException(String detailMessage) {
            super(detailMessage);
        }

        /**
         * Constructs a new {@code UnsupportedCapabilityException} with the current stack trace, the
         * specified detail message and the specified cause.
         *
         * @param detailMessage The detail message for this exception.
         * @param throwable     Tthe cause of this exception.
         */
        public UnsupportedCapabilityException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        /**
         * Constructs a new {@code UnsupportedCapabilityException} with the current stack trace and the
         * specified cause.
         *
         * @param throwable The cause of this exception.
         */
        public UnsupportedCapabilityException(Throwable throwable) {
            super(throwable);
        }
    }
}
