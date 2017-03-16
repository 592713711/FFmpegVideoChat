package com.nercms.util;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;


import com.nercms.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zzc on 2015/12/24.
 */
public class SelectHeadDialog extends DialogFragment {
    GridView gridView;
    SimpleAdapter adapter;
    View view;
    ArrayList<Map<String,Object>> data;
    public int selectposition=-1;
    ImageView oldView=null;
    public Button sure_btn;
    OnClickReturn onclickreturn;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_selecthead, null);
        initView();

        Dialog dialog = new Dialog(getActivity(), R.style.dialog);
        dialog.setContentView(view);

        return dialog;
    }

    private void initView() {
        gridView= (GridView) view.findViewById(R.id.gridView);
        initData();
        String[] from={"image"};
        int[] to={R.id.image_item};
        adapter=new SimpleAdapter(getContext(),data,R.layout.select_head_item,from,to);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectposition = position;
                if (oldView != null)
                    oldView.setImageResource(R.drawable.picture_unselected);
                ImageView imageView = (ImageView) view.findViewById(R.id.image_select);
                imageView.setImageResource(R.drawable.pictures_selected);
                oldView = imageView;
            }
        });

        sure_btn= (Button) view.findViewById(R.id.sure_btn);
        sure_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onclickreturn.onClick(selectposition,Util.getHeadId(selectposition));
                   // head_id = getHeadId(selectHeadDialog.selectposition);
                   // head_btn.setImageResource(head_id);

                //selectHeadDialog.dismiss();
            }
        });
    }

    public void setOnClickReturn(OnClickReturn onclickreturn){
        this.onclickreturn=onclickreturn;
    }

    public void initData(){
        data=new ArrayList<>();
        HashMap<String,Object> map1=new HashMap();
        map1.put("image",R.drawable.h1);
        HashMap<String,Object> map2=new HashMap();
        map2.put("image",R.drawable.h2);
        HashMap<String,Object> map3=new HashMap();
        map3.put("image",R.drawable.h3);
        HashMap<String,Object> map4=new HashMap();
        map4.put("image",R.drawable.h4);
        HashMap<String,Object> map5=new HashMap();
        map5.put("image",R.drawable.h5);
        HashMap<String,Object> map6=new HashMap();
        map6.put("image",R.drawable.h6);
        HashMap<String,Object> map7=new HashMap();
        map7.put("image",R.drawable.h7);
        HashMap<String,Object> map8=new HashMap();
        map8.put("image",R.drawable.h8);
        HashMap<String,Object> map9=new HashMap();
        map9.put("image",R.drawable.h9);
        HashMap<String,Object> map10=new HashMap();
        map10.put("image",R.drawable.h10);

        data.add(map1);
        data.add(map2);
        data.add(map3);
        data.add(map4);
        data.add(map5);
        data.add(map6);
        data.add(map7);
        data.add(map8);
        data.add(map9);
        data.add(map10);

    }

    public interface OnClickReturn{
        public void onClick(int icon,int icon_id);
    }


}
