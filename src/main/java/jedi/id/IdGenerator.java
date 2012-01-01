package jedi.id;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午8:19
 */
public class IdGenerator {


	private JedisPool jedisPool;

	private ConcurrentMap<String, AtomicLong> cachedIds = new ConcurrentHashMap<String, AtomicLong>();
	private ConcurrentMap<String, Object> seqLocks = new ConcurrentHashMap<String, Object>();

	public static final int DEFAULT_BATCH_SIZE = 100;
	private int batchSize = DEFAULT_BATCH_SIZE;

	public IdGenerator(JedisPool jedisPool) {
		this(jedisPool, DEFAULT_BATCH_SIZE);
	}

	public IdGenerator(JedisPool jedisPool, int batchSize) {
		this.jedisPool = jedisPool;
		if (batchSize > 0) {
			this.batchSize = batchSize;
		}
	}

	public Number nextIdFor(Class<?> cls) {

		String idSequenceName = getIdSequenceName(cls);
		AtomicLong cached = cachedIds.get(idSequenceName);

		long result;
		if (cached != null && cached.get() % batchSize > 0) {
			result = cached.incrementAndGet();
		} else {
			seqLocks.putIfAbsent(idSequenceName, new Object());
			Object lock = seqLocks.get(idSequenceName);
			synchronized (lock) {
				Jedis jedis = jedisPool.getResource();
				try {
					result = jedis.incrBy(idSequenceName, batchSize);
					cachedIds.putIfAbsent(idSequenceName, new AtomicLong(0));
					cachedIds.get(idSequenceName).set(result);
				} finally {
					jedisPool.returnResource(jedis);
				}
			}
		}

		return result;
	}

	public static String getIdSequenceName(Class<?> cls) {
		return "seq:" + cls.getName();
	}


}
