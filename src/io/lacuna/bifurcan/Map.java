package io.lacuna.bifurcan;

import io.lacuna.bifurcan.nodes.ChampNode;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

/**
 * @author ztellman
 */
public class Map<K, V> implements IMap<K, V> {

  private static final Object DEFAULT_VALUE = new Object();

  private final BiPredicate<K, K> equalsFn;
  private final ToIntFunction<K> hashFn;
  public ChampNode<K, V> root;
  public final boolean linear;

  public Map(ToIntFunction<K> hashFn, BiPredicate<K, K> equalsFn) {
    this(ChampNode.EMPTY, hashFn, equalsFn, false);
  }

  public Map() {
    this(ChampNode.EMPTY, Objects::hashCode, Objects::equals, false);
  }

  private Map(ChampNode<K, V> root, ToIntFunction<K> hashFn, BiPredicate<K, K> equalsFn, boolean linear) {
    this.root = root;
    this.hashFn = hashFn;
    this.equalsFn = equalsFn;
    this.linear = linear;
  }

  @Override
  public V get(K key, V defaultValue) {
    Object val = root.get(0, keyHash(key), key, equalsFn, DEFAULT_VALUE);

    if (val == DEFAULT_VALUE) {
      return defaultValue;
    } else {
      return (V) val;
    }
  }

  @Override
  public IMap<K, V> put(K key, V value, ValueMerger<V> merge) {
    ChampNode<K, V> rootPrime = root.put(0, this, keyHash(key), key, value, equalsFn, merge);

    if (rootPrime == root) {
      return this;
    } else if (linear) {
      root = rootPrime;
      return this;
    } else {
      return new Map<K, V>(rootPrime, hashFn, equalsFn, false);
    }
  }

  @Override
  public IMap<K, V> remove(K key) {
    ChampNode<K, V> rootPrime = root.remove(0, this, keyHash(key), key, equalsFn);

    if (rootPrime == root) {
      return this;
    } else if (linear) {
      root = rootPrime;
      return this;
    } else {
      return new Map<K, V>(rootPrime, hashFn, equalsFn, false);
    }
  }

  @Override
  public boolean contains(K key) {
    return root.get(0, keyHash(key), key, equalsFn, DEFAULT_VALUE) != DEFAULT_VALUE;
  }

  @Override
  public IList<IEntry<K, V>> entries() {
    return Lists.from(size(), i -> root.nth(i), l -> iterator());
  }

  @Override
  public ISet<K> keys() {
    IList<IEntry<K, V>> entries = entries();
    return Sets.from(
        Lists.from(size(), i -> entries.nth(i).key()),
        this::contains);
  }

  @Override
  public IMap<K, V> forked() {
    if (linear) {
      return new Map<>(root, hashFn, equalsFn, false);
    } else {
      return this;
    }
  }

  @Override
  public IMap<K, V> linear() {
    if (linear) {
      return this;
    } else {
      return new Map<>(root, hashFn, equalsFn, true);
    }
  }

  @Override
  public long size() {
    return root.size();
  }

  @Override
  public Iterator<IEntry<K, V>> iterator() {
    return root.iterator();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IMap) {
      return Maps.equals(this, (IMap<K, V>) obj);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return Maps.toString(this);
  }

  private int keyHash(K key) {
    int hash = hashFn.applyAsInt(key);

    // make sure we don't have too many collisions in the lower bits
    hash ^= (hash >>> 20) ^ (hash >>> 12);
    hash ^= (hash >>> 7) ^ (hash >>> 4);
    return hash;
  }
}
