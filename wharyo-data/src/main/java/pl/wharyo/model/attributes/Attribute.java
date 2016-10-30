package pl.wharyo.model.attributes;

import java.util.Date;

import org.springframework.util.StringUtils;

import pl.wharyo.exceptions.UnsupportedAttributeType;

public class Attribute {

	private String name;
	private Object value;
	private AttributeType type;
	
	public Attribute(String name, AttributeType type) {
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("Attribute name cannot be null");
		} else if (type == null) {
			throw new IllegalArgumentException("Attribute type cannot be null");
		}
		this.name = name;
		this.type = type;
		this.value = null;
	}
	
	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	};
	
	public AttributeType getType() {
		return type;
	};
	
	public void setValue(Object value) throws UnsupportedAttributeType {
		resolveAttributeType(value);
		this.value = value;
	};
	
	private void resolveAttributeType(Object value) throws UnsupportedAttributeType {
		if (value instanceof Long || value instanceof Integer) {
			this.type = AttributeType.LONG;
		} else if (value instanceof Float || value instanceof Double) {
			this.type = AttributeType.DOUBLE;
		} else if (value instanceof String) {
			this.type = AttributeType.TEXT;
		} else if (value instanceof Boolean) {
			this.type = AttributeType.BOOLEAN;
		} else if (value instanceof Date) {
			this.type = AttributeType.DATE;
		} else if (value == null && type != null) {
			return;
		} else {
			throw new UnsupportedAttributeType("Couldn't resolve value type");
		}
	}
}
