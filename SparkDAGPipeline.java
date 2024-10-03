//package com.sbs;
//
//import org.apache.spark.SparkConf;
//import org.apache.spark.api.java.JavaRDD;
//import org.apache.spark.api.java.JavaSparkContext;
//import org.apache.spark.sql.Dataset;
//import org.apache.spark.sql.Row;
//import org.apache.spark.sql.SparkSession;
//
//public class SparkDAGPipeline {
//
//    public static void main(String[] args) {
//        // Create Spark configuration
//        SparkConf conf = new SparkConf().setAppName("Spark DAG Example").setMaster("local[*]");
//
//        // Create JavaSparkContext
//        JavaSparkContext sc = new JavaSparkContext(conf);
//
//        // Create SparkSession
//        SparkSession spark = SparkSession.builder()
//                .appName("Spark DAG Example")
//                .config(sc.getConf())
//                .getOrCreate();
//
//        // Read data from CSV file
//        Dataset<Row> inputData = spark.read()
//                .option("header", "true")
//                .csv("path/to/input.csv");
//
//        // Transformations: Example - Filter and Select
//        Dataset<Row> transformedData = inputData.filter("age > 30")
//                .select("name", "age");
//
//        // Write the result to a new CSV file
//        transformedData.write()
//                .option("header", "true")
//                .csv("path/to/output.csv");
//
//        // Stop the Spark context
//        sc.stop();
//    }
//}
