package c.org.rajawali3d.transform;

import android.support.annotation.NonNull;
import c.org.rajawali3d.annotations.RequiresWriteLock;
import c.org.rajawali3d.scene.graph.SceneNode;
import net.jcip.annotations.ThreadSafe;

/**
 * Interface which an object must implement to be able to work with {@link Transformation} objects.
 *
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
@ThreadSafe
public interface Transformer {

    /**
     * Called by a {@link Transformable} object when the write lock has been acquired and this object may begin its
     * transformations.
     *
     * @param node {@link Transformation} for the {@link SceneNode}.
     */
    @RequiresWriteLock
    void transform(@NonNull Transformation node);
}
