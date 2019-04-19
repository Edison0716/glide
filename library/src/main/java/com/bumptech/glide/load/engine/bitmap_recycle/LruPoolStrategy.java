package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

/**
 * The interface Lru pool strategy.
 */
interface LruPoolStrategy {
  /**
   * 增加一个缓存
   *
   * @param bitmap the bitmap
   */
  void put(Bitmap bitmap);

  /**
   * 从缓存中获取指定尺寸和配置的图片
   *
   * @param width  the width
   * @param height the height
   * @param config the config
   * @return the bitmap
   */
  @Nullable
  Bitmap get(int width, int height, Bitmap.Config config);

  /**
   * 删除一个最不常用的 在Bitmap size > 可用bitmapPoolSize
   *
   * @return the bitmap
   */
  @Nullable
  Bitmap removeLast();

  /**
   * Log bitmap string.
   *
   * @param bitmap the bitmap
   * @return the string
   */
  String logBitmap(Bitmap bitmap);

  /**
   * Log bitmap string.
   *
   * @param width  the width
   * @param height the height
   * @param config the config
   * @return the string
   */
  String logBitmap(int width, int height, Bitmap.Config config);

  /**
   * 获取图片占用的空间
   *
   * @param bitmap the bitmap
   * @return the size
   */
  int getSize(Bitmap bitmap);
}
