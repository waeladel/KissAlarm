package io.github.carlorodriguez.alarmon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

/**
 * View which displays a bitmap containing a face along with overlay graphics that identify the
 * locations of detected facial landmarks.
 */
public class FaceView extends View {
    private static String TAG = FaceView.class.getSimpleName();

    private Bitmap mBitmap;
    private SparseArray<Face> mFaces;
    public int bottomCx;
    public int bottomCy;
    public int liftCx;
    public int liftCy;
    public int rightCx;
    public int rightCy;
    public int noseCy;
    public int noseCx;
    float faceRotation;
    public static Rect rect;
    private Rect mLipBounds;
    private Rect mForeheadBounds;
    private Paint mPaint;
    private Paint mForeheadPaint;
    private Paint mLipsPaint;

    private Path mForHeadPath;
    private Path mLipPath;
    private Region mForHeadregion ;
    private Region mLipRegion;
    private float mLipsDistance;
    private double mAngle = 90;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private double mVisibility;


    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

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
            Log.d(TAG, "mFaces= " +mFaces +"mFaces size= "+ mFaces.size());
            double scale = drawBitmap(canvas);// draw original image
            //drawFaceAnnotations(canvas, scale); // draw face's landmarks
            drawFaceLips(canvas, scale);// draw lip's button
            drawFaceForehead(canvas, scale); // draw forehead's button
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

        Log.d(TAG, "mBitmap height= " +mBitmap.getHeight() + "mBitmap Width=" + mBitmap.getWidth());
        Log.d(TAG, "Canvas height= " +canvas.getHeight() + "Canvas Width=" + canvas.getWidth());

        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
        Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale), (int)(imageHeight * scale));

        mPaint = new Paint();
        //mPaint.setAlpha(40); //you can set your transparent value here

        canvas.drawBitmap(mBitmap, null, destBounds, mPaint);
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
    /*private void drawFaceAnnotations(Canvas canvas, double scale) {
        mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5);

        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);
            faceRotation = face.getEulerZ();
            for (Landmark landmark : face.getLandmarks()) {
                Log.d(TAG, "landmark getType= " +landmark.getType());
                int type = landmark.getType();
                int cx = (int) (landmark.getPosition().x * scale);
                int cy = (int) (landmark.getPosition().y * scale);
                *//*if(type== Landmark.NOSE_BASE || type== Landmark.LEFT_EYE || type== Landmark.RIGHT_EYE ){
                    canvas.drawCircle(cx, cy, 10, mPaint);
                }*//*
                canvas.drawCircle(cx, cy, 10, mPaint);

            }
        }
    }*/

    /**
     * Draws the forehead's button from associated face landmarks.
     */

    private void drawFaceForehead(Canvas canvas, double scale) {
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

        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);
            for (Landmark landmark : face.getLandmarks()) {
                //Log.d(TAG, "landmark getType= " +landmark.getType());
                int cx = (int) (landmark.getPosition().x * scale);
                int cy = (int) (landmark.getPosition().y * scale);
                //canvas.drawCircle(cx, cy, 10, mPaint);
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

        //coordinates of line (x1,y1) to (x2,y2)
        float x1,y1,x2,y2;
        x2= rightCx;
        y2= rightCy;
        x1= liftCx;
        y1= liftCy;

        //get the center of the line
        float centerX = Math.abs((x1+x2)/2);
        float centerY = Math.abs(y1+y2)/2;

        //put the lines in an array
        float[] BaselinePts = new float[] {x1, y1, x2, y2};
        float[] PolelinePts = new float[] {x1, y1, x2, y2};
        float[] RightlinePts = new float[] {x1, y1, x2, y2};
        float[] leftlinePts = new float[] {x1, y1, x2, y2};

        //create the matrix
        Matrix rotateMat = new Matrix();

        //rotate the matrix around the center
        rotateMat.setRotate((float) mAngle, centerX, centerY);
        rotateMat.mapPoints(PolelinePts);

        //Scale the BaselinePts line
        rotateMat.setScale(2,2,centerX, centerY);
        rotateMat.mapPoints(BaselinePts);

        /*rotateMat.setTranslate(PolelinePts [0]-centerX,PolelinePts [1]-centerY);
        rotateMat.mapPoints(BaselinePts);*/

        //rotate the RightlinePts 90 degree
        rotateMat.setRotate((float) mAngle, BaselinePts[2], BaselinePts[3]);
        rotateMat.mapPoints(RightlinePts);
        rotateMat.reset();

        //rotate the leftlinePts 270 degree
        rotateMat.setRotate((float) 270, BaselinePts[0], BaselinePts[1]);
        rotateMat.mapPoints(leftlinePts);
        rotateMat.reset();

        //draw the lines to see the results
        /*canvas.drawLine(BaselinePts [0], BaselinePts [1], BaselinePts [2], BaselinePts [3], mPaint);
        canvas.drawLine(RightlinePts [0], RightlinePts [1], RightlinePts [2], RightlinePts [3], mPaint);
        canvas.drawLine(leftlinePts [0], leftlinePts [1], leftlinePts [2], leftlinePts [3], mPaint);
        canvas.drawLine(PolelinePts [0], PolelinePts [1], PolelinePts [2], PolelinePts [3], mPaint);
        */
        Log.d(TAG, "linePts1= " +BaselinePts [0]+"linePts2= " +BaselinePts [1]+"linePts3= " +BaselinePts [2]+"linePts4= " +BaselinePts [3]);

        //draw the Circles on the rectangle's corners
        /*canvas.drawCircle(BaselinePts [2], BaselinePts [3], 50, mPaint);
        canvas.drawCircle(RightlinePts [0], RightlinePts [1], 20, mPaint);
        canvas.drawCircle(BaselinePts [0], BaselinePts [1], 50, mPaint);
        canvas.drawCircle(PolelinePts[0], PolelinePts[1], 30, mPaint);*/

        //draw the rectangle's Path
        mForHeadPath = new Path();
        mForHeadPath.moveTo(leftlinePts [2], leftlinePts [3]);
        mForHeadPath.lineTo(leftlinePts [0], leftlinePts [1]);
        mForHeadPath.lineTo(RightlinePts [2], RightlinePts [3]);
        mForHeadPath.lineTo(RightlinePts [0], RightlinePts [1]);
        mForHeadPath.close();
        canvas.drawPath(mForHeadPath,mForeheadPaint);

        //create a region to make the path clickable
        RectF rectF = new RectF();
        mForHeadPath.computeBounds(rectF, true);
        mForHeadregion = new Region();
        mForHeadregion.setPath(mForHeadPath, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
    }

    /**
     * Draws the lip's button from associated face landmarks.
     */

    private void drawFaceLips(Canvas canvas, double scale) {
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

        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);
            Log.d(TAG, "getEulerY= " +face.getEulerY()+ "getEulerZ="+ face.getEulerZ());
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
                    case Landmark.NOSE_BASE:
                        noseCx = (int) (landmark.getPosition().x * scale);
                        noseCy = (int) (landmark.getPosition().y * scale);
                        break;
                }

            }
        }

        //Get distance between nose base and bottom lips to use it as a radius
        /*mLipsDistance = (float) Math.sqrt(Math.pow(noseCx - bottomCx, 2) + Math.pow(noseCy - bottomCy, 2));
        Circle lipCircle = new Circle ();
        canvas.drawCircle(bottomCx, bottomCy, mLipsDistance, mPaint);*/

        //put the lines in an array
        float[] NoselinePts = new float[] {noseCx, noseCy, bottomCx, bottomCy};
        float[] BottomlinePts = new float[] {noseCx, noseCy, bottomCx, bottomCy};
        float[] ToplinePts = new float[] {noseCx, noseCy, bottomCx, bottomCy};

        //create the matrix
        Matrix rotateMat = new Matrix();

        //scale Nose line to reached beyond the lip bottom
        rotateMat.setScale(2,2,noseCx, noseCy);
        rotateMat.mapPoints(NoselinePts);

        //Rotate Nose line by 90 degree to become Bottom line
        rotateMat.setRotate((float) mAngle, NoselinePts [2], NoselinePts [3]);
        rotateMat.mapPoints(BottomlinePts);
        rotateMat.reset();

        //Scale Bottom line to become on the center of Nose line
        rotateMat.setScale(-2,-2,BottomlinePts[2], BottomlinePts[3]);
        rotateMat.mapPoints(BottomlinePts);

        //Rotate Nose line by 90 degree to become Top line line
        rotateMat.setRotate((float) mAngle, NoselinePts [0], NoselinePts [1]);
        rotateMat.mapPoints(ToplinePts);
        rotateMat.reset();

        //Scale Top line to become on the center of Nose line
        rotateMat.setScale(2,2,ToplinePts[2], ToplinePts[3]);
        rotateMat.mapPoints(ToplinePts);


        //draw the lines to see the results
        /*canvas.drawLine(NoselinePts [0], NoselinePts [1], NoselinePts [2], NoselinePts [3], mPaint);
        canvas.drawLine(BottomlinePts [0], BottomlinePts [1], BottomlinePts [2], BottomlinePts [3], mPaint);
        canvas.drawLine(ToplinePts [0], ToplinePts [1], ToplinePts [2], ToplinePts [3], mPaint);
        */

        Log.d(TAG, "linePts1= " +NoselinePts [0]+"linePts2= " +NoselinePts [1]+"linePts3= " +NoselinePts [2]+"linePts4= " +NoselinePts [3]);

        //draw the Circles on the rectangle's corners
        /*canvas.drawCircle(NoselinePts [2], NoselinePts [3], 50, mPaint);
        canvas.drawCircle(BottomlinePts [0], BottomlinePts [1], 20, mPaint);
        canvas.drawCircle(ToplinePts [2], ToplinePts [3], 10, mPaint);*/

        //draw the rectangle's Path
        mLipPath = new Path();
        mLipPath.moveTo(ToplinePts [2], ToplinePts [3]);
        mLipPath.lineTo(BottomlinePts [0], BottomlinePts [1]);
        mLipPath.lineTo(BottomlinePts [2], BottomlinePts [3]);
        mLipPath.lineTo(ToplinePts [0], ToplinePts [1]);
        mLipPath.close();
        canvas.drawPath(mLipPath,mLipsPaint);

        //create a region to make the path clickable
        RectF lipRectF = new RectF();
        mLipPath.computeBounds(lipRectF, true);
        mLipRegion = new Region();
        mLipRegion.setPath(mLipPath, new Region((int) lipRectF.left, (int) lipRectF.top, (int) lipRectF.right, (int) lipRectF.bottom));

    }

    /**
     * onTouchEvent for face lip and forehead.
     */

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch(event.getAction()){

            case MotionEvent.ACTION_DOWN:
                //Check if the x and y position of the touch is inside the bitmap
                if(mForHeadregion.contains(x, y)){
                    //BITMAP TOUCHED
                    if(!ActivityAlarmNotification.isToggled){
                        ActivityAlarmNotification.foreheadButton.callOnClick();
                        Log.d(TAG,"forehead TOUCHED so we will snooze");
                    }else {
                        ActivityAlarmNotification.lipButton.callOnClick();
                        Log.d(TAG, "forehead TOUCHED but we will dismiss");
                    }
                }else if(mLipRegion.contains(x, y)){
                    if(!ActivityAlarmNotification.isToggled){
                        ActivityAlarmNotification.lipButton.callOnClick();
                        Log.d(TAG,"lip TOUCHED so we will dismiss");
                    }else{
                        ActivityAlarmNotification.foreheadButton.callOnClick();
                        Log.d(TAG,"lip TOUCHED so but we will snooze");
                    }
                }
                return true;
        }
        return false;
    }
}
