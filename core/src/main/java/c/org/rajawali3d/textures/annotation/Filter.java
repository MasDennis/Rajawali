package c.org.rajawali3d.textures.annotation;

import android.support.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Holder for texture filter integer definitions.
 *
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public interface Filter {

    int NEAREST  = 0;
    int BILINEAR = 1;
    int TRILINEAR = 2;

    /**
     * Texture2D filtering or texture smoothing is the method used to determine the texture color for a texture mapped
     * pixel, using the colors of nearby texels (pixels of the texture).
     *
     * @see <a href="https://www.opengl.org/registry/specs/EXT/texture_filter_anisotropic.txt">
     * GL_EXT_texture_filter_anisotropic</a>
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ NEAREST, BILINEAR, TRILINEAR })
    @interface FilterType {}
}
