package com.batch.config;

import java.io.File;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.launch.JobLaunchingGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.GenericEndpointSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;

@Configuration
@ComponentScan("com.batch")
@IntegrationComponentScan
@EnableIntegration
@EnableBatchIntegration
@PropertySource("classpath:application.properties")
public class IntegrationFlowsConfig {

	private Logger logger = LoggerFactory.getLogger(IntegrationFlowsConfig.class);

	private static final String FILE_NAME_KEY = "input.file.name";

	@Value("${sftp.host}")
	private String sftpHost;

	@Value("${sftp.destination.host}")
	private String sftpDestHost;

	@Value("${sftp.port}")
	private int sftpPort;

	@Value("${sftp.username}")
	private String sftpUserName;

	@Value("${sftp.password}")
	private String sftpPassword;

	@Value("${sftp.source.directory}")
	private String sftpSourceDirectory;

	@Value("${sftp.local.directory}")
	private String sftpLocalDirectory;

	@Value("${sftp.maxFetchsize:20}")
	private int maxFetchSize;

	@Value("${sftp.source.directory.filePattern}")
	private String removeFilePattern;

	@Value("${sftp.source.directory.deleteRemoteFile}")
	private boolean deleteRemoveFile;
	
	@Value("${sftp.destination.processed.directory}")
	private String destinationDirectory;

	@Autowired
	private Job job;

	@Value("${sftp.polling.interval}")
	private int fixedDelay;

	@Autowired
	private JobRepository jobRepository;

	@Bean
	public SessionFactory<LsEntry> sftpSessionFactory() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
		factory.setHost(sftpHost);
		factory.setPort(sftpPort);
		factory.setUser(sftpUserName);
		factory.setPassword(sftpPassword);
		factory.setAllowUnknownKeys(true);
		return new CachingSessionFactory<LsEntry>(factory);
	}

	@Bean
	public SessionFactory<LsEntry> sftpDestinationSessionFactory() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
		factory.setHost(sftpDestHost);
		factory.setPort(sftpPort);
		factory.setUser(sftpUserName);
		factory.setPassword(sftpPassword);
		factory.setAllowUnknownKeys(true);
		return new CachingSessionFactory<LsEntry>(factory);
	}

	@Bean
	public JobLaunchingGateway jobLaunchingGateway() {
		SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
		simpleJobLauncher.setJobRepository(jobRepository);
		simpleJobLauncher.setTaskExecutor(new SyncTaskExecutor());
		JobLaunchingGateway jobLaunchingGateway = new JobLaunchingGateway(simpleJobLauncher);
		return jobLaunchingGateway;
	}

	@Bean
	public JobLaunchRequestTransformer fileMessageToJobRequest() {
		JobLaunchRequestTransformer fileMessageToJobRequest = new JobLaunchRequestTransformer();
		fileMessageToJobRequest.setFileParameterName(FILE_NAME_KEY);
		fileMessageToJobRequest.setJob(job);
		return fileMessageToJobRequest;
	}

	@Bean
	public MessageChannel onSuccessChannel() {
		return MessageChannels.direct().get();
	}

	/**
	 * integrationFlow - Main integration flow DSL
	 * @param jobLaunchingGateway
	 * @return
	 */
	
	@Bean
	public IntegrationFlow integrationFlow(JobLaunchingGateway jobLaunchingGateway) {

		return IntegrationFlows
				.from(Sftp.inboundAdapter(sftpSessionFactory()).remoteDirectory(sftpSourceDirectory)
						.regexFilter(removeFilePattern).localDirectory(new File(sftpLocalDirectory))
						.autoCreateLocalDirectory(true).maxFetchSize(maxFetchSize).deleteRemoteFiles(deleteRemoveFile),
						c -> c.poller(Pollers.fixedRate(fixedDelay).maxMessagesPerPoll(maxFetchSize)))
				.transform(fileMessageToJobRequest()).handle(jobLaunchingGateway)
				.log(LoggingHandler.Level.WARN, "headers.id + ': ' + payload").channel(onSuccessChannel()).get();
	}

	/**
	 * Success scenario , transfer the file to destination directory
	 * 
	 * @return
	 */
	@Bean
	public IntegrationFlow onSuccessFlow() {

		return IntegrationFlows.from(onSuccessChannel()).transform(new GenericTransformer<JobExecution, File>() {
			public File transform(JobExecution source) {
				// if batch operation failed , propagate to spring integration to handle
				if (source.getStatus() == BatchStatus.FAILED) {
					throw new RuntimeException("Batch operation failed");
				}
				
				return new File(source.getJobParameters().getString(FILE_NAME_KEY));

			};
		}).handle(Sftp.outboundAdapter(sftpDestinationSessionFactory()).remoteDirectory(destinationDirectory)
				.fileNameGenerator(new FileNameGenerator() {

					@Override
					public String generateFileName(Message<?> message) {
						return ((File) message.getPayload()).getName() + "-processed-"
								+ LocalDateTime.now().toString().replace(":", "") + ".csv";
					}
				}), c -> c.advice(expressionAdvice(c)))		
				.get();
	}
	
	@Bean
	public ExpressionEvaluatingRequestHandlerAdvice expressionAdvice(GenericEndpointSpec<FileTransferringMessageHandler<ChannelSftp.LsEntry>> c) {
	    ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
	    //advice.setOnSuccessExpressionString("payload.delete()");
	    advice.setTrapException(false);
	    return advice;
	}

	/**
	 * Handles all unexpected errors
	 * 
	 * @return
	 */
	@Bean
	public IntegrationFlow onErrorFlow() {

		return IntegrationFlows.from("errorChannel").transform(new GenericTransformer<MessagingException, File>() {
			@Override
			public File transform(MessagingException payload) {
				logger.error("**** Exception occurred : " + payload.getFailedMessage().getPayload());
				String fileName = ((JobExecution) payload.getFailedMessage().getPayload()).getJobParameters()
						.getString(FILE_NAME_KEY);
				return new File(fileName);
			}
		}).handle(Sftp.outboundAdapter(sftpSessionFactory()).remoteDirectory("/processed")
				.fileNameGenerator(new FileNameGenerator() {

					@Override
					public String generateFileName(Message<?> message) {
						return ((File) message.getPayload()).getName() + "-errored-"
								+ LocalDateTime.now().toString().replace(":", "") + ".csv";
					}
				})).get();

	}

}
