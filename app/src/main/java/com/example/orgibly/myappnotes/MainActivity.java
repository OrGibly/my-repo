package com.example.orgibly.myappnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.orgibly.myappnotes.DialogInterface.OnClickListenerWrapper;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class MainActivity extends Activity {

    //Avoid setting their (noteTitle and noteText) text directly,
    //use "setCurNote(Note note)" method instead.
    TextView noteTitle;
    EditText noteText;
    //saveBtn.isEnabled() indicates if its right moment to save or not.
    //thus use setEnabled correspondingly.
    Button saveBtn;
    Button deleteBtn;
    Button newNoteBtn;
    Button skinBtn;
    //actions on notesList are via the notesListAdapter.
    ListView notesList;
    NotesListAdapter notesListAdapter;
    DataBaseManager db;
    //Represents the Note that is shown on the screen.
    //CurNote is static to keep it when onDestroy()
    //  is called but the app is still running(fixes screen rotation bug).
    private static Note curNote = null;// Never assign directly , use setCurNote() method.

    //SaveBtnLastEnabledState is static to keep it when onDestroy()
    //  is called but the app is still running(fixes screen rotation bug).
    //When the activity starts(after screen rotation), onTextChanged is called and enables saveBtn.
    //* Used in onPause() overriding only.
    private static boolean saveBtnLastEnabledState = false;

    //-----------------------------------------
    // grey,green,pink,blue skins.
    private static final int NUM_OF_SKINS = 4;
    private static final int[][] SKIN_COLORS = {
            {R.color.Grey_1, R.color.Grey_2, R.color.Grey_3, R.color.Grey_4, R.color.Grey_5},
            {R.color.Green_1, R.color.Green_2, R.color.Green_3, R.color.Green_4, R.color.Green_5},
            {R.color.Pink_1, R.color.Pink_2, R.color.Pink_3, R.color.Pink_4, R.color.Pink_5},
            {R.color.Blue_1, R.color.Blue_2, R.color.Blue_3, R.color.Blue_4, R.color.Blue_5}};

    private static final int[][] SKIN_RESOURCES= {
            //grey
            {R.drawable.selector_note_item_grey,R.drawable.selector_new_note_grey,R.drawable.selector_ic_save_grey,
                R.drawable.selector_ic_del_grey,R.drawable.shape_frame_1_grey,R.drawable.shape_frame_2_grey,
                R.drawable.selector_paper_grey},

            //green
            {R.drawable.selector_note_item_green,R.drawable.selector_new_note_green,R.drawable.selector_ic_save_green,
                R.drawable.selector_ic_del_green,R.drawable.shape_frame_1_green,R.drawable.shape_frame_2_green,
                R.drawable.selector_paper_green},

            //pink
            {R.drawable.selector_note_item_pink,R.drawable.selector_new_note_pink,R.drawable.selector_ic_save_pink,
                R.drawable.selector_ic_del_pink,R.drawable.shape_frame_1_pink,R.drawable.shape_frame_2_pink,
                R.drawable.selector_paper_pink},

            //blue
            {R.drawable.selector_note_item_blue,R.drawable.selector_new_note_blue,R.drawable.selector_ic_save_blue,
                    R.drawable.selector_ic_del_blue,R.drawable.shape_frame_1_blue,R.drawable.shape_frame_2_blue,
                    R.drawable.selector_paper_blue}};

    private static int curSkin=0;

    //---------------------------------------
    private static final int MAX_NUM_OF_NOTES=20;

    public static final int METHOD_DO_NOTHING = 0;
    public static final int METHOD_SHOW_NEW_NOTE_DIALOG = 1;
    public static final int METHOD_PICK_A_NOTE = 2;

    //---------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DataBaseManager(this);

        noteTitle = findViewById(R.id.textViewNoteTitle);
        noteText = findViewById(R.id.editTextNoteText);
        noteTitle.addTextChangedListener(textWatcherNoteChanged);
        noteText.addTextChangedListener(textWatcherNoteChanged);

        saveBtn = findViewById(R.id.buttonSave);
        deleteBtn = findViewById(R.id.buttonDelete);
        newNoteBtn = findViewById(R.id.buttonNewNote);
        skinBtn = findViewById(R.id.buttonSkin);
        notesList = findViewById(R.id.listViewNotes);
        notesListAdapter = new NotesListAdapter();
        notesList.setAdapter(notesListAdapter);
        notesList.setOnItemClickListener(onItemClickListener);

        curSkin = getSharedPreferences(
                "curSkin",Context.MODE_PRIVATE).getInt("index",-1);
        setNextSkin();
        setCurNote(curNote);
    }

    //onResume() & onStop() are overridden to fix rotation bug
    //When the activity starts(after screen rotation),
    //onTextChanged is also called and enables saveBtn.
    @Override
    protected void onResume(){
        super.onResume();
        saveBtn.setEnabled(saveBtnLastEnabledState);
    }

    @Override
    protected void onStop(){
        super.onStop();
        saveBtnLastEnabledState = saveBtn.isEnabled();
        getSharedPreferences("curSkin", Context.MODE_PRIVATE)
                .edit().putInt("index",(curSkin-1)%NUM_OF_SKINS).apply();
    }

//    ---------------------------- Main screen buttons related methods ----------------------------
    //new Note button callback
    public void addNewNote(View v){
        if (saveBtn.isEnabled()){
            String msg = getResources().getString(R.string.save_alert_dialog_msg_1);
            showSaveAlertDialog(msg, METHOD_SHOW_NEW_NOTE_DIALOG);
        } else {
            showNewNoteDialog();
        }
    }

    //These references are here for using them also in another method.
    //They might be Null.
    EditText editTextNewNoteTitle;
    AlertDialog dialogNewNote;
    private void showNewNoteDialog(){
        if(notesListAdapter.getCount()<MAX_NUM_OF_NOTES) {
            LayoutInflater inflater = getLayoutInflater();
            View newNoteTitle = inflater.inflate(R.layout.new_note_title_input,null);
            editTextNewNoteTitle = newNoteTitle.findViewById(R.id.editTextNewNoteTitle);
            editTextNewNoteTitle.addTextChangedListener(textWatcherNewNoteDialog);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String msg = getResources().getString(R.string.new_note_dialog_msg);
            dialogNewNote = builder
                    .setMessage(msg)
                    .setView(newNoteTitle)
                    .setPositiveButton("OK",onClickListenerNewNote)
                    .setNegativeButton("Cancel",null)
                    .setCancelable(false)
                    .create();
            dialogNewNote.setOnShowListener(onShowListener);// disable OK button.
            dialogNewNote.show();
        }else {
            Toast toast = Toast.makeText(MainActivity.this,
                    "Can not add a new note,\n" +
                            "Maximal number of notes: "+MAX_NUM_OF_NOTES+".\n" +
                            "To solve this, delete notes.", Toast.LENGTH_LONG);
            toast.show();
            newNoteBtn.setEnabled(false);
        }
    }

    private void addNewNote(String newTitle){
        Note newNote = new Note(newTitle);
        String msg = db.insertNewNote(newNote);
        if (msg == null) return;
        if (msg.equals(DataBaseManager.EMPTY_MSG)){
            notesListAdapter.notifyDataBaseChanged();
            setCurNote(newNote);
        } else {
            noteText.setText(msg);
        }
    }

    private int lastClickedItemPosition=-1;
    //Show on screen the note that last picked from the notes list.
    private void pickANote(){
        int notePosition = lastClickedItemPosition;
        if(notePosition>=0){
            Note noteToPick = (Note)notesListAdapter.getItem(notePosition);
            setCurNote((noteToPick));
        }
    }
//-------------------------------------------------------------------------------

    //save button callback.
    public void saveNote(View v){
        String msg = getResources().getString(R.string.save_alert_dialog_msg_2);
        showSaveAlertDialog(msg, METHOD_DO_NOTHING);
    }

    private void showSaveAlertDialog(String msg,int nextMethod){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(false);
        builder.setMessage(msg);
        onClickListenerSaveNotePos.setNextMethod(nextMethod);//*listener Wrapper method.
        builder.setPositiveButton("Yes", onClickListenerSaveNotePos);
        onClickListenerSaveNoteNeg.setNextMethod(nextMethod);//*listener Wrapper method.
        builder.setNegativeButton("No", onClickListenerSaveNoteNeg);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //save changes in database.
    public void saveNote(){
        curNote.setTitle(noteTitle.getText().toString());
        curNote.setText(noteText.getText().toString());
        String msg = db.updateNote(curNote);
        if (msg == null) return;
        if (msg.equals(DataBaseManager.EMPTY_MSG)) {
            saveBtn.setEnabled(false);
            Toast.makeText(MainActivity.this,
                    "Note has been saved.", Toast.LENGTH_SHORT).show();
            notesListAdapter.notifyDataBaseChanged();
        }
    }

//-------------------------------------------------------------------------------
    //delete button callback.
    public void deleteNote(View v){showDeleteNoteDialog();}

    public void showDeleteNoteDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(false);
        String msg = getResources().getString(R.string.delete_alert_dialog_msg);
        builder.setMessage(msg);
        builder.setPositiveButton("Yes", onClickListenerDeleteNotePos);
        builder.setNegativeButton("No", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteNote(){
        String msg = db.deleteNote(curNote);
        if(msg==null)return;
        if (msg.equals(DataBaseManager.EMPTY_MSG)) {
            Toast.makeText(this,
                    "Note has been deleted.", Toast.LENGTH_SHORT).show();
            setCurNote(null);
            notesListAdapter.notifyDataBaseChanged();
            if (notesListAdapter.getCount()<MAX_NUM_OF_NOTES) {
                newNoteBtn.setEnabled(true);
            }
        }
    }

//-------------------------------------------------------------------------------
    //skin button callback.
    public void setNextSkin(View v){
        setNextSkin();
    }

    public void setNextSkin(){
        curSkin = (curSkin+1)%NUM_OF_SKINS;
        newNoteBtn.setBackgroundResource(SKIN_RESOURCES[curSkin][1]);
        saveBtn.setBackgroundResource(SKIN_RESOURCES[curSkin][2]);
        deleteBtn.setBackgroundResource(SKIN_RESOURCES[curSkin][3]);
        skinBtn.setBackgroundResource(SKIN_RESOURCES[curSkin][1]);
        notesList.setBackgroundResource(SKIN_RESOURCES[curSkin][4]);
        noteText.setBackgroundResource(SKIN_RESOURCES[curSkin][6]);
        noteText.setPadding(30,30,30,30);
        findViewById(R.id.noteTitleLinearLayout).setBackgroundResource(SKIN_COLORS[curSkin][0]);
        findViewById(R.id.noteLinerLayout).setBackgroundResource(SKIN_RESOURCES[curSkin][4]);
        findViewById(R.id.headRelativeLayout).setBackgroundResource(SKIN_RESOURCES[curSkin][5]);
        notesListAdapter.notifyDataSetChanged();
    }
//    ---------------------------- Additional private methods ----------------------------

    private void setCurNote(Note note){
        curNote = note;
        if (curNote!= null) {
            noteTitle.setText(note.getTitle());
            noteText.setText(note.getText());
            noteText.setEnabled(true);
            noteTitle.setEnabled(true);
            deleteBtn.setEnabled(true);
            noteText.requestFocus();
        } else {
            noteTitle.setText("");
            noteText.setText("");
            noteText.setEnabled(false);
            noteTitle.setEnabled(false);
            deleteBtn.setEnabled(false);
        }
        notesListAdapter.markNote(note);
        saveBtn.setEnabled(false);
    }

    private void invokeMethod(int method){
        switch (method) {
            case METHOD_DO_NOTHING:
                break;
            case METHOD_SHOW_NEW_NOTE_DIALOG:
                showNewNoteDialog();
                break;
            case METHOD_PICK_A_NOTE:
                pickANote();
        }
    }
//    ---------------------------- Listeners, Watchers and adapters ----------------------------

    //listener for new note alert dialog.
    DialogInterface.OnClickListener onClickListenerNewNote  = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            addNewNote(editTextNewNoteTitle.getText().toString());
        }
    };

    //listener for new note alert dialog.
    DialogInterface.OnShowListener onShowListener = new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialogInterface) {
            ((AlertDialog)dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE)
                    .setEnabled(false);
        }
    };

//-------------------------------------------------------------------------------
    //listener for alert dialog that appears when you may
    //another note and u didn't save the current note
    //#positive("Yes") button.
    //* implements DialogInterface.OnClickListener interface.
    OnClickListenerWrapper onClickListenerSaveNotePos = new OnClickListenerWrapper() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
                saveNote();
                invokeMethod(this.getNextMethod());
        }
    };

    //listener for alert dialog that appears when you pick
    //another note and u didn't save the current note
    //#negative("No") button.
    //* implements DialogInterface.OnClickListener interface.
    OnClickListenerWrapper onClickListenerSaveNoteNeg = new OnClickListenerWrapper() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            invokeMethod(this.getNextMethod());
        }
    };

//-------------------------------------------------------------------------------
    DialogInterface.OnClickListener onClickListenerDeleteNotePos = new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        deleteNote();
    }
};

//-------------------------------------------------------------------------------
    //Notes list (ListView) listener.
    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            lastClickedItemPosition = i;//to inform the method pickANote() what note to pick.
            if(saveBtn.isEnabled()){
                String msg = getResources().getString(R.string.save_alert_dialog_msg_1);
                showSaveAlertDialog(msg,MainActivity.METHOD_PICK_A_NOTE);
            }else {
                pickANote();
            }
        }
    };

//-------------------------------------------------------------------------------
    //Text watcher on the input field of the title of a new note.
    //Here to prevent adding a note with an empty title.
    TextWatcher textWatcherNewNoteDialog = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if(!charSequence.toString().isEmpty()){
                dialogNewNote.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            }
            else {
                dialogNewNote.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

//-------------------------------------------------------------------------------
    //Text watcher on the text field of the current note.
    //Here to enable save button when text is changed.
    TextWatcher textWatcherNoteChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            saveBtn.setEnabled(true);
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

//---------------------------------- inner class ---------------------------------------
    private class NotesListAdapter extends BaseAdapter{

        private ArrayList<Note> notesArrayList = new ArrayList<>();
        long curMarkedNoteId= -1;
        long lastMarkedNoteId = -1;
        public NotesListAdapter(){
            notesArrayList.addAll(db.getAllNotes());
        }

        private void markNote(@Nullable Note note){
            lastMarkedNoteId = curMarkedNoteId;
            if(note!=null) {
                curMarkedNoteId = note.getId();
            }
            notifyDataSetChanged();
            }

        public void notifyDataBaseChanged(){
            notesArrayList.clear();
            notesArrayList.addAll(db.getAllNotes());
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return notesArrayList.size();
        }

        @Override
        public Object getItem(int i) {
            return notesArrayList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return notesArrayList.get(i).getId();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view==null) {
                view = (getLayoutInflater().inflate(R.layout.note_item, null))
                        .findViewById(R.id.textViewItem);
            }
            ((TextView)view).setText(notesArrayList.get(i).getTitle());
            if(getItemId(i)==curMarkedNoteId){
                view.setBackgroundColor(getResources().getColor(SKIN_COLORS[curSkin][2]));
            }else {
                view.setBackgroundResource(SKIN_RESOURCES[curSkin][0]);
            }
            return view;
        }

        @Override
        public CharSequence[] getAutofillOptions() {
            return new CharSequence[0];
        }
    }
}


