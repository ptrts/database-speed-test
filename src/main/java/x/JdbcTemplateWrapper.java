package x;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.AggregatedBatchUpdateException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterDisposer;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class JdbcTemplateWrapper {

    public static final Logger logger = LoggerFactory.getLogger(JdbcTemplateWrapper.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Калька JdbcTemplate.batchUpdate с добавлением таймера.
     */
    public <T> int[][] batchUpdate(
            String sql,
            Collection<T> batchArgs,
            int batchSize,
            ParameterizedPreparedStatementSetter<T> pss,
            AbstractTimerWrapper timerWrapper
    ) throws DataAccessException {

        if (logger.isDebugEnabled()) {
            logger.debug("Executing SQL batch update [" + sql + "] with a batch size of " + batchSize);
        }
        int[][] result = jdbcTemplate.execute(sql, (PreparedStatementCallback<int[][]>) ps -> {
            List<int[]> rowsAffected = new ArrayList<>();
            try {
                boolean batchSupported = JdbcUtils.supportsBatchUpdates(ps.getConnection());
                int n = 0;
                for (T obj : batchArgs) {
                    pss.setValues(ps, obj);
                    n++;
                    if (batchSupported) {
                        ps.addBatch();
                        if (n % batchSize == 0 || n == batchArgs.size()) {
                            int batchIdx = (n % batchSize == 0) ? n / batchSize : (n / batchSize) + 1;
                            int items = n - ((n % batchSize == 0) ? n / batchSize - 1 : (n / batchSize)) * batchSize;
                            if (logger.isTraceEnabled()) {
                                logger.trace("Sending SQL batch update #" + batchIdx + " with " + items + " items");
                            }
                            try {
                                String fullBatch = items == batchSize ? "1" : "0";
                                int[] updateCounts = timerWrapper.withTimer(ps::executeBatch, "fullBatch", fullBatch);
                                rowsAffected.add(updateCounts);
                            }
                            catch (BatchUpdateException ex) {
                                throw new AggregatedBatchUpdateException(rowsAffected.toArray(int[][]::new), ex);
                            }
                            catch (Exception ignored) {
                            }
                        }
                    }
                    else {
                        int i = ps.executeUpdate();
                        rowsAffected.add(new int[] {i});
                    }
                }
                int[][] result1 = new int[rowsAffected.size()][];
                for (int i = 0; i < result1.length; i++) {
                    result1[i] = rowsAffected.get(i);
                }
                return result1;
            }
            finally {
                if (pss instanceof ParameterDisposer parameterDisposer) {
                    parameterDisposer.cleanupParameters();
                }
            }
        });

        Assert.state(result != null, "No result array");
        return result;
    }
}
