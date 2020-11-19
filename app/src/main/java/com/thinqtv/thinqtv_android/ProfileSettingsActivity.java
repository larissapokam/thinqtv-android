package com.thinqtv.thinqtv_android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.thinqtv.thinqtv_android.data.UserRepository;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

public class ProfileSettingsActivity extends AppCompatActivity {
    private final int PERMISSIONS_READ_FILES = 1;
    private final int PERMISSIONS_CAMERA = 2;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
       setSupportActionBar(toolbar);
       ActionBar actionBar = getSupportActionBar();
       actionBar.setDisplayHomeAsUpEnabled(true);
      //  getSupportActionBar().setDisplayHomeAsUpEnabled(true);

       HashMap<String, String> userInfo = UserRepository.getInstance().getLoggedInUser().getUserInfo();
        EditText name = findViewById(R.id.name);
        name.setText(userInfo.get("name"));
        EditText about = findViewById(R.id.about_you);
        about.setText(userInfo.get("about"));
        EditText topic1 = findViewById(R.id.topic_1);
        topic1.setText(userInfo.get("genre1"));
        EditText topic2 = findViewById(R.id.topic_2);
        topic2.setText(userInfo.get("genre2"));
        EditText topic3 = findViewById(R.id.topic_3);
        topic3.setText(userInfo.get("genre3"));

        if (userInfo.get("profilepic") != null) {
            new DownloadImageTask(findViewById(R.id.profile_image_view)).execute(userInfo.get("profilepic"));
        }
        if (userInfo.get("bannerpic") != null) {
            new DownloadImageTask(findViewById(R.id.banner_image_view)).execute(userInfo.get("bannerpic"));
        }

        imageView = null;
        Button profileImageButton = findViewById(R.id.choose_image_button);
        Context context = this;
        profileImageButton.setOnClickListener(view -> {
            imageView = findViewById(R.id.profile_image_view);
            selectImage(context);
        });

        Button bannerButton = findViewById(R.id.choose_banner_image);
        bannerButton.setOnClickListener(view -> {
            imageView = findViewById(R.id.banner_image_view);
            selectImage(context);
        });

        Button saveButton = findViewById(R.id.save_changes_button);
        saveButton.setOnClickListener(view -> {
                UserRepository.getInstance().updateProfile(context, name.getText().toString(), findViewById(R.id.profile_image_view),
                        about.getText().toString(), topic1.getText().toString(), topic2.getText().toString(),
                        topic3.getText().toString(), findViewById(R.id.banner_image_view));
                finish();});
    }


    private void selectImage(Context context) {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.choose_image));
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                if (isPermissionGrantedForCamera()) {
                    getPictureFromCamera();
                }
            }
            else if (options[item].equals("Choose from Gallery")) {
                if (isPermissionGrantedForExternalStorage()) {
                    getPictureFromStorage();
                }
            }
            else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void getPictureFromCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePicture, 0);
    }
    private void getPictureFromStorage() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        String[] mimeTypes = {"image/jpeg", "image/png", "image/jpg"};
        pickPhoto.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(pickPhoto, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_CANCELED) {
            Bitmap selectedImage = null;
            switch (requestCode) {
                case 0:
                    if (resultCode == RESULT_OK && data != null) {
                        try {
                            selectedImage = (Bitmap) data.getExtras().get("data");
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri imageUri = data.getData();
                        if (imageUri != null) {
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                selectedImage = BitmapFactory.decodeStream(inputStream);
                                inputStream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
            }
            if (selectedImage != null && imageView != null) {

                // Crop the bitmap into a square from the middle.
                // Crop the bitmap from the middle, to match the ratio of the imageView.

                int width = selectedImage.getWidth();
                int height = selectedImage.getHeight();
                double realRatio = (width * 1.0) / height;
                double idealRatio = (imageView.getWidth() * 1.0) / imageView.getHeight();
                /*if (width > height) {
                    selectedImage = Bitmap.createBitmap(selectedImage, width / 2 - height / 2, 0, height, height);
                }*/
                if (realRatio > idealRatio) { // uploaded image is too wide
                    int newWidth = (int)(idealRatio * height);
                    selectedImage = Bitmap.createBitmap(selectedImage, (width / 2) - (newWidth / 2), 0, newWidth, height);
                }


                /*else if (width < height) {
                    selectedImage = Bitmap.createBitmap(selectedImage, 0, height / 2 - width / 2, width, width);
                }*/
                else if (realRatio < idealRatio) { // uploaded image is too tall
                    int newHeight = (int)(width / idealRatio);
                    selectedImage = Bitmap.createBitmap(selectedImage, 0, (height / 2) - (newHeight / 2), width, newHeight);
                }
                imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage, imageView.getWidth(), imageView.getHeight(), true));
            }
        }
    }



    public boolean isPermissionGrantedForExternalStorage() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_READ_FILES);
            return false;
        }
    }
    public boolean isPermissionGrantedForCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_CAMERA);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getPictureFromCamera();
                }
                break;
            case PERMISSIONS_READ_FILES:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getPictureFromStorage();
                }
                break;
        }
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;
        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String...urls) {
            String imageUrl = urls[0];
            Bitmap picture = null;
            try {
                InputStream inputStream = new URL(imageUrl).openStream();
                picture = BitmapFactory.decodeStream(inputStream);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            return picture;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
