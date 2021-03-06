package pl.wharyo.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import pl.wharyo.exceptions.BrokenFeatureException;
import pl.wharyo.exceptions.LayerConfigurationBrokenException;
import pl.wharyo.exceptions.LayerDataSourceNotAvailableException;
import pl.wharyo.exceptions.UnsupportedAttributeType;
import pl.wharyo.model.Feature;
import pl.wharyo.model.attributes.Attribute;
import pl.wharyo.model.attributes.AttributeType;

public class GeoJsonFeatureDAOTest {

	private GeoJsonFeatureDAO dao;
	private GeometryFactory geomFactory;
	private final String WKT = "POLYGON ((347986.557996313727926 650018.791745546972379,347984.491029290191364 650008.161629425943829,347966.872596089728177 650012.00028246967122,347968.93956311326474 650022.827252592891455,347986.557996313727926 650018.791745546972379))";
	private final String BAD_WKT = "POINT (347986.557996313727926 650018.791745546972379)";
	private WKTReader reader;
	private final static String LAYER_NAME = "test_geojson";
	
	@Before
	public void setUp() throws Exception {
		clearGeoJson();
		copyGeoJson();
		dao = new GeoJsonFeatureDAO("src/test/resources/test_geojson_copy");
		geomFactory = JTSFactoryFinder.getGeometryFactory();
		reader = new WKTReader();
	}

	@After
	public void tearDown() throws Exception {
		clearGeoJson();
	}
	
	private void copyGeoJson() throws Exception {
		File shp_folder = new File("src/test/resources/test_geojson");
		File shp_folder_copy = new File("src/test/resources/test_geojson_copy/test_geojson");
		for (String shp_part_name: shp_folder.list()) {
			File shp_part = new File(shp_folder, shp_part_name);
			File shp_part_copy = new File(shp_folder_copy, shp_part_name);
			InputStream in = new BufferedInputStream(new FileInputStream(shp_part));
			OutputStream out = new BufferedOutputStream(new FileOutputStream(shp_part_copy));
			byte[] buff = new byte[1024];
			int length;
			while ((length = in.read(buff)) > 0) {
				out.write(buff, 0, length);
			}
			in.close();
			out.close();
		}
	}
	
	private void clearGeoJson() throws Exception {
		File shp_folder_copy = new File("src/test/resources/test_geojson_copy/test_geojson");
		for (File file: shp_folder_copy.listFiles()) {
			file.delete();
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void createFeature_nullLayerName_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		dao.createFeature(new Feature(), null);
	}
	
	@Test(expected=LayerDataSourceNotAvailableException.class)
	public void createFeature_notExistingLayerName_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, ParseException {
		Feature feature = new Feature();

		Geometry geom = reader.read(WKT);
		feature.setGeom(geom);
		dao.createFeature(feature, "fake_layername");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void createFeature_emptyLayerName_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		dao.createFeature(new Feature(), "");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void createFeature_nullFeature_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		dao.createFeature(null, LAYER_NAME);
		
	}
	
	@Test(expected=BrokenFeatureException.class)
	public void createFeature_brokenFeatureNullGeometry_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		dao.createFeature(new Feature(), LAYER_NAME);
	}
	
	@Test(expected=BrokenFeatureException.class)
	public void createFeature_brokenFeatureEmtpyGeometry_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		Feature feature = new Feature();
		Coordinate coord = null;
		feature.setGeom(geomFactory.createPoint(coord));
		dao.createFeature(feature, LAYER_NAME);
	}
	
	@Test
	public void createFeature_properFeatureGeometryNoAttributes_shouldReturnNewFetureId() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, ParseException {
		Feature feature = new Feature();
	
		Geometry geom = reader.read(WKT);
		feature.setGeom(geom);
		Long id = dao.createFeature(feature, LAYER_NAME);
		assertNotNull(id);
		assertEquals(new Long(4), id);

	}
	
	@Test
	public void createFeature_properFeatureGeometryWithAttributes_shouldReturnNewFeatureId() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, UnsupportedAttributeType, ParseException {
		Feature feature = new Feature();
		
		Attribute textAttr = new Attribute("name", AttributeType.TEXT);
		textAttr.setValue("test");
		
		Attribute doubleAttr = new Attribute("count", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		
		feature.addAttribute(textAttr);
		feature.addAttribute(doubleAttr);

		Geometry geom = reader.read(WKT);
		feature.setGeom(geom);
		Long id = dao.createFeature(feature, LAYER_NAME);
		assertNotNull(id);
		Feature resultFeature = dao.getFeatureById(id, LAYER_NAME);
		
		assertEquals(new Long(4), resultFeature.getId());
		assertEquals("test", (String) resultFeature.getAttribute("name").getValue());
		assertEquals(new Double(4.44), (Double) resultFeature.getAttribute("count").getValue());
		assertNotNull(resultFeature.getAttribute("date"));
		assertNull(resultFeature.getAttribute("date").getValue());
		assertTrue(geom.equals(resultFeature.getGeom()));	
	}
	
	@Test(expected=BrokenFeatureException.class)
	public void createFeature_wrongGeometryType_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, UnsupportedAttributeType, ParseException {
		Feature feature = new Feature();
		
		Attribute textAttr = new Attribute("name", AttributeType.TEXT);
		textAttr.setValue("test");
		
		Attribute doubleAttr = new Attribute("count", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		feature.addAttribute(textAttr);
		feature.addAttribute(doubleAttr);

		Geometry geom = reader.read(BAD_WKT);
		feature.setGeom(geom);
		dao.createFeature(feature, LAYER_NAME);
	}
	
	@Test(expected=BrokenFeatureException.class)
	public void createFeature_wrongGeometryCRS_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, UnsupportedAttributeType, ParseException {
		Feature feature = new Feature();
		
		Attribute textAttr = new Attribute("name", AttributeType.TEXT);
		textAttr.setValue("test");
		
		Attribute doubleAttr = new Attribute("count", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		feature.addAttribute(textAttr);
		feature.addAttribute(doubleAttr);

		Geometry geom = reader.read(WKT);
		//Test shapefile has SRID=2180
		geom.setSRID(2179);
		feature.setGeom(geom);
		dao.createFeature(feature, LAYER_NAME);
	}
	
	@Test
	public void createFeature_featureContainsWrongAttributeNames_shouldIgnoreWrongAttributes() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, UnsupportedAttributeType, ParseException {
		Feature feature = new Feature();
		
		Attribute textAttr = new Attribute("name", AttributeType.TEXT);
		textAttr.setValue("test");
		
		Attribute doubleAttr = new Attribute("count", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		
		feature.addAttribute(textAttr);
		feature.addAttribute(doubleAttr);
		
		Attribute textAttr_fake = new Attribute("name", AttributeType.TEXT);
		textAttr.setValue("test");
		
		Attribute doubleAttr_fake = new Attribute("count", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		
		feature.addAttribute(textAttr_fake);
		feature.addAttribute(doubleAttr_fake);

		Geometry geom = reader.read(WKT);
		feature.setGeom(geom);
		Long id = dao.createFeature(feature, LAYER_NAME);
		Feature resultFeature = dao.getFeatureById(id, LAYER_NAME);
		assertNull(resultFeature.getAttribute("name_fake"));
		assertNull(resultFeature.getAttribute("count_fake"));
		assertNotNull(resultFeature.getAttribute("name"));
		assertNotNull(resultFeature.getAttribute("count"));
		assertEquals("test", (String) resultFeature.getAttribute("name").getValue());
		assertEquals(new Double(4.44), (Double) resultFeature.getAttribute("count").getValue());

	}
	
	@Test
	public void createFeature_featureContainsWrongAttributeTypes_shouldIgnoreWrongAttributes() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, UnsupportedAttributeType, ParseException {
		Feature feature = new Feature();
		
		Attribute textAttr = new Attribute("name", AttributeType.LONG);
		textAttr.setValue(123L);
		
		Attribute doubleAttr = new Attribute("count", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		
		feature.addAttribute(textAttr);
		feature.addAttribute(doubleAttr);
		
		Geometry geom = reader.read(WKT);
		feature.setGeom(geom);
		Long id = dao.createFeature(feature, LAYER_NAME);
		Feature resultFeature = dao.getFeatureById(id, LAYER_NAME);
		assertNull(resultFeature.getAttribute("name").getValue());
		assertEquals(new Double(4.44), resultFeature.getAttribute("count").getValue());

	}
	
	// update attribute tests
	
	@Test(expected=IllegalArgumentException.class)
	public void updateFeatureAttributes_nullId_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		dao.updateFeatureAttributes(null, new ArrayList<Attribute>(), LAYER_NAME);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void updateFeatureAttributes_nullAttributesList_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		dao.updateFeatureAttributes(1L, null, LAYER_NAME);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void updateFeatureAttributes_emptyAttributesList_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		dao.updateFeatureAttributes(1L, null, LAYER_NAME);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void updateFeatureAttributes_nullLayerName_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		dao.updateFeatureAttributes(1L, new ArrayList<Attribute>(), null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void updateFeatureAttributes_emptyLayerName_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		dao.updateFeatureAttributes(1L, new ArrayList<Attribute>(), null);
	}
	
	@Test
	public void updateFeatureAttributes_allAttributesAreValid_shouldUpdateAllAttributes() throws UnsupportedAttributeType, LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		List<Attribute> attributes = new ArrayList<Attribute>();
		
		Attribute textAttr = new Attribute("name", AttributeType.TEXT);
		textAttr.setValue("test");
		
		Attribute doubleAttr = new Attribute("count", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		Attribute dateAttr = new Attribute("date", AttributeType.DATE);
		
		Calendar calendar = new GregorianCalendar(2000, 1, 1);
		dateAttr.setValue(calendar.getTime());
		
		attributes.add(textAttr);
		attributes.add(doubleAttr);
		attributes.add(dateAttr);
		dao.updateFeatureAttributes(1L, attributes, LAYER_NAME);
		Feature feature = dao.getFeatureById(1L, LAYER_NAME);
		assertEquals("test", feature.getAttribute("name").getValue());
		assertEquals(new Double(4.44), feature.getAttribute("count").getValue());
		assertEquals(calendar.getTime(), feature.getAttribute("date").getValue());
	}
	
	@Test
	public void updateFeatureAttributes_someAttributesContainInvalidNames_shouldIgnoreBrokenAttributes() throws UnsupportedAttributeType, LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		List<Attribute> attributes = new ArrayList<Attribute>();
		
		Attribute textAttr = new Attribute("bad_name", AttributeType.TEXT);
		textAttr.setValue("test");
		
		Attribute doubleAttr = new Attribute("count", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		
		Attribute dateAttr = new Attribute("date", AttributeType.DATE);
		Calendar calendar = new GregorianCalendar(2000, 1, 1);
		dateAttr.setValue(calendar.getTime());
		
		attributes.add(textAttr);
		attributes.add(doubleAttr);
		attributes.add(dateAttr);
		dao.updateFeatureAttributes(1L, attributes, LAYER_NAME);
		Feature feature = dao.getFeatureById(1L, LAYER_NAME);
		assertEquals("name1", feature.getAttribute("name").getValue());
		assertEquals(new Double(4.44), feature.getAttribute("count").getValue());
		assertEquals(calendar.getTime(), feature.getAttribute("date").getValue());
	}
	
	@Test
	public void updateFeatureAttributes_allAttributesContainInvalidNames_shouldIgnoreBrokenAttributes() throws UnsupportedAttributeType, LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		List<Attribute> attributes = new ArrayList<Attribute>();
		
		Attribute textAttr = new Attribute("bad_name", AttributeType.TEXT);
		textAttr.setValue("test");
		
		Attribute doubleAttr = new Attribute("bad_name2", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		
		Attribute dateAttr = new Attribute("bad_name3", AttributeType.DATE);
		Calendar calendar = new GregorianCalendar(2000, 1, 1);
		dateAttr.setValue(calendar.getTime());
		
		attributes.add(textAttr);
		attributes.add(doubleAttr);
		attributes.add(dateAttr);
		dao.updateFeatureAttributes(1L, attributes, LAYER_NAME);
		Feature feature = dao.getFeatureById(1L, LAYER_NAME);
		assertEquals("name1", feature.getAttribute("name").getValue());
		assertEquals(new Double(1.11), feature.getAttribute("count").getValue());
		assertEquals(null, feature.getAttribute("date").getValue());
	}
	
	@Test
	public void updateFeatureAttributes_someAttributesContainInvalidValueTypes_shouldIgnoreInvalidValueTypes() throws UnsupportedAttributeType, LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		List<Attribute> attributes = new ArrayList<Attribute>();
		
		Attribute textAttr = new Attribute("name", AttributeType.DOUBLE);
		textAttr.setValue(4.44);
		
		Attribute doubleAttr = new Attribute("count", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		
		Attribute dateAttr = new Attribute("date", AttributeType.DATE);
		Calendar calendar = new GregorianCalendar(2000, 1, 1);
		dateAttr.setValue(calendar.getTime());
		
		attributes.add(textAttr);
		attributes.add(doubleAttr);
		attributes.add(dateAttr);
		dao.updateFeatureAttributes(1L, attributes, LAYER_NAME);
		Feature feature = dao.getFeatureById(1L, LAYER_NAME);
		assertEquals("name1", feature.getAttribute("name").getValue());
		assertEquals(new Double(4.44), feature.getAttribute("count").getValue());
		assertEquals(calendar.getTime(), feature.getAttribute("date").getValue());
	}
	
	@Test
	public void updateFeatureAttributes_allAttributesContainInvalidValueTypes_shouldIgnoreInvalidValueTypes() throws UnsupportedAttributeType, LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		List<Attribute> attributes = new ArrayList<Attribute>();
		
		Attribute textAttr = new Attribute("count", AttributeType.TEXT);
		textAttr.setValue("name1");
		
		Attribute doubleAttr = new Attribute("date", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		
		Attribute dateAttr = new Attribute("name", AttributeType.DATE);
		Calendar calendar = new GregorianCalendar(2000, 1, 1);
		dateAttr.setValue(calendar.getTime());
		
		attributes.add(textAttr);
		attributes.add(doubleAttr);
		attributes.add(dateAttr);
		dao.updateFeatureAttributes(1L, attributes, LAYER_NAME);
		Feature feature = dao.getFeatureById(1L, LAYER_NAME);
		assertEquals("name1", feature.getAttribute("name").getValue());
		assertEquals(new Double(1.11), feature.getAttribute("count").getValue());
		assertEquals(null, feature.getAttribute("date").getValue());
	}
	
	@Test
	public void updateFeatureAttributes_idNotExists_shouldDoNothing() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, UnsupportedAttributeType, ParseException {
		List<Attribute> attributes = new ArrayList<Attribute>();
		
		Attribute textAttr = new Attribute("name", AttributeType.TEXT);
		textAttr.setValue("test");
		
		Attribute doubleAttr = new Attribute("count", AttributeType.DOUBLE);
		doubleAttr.setValue(4.44);
		Attribute dateAttr = new Attribute("date", AttributeType.DATE);
		
		Calendar calendar = new GregorianCalendar(2000, 1, 1);
		dateAttr.setValue(calendar.getTime());
		
		attributes.add(textAttr);
		attributes.add(doubleAttr);
		attributes.add(dateAttr);
		dao.updateFeatureAttributes(4L, attributes, LAYER_NAME);
		Feature feature = dao.getFeatureById(4L, LAYER_NAME);
		assertNull(feature);
	}
	
	// update gemoetry test
	
	@Test(expected=IllegalArgumentException.class)
	public void updateFeatureGeometry_nullId_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, ParseException, BrokenFeatureException {
		Geometry geom = reader.read(WKT);
		dao.updateFeatureGeometry(null, geom, LAYER_NAME);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void updateFeatureGeometry_nullGeometry_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		dao.updateFeatureGeometry(1L, null, LAYER_NAME);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void updateFeatureGeometry_nullLayerName_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, ParseException, BrokenFeatureException {
		Geometry geom = reader.read(WKT);
		dao.updateFeatureGeometry(1L, geom, null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void updateFeatureGeometry_emptyLayerName_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, ParseException, BrokenFeatureException {
		Geometry geom = reader.read(WKT);
		dao.updateFeatureGeometry(1L, geom, "");
	}
	
	@Test(expected=BrokenFeatureException.class)
	public void updateFeatureGeometry_wrongGeometryType_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, ParseException {
		Geometry geom = reader.read(BAD_WKT);
		dao.updateFeatureGeometry(1L, geom, LAYER_NAME);
	}
	
	@Test(expected=BrokenFeatureException.class)
	public void updateFeatureGeometry_wrongGeometryCRS_shouldThrowException() throws ParseException, LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException {
		Geometry geom = reader.read(WKT);
		//Test shapefile has SRID=2180
		geom.setSRID(2179);
		dao.updateFeatureGeometry(1L, geom, LAYER_NAME);
	}
	
	@Test
	public void updateFeatureGeometry_properGeometry_shouldUpdateGeometry() throws ParseException, LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, UnsupportedAttributeType {
		Geometry geom = reader.read(WKT);
		dao.updateFeatureGeometry(1L, geom, LAYER_NAME);
		dao.updateFeatureGeometry(2L, geom, LAYER_NAME);
		Feature feature1 = dao.getFeatureById(1L, LAYER_NAME);
		Feature feature2 = dao.getFeatureById(2L, LAYER_NAME);
		assertEquals(geom.getCentroid().getX(), feature1.getGeom().getCentroid().getX(), 1);
		assertEquals(geom.getCentroid().getY(), feature1.getGeom().getCentroid().getY(), 1);
		assertEquals(geom.getCentroid().getX(), feature2.getGeom().getCentroid().getX(), 1);
		assertEquals(geom.getCentroid().getY(), feature2.getGeom().getCentroid().getY(), 1);
	}
	
	@Test
	public void updateFeatureGeometry_idNotExists_shouldDoNothing() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, BrokenFeatureException, UnsupportedAttributeType, ParseException {
		Geometry geom = reader.read(WKT);
		dao.updateFeatureGeometry(4L, geom, LAYER_NAME);
		Feature feature = dao.getFeatureById(4L, LAYER_NAME);
		assertNull(feature);
	}
	
	// delete feature test
	
	@Test(expected=IllegalArgumentException.class) 
	public void deleteFeature_nullId_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		dao.deleteFeature(null, LAYER_NAME);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void deleteFeature_nullLayerName_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		dao.deleteFeature(1L, null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void deleteFeature_emptyLayerName_shouldThrowException() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException {
		dao.deleteFeature(1L, "");
	}
	
	@Test
	public void deleteFeature_idExists_shouldDeleteFeature() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, UnsupportedAttributeType {
		Feature feature = dao.getFeatureById(1L, LAYER_NAME);
		assertNotNull(feature);
		dao.deleteFeature(1L, LAYER_NAME);
		Feature removedFeature = dao.getFeatureById(1L, LAYER_NAME);
		assertNull(removedFeature);
	}
	
	@Test
	public void deleteFeature_idExists_shouldDoNothing() throws LayerDataSourceNotAvailableException, LayerConfigurationBrokenException, UnsupportedAttributeType {
		dao.deleteFeature(4L, LAYER_NAME);
		assertNotNull(dao.getFeatureById(1L, LAYER_NAME));
		assertNotNull(dao.getFeatureById(2L, LAYER_NAME));
		assertNotNull(dao.getFeatureById(3L, LAYER_NAME));
		
	}
	
	// support layer
	
	@Test
	public void supportsLayer_nullLayerName_shouldReturnFalse() {
		assertFalse(dao.supportsLayer(null));
	}
	
	@Test
	public void supportsLayer_emptyLayerName_shouldReturnFalse() {
		assertFalse(dao.supportsLayer(""));
	}
	
	@Test
	public void supportsLayer_layerNameExists_shouldReturnTrue() {
		assertTrue(dao.supportsLayer(LAYER_NAME));
	}
	
	@Test
	public void supportsLayer_layerNameDoesNotExist_shouldReturnFalse() {
		assertFalse(dao.supportsLayer("fake_layer_name"));
	}
}
