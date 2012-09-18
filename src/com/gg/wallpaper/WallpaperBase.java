package com.gg.wallpaper;

import java.util.LinkedList;
import java.util.List;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.RotationByModifier;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.extension.ui.livewallpaper.BaseLiveWallpaperService;
import org.andengine.opengl.view.IRendererListener;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;

public abstract class WallpaperBase extends BaseLiveWallpaperService {

    private static String TAG = WallpaperBase.class.getName();

    protected static final int CAMERA_WIDTH = 480;
    protected static final int CAMERA_HEIGHT = 800;
    protected static final float MAX_FRAME_TIME = 0.7f;

    private static final float SMOOTH_RATE = 20;
    private static final float STANDARD_RATE = 15;

    private static float FRAME_RATE = STANDARD_RATE;
    private static float FRAME_TIME = 1000f / FRAME_RATE;

    private static final float SMOOTH_TIME = 1000f / FRAME_RATE;

    private static final int SCROLL_SMOOTH = 2;

    protected static boolean reload = false;
    protected static boolean restart = false;
    private static boolean render = true;

    /*
     * This is the most important value , contains the translation of the screen
     * , use that value to set x position for the objects on the screen
     */
    protected float deltaX = -CAMERA_WIDTH / 2;

    @Override
    public void onCreate() {

        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.wallpaper_settings,
                false);

    }

    @Override
    public Engine onCreateEngine() {
        reload = true;
        return new WallpaperEngine(this);
    }

    @Override
    public EngineOptions onCreateEngineOptions() {
        final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

        return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED,
                new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
    }

    @Override
    public void onCreateScene(OnCreateSceneCallback pOnCreateSceneCallback)
            throws Exception {
        this.scene = new Scene();
        this.loadScene();
        pOnCreateSceneCallback.onCreateSceneFinished(scene);
    }

    @Override
    public void onPopulateScene(Scene pScene,
            OnPopulateSceneCallback pOnPopulateSceneCallback) throws Exception {

        pOnPopulateSceneCallback.onPopulateSceneFinished();

    }

    /**
     * Implement this function to add to scene all wallpaper elements
     */
    public abstract void loadScene();

    public static boolean isReload() {
        return reload;
    }

    public static void setReload(boolean reload) {
        WallpaperBase.reload = reload;
    }

    @Override
    protected void onResume() {
        super.onResume();
        restart = true;
        Log.d(TAG, "onResume reload[" + reload + "]");

        if (reload) {
            scene.detachChildren();
            reload = false;
        }

    }

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

    float lastOffset = 0;

    List<Float> offsetValues = new LinkedList<Float>();

    boolean changedDir = false;
    int direction = 0;

    /**
     * Cotrol offset change, that is when user swipe on screen. This function
     * add a smooth and tail effect to moovement. The final value of smooting
     * effect is deltaX. Moreover , for make translation better, during
     * translation framrate is increased , and at the end resetted to default
     * value
     * 
     */
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

                float start = lastOffset;
                float stop = xPixelOffset;
                synchronized (offsetValues) {

                    if (cangedDirection && !offsetValues.isEmpty()) {

                        Log.d(TAG, "onOffsetsChanged [changed DIRECTION]");

                        start = deltaX;
                        offsetValues.clear();
                        cangedDirection = false;

                    }

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

            Log.d(TAG, "onOffsetsChanged deltaX[" + deltaX + "]");
            Log.d(TAG, "onOffsetsChanged lastOffset[" + lastOffset + "]");

        }

    }

    /**
     * this method set deltaX and reset framerate if necessary
     */
    protected void triggerFrame() {

        if (!offsetValues.isEmpty()) {
            synchronized (offsetValues) {
                deltaX = offsetValues.get(0);
                offsetValues.remove(0);
            }
        } else {
            setFRAME_RATE(STANDARD_RATE);
        }
    }

    public static void setFRAME_RATE(float fRAME_RATE) {
        FRAME_RATE = fRAME_RATE;
        FRAME_TIME = 1000 / FRAME_RATE;
    }

    /**
     * 
     * @author luigiantonini
     * 
     *         This is a simle lateral slider , the element slide from the le
     *         left of the screen to the right, and restart when have
     *         disappeared in the right side
     * 
     */
    protected class SlideAnimator implements IUpdateHandler {

        protected float slideIncrement = 0;
        protected Sprite sprite;
        protected float startX;
        protected float startY;

        protected float frameRate;
        protected float frameTime;

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

            slideIncrement += (pSecondsElapsed) / frameTime;
            animate(pSecondsElapsed);
        }

        public void animate(float pSecondsElapsed) {

            float x = startX;// - deltaX;

            float finalX = x + slideIncrement;

            if (slideIncrement >= (sprite.getWidth())) {
                slideIncrement = 0;
            }

            float finalY = startY;

            // Log.d(TAG, "onUpdate final x[" + finalX + "]");
            // Log.d(TAG, "onUpdate final y[" + finalY + "]");

            sprite.setPosition(finalX + (deltaX / 2), finalY);

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

    /**
     * 
     * @author luigiantonini
     * 
     *         This is a standard rotator, but avoid to render a frame if
     *         pSecondsElapsed is too high, to avoid a flounce
     * 
     */
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

    /**
     * 
     * @author luigiantonini
     * 
     *         BaseWallpaperGLEngine reimplementation, rendering is not more
     *         continue but have a fixed framerate. Morover the texure reload
     *         after onChancheVisibility is avoided , and when Wallpaper is not
     *         visible only the rendering is stopped, but not the engine , so it
     *         not need to be reloaded.
     * 
     */
    protected class WallpaperEngine extends BaseWallpaperGLEngine {

        public WallpaperEngine(IRendererListener pRendererListener) {
            super(pRendererListener);
            this.setRenderMode(GLEngine.RENDERMODE_WHEN_DIRTY);
            startRenderThread();
        }

        private final String TAG = WallpaperBase.WallpaperEngine.class
                .getName();

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

        @Override
        public void onVisibilityChanged(boolean pVisibility) {

            Log.d(TAG, "onVisibilityChanged visibility[" + pVisibility + "]");
            Log.d(TAG, "onVisibilityChanged isPreview[" + isPreview() + "]");
            Log.d(TAG, "onVisibilityChanged reload[" + reload + "]");

            // stop the rendering if Wallpaper is not visible
            if (!pVisibility)
                render = false;
            else {

                // this verify if reload is needed
                if (reload && scene != null) {

                    // reload resources
                    WallpaperBase.this.getEngine().onReloadResources();

                    // remove elemets from scene
                    scene.detachChildren();

                    // reload elements on scene
                    loadScene();

                    Log.d(TAG, "onVisibilityChanged isPreview[" + isPreview()
                            + "]");

                    // if is in preview mode force to reload all next time
                    if (isPreview())
                        reload = true;
                    else
                        reload = false;
                }

                // start rendering
                startRenderThread();
            }

        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            WallpaperBase.this.onTouchEvent(event);
            Log.d(TAG, "onTouchEvent event get X[" + event.getX() + "]");
            super.onTouchEvent(event);
        }

    }

}