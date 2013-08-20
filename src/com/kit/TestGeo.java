package com.kit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class TestGeo {

	public static void readSHP() throws IOException {
		ShapefileDataStore shpDataStore = null;
		long time1 = System.currentTimeMillis();

		try {
			// shp�ļ�·��
			shpDataStore = new ShapefileDataStore(
					new File(
							"D:\\chinatest\\ȫ��ʡ�������м������м���������shp\\�ؼ�_��ֹ08��_shp\\China counties\\counties_china.shp")
							.toURI().toURL());
			shpDataStore.setStringCharset(Charset.forName("GBK"));
			// �ļ�����
			String typeName = shpDataStore.getTypeNames()[0];
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = null;
			featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) shpDataStore
					.getFeatureSource(typeName);
			FeatureCollection<SimpleFeatureType, SimpleFeature> result = featureSource
					.getFeatures();
			FeatureIterator itertor = result.features();
			int i=1;
			while (itertor.hasNext()) {
				Feature feature = itertor.next();
				File file = new File("d:/chinatest/"+i+".log");
				if (!file.exists()) {
					file.createNewFile();
				}
				FileWriter fw = new FileWriter(file);
		
				Collection<Property> properties = feature.getProperties();
				Iterator iter = properties.iterator();

				while (iter.hasNext()) {

					fw.write(properties.toString() + "\t");
		
				}

				fw.write("\n");
				fw.write("\n");
				fw.write("\n");
				fw.write("\n");
				i++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(System.currentTimeMillis() - time1 + "��ȡshp�ļ�ʱ��");

	}

	public void readShp() {
	}

	public static void main(String[] args) throws IOException {
		readSHP();
	}
}
