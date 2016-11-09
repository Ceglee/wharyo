package pl.wharyo.dao.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.springframework.util.StringUtils;

import com.vividsolutions.jts.geom.Geometry;

import pl.wharyo.dao.FeatureDAO;
import pl.wharyo.exceptions.BrokenFeatureException;
import pl.wharyo.exceptions.LayerConfigurationBrokenException;
import pl.wharyo.exceptions.LayerDataSourceNotAvailableException;
import pl.wharyo.exceptions.UnsupportedAttributeType;
import pl.wharyo.model.Feature;
import pl.wharyo.model.attributes.Attribute;

public class GeoJsonFeatureDAO implements FeatureDAO {

	private final String GEOJSON_HOME;

	public GeoJsonFeatureDAO(URI jsonDirectory) {
		this(jsonDirectory.getPath());
	}

	public GeoJsonFeatureDAO(String jsonDirectory) {
		GEOJSON_HOME = jsonDirectory;
	}

	public Long createFeature(Feature feature, String layerName)
			throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		// TODO Auto-generated method stub
		return null;
	}

	public Feature getFeatureById(Long id, String layerName)
			throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, UnsupportedAttributeType {
		if (StringUtils.isEmpty(layerName)) {
			throw new IllegalArgumentException("LayerName parameter cannot be null or empty string");
		} else if (id == null) {
			throw new IllegalArgumentException("Feature id cannot be null");
		}
		FeatureJSON in = new FeatureJSON();
		InputStream stream;
		SimpleFeatureType featureType;
		try {
			stream = createInputStream(layerName);
			featureType = in.readFeatureCollectionSchema(stream, true);
			for(AttributeDescriptor desc: featureType.getAttributeDescriptors()) {
				System.out.println(desc.getName() + " " + desc.getType().getBinding());
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

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

	private InputStream createInputStream(String layerName) throws FileNotFoundException {
		StringBuilder builder = new StringBuilder(GEOJSON_HOME);
		builder.append(File.separator).append(layerName).append(File.separator).append(layerName).append(".geojson");
		File file = new File(builder.toString());
		return new FileInputStream(file);
	}
}
