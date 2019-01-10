package com.example.orgibly.myappnotes.DialogInterface;

import android.content.DialogInterface;

/**
 * Created by OrGibly on 6.2.2018.
 */

public abstract class OnClickListenerWrapper implements DialogInterface.OnClickListener{
    private int nextMethod = 0;
    public void setNextMethod(int nextMethod){this.nextMethod = nextMethod;}
    public int getNextMethod(){return this.nextMethod;}
}
