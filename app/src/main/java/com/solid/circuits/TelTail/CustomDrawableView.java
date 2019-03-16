/*
	Copyright 2019 Matthew Sauve	mattsauve@solidcircuits.net

	This file is part of the TTL android app.

	The TTL android app is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The TTL android app is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.solid.circuits.TelTail;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class CustomDrawableView extends View {
    public ShapeDrawable mBase;
    public ShapeDrawable mJoy;
    public ShapeDrawable mBackground;

    private int x = getScreenWidth()/2;
    private int y = getScreenHeight()/2;
    private int joyWidth = 200;
    private int joyHeight = 200;
    private int baseWidth = 250;
    private int baseHeight = 250;

    private int touchX = 0;
    private int touchY = 0;
    private int touchStartX = 0;
    private int touchStartY = 0;

    public CustomDrawableView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mBackground = new ShapeDrawable(new RectShape());
        mBackground.getPaint().setColor(0xFF2F2F2F);
        mBackground.setBounds(0, 0, 1100, 1700);
        mJoy = new ShapeDrawable(new OvalShape());
        mJoy.getPaint().setColor(0xff00D89E);
        mJoy.setAlpha(0x00);
        mJoy.setBounds(x, y, x + joyWidth, y + joyHeight);
        mBase = new ShapeDrawable(new OvalShape());
        mBase.getPaint().setColor(0xffffffff);
        mBase.setAlpha(0x00);
        mBase.setBounds(x, y, x + baseWidth, y + baseHeight);
    }

    protected void onDraw(Canvas canvas) {
        mBackground.draw(canvas);
        mBase.draw(canvas);
        mJoy.draw(canvas);
    }

    public static int getScreenWidth() {
        int temp = (int)(Resources.getSystem().getDisplayMetrics().widthPixels);
        return temp;
    }

    public static int getScreenHeight() {
        int temp = (int)(Resources.getSystem().getDisplayMetrics().heightPixels);
        return temp;
    }

    public boolean handleTouch(MotionEvent event) {
        final int action = event.getAction();
        int offBaseX = 110;
        int offBaseY = 370;
        int offJoyX = 85;
        int offJoyY = 345;
        int joyLimit = 325;
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP: {
                mJoy.setAlpha(0x00);
                mBase.setAlpha(0x00);
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                //if (remote_connected == 0) {
                touchStartX = ((int) event.getX());
                touchStartY = ((int) event.getY());
                mBase.setBounds(touchStartX - offBaseX, touchStartY - offBaseY, touchStartX + baseWidth - offBaseX,
                        touchStartY + baseHeight - offBaseY);
                mJoy.setBounds(touchStartX - offJoyX, touchStartY - offJoyY, touchStartX + joyWidth - offJoyX,
                        touchStartY + joyHeight - offJoyY);
                //Log.d("coord", "" + touchStartX + "  " + touchStartY);
                mBase.setAlpha(0xFF);
                //} else
                //Toast.makeText(RemoteActivity.this, "        Nunchuck connected\nDisconnect to use this remote", Toast.LENGTH_LONG).show();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                //if (remote_connected == 0) {
                //touchX = ((int) event.getX());
                touchX = touchStartX;
                touchY = ((int) event.getY());

/*                    if ((touchY - touchStartY) < 0 && (touchX - touchStartX) >= 0) {
                        if (touchX >= (Math.cos(Math.atan2(-(touchY - touchStartY), touchX - touchStartX)) * joyLimit) + touchStartX)
                            touchX = (int) (Math.cos(Math.atan2(-(touchY - touchStartY), touchX - touchStartX)) * joyLimit) + touchStartX;

                        if (touchY <= (Math.sin(Math.atan2(-(touchY - touchStartY), touchX - touchStartX)) * -joyLimit) + touchStartY)
                            touchY = (int) (Math.sin(Math.atan2(-(touchY - touchStartY), touchX - touchStartX)) * -joyLimit) + touchStartY;
                    }
                    if ((touchY - touchStartY) >= 0 && (touchX - touchStartX) < 0) {
                        if (touchX <= (Math.cos(Math.atan2(touchY - touchStartY, -(touchX - touchStartX))) * -joyLimit) + touchStartX)
                            touchX = (int) (Math.cos(Math.atan2(touchY - touchStartY, -(touchX - touchStartX))) * -joyLimit) + touchStartX;

                        if (touchY >= (Math.sin(Math.atan2(touchY - touchStartY, -(touchX - touchStartX))) * joyLimit) + touchStartY)
                            touchY = (int) (Math.sin(Math.atan2(touchY - touchStartY, -(touchX - touchStartX))) * joyLimit) + touchStartY;
                    }
                    if ((touchY - touchStartY) < 0 && (touchX - touchStartX) < 0) {
                        if (touchX <= (Math.cos(Math.atan2(-(touchY - touchStartY), -(touchX - touchStartX))) * -joyLimit) + touchStartX)
                            touchX = (int) (Math.cos(Math.atan2(-(touchY - touchStartY), -(touchX - touchStartX))) * -joyLimit) + touchStartX;

                        if (touchY <= (Math.sin(Math.atan2(-(touchY - touchStartY), -(touchX - touchStartX))) * -joyLimit) + touchStartY)
                            touchY = (int) (Math.sin(Math.atan2(-(touchY - touchStartY), -(touchX - touchStartX))) * -joyLimit) + touchStartY;
                    }
                    if ((touchY - touchStartY) >= 0 && (touchX - touchStartX) >= 0) {
                        if (touchX >= (Math.cos(Math.atan2(touchY - touchStartY, touchX - touchStartX)) * joyLimit) + touchStartX)
                            touchX = (int) (Math.cos(Math.atan2(touchY - touchStartY, touchX - touchStartX)) * joyLimit) + touchStartX;

                        if (touchY >= (Math.sin(Math.atan2(touchY - touchStartY, touchX - touchStartX)) * joyLimit) + touchStartY)
                            touchY = (int) (Math.sin(Math.atan2(touchY - touchStartY, touchX - touchStartX)) * joyLimit) + touchStartY;
                    }*/

                if(touchY > joyLimit + touchStartY)
                    touchY = joyLimit + touchStartY;
                else if(touchY < -joyLimit + touchStartY)
                    touchY = -joyLimit + touchStartY;

                mJoy.setBounds(touchX - offJoyX, touchY - offJoyY, touchX + joyWidth - offJoyX,
                        touchY + joyHeight - offJoyY);

                //Log.d("coord", "" + touchX + "  " + touchY);
                mJoy.setAlpha(0xFF);
                //}
            }
            break;
        }

        invalidate();
        return true;
    }
}


