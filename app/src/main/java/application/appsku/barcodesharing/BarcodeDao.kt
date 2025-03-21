package application.appsku.barcodesharing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BarcodeDao {

    @Insert
    suspend fun insert(barcode: BarcodeEntity)

    @Query("SELECT * FROM Barcode")
    suspend fun getAllBarcode(): List<BarcodeEntity>
}