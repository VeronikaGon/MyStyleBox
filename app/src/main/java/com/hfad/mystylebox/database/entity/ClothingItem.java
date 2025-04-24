package com.hfad.mystylebox.database.entity;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "clothing_item",
        foreignKeys = @ForeignKey(entity = Subcategory.class,
                parentColumns = "id",
                childColumns = "subcategory_id",
                onDelete = ForeignKey.CASCADE))
public class ClothingItem implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String gender;
    public String imagePath;
    public String name;
    @ColumnInfo(name = "subcategory_id")
    public int subcategoryId;
    public String brend;
    public List<String> seasons;
    public float cost;
    public String status;
    public String size;
    public String notes;

    public ClothingItem(String name, int subcategoryId, String brend, String gender, String imagePath,
                        List<String> seasons, Float cost, String status, String size, String notes) {
        this.imagePath = imagePath;
        this.name = name;
        this.subcategoryId = subcategoryId;
        this.brend = brend;
        this.gender = gender;
        this.seasons = seasons;
        this.cost = cost;
        this.status = status;
        this.size = size;
        this.notes = notes;
    }
    protected ClothingItem(Parcel in) {
        id = in.readInt();
        gender = in.readString();
        imagePath = in.readString();
        name = in.readString();
        subcategoryId = in.readInt();
        brend = in.readString();
        seasons = in.createStringArrayList();
        cost = in.readFloat();
        status = in.readString();
        size = in.readString();
        notes = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(gender);
        dest.writeString(imagePath);
        dest.writeString(name);
        dest.writeInt(subcategoryId);
        dest.writeString(brend);
        dest.writeStringList(seasons);
        dest.writeFloat(cost);
        dest.writeString(status);
        dest.writeString(size);
        dest.writeString(notes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ClothingItem> CREATOR = new Creator<ClothingItem>() {
        @Override
        public ClothingItem createFromParcel(Parcel in) {
            return new ClothingItem(in);
        }

        @Override
        public ClothingItem[] newArray(int size) {
            return new ClothingItem[size];
        }
    };
}