package me.dungnguyen.demoocr;

import android.content.Intent;
import android.content.res.AssetManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.dungnguyen.demoocr.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";
    ActivityMainBinding binding;

    private int PICK_IMAGE = 1221;
    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/DemoOCR/";
    private String lang = "vie";
    private Bitmap yourSelectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        initValue();
        initEvent();
    }

    private void initValue() {
        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };
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
            new AsyncTask() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    binding.progress.setVisibility(View.VISIBLE);

                }

                @Override
                protected Object doInBackground(Object[] objects) {

                    TessBaseAPI baseApi = new TessBaseAPI();
                    // DATA_PATH = Path to the storage
                    // lang = for which the language data exists, usually "eng"
                    baseApi.init(DATA_PATH, lang);
                    // Eg. baseApi.init("/mnt/sdcard/tesseract/tessdata/eng.traineddata", "eng");
                    baseApi.setImage(yourSelectedImage);
                    String recognizedText = baseApi.getUTF8Text();
                    baseApi.end();

                    return recognizedText;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    binding.progress.setVisibility(View.GONE);
                    binding.tvResult.setText(String.valueOf(o));

                }
            }.execute();
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

}
