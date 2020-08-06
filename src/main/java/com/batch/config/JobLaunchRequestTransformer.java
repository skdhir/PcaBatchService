package com.batch.config;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.Message;

/**
 * Spring integration transformer for JobLaunch request from sftp file message *
 * 
 *
 */
public class JobLaunchRequestTransformer {
	private Job job;

	private String fileParameterName;

	public void setFileParameterName(String fileParameterName) {
		this.fileParameterName = fileParameterName;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	/**
	 * Spring integration transformer for JobLaunch request from sftp file message
	 * 
	 * @param message
	 * @return
	 */
	@Transformer
	public JobLaunchRequest toRequest(Message<File> message) {
		JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
		jobParametersBuilder.addString(fileParameterName, message.getPayload().getAbsolutePath());
		jobParametersBuilder.addDate("currentDateTime", new Date());
		return new JobLaunchRequest(job, jobParametersBuilder.toJobParameters());
	}

}