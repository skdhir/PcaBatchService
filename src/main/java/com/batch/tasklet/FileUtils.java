package com.batch.tasklet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.batch.db.Record;
import com.opencsv.CSVReader;

public class FileUtils {

	private final Logger logger = LoggerFactory.getLogger(FileUtils.class);

	private String fileName;
	private CSVReader reader;
	private FileReader fileReader;
	private File file;

	public FileUtils(String fileName) {
		this.fileName = fileName;
	}

	public Record readLine() {
		try {
			if (reader == null)
				initReader();
			String[] line = reader.readNext();
			if (line == null)
				return null;
			return getRecord(line);
			
		} catch (Exception e) {
			logger.error("Error while reading line in file: " + this.fileName);
			return null;
		}
	}

	private Record getRecord(String[] line) {
		Record record  = new Record();		
		record.setColumn1(line[1]);
		record.setColumn2(line[2]);
		return record;
	}

	private void initReader() throws Exception {
		if (file == null)
			file = new File(fileName);
		if (fileReader == null)
			fileReader = new FileReader(file);
		if (reader == null)
			reader = new CSVReader(fileReader);
	}

	public void closeReader() {
		try {
			if (reader != null)
				reader.close();
			if (fileReader != null)
				fileReader.close();
		} catch (IOException e) {
			logger.error("Error while closing reader.");
		}
	}

}