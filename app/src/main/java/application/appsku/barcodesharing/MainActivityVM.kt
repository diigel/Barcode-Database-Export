package application.appsku.barcodesharing

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class MainActivityVM(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val barcodeDao = db.barcodeDao()

    fun insertUser(barcodeCode : String) {
        viewModelScope.launch {
            barcodeDao.insertUser(BarcodeEntity(barcodeCode = barcodeCode))
        }
    }

    fun getAllBarcode(context: Context) {
        viewModelScope.launch {
            val barcodeEntities = barcodeDao.getAllBarcode()
            barcodeEntities.forEach {
                println("Barcode is -> ${it.barcodeCode}")
            }
            if (barcodeEntities.isNotEmpty()){
                exportDatabaseToSDCard(context)
            }
        }
    }

    /**
     * Copy Sqlite to SD Card (overwrite)
     */
    private fun exportDatabaseToSDCard(context: Context) {
        val dbPath = File(context.getDatabasePath("barcode_database.db").absolutePath)

        // ✅ Cari Volume SD Card
        val volumeNames = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context)
        } else {
            File("/storage/")
                .listFiles()
                ?.filter { it.isDirectory && it.canRead() && it.name != "emulated" }
                ?.map { it.absolutePath }
                ?.toSet()
                ?: emptySet()
        }
        val sdCardVolume = volumeNames.find { it != "external_primary" } ?: run {
            Log.e("DatabaseExport", "Tidak ditemukan SD Card!")
            return
        }

        // ✅ Cek apakah file sudah ada di MediaStore
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf("barcode_database.db", "Documents/BarcodeApp/")

        val uriToDelete = context.contentResolver.query(
            MediaStore.Files.getContentUri(sdCardVolume),
            arrayOf(MediaStore.Files.FileColumns._ID),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                ContentUris.withAppendedId(MediaStore.Files.getContentUri(sdCardVolume), id)
            } else null
        }

        // ✅ Jika file lama ada, hapus sebelum overwrite
        uriToDelete?.let {
            context.contentResolver.delete(it, null, null)
            Log.d("DatabaseExport", "File lama dihapus sebelum overwrite.")
        }

        // ✅ Simpan database baru ke SD Card
        val contentValues = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, "barcode_database.db")
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/x-sqlite3")
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, "Documents/BarcodeApp/")
        }

        val uri = context.contentResolver.insert(
            MediaStore.Files.getContentUri(sdCardVolume), contentValues
        )

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                dbPath.inputStream().copyTo(outputStream)
                Log.d("DatabaseExport", "Database berhasil diekspor ke SD Card!")
            }
        } ?: Log.e("DatabaseExport", "Gagal menyimpan database ke SD Card!")
    }


    /**
     * Copy Sqlite to Internal Storage DIRECTORY_DOCUMENTS (overwrite)
     */
    private fun exportDatabase(context: Context) {
        val dbPath = File(context.getDatabasePath("barcode_database.db").absolutePath)
        val exportPath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "barcode_database.db")

        if (dbPath.exists()) {
            dbPath.copyTo(exportPath, overwrite = true)
        }
    }

}