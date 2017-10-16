package me.dungnguyen.demoocr;

import android.content.Intent;
import android.content.res.AssetManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.dungnguyen.demoocr.databinding.ActivityMainBinding;

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
            for (int i = 0; i < rectArrayList.size(); i++) {
                Rect itemRect = rectArrayList.get(i);
               /* if(i == 30){
                    Bitmap bitmap = Bitmap.createBitmap(yourSelectedImage, itemRect.x, itemRect.y, itemRect.width, itemRect.height);
                    binding.imageChanged.setImageBitmap(bitmap);
                }*/
                canvas.drawRect(itemRect.x, itemRect.y, itemRect.width, itemRect.height, paint);
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

    private ArrayList<Rect> detectTextZone(Bitmap bitmap) {
        Mat large = new Mat();

        Mat rgb = new Mat();
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        Utils.bitmapToMat(bmp, large);

        ArrayList<Rect> rectArrayList = new ArrayList<>();
        Mat img_gray = new Mat(), img_sobel = new Mat(), img_threshold = new Mat(), element = new Mat();
        Imgproc.cvtColor(large, img_gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Sobel(img_gray, img_sobel, CvType.CV_8U, 1, 0, 3, 1, 0, Core.BORDER_DEFAULT);
        //at src, Mat dst, double thresh, double maxval, int type
        Imgproc.threshold(img_sobel, img_threshold, 0, 255, 8);
        element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 1));
        Imgproc.morphologyEx(img_threshold, img_threshold, Imgproc.MORPH_CLOSE, element);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(img_threshold, contours, hierarchy, 0, 1);

        List<MatOfPoint> contours_poly = new ArrayList<MatOfPoint>(contours.size());

        for (int i = 0; i < contours.size(); i++) {

            MatOfPoint2f mMOP2f1 = new MatOfPoint2f();
            MatOfPoint2f mMOP2f2 = new MatOfPoint2f();

            contours.get(i).convertTo(mMOP2f1, CvType.CV_32FC2);
            Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, 3, true);
            mMOP2f2.convertTo(contours.get(i), CvType.CV_32S);


            Rect appRect = Imgproc.boundingRect(contours.get(i));
            if (appRect.width > appRect.height && !rectArrayList.contains(appRect)) {
                rectArrayList.add(appRect);
            }

        }
        return rectArrayList;
    }
}


