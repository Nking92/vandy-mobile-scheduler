package com.vmat;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class TwitterDB extends SQLiteOpenHelper {
    private static final  String DATABASE_NAME="twitter.db";
    public static final String TABLE_NAME = "twitter";
    private static final int SCHEMA_VERSION=1;
    private static final String WEB_ADDRESS = "http://api.twitter.com/1/statuses/user_timeline.json?screen_name=VandyMobile&include_rts=1";
    static final String CREATED_AT = "created_at";
    static final String TEXT = "text";
    static final String ID = "_id";

    public static final String DEFAULT_ORDER = "_id";

    public TwitterDB(Context context){
        super(context, DATABASE_NAME, null, SCHEMA_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "created_at TEXT, text TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
        onCreate(db);
    }

    public void insert(String created_at, String text)
    {
        ContentValues cv = new ContentValues();
        cv.put(CREATED_AT, created_at);
        cv.put(TEXT, text);
        getWritableDatabase().insert("twitter", null, cv);

    }

    public void update(String where, String[] whereArgs, String created_at, String text)
    {
        ContentValues cv = new ContentValues();
        cv.put(CREATED_AT, created_at);
        cv.put(TEXT, text);
        getWritableDatabase().update(TABLE_NAME, cv, where, whereArgs);
    }
}
