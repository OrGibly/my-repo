package com.example.orgibly.myappnotes;

public class Note {
    //The note should get id from the database.
    private int id = -1;
    private String title = "";
    private String text = "";
    //for view purposes.
    private boolean isMarked = false;

    public Note(String title){
        this.title = title;
    }

    /**
     * Can use this method only once per a note.
     * @param id The id given to this note.
     */
    public void setId(int id){
        if(this.id==-1)this.id=id;}

    public void setTitle(String title) {this.title=title;}

    public void setText(String text){this.text=text;}

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String toString(){return this.title;}
}
