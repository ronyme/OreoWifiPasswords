package com.gmail.ndraiman.wifipasswords.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;
import java.util.Date;


public class PasswordDB {

    private PasswordHelper mHelper;
    private SQLiteDatabase mDatabase;
    private static final String TAG = "PasswordDB";

    //TODO try switching all methods to use ContentValues

    public PasswordDB(Context context) {
        mHelper = new PasswordHelper(context);
        mDatabase = mHelper.getWritableDatabase();

    }


    /******************************************************/
    /****************** Insert Methods ********************/
    /******************************************************/

    public void insertEntry(WifiEntry entry, boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_PASSWORDS_HIDDEN : PasswordHelper.TABLE_PASSWORDS_MAIN;
        Log.d(TAG, "insertEntry - isHidden=" + isHidden + " table=" + table);

        String title = entry.getTitle();
        String password = entry.getPassword();

        String query = "INSERT INTO " + table + "("
                + PasswordHelper.COLUMN_TITLE + ", "
                + PasswordHelper.COLUMN_PASSWORD + ") VALUES ('"
                + title + "', '" + password + "');";

        mDatabase.execSQL(query);
    }

    public void insertWifiEntries(ArrayList<WifiEntry> listWifi, boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_PASSWORDS_HIDDEN : PasswordHelper.TABLE_PASSWORDS_MAIN;
        Log.d(TAG, "insertWifiEntries - isHidden=" + isHidden + " table=" + table);

        //create a sql prepared statement
        String sql = "INSERT INTO " + table + " VALUES (?,?,?);";

        //compile the statement and start a transaction
        SQLiteStatement statement = mDatabase.compileStatement(sql);
        mDatabase.beginTransaction();

        for (int i = 0; i < listWifi.size(); i++) {
            WifiEntry current = listWifi.get(i);
            statement.clearBindings();

            statement.bindString(2, current.getTitle());
            statement.bindString(3, current.getPassword());

            statement.execute();
        }

        //set the transaction as successful and end the transaction
        Log.d(TAG, "inserting entries " + listWifi.size() + new Date(System.currentTimeMillis()));
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /***************************************************/
    /****************** Get Methods ********************/
    /***************************************************/

    public ArrayList<WifiEntry> getAllWifiEntries(boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_PASSWORDS_HIDDEN : PasswordHelper.TABLE_PASSWORDS_MAIN;
        Log.d(TAG, "getAllWifiEntries - isHidden=" + isHidden + " table=" + table);

        String selection = null;

        if (!isHidden) {
            selection = PasswordHelper.COLUMN_TITLE + " NOT IN (SELECT "
                    + PasswordHelper.COLUMN_TITLE
                    + " FROM " + PasswordHelper.TABLE_PASSWORDS_HIDDEN + ")";
        }

        ArrayList<WifiEntry> listWifi = new ArrayList<>();

        String[] columns = {PasswordHelper.COLUMN_UID,
                PasswordHelper.COLUMN_TITLE,
                PasswordHelper.COLUMN_PASSWORD};

        //TODO modify query to return entries NOT IN hidden table
        Cursor cursor = mDatabase.query(table, columns, selection, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Log.d(TAG, "loading entries " + cursor.getCount() + new Date(System.currentTimeMillis()));

            do {

                //create a new WifiEntry object and retrieve the data from the cursor to be stored in this WifiEntry object
                WifiEntry wifiEntry = new WifiEntry();
                //each step is a 2 part process, find the index of the column first, find the data of that column using
                //that index and finally set our blank WifiEntry object to contain our data
                wifiEntry.setTitle(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_TITLE)));
                wifiEntry.setPassword(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_PASSWORD)));
                //add the WifiEntry to the list of WifiEntry objects which we plan to return
                listWifi.add(wifiEntry);

            } while (cursor.moveToNext());

            cursor.close();
        }
        return listWifi;
    }

    /******************************************************/
    /****************** Delete Methods ********************/
    /******************************************************/

    public void deleteAll(boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_PASSWORDS_HIDDEN : PasswordHelper.TABLE_PASSWORDS_MAIN;
        Log.d(TAG, "deleteAll - isHidden=" + isHidden + " table=" + table);

        mDatabase.delete(table, null, null);
    }

    public void deleteWifiEntry(WifiEntry entry, boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_PASSWORDS_HIDDEN : PasswordHelper.TABLE_PASSWORDS_MAIN;
        Log.d(TAG, "deleteWifiEntry - isHidden=" + isHidden + " table=" + table);

        String whereClause = PasswordHelper.COLUMN_TITLE + " = ?";
        String[] whereArgs = new String[]{entry.getTitle()};

        mDatabase.delete(table, whereClause,whereArgs);
    }

    public void deleteWifiEntries(ArrayList<WifiEntry> listWifi, boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_PASSWORDS_HIDDEN : PasswordHelper.TABLE_PASSWORDS_MAIN;
        Log.d(TAG, "deleteWifiEntries - isHidden=" + isHidden + " table=" + table);

        String whereClause = PasswordHelper.COLUMN_TITLE + " = ?";
        String[] whereArgs;

        for (int i = 0; i < listWifi.size(); i++) {
            WifiEntry current = listWifi.get(i);
            whereArgs = new String[]{current.getTitle()};
            mDatabase.delete(table, whereClause, whereArgs);
        }

    }

    public void close() {
        mDatabase.close();
    }


    /*************************************************************/
    /****************** Database Helper Class ********************/
    /*************************************************************/
    public static class PasswordHelper extends SQLiteOpenHelper {

        public static final String DB_NAME = "passwords_db";
        public static final int DB_VERSION = 1;

        public static final String TABLE_PASSWORDS_MAIN = "passwords_main";
        public static final String TABLE_PASSWORDS_HIDDEN = "passwords_hidden";

        public static final String COLUMN_UID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_PASSWORD = "password";

        public static final String CREATE_TABLE_MAIN = "CREATE TABLE " + TABLE_PASSWORDS_MAIN
                + " ("
                + COLUMN_UID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_PASSWORD + " TEXT"
                + ");";

        public static final String CREATE_TABLE_HIDDEN = "CREATE TABLE " + TABLE_PASSWORDS_HIDDEN
                + " ("
                + COLUMN_UID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_PASSWORD + " TEXT"
                + ");";

        private Context mContext;


        public PasswordHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            mContext = context;

        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            try {
                db.execSQL(CREATE_TABLE_MAIN);
                Log.d(TAG, "create table main executed");
                db.execSQL(CREATE_TABLE_HIDDEN);
                Log.d(TAG, "create table hidden executed");

            } catch (SQLiteException e) {
                Log.e(TAG, "ERROR: Helper onCreate - " + e);
            }

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            try {
                Log.e(TAG, "upgrade table box office executed");
                db.execSQL("DROP TABLE " + TABLE_PASSWORDS_MAIN + " IF EXISTS;");
                db.execSQL("DROP TABLE " + TABLE_PASSWORDS_HIDDEN + " IF EXISTS;");
                onCreate(db);

            } catch (SQLiteException e) {
                Log.e(TAG, "ERROR: Helper onUpgrade - " + e);
            }

        }
    }


}
