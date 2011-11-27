package com.gg.wallpaper;

import java.util.LinkedList;
import java.util.List;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.RotationByModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.extension.ui.livewallpaper.BaseLiveWallpaperService;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;

public class WallpaperBase extends BaseLiveWallpaperService {

	private static String TAG = WallpaperBase.class.getName();

	protected static final int CAMERA_WIDTH = 480;
	protected static final int CAMERA_HEIGHT = 800;
	protected static final float MAX_FRAME_TIME = 0.7f;

	private static final float SMOOTH_RATE = 20;
	private static final float STANDARD_RATE = 10;

	private static float FRAME_RATE = 10;
	private static float FRAME_TIME = 1000f / FRAME_RATE;

	private static final float SMOOTH_TIME = 1000f / FRAME_RATE;

	private static final int SCROLL_SMOOTH = 2;

	private ScreenOrientation mScreenOrientation;

	protected static boolean reload = false;
	protected static boolean restart = false;
	private static boolean render = true;

	@Override
	public org.anddev.andengine.engine.Engine onLoadEngine() {
		return new org.anddev.andengine.engine.Engine(new EngineOptions(true,
				this.mScreenOrientation, new FillResolutionPolicy(),
				new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT)));

	}

	@Override
	public void onLoadResources() {

		PreferenceManager.setDefaultValues(this, R.xml.wallpaper_settings,
				false);

		loadScene();
	}

	protected void loadScene() {
		scene = new Scene();
	}

	public static boolean isReload() {
		return reload;
	}

	public static void setReload(boolean reload) {
		WallpaperBase.reload = reload;
	}

	@Override
	public void onUnloadResources() {

	}

	@Override
	public Scene onLoadScene() {

		return scene;

	}

	@Override
	protected void onResume() {
		super.onResume();
		restart = true;
		Log.d(TAG, "onResume reload[" + reload + "]");

		if (reload) {
			reload = false;
		}

	}

	@Override
	public Engine onCreateEngine() {
		WallpaperEngine wallpaperEngine = new WallpaperEngine();
		wallpaperEngine.setTouchEventsEnabled(true);
		return wallpaperEngine;
	}

	protected float deltaX = -CAMERA_WIDTH / 2;

	protected Scene scene;

	float lastX;
	boolean start = true;
	boolean cangedDirection = false;

	protected void onTouchEvent(MotionEvent event) {

		int currentDirection;

		if (start) {

			lastX = event.getX();
			start = false;

		} else {

			if (lastX > event.getX()) {
				currentDirection = 0;
			} else {
				currentDirection = 1;
			}

			if (currentDirection != direction) {
				cangedDirection = true;
			}

			direction = currentDirection;
		}

	}

	// float currentStart = -1;
	float lastOffset = 0;
	// float currentOffset = 0;

	List<Float> offsetValues = new LinkedList<Float>();

	boolean changedDir = false;
	int direction = 0;

	protected void onOffsetsChanged(float xOffset, float yOffset,
			float xOffsetStep, float yOffsetStep, int xPixelOffset,
			int yPixelOffset) {

		// Log.d(this.getClass().getName(), "xOffset = " + xOffset);
		// Log.d(this.getClass().getName(), "xOffsetStep = " + xOffsetStep);
		//
		// Log.d(this.getClass().getName(), "xPixelOffset = " + xPixelOffset);
		// Log.d(this.getClass().getName(), "lastOffset = " + lastOffset);
		//
		// Log.d(this.getClass().getName(), "lastOffset < xPixelOffset ["
		// + (lastOffset < xPixelOffset) + "]");

		if (xOffsetStep == -1 || xOffsetStep == 0) {
			deltaX = -CAMERA_WIDTH / 2;
		} else {

			if (lastOffset != xPixelOffset) {

				setFRAME_RATE(SMOOTH_RATE);

				// int currentDirection = 0;
				// if (lastOffset < xPixelOffset) {
				// currentDirection = 1;
				// } else {
				// direction = 0;
				// }

				float start = lastOffset;
				float stop = xPixelOffset;
				synchronized (offsetValues) {

					if (cangedDirection && !offsetValues.isEmpty()) {

						Log.d(TAG, "onOffsetsChanged [changed DIRECTION]");

						start = deltaX;
						offsetValues.clear();
						cangedDirection = false;

					}

					// offsetValues.add((float) xPixelOffset);

					for (int i = 0; i < SCROLL_SMOOTH; i++) {

						float smootedValue = ((stop - start) / SCROLL_SMOOTH)
								* i;

						offsetValues.add(start + smootedValue);
						// Log.d(TAG, "onOffsetsChanged smooted value["
						// + (start + smootedValue) + "]");
					}

				}

				lastOffset = xPixelOffset;

			}

		}

	}

	/**
	 * this method is intendeg for scene update indipendet from and engine
	 */
	protected void triggerFrame() {

		if (!offsetValues.isEmpty()) {
			synchronized (offsetValues) {
				deltaX = offsetValues.get(0);
				offsetValues.remove(0);

				// for (int i = 0; i < remove; i++) {
				// if (!offsetValues.isEmpty())
				// offsetValues.remove(0);
				// }

			}
		} else {
			setFRAME_RATE(STANDARD_RATE);
		}
	}

	public static void setFRAME_RATE(float fRAME_RATE) {
		FRAME_RATE = fRAME_RATE;
		FRAME_TIME = 1000 / FRAME_RATE;
	}

	@Override
	public void onLoadComplete() {

	}

	@Override
	public void onPauseGame() {

	}

	@Override
	public void onResumeGame() {

	}

	protected class SlideAnimator implements IUpdateHandler {

		private float xIncrement = 0;
		Sprite sprite;
		float startX;
		float startY;

		float frameRate;
		float frameTime;

		public SlideAnimator(Sprite sprite, float speed) {
			this.sprite = sprite;
			this.startX = sprite.getX();
			this.startY = sprite.getY();
			setSpeed(speed);
		}

		@Override
		public void reset() {

		}

		@Override
		public void onUpdate(float pSecondsElapsed) {

			if (restart || pSecondsElapsed > MAX_FRAME_TIME) {
				restart = false;
				Log.d(TAG, "onUpdate pSecondsElapsed[" + pSecondsElapsed + "]");
				return;
			}

			float x = startX;// - deltaX;

			// if (!goBack) {
			xIncrement += (pSecondsElapsed) / frameTime;
			// } else {
			// xIncrement -= (pSecondsElapsed) / frameTime;
			// }

			float finalX = x + xIncrement;

			// fine del giro della luna, la rimetto all'inizio
			// if (!goBack && xIncrement > 0) {
			// goBack = true;
			// }
			// if (goBack && xIncrement <= -(sprite.getWidth() - CAMERA_WIDTH))
			// {
			// goBack = false;
			// }

			if (xIncrement >= (sprite.getWidth())) {
				xIncrement = 0;
			}

			// Log.d(TAG, "onUpdate xIncrement[" + xIncrement + "]");

			float finalY = startY;

			// Log.d(TAG, "onUpdate final x[" + finalX + "]");
			// Log.d(TAG, "onUpdate final y[" + finalY + "]");

			sprite.setPosition(finalX + (deltaX / 2), finalY);
			// }
		}

		public void setSpeed(float frameRate) {
			this.frameRate = frameRate;
			frameTime = 1 / this.frameRate;
		}

		public Sprite getSprite() {
			return sprite;
		}

		public void setSprite(Sprite sprite) {
			this.sprite = sprite;
		}

		public float getStartX() {
			return startX;
		}

		public void setStartX(float startX) {
			this.startX = startX;
		}

		public float getStartY() {
			return startY;
		}

		public void setStartY(float startY) {
			this.startY = startY;
		}

		public float getFrameTime() {
			return frameTime;
		}

	}

	protected class MyRotationByModifier extends RotationByModifier {

		public MyRotationByModifier(float pDuration, float pRotation) {
			super(pDuration, pRotation);
		}

		@Override
		protected void onChangeValue(float pSecondsElapsed, IEntity pEntity,
				float pRotation) {
			if (restart || pSecondsElapsed > MAX_FRAME_TIME) {
				restart = false;
				Log.d(TAG, "onUpdate pSecondsElapsed[" + pSecondsElapsed + "]");
				return;
			}
			super.onChangeValue(pSecondsElapsed, pEntity, pRotation);
		}

	}

	protected class WallpaperEngine extends BaseWallpaperGLEngine {

		private final String TAG = WallpaperBase.WallpaperEngine.class
				.getName();

		public WallpaperEngine() {
			super();
			this.setRenderMode(RENDERMODE_WHEN_DIRTY);
		}

		private void startRenderThread() {
			render = true;
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					while (render) {
						requestRender();
						WallpaperBase.this.triggerFrame();
						try {

							Thread.sleep((long) FRAME_TIME);

						} catch (InterruptedException e) {

						}
					}
				}
			};
			new Thread(runnable).start();
		}

		private void startSmoothThread() {
			render = true;
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					while (render) {

						WallpaperBase.this.triggerFrame();
						try {
							Thread.sleep((long) SMOOTH_TIME);
						} catch (InterruptedException e) {

						}
					}
				}
			};
			new Thread(runnable).start();
		}

		@Override
		public void onPause() {
			super.onPause();
			render = false;
			Log.d(TAG, "onPause [stopped thread]");
		}

		@Override
		public void onResume() {
			super.onResume();
			// startSmoothThread();
			startRenderThread();
			Log.d(TAG, "onResume [started thread]");
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			WallpaperBase.this.onTouchEvent(event);

			Log.d(TAG, "onTouchEvent event get X[" + event.getX() + "]");
			super.onTouchEvent(event);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {

			WallpaperBase.this.onOffsetsChanged(xOffset, yOffset, xOffsetStep,
					yOffsetStep, xPixelOffset, yPixelOffset);

			super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep,
					xPixelOffset, yPixelOffset);
		}
	}

}