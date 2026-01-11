package x.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import x.test.parameters.LaunchParameters;

import java.util.List;

@RestController
@RequestMapping("test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private BigTest bigTest;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private AsyncTaskExecutor applicationTaskExecutor;

    @PostMapping(consumes = MediaType.APPLICATION_YAML_VALUE)
    public void test(@RequestBody List<LaunchParameters> launches) {
        applicationTaskExecutor.submit(() -> {
            launches.forEach(launch -> {
                logger.info("{}", launch);
                bigTest.test(launch);
            });
        });
    }
}
