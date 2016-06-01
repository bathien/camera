package com.exercise.AndroidCamera;

import com.exercise.AndroidCamera.R;
import com.exercise.AndroidCamera.R.drawable;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

public class FlashButton extends ImageButton{

    public enum FlashEnum {
        AUTOMATIC, ON, OFF
    }

    public interface FlashListener {
        void onAutomatic();
        void onOn();
        void onOff();
    }

    private FlashEnum mState;
    private FlashListener mFlashListener;

    public FlashButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int next = ((mState.ordinal() + 1) % FlashEnum.values().length);
                setState(FlashEnum.values()[next]);
                performFlashClick();
            }
        });
        //Sets initial state
        setState(FlashEnum.AUTOMATIC);
    }

    private void performFlashClick() {
        if(mFlashListener == null)return;
       switch (mState) {
            case AUTOMATIC:
            	mFlashListener.onAutomatic();
                
                break;
            case ON:
                mFlashListener.onOn();
                break;
            case OFF:
                mFlashListener.onOff();
                break;
        }
        
    }

    private void createDrawableState() {
        switch (mState) {
            case AUTOMATIC:
            	        	  
                setImageResource(R.drawable.ic_flash_auto);
                break;
            case ON:
            
                setImageResource(R.drawable.ic_flash_on);
                break;
            case OFF:
                setImageResource(R.drawable.ic_flash_off);
                break;
        }
    }


    public FlashEnum getState() {
        return mState;
    }

    public void setState(FlashEnum state) {
        if(state == null)return;
        this.mState = state;
        createDrawableState();

    }

    public FlashListener getFlashListener() {
        return mFlashListener;
    }

    public void setFlashListener(FlashListener flashListener) {
        this.mFlashListener = flashListener;
    }

}