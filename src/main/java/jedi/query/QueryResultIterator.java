package jedi.query;

import com.google.common.collect.AbstractIterator;
import jedi.Jedi;

import java.util.Iterator;

/**
 * User: zhaoyao
 * Date: 12-1-1
 */
public class QueryResultIterator<T> extends AbstractIterator<T> {

	private Jedi jedi;
	private Class<T> type;
	private Iterator<Long> idIterator;

	public QueryResultIterator(Jedi jedi, Class<T> type, Iterator<Long> idIterator) {
		this.jedi = jedi;
		this.type = type;
		this.idIterator = idIterator;
	}

	@Override
	protected T computeNext() {
		if (!idIterator.hasNext()) {
			endOfData();
			return null;
		}

		return jedi.get(type, idIterator.next());
	}
}
