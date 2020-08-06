package com.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.batch.db.RecordRepository;
import com.batch.tasklet.LinesProcessorDB;
import com.batch.tasklet.LinesReader;

@Configuration
@EnableBatchProcessing
@EnableBatchIntegration
@ComponentScan("com.batch")
public class BatchJobConfig {

	@Autowired
	private JobBuilderFactory jobs;

	@Autowired
	private StepBuilderFactory steps;

	@Autowired
	private RecordRepository recordRepository;

	@Bean
	public LinesReader linesReader() {
		return new LinesReader();
	}

	@Bean
	public LinesProcessorDB linesProcessor() {
		return new LinesProcessorDB(recordRepository);
	}

	@Bean
	protected Step readLines() {

		return steps.get("readLines").tasklet(linesReader()).build();
	}

	@Bean
	protected Step processLines() {
		return steps.get("processLines").tasklet(linesProcessor()).build();
	}

	@Bean
	public Job job() {
		return jobs.get("taskletsJob").start(readLines()).next(processLines()).build();
	}

}
