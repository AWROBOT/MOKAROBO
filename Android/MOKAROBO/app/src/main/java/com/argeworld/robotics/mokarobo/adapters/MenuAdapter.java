package com.argeworld.robotics.mokarobo.adapters;

import com.argeworld.robotics.mokarobo.R;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MenuAdapter extends ArrayAdapter<String>
{
	Context context; 
    LayoutInflater inflater;
    int layoutResourceId;
    
    public ListView shelfList;
    
    public MenuAdapter(Context context, int layoutResourceId, String[] items) 
    {
        super(context, layoutResourceId, items);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
    }

	@Override
    public View getView(int position, View convertView, ViewGroup parent)
	{
		View retval = LayoutInflater.from(getContext()).inflate(layoutResourceId, null);
		
		TextView titleText = (TextView) retval.findViewById(R.id.title);
		
		titleText.setText(getItem(position));

		return retval;
    }
}
