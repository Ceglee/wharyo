package pl.wharyo.dao.impl;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.vividsolutions.jts.geom.Geometry;

import pl.wharyo.dao.DAO;
import pl.wharyo.dao.FeatureDAO;
import pl.wharyo.model.Feature;
import pl.wharyo.model.attributes.Attribute;

public class WfsFeatureDAO extends DAO implements FeatureDAO {

	public WfsFeatureDAO(JdbcTemplate template) {
		super(template);
		// TODO Auto-generated constructor stub
	}

	public Long createFeature(Feature feature, String layer) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Feature> getAllFeatures(String layerName) {
		// TODO Auto-generated method stub
		return null;
	}

	public Feature getFeatureById(Long id, String layerName) {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateFeatureAttributes(Long id, List<Attribute> attributes, String layerName) {
		// TODO Auto-generated method stub
		
	}

	public void updateFeatureGeometry(Long id, Geometry geometry, String layerName) {
		// TODO Auto-generated method stub
		
	}

	public void updateFeature(Feature feature, String layerName) {
		// TODO Auto-generated method stub
		
	}

	public void deleteFeature(Long id, String laterName) {
		// TODO Auto-generated method stub
		
	}

	public boolean supportsLayer(String layerName) {
		// TODO Auto-generated method stub
		return false;
	}

}
