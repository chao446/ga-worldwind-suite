/*******************************************************************************
 * Copyright 2012 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.worldwind.animator.application.render;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.view.orbit.OrbitView;

import java.io.File;

import au.gov.ga.worldwind.animator.animation.Animation;
import au.gov.ga.worldwind.animator.animation.RenderParameters;
import au.gov.ga.worldwind.animator.application.Animator;
import au.gov.ga.worldwind.animator.application.AnimatorSceneController;
import au.gov.ga.worldwind.animator.application.ScreenshotPaintTask;
import au.gov.ga.worldwind.animator.layers.immediate.ImmediateMode;
import au.gov.ga.worldwind.common.util.Validate;

/**
 * An {@link AnimationRenderer} that works by applying the animation state, then
 * taking a screenshot of the current viewport window.
 * <p>
 * Has the advantage that animation state is updated as each frame is rendered,
 * but suffers from the disadvantage that the viewport window *must* be kept as
 * the foremost window in the user's desktop.
 * </p>
 * <p>
 * This class is no longer used by the animator, as it has been replaced by the
 * {@link OffscreenRenderer} which uses an fbo for rendering.
 * </p>
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class ViewportScreenshotRenderer extends AnimationRendererBase
{
	private WorldWindow worldWindow;
	private Animator targetApplication;
	private AnimatorSceneController animatorSceneController;

	private boolean detectCollisions;
	private double detailHintBackup;
	private boolean wasImmediate;

	public ViewportScreenshotRenderer(WorldWindow wwd, Animator targetApplication)
	{
		Validate.notNull(wwd, "A world window is required");
		Validate.notNull(targetApplication, "An Animator application is required");

		this.worldWindow = wwd;
		this.targetApplication = targetApplication;
		this.animatorSceneController = (AnimatorSceneController) wwd.getSceneController();
	}

	@Override
	protected void doPreRender(Animation animation, RenderParameters renderParams)
	{
		wasImmediate = ImmediateMode.isImmediate();
		ImmediateMode.setImmediate(true);

		targetApplication.resizeWindowToRenderDimensions();
		targetApplication.disableUtilityLayers();

		detailHintBackup = targetApplication.getDetailedElevationModel().getDetailHint();
		targetApplication.getDetailedElevationModel().setDetailHint(renderParams.getDetailLevel());

		OrbitView orbitView = (OrbitView) worldWindow.getView();
		detectCollisions = orbitView.isDetectCollisions();
		orbitView.setDetectCollisions(false);

		targetApplication.getFrame().setAlwaysOnTop(true);
	}

	@Override
	protected void doRender(int frame, File targetFile, Animation animation, RenderParameters renderParams)
	{
		targetApplication.setSlider(frame);
		animation.applyFrame(frame);

		ScreenshotPaintTask screenshotTask = new ScreenshotPaintTask(targetFile, renderParams.isRenderAlpha());
		animatorSceneController.addPostPaintTask(screenshotTask);
		worldWindow.redraw();
		screenshotTask.waitForScreenshot();
	}

	@Override
	protected void doPostRender(Animation animation, RenderParameters renderParams)
	{
		targetApplication.reenableUtilityLayers();

		targetApplication.getDetailedElevationModel().setDetailHint(detailHintBackup);
		((OrbitView) worldWindow.getView()).setDetectCollisions(detectCollisions);
		ImmediateMode.setImmediate(wasImmediate);

		targetApplication.getFrame().setAlwaysOnTop(false);
	}

}
