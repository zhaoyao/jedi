package jedi.index;

import com.google.common.base.Joiner;
import jedi.util.Utils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Index~
 *
 * 索引属性分为两种: 固定值和range值
 *
 * <p/>
 * 格式: fcn:propertyNames:values
 * <p/>
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午9:56
 */
public class Index {

	private Class<?> type;
	private String[] indexedPropertyNames;
	private String rangePropertyName;
	private double rangeNullValue;

	public String getRangePropertyName() {
		return rangePropertyName;
	}

	public Index(Class<?> type, String[] indexedPropertyNames, String range, double rangeNullValue) {
		this.type = type;
		this.indexedPropertyNames = indexedPropertyNames;
		this.rangePropertyName = range;
		this.rangeNullValue = rangeNullValue;
	}

	public void add(Transaction jedis, Map<String, String> attributeValuePairs) {

		Double rangePropertyValue = getRangePropertyValue(attributeValuePairs);

		String indexName = getIndexName(attributeValuePairs);

		jedis.zadd(indexName, rangePropertyValue, attributeValuePairs.get("id"));
	}

	private Object[] getIndexedPropertyValues(Map<String, String> attributeValuePairs) {
		Object[] indexedPropertyValues = new Object[this.indexedPropertyNames.length];
		for (int i = 0; i < indexedPropertyNames.length; i++) {
			String propertyName = indexedPropertyNames[i];
			String val = attributeValuePairs.get(propertyName);
			if (val == null) {
				throw new IllegalStateException("Can not add index[" + Joiner.on(":").join(indexedPropertyNames) + "] with null property: " + propertyName);
			}

			indexedPropertyValues[i] = val;
		}

		return indexedPropertyValues;
	}

	private boolean hasRangeProperty() {
		return rangePropertyName != null;
	}

	/**
	 * 获取用于构建索引时使用的score value, 指定了sortedBy属性的索引, 使用相应的属性值
	 * 未指定的, 使用epoch值
	 *
	 * @param attributes
	 * @return
	 */
	private Double getRangePropertyValue(Map<String, String> attributes) {
		Double rangePropertyValue;
		if (hasRangeProperty()) {
			String rangeStringValue = attributes.get(this.rangePropertyName);
			//andRange property 必须是not null的?
			if (rangeStringValue == null) {
				return this.rangeNullValue;
			}
			rangePropertyValue = parseRangeValue(rangeStringValue);
		} else {

			//未指定, 默认使用id
			rangePropertyValue = parseRangeValue(attributes.get(Utils.getIdPropertyName(type)));
		}

		return rangePropertyValue;
	}

	private Double parseRangeValue(String rangePropertyValue) {
		Double rangeValue;
		try {
			rangeValue = Double.parseDouble(rangePropertyValue);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Range property used by index " + Joiner.on(":").join(indexedPropertyNames) + " must be a double value", e);
		}
		return rangeValue;
	}

	protected String getIndexName(Map<String, String> attributeValuePairs) {
		return getIndexName(getIndexedPropertyValues(attributeValuePairs));
	}

	public String getIndexName(Object[] indexedPropertyValues) {
		String propertyNamesString = Joiner.on(":").join(indexedPropertyNames);
		if (rangePropertyName != null) {
			propertyNamesString += ":" + rangePropertyName;
		}
		return Joiner.on(":").join(
				type.getName(),
				propertyNamesString,
				(Object[]) Utils.stringrify(indexedPropertyValues));
	}

	public void remove(Transaction transaction, Map<String, String> attributeValuePairs) {
		String indexName = getIndexName(attributeValuePairs);
		String id = attributeValuePairs.get("id");
		transaction.zrem(indexName, id);
	}

	public Iterator<Long> iterator(Jedis jedis, int start, int end, Object... params) {
		if (params.length != indexedPropertyNames.length) {
			throw new IllegalArgumentException(
					"Can not iterate index: " + Joiner.on(":").join(indexedPropertyNames) + ",  passed in " + Arrays.toString(params));
		}

		Set<String> ids = jedis.zrange(getIndexName(params), start, end);

		final Iterator<String> stringIdIterator = ids.iterator();
		return new Iterator<Long>() {
			@Override
			public boolean hasNext() {
				return stringIdIterator.hasNext();
			}

			@Override
			public Long next() {
				return Long.parseLong(stringIdIterator.next());
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public Iterator<Long> rangeIterator(Jedis jedis, double min, double max, Object... params) {
		if (params.length != indexedPropertyNames.length) {
			throw new IllegalArgumentException(
					"Can not iterate index: " + Joiner.on(":").join(indexedPropertyNames) + ",  passed in " + Arrays.toString(params));
		}

		Set<String> ids = jedis.zrangeByScore(getIndexName(params), min, max);

		final Iterator<String> stringIdIterator = ids.iterator();
		return new Iterator<Long>() {
			@Override
			public boolean hasNext() {
				return stringIdIterator.hasNext();
			}

			@Override
			public Long next() {
				return Long.parseLong(stringIdIterator.next());
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}


	public int count(Jedis jedis, Object... params) {
		return jedis.zcount(getIndexName(params), Double.MIN_VALUE, Double.MAX_VALUE).intValue();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Index index = (Index) o;

		if (!Arrays.equals(indexedPropertyNames, index.indexedPropertyNames)) return false;
		if (this.rangePropertyName != null ? !this.rangePropertyName.equals(index.rangePropertyName) : index.rangePropertyName != null)
			return false;
		if (type != null ? !type.equals(index.type) : index.type != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = type != null ? type.hashCode() : 0;
		result = 31 * result + (indexedPropertyNames != null ? Arrays.hashCode(indexedPropertyNames) : 0);
		result = 31 * result + (this.rangePropertyName != null ? this.rangePropertyName.hashCode() : 0);
		return result;
	}
}
