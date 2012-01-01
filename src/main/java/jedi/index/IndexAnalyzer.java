package jedi.index;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import jedi.JediException;
import jedi.annotation.Indexes;
import jodd.util.ArraysUtil;
import jodd.util.StringUtil;

import java.util.*;

/**
 * todo 预生成Index, 报告非法索引: andRange 包含于 indexedProperty中, sortedBy与 indexed均为空
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午10:02
 */
public class IndexAnalyzer {

	/**
	 * 根据新对象和老对象属性值间的不同, 分析出
	 * 1. 哪些索引需要被更新
	 * 2. 哪些索引需要删除
	 *
	 * @param oldAttributeValuePairs
	 * @param newAttributeValuePairs
	 * @return
	 */
	public static boolean hasStaledIndex(Map<String, String> oldAttributeValuePairs,
	                                     Map<String, String> newAttributeValuePairs) {
		return !Maps.difference(oldAttributeValuePairs, newAttributeValuePairs).areEqual();
	}

	public static Set<Index> getStaledIndexes(Class<?> type, Map<String, String> oldAttributeValuePairs,
	                                          Map<String, String> newAttributeValuePairs) {
		//没有任何更新
		Set<Index> result = new HashSet<Index>();
		MapDifference<String, String> difference = Maps.difference(oldAttributeValuePairs, newAttributeValuePairs);
		for (String differingProperty : difference.entriesDiffering().keySet()) {
			Set<Index> relatedIndexes = getRelatedIndexes(type, differingProperty);
			result.addAll(relatedIndexes);
		}

		return result;
	}

	/**
	 * 哪些索引会收录这些属性对?
	 * @param type
	 * @param attributeValuePairs
	 * @return
	 */
	public static Set<Index> getAffectedIndexes(Class<?> type, Map<String, String> attributeValuePairs) {

		jedi.annotation.Index[] indexDeclarations = getIndexDeclarations(type);

		Set<Index> result = new HashSet<Index>(indexDeclarations.length);
		for (jedi.annotation.Index indexDeclaration : indexDeclarations) {
			String[] indexedProperties = getIndexedPropertyNames(indexDeclaration);

			boolean match = true;
			for (String property : indexedProperties) {
				if (attributeValuePairs.get(property) == null) {
					//索引字段的某项为空,
					match = false;
					break;
				}
			}
			if (!match) {
				continue;
			}

			result.add(buildIndex(type, indexDeclaration));
		}

		//todo cache result
		return result;
	}

	/**
	 * 该属性的变动, 会引发哪些索引失效?
	 * @param type
	 * @param property
	 * @return
	 */
	public static Set<Index> getRelatedIndexes(Class<?> type, String property) {

		jedi.annotation.Index[] indexDeclarations = getIndexDeclarations(type);
		Set<Index> result = new HashSet<Index>(indexDeclarations.length);
		for (jedi.annotation.Index indexDeclaration : indexDeclarations) {
			String[] indexedProperties = getIndexedPropertyNames(indexDeclaration);
			if (ArraysUtil.contains(indexedProperties, property)) {
				result.add(buildIndex(type, indexDeclaration));
			}
		}

		return result;
	}

	/**
	 * 根据这些属性查询, 应该走哪个索引?
	 * 1. 查询属性必须与索引属性完全相同
	 * 2. 如果指定了range, 那么range也必须相同
	 * @param type
	 * @param rangeProperty
	 * @param properties
	 * @return
	 */
	public static Index getMatchingIndex(Class<?> type, String rangeProperty, String... properties) {

		jedi.annotation.Index[] indexDeclarations = getIndexDeclarations(type);

		for (jedi.annotation.Index indexDeclaration : indexDeclarations) {
			String[] indexedProperties = getIndexedPropertyNames(indexDeclaration);
			Arrays.sort(properties);
			Arrays.sort(indexedProperties);
			if (Arrays.equals(indexedProperties, properties)) {
				//compare andRange property if specified
				if (rangeProperty != null) {
					if (!rangeProperty.equals(indexDeclaration.range())) {
						continue;
					}
				}
				return buildIndex(type, indexDeclaration);
			}
		}

		throw new JediException("No matching index found for query: " + Arrays.toString(properties));
	}

	private static String[] getIndexedPropertyNames(jedi.annotation.Index indexDeclaration) {
		String[] indexedProperties = indexDeclaration.on();
		if (indexedProperties.length == 1 && indexedProperties[0].equals("")) {
			indexedProperties = new String[0];
		}
		//on = "p1, p2" style declaration
		if (indexedProperties.length == 1 && indexedProperties[0].contains(",")) {
			indexedProperties = StringUtil.split(indexedProperties[0], ",");
			List<String> list = new ArrayList<String>(indexedProperties.length);
			for (String indexedProperty : indexedProperties) {
				indexedProperty = indexedProperty.trim();
				if (StringUtil.isNotBlank(indexedProperty)) {
					list.add(indexedProperty.trim());
				}
			}
			indexedProperties = list.toArray(new String[list.size()]);
		}
		return indexedProperties;
	}



	private static jedi.annotation.Index[] getIndexDeclarations(Class<?> type) {
		Indexes indexes = type.getAnnotation(Indexes.class);
		jedi.annotation.Index[] indexDeclarations;
		if (indexes != null) {
			indexDeclarations = indexes.value();
		} else {
			jedi.annotation.Index indexDeclaration = type.getAnnotation(jedi.annotation.Index.class);
			if (indexDeclaration != null) {
				indexDeclarations = new jedi.annotation.Index[]{indexDeclaration};
			} else {
				indexDeclarations = new jedi.annotation.Index[0];
			}
		}
		return indexDeclarations;
	}

	private static Index buildIndex(Class<?> type, jedi.annotation.Index indexDeclaration) {
		return new Index(type,
				getIndexedPropertyNames(indexDeclaration),
				getRangePropertyName(indexDeclaration),
				indexDeclaration.rangeNullValue());
	}

	private static String getRangePropertyName(jedi.annotation.Index indexDeclaration) {
		String propertyName = indexDeclaration.range();
		return propertyName.equals("") ? null : propertyName;
	}

}
