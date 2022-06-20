package com.burcuerdogan.artbook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.burcuerdogan.artbook.databinding.ActivityMainBinding;

import java.util.ArrayList;

/*
 Gradle(Module)'da BuildFeatures'a viewBinding true ekliyoruz.
 res altında menu klasörü içerisinde menu.xml oluşturuyoruz
 onCreateOptionMenu ile menuyu oluşturup içerisinde art_menu.xml'i MenuInflater ile bağlıyoruz
 onOptionItemSelected ile menuye tıklandığında ne olacağını yazıyoruz (Intent ile ArtActivity'e gidecek)
 Galeriye gitmek için izin işlemlerini yapıyoruz
 https://developer.android.com/reference/android/Manifest.permission
 Genel olarak izinleri ikiye ayırırız:
 Normal izinler (protection level: normal) ve Tehlikeli izinler (protection level: dangerous)
 AndroidManifest.xml'e gelip package altına uses-permission ile kullanacağımız permissionı ekliyoruz
 selectImage methodu altında izinleri kullanacağız
 Galeriye gitme izni istemeden önce if(ContextCompat.checkSelfPermission(context,permission)) ile Self-Permission kontrolü yapmamız gerekiyor.
 Android'e Self-Permission yok mu diye soruyoruz
 Önce kullanıcıya izin isteme mantığını / mesajı göstermek zorunda mıyız onu kontrol ediyoruz
 Eğer kullanıcıya mesaj göstermek zorundaysak -> SnackBar ya da AlertDialog ile kullanıcıya mesajı gösterip izni istiyoruz
 Tıklandığında -> Request Permission
 Eğer kullanıcıya mesaj göstermek zorunda değilsek -> Request Permission
 Self-Permission varsa -> Go to Gallery
 Görseli seçtikten sonra ne olacağını yazmamız lazım.
 Bir aktivite sonucunda neyi başlatacağımızıs söyleyebilmek için;
 onCreate'in üstünde ActivityResultLauncher'larımızı tanımlıyoruz.
 Galeriye gidildikten sonra resim seçilince ne yapıcaz:
 ActivityResultLauncher<Intent> activityResultLauncher;
 İzni istedikten sonra izin verilirse ne yapıcaz:
 ActivityResultLauncher<String> permissionLauncher;
 Launcherları -onCreate altında- ayrı bir method içerisinde register edip tanımlamamız lazım.
 Kullanıcı galeriye gittikten sonra görseli seçmemiş olabilir
 getData -> intenti veriyor
 getData -> Kullanıcının seçtiği görselin nerede kayıtlı olduğunu veriyor
 Bize bu görselin verisi lazım!
 Uri imageData = intentFromResult.getData();
 binding.imageView.setImageURI(imageData);
 Kullanıcının seçtiği görseli bitmape çevirme
 Yazdıklarımı dene bir sıkıntı çıkarsa catch içerisinde yakala
 Görsel yerini görsele çevirme ihtimalimiz %100 olmadığı için try & catch içerisindeyiz
 Decoder ile Uri kullanarak Bitmap'e çevirme işlemi yapılır.
 ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageData);
 selectedImage = ImageDecoder.decodeBitmap(source);
 Oluşturduğumuz bitmapi -> selectedImage'ı imageView'a veriyoruz.
 binding.imageView.setImageBitmap(selectedImage);
 registerLauncher'ı kullanmadan önce onCreate altında çağırmamız lazım.
 Save methodu içerisinde Stringleri alıyoruz.
 String name = binding.nameText.getText().toString; ,etc.
 Görseli alırken selectedImage'ı önce küçültmemiz gerekir.
 SQLite boyut sınırlaması vardır.
 O nedenle yerel veritabanlarına görsel kaydetmemizi gereken durumlarda görsellerin boyutunu küçültme işleminde kullanacağımız bir algoritma yazıyoruz.
 Verileri Çekme İşlemi -> Try and Catch içerisinde yapıyoruz.
 Her şeyi seçicez ama bütün verileri almamıza gerek. Sadece ismini göstereceğimiz için ismini ve detayların gösterileceği aktivite ile eşleştirebilmek için id'sini çekmemiz yeterli.
 Daha sonrasında bu verileri REecycler View içerisinde göstermeye çalışıcaz.
 RecyclerAdapter'ü yazıyoruz.
 ArtAdapter adında yeni bir java classı oluşturuyoruz.
 extend ederek RecyclerView.Adapter<VH>'den inheritance / miras alıyoruz.
 ViewHolder ekleyebilmek için class içerisine ArtHolder isimli yardımcı bir class oluturuyoruz.
 ArtHolder yardımcı classımıza da REcyclerView.Viewholder'dan miras alıyoruz.
 ArtHolder'a bağlayabileceğimiz bir recycler_row.XML'i oluşturuyoruz.
 XML'de Linear Layout içerisine ArtName'in liste içerisinde RecyclerView olarak gösterileceği bir TextView koyuyoruz.
 TextView id: recyclerViewTextView
 ArtAdapter classı içerisinde miras aldığımız RecyclerView.Adapter<VH> içerisine <ArtAdapter.ArtHolder> yazıyoruz.
 Alt + Enter -> 3 tane methodu uyguluyoruz.
 1.method: onCreateViewholder() -> XML'imizi koda bağlama işlemini yapıyoruz.
 2.method: onBindViewHolder() -> XML içerisinde hangi verilerin gösterileceğini söylüyoruz.
 3.method: getItemCount() -> XML'imizin kaç defa gösterileceğini söylüyoruz.
 MainActivity'de namelere tıklanınca ilgili ArtActivity'e gitme işlemini yapıyoruz.
 Önce kullanıcının hangi id'yi seçtiğini anlamamız lazım.
 Ama kullanıcının name'e mi tıklayıp gittiğini yoksa add_art'a tıklayıp mı gittiğini bilmemiz lazım.

*/

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    ArrayList<Art> artArrayList;
    ArtAdapter artAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        artArrayList = new ArrayList<>();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        artAdapter = new ArtAdapter(artArrayList);
        binding.recyclerView.setAdapter(artAdapter);

        //Veri geldiğinde artAdapter'un kendisini güncellemesi gerekir.
        //getData() içerisinde cursor.close()'dan önce artAdapter.notifyDataSetChanged() dememiz lazım.

        getData();

    }

    //Verileri Çekme İşlemi

    public void getData(){

        try {

            SQLiteDatabase sqLiteDatabase = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM arts", null);
            //Her şeyi seçicez ama bütün verileri almamıza gerek. Sadece ismini göstereceğimiz için ismini ve detayların gösterileceği aktivite ile eşleştirebilmek için id'sini çekmemiz yeterli.

            int nameIx = cursor.getColumnIndex("artname");
            int idIx = cursor.getColumnIndex("id");

            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIx);
                int id = cursor.getInt(idIx);
                Art art = new Art(name,id);
                artArrayList.add(art);

            }
            //Veri seti değişti haberin olsun diyoruz.
            artAdapter.notifyDataSetChanged();

            cursor.close();

        } catch (Exception e){
            e.printStackTrace();
        }

    }

    //Menüyü bağlamak için 2 tane methodu çağırıp bağlamamız lazım (Sonra onCreate'den önce view Bindinglerimizi yapıcaz)
    // Oluşturduğumuz menüyü aktiviteye/koda bağlama işlemi -> MenuInflater

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.art_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    //Menüye tıklandığında ne olacağını söyleyen method

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.add_art){
            Intent intent = new Intent(this, ArtActivity.class);
            intent.putExtra("info", "new");
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}