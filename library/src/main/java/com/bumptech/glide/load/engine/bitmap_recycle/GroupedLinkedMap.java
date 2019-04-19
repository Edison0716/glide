package com.bumptech.glide.load.engine.bitmap_recycle;

import android.support.annotation.Nullable;
import com.bumptech.glide.util.Synthetic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Similar to {@link java.util.LinkedHashMap} when access ordered except that it is access ordered
 * on groups of bitmaps rather than individual objects. The idea is to be able to find the LRU
 * bitmap size, rather than the LRU bitmap object. We can then remove bitmaps from the least
 * recently used size of bitmap when we need to reduce our cache size.
 *
 * For the purposes of the LRU, we count gets for a particular size of bitmap as an access, even if
 * no bitmaps of that size are present. We do not count addition or removal of bitmaps as an
 * access.
 */

/**
 * groupedMap是GroupedLinkedMap的实例，GroupedLinkedMap内部使用了一个名为head的链表，链表的key是由bitmap size和config构成的Key，
 * value是一个由bitmap构成的链表。这样GroupedLinkedMap中的每个元素就相当于是一个组，这个组中的bitmap具有相同的size和config,
 * 对应的存储类实现就是GroupedLinkedMap中的LinkedEntry。同时，为了加快查找速度，GroupedLinkedMap中还有一个keyToEntry的Hashmap，
 * 将key和链表中的LinkedEntry对应起来。
 * 在GroupedLinkedMap的Put和get方法中，会将操作元素对应所在的LinkedEntry在head链表中往前移动，由于链表的移动成本很低，因存取效率很高。
 * ---------------------
 * 作者：大雀儿飞飞
 * 来源：CSDN
 * 原文：https://blog.csdn.net/yxz329130952/article/details/65447706
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 *
 *
 * https://www.jianshu.com/p/62b7f990ee83 分析的也挺好
 */

class GroupedLinkedMap<K extends Poolable, V> {
  private final LinkedEntry<K, V> head = new LinkedEntry<>();
  private final Map<K, LinkedEntry<K, V>> keyToEntry = new HashMap<>();

  public void put(K key, V value) {
    LinkedEntry<K, V> entry = keyToEntry.get(key);

    if (entry == null) {
      entry = new LinkedEntry<>(key);
      makeTail(entry);
      keyToEntry.put(key, entry);
    } else {
      key.offer();
    }

    entry.add(value);
  }

  @Nullable
  public V get(K key) {
    LinkedEntry<K, V> entry = keyToEntry.get(key);
    if (entry == null) {
      entry = new LinkedEntry<>(key);
      keyToEntry.put(key, entry);
    } else {
      key.offer();
    }

    makeHead(entry);

    return entry.removeLast();
  }

  @Nullable
  public V removeLast() {
    LinkedEntry<K, V> last = head.prev;

    while (!last.equals(head)) {
      V removed = last.removeLast();
      if (removed != null) {
        return removed;
      } else {
        // We will clean up empty lru entries since they are likely to have been one off or
        // unusual sizes and
        // are not likely to be requested again so the gc thrash should be minimal. Doing so will
        // speed up our
        // removeLast operation in the future and prevent our linked list from growing to
        // arbitrarily large
        // sizes.
        removeEntry(last);
        keyToEntry.remove(last.key);
        last.key.offer();
      }

      last = last.prev;
    }

    return null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GroupedLinkedMap( ");
    LinkedEntry<K, V> current = head.next;
    boolean hadAtLeastOneItem = false;
    while (!current.equals(head)) {
      hadAtLeastOneItem = true;
      sb.append('{').append(current.key).append(':').append(current.size()).append("}, ");
      current = current.next;
    }
    if (hadAtLeastOneItem) {
      sb.delete(sb.length() - 2, sb.length());
    }
    return sb.append(" )").toString();
  }

  // Make the entry the most recently used item.
  private void makeHead(LinkedEntry<K, V> entry) {
    removeEntry(entry);
    entry.prev = head;
    entry.next = head.next;
    updateEntry(entry);
  }

  // Make the entry the least recently used item.
  private void makeTail(LinkedEntry<K, V> entry) {
    removeEntry(entry);
    entry.prev = head.prev;
    entry.next = head;
    updateEntry(entry);
  }

  private static <K, V> void updateEntry(LinkedEntry<K, V> entry) {
    entry.next.prev = entry;
    entry.prev.next = entry;
  }

  private static <K, V> void removeEntry(LinkedEntry<K, V> entry) {
    entry.prev.next = entry.next;
    entry.next.prev = entry.prev;
  }

  private static class LinkedEntry<K, V> {
    @Synthetic final K key;
    private List<V> values;
    LinkedEntry<K, V> next;
    LinkedEntry<K, V> prev;

    // Used only for the first item in the list which we will treat specially and which will not
    // contain a value.
    LinkedEntry() {
      this(null);
    }

    LinkedEntry(K key) {
      next = prev = this;
      this.key = key;
    }

    @Nullable
    public V removeLast() {
      final int valueSize = size();
      return valueSize > 0 ? values.remove(valueSize - 1) : null;
    }

    public int size() {
      return values != null ? values.size() : 0;
    }

    public void add(V value) {
      if (values == null) {
        values = new ArrayList<>();
      }
      values.add(value);
    }
  }
}
