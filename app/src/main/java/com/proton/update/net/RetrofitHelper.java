package com.proton.update.net;

import android.text.TextUtils;
import android.util.Log;

import com.proton.update.BuildConfig;
import com.proton.update.component.App;
import com.proton.update.net.api.ManagerCenterApi;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitHelper {
    public static final String COMPANY = "protontekTemp";
    private static Retrofit mRetrofit;
    private static Cache cache = null;
    private static File httpCacheDirectory;
    private static ManagerCenterApi managerCenterApi;//管理中心

    private RetrofitHelper() {
    }

    private static Response addHeaderInterceptor(Interceptor.Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder()
                .addHeader("company", COMPANY)
                .addHeader("user-agent", "Android")
                .addHeader("version", App.get().getVersion())
                .addHeader("model", App.get().getSystemInfo());
        return chain.proceed(builder.build());
    }

    public static Retrofit getRetrofit() {
        return mRetrofit;
    }

    public static <T> T create(final Class<T> service) {
        if (mRetrofit == null) {
            init();
        }
        return mRetrofit.create(service);
    }

    private static void init() {
        if (httpCacheDirectory == null) {
            httpCacheDirectory = new File(App.get().getCacheDir(), "net_cache");
        }

        try {
            if (cache == null) {
                cache = new Cache(httpCacheDirectory, 10 * 1024 * 1024);
            }
        } catch (Exception e) {
            Log.e("OKHttp", "Could not create http cache", e);
        }

        //获取request
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .cache(cache)
                .addInterceptor(RetrofitHelper::addHeaderInterceptor)
                .addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
                .build();

        mRetrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SERVER_PATH)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build();

    }


    /**
     * @return 管理中心api
     */
    public static ManagerCenterApi getManagerCenterApi() {
        if (null == managerCenterApi)
            managerCenterApi = create(ManagerCenterApi.class);
        return managerCenterApi;
    }


}
