package pe.edu.idat.apppermisocamara

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import pe.edu.idat.apppermisocamara.commom.Constantes
import pe.edu.idat.apppermisocamara.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import kotlin.math.min
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var mRutaFotoActual = ""
    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btntomarfoto.setOnClickListener {
            if(permisoEscrituraAlmacenamiento()){
                try {
                    intencionTomarFoto()
                }catch (e: IOException){
                    e.printStackTrace()
                }
            }else{
                solicitarPermiso()
            }
        }
        binding.btncompartir.setOnClickListener {
            if(mRutaFotoActual != ""){
                val contentUri = obtenerContentUri(File(mRutaFotoActual))
                // Create the text message with a string
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    type = "image/jpeg"
                }
                //Android Sharesheet brinda a los usuarios la capacidad de compartir información con la persona adecuada
                val chooser: Intent = Intent.createChooser(sendIntent, "Compartir Imagen")
                // Verify that the intent will resolve to an activity
                if (sendIntent.resolveActivity(packageManager) != null) {
                    startActivity(chooser)
                }
            }else{
                Toast.makeText(applicationContext, "Debe seleccionar una imagen para compartirlo",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun permisoEscrituraAlmacenamiento(): Boolean{
        val result = ContextCompat.checkSelfPermission(
            applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        var exito = false
        if (result == PackageManager.PERMISSION_GRANTED) exito = true
        return exito
    }

    private fun solicitarPermiso(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            Constantes.ID_REQUEST_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == Constantes.ID_REQUEST_PERMISSION){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                intencionTomarFoto()
            }else{
                Toast.makeText(applicationContext, "Permiso Denegado", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //Crear el método donde guardar la imagen
    //Este método crea una Excepción por que puede devolver NULL
    @Throws(IOException::class)
    private fun crearArchivoTemporal(): File {
        val nombreImagen: String = "JPEG_"+ SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val directorioImagenes: File = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val archivoTemporal: File = File.createTempFile(nombreImagen, ".jpg", directorioImagenes)
        mRutaFotoActual = archivoTemporal.absolutePath
        return archivoTemporal
    }

    //Llamamos a la cámara mediante un Intent implícito.
    @Throws(IOException::class)
    private fun intencionTomarFoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //Validamos que el dispositivo tiene la aplicación de la cámara.
        if (takePictureIntent.resolveActivity(this.packageManager) != null) {
            val photoFile = crearArchivoTemporal()
            if (photoFile != null) {
                //creamos una URI para para el archivo
                val photoURI = obtenerContentUri(photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                getResult.launch(takePictureIntent)
            }
        }
    }
    // Receiver
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
                if(it.resultCode == Activity.RESULT_OK){

                    mostrarFoto()
                }
        }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode ==  Constantes.ID_CAMARA_REQUEST){
            if(resultCode == Activity.RESULT_OK){
                mostrarFoto()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }*/


    //Llamamos a la cámara utilizando Intent implícito.
    private fun mostrarFoto() {
        val targetW: Int = binding.ivfoto.width
        val targetH: Int = binding.ivfoto.height
        val bmOptions = BitmapFactory.Options()
        // el decodificador devolverá un valor nulo (sin mapa de bits),
        // pero los campos de salida ... aún se establecerán
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mRutaFotoActual, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight
        val scaleFactor = min(photoW / targetW, photoH / targetH)
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(mRutaFotoActual, bmOptions)
        binding.ivfoto.setImageBitmap(bitmap)

    }


    private fun obtenerContentUri(archivo: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                applicationContext,
                "pe.edu.idat.apppermisocamara.fileprovider", archivo
            )
        } else {
            Uri.fromFile(archivo)
        }
    }
}