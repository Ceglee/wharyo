package pl.wharyo.dao.impl;

import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

import pl.wharyo.dao.FeatureDAO;
import pl.wharyo.exceptions.BrokenFeatureException;
import pl.wharyo.exceptions.LayerConfigurationBrokenException;
import pl.wharyo.exceptions.LayerDataSourceNotAvailableException;
import pl.wharyo.exceptions.UnsupportedAttributeType;
import pl.wharyo.model.Feature;
import pl.wharyo.model.attributes.Attribute;

public class GeoJSONFeatureDAO implements FeatureDAO {

	public Long createFeature(Feature feature, String layer)
			throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		// TODO Auto-generated method stub
		return null;
	}

	public Feature getFeatureById(Long id, String layerName)
			throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, UnsupportedAttributeType {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateFeatureAttributes(Long id, List<Attribute> attributes, String layerName)
			throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		// TODO Auto-generated method stub
		
	}

	public void updateFeatureGeometry(Long id, Geometry geometry, String layerName)
			throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		// TODO Auto-generated method stub
		
	}

	public void deleteFeature(Long id, String layerName)
			throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		// TODO Auto-generated method stub
		
	}

	public boolean supportsLayer(String layerName) {
		// TODO Auto-generated method stub
		return false;
	}

}
