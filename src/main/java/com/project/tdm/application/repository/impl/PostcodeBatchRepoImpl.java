package com.project.tdm.application.repository.impl;

import com.project.tdm.application.entity.PostcodeEntity;
import com.project.tdm.application.repository.PostcodeBatchRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostcodeBatchRepoImpl implements PostcodeBatchRepo {

    private static final Logger logger = LoggerFactory.getLogger(PostcodeBatchRepoImpl.class);
    private final JdbcTemplate jdbcTemplate;

    public PostcodeBatchRepoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String UPSERT_SQL =
            "INSERT INTO postcodes (postcode, latitude, longitude) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT (postcode) " +
                    "DO UPDATE SET latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude";

    @Override
    public void batchUpsertWithFallback(List<PostcodeEntity> records) {
        try {
            jdbcTemplate.batchUpdate(UPSERT_SQL, records, records.size(),
                    (ps, record) -> {
                        ps.setString(1, record.getPostcode());
                        ps.setDouble(2, record.getLatitude());
                        ps.setDouble(3, record.getLongitude());
                    });
        }
        catch (DataAccessException e) {
            logger.warn("Batch execution failed for {} records. Triggering single-row fallback. Reason: {}", records.size(), e.getMessage());
            fallbackSingleUpsert(records);
        }
    }

    private void fallbackSingleUpsert(List<PostcodeEntity> records) {
        for (PostcodeEntity record : records) {
            try {
                jdbcTemplate.update(UPSERT_SQL, record.getPostcode(), record.getLatitude(), record.getLongitude());
            }
            catch (DataAccessException ex) {
                logger.error("Failed to process individual postcode record [{}]: {}", record.getPostcode(), ex.getMessage());
            }
        }
    }
}
