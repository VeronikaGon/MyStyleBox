package com.hfad.mystylebox.database.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "outfits")
public class Outfit implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;
    public List<String> seasons;
    public String description;
    public int minTemp;
    public int maxTemp;
    public String imagePath;

    public Outfit() {
        this.name = "";
        this.seasons = new ArrayList<>();
        this.description = "";
        this.minTemp = 0;
        this.maxTemp = 0;
        this.imagePath = "";
    }

    public Outfit(@NonNull String name, List<String> seasons, String description,
                  int minTemp, int maxTemp, String imagePath) {
        this.name = name;
        this.seasons = seasons;
        this.description = description;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.imagePath = imagePath;
    }

    protected Outfit(Parcel in) {
        id = in.readInt();
        name = in.readString();
        seasons = in.createStringArrayList();
        description = in.readString();
        minTemp = in.readInt();
        maxTemp = in.readInt();
        imagePath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeStringList(seasons);
        dest.writeString(description);
        dest.writeInt(minTemp);
        dest.writeInt(maxTemp);
        dest.writeString(imagePath);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Outfit> CREATOR = new Creator<Outfit>() {
        @Override
        public Outfit createFromParcel(Parcel in) {
            return new Outfit(in);
        }

        @Override
        public Outfit[] newArray(int size) {
            return new Outfit[size];
        }
    };
}