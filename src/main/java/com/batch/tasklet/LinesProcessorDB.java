package com.batch.tasklet;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.batch.db.Record;
import com.batch.db.RecordRepository;

public class LinesProcessorDB implements Tasklet, StepExecutionListener {

	private Logger logger = LoggerFactory.getLogger(LinesProcessorDB.class);

	private List<Record> records;

	private RecordRepository recordRepository;

	public LinesProcessorDB(RecordRepository recordRepository) {
		this.recordRepository = recordRepository;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void beforeStep(StepExecution stepExecution) {
		ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
		this.records = (List<Record>) executionContext.get("records");
		logger.debug("Lines Processor initialized.");
	}

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		for (Record record : records) {
			logger.info("Record saved on DB : " + record.toString());
			recordRepository.save(record);
			// To simulate failure
			// throw new RuntimeException();

		}
		chunkContext.setComplete();
		logger.info("************** Records saved in DB : " + recordRepository.count());
		return RepeatStatus.FINISHED;
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		logger.debug("Lines Processor ended.");
		return ExitStatus.COMPLETED;
	}
}