package com.example.sanatkitabi

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.sanatkitabi.databinding.ActivityArtBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.lang.Exception
//Bilgi sql lite içersinde 1mb aşan bir row oluşturamıyoruz.Hafizayı çökertir çökmese bile sorun açar.!!
//Veri Tabanlarına resim kaydederken genellikle veriye çevirip öyle kaydederi jpg ve benzeri kayıtlar olumsuzdur.
class ArtActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher:ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher:ActivityResultLauncher<String>
      var selectedBitmap  : Bitmap?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityArtBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)
        //Register Kısımlarını kayıt etme kısımlarını Oncreate altında yapmamız gerekmektedir.
        registerLauncher()
    }
    fun saveButtonClick(view :View){
        //Bu alanda sql lite row da aşım olmaması için kilobyte olarak ayarlama yapacağız.
        val artName=binding.artnameTxt.text.toString()
        val artistName=binding.artistnameTxt.toString()
        val year=binding.yeartxtname.toString()
        if (selectedBitmap!=null){

            val smallBitmap=makeSmallerBitmap(selectedBitmap!!,300)

            val outputStream=ByteArrayOutputStream()//değişken oluşturduk
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)//ezip format olarakpng belirttik ve kalite olarak da 50 verdik sonra da aktarım alanını cavaplıyoruz.
            val byteArray=outputStream.toByteArray()

            try {
                val database=this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)                                       //görsel olaeak önemli değil veri olarak da işledik.
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRİMARY KEY, artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)")//ALT SORGUDA İSTENEN ŞARTI SAĞLAMAMASI DURUMUNDAN KULLANILIR.
           val sqlString= "INSERT INTO arts (artname,artistname,yeae,image) VALUES (?,?,?,?)"
                val statement=database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()
            }catch (ex:Exception){
                ex.printStackTrace()
            }
            //burdan sonra main activiye dönmemiz lazım bunun için ya finish kullanırız yada Intent Çalıştırmak.BURADA finish mainin arka tarafda açık bırakır o yüzden addFlag kullanıyoruz.
            val intent=Intent(this@ArtActivity ,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)// BU Şu Anlama Gelir Bundan Önce Açık Ne kadar aktivite varsa hepsini kapat!!ve maine geri dön
            startActivity(intent)

        }

    }//Bu Fonsiyon OOP her alanda kullanılabilir.
    private fun makeSmallerBitmap(image:Bitmap,maximumsize:Int):Bitmap{
        //bu kısımda resimi bozmadan küçültmesi için kodlama yapıyoruz.
        var width=image.width
        var height=image.height
        val bitmapRatio:Double=width.toDouble()/height.toDouble()
        if (bitmapRatio>1){
            width=maximumsize
            //değiştirilen Boyut =height
            val scaledHeight=width/bitmapRatio
            height=scaledHeight.toInt()
//landscape image
        }else{
            height=maximumsize
            val scaledWidth=height*bitmapRatio//bu alanda dikey olduğu için 0.3 olduğundan çarpma olarak aldık
            width=scaledWidth.toInt()
//portrait image

        }
        //bitmap=piksel verileriyle tanımlanan görüntülerle çalışmak için kullanılan bir nesnedir.
        return Bitmap.createScaledBitmap(image,width,height,true)
// yukardaki fonksiyon resim kalitesini bozmadan kayıt alma için OOP olarak tasarladım her alanda kullanılabilir.
    }
    fun selectimage(view: View){
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){// Bu bir satır kod da önemli olan alanın başlangıcı!!!!!33 ve sonrası için...
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_MEDIA_IMAGES)!=PackageManager.PERMISSION_GRANTED

            ){
                /*Compet üzerine kullanım yapacağız eski versiyonlarla uyumlu olma Hikayesi
                * api 19 ile gümüze geldi ondan önce izin falan istemiyorduk */
                /*allta açılan kısıma ister misin istemez misin kısmına mantık rationale deniyor ,android işletim sistemi karar veriypr
                * bunu kullanıcıya göstereyim mi göstermeyeyim mi diye!*/
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                    // Yani izin alma mantığını kullanıcıya göstereyim mi!shouldShowRequestPermissionRationale
                    //rationale istedikden sonra bu şekilde bırakmayacaız izni isteyeciz mantığı alıp bırakmıyoruz.
                    Snackbar.make(view,"permisson for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        //request permission
                    }).show()
                    //burada bizden görünüm veya context istiyor.
//burada gösterdikten sonra istıyoruz
                }

                //sdk 33 ve üzeri olduğu durumlarda kullanıyoruz.
             //ÖNEMLİ !!!!! ALT KISIMDA VERDİĞİMİZ READ_MEDIA_IMAGES YI MANİFESTE EKLEDİKTEN SONRA
                // BU KISIMDAKİ ALANLARDA VERDİĞİMİZ İZİNLERDEN PERMİSSİONLARDAN SONRA EKLEMEMİZ GEREKİR.!


                else{
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    // requestPermissions
                    //burada göstermeden istiyoruz.
                }
            }
            else {
                val ıntentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                //Galeri ye gideceğiz ve oradan bir action pick yapacagız. ve ordaan bir veri alacağız.
                //Sonra İntenti yapacağız ve bu start activity ile değil! Start for activity result
                //Sonuç için bunu başlat.
                activityResultLauncher.launch(ıntentToGallery)
            }
            //Androıd 33+-> Read_Media_images
        }else{
            //andrid 32-->Read External Storage
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_MEDIA_IMAGES)!=PackageManager.PERMISSION_GRANTED

            ){
                /*Compet üzerine kullanım yapacağız eski versiyonlarla uyumlu olma Hikayesi
                * api 19 ile gümüze geldi ondan önce izin falan istemiyorduk */
                /*allta açılan kısıma ister misin istemez misin kısmına mantık rationale deniyor ,android işletim sistemi karar veriypr
                * bunu kullanıcıya göstereyim mi göstermeyeyim mi diye!*/
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                    // Yani izin alma mantığını kullanıcıya göstereyim mi!shouldShowRequestPermissionRationale
                    //rationale istedikden sonra bu şekilde bırakmayacaız izni isteyeciz mantığı alıp bırakmıyoruz.
                    Snackbar.make(view,"permisson for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        //request permission
                    }).show()
                    //burada bizden görünüm veya context istiyor.
//burada gösterdikten sonra istıyoruz
                }else{
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    // requestPermissions
                    //burada göstermeden istiyoruz.
                }
            }
            else {
                val ıntentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                //Galeri ye gideceğiz ve oradan bir action pick yapacagız. ve ordaan bir veri alacağız.
                //Sonra İntenti yapacağız ve bu start activity ile değil! Start for activity result
                //Sonuç için bunu başlat.
                activityResultLauncher.launch(ıntentToGallery)
            }
        }


    }
    private fun registerLauncher(){
        //bir aktivite başlatacak bir galeriye gidip bir sonuca bakacak.
activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
    if (result.resultCode== RESULT_OK){
//Bu alanlara print yazarak logcat içersinde hata ayıklanabilir.
        val intentFromResult=result.data
        if (intentFromResult!=null){
           val imageData=intentFromResult.data
           // binding.imageView.setImageURI(imageData)
            //normalde işimiz burda bitiyor ama bitmap alıp sql lite ye küçültüp kayıt yapmamız gerekmektedir
            if (imageData!==null){
//if içersine de yazarak hata bulunabilir.
        try {
            if (Build.VERSION.SDK_INT>=28){//bu kısımda createSource sdk sı 28 üzeri bulunması gerektiği için if şartı bulundurduk.
                val source= ImageDecoder.createSource(this@ArtActivity.contentResolver,imageData)
                selectedBitmap=ImageDecoder.decodeBitmap(source)
                binding.imageView.setImageBitmap(selectedBitmap)
            } else
            {
                selectedBitmap=MediaStore.Images.Media.getBitmap(contentResolver,imageData)//Burada Kaldım15.00
           binding.imageView.setImageBitmap(selectedBitmap)
            }



        }catch (ex:Exception){
            ex.printStackTrace()
        }
        }
}
    }

}
permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){result->
    if (result){
      //permission granted__  izin verildi
        val ıntentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
activityResultLauncher.launch(ıntentToGallery)

    }else
    {
        Toast.makeText(this@ArtActivity,"permission needed",Toast.LENGTH_LONG).show()
        //permission denied __  izin reddedildi

    }

}

    }
}