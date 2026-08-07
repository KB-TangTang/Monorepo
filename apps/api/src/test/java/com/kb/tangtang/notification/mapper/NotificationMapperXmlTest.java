package com.kb.tangtang.notification.mapper;

import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.parsing.XPathParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * mapper/notification/*.xml 이 XML 로서 파싱되는지 검사한다.
 * SQL 부등호를 이스케이프하지 않으면 sqlSessionFactory 생성이 실패해 컨텍스트가 통째로 죽는다.
 */
class NotificationMapperXmlTest {

    @Test
    @DisplayName("알림 매퍼 XML 이 전부 파싱된다")
    void everyMapperXmlIsWellFormed() throws Exception {
        Path dir = Path.of("src/main/resources/mapper/notification");
        List<Path> files;
        try (Stream<Path> paths = Files.list(dir)) {
            files = paths.filter(p -> p.getFileName().toString().endsWith(".xml")).sorted().toList();
        }
        assertFalse(files.isEmpty(), "매퍼 XML 을 찾지 못했다");

        for (Path file : files) {
            try (InputStream in = Files.newInputStream(file)) {
                new XPathParser(in, true, null, new XMLMapperEntityResolver());
            } catch (Exception e) {
                fail(file.getFileName() + " 파싱 실패 — SQL 안의 <, & 를 이스케이프했는지 확인할 것: " + e);
            }
        }
    }
}
