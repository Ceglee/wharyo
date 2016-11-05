package pl.wharyo.dao.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.SortByImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.referencing.CRS;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import com.vividsolutions.jts.geom.Geometry;

import pl.wharyo.dao.DAO;
import pl.wharyo.dao.FeatureDAO;
import pl.wharyo.exceptions.BrokenFeatureException;
import pl.wharyo.exceptions.LayerConfigurationBrokenException;
import pl.wharyo.exceptions.LayerDataSourceNotAvailableException;
import pl.wharyo.exceptions.UnsupportedAttributeType;
import pl.wharyo.model.Feature;
import pl.wharyo.model.attributes.Attribute;
import pl.wharyo.model.attributes.AttributeType;

public class ShapefileFeatureDAO extends DAO implements FeatureDAO {

	private final String SHP_HOME;
	private static final Logger logger = Logger.getLogger(ShapefileFeatureDAO.class);
	
	public ShapefileFeatureDAO(JdbcTemplate template, URI uri) {
		this(template, uri.getPath());
	}
	
	public ShapefileFeatureDAO(JdbcTemplate template, String shapeHomeDirectory) {
		super(template);
		SHP_HOME = shapeHomeDirectory;
		try {
			Class.forName("org.geotools.referencing.crs.EPSGCRSAuthorityFactory");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public Long createFeature(Feature feature, String layerName) throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		if (feature == null) {
			throw new IllegalArgumentException("Feature parameter cannot be null");
		} else if (StringUtils.isEmpty(layerName)) {
			throw new IllegalArgumentException("LayerName parameter cannot be null or empty string");
		} else if (feature.getGeom() == null || feature.getGeom().isEmpty()) {
			throw new BrokenFeatureException("Feature contains empty geometry or it is null");
		}
	
		DataStore dStore = createDataStore(layerName);
		SimpleFeatureStore fStore = createFeatureStore(dStore, layerName);
		SimpleFeatureType featureType = fStore.getSchema();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		
		PropertyName idProperty = null;
		FilterFactory ff = CommonFactoryFinder.getFilterFactory( null );
		for(AttributeDescriptor attrDesc: featureType.getAttributeDescriptors()) {
			if (attrDesc.getLocalName().equalsIgnoreCase("id")) {
				idProperty = ff.property(attrDesc.getLocalName());
			} else {
				Attribute attr = feature.getAttribute(attrDesc.getLocalName());
				if (attr != null && compareAttributeTypes(attrDesc.getType(), attr)) {
					featureBuilder.set(attrDesc.getName(), attr.getValue());
				}
			}
		}
		
		GeometryDescriptor geomDesc = featureType.getGeometryDescriptor();
		if (geomDesc != null) {
			try {
				if (feature.getGeom().getSRID() > 0 && 
						!geomDesc.getCoordinateReferenceSystem().getCoordinateSystem()
						.equals(CRS.decode("EPSG:" + feature.getGeom().getSRID()))) {
					throw new BrokenFeatureException("Feature CRS doesn't match shapefile CRS");
				} else {
					logger.warn("Saving feature with empty geometry srid");
				}
			} catch (NoSuchAuthorityCodeException e) {
				throw new BrokenFeatureException("Feature geometry contains unknown CRS");
			} catch (FactoryException e) {
				// Nothing we can do :(
			}
			if (!compareGeometryTypes(feature.getGeom(), geomDesc.getType().getName().getLocalPart())) {
				throw new BrokenFeatureException("Feature contains geometry which is not koherent with geometry type in layer: " + layerName);
			}
			featureBuilder.set(geomDesc.getLocalName(), feature.getGeom());
		} else {
			logger.error("No geometry descriptor for given shapefile layer: " + layerName);
			throw new LayerConfigurationBrokenException("Could not obtain geometry description for given layer: " + layerName, LayerConfigurationBrokenException.Reason.NO_GEOMETRY_METADATA);
		}
		
		try {
			Transaction transaction = new DefaultTransaction("wharyo_full_lock");
			fStore.setTransaction(transaction);
			try {
				Long nextId = getCurrentId(idProperty, fStore) + 1;
				featureBuilder.set(idProperty.getPropertyName(), nextId);
				SimpleFeature sFeature = featureBuilder.buildFeature(null);
				fStore.addFeatures(DataUtilities.collection(sFeature));
				transaction.commit();
				return nextId;
			} catch (Exception ex) {
				transaction.rollback();
				return null;
			} finally {
				transaction.close();
			}
		} catch (IOException ex) {
			// Transaction rollback/close fail
			return null;
		}			
	}

	public Feature getFeatureById(Long id, String layerName) throws LayerConfigurationBrokenException, LayerDataSourceNotAvailableException, UnsupportedAttributeType {
		if (StringUtils.isEmpty(layerName)) {
			throw new IllegalArgumentException("LayerName parameter cannot be null or empty string");
		} else if (id == null) {
			throw new IllegalArgumentException("Feature id cannot be null");
		}
		
		try {
			DataStore dStore = createDataStore(layerName);
			SimpleFeatureStore fStore = createFeatureStore(dStore, layerName);
			Query query = createIdQuery(id, layerName);
			
			SimpleFeatureCollection result = fStore.getFeatures(query);
			SimpleFeatureIterator iter = result.features();
			if (iter.hasNext()) {
				Feature feature = new Feature();
				SimpleFeature resultFeature = iter.next();
				for (Property prop: resultFeature.getProperties()) {
					Attribute attr = null;
					if (prop.getName().getLocalPart().equalsIgnoreCase("id")) {
						feature.setId((Long) prop.getValue());
					} else if (prop.getType().getBinding() == Integer.class || prop.getType().getBinding() == Long.class) {
						attr = new Attribute(prop.getName().getLocalPart(), AttributeType.LONG);
						attr.setValue(prop.getValue());
					} else if (prop.getType().getBinding() == Double.class || prop.getType().getBinding() == Float.class) {
						attr = new Attribute(prop.getName().getLocalPart(), AttributeType.DOUBLE);
						attr.setValue(prop.getValue());
					} else if (prop.getType().getBinding() == String.class) {
						attr = new Attribute(prop.getName().getLocalPart(), AttributeType.TEXT);
						attr.setValue(prop.getValue());
					} else if (prop.getType().getBinding() == Date.class) {
						attr = new Attribute(prop.getName().getLocalPart(), AttributeType.DATE);
						attr.setValue(prop.getValue());
					}
					if (attr != null) {
						feature.addAttribute(attr);
					}
				}
				feature.setGeom((Geometry) resultFeature.getDefaultGeometry());
				iter.close();
				return feature;
			}
		} catch (IOException ex) {
			// Nothing we can do :(
		}
		return null;
	}

	public void updateFeatureAttributes(Long id, List<Attribute> attributes, String layerName) throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		if (id == null) {
			throw new IllegalArgumentException("Feature id cannot be null");
		} else if (attributes == null) {
			throw new IllegalArgumentException("Attribute list cannot be null");
		} else if (StringUtils.isEmpty(layerName)) {
			throw new IllegalArgumentException("LayerName parameter cannot be null or empty string");
		} else if (attributes.size() == 0) {
			return;
		}
		
		
		DataStore dStore = createDataStore(layerName);
		SimpleFeatureStore fStore = createFeatureStore(dStore, layerName);
		
		List<Name> attrNames = new ArrayList<Name>();
		List<Object> attrValues = new ArrayList<Object>();
		int i = 0;
		for (Attribute attr: attributes) {
			for (AttributeDescriptor desc : fStore.getSchema().getAttributeDescriptors()) {
				if (desc.getName().getLocalPart().equalsIgnoreCase(attr.getName())
						&& compareAttributeTypes(desc.getType(), attr)) {
					attrNames.add(desc.getName());
					attrValues.add(attr.getValue());
					i++;
					break;
				}
			}
		}
		try {	
			Transaction transaction = new DefaultTransaction("wharyo_full_lock");
			fStore.setTransaction(transaction);
			try {
				if (i > 0) {
					fStore.modifyFeatures(attrNames.toArray(new Name[attrNames.size()]), attrValues.toArray(), CQL.toFilter("id = " + id));
					transaction.commit();
				}
			} catch (IOException e) {
				transaction.rollback();
			} finally {
				transaction.close();
			}
		} catch (CQLException e) {
			logger.error("No id filed found for shapefile: " + layerName);
			throw new LayerConfigurationBrokenException("Coulnd't find proper id field in shapefile " + layerName, LayerConfigurationBrokenException.Reason.INVALID_ID_FIELD);
		} catch (IOException e) {
			// Transaction rollback/close fail
		}
		
	}

	public void updateFeatureGeometry(Long id, Geometry geometry, String layerName) throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		if (id == null) {
			throw new IllegalArgumentException("Feature id cannot be null");
		} else if (geometry == null) {
			throw new IllegalArgumentException("Geometry cannot be null");
		} else if (StringUtils.isEmpty(layerName)) {
			throw new IllegalArgumentException("LayerName parameter cannot be null or empty string");
		}
		
		DataStore dStore = createDataStore(layerName);
		SimpleFeatureStore fStore = createFeatureStore(dStore, layerName);
		SimpleFeatureType featureType = fStore.getSchema();
		
		GeometryDescriptor geomDesc = featureType.getGeometryDescriptor();
		if (geomDesc != null) {
			try {
				if (geometry.getSRID() > 0 && 
						!geomDesc.getCoordinateReferenceSystem().getCoordinateSystem()
						.equals(CRS.decode("EPSG:" + geometry.getSRID()))) {
					throw new BrokenFeatureException("Feature CRS doesn't match shapefile CRS");
				} else {
					logger.warn("Saving feature with empty geometry srid");
				}
			} catch (NoSuchAuthorityCodeException e) {
				throw new BrokenFeatureException("Feature geometry contains unknown CRS");
			} catch (FactoryException e) {
				// Nothing we can do :(
			}
			if (!compareGeometryTypes(geometry, geomDesc.getType().getName().getLocalPart())) {
				throw new BrokenFeatureException("Feature contains geometry which is not koherent with geometry type in layer: " + layerName);
			}
		} else {
			logger.error("No geometry descriptor for given shapefile layer: " + layerName);
			throw new LayerConfigurationBrokenException("Could not obtain geometry description for given layer: " + layerName, LayerConfigurationBrokenException.Reason.NO_GEOMETRY_METADATA);
		}
		
		try {
			Transaction transaction = new DefaultTransaction("wharyo_full_lock");
			fStore.setTransaction(transaction);
			try {
				fStore.modifyFeatures(featureType.getGeometryDescriptor().getName(), geometry,  CQL.toFilter("id = " + id));
				transaction.commit();
			} catch (IOException e) {
				transaction.rollback();
			} finally {
				transaction.close();
			}
		} catch (CQLException e) {
			logger.error("No id filed found for shapefile: " + layerName);
			throw new LayerConfigurationBrokenException("Coulnd't find proper id field in shapefile " + layerName, LayerConfigurationBrokenException.Reason.INVALID_ID_FIELD);
		} catch (IOException e) {
			// Transaction rollback/close fail
		}
	}

	public void deleteFeature(Long id, String laterName) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean supportsLayer(String layerName) {
		if(StringUtils.isEmpty(layerName)) {
			return false;
		}
		String[] args = {layerName, "SHAPEFILE"};
		String query = "SELECT count(*) FROM layer_config WHERE layer_name = ? AND layer_type = ?";
		Integer count = getTemplate().queryForObject(query, args, Integer.class);
		if (count > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	private DataStore createDataStore(String layerName) throws LayerDataSourceNotAvailableException {
		try {
			StringBuilder builder = new StringBuilder(SHP_HOME);
			builder.append(File.separator).append(layerName).append(File.separator).append(layerName).append(".shp");
			
			File file = new File(builder.toString());
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("url", file.toURI().toURL());
			
			DataStore dStore = DataStoreFinder.getDataStore(params);
			return dStore;
		} catch (MalformedURLException e1) {
			logger.error("Shapefile with given name cannnot be found: " + layerName);
			throw new IllegalArgumentException("Coulnd't create URL with given layerName parameter: " + layerName);
		} catch (IOException e) {
			logger.error("Couldn't connect to given layer: " + layerName);
			throw new LayerDataSourceNotAvailableException("Coulnd't connect to layer datasource", LayerDataSourceNotAvailableException.Reason.CONNECTION_UNAVAILABLE);
		}
	}
	
	private SimpleFeatureStore createFeatureStore(DataStore dStore, String layerName) throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		try {
			if (dStore.getFeatureSource(layerName) instanceof SimpleFeatureStore) {
				if (dStore.getTypeNames() != null && dStore.getTypeNames().length != 0) {
					String typeName = dStore.getTypeNames()[0];
					return (SimpleFeatureStore) dStore.getFeatureSource(typeName);
				} else {
					logger.error("Wrong typeName configuration for given shapefile: " + layerName);
					throw new LayerConfigurationBrokenException("Coulnd't find any typeNames for shapefile " + layerName, LayerConfigurationBrokenException.Reason.NO_ATTRIBUTE_METADATA);
				}
			} else {
				logger.error("FeatureSource doesn't implement FeatureStore: " + layerName);
				throw new LayerDataSourceNotAvailableException("Cannot create read/write access", LayerDataSourceNotAvailableException.Reason.READONLY_ACCESS);
			}
		} catch (IOException e) {
			logger.error("Couldn't connect to given layer: " + layerName);
			throw new LayerDataSourceNotAvailableException("Coulnd't connect to layer datasource", LayerDataSourceNotAvailableException.Reason.CONNECTION_UNAVAILABLE);
		}
	}
	
	private Query createIdQuery(Long id, String layerName) throws LayerConfigurationBrokenException, IOException {
		try {
			Query query = new Query();
			query.setFilter(CQL.toFilter("id = " + id));
			query.setMaxFeatures(1);
			return query;
		} catch (CQLException e) {
			logger.error("No id filed found for shapefile: " + layerName);
			throw new LayerConfigurationBrokenException("Coulnd't find proper id field in shapefile " + layerName, LayerConfigurationBrokenException.Reason.INVALID_ID_FIELD);
		}
		
	}
	
	private Long getCurrentId(PropertyName idProperty, SimpleFeatureStore store) {
		if (idProperty == null || store == null) {
			logger.error("idProperty or store cannot be null");
			throw new IllegalArgumentException("idProperty or store cannot be null");
		}
		Query query = new Query();
		query.setMaxFeatures(1);
		SortBy idSort = new SortByImpl(idProperty, SortOrder.DESCENDING);
		query.setSortBy(new SortBy[]{idSort});
		try {
			SimpleFeatureCollection result = store.getFeatures(query);
			SimpleFeatureIterator iter = result.features();
			if (iter.hasNext()) {
				SimpleFeature feature = iter.next();
				Long id = (Long) feature.getProperty("id").getValue();
				iter.close();
				return id;
			}
			return 0L;
		} catch (IOException e) {
			return null;
		}
	}
	
	private boolean compareAttributeTypes(PropertyType type, Attribute attr) {
		switch(attr.getType()){
			case TEXT:
				if (type.getBinding() == String.class) 
					return true;
				else 
					return false;
			case LONG:
				if (type.getBinding() == Long.class || type.getBinding() == Integer.class)
					return true;
				else 
					return false;
			case DOUBLE:
				if (type.getBinding() == Double.class || type.getBinding() == Float.class) 
					return true;
				else 
					return false;
			case DATE:
				if (type.getBinding() == Date.class)
					return true;
				else 
					return false;
			default:
				return false;
		}	
	}
	
	private boolean compareGeometryTypes(Geometry geom, String type_2) {
		String type_1 = geom.getGeometryType();
		if (type_1 == null && type_2 == null) {
			return true;
		} else if ((type_1 != null && type_2 == null) 
				||(type_1 == null && type_2 != null)) {
			return false;
		}
		if (type_1.equals(type_2)) {
			return true;
		} else if ((type_1.equalsIgnoreCase("LineString") || type_1.equalsIgnoreCase("MultiLineString"))
				&& (type_2.equalsIgnoreCase("LineString") || type_2.equalsIgnoreCase("MultiLineString"))) {
			return true;
		} else if ((type_1.equalsIgnoreCase("Polygon") || type_1.equalsIgnoreCase("MultiPolygon"))
				&& (type_2.equalsIgnoreCase("Polygon") || type_2.equalsIgnoreCase("MultiPolygon"))) {
			return true;
		} else if ((type_1.equalsIgnoreCase("Point") || type_1.equalsIgnoreCase("MultiPoint"))
				&& (type_2.equalsIgnoreCase("Point") || type_2.equalsIgnoreCase("MultiPoint"))) {
			return true;
		} else {
			return false;
		}
	}
}
