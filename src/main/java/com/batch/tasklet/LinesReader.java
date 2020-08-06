package com.batch.tasklet;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import com.batch.db.Record;

/**
 * Simple line reader
 */
public class LinesReader implements Tasklet, StepExecutionListener {

	private final Logger logger = LoggerFactory.getLogger(LinesReader.class);

	private List<Record> records;
	private FileUtils fu;

	@Override
	public void beforeStep(StepExecution stepExecution) {
		String path = stepExecution.getJobExecution().getJobParameters().getString("input.file.name");
		records = new ArrayList<>();
		fu = new FileUtils(path);
		logger.debug("Lines Reader initialized.");
	}

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		Record record = fu.readLine();
		while (record != null) {
			records.add(record);
			logger.debug("Read line: " + record.toString());
			record = fu.readLine();
		}
		return RepeatStatus.FINISHED;
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		fu.closeReader();
		stepExecution.getJobExecution().getExecutionContext().put("records", this.records);
		logger.debug("Lines Reader ended.");
		return ExitStatus.COMPLETED;
	}
}