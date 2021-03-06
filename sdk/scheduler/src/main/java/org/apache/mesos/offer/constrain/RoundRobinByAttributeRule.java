package org.apache.mesos.offer.constrain;

import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.mesos.Protos.Attribute;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.offer.AttributeStringUtils;
import org.apache.mesos.offer.TaskUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Implements logic for Marathon's GROUP_BY operator for attribute values. Ensures that tasks are
 * evenly distributed across distinct attribute values in the system as they are rolled out.
 *
 * Example:
 *  attribute |     tasks
 * -----------+---------------
 *    rack:1  | a-1, b-1, c-1
 *    rack:2  | a-2, c-2, c-3
 *    rack:3  | b-2, c-4
 * Result:
 *  allow rack:3 only, unless we know that there's >3 racks via the attribute_count parameter
 *
 * Example:
 *  attribute |     tasks
 * -----------+---------------
 *    rack:1  | a-1, b-1, c-1
 *    rack:2  | a-2, c-2, c-3
 *    rack:3  | b-2, c-4, b-3
 * Result:
 *  allow any of rack:1/rack:2/rack:3, unless we know that there's >3 racks via the attribute_count
 *  parameter.
 */
public class RoundRobinByAttributeRule extends RoundRobinBaseRule {

    private final String attributeName;

    public RoundRobinByAttributeRule(String attribute, Optional<Integer> attributeValueCount) {
        this(attribute, attributeValueCount, null);
    }

    @JsonCreator
    public RoundRobinByAttributeRule(
            @JsonProperty("name") String attributeName,
            @JsonProperty("value_count") Optional<Integer> attributeValueCount,
            @JsonProperty("task_filter") StringMatcher taskFilter) {
        super(taskFilter, attributeValueCount);
        this.attributeName = attributeName;
    }

    @Override
    protected String getValue(Offer offer) {
        for (Attribute attribute : offer.getAttributesList()) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                return AttributeStringUtils.valueString(attribute);
            }
        }
        return null;
    }

    @Override
    protected String getValue(TaskInfo task) {
        for (String taskAttributeString : TaskUtils.getOfferAttributeStrings(task)) {
            AttributeStringUtils.NameValue taskAttributeNameValue =
                    AttributeStringUtils.split(taskAttributeString);
            if (taskAttributeNameValue.name.equalsIgnoreCase(attributeName)) {
                return taskAttributeNameValue.value;
            }
        }
        return null;
    }

    @JsonProperty("name")
    private String getAttributeName() {
        return attributeName;
    }

    @JsonProperty("value_count")
    private Optional<Integer> getAttributeValueCount() {
        return distinctValueCount;
    }

    @JsonProperty("task_filter")
    private StringMatcher getTaskFilter() {
        return taskFilter;
    }

    @Override
    public String toString() {
        return String.format("RoundRobinByAttributeRule{attribute=%s, attribute_count=%s, task_filter=%s}",
                attributeName, distinctValueCount, taskFilter);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
