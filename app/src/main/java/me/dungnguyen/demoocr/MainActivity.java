package me.dungnguyen.demoocr;

import android.content.Intent;
import android.content.res.AssetManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.dungnguyen.demoocr.databinding.ActivityMainBinding;

import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.Sobel;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";
    ActivityMainBinding binding;
    private int PICK_IMAGE = 1221;
    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/DemoOCR/";
    private String lang = "vie";
    private Bitmap yourSelectedImage;

    static {
        System.loadLibrary("jpgt");
        System.loadLibrary("pngt");
        System.loadLibrary("lept");
        System.loadLibrary("tess");
    }

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "open cv not loaded");
        } else {
            Log.e(TAG, "open cv loaded");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initValue();
        initEvent();
    }

    private void initValue() {
        String[] paths = new String[]{DATA_PATH, DATA_PATH + "tessdata/"};
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }
        }

        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }
    }

    private void initEvent() {
        binding.btnPickPhoto.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        });


        binding.btnScan.setOnClickListener(view -> {
            binding.tvResult.setText("");
            //SparseArray<TextBlock> sparseArray = textRecognizer.detect(frame);
            String myText = "";
            TessBaseAPI baseApi = new TessBaseAPI();
            baseApi.init(DATA_PATH, lang);
            ArrayList<Rect> rectArrayList = detectTextZone(yourSelectedImage);
            Bitmap workingBitmap = Bitmap.createBitmap(yourSelectedImage);
            Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(5.0f);
            for (int i = rectArrayList.size() - 1; i >= 0; i--) {
                Rect itemRect = rectArrayList.get(i);
                int x = Math.max(itemRect.x - 10, 0);
                int y = Math.max(itemRect.y - 10, 0);
                int width = Math.min(itemRect.width + 10, yourSelectedImage.getWidth());
                int height = Math.min(itemRect.height + 10, yourSelectedImage.getHeight());
                Bitmap bitmap = Bitmap.createBitmap(yourSelectedImage, x, y, width, height);
                baseApi.setImage(bitmap);
                myText = myText + " " + baseApi.getUTF8Text();
                Log.e(TAG, "my text : " + myText);
               /* if(i == 30){
                    Bitmap bitmap = Bitmap.createBitmap(yourSelectedImage, itemRect.x, itemRect.y, itemRect.width, itemRect.height);
                    binding.imageChanged.setImageBitmap(bitmap);
                }*/
                canvas.drawRect(Math.max(0, itemRect.x - 5), Math.max(0, itemRect.y - 1), itemRect.x + itemRect.width + 1, itemRect.y + itemRect.height + 1, paint);
            }
            binding.imvPhoto.setImageBitmap(mutableBitmap);

            baseApi.end();
           /* new AsyncTask() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    binding.progress.setVisibility(View.VISIBLE);
                }

                @Override
                protected Object doInBackground(Object[] objects) {
                    String myText = "";
                    TessBaseAPI baseApi = new TessBaseAPI();
                    baseApi.init(DATA_PATH, lang);
                    ArrayList<Rect> rectArrayList = detectTextZone(yourSelectedImage);
                    Canvas canvas = new Canvas(yourSelectedImage);
                    Paint paint = new Paint();
                    paint.setColor(Color.BLUE);
                    paint.setStrokeWidth(2.0f);
                    for (int i = 0; i < rectArrayList.size(); i++) {
                        Rect itemRect = rectArrayList.get(i);
                        canvas.drawRect(itemRect.x, itemRect.y, itemRect.width, itemRect.height, paint);
                        *//*Bitmap itemBitmap = Bitmap.createBitmap(yourSelectedImage, itemRect.x, itemRect.y, itemRect.width, itemRect.height);
                        baseApi.setImage(itemBitmap);

                        binding.imvPhoto.draw(new Canvas());
                        myText = myText + " " + baseApi.getUTF8Text();
                        Log.e(TAG, "text : " + myText);*//*
                    }
                    binding.imvPhoto.draw(canvas);
                    // DATA_PATH = Path to the storage
                    // lang = for which the language data exists, usually "eng"
                    // Eg. baseApi.init("/mnt/sdcard/tesseract/tessdata/eng.traineddata", "eng");

                    baseApi.end();

                    return myText;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    binding.progress.setVisibility(View.GONE);
                    binding.tvResult.setText(String.valueOf(o));

                }
            }.execute();*/
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE) {
            Uri selectedImage = data.getData();
            InputStream imageStream = null;
            try {
                imageStream = getContentResolver().openInputStream(selectedImage);
                yourSelectedImage = BitmapFactory.decodeStream(imageStream);
                binding.imvPhoto.setImageBitmap(yourSelectedImage);
            } catch (FileNotFoundException e) {
                Toast.makeText(MainActivity.this, "File not found", Toast.LENGTH_SHORT).show();
            }

        }
    }

    /* private ArrayList<Rect> detectTextZone(Bitmap bitmap) {
         ArrayList<Rect> rectArrayList = new ArrayList<>();
         Mat large = new Mat();
         Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
         Utils.bitmapToMat(bmp, large);
         Mat rgb = new Mat();
         Imgproc.pyrDown(large, rgb);
         Mat small = new Mat();
         Imgproc.cvtColor(large, small, Imgproc.COLOR_BGR2GRAY);
         Mat grad = new Mat();
         Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
         Imgproc.morphologyEx(small, grad, Imgproc.MORPH_GRADIENT, morphKernel);
         // binarize
         //Mat bw = new Mat();
         //Imgproc.threshold(grad, bw, 80, 255.0, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
         // connect horizontally oriented regions
         Mat connected = new Mat();
         morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 1));
         Imgproc.morphologyEx(grad, connected, Imgproc.MORPH_CLOSE, morphKernel);
         //
         Mat mask = Mat.zeros(grad.size(), CvType.CV_8UC1);
         ArrayList<MatOfPoint> contours = new ArrayList<>();
         Imgproc.findContours(connected, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
         for (int i = 0; i < contours.size(); i++) {
             Rect rect = Imgproc.boundingRect(contours.get(i));
             Mat maskROI = new Mat(mask, rect);
             maskROI.setTo(new Scalar(0, 0, 0));
             Imgproc.drawContours(mask, contours, i, new Scalar(255, 255, 255), Core.FILLED);
             double r = (double) Core.countNonZero(maskROI) / (rect.width * rect.height);
             rectArrayList.add(rect);
             if(rect.width > rect.height){
                rectArrayList.add(rect);
            }
            if (r > .45 // assume at least 45% of the area is filled if it contains text
                    &&
                    (rect.height > 8 && rect.width > 8 )  //constraints on region size
                // these two conditions alone are not very robust. better to use something
                // like the number of significant peaks in a horizontal projection as a third condition
                    ) {
                rectArrayList.add(rect);
            }
        }
        return rectArrayList;
    }*/
    private ArrayList<Rect> detectTextZone(Bitmap bitmap) {
        ArrayList<Rect> rectArrayList = new ArrayList<>();
        Mat img_gray = new Mat();
        Mat img_sobel = new Mat();
        Mat img_threshold = new Mat();
        Mat element = new Mat();
        Mat large = new Mat();
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp, large);
        Imgproc.cvtColor(large, img_gray, Imgproc.COLOR_BGR2GRAY);
        img_gray = processNoisy(img_gray);
        Sobel(img_gray, img_sobel, CvType.CV_8UC1, 1, 0, 3, 1, 0, Core.BORDER_DEFAULT);
        Imgproc.threshold(img_sobel, img_threshold, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);
        element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(85, 30));
        Imgproc.morphologyEx(img_threshold, img_threshold, Imgproc.MORPH_CLOSE, element);
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(img_threshold, contours, new Mat(), 0, 1);
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            List<Point> listPoint2f = new ArrayList<>();
            Converters.Mat_to_vector_Point2f(contours.get(i), listPoint2f);
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            Imgproc.approxPolyDP(contour2f, approxCurve, 3, true);
            MatOfPoint tempPoint = new MatOfPoint(approxCurve.toArray());
            Rect rect = Imgproc.boundingRect(tempPoint);
            if (rect.width > rect.height) {
                rectArrayList.add(rect);
            }
        }
        //
        return rectArrayList;
    }


    private Mat processNoisy(Mat grayMat) {
        Mat element1 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2), new Point(1, 1));
        Mat element2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2), new Point(1, 1));
        Imgproc.dilate(grayMat, grayMat, element1);
        Imgproc.erode(grayMat, grayMat, element2);

        GaussianBlur(grayMat, grayMat, new Size(3, 3), 0);
        // The thresold value will be used here
        Imgproc.threshold(grayMat, grayMat, 80, 255, Imgproc.THRESH_BINARY);

        return grayMat;
    }

  /* private ArrayList<Rect> detectTextZone(Bitmap bitmap) {
       ArrayList<Rect> rectArrayList = new ArrayList<>();
       Mat large = new Mat();
       Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
       Utils.bitmapToMat(bmp, large);
       Mat rgb = new Mat();
       Imgproc.pyrDown(large, rgb);
       Mat small = new Mat();
       Imgproc.cvtColor(rgb, small, Imgproc.COLOR_BGR2GRAY);
       Mat grad = new Mat();
       Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
       Imgproc.morphologyEx(small, grad, Imgproc.MORPH_GRADIENT, morphKernel);
       // binarize
       Mat bw = new Mat();
       Imgproc.threshold(grad, bw, 0.0, 255.0, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
       // connect horizontally oriented regions
       Mat connected = new Mat();
       morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 1));
       Imgproc.morphologyEx(bw, connected, Imgproc.MORPH_CLOSE, morphKernel);
       //
       Mat mask = Mat.zeros(bw.size(), CvType.CV_8UC1);
       ArrayList<MatOfPoint> contours = new ArrayList<>();
       Imgproc.findContours(connected, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
       for (int i = 0; i < contours.size(); i++) {
           Rect rect = Imgproc.boundingRect(contours.get(i));
           Mat maskROI = new Mat(mask, rect);
           Imgproc.drawContours(mask, contours, i, new Scalar(255, 255, 255), Core.FILLED);
           double r = (double) Core.countNonZero(maskROI) / (rect.width * rect.height);
           if (r > .45 *//**//* assume at least 45% of the area is filled if it contains text *//**//*
                   &&
                   (rect.height > 8 && rect.width > 8 && (rect.width > rect.height)) *//**//* constraints on region size *//**//*
            *//**//* these two conditions alone are not very robust. better to use something
           like the number of significant peaks in a horizontal projection as a third condition *//**//*
                    ) {
               rectArrayList.add(rect);
           }

       }
       return rectArrayList;
   }*/

}


