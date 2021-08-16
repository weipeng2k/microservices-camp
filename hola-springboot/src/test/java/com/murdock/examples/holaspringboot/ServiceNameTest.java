package com.murdock.examples.holaspringboot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author weipeng2k 2018年05月30日 下午14:52:31
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ServiceNameTest {

    private RestTemplate template = new RestTemplate();

    private Map<String, Integer> map = new HashMap<>();

    private int errorCount = 0;

    /**
     * appName
     *
     * @throws Exception
     */
    @Test
    public void content() throws Exception {
        Path path = Paths.get(new File("service.txt").toURI());
        Files.lines(path)
                .map(this::create)
                .filter((si) -> si.num > 0)
                .forEach((si) -> {
                    try {
                        Map object = template.getForObject(
                                "http://hsf.alibaba-inc.com/hsfops/envs/online/services/{service}/application",
                                Map.class, si.service);

                        String appName = (String) object.get("appName");
                        map.computeIfAbsent(appName, (an) -> si.num);
                        map.computeIfPresent(appName, (an, old) -> Math.max(si.num, old));
                    } catch(Exception ex) {
                        System.out.println("got error on " + si);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        errorCount++;
                    }
                });

        map.entrySet().stream()
                .map(stringIntegerEntry -> {
                    AppInstance appInstance = new AppInstance();
                    appInstance.app = stringIntegerEntry.getKey();
                    appInstance.num = stringIntegerEntry.getValue();
                    return appInstance;
                })
                .sorted(Comparator.comparingInt(AppInstance::getNum))
                .forEach(System.out::println);
    }

    ServiceInstance create(String line) {
        String[] split = line.split("\t");
        ServiceInstance si = new ServiceInstance();
        si.service = split[0];
        si.num = Integer.parseInt(split[2]);
        return si;
    }

    @Test
    public void test() {
        Map object = template.getForObject(
                "http://hsf.alibaba-inc.com/hsfops/envs/online/services/{service}/application",
                Map.class, "com.wdk.wdkhummer.client.shop.service.ShopStockService:1.0.0");
        System.out.println(object);
    }

    class ServiceInstance {
        String service;

        int num;


        @Override
        public String toString() {
            return "ServiceInstance{" +
                    "service='" + service + '\'' +
                    ", num=" + num +
                    '}';
        }
    }

    class AppInstance {
        String app;

        int num;

        public int getNum() {
            return num;
        }

        @Override
        public String toString() {
            return app + ":" + num;
        }
    }

}
