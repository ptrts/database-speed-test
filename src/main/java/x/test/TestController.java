package x.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping(consumes = MediaType.APPLICATION_YAML_VALUE)
    public void test(@RequestBody List<LaunchParameters> launches) {
        launches.forEach(launch -> {
            logger.info("{}", launch);
            //bigTest.test(launch);
        });
    }
}
