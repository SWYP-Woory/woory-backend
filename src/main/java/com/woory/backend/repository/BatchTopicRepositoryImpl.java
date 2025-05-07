package com.woory.backend.repository;

import com.woory.backend.dto.TopicDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Repository
public class BatchTopicRepositoryImpl implements BatchTopicRepository {

    private NamedParameterJdbcTemplate jdbcTemplate;
    public static final Integer BATCH_SIZE = 1000;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public BatchTopicRepositoryImpl(PlatformTransactionManager tm, DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(tm);
        this.transactionTemplate.setPropagationBehavior(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    @Override
    public int[] saveAll(List<TopicDto> topics) {
        log.info("토픽 배치 저장 실행");

        List<int[]> res = new ArrayList<>();
        for (int i = 0; i < topics.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, topics.size());
            List<TopicDto> chunk = topics.subList(i, end);

            int[] count = transactionTemplate.execute(status -> saveChunk(chunk));

            res.add(count);

        }

        return res.stream()
            .flatMapToInt(Arrays::stream)
            .toArray();
    }

    public int[] saveChunk(List<TopicDto> chunk) {
        List<Long> idx = jdbcTemplate.queryForList(
            "SELECT nextval('topic_seq') FROM generate_series(1, :cnt)",
            Map.of("cnt", BATCH_SIZE),
            Long.class
        );

        SqlParameterSource[] batch = new SqlParameterSource[chunk.size()];
        for (int j = 0; j < chunk.size(); j++) {
            TopicDto dto = chunk.get(j);
            batch[j] = new MapSqlParameterSource()
                .addValue("idx", idx.get(j))
                .addValue("topicContent", dto.getTopicContent())
                .addValue("issueDate", dto.getIssueDate())
                .addValue("groupId", dto.getGroupId())
                .addValue("topicByte", dto.getTopicByte());
        }

        int[] count = jdbcTemplate.batchUpdate(
            "INSERT INTO topic(topic_id, topic_content, issue_date, group_id, topic_byte) VALUES(:idx, :topicContent, :issueDate, :groupId, :topicByte)",
            batch
        );
        return count;
    }
}
