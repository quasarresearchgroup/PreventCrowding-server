package org.quasar.route.graphhopper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.influxdb.query.FluxTable;
import com.mongodb.client.FindIterable;
import org.bson.Document;

import com.graphhopper.ResponsePath;
import com.graphhopper.util.shapes.GHPoint3D;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

/**
 * MongoConnection is a class that allows to connect to MongoDB database and
 * extract the data provided about the crowding levels
 * 
 * @author Rúben Beirão
 * @author Fernando Brito e Abreu
 *
 */

public class MongoConnection {
	private MongoClient mongoClient;
	private MongoDatabase database;
	private MongoCollection<Document> pois;
	private MongoCollection<Document> grid_segments;
	private MongoCollection<Document> segments;

	/**
	 * Creates an instance based on a (single) mongodb node (localhost, default
	 * port). Gets a MongoDatabase representing the specified database. Gets a
	 * specified collection.
	 * 
	 */
	public MongoConnection() {
		this.mongoClient = new MongoClient("194.210.120.12", 27017);
		this.database = mongoClient.getDatabase("crowding");
		this.pois = database.getCollection("POIs");
		this.grid_segments = database.getCollection("grid_segments");
		this.segments = database.getCollection("segments");
		java.util.logging.Logger.getLogger("org.mongodb.driver").setLevel(Level.OFF);
	}

	/**
	 * This method iterates the coordinates of a path between two points and
	 * searches in the defined database and collection of MongoDB the values of
	 * crowding of those coordinates. Then returns the maximum value of crowding
	 * found.
	 * 
	 * @param path Represents a ResponsePath which holds the data of a Path
	 * @return An int representing the maximum value of crowding in a path
	 */
	public int getMaxCrowdingPath(ResponsePath path, Timestamp time) {
		int maxCrowding = 0;

		int pathSize = path.getPoints().size();

		System.out.println(time.getTime()/1000);
		System.out.println("PATHSIZE" + path);

		for (GHPoint3D ghPoint : path.getPoints()) {
			Point refPoint = new Point(new Position(ghPoint.getLon(), ghPoint.getLat()));
			Document closestCrowding = pois.find(Filters.near("geometry", refPoint, 3000.0, 0.0)).first();
			Document properties = (Document) closestCrowding.get("properties");


			Document gridWithin = grid_segments.find(Filters.geoIntersects("geometry", refPoint)).first();
			//for (Document doc : segmentsWithin) {
			//	Document prop = (Document) doc.get("properties");
			//	int gridId = prop.getInteger("id");
			//	System.out.println(gridId);
			//}
			Document prop = (Document) gridWithin.get("properties");
			int grid = Integer.valueOf(prop.getInteger("id"));

			InfluxConnection influx = new InfluxConnection();
			int crowding = influx.queryCrowdingFromGrid(time.getTime()/1000, grid);

			if (crowding > maxCrowding)
				maxCrowding = crowding;
		}

		System.out.println("MAXCROWDING " + maxCrowding);

		return maxCrowding;
	}

	/**
	 * Gets a MongoDB client with internal connection pooling.
	 * 
	 * @return an instance of a MongoClient
	 */
	public MongoClient getMongoClient() {
		return mongoClient;
	}

	/**
	 * Gets the MongoDB database
	 * 
	 * @return a MongoDatabase representing the accessed database in MongoDB
	 */
	public MongoDatabase getDatabase() {
		return database;
	}

	/**
	 * Gets the MongoDB database collection
	 * 
	 * @return a MongoCollection document representing the collection in the
	 *         database in MongoDB
	 */
	public MongoCollection<Document> getPois() {
		return pois;
	}

	public MongoCollection<Document> getSegments() {
		return segments;
	}

	public static void main(String[] args) {

		MongoClient mongoClient = new MongoClient("194.210.120.12", 27017);
		MongoDatabase database = mongoClient.getDatabase("Crowding");
		MongoCollection<Document> collection = database.getCollection("POIs");

		for (Document cur : collection.find()) {
			Document geometry = (Document) cur.get("geometry");
			ArrayList<ArrayList<Double>> lines = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");

			for (ArrayList<Double> p : lines) {
				System.out.println(p.get(0) + "," + p.get(1));
			}
			System.out.println();
		}

	}

}
