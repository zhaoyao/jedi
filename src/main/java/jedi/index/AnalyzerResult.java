package jedi.index;

import java.util.Set;

/**
 * User: zhaoyao
 * Date: 11-12-31
 * Time: 上午12:06
 */
public class AnalyzerResult {

    private Set<Index> addedTo;
    private Set<Index> deletedFrom;

    public AnalyzerResult(Set<Index> addedTo, Set<Index> deletedFrom) {
        this.addedTo = addedTo;
        this.deletedFrom = deletedFrom;
    }

    public Set<Index> getAddedTo() {
        return addedTo;
    }

    public Set<Index> getDeletedFrom() {
        return deletedFrom;
    }
	
	public boolean indexIsStaled() {
		return deletedFrom != null;
	}
}
