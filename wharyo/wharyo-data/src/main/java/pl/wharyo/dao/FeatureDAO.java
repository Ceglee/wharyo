package pl.wharyo.dao;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;

import com.vividsolutions.jts.geom.Geometry;

import pl.wharyo.exceptions.BrokenFeatureException;
import pl.wharyo.exceptions.LayerConfigurationBrokenException;
import pl.wharyo.exceptions.LayerDataSourceNotAvailableException;
import pl.wharyo.exceptions.UnsupportedAttributeType;
import pl.wharyo.model.Feature;
import pl.wharyo.model.attributes.Attribute;

/**
 * Main interface for managing data regardless of data source.
 * @author cegli
 *
 */
		
public interface FeatureDAO {

	// C
	public Long createFeature(Feature feature, String layerName) throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException;
	
	// R
	public Feature getFeatureById(Long id, String layerName) throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, UnsupportedAttributeType;
	
	// U
	public void updateFeatureAttributes(Long id, List<Attribute> attributes, String layerName) throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException;
	public void updateFeatureGeometry(Long id, Geometry geometry, String layerName) throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException;
	
	// D
	public void deleteFeature(Long id, String layerName) throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException;
	
	@Cacheable
	public boolean supportsLayer(String layerName);
}
