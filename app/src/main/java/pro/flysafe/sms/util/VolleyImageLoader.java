package pro.flysafe.sms.util;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Small Volley-backed image loader with an in-memory LRU cache.
 * This keeps the SMS companion app lightweight and avoids adding new dependencies.
 */
public final class VolleyImageLoader {
    private static ImageLoader imageLoader;

    private VolleyImageLoader() {
        // No instances.
    }

    public static ImageLoader get(@NonNull Context context) {
        if (imageLoader == null) {
            // Use application context to avoid leaking activities or fragments.
            Context appContext = context.getApplicationContext();
            RequestQueue queue = Volley.newRequestQueue(appContext);
            ImageLoader.ImageCache cache = new ImageLoader.ImageCache() {
                private final LruCache<String, Bitmap> lru = new LruCache<>(40);

                @Override
                public Bitmap getBitmap(String url) {
                    return lru.get(url);
                }

                @Override
                public void putBitmap(String url, Bitmap bitmap) {
                    lru.put(url, bitmap);
                }
            };
            imageLoader = new ImageLoader(queue, cache);
        }
        return imageLoader;
    }
}
