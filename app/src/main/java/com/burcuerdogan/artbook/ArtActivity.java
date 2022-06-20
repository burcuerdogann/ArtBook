package com.burcuerdogan.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.burcuerdogan.artbook.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;

    //Galeriye gidildikten sonra resim seçilince ne yapıcaz
    ActivityResultLauncher<Intent> activityResultLauncher;
    //İzni istedikten sonra izin verilirse ne yapıcaz
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        SQLiteDatabase database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if (info.equals("new")) {
            //new art
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.upload);
        } else {
            int artId = intent.getIntExtra("artId", 0);
            binding.button.setVisibility(View.INVISIBLE);

            try {

                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", new String[] {String.valueOf(artId)} );
                //artId'ye göre filtreleme işlemi
                int artNameIx = cursor.getColumnIndex("artname");
                int artistNameIx = cursor.getColumnIndex("artistname");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.artistText.setText(cursor.getString(artistNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));
                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }

                cursor.close();



            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    //Save butonu metodu
    public void save(View view){

        String name = binding.nameText.getText().toString();
        String artistName = binding.artistText.getText().toString();
        String year = binding.yearText.getText().toString();

        //selectedImage'ı almadan önce altta bitmap döndüren makeSmallerImage methoduyla küçültme işlemini yapıyoruz.
        //Save ettikten sonra SQLite içerisine kaydedilecek daha küçük boyutlu yeni bir bitmap oluşturuyoruz.
        Bitmap smallImage = makeSmallerImage(selectedImage,300);

        //Daha sonra bu oluşturduğumuz smallImage'ı byte serisine çeviriyoruz.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        //outputStream.toByteArray();
        //Görselin SQLite'a kaydedilecek olan verisini, byteArray şeklinde tanımlıyoruz.
        byte[] byteArray = outputStream.toByteArray();

        try {

            SQLiteDatabase database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)");

            //database.execSQL("INSERT INTO arts (artname, artistname, year, image) VALUES (?,?, ?, ?)");
            //Değer yazmak yerine sonradan çalıştırılabilecek bir SQLite Statement oluşturucaz.
            String sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)";
            //sqlString'i alıp database içerisinde çalıştırmak için SQLiteStatement classını kullanıyoruz.
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2, artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //Kaydettikten sonra MainActivity'e geri dönmek için
        //1.Yöntem -> finish(); çağırabiliriz -> ArtActivity kapanır. MainActivity'e geri döner.
        //2.Yöntem -> intent ve flag kullanabiliriz -> Bundan önceki bütün aktiviteleri kapat şimdi gideceğim aktiviteyi aç.
        Intent intent = new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);


    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){

        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1){
            //Landscape Image
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        } else {
            //Portrait Image
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }

        return image.createScaledBitmap(image, width, height,true);
    }

    //Select Image methodu
    public void selectImage (View view){

        //Galeriye gitme izni istemeden önce Self-Permission kontrolü yapmamız gerekiyor.
        //Android'e Self-Permission yok mu diye soruyoruz
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

            //Önce kullanıcıya izin isteme mantığını / mesajı göstermek zorunda mıyız onu kontrol ediyoruz
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){

                //Eğer kullanıcıya mesaj göstermek zorundaysak SnackBar ya da AlertDialog ile kullanıcıya mesajı gösterip izni istiyoruz
                Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Tıklandığında -> Request Permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();

            } else {
                //Eğer kullanıcıya mesaj göstermek zorunda değilsek -> Request Permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }


        } else {
            //Self-Permission varsa -> Go to Gallery
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);            /*
            Görseli seçtikten sonra ne olacağını yazmamız lazım.
            Bir aktivite sonucunda neyi başlatacağımızıs söyleyebilmek için;
            onCreate'in üstünde ActivityResultLauncher'larımızı tanımlıyoruz.
            */

        }


    }

    private void registerLauncher(){

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {

                //Kullanıcı galeriye gittikten sonra görseli seçmemiş olabilir
                if (result.getResultCode() == RESULT_OK){
                    //getData -> intenti veriyor
                    Intent intentFromResult = result.getData();
                    if (intentFromResult != null) {

                        //getData -> Kullanıcının seçtiği görselin nerede kayıtlı olduğunu veriyor
                        //Bize bu görselin verisi lazım!
                        Uri imageData = intentFromResult.getData();

                        //binding.imageView.setImageURI(imageData);

                        //Kullanıcının seçtiği görseli bitmape çevirme

                        try {
                            if (Build.VERSION.SDK_INT >= 28){
                                //Yazdıklarımı dene bir sıkıntı çıkarsa catch içerisinde yakala
                                //Görsel yerini görsele çevirme ihtimalimiz %100 olmadığı için try & catch içerisindeyiz
                                //Decoder ile Uri kullanarak Bitmap'e çevirme işlemi yapılır.
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                //Oluşturduğumuz bitmapi -> selectedImage'ı imageView'a veriyoruz.
                                binding.imageView.setImageBitmap(selectedImage);
                            } else {
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(), imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }

                        } catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                }

            }
        });

        //İzni isteme işlemi
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {

                //Eğer result true ise izin verildi
                if (result){

                    //Permission Granted
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);

                } else {

                    //Permission Denied
                    //Toast Message
                    Toast.makeText(ArtActivity.this, "Permission needed!", Toast.LENGTH_LONG).show();
                }

            }
        });


    }














}