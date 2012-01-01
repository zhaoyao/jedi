package jedi;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: zhaoyao
 * Date: 11-12-31
 */
public class BaseTest extends Assert {

	protected JedisPool jedisPool = new JedisPool("localhost", 6379);
	protected Jedi jedi = new Jedi(jedisPool);

	protected Jedis getJedis() {
		return jedisPool.getResource();
	}

	@Before
	public void cleanup() {
		getJedis().flushAll();
	}

	protected Matcher<Set<String>> isEmpty() {
		return new IsEmpty();
	}

	protected Matcher<Set<String>> equalToIndex(long... ids) {
		return new EqualToIndex(ids);
	}

	protected Matcher<Object> isNull() {
		return new BaseMatcher<Object>() {
			@Override
			public boolean matches(Object o) {
				return o == null;
			}

			@Override
			public void describeTo(Description description) {
			}
		};
	}

	protected Matcher<Object> isNotNull() {
		return new BaseMatcher<Object>() {
			@Override
			public boolean matches(Object o) {
				return o != null;
			}

			@Override
			public void describeTo(Description description) {
			}
		};
	}

	protected static class EqualToIndex extends BaseMatcher<Set<String>> {

		private Set<String> ids;

		public EqualToIndex(long... ids) {
			this.ids = new HashSet<String>();
			for (long id : ids) {
				this.ids.add(String.valueOf(id));
			}
		}

		@Override
		public boolean matches(Object o) {
			if (o instanceof Set) {
				Set set = (Set) o;
				return set.equals(ids);
			}

			return false;
		}

		@Override
		public void describeTo(Description description) {
		}
	}

	protected static class IsEmpty extends BaseMatcher<Set<String>> {

		@Override
		public boolean matches(Object o) {

			if (o instanceof Collection) {
				return ((Collection) o).isEmpty();
			}

			if (o instanceof Map) {
				return ((Map) o).isEmpty();
			}
			return false;
		}

		@Override
		public void describeTo(Description description) {
		}
	}
}
