package cn.geodata.models.geojson;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.factory.GeoTools;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeoJSON {
	private static Logger log = Logger.getLogger("cn.geodata.geojson");
	
	private GeometryFactory geometryFactory;
	
	public GeoJSON() {
		this.geometryFactory = new GeometryFactory();
	}
	
	/**
	 * Parse GeoJSON string, and return the corresponding objects
	 * @param json
	 * @return
	 * @throws IOException
	 * @throws IllegalAttributeException
	 */
	public Object parse(String json) throws IOException, IllegalAttributeException{
		JSONObject _obj = JSONObject.fromObject(json);
		if(_obj.has("type") == false){
			throw new UnsupportedOperationException("no type attribute");
		}
		
		String _type = _obj.getString("type");
		if(_type.equalsIgnoreCase("Point") || 
				_type.equalsIgnoreCase("LineString") ||
				_type.equalsIgnoreCase("Polygon") || 
				_type.equalsIgnoreCase("MultiPoint") || 
				_type.equalsIgnoreCase("MultiLineString") || 
				_type.equalsIgnoreCase("MultiPolygon")){
			return this.parseGeometry(_obj);
		}
		else if(_type.equalsIgnoreCase("Box")){
			return this.parseBox(_obj);
		}
		else if(_type.equalsIgnoreCase("GeometryCollection")){
			return this.parseGeometryCollection(_obj);
		}
		else if(_type.equalsIgnoreCase("FeatureCollection")){
			return this.parseFeatureCollection(_obj);
		}
		
		throw new UnsupportedGeoJSONType(_type);
	}
	
	/**
	 * Parse all geometry types, including Point, LineString, Polygon, MultiPoing, MultiLineString, MultiPolygon
	 * @param obj
	 * @return
	 * @throws UnsupportedGeoJSONType
	 */
	public Geometry parseGeometry(JSONObject obj) throws UnsupportedGeoJSONType{
		if(obj.has("type") == false){
			throw new UnsupportedOperationException("no type attribute");
		}
		
		String _type = obj.getString("type");
		if(_type.equalsIgnoreCase("Point")){
			return this.parsePoint(obj);
		}
		else if(_type.equalsIgnoreCase("LineString")){
			return this.parseLineString(obj);
		}
		else if(_type.equalsIgnoreCase("Polygon")){
			return this.parsePolygon(obj);
		}
		else if(_type.equalsIgnoreCase("MultiPoint")){
			return this.parseMultiPoint(obj);
		}
		else if(_type.equalsIgnoreCase("MultiLineString")){
			return this.parseMultiLineString(obj);
		}
		else if(_type.equalsIgnoreCase("MultiPolygon")){
			return this.parseMultiPolygon(obj);
		}
		
		throw new UnsupportedGeoJSONType(_type);
	}
	
	/**
	 * Parse Point
	 * @param obj
	 * @return
	 */
	public Point parsePoint(JSONObject obj){
		return this.parseCoordinatePoint(obj.getJSONArray("coordinates"));
	}
	
	/**
	 * Parse LineString
	 * @param obj
	 * @return
	 */
	public LineString parseLineString(JSONObject obj){
		return this.parseCoordinateLineString(obj.getJSONArray("coordinates"));
	}
	
	/**
	 * Parse Polygon
	 * @param obj
	 * @return
	 */
	public Polygon parsePolygon(JSONObject obj){
		return this.parseCoordinatePolygon(obj.getJSONArray("coordinates"));
	}
	
	/**
	 * Parse MultiPoint
	 * @param obj
	 * @return
	 */
	public MultiPoint parseMultiPoint(JSONObject obj){
		return this.geometryFactory.createMultiPoint(this.parseCoordinates(obj.getJSONArray("coordinates")));
	}
	
	/**
	 * Parse MultiLineString
	 * @param obj
	 * @return
	 */
	public MultiLineString parseMultiLineString(JSONObject obj){
		JSONArray _c = obj.getJSONArray("coordinates");
		LineString[] _lines = new LineString[_c.size()];
		for (int i = 0; i < _lines.length; i++) {
			_lines[i] = this.parseCoordinateLineString(_c.getJSONArray(i));
		}
		
		return this.geometryFactory.createMultiLineString(_lines);
	}

	/**
	 * Parse MultiPolygon
	 * @param obj
	 * @return
	 */
	public MultiPolygon parseMultiPolygon(JSONObject obj){
		JSONArray _c = obj.getJSONArray("coordinates");
		Polygon[] _polys = new Polygon[_c.size()];
		for (int i = 0; i < _polys.length; i++) {
			_polys[i] = this.parseCoordinatePolygon(_c.getJSONArray(i));
		}
		
		return this.geometryFactory.createMultiPolygon(_polys);
	}

	/**
	 * Parse Box
	 * @param obj
	 * @return
	 */
	public Envelope parseBox(JSONObject obj){
		Coordinate[] _pts = this.parseCoordinates(obj.getJSONArray("coordinates"));
		if(_pts.length != 2){
			throw new IndexOutOfBoundsException();
		}
		
		return new Envelope(_pts[0], _pts[1]);
	}
	
	/**
	 * Parse GeometryCollection
	 * @param obj
	 * @return
	 * @throws UnsupportedGeoJSONType
	 */
	public GeometryCollection parseGeometryCollection(JSONObject obj) throws UnsupportedGeoJSONType{
		if(obj.getString("type").equals("GeometryCollection") == false){
			throw new UnsupportedOperationException("Not GeometryCollection type");
		}
		
		JSONArray _list = obj.getJSONArray("members");
		Geometry[] _cols = new Geometry[_list.size()];
		for (int i = 0; i < _cols.length; i++) {
			_cols[i] = this.parseGeometry(_list.getJSONObject(i));
		}
		
		return this.geometryFactory.createGeometryCollection(_cols);
	}
	
	/**
	 * Parse Feature with appointed FeatureType
	 * @param featureType
	 * @param obj
	 * @return
	 * @throws IllegalAttributeException
	 * @throws UnsupportedGeoJSONType
	 */
	public SimpleFeature parseFeature(SimpleFeatureType featureType, JSONObject obj) throws IllegalAttributeException, UnsupportedGeoJSONType{
		if(obj.getString("type").equalsIgnoreCase("Feature") == false){
			throw new UnsupportedOperationException("Not Feature type");
		}
		
		JSONObject _props = obj.getJSONObject("properties");
		SimpleFeatureBuilder _builder = new SimpleFeatureBuilder(featureType);
		Geometry _geo = this.parseGeometry(obj.getJSONObject("geometry"));
		
		for(AttributeDescriptor _t: featureType.getAttributeDescriptors()){
			if(_t.getLocalName().equals(featureType.getGeometryDescriptor().getLocalName())){
				_builder.add(_geo);
			}
			else{
				_builder.add(_props.get(_t.getLocalName()));
			}
		}
		
		return _builder.buildFeature(obj.getString("id"));
	}
	
	/**
	 * Parse FeatureType
	 * @param obj
	 * @return
	 * @throws IllegalAttributeException
	 * @throws IOException
	 */
	public FeatureCollection<SimpleFeatureType, SimpleFeature> parseFeatureCollection(JSONObject obj) throws IllegalAttributeException, IOException{
		SimpleFeatureType _featureType = this.parseFeatureType(obj.getJSONArray("features"));
		
		JSONArray _members = obj.getJSONArray("features");
		FeatureCollection<SimpleFeatureType, SimpleFeature> _featureCollection = CommonFactoryFinder.getFeatureCollections(GeoTools.getDefaultHints()).newCollection();
		for (int i = 0; i < _members.size(); i++) {
			_featureCollection.add(this.parseFeature(_featureType, _members.getJSONObject(i)));
		}
		
		return _featureCollection;
	}
	
	protected Coordinate parseCoordinate(JSONArray obj){
		if(obj.size() < 2){
			throw new IndexOutOfBoundsException("Not enough coordinates");
		}
		
		return new Coordinate(obj.getDouble(0), obj.getDouble(1));
	}

	protected Coordinate[] parseCoordinates(JSONArray obj){
		Coordinate[] _cs = new Coordinate[obj.size()];
		for(int i=0;i<obj.size();i++){
			_cs[i] = this.parseCoordinate(obj.getJSONArray(i));
		}
		
		return _cs;
	}
	
	protected Point parseCoordinatePoint(JSONArray obj){
		return this.geometryFactory.createPoint(this.parseCoordinate(obj));
	}

	protected LineString parseCoordinateLineString(JSONArray obj){
		return this.geometryFactory.createLineString(this.parseCoordinates(obj));
	}

	protected LinearRing parseCoordinateLineRing(JSONArray obj){
		return this.geometryFactory.createLinearRing(this.parseCoordinates(obj));
	}
	
	protected Polygon parseCoordinatePolygon(JSONArray obj){
		if(obj.size() < 0){
			throw new IndexOutOfBoundsException("Not enough line string");
		}
		
		LinearRing _out = this.parseCoordinateLineRing(obj.getJSONArray(0));
		LinearRing[] _inners = new LinearRing[obj.size() - 1];
		for(int i=0;i<_inners.length;i++){
			_inners[i] = this.parseCoordinateLineRing(obj.getJSONArray(i+1));
		}
		
		return this.geometryFactory.createPolygon(_out, _inners);		
	}
	
	public SimpleFeatureType parseFeatureType(JSONArray obj) throws IOException{
		if(obj.size() == 0){
			throw new NullPointerException("No element inside");
		}
		
		FeatureTypeFactory _factory = new FeatureTypeFactoryImpl();
		GeometryDescriptor _geometry = parseComplexElement(obj);
		
		List<AttributeDescriptor> _attributes = new ArrayList<AttributeDescriptor>();
		_attributes.add(_geometry);
		
		JSONObject _ele = obj.getJSONObject(0).getJSONObject("properties");
		Iterator<String> _it = _ele.keys();
		while(_it.hasNext()){
			String _key = _it.next();
			_attributes.add(parseSimpleElement(obj, _key));
		}
		
		try {
			return (new FeatureTypeFactoryImpl()).createSimpleFeatureType(new NameImpl("shape"), _attributes, _geometry, true, null, null, null);
		} catch (FactoryRegistryException e) {
			throw new IOException(e.getMessage());
		}
	}
	
	protected AttributeDescriptor parseSimpleElement(JSONArray fs, String eleName) throws IOException{
		//int 0
		//double 1
		//string 2
		
		int _type = 0;
		int _len = 0;
		for(int i=0;i<fs.size();i++){
			String _val = fs.getJSONObject(i).getJSONObject("properties").getString(eleName);

			int _newType = evaluateSimpleType(_val);
			if(_newType > _type){
				_type = _newType;
			}
			
			if(_type == 2){
				_len = Math.max(_len, _val.getBytes("UTF-8").length);
			}
		}
		
		AttributeTypeBuilder _builder = new AttributeTypeBuilder();
		switch (_type) {
			case 0:
				_builder.setBinding(Integer.class);
				break;
			case 1:
				_builder.setBinding(Double.class);
				break;
			case 2:
				_builder.setBinding(String.class);
				_builder.setLength(_len);
				break;
			default:
				throw new IOException("Unknown type:" + _type);
		}
		
		_builder.setName(eleName);
		
		return _builder.buildDescriptor(eleName);
	}

	
	protected int evaluateSimpleType(String val){
		if(Pattern.matches("\\d+", val)){
			return 0;
		}
		else if(Pattern.matches("\\d*\\.\\d*", val)){
			return 1;
		}
		return 2;
	}

	protected GeometryDescriptor parseComplexElement(JSONArray obj) throws UnsupportedGeoJSONType{
		FeatureTypeFactory _factory = new FeatureTypeFactoryImpl();
		
		String _eleName = "geometry";
		JSONObject _obj = obj.getJSONObject(0).getJSONObject(_eleName);
		
		if(_obj.getString("type").equalsIgnoreCase("Point")){
			return _factory.createGeometryDescriptor(_factory.createGeometryType(new NameImpl(_eleName), Point.class, null, false, false, null, null, null), new NameImpl("shape"), 0, 1, true, null);
		}
		if(_obj.getString("type").equalsIgnoreCase("LineString")){
			return _factory.createGeometryDescriptor(_factory.createGeometryType(new NameImpl(_eleName), LineString.class, null, false, false, null, null, null), new NameImpl("shape"), 0, 1, true, null);
		}
		if(_obj.getString("type").equalsIgnoreCase("Polygon")){
			return _factory.createGeometryDescriptor(_factory.createGeometryType(new NameImpl(_eleName), Polygon.class, null, false, false, null, null, null), new NameImpl("shape"), 0, 1, true, null);
		}
		if(_obj.getString("type").equalsIgnoreCase("MultiPoint")){
			return _factory.createGeometryDescriptor(_factory.createGeometryType(new NameImpl(_eleName), MultiPoint.class, null, false, false, null, null, null), new NameImpl("shape"), 0, 1, true, null);
		}
		if(_obj.getString("type").equalsIgnoreCase("MultiLineString")){
			return _factory.createGeometryDescriptor(_factory.createGeometryType(new NameImpl(_eleName), MultiLineString.class, null, false, false, null, null, null), new NameImpl("shape"), 0, 1, true, null);
		}
		if(_obj.getString("type").equalsIgnoreCase("MultiPolygon")){
			return _factory.createGeometryDescriptor(_factory.createGeometryType(new NameImpl(_eleName), MultiPolygon.class, null, false, false, null, null, null), new NameImpl("shape"), 0, 1, true, null);
		}
		
		throw new UnsupportedGeoJSONType(_obj.getString("type"));
	}
	
	protected JSONArray encodeCoordiniate(Coordinate c){
		double[] _pts = new double[] {c.x, c.y};
		return JSONArray.fromObject(_pts);
	}
	
	public JSONArray encodeCoordinates(Coordinate[] cs){
		JSONArray[] _list = new JSONArray[cs.length];
		for (int i = 0; i < _list.length; i++) {
			_list[i] = this.encodeCoordiniate(cs[i]);
		}
		
		return JSONArray.fromObject(_list);
	}
	
	public JSONObject encodePoint(Point pt){
		JSONObject _obj = new JSONObject();
		_obj.put("type", "Point");
		_obj.put("coordinates", this.encodeCoordiniate(pt.getCoordinate()));
		
		return _obj;
	}
	
	public JSONObject encodeLineString(LineString line){
		JSONObject _obj = new JSONObject();
		
		_obj.put("type", "LineString");
		_obj.put("coordinates", this.encodeCoordinates(line.getCoordinates()));
		
		return _obj;
	}
	
	public JSONObject encodePolygon(Polygon polygon){
		List<JSONArray> _lines = new ArrayList<JSONArray>();
		_lines.add(this.encodeCoordinates(polygon.getExteriorRing().getCoordinates()));
		
		for(int i=0;i<polygon.getNumInteriorRing();i++){
			_lines.add(this.encodeCoordinates(polygon.getInteriorRingN(i).getCoordinates()));
		}
		
		JSONObject _obj = new JSONObject();
		_obj.put("type", "Polygon");		
		_obj.put("coordinates", JSONArray.fromObject(_lines));
		
		return _obj;
	}
	
	public JSONObject encodeMultiPoint(MultiPoint pts){
		JSONObject _obj = new JSONObject();
		_obj.put("type", "MultiPoint");
		_obj.put("coordinates", this.encodeCoordinates(pts.getCoordinates()));
		
		return _obj;
	}
	
	public JSONObject encodeMultiLineString(MultiLineString lines){
		List<JSONArray> _list = new ArrayList<JSONArray>();
		for(int i=0;i<lines.getNumGeometries();i++){
			_list.add(this.encodeCoordinates(lines.getGeometryN(i).getCoordinates()));
		}
		
		JSONObject _obj = new JSONObject();
		
		_obj.put("type", "MultiLineString");
		_obj.put("coordinates", JSONArray.fromObject(_list.toArray()));
		
		return _obj;
	}
	
	public JSONObject encodeMultiPolygon(MultiPolygon polygons){
		JSONArray[] _polygons = new JSONArray[polygons.getNumGeometries()];
		for(int i=0;i<polygons.getNumGeometries();i++){
			Polygon _polygon = (Polygon) polygons.getGeometryN(i);
			
			JSONArray[] _lines = new JSONArray[_polygon.getNumInteriorRing() + 1];
			_lines[0] = this.encodeCoordinates(_polygon.getExteriorRing().getCoordinates());
			
			for(int j=0;j<_polygon.getNumInteriorRing();j++){
				_lines[j+1] = this.encodeCoordinates(_polygon.getInteriorRingN(j).getCoordinates());
			}
			
			_polygons[i] = JSONArray.fromObject(_lines);
		}
		
		JSONObject _obj = new JSONObject();
		
		_obj.put("type", "MultiPolygon");
		_obj.put("coordinates", JSONArray.fromObject(_polygons));
		
		return _obj;
	}
	
	public JSONObject encodeGeometry(Geometry geo) throws UnsupportedGeoJSONType{
		if (geo instanceof Point) {
			return this.encodePoint((Point)geo);
		}
		else if(geo instanceof LineString) {
			return this.encodeLineString((LineString)geo);
		}
		else if (geo instanceof Polygon) {
			return this.encodePolygon((Polygon)geo);
		}
		else if (geo instanceof MultiPoint) {
			return this.encodeMultiPoint((MultiPoint)geo);
		}
		else if (geo instanceof MultiLineString) {
			return this.encodeMultiLineString((MultiLineString)geo);
		}
		else if (geo instanceof MultiPolygon) {
			return this.encodeMultiPolygon((MultiPolygon)geo);
		}
		
		throw new UnsupportedGeoJSONType(geo.getGeometryType());
	}
	
	public JSONObject encodeFeature(SimpleFeature f) throws UnsupportedGeoJSONType{
		if(f.getDefaultGeometry() == null)
			return null;
		
		JSONObject _f = new JSONObject();
		_f.put("type", "Feature");
		_f.put("id", f.getID());
		
		SimpleFeatureType _featureType = f.getFeatureType();
		JSONObject _props = new JSONObject();
		
		List<AttributeDescriptor> _attrs = _featureType.getAttributeDescriptors();
		for(int i=0;i<_attrs.size();i++){
			if(_attrs.get(i).equals(_featureType.getGeometryDescriptor()) == false){
//				log.info(_featureType.getAttributeType(i).getLocalName() + ":" + f.getAttribute(i).getClass().toString());
				_props.put(_attrs.get(i).getLocalName(), f.getAttribute(i));
			}
		}
		_f.put("properties", _props);
		_f.put("geometry", this.encodeGeometry((Geometry)f.getDefaultGeometry()));
		
		return _f;
	}
	
	public JSONObject encodeFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> fs) throws UnsupportedGeoJSONType{
		List<JSONObject> _f = new ArrayList<JSONObject>();
		FeatureIterator<SimpleFeature> _it = fs.features();
		try{
			while(_it.hasNext()){
				_f.add(this.encodeFeature(_it.next()));
			}
		}
		finally{
			_it.close();
		}
		
		while(_f.contains(null))
			_f.remove(null);
		
		JSONObject _type = new JSONObject();
		_type.put("type", "FeatureCollection");
		_type.put("features", JSONArray.fromObject(_f.toArray()));
		
		return _type;
	}
	
	public JSONObject encodeBox(Envelope env){
		JSONObject _type = new JSONObject();
		_type.put("type", "Box");
		_type.put("coordinates", this.encodeCoordinates(
				new Coordinate[]{
					new Coordinate(env.getMinX(), env.getMinY()),
					new Coordinate(env.getMaxX(), env.getMaxY())
				}));
		
		return _type;
	}
	
	/**
	 * Encode geometry, feature, featureCollection, envelope, etc to GeoJSON format
	 * 
	 * @param obj
	 * @return
	 * @throws UnsupportedGeoJSONType
	 */
	public JSONObject encode(Object obj) throws UnsupportedGeoJSONType{
		if (obj instanceof Geometry) {
			Geometry _geo = (Geometry) obj;
			return this.encodeGeometry(_geo);
		}
		else if(obj instanceof FeatureCollection){
			FeatureCollection<SimpleFeatureType, SimpleFeature> _fs = (FeatureCollection<SimpleFeatureType, SimpleFeature>) obj;
			return this.encodeFeatureCollection(_fs);
		}
		else if(obj instanceof SimpleFeature){
			SimpleFeature _f = (SimpleFeature) obj;
			return this.encodeFeature(_f);
		}
		else if(obj instanceof Envelope){
			Envelope _env = (Envelope) obj;
			return this.encodeBox(_env);
		}
		
		throw new UnsupportedGeoJSONType(obj.toString());
	}
	
	public void outputFeatureCollection(JSONObject fs, OutputStream stream){
//		StringBuffer _data = new StringBuffer();
//		_data.append("{");
//		_data.append("\"type\":" + "\"" + fs.getString("type") + "\",\"members\":[");
//		
//		JSONArray _members = fs.getJSONArray("members");
//		for(int i=0; i<_members.length();i++){
//		}
//		
//		IOUtils.write(_data, stream);
//		IOUtils.write("]}", stream);
	}

}