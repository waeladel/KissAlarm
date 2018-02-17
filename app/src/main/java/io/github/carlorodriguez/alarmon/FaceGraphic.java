package io.github.carlorodriguez.alarmon;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mForeheadPaint;
    private Paint mLipsPaint;

    private volatile Face mFace;
    private int mFaceId;
    private float mFaceHappiness;
    private float x ;
    private float y ;

    private float xOffset ;
    private float yOffset;
    private float left ;
    private float top ;
    private float right ;
    private float bottom ;

    public static RectF lipRect;
    public static RectF foreheadRect;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        //mFacePositionPaint.setColor(selectedColor);
        mFacePositionPaint.setColor(Color.CYAN);
        if(ActivityAlarmNotification.isShown){
            mFacePositionPaint.setAlpha(ActivityAlarmNotification.visibility); //set transparent value: 0 fully transparent
        }else{
            mFacePositionPaint.setAlpha(0); //set transparent value: 0 fully transparent
        }

        mForeheadPaint = new Paint();
        //mIdPaint.setColor(selectedColor);
        mForeheadPaint.setStyle(Paint.Style.STROKE);
        mForeheadPaint.setColor(Color.CYAN);
        mForeheadPaint.setStrokeWidth(BOX_STROKE_WIDTH);
        if(ActivityAlarmNotification.isShown){
            mForeheadPaint.setAlpha(ActivityAlarmNotification.visibility); //set transparent value: 0 fully transparent
        }else{
            mForeheadPaint.setAlpha(0); //set transparent value: 0 fully transparent
        }


        mLipsPaint = new Paint();
        //mBoxPaint.setColor(selectedColor);
        mLipsPaint.setColor(Color.MAGENTA);
        mLipsPaint.setStyle(Paint.Style.STROKE);
        mLipsPaint.setStrokeWidth(BOX_STROKE_WIDTH);
        if(ActivityAlarmNotification.isShown){
            mLipsPaint.setAlpha(ActivityAlarmNotification.visibility); //set transparent value: 0 fully transparent
        }else{
            mLipsPaint.setAlpha(0); //set transparent value: 0 fully transparent
        }
    }

    void setId(int id) {
        mFaceId = id;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        //canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBoxPaint);
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        x = translateX(face.getPosition().x + face.getWidth() / 2);
        y = translateY(face.getPosition().y + face.getHeight() / 2);
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
        //canvas.drawText("id: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mFacePositionPaint);
        /*canvas.drawText("happiness: " + String.format("%.2f", face.getIsSmilingProbability()), x - ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint);
        canvas.drawText("right eye: " + String.format("%.2f", face.getIsRightEyeOpenProbability()), x + ID_X_OFFSET * 2, y + ID_Y_OFFSET * 2, mIdPaint);
        canvas.drawText("left eye: " + String.format("%.2f", face.getIsLeftEyeOpenProbability()), x - ID_X_OFFSET*2, y - ID_Y_OFFSET*2, mIdPaint);
        */
        // Draws a bounding box around the face.
        xOffset = scaleX(face.getWidth() / 2.0f);
        yOffset = scaleY(face.getHeight() / 2.0f);
        left = x - xOffset;
        top = y - yOffset;
        right = x + xOffset;
        bottom = y + yOffset;
        //canvas.drawRect(left, top, right, bottom, mBoxPaint);
        //drawFaceAnnotations(canvas);
        drawFaceLips(canvas);// draw lip's button
        drawFaceForehead(canvas); // draw forehead's button
    }


    private void drawFaceForehead(Canvas canvas) {

        //canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBoxPaint);
        if (mFace == null) {
            return;
        }
        // Draws a bounding box around the forehead.
        xOffset = scaleX(mFace.getWidth() / 2.0f);
        yOffset = scaleY(mFace.getHeight() / 2.0f);
        left = x - xOffset;
        top = y - yOffset;
        right = x + xOffset;
        bottom = y ;
        foreheadRect = new RectF(left, top, right, bottom);
        canvas.drawRect(foreheadRect, mForeheadPaint);
    }

    private void drawFaceLips(Canvas canvas) {
        //canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBoxPaint);
        if (mFace == null) {
            return;
        }
        // Draws a bounding box around the lips.
        xOffset = scaleX(mFace.getWidth() / 2.0f);
        yOffset = scaleY(mFace.getHeight() / 2.0f);
        left = x - xOffset;
        top = y + (yOffset/3);
        right = x + xOffset;
        bottom = y + yOffset+ (yOffset/3);
        lipRect = new RectF(left, top, right, bottom);
        canvas.drawRect(lipRect, mLipsPaint);

    }

    /**
     * Draws a small circle for each detected landmark, centered at the detected landmark position.
     * <p>
     *
     * Note that eye landmarks are defined to be the midpoint between the detected eye corner
     * positions, which tends to place the eye landmarks at the lower eyelid rather than at the
     * pupil position.
     */
    private void drawFaceAnnotations(Canvas canvas) {

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(Color.GREEN);
        mFacePositionPaint.setStyle(Paint.Style.STROKE);
        mFacePositionPaint.setStrokeWidth(5);

            for (Landmark landmark : mFace.getLandmarks()) {
                    int cx = (int) (landmark.getPosition().x );
                    int cy = (int) (landmark.getPosition().y );
                    canvas.drawCircle(cx, cy, 3, mFacePositionPaint);
            }
    }


}
