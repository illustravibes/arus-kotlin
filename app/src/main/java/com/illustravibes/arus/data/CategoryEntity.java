package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "categories", indices = @Index(value = {"kind", "name"}, unique = true))
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public String kind;
    public String icon;
    public int colorArgb;
    public boolean isSystem;
    public boolean isArchived;
    public int sortOrder;

    public CategoryEntity(String name, String kind, String icon, int colorArgb, boolean isSystem, boolean isArchived, int sortOrder) {
        this.name = name;
        this.kind = kind;
        this.icon = icon;
        this.colorArgb = colorArgb;
        this.isSystem = isSystem;
        this.isArchived = isArchived;
        this.sortOrder = sortOrder;
    }
}
