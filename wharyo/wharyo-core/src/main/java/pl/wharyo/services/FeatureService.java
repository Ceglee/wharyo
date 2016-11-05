package pl.wharyo.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.vividsolutions.jts.geom.Geometry;

import pl.wharyo.dao.FeatureDAO;
import pl.wharyo.exceptions.LayerNameNotSupportedException;
import pl.wharyo.model.Feature;
import pl.wharyo.model.attributes.Attribute;

@Service
public class FeatureService {
	
	@Autowired
	private List<FeatureDAO> daos;
	
	public Long createFeature(List<Attribute> attributes, Geometry geom, String layerName) throws LayerNameNotSupportedException {
		Feature feature = new Feature();
		feature.setAttributes(attributes);
		feature.setGeom(geom);
		FeatureDAO dao = chooseFeatureDAO(layerName);
		Long id = dao.createFeature(feature, layerName);
		return id;
	}
	
	public Feature getFeature(Long id, String layerName) throws LayerNameNotSupportedException {
		FeatureDAO dao = chooseFeatureDAO(layerName);
		return dao.getFeatureById(id, layerName);
	}

	private FeatureDAO chooseFeatureDAO(String layerName) throws LayerNameNotSupportedException {
		for (FeatureDAO dao: daos) {
			if (dao.supportsLayer(layerName)) {
				return dao;
			}
		}
		throw new LayerNameNotSupportedException("Couldn't find data manager for given layer name");
	}

	public List<FeatureDAO> getDaos() {
		return daos;
	}

	public void setDaos(List<FeatureDAO> daos) {
		this.daos = daos;
	}
}
