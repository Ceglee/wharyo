package pl.wharyo.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.vividsolutions.jts.geom.Geometry;

import pl.wharyo.model.attributes.Attribute;

/**
 * Class which describes single portion of spatial data.
 * @author cegli
 *
 */
public class Feature {

	private Long id;
	private Geometry geom;
	private List<Attribute> attributes;
	
	public Feature() {
		attributes = new ArrayList<Attribute>();
	}
	
	public Feature(Geometry geom, List<Attribute> attributes) {
		this();
		this.geom = geom;
		this.attributes = attributes;
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Geometry getGeom() {
		return geom;
	}
	public void setGeom(Geometry geom) {
		this.geom = geom;
	}
	
	public Attribute getAttribute(String name) {
		if (StringUtils.isEmpty(name)) {
			return null;
		}
		for (Attribute attr: attributes) {
			if (name.equals(attr.getName())) {
				return attr;
			}
		}
		return null;
	}
	
	public void addAttribute(Attribute attribute) {
		if (attribute == null || attribute.getName() == null) {
			return;
		}
		for (Attribute attr: attributes) {
			if (attribute.getName().equalsIgnoreCase(attr.getName())) {
				return;
			}
		}
		attributes.add(attribute);
	}
	
	public void removeAttribute(String name) {
		if (StringUtils.isEmpty(name)) {
			return;
		}
		for (Attribute attr: attributes) {
			if (name.equalsIgnoreCase(attr.getName())) {
				attributes.remove(attr);
				return;
			}
		}
	}
	
}
