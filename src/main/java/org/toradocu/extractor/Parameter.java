package org.toradocu.extractor;

import java.util.Objects;

final class Parameter {
	
	private final String type;
	private final String name;
	
	public Parameter(String type, String name) {
		this.type = type;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public String getType() {
		return type;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Parameter)) return false;
		
		Parameter that = (Parameter) obj;
		return type.equals(that.type) && name.equals(that.name);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(type, name);
	}
	
	@Override
	public String toString() {
		return type + " " + name;
	}
}
