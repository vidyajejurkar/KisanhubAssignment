import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DataFetcher {

	Map<String, Double> avgMap = new HashMap<String, Double>();
	Map<String, ArrayList<WeatherData>> dataMap = new HashMap<String, ArrayList<WeatherData>>();
	String maxKey = null;
	Double max = 0.0;
    String Url = "https://www.metoffice.gov.uk/climate/uk/summaries/datasets#yearOrdered";
	public void getTableData() { //Parsing HTML using Jsoup library
		Document doc;
		System.out.println("Program Started\n");
		try {
			doc = Jsoup.connect(Url).get();

			Element table = doc.select("table.table").get(1);
			Elements rows = table.select("tr");

			for (int j = 1; j < 5; j++) {
				Elements cols = rows.get(j).select("td");

				if (cols != null && cols.size() > 0) {
					String title = "";
					for (int i = 1; i < 6; i++) {
						title = cols.get(i).select("a[href]").attr("title");
						String url = cols.get(i).select("a[href]").attr("href");
						String arr[] = title.split(" ");
						String region = arr[0];
						String weather_param = arr[2];

						dataMap.put(region + "." + weather_param, downloadFile(url, region, weather_param));
					}
				}
			}

			writeToCSV(dataMap);
			System.out.println("--------DATA--------------");
			for (int i = 0; i < ConstantData.WEATHER_PARAMETER.length; i++) {
				max(avgMap, ConstantData.WEATHER_PARAMETER[i]);
				WeatherData data = getFactsData(dataMap.get(maxKey));

				System.out.println("Averagely " + data.region_code + " has the highest value for "
						+ ConstantData.WEATHER_PARAMETER_FOR_CSV[i].toLowerCase()
						+ " among other regions. From 1910 to 2017 it was highest in " + data.key.toLowerCase() + " "
						+ data.year + " and value was " + data.value);
			}
			System.out.println("-----------------------------");
			System.out.println("\nCSV File is ready. Check dowbload directory folder.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * To Download the file from the given URL and create a Arraylist of all
	 * files
	 */
	public ArrayList<WeatherData> downloadFile(String url, String title, String weatherParam) throws IOException {
		ArrayList<WeatherData> weatherDataList = new ArrayList<>();

		BufferedReader bufferedReader;
		try {
			url = url.replace("http", "https");
			URL fileUrl = new URL(url);
			String line = null;

			URLConnection con = fileUrl.openConnection();
			InputStream inputStream = con.getInputStream();
			InputStreamReader streamReader = new InputStreamReader(inputStream);
			bufferedReader = new BufferedReader(streamReader);

			// To skip first 8 lines of description
			for (int i = 1; i <= 8; i++) {
				bufferedReader.readLine();
			}

			while ((line = bufferedReader.readLine()) != null) {
				line = line.replaceAll("\\s{5,}", " " + ConstantData.NA + " ").trim(); // If more than 5 spaces then insert N/A
				line = line.replaceAll("\\s+", ","); //Replace all spaces with ,

				String[] arr = line.split(",");

				for (int i = 0, j = 1; i < ConstantData.MONTHS.length; i++, j++) {
					weatherDataList.add(new WeatherData(title, weatherParam, arr[0], ConstantData.MONTHS[i], arr[j]));
				}
			}
			inputStream.close();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		return weatherDataList;
	}

	private void writeToCSV(Map<String, ArrayList<WeatherData>> weatherMap) {
		File downloadDirectory = new File("download directory");
		if (!downloadDirectory.exists()) {
			downloadDirectory.mkdirs();
		}

		FileWriter writer = null;
		try {
			writer = new FileWriter(downloadDirectory + "/" + "weather_data.csv");
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// Delimiter used in CSV file
		String COMMA_DELIMITER = ",";
		String NEW_LINE_SEPARATOR = "\n";
		// CSV file header
		String FILE_HEADER = "region_code,weather_param,year, key, value";

		// Write the CSV file header
		try {
			writer.append(FILE_HEADER.toString());
			// Add a new line separator after the header
			writer.append(NEW_LINE_SEPARATOR);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < ConstantData.REGION_LIST.length; i++) {
			for (int j = 0; j < ConstantData.WEATHER_PARAMETER.length; j++) {
				ArrayList<WeatherData> dataList = weatherMap
						.get(ConstantData.REGION_LIST[i] + "." + ConstantData.WEATHER_PARAMETER[j]);
				try {
					for (WeatherData weatherData : dataList) {
						writer.append(weatherData.getRegion_code());
						writer.append(COMMA_DELIMITER);
						writer.append(ConstantData.WEATHER_PARAMETER_FOR_CSV[j]);
						writer.append(COMMA_DELIMITER);
						writer.append(weatherData.getYear());
						writer.append(COMMA_DELIMITER);
						writer.append(weatherData.getKey());
						writer.append(COMMA_DELIMITER);
						writer.append(String.valueOf(weatherData.getValue()));
						writer.append(NEW_LINE_SEPARATOR);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				getAverage(dataList);
			}
		}

		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void getAverage(ArrayList<WeatherData> dataList) {
		double avg = 0;
		for (WeatherData wd : dataList) {
			if (!wd.value.equals(ConstantData.NA))
				avg += Double.parseDouble(wd.value);
		}
		avg = avg / dataList.size();
		avgMap.put(dataList.get(0).region_code + "." + dataList.get(0).weather_param, avg);
		dataMap.put(dataList.get(0).region_code + "." + dataList.get(0).weather_param, dataList);
	}

	public void max(Map<String, Double> avgMap, String weather_param) {

		Set<String> keySet = avgMap.keySet();
		max = 0.0;
		maxKey = null;
		for (String val : keySet) {
			if (val.contains(weather_param)) {
				if (max < avgMap.get(val)) {
					max = avgMap.get(val);
					maxKey = val;
				}
			}
		}
	}

	private WeatherData getFactsData(ArrayList<WeatherData> dataList) {
		WeatherData weatherData = null;
		Double prev = 0.0;

		Iterator<WeatherData> iterator = dataList.iterator();
		while (iterator.hasNext()) {
			WeatherData object = (WeatherData) iterator.next();
			Double curr = 0.0;
			if (!object.value.equals(ConstantData.NA)) {
				curr = Double.parseDouble(object.value);
			}
			if (curr > 0.0 && prev < curr) {
				prev = curr;
				weatherData = object;
			}
		}
		return weatherData;
	}
}
