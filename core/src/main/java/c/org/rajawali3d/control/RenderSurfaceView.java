package c.org.rajawali3d.control;

import c.org.rajawali3d.surface.SurfaceView;

/**
 * Internal extensions of {@link SurfaceView} required by a {@link RenderControl} implementation. The goal is to
 * define a minimal interface that can reasonably be expected to be supportable by current (GLES) and future
 * (Vulkan?) graphics systems.
 *
 * @author Randy Picolet
 */
public interface RenderSurfaceView extends SurfaceView {

    /**
     * Sets whether frames are rendered only on request (and when the surface is first created), or continuously
     *
     * @param onRequest {@code boolean} true to render on request, false to render continuously
     */
    void setRenderFramesOnRequest(boolean onRequest);

    /**
     * Request rendering of the next complete frame for the SurfaceView.
     */
    void requestFrameRender();

    /**
     *
     * @param runnable
     */
    void queueEvent(Runnable runnable);

    /**
     *
     * @param runnable
     */
    void post(Runnable runnable);
}
