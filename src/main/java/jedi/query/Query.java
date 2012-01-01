package jedi.query;

import jedi.Jedi;
import jedi.JediException;
import jedi.index.Index;
import jedi.index.IndexAnalyzer;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: zhaoyao
 * Date: 12-1-1
 */
public class Query<T> {

	private Jedi jedi;
	private Jedis jedis;

	private List<Criteria> criterias;

	private String rangeProperty;


	private Class<T> type;

	public Query(Jedi jedi, Jedis jedis, Class<T> type, List<Criteria> criterias, String rangeProperty) {
		this.jedi = jedi;
		this.jedis = jedis;
		this.type = type;
		this.criterias = criterias;

		this.rangeProperty = rangeProperty;
	}

	/**
	 * todo 如何归还jedis?
	 *
	 * @param offset
	 * @param count
	 * @return
	 */
	public Iterator<T> iterate(int offset, int count) {
		Index index = findIndex();
		Object[] queryParams = getQueryParams();
		return new QueryResultIterator<T>(
				jedi,
				type,
				index.iterator(jedis, offset, offset + count, queryParams));
	}

	private Object[] getQueryParams() {
		Object[] queryParams = new Object[this.criterias.size()];
		for (int i = 0; i < queryParams.length; i++) {
			queryParams[i] = criterias.get(i).getValue();

		}
		return queryParams;
	}

	public Iterator<T> iterateByRange(double min, double max) {
		Index index = findIndex();
		Object[] queryParams = getQueryParams();
		return new QueryResultIterator<T>(
				jedi,
				type,
				index.rangeIterator(jedis, min, max, queryParams));
	}

	public long count() {
		Index index = findIndex();
		Object[] queryParams = getQueryParams();

		return jedis.zcard(index.getIndexName(queryParams));
	}

	private Index findIndex() {
		String[] usingProperties = new String[criterias.size()];
		for (int i = 0; i < criterias.size(); i++) {
			usingProperties[i] = criterias.get(i).getProperty();
		}
		return IndexAnalyzer.getMatchingIndex(type, rangeProperty, usingProperties);
	}


	public static class QueryBuilder<T> {
		private List<Criteria> criterias = new ArrayList<Criteria>();
		private String rangeProperty;

		private Class<T> type;
		private Jedi jedi;
		private Jedis jedis;

		private String pendingCriteriaPropertyName;


		public QueryBuilder(Jedi jedi, Jedis jedis, Class<T> type) {
			this.jedi = jedi;
			this.jedis = jedis;
			this.type = type;
		}

		public QueryBuilder<T> where(String property) {
			this.pendingCriteriaPropertyName = property;
			return this;
		}

		public QueryBuilder<T> is(Object value) {
			if (this.pendingCriteriaPropertyName == null) {
				throw new JediException("Must call where() before is()!");
			}
			this.criterias.add(new Criteria(pendingCriteriaPropertyName, value));
			this.pendingCriteriaPropertyName = null;
			return this;
		}

		public QueryBuilder<T> and(String property) {
			return where(property);
		}

		public QueryBuilder<T> andRange(String property) {
			this.rangeProperty = property;
			return this;
		}

		public Iterator<T> within(double min, double max) {
			Query<T> query = new Query<T>(jedi, jedis, type, this.criterias, this.rangeProperty);
			return query.iterateByRange(min, max);
		}


		public Iterator<T> iterate(int offset, int count) {
			Query<T> query = new Query<T>(jedi, jedis, type, this.criterias, this.rangeProperty);
			return query.iterate(offset, count);
		}


		public long count() {
			Query<T> query = new Query<T>(jedi, jedis, type, this.criterias, this.rangeProperty);
			return query.count();
		}
	}
}
