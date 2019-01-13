package com.example.orgibly.myappnotes;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DataBaseManager extends SQLiteOpenHelper{
    public static final String DATABASE_NAME = "notes_db";
    public static final String TABLE_NAME = "notes";
    public static final String COL_1_id = "id";
    public static final String COL_2_title = "title";
    public static final String COL_3_text = "text";
    public static final String EMPTY_MSG = ""; //This msg is sent when everything went good.

    public static final String CREATE_TABLE =
            "CREATE TABLE "
                    +TABLE_NAME+"("
                    +COL_1_id+" integer PRIMARY KEY AUTOINCREMENT, "
                    +COL_2_title+" text,"
                    +COL_3_text+" text);";

    public DataBaseManager(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }

    public String insertNewNote(Note note) {
        if (note == null)return null;
        String msg = EMPTY_MSG;
        try {
//            insert new note to database.
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL(
                            "INSERT INTO "+TABLE_NAME +"("
                            +COL_2_title+","
                            +COL_3_text+") "
                            +"VALUES("
                            +"'"+note.getTitle()+"',"
                            +"'"+note.getText()+"');"
                    );
//            set note id with the id given from the database.
            String getIdQuery ="SELECT MAX("+COL_1_id+") FROM "+TABLE_NAME+";";
            Cursor cursor = db.rawQuery(getIdQuery,null);
            if(cursor.moveToFirst()){
                note.setId(cursor.getInt(0));
            }
            else {
                msg = "Error: Cursor.";
            }
            cursor.close();
        } catch (SQLException e) {
            msg = e.getMessage();
        }
        return msg;
    }

    public String updateNote(Note upDatedNote){
        if (upDatedNote == null)return null;
        String msg = EMPTY_MSG;
        try {
            getWritableDatabase().execSQL("UPDATE "+TABLE_NAME
                    +" SET "
                    +COL_2_title+"="+"'"+upDatedNote.getTitle()+"'"+", "
                    +COL_3_text+"="+"'"+upDatedNote.getText()+"'"
                    +" WHERE "+COL_1_id+"="+upDatedNote.getId()
            );
        } catch (SQLException e) {
            msg = e.getMessage();
        }
        return msg;
    }

    public String deleteNote(Note note){
        if (note == null)return null;
        String msg = EMPTY_MSG;
        try {
            getWritableDatabase().execSQL("DELETE FROM "+TABLE_NAME+" WHERE id="+note.getId()+";");
        } catch (SQLException e) {
            msg = e.getMessage();
        }
        return msg;
    }

    public Note getNote(int id){
        String title;
        String text;
        Note note = null;
        Cursor cursor = getWritableDatabase().rawQuery("SELECT "+COL_2_title+","+COL_3_text
                +" FROM "+TABLE_NAME+";",null);
        if(cursor.moveToNext()) {
            title = cursor.getString(0);
            text = cursor.getString(1);
            note = new Note(title);
            note.setId(id);
            note.setText(text);
        }
        cursor.close();
        return note;
    }

//    can be optimized by handling a "notes" field and update it when database is changed.
    public ArrayList<Note> getAllNotes(){
        ArrayList<Note> notes = new ArrayList<>();

        Cursor cursor = null;
        try{cursor = getWritableDatabase().rawQuery("SELECT * FROM "+TABLE_NAME
                        +" ORDER BY "+COL_1_id+" DESC;",
                null);}
                catch (Exception e){
                    Log.i("exception-----", e.getMessage());
                }


        while (cursor.moveToNext()){
            Note newNote = new Note(cursor.getString(1));
            newNote.setId(cursor.getInt(0));
            newNote.setText(cursor.getString(2));
            notes.add(newNote);
        }
        cursor.close();
        return notes;
    }

}
