package pl.wharyo.dao.impl;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import com.vividsolutions.jts.geom.Geometry;

import pl.wharyo.dao.FeatureDAO;
import pl.wharyo.model.Feature;
import pl.wharyo.model.attributes.Attribute;

public class DatabaseFeatureDAO implements FeatureDAO {

	private JdbcTemplate template;
	public DatabaseFeatureDAO(JdbcTemplate template) {
		this.template = template;
	}
	
	public Long createFeature(Feature feature, String layer) {
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

	public void deleteFeature(Long id, String laterName) {
		// TODO Auto-generated method stub
		
	}

	public boolean supportsLayer(String layerName) {
		if(StringUtils.isEmpty(layerName)) {
			return false;
		}
		String[] args = {layerName, "DATABASE"};
		String query = "SELECT count(*) FROM layer_config WHERE layer_name = ? AND layer_type = ?";
		Integer count = template.queryForObject(query, args, Integer.class);
		if (count > 0) {
			return true;
		} else {
			return false;
		}
	}
}
