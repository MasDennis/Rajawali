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
package org.rajawali3d.view;

import android.annotation.TargetApi;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;

import org.rajawali3d.view.Surface.ANTI_ALIASING_CONFIG;
import org.rajawali3d.renderer.ISurfaceRenderer;

import c.org.rajawali3d.gl.Capabilities;
import c.org.rajawali3d.surface.gles.GLESSurfaceView;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public abstract class Daydream extends DreamService implements Display {

	protected GLESSurfaceView mGLESSurfaceView;
	protected FrameLayout mLayout;

	private ISurfaceRenderer mRajawaliRenderer;

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();

		mGLESSurfaceView = new GLESSurfaceView(this);
		mGLESSurfaceView.setEGLContextClientVersion(Capabilities.getGLESMajorVersion());

		setInteractive(false);
		setFullscreen(true);

		mLayout = new FrameLayout(this);
		mLayout.addView(mGLESSurfaceView);

		setContentView(mLayout);

		setRenderer(createRenderer());
	}

	@Override
	public void onDreamingStarted() {
		super.onDreamingStarted();
		mGLESSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		mGLESSurfaceView.onResume();
	}

	@Override
	public void onDreamingStopped() {
		super.onDreamingStopped();
		mGLESSurfaceView.onPause();
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mRajawaliRenderer.onRenderSurfaceDestroyed(null);
		unbindDrawables(mLayout);
		System.gc();
	}

    protected void setAntiAliasingConfig(ANTI_ALIASING_CONFIG config) {
		//mGLESSurfaceView.setSurfaceAntiAliasing(config);
	}

	protected void setRenderer(ISurfaceRenderer renderer) {
		mRajawaliRenderer = renderer;
        //mGLESSurfaceView.setSurfaceRenderer(mRajawaliRenderer);
	}

	private void unbindDrawables(View view) {
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			((ViewGroup) view).removeAllViews();
		}
	}
}
