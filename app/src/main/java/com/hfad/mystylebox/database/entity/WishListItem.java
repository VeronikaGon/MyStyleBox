package com.hfad.mystylebox.database.entity;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.hfad.mystylebox.database.entity.Subcategory;

@Entity(
        tableName = "wish_list_item",
        foreignKeys = @ForeignKey(
                entity = Subcategory.class,
                parentColumns = "id",
                childColumns = "subcategory_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "subcategory_id")}
)
public class WishListItem implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "image_path")
    public String imagePath;

    public String name;
    public double price;
    public String notes;
    public String size;

    @ColumnInfo(name = "subcategory_id")
    public int subcategoryId;

    public String gender;

    public WishListItem(String imagePath,
                        String name,
                        double price,
                        String notes,
                        String size,
                        int subcategoryId,
                        String gender) {
        this.imagePath     = imagePath;
        this.name          = name;
        this.price         = price;
        this.notes         = notes;
        this.size          = size;
        this.subcategoryId = subcategoryId;
        this.gender        = gender;
    }

    protected WishListItem(Parcel in) {
        id             = in.readInt();
        imagePath      = in.readString();
        name           = in.readString();
        price          = in.readDouble();
        notes          = in.readString();
        size           = in.readString();
        subcategoryId  = in.readInt();
        gender         = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(imagePath);
        dest.writeString(name);
        dest.writeDouble(price);
        dest.writeString(notes);
        dest.writeString(size);
        dest.writeInt(subcategoryId);
        dest.writeString(gender);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<WishListItem> CREATOR =
            new Parcelable.Creator<WishListItem>() {
                @Override
                public WishListItem createFromParcel(Parcel in) {
                    return new WishListItem(in);
                }
                @Override
                public WishListItem[] newArray(int size) {
                    return new WishListItem[size];
                }
            };
}