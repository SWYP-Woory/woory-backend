package com.woory.backend.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import io.findify.s3mock.S3Mock;
@Profile("test")
@TestConfiguration
public class S3MockConfig {
	@Bean(name = "s3Mock")
	public S3Mock s3Mock() {
		return new S3Mock.Builder().withPort(8081).withInMemoryBackend().build();
	}

	@Primary
	@Bean(destroyMethod = "shutdown")
	public AmazonS3 amazonS3() {
		AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(
			"http://127.0.0.1:8081",
			Regions.AP_NORTHEAST_2.name());

		return AmazonS3ClientBuilder
			.standard()
			.withPathStyleAccessEnabled(true)
			.withEndpointConfiguration(endpoint)
			.withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
			.build();
	}
}

