package com.kit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Geometry;



public class TestGeojson {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
	    //File file=new File("C:\\Users\\kit_hp\\workspace\\geotest\\src\\com\\kit\\example.js");
		String filepath=
				"D:\\县级bou4_4m\\BOUNT_poly.shp";
		new TestGeojson().readSHP(filepath);
	}
	/** 
	 * 读取shap格式的文件 
	 *  
	 * @param path 
	 * @throws IOException 
	 */  
	public void readSHP(String path) throws IOException {  
	    ShapefileDataStore shpDataStore = null;  
	    File  file=new File("d:\\chinatest\\Test.json");
	    FileWriter  fw=new FileWriter(file);
	    try {  
	        shpDataStore = new ShapefileDataStore(new File(path).toURI()  
	                .toURL());  
	        shpDataStore.setStringCharset(Charset.forName("GBK"));  
	        // 文件名称   
	        String typeName = shpDataStore.getTypeNames()[0]; 
	        System.out.println(typeName);
	        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = null;  
	        featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) shpDataStore  
	                .getFeatureSource(typeName);  
	        FeatureCollection<SimpleFeatureType, SimpleFeature> result = featureSource  
	                .getFeatures();  
	       // System.out.println(result);
	        SimpleFeatureType schema = result.getSchema(); // schema   
	        List<AttributeDescriptor> columns = schema  
	                .getAttributeDescriptors();  
	        FeatureIterator<SimpleFeature> itertor = result.features();  
	        System.out.println(schema);
	        /* 
	         * 或者使用 FeatureReader FeatureReader reader = 
	         * DataUtilities.reader(result); while(reader.hasNext()){ 
	         * SimpleFeature feature = (SimpleFeature) reader.next(); } 
	         */  
	        while (itertor.hasNext()) {  
	            SimpleFeature feature = itertor.next();  
	           
	            for (AttributeDescriptor attributeDes : columns) {  
	                String attributeName = attributeDes.getName().toString();// attribute   
	                if (attributeName.equals("the_geom"))  
	                    continue;  
	                feature.getAttribute(attributeName); // attributeValue   
	                String   str=attributeName+":"+ feature.getAttribute(attributeName)+"\t";
	               System.out.print(str);
	               fw.write(str);
	            }  
	            Geometry g = (Geometry) feature.getDefaultGeometry();// Geometry   
	            System.out.println("\n");
	            fw.write("\n");
	        }  
	        itertor.close();  
	    } catch (MalformedURLException e) {  
	        e.printStackTrace();  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    }  
	} 
	
	
}
