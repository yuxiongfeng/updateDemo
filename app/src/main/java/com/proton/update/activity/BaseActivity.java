package com.proton.update.activity;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.proton.update.R;
import com.proton.update.utils.StatusBarUtil;
import com.proton.update.utils.Utils;
import com.wms.logger.Logger;

/**
 * Created by yuxiongfeng.
 * Date: 2019/7/18
 */
public abstract class BaseActivity <DB extends ViewDataBinding> extends AppCompatActivity {
    protected DB binding;
    protected Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        init();
        initView();
        Utils.setStatusBarTextColor(this, isDarkIcon());
        setStatusBar();
        initData();
        long startTime = System.currentTimeMillis();
        int layoutID = inflateContentView();
        if (layoutID != 0) {
            binding = DataBindingUtil.setContentView(this, layoutID);
        }
        Logger.w("耗时:" + (System.currentTimeMillis() - startTime) + "," + this.getClass().getSimpleName());

        setTopTextView();
    }

    protected void setStatusBar() {
        StatusBarUtil.setStatusBarDrawable(this, R.drawable.drawable_status_bar);
        initToolbar();
    }
    protected void initToolbar() {
        //透明状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setStatusBarColor();
        if (null != findViewById(R.id.toolbar)) {
            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setTitle("");
            if (getBackIcon() != -1) {
                toolbar.setNavigationIcon(getBackIcon());
            }
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(showBackBtn());
            getSupportActionBar().setHomeButtonEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    protected int getBackIcon() {
        return R.drawable.btn_back_img;
    }

    protected boolean showBackBtn() {
        return true;
    }

    protected void setStatusBarColor() {
        StatusBarUtil.setColor(this, getResources().getColor(R.color.white));
    }

    protected boolean isDarkIcon() {
        return false;
    }

    protected void init() {

    }

    protected void initView() {

    }
    protected void  initData(){

    }

    /**
     * 为activity设置布局
     *
     * @return layout布局
     */
    abstract protected int inflateContentView();

    protected void showToast(String msg){
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    protected void setTopTextView() {
        TextView titleText = findViewById(R.id.title);
        if (titleText != null && !TextUtils.isEmpty(getTopCenterText())) {
            titleText.setText(getTopCenterText());
        }
    }


    /**
     * 标题
     */
    public String getTopCenterText() {
        return "";
    }

}
