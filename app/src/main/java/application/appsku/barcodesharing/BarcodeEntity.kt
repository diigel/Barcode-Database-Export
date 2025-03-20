package application.appsku.barcodesharing

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Barcode")
data class BarcodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,
    val barcodeCode : String
)
