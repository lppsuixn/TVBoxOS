package com.github.tvbox.osc.ui.adapter;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.MD5;
import com.orhanobut.hawk.Hawk;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class HomeHotVodAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {

    public HomeHotVodAdapter() {
        super(R.layout.item_user_hot_vod, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video item) {
    	// takagen99: Add Delete Mode
        FrameLayout tvDel = helper.getView(R.id.delFrameLayout);
        if (HawkConfig.hotVodDelete) {
            tvDel.setVisibility(View.VISIBLE);
        } else {
            tvDel.setVisibility(View.GONE);
        }


        TextView tvRate = helper.getView(R.id.tvRate);
        if (Hawk.get(HawkConfig.HOME_REC, 0) == 2) {
            tvRate.setText(ApiConfig.get().getSource(item.sourceKey).getName());
        } else if (Hawk.get(HawkConfig.HOME_REC, 0) == 0) {
            tvRate.setText("豆瓣热播");
        } else {
            tvRate.setVisibility(View.GONE);
        }

        TextView tvNote = helper.getView(R.id.tvNote);
        if (item.note == null || item.note.isEmpty()) {
            tvNote.setVisibility(View.GONE);
        } else {
            tvNote.setText(item.note);
            tvNote.setVisibility(View.VISIBLE);
        }
        helper.setText(R.id.tvName, item.name);
        ImageView ivThumb = helper.getView(R.id.ivThumb);

        //由于部分电视机使用glide报错
        if (!TextUtils.isEmpty(item.pic)) {
            FrameLayout itemGrid = helper.getView(R.id.itemGrid);
            if (item.sourceKey != null && item.sourceKey.contains("py_bili")) {
                itemGrid.getLayoutParams().width = 580;
                itemGrid.getLayoutParams().height = 400;
            }
            Transformation transformation = new Transformation() {
                @Override
                public Bitmap transform(Bitmap source) {
                    //设置宽度固定为width，如果高度固定宽度自适应同理
                    int targetWidth = itemGrid.getWidth();
                    if(targetWidth == 0){
                        return source;
                    }
                    Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, 362, false);
                    if (result != source) {
                        //如果是同等大小的就回收
                        source.recycle();
                    }
                    return result;
                }
                @Override
                public String key() {
                    return MD5.string2MD5(item.pic + "position=" + helper.getLayoutPosition());
                }
            };
            if (item.sourceKey != null && item.sourceKey.contains("py_bili")) {
                Picasso.get()
                        .load(DefaultConfig.checkReplaceProxy(item.pic))
                        .transform(transformation)
                        .placeholder(R.drawable.img_loading_placeholder)
                        .error(R.drawable.img_loading_placeholder)
                        .into(ivThumb);
            }else {
                Picasso.get()
                        .load(DefaultConfig.checkReplaceProxy(item.pic))
                        .transform(new RoundTransformation(MD5.string2MD5(item.pic + "position=" + helper.getLayoutPosition()))
                                .centerCorp(true)
                                .override(AutoSizeUtils.mm2px(mContext, 300), AutoSizeUtils.mm2px(mContext, 400))
                                .roundRadius(AutoSizeUtils.mm2px(mContext, 10), RoundTransformation.RoundType.ALL))
                        .placeholder(R.drawable.img_loading_placeholder)
                        .error(R.drawable.img_loading_placeholder)
                        .into(ivThumb);
            }
            item.pic=item.pic.trim();
            Picasso.get()
                    .load(DefaultConfig.checkReplaceProxy(item.pic))
                    .transform(new RoundTransformation(MD5.string2MD5(item.pic))
                            .centerCorp(true)
                            .override(AutoSizeUtils.mm2px(mContext, 300), AutoSizeUtils.mm2px(mContext, 400))
                            .roundRadius(AutoSizeUtils.mm2px(mContext, 10), RoundTransformation.RoundType.ALL))
                    .placeholder(R.drawable.img_loading_placeholder)
                    .noFade()
                    .error(R.drawable.img_loading_placeholder)
                    .into(ivThumb);
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }
}