package com.hfad.mystylebox.database.entity

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded

data class ClothingItemWithCategory(
    @Embedded
    val clothingItem: ClothingItem,

    @ColumnInfo(name = "category_name")
    val categoryName: String,

    @ColumnInfo(name = "subcategory_name")
    val subcategoryName: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(ClothingItem::class.java.classLoader)!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(clothingItem, flags)
        parcel.writeString(categoryName)
        parcel.writeString(subcategoryName)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ClothingItemWithCategory> {
        override fun createFromParcel(parcel: Parcel): ClothingItemWithCategory {
            return ClothingItemWithCategory(parcel)
        }

        override fun newArray(size: Int): Array<ClothingItemWithCategory?> {
            return arrayOfNulls(size)
        }
    }
}