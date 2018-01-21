package io.github.carlorodriguez.alarmon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import static io.github.carlorodriguez.alarmon.ActivityAlarmNotification.foreheadLayout;
import static io.github.carlorodriguez.alarmon.ActivityAlarmNotification.lipButton;
import static io.github.carlorodriguez.alarmon.ActivityAlarmNotification.lipLayout;

/**
 * View which displays a bitmap containing a face along with overlay graphics that identify the
 * locations of detected facial landmarks.
 */
public class FaceView extends View {
    private static String TAG = FaceView.class.getSimpleName();

    private Bitmap mBitmap;
    private SparseArray<Face> mFaces;

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public static Path triangle;

    int bottomCx;
    int bottomCy;
    int liftCx;
    int liftCy;
    int rightCx;
    int rightCy;
    public static Rect rect;
    /**
     * Sets the bitmap background and the associated face detections.
     */

    void setContent(Bitmap bitmap, SparseArray<Face> faces) {
        mBitmap = bitmap;
        mFaces = faces;
        invalidate();
    }

    /**
     * Draws the bitmap background and the associated face landmarks.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if ((mBitmap != null) && (mFaces != null)) {
            Log.d(TAG, "mBitmap height= " +mBitmap.getHeight());
            double scale = drawBitmap(canvas);
            drawFaceAnnotations(canvas, scale);
            drawFaceLips(canvas, scale);
            drawFaceForehead(canvas, scale);
        }
    }

    /**
     * Draws the bitmap background, scaled to the device size.  Returns the scale for future use in
     * positioning the facial landmark graphics.
     */
    private double drawBitmap(Canvas canvas) {
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();

        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
        Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale), (int)(imageHeight * scale));

        Paint paint = new Paint();
        paint.setAlpha(40); //you can set your transparent value here

        canvas.drawBitmap(mBitmap, null, destBounds, paint);
        return scale;
    }

    /**
     * Draws a small circle for each detected landmark, centered at the detected landmark position.
     * <p>
     *
     * Note that eye landmarks are defined to be the midpoint between the detected eye corner
     * positions, which tends to place the eye landmarks at the lower eyelid rather than at the
     * pupil position.
     */
    private void drawFaceAnnotations(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);
            for (Landmark landmark : face.getLandmarks()) {
                Log.d(TAG, "landmark getType= " +landmark.getType());
                int type = landmark.getType();

                if (type == Landmark.LEFT_MOUTH){
                    int cx = (int) (landmark.getPosition().x * scale);
                    int cy = (int) (landmark.getPosition().y * scale);
                    canvas.drawCircle(cx, cy, 10, paint);
                }

            }
        }
    }

    private void drawFaceForehead(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);
            for (Landmark landmark : face.getLandmarks()) {
                //Log.d(TAG, "landmark getType= " +landmark.getType());
                int cx = (int) (landmark.getPosition().x * scale);
                int cy = (int) (landmark.getPosition().y * scale);
                canvas.drawCircle(cx, cy, 10, paint);
                int type = landmark.getType();
                switch (type) { // get x and y for every dot
                    case Landmark.NOSE_BASE:
                        bottomCx = (int) (landmark.getPosition().x * scale);
                        bottomCy = (int) (landmark.getPosition().y * scale);
                        break;
                    case Landmark.LEFT_EYE:
                        liftCx = (int) (landmark.getPosition().x * scale);
                        liftCy = (int) (landmark.getPosition().y * scale);
                        break;
                    case Landmark.RIGHT_EYE:
                        rightCx = (int) (landmark.getPosition().x * scale);
                        rightCy = (int) (landmark.getPosition().y * scale);
                        break;
                }

            }
        }

        int newRightCy = rightCy - ((bottomCy - rightCy)*2); //+ (bottomCy - rightCy)/2) );// new Right dot y to make it above lips
        int newRightCx = rightCx - (bottomCx - rightCx);
        foreheadLayout.setX(newRightCx);
        foreheadLayout.setY(newRightCy); // set button position
        ViewGroup.LayoutParams params= foreheadLayout.getLayoutParams(); // get lipLayout params to change it
        params.width= liftCx - rightCx + ((bottomCx - rightCx )*2);
        params.height= (bottomCy - rightCy) + ((bottomCy - rightCy)/2);
        foreheadLayout.setLayoutParams(params); // set new params

    }

    private void drawFaceLips(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);
            for (Landmark landmark : face.getLandmarks()) {
                //Log.d(TAG, "landmark getType= " +landmark.getType());
                int type = landmark.getType();
                switch (type) { // get x and y for every dot
                    case Landmark.BOTTOM_MOUTH:
                         bottomCx = (int) (landmark.getPosition().x * scale);
                         bottomCy = (int) (landmark.getPosition().y * scale);
                        break;
                    case Landmark.LEFT_MOUTH:
                         liftCx = (int) (landmark.getPosition().x * scale);
                         liftCy = (int) (landmark.getPosition().y * scale);
                        break;
                    case Landmark.RIGHT_MOUTH:
                         rightCx = (int) (landmark.getPosition().x * scale);
                         rightCy = (int) (landmark.getPosition().y * scale);
                        break;
                }

            }
        }

        lipButton.setBackgroundColor(Color.BLUE);
        int newRightCy = rightCy - (bottomCy - rightCy);// new Right dot y to make it above lips
        lipLayout.setX(rightCx);
        lipLayout.setY(newRightCy); // set button position
        ViewGroup.LayoutParams params= lipLayout.getLayoutParams(); // get lipLayout params to change it
        params.width= liftCx - rightCx;
        params.height= bottomCy - newRightCy;
        lipLayout.setLayoutParams(params); // set new params

    }
}

