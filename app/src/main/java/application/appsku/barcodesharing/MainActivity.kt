package application.appsku.barcodesharing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import application.appsku.barcodesharing.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewmodel: MainActivityVM by viewModels()
    private val adapter by lazy { ItemDbAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!hasStoragePermission(this)) {
            requestStoragePermission(this)
        } else {
            binding.run {
//                lifecycleScope.launch(Dispatchers.IO) {
//                    repeatOnLifecycle(Lifecycle.State.STARTED){
//                        createDatabaseOnSDCard(this@MainActivity)
//                    }
//                }
                rvItemDb.also { recyclerView ->
                    recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                    recyclerView.adapter = adapter
                }

                btnSubmit.setOnClickListener {
                    //insertDataToSDCardDatabase(this@MainActivity, binding.etBarcode.text.toString())
                    //adapter.addData(readDatabaseFromSDCard(this@MainActivity))
                    viewmodel.insert(binding.etBarcode.text.toString())
                    viewmodel.getAllBarcode(this@MainActivity) {
                        adapter.addData(it)
                    }
                }
            }
        }
    }

    private val requestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE)
    } else {
        arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            requestPermission.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(activity, requestPermission, 1001)
        }
    }
    fun getDatabasePathFromSDCard(context: Context): String? {
        val uri = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(MediaStore.Files.FileColumns.DATA) // Ambil path file
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("barcode_database.db")

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }

//
    private fun Context.getSDCardPath(): String? {
        val volumeNames = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(this@MainActivity)
        } else {
            File("/storage/")
                .listFiles()
                ?.filter { it.isDirectory && it.canRead() && it.name != "emulated" }
                ?.map { it.absolutePath }
                ?.toSet()
                ?: emptySet()
        }
        return volumeNames.find { it != "external_primary" } ?: run {
            Log.e("DatabaseExport", "Tidak ditemukan SD Card!")
            ""
        }
    }
//
//
//    private fun createDatabaseOnSDCard(context: Context) {
//        val sdCardPath = context.getSDCardPath() ?: return
//        //val dbFolder = File(sdCardPath, "BarcodeApp")
//        val dbFile = File(sdCardPath, "barcode_database.db")
//
////        if (!dbFolder.exists()) {
////            val isCreated = dbFolder.mkdirs()
////            if (!isCreated) {
////                Log.e("DatabaseSDCard", "Gagal membuat folder di SD Card!")
////                return
////            }
////        }
//
//        if (!dbFile.exists()) {
//            val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
//            try {
//                // Buat tabel jika belum ada
//                db.execSQL("CREATE TABLE IF NOT EXISTS barcode_data (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT NOT NULL)")
//
//                // Insert contoh data
//                db.execSQL("INSERT INTO barcode_data (code) VALUES ('QR_12345')")
//                db.execSQL("INSERT INTO barcode_data (code) VALUES ('QR_67890')")
//
//                Log.d("DatabaseSDCard", "Database berhasil dibuat di SD Card: ${dbFile.absolutePath}")
//            } catch (e: Exception) {
//                Log.e("DatabaseSDCard", "Gagal membuat database!", e)
//            } finally {
//                db.close()
//            }
//        } else {
//            Log.d("DatabaseSDCard", "Database sudah ada di SD Card.")
//        }
//
//    }
//
//    private fun readDatabaseFromSDCard(context: Context): List<String> {
//        val list = mutableListOf<String>()
//        val sdCardPath = context.getSDCardPath() ?: return list
//        val dbFile = File(sdCardPath, "BarcodeApp/barcode_database.db")
//
//        if (!dbFile.exists()) {
//            Log.e("DatabaseSDCard", "Database tidak ditemukan di SD Card!")
//            return list
//        }
//
//        val db =
//            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
//
//        try {
//            val cursor = db.rawQuery("SELECT * FROM barcode_data", null)
//            while (cursor.moveToNext()) {
//                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
//                val code = cursor.getString(cursor.getColumnIndexOrThrow("code"))
//                Log.d("DatabaseSDCard", "ID: $id, Code: $code")
//                list.add(code)
//            }
//
//            cursor.close()
//        } catch (e: Exception) {
//            Log.e("DatabaseSDCard", "Gagal membaca database!", e)
//        } finally {
//            db.close()
//        }
//        return list
//    }
//
//    private fun insertDataToSDCardDatabase(context: Context, barcode: String) {
//        val sdCardPath = context.getSDCardPath() ?: return
//        val dbFile = File(sdCardPath, "BarcodeApp/barcode_database.db")
//
//        if (!dbFile.exists()) {
//            Log.e("DatabaseSDCard", "Database tidak ditemukan di SD Card!")
//            return
//        }
//
//        val db =
//            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
//
//        try {
//            val sql = "INSERT INTO barcode_data (code) VALUES (?)"
//            val statement = db.compileStatement(sql)
//            statement.bindString(1, barcode)
//            statement.executeInsert()
//
//            Log.d("DatabaseSDCard", "Data berhasil disimpan: $barcode")
//        } catch (e: Exception) {
//            Log.e("DatabaseSDCard", "Gagal menyimpan data!", e)
//        } finally {
//            db.close()
//        }
//    }

}