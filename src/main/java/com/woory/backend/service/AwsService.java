package com.woory.backend.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.woory.backend.error.CustomException;
import com.woory.backend.error.ErrorCode;
import com.woory.backend.utils.PhotoUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsService {
	private final AmazonS3 amazonS3;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;
	@Value("${spring.servlet.multipart.max-file-size}")
	private String maxSizeString;

	public String saveFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return null;
		}
		String filename = generateRandomFilename(file);

		log.info("File upload started : {}", filename);

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.getSize());
		metadata.setContentType(file.getContentType());

		try {
			amazonS3.putObject(bucket, filename, file.getInputStream(), metadata);
		} catch (AmazonS3Exception e) {
			log.error("파일 업로드 중 아마존 S3 오류 발생: {}", e.getMessage());
			throw new CustomException(ErrorCode.ERROR_SAVING_FILE);
		} catch (SdkClientException e) {
			log.error("파일 업로드 중 AWS SDK 클라이언트 오류 발생: {}", e.getMessage());
			throw new CustomException(ErrorCode.ERROR_SAVING_FILE);
		} catch (IOException e) {
			log.error("파일 업로드 중 IO 오류 발생");
			throw new CustomException(ErrorCode.ERROR_SAVING_FILE);
		}

		log.info("파일 업로드 성공: {}", filename);
		return amazonS3.getUrl(bucket, filename).toString();
	}

	public String generateRandomFilename(MultipartFile multipartFile) {
		String orgFilename = multipartFile.getOriginalFilename();
		return UUID.randomUUID() + "." + PhotoUtils.validateFileExtension(orgFilename);
	}

	public void deleteImage(String fileUrl) {
		String[] urlParts = fileUrl.split("/");
		String fileBucket = urlParts[2].split("\\.")[0];

		if (!fileBucket.equals(bucket)) {
			throw new CustomException(ErrorCode.FILE_DOES_NOT_EXIST);
		}

		String objectKey = String.join("/", Arrays.copyOfRange(urlParts, 3, urlParts.length));

		if (!amazonS3.doesObjectExist(bucket, objectKey)) {
			throw new CustomException(ErrorCode.FILE_DOES_NOT_EXIST);
		}

		try {
			amazonS3.deleteObject(bucket, objectKey);
		} catch (AmazonS3Exception e) {
			log.error("파일 삭제 오류 발생 : " + e.getMessage());
			throw new CustomException(ErrorCode.ERROR_DELETING_FILE);
		} catch (SdkClientException e) {
			log.error("AWS SDK 클라이언트 오류 발생 : " + e.getMessage());
			throw new CustomException(ErrorCode.ERROR_DELETING_FILE);
		}

		log.info("파일 삭제 성공 : {}", objectKey);
	}
}