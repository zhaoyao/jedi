package jedi;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import jedi.id.IdGenerator;
import jedi.index.Index;
import jedi.index.IndexAnalyzer;
import jedi.query.Query;
import jedi.serialization.StringSerializers;
import jedi.util.Utils;
import jodd.bean.BeanUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午8:16
 */
public class Jedi implements Serializable {

	private JedisPool jedisPool;
	private IdGenerator idGenerator;

	public Jedi(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
		this.idGenerator = new IdGenerator(jedisPool);
	}

	/**
	 * Persist the giving object
	 *
	 * @param object the object to be saved
	 * @return the generated identity
	 */
	public long save(final Object object) {
		Preconditions.checkNotNull(object);

		final Map<String, String> attributeValuePairs = Utils.serialize(object);
		Number id = generateId(object, attributeValuePairs);

		final String key = buildPersistKey(object.getClass(), id);

		withTransaction(new TransactionCallback() {
			@Override
			public void execute(Transaction transaction) {
				addObjectToIndexes(transaction, object.getClass(), attributeValuePairs);
				saveObject(transaction, key, attributeValuePairs);
			}
		});

		return id.longValue();
	}

	private Number generateId(Object object, Map<String, String> attributeValuePairs) {
		Number id = idGenerator.nextIdFor(object.getClass());
		setId(id, object, attributeValuePairs);
		return id;
	}

	private void setId(Number id, Object object, Map<String, String> attributeValuePairs) {
		String idPropertyName = Utils.getIdPropertyName(object);
		attributeValuePairs.put(idPropertyName, id.toString());
		BeanUtil.setSimpleProperty(object, idPropertyName, id, true);
	}


	private void saveObject(Transaction jedis, String key, Map<String, String> attributeValuePairs) {
		jedis.hmset(key, attributeValuePairs);
	}

	private String buildPersistKey(Class<?> type, Object id) {
		return type.getName() + ":" + id;
	}

	public <T> T get(Class<T> type, long id) {
		Map<String, String> attributes = getRaw(type, id);
		if (attributes == null) {
			return null;
		}
		return Utils.deserialize(attributes, type);
	}

	private Map<String, String> getRaw(final Class<?> type, Object id) {
		final String key = buildPersistKey(type, id);
		final Map<String, String> attributeValuePairs = new HashMap<String, String>();

		withJedis(new JedisCallback() {
			@Override
			public void execute(Jedis jedis) {
				String[] names = Utils.getAttributeNames(type);
				List<String> values = jedis.hmget(key, names);

				for (int i = 0; i < names.length; i++) {
					String name = names[i];
					String value = values.get(i);
					attributeValuePairs.put(name, value);
				}
			}
		});

		if (Iterables.all(attributeValuePairs.values(), Predicates.isNull())) {
			return null;
		}

		return attributeValuePairs;
	}


	public void update(final Object object) {
		final Object id = Utils.getIdValue(object);
		if (id == null) {
			throw new JediException("Id of " + object + " is null, can not be updated");
		}

		final Class<?> type = object.getClass();

		final Map<String, String> oldAttributes = getRaw(type, id);
		final Map<String, String> newAttributes = Utils.serialize(object);

		withTransaction(new TransactionCallback() {
			@Override
			public void execute(Transaction transaction) {
				updateIndex(transaction, type, oldAttributes, newAttributes);
				updateObject(transaction, object, id);
			}
		});

	}

	private void updateIndex(Transaction transaction, Class<?> type, Map<String, String> oldAttributes, Map<String, String> newAttributes) {
		if (IndexAnalyzer.hasStaledIndex(oldAttributes, newAttributes)) {
			//从老索引中删除, 不应该包含range property引发的索引变动,
			removeObjectFromIndexes(transaction, type, oldAttributes, newAttributes);
			//添加到新索引中
			addObjectToIndexes(transaction, type, newAttributes);
		} else {
			addObjectToIndexes(transaction, type, newAttributes);
		}
	}

	private void addObjectToIndexes(Transaction transaction, Class<?> type, Map<String, String> attributes) {
		Set<Index> indexes = IndexAnalyzer.getAffectedIndexes(type, attributes);
		for (Index index : indexes) {
			index.add(transaction, attributes);
		}
	}

	private void removeObjectFromIndexes(Transaction transaction, Class<?> type, Map<String, String> oldAttributes, Map<String, String> newAttributes) {
		Set<Index> statedIndexes = IndexAnalyzer.getStaledIndexes(type, oldAttributes, newAttributes);
		for (Index index : statedIndexes) {
			index.remove(transaction, oldAttributes);
		}
	}

	private void updateObject(Transaction transaction, Object object, Object id) {
		String key = buildPersistKey(object.getClass(), id);
		String[] names = Utils.getAttributeNames(object.getClass());
		for (String name : names) {
			Object value = BeanUtil.getSimpleProperty(object, name, true);
			if (value == null) {
				transaction.hdel(key, name);
			} else {
				transaction.hset(key, name, StringSerializers.toString(value));
			}
		}
	}

	public void delete(final Class<?> type, final long id) {
		Object objectToBeDeleted = get(type, id);
		if (objectToBeDeleted == null) {
			throw new JediException("Trying to delete an unexists object " + type + ":" + id);
		}
		delete(objectToBeDeleted);
	}

	public void delete(final Object object) {
		Preconditions.checkNotNull(object);

		final Object id = Utils.getIdValue(object);
		if (id == null) {
			throw new JediException("Id of " + object + " is null, can not be deleted");
		}

		withTransaction(new TransactionCallback() {
			public void execute(Transaction transaction) {
				Class type = object.getClass();
				refreshIndexOnDelete(transaction, type, Utils.serialize(object));
				transaction.del(buildPersistKey(type, id));
			}
		});
	}

	private void refreshIndexOnDelete(Transaction transaction, Class<?> type, Map<String, String> attributes) {
		Set<Index> affectedIndexes = IndexAnalyzer.getAffectedIndexes(type, attributes);
		for (Index affectedIndex : affectedIndexes) {
			affectedIndex.remove(transaction, attributes);
		}
	}

	public void withJedis(JedisCallback callback) {
		Jedis jedis = jedisPool.getResource();
		try {
			callback.execute(jedis);
		} finally {
			jedisPool.returnResource(jedis);
		}
	}

	public void withTransaction(final TransactionCallback callback) {
		withTransaction(null, callback);
	}

	public void withTransaction(Jedis jedis, final TransactionCallback callback) {
		if (jedis == null) {
			withJedis(new JedisCallback() {
				public void execute(Jedis jedis) {
					doInTransaction(jedis, callback);
				}
			});
		} else {
			doInTransaction(jedis, callback);
		}
	}

	private void doInTransaction(Jedis jedis, TransactionCallback callback) {
		Transaction transaction = jedis.multi();
		try {
			callback.execute(transaction);
			transaction.exec();
		} catch (Throwable e) {
			transaction.discard();
			throw new JediException(e);
		}
	}

	private interface JedisCallback {
		void execute(Jedis jedis);
	}

	private interface TransactionCallback {
		void execute(Transaction transaction);
	}

	public <T> Query.QueryBuilder<T> find(Class<T> type) {
		Jedis jedis = jedisPool.getResource();
		return new Query.QueryBuilder<T>(this, jedis, type);
	}

}
